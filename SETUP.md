# Smart Parking & Automated Payment System

## Complete Setup Guide

### Project Structure
```
project/
├── backend/              # Node.js Express API
│   ├── server.js         # Main server entry
│   ├── database.js       # SQLite database layer
│   ├── blockchain.js     # Blockchain integration
│   ├── middleware/auth.js # JWT authentication
│   ├── routes/           # API route handlers
│   ├── seed.js           # Sample data seeder
│   └── package.json
├── blockchain/           # Smart contracts (Hardhat)
│   ├── contracts/ParkingPayment.sol
│   ├── scripts/deploy.js
│   ├── test/ParkingPayment.test.js
│   └── hardhat.config.js
├── SmartParkingApp/      # Android app (Java)
│   └── app/src/main/
│       ├── java/com/smartparking/
│       │   ├── activities/   # All Activity classes
│       │   ├── adapters/     # RecyclerView adapters
│       │   ├── api/          # Retrofit API client
│       │   └── models/       # Data models
│       └── res/              # Layouts, drawables, values
└── SETUP.md
```

---

## Step 1: Backend Setup

### Prerequisites
- Node.js v18+ installed

### Install & Run
```bash
cd backend
npm install
node seed.js        # Load sample test data
node server.js      # Start API server on port 3000
```

The server runs on `http://localhost:3000`.

### Sample Test Accounts
| Email | Password | Balance | Vehicles |
|-------|----------|---------|----------|
| john@example.com | password123 | $50.00 | ABC1234, XYZ5678 |
| jane@example.com | password123 | $10.00 | DEF9012 |
| bob@example.com | password123 | $1.00 | LOW0001 |

---

## Step 2: Blockchain Setup (Optional)

### Prerequisites
- Node.js v18+

### Setup
```bash
cd blockchain
npm install
```

### Start Local Blockchain (Terminal 1)
```bash
npx hardhat node
```
This starts a local Ethereum node on `http://127.0.0.1:8545` with 20 pre-funded accounts.

### Deploy Smart Contract (Terminal 2)
```bash
npx hardhat run scripts/deploy.js --network localhost
```
This deploys the ParkingPayment contract and saves the address/ABI to the `backend/` directory.

### Run Smart Contract Tests
```bash
npx hardhat test
```

### How Crypto Payments Work
1. Each user is mapped to a Hardhat test account (accounts 1-19)
2. Users deposit ETH to the smart contract via `/api/crypto/deposit`
3. When using crypto payment for parking, the contract's `payForParking()` function is called
4. The contract deducts 0.002 ETH (parking fee) from user's on-chain balance
5. A `ParkingPaid` event is emitted on-chain

---

## Step 3: Android App Setup

### Prerequisites
- Android Studio (latest stable)
- Android SDK 34
- Android Emulator configured

### Setup
1. Open Android Studio
2. Select **File > Open** and navigate to `SmartParkingApp/`
3. Let Gradle sync complete (may take a few minutes)
4. Create/select an emulator (API 24+)
5. Click **Run** (green play button)

### Network Configuration
The app uses `10.0.2.2:3000` to reach the host machine's `localhost:3000` from the emulator. This is Android's built-in alias. No extra configuration needed.

### Important
- Make sure the backend server is running before launching the app
- For crypto payments, the Hardhat node must also be running
- The app uses cleartext HTTP (configured in AndroidManifest.xml)

---

## Step 4: Full System Test

### Test Flow (Wallet Payment)
1. Start backend: `cd backend && node server.js`
2. Run Android app on emulator
3. Login with `john@example.com` / `password123`
4. Go to **Park Now** > Enter `ABC1234` > Select **Wallet** > Tap **Simulate Gate Entry**
5. Result: Gate opens, $2.00 deducted

### Test Flow (Crypto Payment)
1. Start backend: `cd backend && node server.js`
2. Start Hardhat: `cd blockchain && npx hardhat node`
3. Deploy contract: `cd blockchain && npx hardhat run scripts/deploy.js --network localhost`
4. Restart backend (to pick up contract)
5. Deposit ETH via API:
   ```bash
   # Login first
   TOKEN=$(curl -s -X POST http://localhost:3000/api/login \
     -H "Content-Type: application/json" \
     -d '{"email":"john@example.com","password":"password123"}' | node -p "JSON.parse(require('fs').readFileSync(0,'utf8')).token")

   # Deposit 0.01 ETH
   curl -X POST http://localhost:3000/api/crypto/deposit \
     -H "Content-Type: application/json" \
     -H "Authorization: Bearer $TOKEN" \
     -d '{"amount": 0.01}'
   ```
6. In Android app: **Park Now** > Enter plate > Select **Crypto (ETH)** > Simulate Entry

### Test Flow (Insufficient Balance)
1. Login as `bob@example.com` (balance: $1.00, fee: $2.00)
2. Try to simulate entry with plate `LOW0001`
3. Result: Gate stays closed, "Insufficient balance" message

---

## API Documentation

