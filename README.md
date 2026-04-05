# Smart Parking & Automated Payment System

A complete smart parking system that simulates vehicle detection, verifies registration, processes payments automatically, and controls gate access. Built with an Android mobile app, Node.js backend API, SQLite database, and Solidity smart contracts for optional cryptocurrency payments.

---

## Table of Contents

- [Overview](#overview)
- [System Architecture](#system-architecture)
- [Features](#features)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Setup & Installation](#setup--installation)
- [API Documentation](#api-documentation)
- [Database Schema](#database-schema)
- [Smart Contract](#smart-contract)
- [Testing the System](#testing-the-system)
- [Contributors](#contributors)

---

## Overview

Traditional parking systems rely on manual ticketing and cash payments, leading to long queues and inefficiency. This project solves that by building a **software-simulated smart parking system** where:

1. A vehicle approaches the gate (simulated by entering a number plate)
2. The system verifies the plate is registered
3. The parking fee is automatically deducted from the user's wallet or crypto balance
4. The gate opens (or denies entry with a clear reason)
5. Every transaction is logged for history and auditing

The system is fully functional on a local environment — Android emulator/phone + localhost backend.

---

## System Architecture

```
┌──────────────────┐         HTTP/JSON          ┌──────────────────┐
│                  │  ◄───────────────────────►  │                  │
│   Android App    │      REST API + JWT         │  Node.js Backend │
│   (Java)         │                             │  (Express.js)    │
│                  │                             │                  │
└──────────────────┘                             └────────┬─────────┘
                                                          │
                                          ┌───────────────┼───────────────┐
                                          │               │               │
                                          ▼               ▼               ▼
                                    ┌──────────┐   ┌──────────┐   ┌──────────────┐
                                    │  SQLite   │   │   JWT    │   │  Blockchain   │
                                    │ Database  │   │  Auth    │   │  (Hardhat)    │
                                    └──────────┘   └──────────┘   └──────────────┘
```

---

## Features

### Mobile App (Android - Java)
- **User Authentication** — Register and login with email/password
- **Vehicle Management** — Add, view, and delete registered vehicles
- **Wallet System** — View balance and add funds (simulated top-up)
- **Parking Simulation** — Enter a plate number, choose payment method, and simulate gate entry
- **Transaction History** — View all past entries and payments with status indicators
- **Loading Indicators** — Progress bars during API calls
- **Error Handling** — Clear success/failure messages with detailed reasons

### Backend API (Node.js + Express)
- **RESTful API** with 11 endpoints
- **JWT Token Authentication** for secure access
- **Input Validation** on all endpoints
- **Duplicate Entry Prevention** — blocks rapid re-entries within 60 seconds
- **Atomic Transactions** — balance deduction and transaction logging in a single DB transaction
- **Graceful Blockchain Fallback** — crypto features disabled cleanly if Hardhat isn't running

### Blockchain (Solidity + Hardhat)
- **ParkingPayment Smart Contract** — on-chain deposit, payment, and balance tracking
- **Event Emission** — `Deposited`, `ParkingPaid`, `Withdrawn` events for transparency
- **Owner Controls** — fee adjustment and fund withdrawal
- **Full Test Suite** — unit tests for all contract functions

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| **Mobile App** | Java, Android SDK 34, Retrofit 2, OkHttp, Gson, Material Design, RecyclerView |
| **Backend** | Node.js, Express.js, JWT (jsonwebtoken), bcrypt.js, sql.js (SQLite), ethers.js |
| **Blockchain** | Solidity 0.8.19, Hardhat, ethers.js |
| **Database** | SQLite (file-based, zero config) |
| **Authentication** | JSON Web Tokens (JWT), bcrypt password hashing |

---

## Project Structure

```
project/
├── backend/                    # Node.js Express API
│   ├── server.js               # Main server entry point
│   ├── database.js             # SQLite database initialization & schema
│   ├── blockchain.js           # Blockchain integration (ethers.js)
│   ├── seed.js                 # Sample data seeder (test users & vehicles)
│   ├── middleware/
│   │   └── auth.js             # JWT authentication middleware
│   ├── routes/
│   │   ├── auth.js             # /register, /login
│   │   ├── vehicles.js         # /vehicles (CRUD)
│   │   ├── wallet.js           # /wallet, /wallet/topup
│   │   └── parking.js          # /simulate-entry, /transactions, /crypto
│   └── package.json
│
├── blockchain/                 # Smart contracts (Hardhat)
│   ├── contracts/
│   │   └── ParkingPayment.sol  # Parking payment smart contract
│   ├── scripts/
│   │   └── deploy.js           # Deployment script
│   ├── test/
│   │   └── ParkingPayment.test.js
│   ├── hardhat.config.js
│   └── package.json
│
├── SmartParkingApp/            # Android app (Java)
│   ├── app/src/main/
│   │   ├── AndroidManifest.xml
│   │   ├── java/com/smartparking/
│   │   │   ├── activities/     # UI screens (Login, Register, Main, etc.)
│   │   │   ├── adapters/       # RecyclerView adapters
│   │   │   ├── api/            # Retrofit API client & response models
│   │   │   └── models/         # Data models (User, Vehicle, Transaction)
│   │   └── res/                # Layouts, drawables, colors, strings
│   ├── build.gradle
│   └── settings.gradle
│
└── README.md
```

---

## Setup & Installation

### Prerequisites

- **Node.js** v18 or higher
- **Android Studio** (latest stable version)
- **Android SDK 34** and an emulator (API 24+) or physical device
- **Git**

---

### 1. Clone the Repository

```bash
git clone https://github.com/meshtirop1/mobile_payment_android.git
cd mobile_payment_android
```

### 2. Backend Setup

```bash
cd backend
npm install
node seed.js          # Creates database with sample test data
node server.js        # Starts API server on http://localhost:3000
```

You should see:
```
Smart Parking API running on http://0.0.0.0:3000
```

### 3. Blockchain Setup (Optional)

Only needed if you want to test cryptocurrency payments.

**Terminal 1** — Start local blockchain:
```bash
cd blockchain
npm install
npx hardhat node
```

**Terminal 2** — Deploy the smart contract:
```bash
cd blockchain
npx hardhat run scripts/deploy.js --network localhost
```

Then restart the backend so it detects the deployed contract.

### 4. Android App Setup

1. Open Android Studio
2. **File > Open** → select the `SmartParkingApp/` folder
3. Wait for Gradle sync to complete
4. Select your emulator or connect a physical device
5. Click **Run** (green play button)

#### Network Configuration

| Device Type | API Base URL |
|-------------|-------------|
| Android Emulator | `http://10.0.2.2:3000/` (auto-configured) |
| Physical Device | Change `BASE_URL` in `ApiClient.java` to your PC's local IP (e.g., `http://192.168.1.x:3000/`) |

> **Note for physical devices:** Your phone and PC must be on the **same Wi-Fi network**, and you may need to allow port 3000 through Windows Firewall.

---

## API Documentation

### Authentication

All endpoints except `/api/register`, `/api/login`, and `/api/health` require a JWT token:
```
Authorization: Bearer <token>
```

### Endpoints

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| `POST` | `/api/register` | Create a new user account | No |
| `POST` | `/api/login` | Login and receive JWT token | No |
| `GET` | `/api/vehicles` | List user's registered vehicles | Yes |
| `POST` | `/api/vehicles` | Register a new vehicle | Yes |
| `DELETE` | `/api/vehicles/:id` | Remove a vehicle | Yes |
| `GET` | `/api/wallet` | Get wallet balance | Yes |
| `POST` | `/api/wallet/topup` | Add funds to wallet | Yes |
| `POST` | `/api/simulate-entry` | Simulate parking gate entry | Yes |
| `GET` | `/api/transactions/history` | Get transaction history | Yes |
| `POST` | `/api/crypto/deposit` | Deposit ETH to smart contract | Yes |
| `GET` | `/api/crypto/balance` | Get crypto balance | Yes |
| `GET` | `/api/blockchain/status` | Check blockchain connection | Yes |
| `GET` | `/api/health` | Health check | No |

### Key Request/Response Examples

**Login:**
```json
// POST /api/login
{ "email": "john@example.com", "password": "password123" }

// Response 200
{ "message": "Login successful", "token": "eyJhbG...", "user": { "id": 1, "name": "John Doe", "email": "john@example.com", "balance": 50 } }
```

**Simulate Entry (Success):**
```json
// POST /api/simulate-entry
{ "plate_number": "ABC1234", "payment_method": "wallet" }

// Response 200
{ "message": "Gate opened - Wallet payment successful", "plate_number": "ABC1234", "fee_charged": 2.00, "remaining_balance": 48.00, "gate": "open" }
```

**Simulate Entry (Failure):**
```json
// Response 402
{ "error": "Insufficient wallet balance", "required": 2.00, "current_balance": 1.00, "gate": "closed" }
```

---

## Database Schema

### Users Table
| Column | Type | Description |
|--------|------|-------------|
| id | INTEGER (PK) | Auto-increment |
| name | TEXT | Full name |
| email | TEXT (UNIQUE) | Login email |
| password | TEXT | Bcrypt hashed password |
| balance | REAL | Wallet balance (default: 0.00) |
| created_at | DATETIME | Registration timestamp |

### Vehicles Table
| Column | Type | Description |
|--------|------|-------------|
| id | INTEGER (PK) | Auto-increment |
| user_id | INTEGER (FK) | References users.id |
| plate_number | TEXT (UNIQUE) | License plate number |
| created_at | DATETIME | Registration timestamp |

### Transactions Table
| Column | Type | Description |
|--------|------|-------------|
| id | INTEGER (PK) | Auto-increment |
| user_id | INTEGER (FK) | References users.id |
| vehicle_id | INTEGER (FK) | References vehicles.id |
| plate_number | TEXT | Plate number used |
| amount | REAL | Fee amount |
| type | TEXT | `entry` or `topup` |
| status | TEXT | `success` or `failed` |
| message | TEXT | Description of outcome |
| created_at | DATETIME | Transaction timestamp |

---

## Smart Contract

### ParkingPayment.sol

Deployed on a local Hardhat network. Handles on-chain parking payments as an alternative to wallet-based payments.

| Function | Description |
|----------|-------------|
| `deposit()` | Deposit ETH to user's on-chain balance (payable) |
| `payForParking(string plateNumber)` | Deduct 0.002 ETH parking fee from balance |
| `getBalance(address user)` | View user's deposited balance |
| `setParkingFee(uint256 newFee)` | Update the parking fee (owner only) |
| `withdraw()` | Withdraw collected fees (owner only) |

### Running Contract Tests

```bash
cd blockchain
npx hardhat test
```

---

## Testing the System

### Sample Test Accounts

Pre-loaded via `node seed.js`:

| Email | Password | Balance | Vehicles |
|-------|----------|---------|----------|
| john@example.com | password123 | $50.00 | ABC1234, XYZ5678 |
| jane@example.com | password123 | $10.00 | DEF9012 |
| bob@example.com | password123 | $1.00 | LOW0001 |

### Test Scenarios

| Scenario | Steps | Expected Result |
|----------|-------|-----------------|
| Successful entry | Login as John → Park Now → Enter `ABC1234` → Wallet → Simulate | Gate opens, $2.00 deducted |
| Insufficient balance | Login as Bob → Park Now → Enter `LOW0001` → Wallet → Simulate | Gate closed, "Insufficient balance" |
| Unregistered plate | Login as John → Park Now → Enter `FAKE999` → Simulate | Gate closed, "Vehicle not registered" |
| Wrong owner | Login as Jane → Park Now → Enter `ABC1234` → Simulate | Gate closed, "Not registered to your account" |
| Duplicate entry | Simulate same plate twice within 60 seconds | Second attempt blocked |

---

## Contributors

| Name | GitHub | Role |
|------|--------|------|
| Meshack | [@meshtirop1](https://github.com/meshtirop1) | Project Lead / Full-Stack Developer |
| Ajax | [@Ajax120](https://github.com/Ajax120) | Developer |
| Grace | [@GraceDhieu](https://github.com/GraceDhieu) | Developer |
| Lauria | [@lauria-gl](https://github.com/lauria-gl) | Developer |
| Deborah | [@Umwari-dev](https://github.com/Umwari-dev) | Developer |

---

## License

This project was built as part of a Mobile Programming class project.
