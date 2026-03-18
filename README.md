# CalcuLux Enterprise Edition Pro Max Ultra

### *The World's Most Overengineered Calculator*

> "We didn't ask if we should. We asked if we could. Then we did it anyway." — Lead Architect

---

<p align="center">
  <img src="genie.jpg" width="300" alt="The Lead Architect, moments after deploying 30 CalBots to production"/>
  <br/>
  <em>Our Lead Architect contemplating whether 30 CalBots is really enough</em>
</p>

---

## What Is This?

A simple calculator app for INF2007. But we didn't stop there.

What began as "implement addition" has evolved into a **distributed, blockchain-verified, AI-enhanced, JNI-accelerated, quantum-entangled arithmetic platform** featuring 30 autonomous CalBot instances, a Room database-backed immutable history ledger, and a native C++ Fibonacci computation engine because Kotlin was simply too slow for adding two numbers together.

## Enterprise Architecture Overview

```
                            ┌─────────────────────────────┐
                            │    CalcuLux API Gateway      │
                            │  (Jetpack Compose Frontend)  │
                            └──────────┬──────────────────┘
                                       │
                    ┌──────────────────┼──────────────────┐
                    │                  │                  │
            ┌───────▼──────┐  ┌───────▼──────┐  ┌───────▼──────┐
            │  Addition     │  │ Subtraction  │  │Multiplication│
            │  Microservice │  │ Microservice │  │ Microservice │
            │  (Hilt DI)    │  │  (Hilt DI)   │  │  (Hilt DI)   │
            └───────┬──────┘  └───────┬──────┘  └───────┬──────┘
                    │                  │                  │
                    └──────────────────┼──────────────────┘
                                       │
                            ┌──────────▼──────────────────┐
                            │   CalBot Orchestration Layer  │
                            │     (30 Autonomous Nodes)     │
                            │   "Kubernetes at Home" (tm)   │
                            └──────────┬──────────────────┘
                                       │
                    ┌──────────────────┼──────────────────┐
                    │                  │                  │
            ┌───────▼──────┐  ┌───────▼──────┐  ┌───────▼──────┐
            │  Room DB      │  │ MathJS API   │  │ C++ Native   │
            │  Blockchain   │  │ Redundancy   │  │ Fibonacci    │
            │  Ledger       │  │ Fallback     │  │ Engine (JNI) │
            └──────────────┘  └──────────────┘  └──────────────┘
```

## Features

### Core Arithmetic Engine
- **Addition** — Enterprise-grade summation with Hilt dependency injection
- **Subtraction** — Negative number support (patent pending)
- **Multiplication** — O(1) complexity achieved through the revolutionary `*` operator
- **Division** — With artisanal, hand-crafted divide-by-zero protection

### 30 CalBot Autonomous Computing Nodes
Each CalBot is a fully independent calculator instance with its own:
- Persistent state management via Room database
- Individual calculation history (blockchain-verified, see below)
- Unique personality (CalBot 7 prefers multiplication, CalBot 23 is going through a division phase)

When CalBot 7 computes 2+2, CalBot 13 already knows the answer through **quantum-entangled state synchronization**. We can't explain how this works. Neither can our physicists.

### Native C++ Fibonacci Engine (JNI)
Pure Kotlin cannot achieve the raw computational throughput required for enterprise-grade Fibonacci sequences. Our C++ implementation uses **O(log n) matrix exponentiation** — deploying linear algebra to compute what is fundamentally a sequence of additions.

**Technical specs:**
- Crosses 3 language boundaries per computation (Kotlin → JNI → C++)
- Uses 2x2 matrix multiplication for a problem solvable by a for loop
- The JNI overhead is approximately 10,000x the actual computation time for typical inputs
- Built with CMake because we needed a build system for our build system

```cpp
// The crown jewel of our computational infrastructure
Matrix2x2 result = matpow(fib_matrix, n - 1);
// ^ This line alone justifies the entire NDK dependency
```

