/// Blockchain-Verified Calculation History Module
///
/// Every arithmetic operation is recorded in an immutable, hash-chained
/// ledger sealed with post-quantum LWE cryptography.
///
/// Architecture: In-memory blockchain backed by an append-only flat file,
/// so your calculations survive app restarts. Not distributed. Not on any
/// mainnet. But persistent, post-quantum secured, and more over-engineered
/// than any calculator has the right to be.
///
/// Persistence format: one block per line, pipe-delimited:
///   index|timestamp|expression|result|previous_hash|hash|pq_seal

use crate::pq_crypto;
use crate::zkp;
use sha2::{Digest, Sha256};
use std::fs;
use std::io::Write;
use std::sync::Mutex;
use std::time::{SystemTime, UNIX_EPOCH};

/// A single block in our calculation blockchain.
/// Carries a post-quantum LWE seal AND a Schnorr zero-knowledge proof,
/// because one exotic cryptographic primitive per block is amateur hour.
#[derive(Clone, Debug)]
pub struct Block {
    pub index: u64,
    pub timestamp: u64,
    pub expression: String,
    pub result: String,
    pub previous_hash: String,
    pub hash: String,
    pub pq_seal: String,
    pub zkp_proof: String,
}

impl Block {
    fn compute_hash(
        index: u64,
        timestamp: u64,
        expression: &str,
        result: &str,
        previous_hash: &str,
    ) -> String {
        let input = format!(
            "{}:{}:{}:{}:{}",
            index, timestamp, expression, result, previous_hash
        );
        let mut hasher = Sha256::new();
        hasher.update(input.as_bytes());
        format!("{:x}", hasher.finalize())
    }

    fn new(index: u64, expression: &str, result: &str, previous_hash: &str) -> Self {
        let timestamp = SystemTime::now()
            .duration_since(UNIX_EPOCH)
            .unwrap_or_default()
            .as_secs();

        let hash = Self::compute_hash(index, timestamp, expression, result, previous_hash);

        // PQ seal covers expression, result, and block hash — tamper-evident
        let pq_seal = pq_crypto::seal_data(&format!("{}={}:{}", expression, result, hash));

        // ZKP: Schnorr proof of knowledge of the computation
        let zkp_proof = zkp::generate_proof(expression, result);

        Block {
            index,
            timestamp,
            expression: expression.to_string(),
            result: result.to_string(),
            previous_hash: previous_hash.to_string(),
            hash,
            pq_seal,
            zkp_proof,
        }
    }

    fn genesis() -> Self {
        let timestamp = SystemTime::now()
            .duration_since(UNIX_EPOCH)
            .unwrap_or_default()
            .as_secs();

        let hash = Self::compute_hash(0, timestamp, "GENESIS", "0", "0");
        let pq_seal = pq_crypto::seal_data(&format!("GENESIS=0:{}", hash));
        let zkp_proof = zkp::generate_proof("GENESIS", "0");

        Block {
            index: 0,
            timestamp,
            expression: "GENESIS".to_string(),
            result: "0".to_string(),
            previous_hash: "0".to_string(),
            hash,
            pq_seal,
            zkp_proof,
        }
    }

    /// Serialise to the flat-file persistence format.
    /// Format v2: index|timestamp|expression|result|previous_hash|hash|pq_seal|zkp_proof
    fn to_line(&self) -> String {
        format!(
            "{}|{}|{}|{}|{}|{}|{}|{}",
            self.index,
            self.timestamp,
            self.expression,
            self.result,
            self.previous_hash,
            self.hash,
            self.pq_seal,
            self.zkp_proof
        )
    }

    /// Deserialise from the flat-file persistence format.
    /// Handles both v1 (7 fields) and v2 (8 fields, with ZKP proof).
    fn from_line(line: &str) -> Option<Block> {
        let parts: Vec<&str> = line.splitn(8, '|').collect();
        if parts.len() < 7 {
            return None;
        }
        Some(Block {
            index: parts[0].parse().ok()?,
            timestamp: parts[1].parse().ok()?,
            expression: parts[2].to_string(),
            result: parts[3].to_string(),
            previous_hash: parts[4].to_string(),
            hash: parts[5].to_string(),
            pq_seal: parts[6].to_string(),
            zkp_proof: parts.get(7).unwrap_or(&"").to_string(),
        })
    }
}

/// The blockchain itself — a Vec<Block> behind a Mutex because
/// thread safety is non-negotiable in enterprise calculator software.
pub struct Blockchain {
    chain: Vec<Block>,
}

impl Blockchain {
    fn new() -> Self {
        Blockchain {
            chain: vec![Block::genesis()],
        }
    }

    pub fn record(&mut self, expression: &str, result: &str) -> String {
        let prev_hash = self.chain.last().unwrap().hash.clone();
        let index = self.chain.len() as u64;
        let block = Block::new(index, expression, result, &prev_hash);
        let hash = block.hash.clone();
        append_block_to_file(&block);
        self.chain.push(block);
        hash
    }

    pub fn verify(&self) -> bool {
        for i in 1..self.chain.len() {
            let current = &self.chain[i];
            let previous = &self.chain[i - 1];

            if current.previous_hash != previous.hash {
                return false;
            }

            let expected_hash = Block::compute_hash(
                current.index,
                current.timestamp,
                &current.expression,
                &current.result,
                &current.previous_hash,
            );
            if current.hash != expected_hash {
                return false;
            }
        }
        true
    }

    pub fn len(&self) -> i32 {
        self.chain.len() as i32
    }

