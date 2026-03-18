/// Blockchain-Verified Calculation History Module
///
/// Every arithmetic operation is recorded in an immutable, hash-chained
/// ledger. This ensures regulatory compliance for your calculator homework
/// and provides a tamper-proof audit trail proving you really did compute
/// 7 * 8 = 56.
///
/// Architecture: In-memory blockchain (not distributed, not decentralized,
/// not on any mainnet — but it IS a blockchain and that's what matters
/// for the README).

use sha2::{Digest, Sha256};
use std::sync::Mutex;
use std::time::{SystemTime, UNIX_EPOCH};

/// A single block in our calculation blockchain.
#[derive(Clone, Debug)]
pub struct Block {
    pub index: u64,
    pub timestamp: u64,
    pub expression: String,
    pub result: String,
    pub previous_hash: String,
    pub hash: String,
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

        Block {
            index,
            timestamp,
            expression: expression.to_string(),
            result: result.to_string(),
            previous_hash: previous_hash.to_string(),
            hash,
        }
    }

    fn genesis() -> Self {
        let timestamp = SystemTime::now()
            .duration_since(UNIX_EPOCH)
            .unwrap_or_default()
            .as_secs();

        let hash = Self::compute_hash(0, timestamp, "GENESIS", "0", "0");

        Block {
            index: 0,
            timestamp,
            expression: "GENESIS".to_string(),
            result: "0".to_string(),
            previous_hash: "0".to_string(),
            hash,
        }
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
        self.chain.push(block);
        hash
    }

    pub fn verify(&self) -> bool {
        for i in 1..self.chain.len() {
            let current = &self.chain[i];
            let previous = &self.chain[i - 1];

            // Verify hash chain linkage
            if current.previous_hash != previous.hash {
                return false;
            }

            // Verify block hash integrity
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

    pub fn get_block_info(&self, index: i32) -> Option<String> {
        self.chain.get(index as usize).map(|block| {
            format!(
                "{{\"index\":{},\"timestamp\":{},\"expression\":\"{}\",\"result\":\"{}\",\"hash\":\"{}\",\"previous_hash\":\"{}\"}}",
                block.index, block.timestamp, block.expression, block.result, block.hash, block.previous_hash
            )
        })
    }
}

// Global blockchain instance — because global mutable state is
// the foundation of all great enterprise architecture.
static BLOCKCHAIN: Mutex<Option<Blockchain>> = Mutex::new(None);

fn with_blockchain<F, R>(f: F) -> R
where
    F: FnOnce(&mut Blockchain) -> R,
{
    let mut guard = BLOCKCHAIN.lock().unwrap();
    if guard.is_none() {
        *guard = Some(Blockchain::new());
    }
    f(guard.as_mut().unwrap())
}

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

/// Proof-of-Work Mining Engine
///
/// Before a calculation result can be displayed to the user, we must first
/// mine a block by finding a nonce where SHA-256(data + nonce) has a leading
/// zero nibble. Average difficulty: ~16 iterations. Peak enterprise.
///
/// Returns (nonce, hash) tuple as a JSON string.
pub fn proof_of_work(data: &str) -> String {
    let mut nonce: u64 = 0;
    loop {
        let input = format!("{}:{}", data, nonce);
        let mut hasher = Sha256::new();
        hasher.update(input.as_bytes());
        let hash = format!("{:x}", hasher.finalize());

        // Difficulty: 1 leading hex zero (4 bits) — we're not monsters
        if hash.starts_with('0') {
            return format!(
                "{{\"nonce\":{},\"hash\":\"{}\",\"iterations\":{}}}",
                nonce, hash, nonce + 1
            );
        }
        nonce += 1;
    }
}
