/// Post-Quantum Lattice Cryptography Module
///
/// Implements Learning With Errors (LWE) encryption, the same hard lattice
/// problem underlying CRYSTALS-Kyber (NIST FIPS 203, ML-KEM).
///
/// Parameters: n=16, q=3329 (Kyber prime), η=2 (centered binomial error).
/// Kyber-512 uses n=256. We use n=16 because this is a calculator, not GCHQ.
/// The math is correct. The security level is "educational". The vibe is immaculate.
///
/// Security proof sketch: Breaking this requires solving LWE_{16,3329,B_2},
/// which is hard (for a computer from 1987). Modern adversaries may vary.
///
/// Reference: Regev, O. (2005). "On lattices, learning with errors, random
/// linear codes, and cryptography." STOC '05.
use sha2::{Digest, Sha256};

/// LWE parameters — inspired by Kyber, scaled down for a pocket calculator
const N: usize = 16;
const Q: u32 = 3329; // Kyber's prime: smallest prime ≡ 1 (mod 256)
const ETA: usize = 2; // Error distribution half-width (Kyber: 2 or 3)

/// Deterministic pseudorandom generator via iterated SHA-256.
/// No `rand` crate needed — we live off the land.
struct Prng {
    state: [u8; 32],
    ctr: u64,
}

impl Prng {
    fn new(seed: &[u8]) -> Self {
        let h = Sha256::digest(seed);
        let mut state = [0u8; 32];
        state.copy_from_slice(&h);
        Prng { state, ctr: 0 }
    }

    fn next_block(&mut self) -> [u8; 32] {
        let mut h = Sha256::new();
        h.update(self.state);
        h.update(self.ctr.to_le_bytes());
        self.ctr += 1;
        let out = h.finalize();
        let mut b = [0u8; 32];
        b.copy_from_slice(&out);
        b
    }

    /// Sample uniformly from Z_q
    fn uniform_u32(&mut self) -> u32 {
        let b = self.next_block();
        u32::from_le_bytes([b[0], b[1], b[2], b[3]]) % Q
    }

    /// Sample an N-element vector uniformly from Z_q^N
    fn uniform_vec(&mut self) -> [u32; N] {
        let mut v = [0u32; N];
        for x in v.iter_mut() {
            *x = self.uniform_u32();
        }
        v
    }

    /// Sample from centered binomial distribution B_η.
    /// Each coefficient is (sum of η bits) − (sum of η bits) ∈ [−η, η].
    /// This approximates a discrete Gaussian, which is what real Kyber uses.
    fn cbd_vec(&mut self) -> [i32; N] {
        let mut v = [0i32; N];
        let b = self.next_block();
        for i in 0..N {
            let byte = b[i % 32] as usize;
            let pos: i32 = (0..ETA).map(|j| ((byte >> j) & 1) as i32).sum();
            let neg: i32 = (ETA..2 * ETA).map(|j| ((byte >> j) & 1) as i32).sum();
            v[i] = pos - neg;
        }
        v
    }
}

/// Reduction modulo Q (handles negative values via rem_euclid)
#[inline]
fn modq(x: i64) -> u32 {
    x.rem_euclid(Q as i64) as u32
}

/// Matrix-vector product: A · s (mod Q), A is row-major
fn matvec(a: &[[u32; N]; N], s: &[i32; N]) -> [u32; N] {
    let mut r = [0u32; N];
    for (i, row) in a.iter().enumerate() {
        r[i] = modq(
            row.iter()
                .zip(s.iter())
                .map(|(&ai, &si)| ai as i64 * si as i64)
                .sum(),
        );
    }
    r
}

/// Transposed matrix-vector product: A^T · r (mod Q)
fn mat_t_vec(a: &[[u32; N]; N], r: &[i32; N]) -> [u32; N] {
    let mut out = [0u32; N];
    for j in 0..N {
        out[j] = modq(
            (0..N)
                .map(|i| a[i][j] as i64 * r[i] as i64)
                .sum(),
        );
    }
    out
}

/// Inner product of u32 vector and i32 vector mod Q
fn dot_ui(a: &[u32; N], b: &[i32; N]) -> u32 {
    modq(
        a.iter()
            .zip(b.iter())
            .map(|(&ai, &bi)| ai as i64 * bi as i64)
            .sum(),
    )
}

/// Inner product of two u32 vectors mod Q
fn dot_uu(a: &[u32; N], b: &[u32; N]) -> u32 {
    modq(
        a.iter()
            .zip(b.iter())
            .map(|(&ai, &bi)| ai as i64 * bi as i64)
            .sum(),
    )
}

/// LWE public key: (A, b) where b = A·s + e mod Q
struct Pk {
    a: [[u32; N]; N],
    b: [u32; N],
}

/// LWE secret key: s (stored as non-negative Z_q representatives)
struct Sk {
    s: [u32; N],
}

/// Generate an LWE keypair deterministically from a seed.
///
/// KeyGen:
///   A  ← Uniform(Z_q^{N×N})   (public matrix, derived from seed)
///   s  ← B_η^N                 (small secret)
///   e  ← B_η^N                 (small error)
///   b  = A·s + e  mod Q        (public vector)
fn keygen(seed: &[u8]) -> (Pk, Sk) {
    let mut rng = Prng::new(seed);

    // Sample public matrix A
    let mut a = [[0u32; N]; N];
    for row in a.iter_mut() {
        *row = rng.uniform_vec();
    }

    // Sample secret and error from centered binomial
    let s_signed = rng.cbd_vec();
    let e_signed = rng.cbd_vec();

    // b = A·s + e mod Q
    let as_prod = matvec(&a, &s_signed);
    let mut b = [0u32; N];
    for i in 0..N {
        b[i] = modq(as_prod[i] as i64 + e_signed[i] as i64);
    }

    // Store s as non-negative Z_q representatives
    let mut sv = [0u32; N];
    for i in 0..N {
        sv[i] = s_signed[i].rem_euclid(Q as i32) as u32;
    }

    (Pk { a, b }, Sk { s: sv })
}