    pub fn verify_block_detailed(&self, index: i32) -> Option<String> {
        let idx = index as usize;
        let block = self.chain.get(idx)?;

        // 1. Hash valid — recompute and compare
        let expected_hash = Block::compute_hash(
            block.index,
            block.timestamp,
            &block.expression,
            &block.result,
            &block.previous_hash,
        );
        let hash_valid = block.hash == expected_hash;

        // 2. Chain link valid — prev_hash matches previous block's hash
        let chain_link_valid = if idx == 0 {
            block.previous_hash == "0"
        } else {
            block.previous_hash == self.chain[idx - 1].hash
        };

        // 3. PQ seal valid — re-seal and compare
        let seal_data = format!("{}={}:{}", block.expression, block.result, block.hash);
        let pq_seal_valid = pq_crypto::verify_seal(&seal_data, &block.pq_seal);

        // 4. ZKP valid — verify Schnorr proof (guard empty proof for v1 blocks)
        let zkp_valid = if block.zkp_proof.is_empty() {
            true // v1 blocks have no proof — pass by default
        } else {
            zkp::verify_proof(&block.expression, &block.result, &block.zkp_proof)
        };

        Some(format!(
            "{{\"hash_valid\":{},\"chain_link_valid\":{},\"pq_seal_valid\":{},\"zkp_valid\":{}}}",
            hash_valid, chain_link_valid, pq_seal_valid, zkp_valid
        ))
    }

    pub fn get_block_info(&self, index: i32) -> Option<String> {
        self.chain.get(index as usize).map(|block| {
            format!(
                "{{\"index\":{},\"timestamp\":{},\"expression\":\"{}\",\"result\":\"{}\",\
                \"hash\":\"{}\",\"previous_hash\":\"{}\",\"pq_seal\":\"{}\",\"zkp_proof\":\"{}\"}}",
                block.index,
                block.timestamp,
                block.expression,
                block.result,
                block.hash,
                block.previous_hash,
                block.pq_seal,
                block.zkp_proof
            )
        })
    }
}

// ── Persistence ───────────────────────────────────────────────────────────────

/// Path to the flat-file blockchain store, set once by initStorage.
static STORAGE_PATH: Mutex<Option<String>> = Mutex::new(None);

/// Set the directory where the blockchain file will be persisted.
/// Must be called before any blockchain operations (at app start).
pub fn init_storage(files_dir: &str) {
    let path = format!("{}/blockchain.dat", files_dir);
    let mut guard = STORAGE_PATH.lock().unwrap();
    *guard = Some(path);
}

/// Append a single block line to the persistence file.
fn append_block_to_file(block: &Block) {
    let path = {
        let guard = STORAGE_PATH.lock().unwrap();
        guard.clone()
    };
    if let Some(ref path) = path {
        if let Ok(mut file) = fs::OpenOptions::new()
            .create(true)
            .append(true)
            .open(path)
        {
            let _ = writeln!(file, "{}", block.to_line());
        }
    }
}

/// Load the chain from file, or create a fresh one (with genesis) if absent.
fn load_or_create() -> Blockchain {
    let path = {
        let guard = STORAGE_PATH.lock().unwrap();
        guard.clone()
    };

    if let Some(ref path) = path {
        if let Ok(content) = fs::read_to_string(path) {
            let blocks: Vec<Block> = content
                .lines()
                .filter(|l| !l.is_empty())
                .filter_map(Block::from_line)
                .collect();
            if !blocks.is_empty() {
                return Blockchain { chain: blocks };
            }
        }
    }

    // No file or empty — create fresh chain and persist the genesis block
    let bc = Blockchain::new();
    append_block_to_file(&bc.chain[0]);
    bc
}

// ── Global state ──────────────────────────────────────────────────────────────

/// Global blockchain instance — because global mutable state is
/// the foundation of all great enterprise architecture.
static BLOCKCHAIN: Mutex<Option<Blockchain>> = Mutex::new(None);

fn with_blockchain<F, R>(f: F) -> R
where
    F: FnOnce(&mut Blockchain) -> R,
{
    let mut guard = BLOCKCHAIN.lock().unwrap();
    if guard.is_none() {
        *guard = Some(load_or_create());
    }
    f(guard.as_mut().unwrap())
}

// ── Public API ────────────────────────────────────────────────────────────────

pub fn record_calculation(expression: &str, result: &str) -> String {
    with_blockchain(|bc| bc.record(expression, result))
}

pub fn verify_chain() -> bool {
    with_blockchain(|bc| bc.verify())
}

pub fn get_chain_length() -> i32 {
    with_blockchain(|bc| bc.len())
}

pub fn get_block_info(index: i32) -> String {
    with_blockchain(|bc| bc.get_block_info(index).unwrap_or_default())
}

pub fn verify_block_detailed(index: i32) -> String {
    with_blockchain(|bc| bc.verify_block_detailed(index).unwrap_or_default())
}

/// Proof-of-Work Mining Engine
///
/// Before a calculation result can be displayed to the user, we must first
/// mine a block by finding a nonce where SHA-256(data + nonce) has a leading
/// zero nibble. Average difficulty: ~16 iterations. Peak enterprise.
pub fn proof_of_work(data: &str) -> String {
    let mut nonce: u64 = 0;
    loop {
        let input = format!("{}:{}", data, nonce);
        let mut hasher = Sha256::new();
        hasher.update(input.as_bytes());
        let hash = format!("{:x}", hasher.finalize());

        if hash.starts_with('0') {
            return format!(
                "{{\"nonce\":{},\"hash\":\"{}\",\"iterations\":{}}}",
                nonce,
                hash,
                nonce + 1
            );
        }
        nonce += 1;
    }
}