### MathJS API Integration
Why compute locally when you can add network latency? Our MathJS API toggle lets you:
- Send `2 + 2` over HTTPS to a server in another continent
- Wait 200ms for a response you could have computed in 2 nanoseconds
- Experience the thrill of your calculator failing because of DNS resolution

### Blockchain-Verified Calculation History
Every calculation is cryptographically hashed and chained to the previous entry in our Room database. This provides:
- **Immutable audit trail** for regulatory compliance (in case the SEC audits your homework)
- **Tamper-proof records** proving you really did compute 7 × 8 = 56
- **Enterprise governance** — CalBot 14 cannot retroactively claim it computed 7 × 8 = 54

### AI-Powered Operator Prediction
Our proprietary machine learning model analyzes the two numbers you've entered and predicts which operator you want:
- Enter `100` and `0`? The AI knows you want division (and pre-emptively shows the error screen)
- Enter `1` and `1`? Obviously addition. Unless you're CalBot 29, who always chooses subtraction out of spite

## Tech Stack

| Layer | Technology | Justification |
|-------|-----------|---------------|
| UI | Jetpack Compose + Material 3 | Because XML layouts are so 2019 |
| Navigation | Navigation 3 | Living on the bleeding edge |
| DI | Hilt/Dagger | For injecting dependencies into a calculator |
| Database | Room | To persist the permanent record of 2+2=4 |
| Paging | Paging 3 | In case you compute so many things you need infinite scroll |
| Serialization | Kotlin Serialization | For serializing integers (they're complex objects now) |
| Preferences | DataStore | SharedPreferences was too simple |
| Network | Retrofit + Gson | For outsourcing arithmetic to the cloud |
| Native | C++ via JNI/NDK | For Fibonacci. Just Fibonacci. |
| Build | Gradle + CMake | Two build systems are better than one |

## System Requirements

- Android 11+ (API 30)
- 30 CalBots (non-negotiable)
- NDK 28.x for the C++ Fibonacci Engine
- An internet connection (for when you don't trust local arithmetic)
- A deep appreciation for enterprise software architecture

## Building

```bash
./gradlew assembleDebug
```

This compiles Kotlin, C++, and your hopes and dreams into a single APK.

## Testing

```bash
cd test && ./test.sh
```

> "The test depends on timeout, it may not always give you correct result."
> — Actual documentation from this project. This is not satire. This is real.

## Performance Benchmarks

| Operation | CalcuLux | iPhone Calculator | Notes |
|-----------|----------|-------------------|-------|
| 2 + 2 | 847ms | 0.001ms | Includes JNI bridge, Room write, blockchain hash, API call |
| fib(10) | 2.3ms | N/A | Matrix exponentiation in C++ via JNI, absolutely worth it |
| fib(1) | 0.001ms | N/A | Handled in Kotlin because even we have limits |
| Division by 0 | ∞ | Error | Our CalBots are still working on it |

## Roadmap

- [ ] Kubernetes deployment for CalBot horizontal auto-scaling
- [ ] GraphQL API for querying calculation history with field-level granularity
- [ ] Machine learning model for predicting your next calculation before you think of it
- [ ] WebAssembly port of the C++ Fibonacci engine for cross-platform number computation
- [ ] CalBot-to-CalBot P2P gossip protocol for consensus on shared state
- [ ] NFT receipts for each calculation
- [ ] OAuth2 + PKCE for CalBot authentication
- [ ] Terraform modules for reproducible CalBot infrastructure
- [ ] CalBot 31 (we're in discussions)

---

## Support Development

If this project has enriched your understanding of what a calculator can become, consider supporting the continued overengineering of basic arithmetic:

<p align="center">
  <img src="paynow.jpg" width="250" alt="PayNow QR"/>
  <br/>
  <em>Every donation funds another CalBot</em>
</p>

---

<p align="center">
  <strong>CalcuLux Enterprise Edition Pro Max Ultra</strong><br/>
  <em>Because sometimes 2+2 needs a 50MB APK.</em><br/><br/>
  Made with an mass_of_electron amount of seriousness for INF2007 @ SIT
</p>