/// Encrypt a single bit m ∈ {0, 1} under public key pk.
///
/// Enc(pk, m, nonce):
///   r   ← B_η^N    (one-time blinding vector)
///   e₁  ← B_η^N    (ciphertext error)
///   e₂  ← small    (scalar error)
///   u   = A^T·r + e₁  mod Q
///   v   = b·r + e₂ + ⌊Q/2⌋·m  mod Q
fn encrypt(pk: &Pk, bit: u8, nonce: &[u8]) -> ([u32; N], u32) {
    let mut rng = Prng::new(nonce);

    let r = rng.cbd_vec();
    let e1 = rng.cbd_vec();
    let e2 = rng.uniform_u32() % (ETA as u32 + 1); // small scalar error

    // u = A^T · r + e1 mod Q
    let atr = mat_t_vec(&pk.a, &r);
    let mut u = [0u32; N];
    for i in 0..N {
        u[i] = modq(atr[i] as i64 + e1[i] as i64);
    }

    // v = b · r + e2 + ⌊Q/2⌋·m mod Q
    let br = dot_ui(&pk.b, &r);
    let msg_term: u32 = if bit == 1 { (Q + 1) / 2 } else { 0 };
    let v = modq(br as i64 + e2 as i64 + msg_term as i64);

    (u, v)
}

/// Decrypt ciphertext (u, v) using secret key sk.
///
/// Dec(sk, u, v):
///   d  = v − s·u  mod Q
///   If d ∈ [Q/4, 3Q/4): output 1, else output 0
///
/// Correctness: d = v − s·u = (b·r + e₂ + msg) − s·(A^T·r + e₁)
///            = (A·s+e)·r + e₂ + msg − s·A^T·r − s·e₁
///            = e·r + e₂ − s·e₁ + msg
/// Noise |e·r + e₂ − s·e₁| ≤ N·η² + η + N·η·η ≈ 128 << Q/4 = 832 ✓
fn decrypt(sk: &Sk, u: &[u32; N], v: u32) -> u8 {
    let su = dot_uu(&sk.s, u);
    let d = modq(v as i64 - su as i64 + Q as i64 * 2);

    // Round to nearest multiple of ⌊Q/2⌋
    let q_quarter = Q / 4;
    let q_three_quarters = 3 * Q / 4;
    if d >= q_quarter && d < q_three_quarters {
        1
    } else {
        0
    }
}

/// Fixed global key seed — every installation uses the same "enterprise" key.
/// In production you'd rotate this. This is a calculator.
static PQ_KEY_SEED: &[u8] = b"CalcuLux-Enterprise-PQ-KeySeed-v2.1.0:lattice-goes-brrr";

/// Produce a compact post-quantum seal for arbitrary data.
///
/// Uses deterministic LWE encryption (nonce = SHA-256(data)) so the same
/// data always produces the same seal. This makes it a commitment scheme:
/// you can verify a seal by re-running seal_data and comparing.
///
/// Returns a 12-hex-char string: first two ciphertext elements + v.
/// Real Kyber-512 ciphertext is 768 bytes. Ours is 6 bytes. Efficiency.
pub fn seal_data(data: &str) -> String {
    let (pk, _) = keygen(PQ_KEY_SEED);

    // Deterministic nonce from data content
    let nonce = Sha256::digest(data.as_bytes());

    // Encrypt bit 1 — "this calculation has been post-quantum acknowledged"
    let (u, v) = encrypt(&pk, 1, &nonce);

    // Compact ciphertext: first two u elements + v (3 × 4 hex chars = 12 chars)
    format!("{:04x}{:04x}{:04x}", u[0], u[1], v)
}

/// Verify that a seal was produced from the given data.
/// Returns true if the seal is authentic (i.e., data has not been tampered with).
pub fn verify_seal(data: &str, seal: &str) -> bool {
    seal_data(data) == seal
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_encrypt_decrypt_zero() {
        let (pk, sk) = keygen(b"test-seed");
        let (u, v) = encrypt(&pk, 0, b"nonce-0");
        assert_eq!(decrypt(&sk, &u, v), 0, "decrypt(encrypt(0)) should be 0");
    }

    #[test]
    fn test_encrypt_decrypt_one() {
        let (pk, sk) = keygen(b"test-seed");
        let (u, v) = encrypt(&pk, 1, b"nonce-1");
        assert_eq!(decrypt(&sk, &u, v), 1, "decrypt(encrypt(1)) should be 1");
    }

    #[test]
    fn test_seal_deterministic() {
        let s1 = seal_data("11+2=13");
        let s2 = seal_data("11+2=13");
        assert_eq!(s1, s2, "same data must produce same seal");
    }

    #[test]
    fn test_seal_distinct() {
        let s1 = seal_data("1+1=2");
        let s2 = seal_data("2+2=4");
        assert_ne!(s1, s2, "different data must produce different seals");
    }

    #[test]
    fn test_verify_seal() {
        let data = "fib(10)=55";
        let seal = seal_data(data);
        assert!(verify_seal(data, &seal));
        assert!(!verify_seal("fib(10)=56", &seal));
    }
}