### Authentication
All endpoints except `/api/register`, `/api/login`, and `/api/health` require:
```
Authorization: Bearer <JWT_TOKEN>
```

### Endpoints

#### `POST /api/register`
```json
// Request
{ "name": "John", "email": "john@example.com", "password": "pass123" }
// Response 201
{ "message": "Registration successful", "token": "...", "user": {...} }
```

#### `POST /api/login`
```json
// Request
{ "email": "john@example.com", "password": "pass123" }
// Response 200
{ "message": "Login successful", "token": "...", "user": {...} }
```

#### `GET /api/vehicles`
```json
// Response 200
{ "vehicles": [{ "id": 1, "plate_number": "ABC1234", "created_at": "..." }] }
```

#### `POST /api/vehicles`
```json
// Request
{ "plate_number": "ABC1234" }
// Response 201
{ "message": "Vehicle added successfully", "vehicle": {...} }
```

#### `DELETE /api/vehicles/:id`
```json
// Response 200
{ "message": "Vehicle removed successfully" }
```

#### `GET /api/wallet`
```json
// Response 200
{ "balance": 50.00 }
```

#### `POST /api/wallet/topup`
```json
// Request
{ "amount": 20.00 }
// Response 200
{ "message": "Successfully added $20.00 to wallet", "balance": 70.00 }
```

#### `POST /api/simulate-entry`
```json
// Request
{ "plate_number": "ABC1234", "payment_method": "wallet" }
// Success Response 200
{
  "message": "Gate opened - Wallet payment successful",
  "plate_number": "ABC1234",
  "fee_charged": 2.00,
  "remaining_balance": 48.00,
  "gate": "open",
  "payment_method": "wallet"
}
// Crypto Success Response 200
{
  "message": "Gate opened - Crypto payment successful",
  "plate_number": "ABC1234",
  "fee_charged": "0.002 ETH",
  "remaining_crypto_balance": "0.008 ETH",
  "tx_hash": "0x...",
  "gate": "open",
  "payment_method": "crypto"
}
// Failure Response 402
{ "error": "Insufficient balance", "gate": "closed" }
```

#### `GET /api/transactions/history`
```json
// Response 200
{
  "transactions": [{
    "id": 1, "plate_number": "ABC1234", "amount": 2.00,
    "type": "entry", "status": "success",
    "message": "Gate opened - Wallet payment", "created_at": "..."
  }]
}
```

#### `POST /api/crypto/deposit`
```json
// Request
{ "amount": 0.01 }
// Response 200
{ "message": "Deposited 0.01 ETH to smart contract", "tx_hash": "0x...", "crypto_balance": "0.01 ETH" }
```

#### `GET /api/crypto/balance`
```json
// Response 200
{ "balance": "0.01 ETH", "address": "0x...", "connected": true }
```

#### `GET /api/blockchain/status`
```json
// Response 200
{ "connected": true, "network": "localhost (Hardhat)" }
```

#### `GET /api/health`
```json
// Response 200
{ "status": "ok", "message": "Smart Parking API is running" }
```

---

## Database Schema

### Users
| Column | Type | Description |
|--------|------|-------------|
| id | INTEGER PK | Auto-increment |
| name | TEXT | User's full name |
| email | TEXT UNIQUE | Login email |
| password | TEXT | Bcrypt hashed |
| balance | REAL | Wallet balance |
| created_at | DATETIME | Registration date |

### Vehicles
| Column | Type | Description |
|--------|------|-------------|
| id | INTEGER PK | Auto-increment |
| user_id | INTEGER FK | Owner reference |
| plate_number | TEXT UNIQUE | License plate |
| created_at | DATETIME | Registration date |

### Transactions
| Column | Type | Description |
|--------|------|-------------|
| id | INTEGER PK | Auto-increment |
| user_id | INTEGER FK | User reference |
| vehicle_id | INTEGER FK | Vehicle reference |
| plate_number | TEXT | Plate number |
| amount | REAL | Fee amount |
| type | TEXT | 'entry' or 'topup' |
| status | TEXT | 'success' or 'failed' |
| message | TEXT | Description |
| created_at | DATETIME | Transaction date |

---

## Smart Contract (ParkingPayment.sol)

### Functions
| Function | Description |
|----------|-------------|
| `deposit()` | Deposit ETH to user's on-chain balance (payable) |
| `payForParking(string plateNumber)` | Deduct parking fee from balance |
| `getBalance(address user)` | View user's deposited balance |
| `setParkingFee(uint256 newFee)` | Owner-only: update fee |
| `withdraw()` | Owner-only: withdraw collected fees |

### Events
| Event | Parameters |
|-------|-----------|
| `Deposited` | user address, amount |
| `ParkingPaid` | user address, amount, plateNumber |
| `Withdrawn` | to address, amount |

---

## Technology Stack
- **Android**: Java, Retrofit, RecyclerView, Material Design
- **Backend**: Node.js, Express.js, JWT, SQLite (sql.js)
- **Blockchain**: Solidity 0.8.19, Hardhat, ethers.js
- **IoT Simulation**: Backend endpoint simulating gate controller
