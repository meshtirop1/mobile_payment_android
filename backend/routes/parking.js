const express = require('express');
const { getDb } = require('../database');
const { authenticateToken } = require('../middleware/auth');
const blockchain = require('../blockchain');

const router = express.Router();

const PARKING_FEE = 2.00;
const DUPLICATE_ENTRY_WINDOW_SECONDS = 60;

// POST /api/simulate-entry
router.post('/simulate-entry', authenticateToken, (req, res) => {
  const db = getDb();
  const { plate_number, payment_method } = req.body;
  const method = payment_method || 'wallet'; // default to wallet

  if (!plate_number) {
    return res.status(400).json({ error: 'Plate number is required', gate: 'closed' });
  }

  const normalized = plate_number.trim().toUpperCase();

  // 1. Check if plate exists
  const vehicle = db.prepare(
    'SELECT v.id, v.user_id, v.plate_number, u.balance, u.name FROM vehicles v JOIN users u ON v.user_id = u.id WHERE v.plate_number = ?'
  ).get(normalized);

  if (!vehicle) {
    return res.status(404).json({
      error: 'Vehicle not registered in the system',
      plate_number: normalized,
      gate: 'closed'
    });
  }

  if (vehicle.user_id !== req.user.id) {
    return res.status(403).json({
      error: 'This vehicle is not registered to your account',
      plate_number: normalized,
      gate: 'closed'
    });
  }

  // 2. Check for duplicate rapid entries
  const recentEntry = db.prepare(
    `SELECT id FROM transactions
     WHERE vehicle_id = ? AND type = 'entry' AND status = 'success'
     AND created_at > datetime('now', '-${DUPLICATE_ENTRY_WINDOW_SECONDS} seconds')`
  ).get(vehicle.id);

  if (recentEntry) {
    return res.status(429).json({
      error: `Entry already recorded within the last ${DUPLICATE_ENTRY_WINDOW_SECONDS} seconds`,
      plate_number: normalized,
      gate: 'closed'
    });
  }

  // 3. Process payment based on method
  if (method === 'crypto') {
    handleCryptoPayment(req, res, db, vehicle, normalized);
  } else {
    handleWalletPayment(req, res, db, vehicle, normalized);
  }
});

function handleWalletPayment(req, res, db, vehicle, normalized) {
  // Check balance
  if (vehicle.balance < PARKING_FEE) {
    db.prepare(
      `INSERT INTO transactions (user_id, vehicle_id, plate_number, amount, type, status, message)
       VALUES (?, ?, ?, ?, 'entry', 'failed', ?)`
    ).run(vehicle.user_id, vehicle.id, normalized, PARKING_FEE, 'Insufficient wallet balance');

    return res.status(402).json({
      error: 'Insufficient wallet balance',
      required: PARKING_FEE,
      current_balance: vehicle.balance,
      plate_number: normalized,
      gate: 'closed',
      payment_method: 'wallet'
    });
  }

  // Deduct fee and record transaction
  const deductAndRecord = db.transaction(() => {
    db.prepare('UPDATE users SET balance = balance - ? WHERE id = ?').run(PARKING_FEE, vehicle.user_id);

    db.prepare(
      `INSERT INTO transactions (user_id, vehicle_id, plate_number, amount, type, status, message)
       VALUES (?, ?, ?, ?, 'entry', 'success', 'Gate opened - Wallet payment')`
    ).run(vehicle.user_id, vehicle.id, normalized, PARKING_FEE);

    return db.prepare('SELECT balance FROM users WHERE id = ?').get(vehicle.user_id);
  });

  const updatedUser = deductAndRecord();

  res.json({
    message: 'Gate opened - Wallet payment successful',
    plate_number: normalized,
    fee_charged: PARKING_FEE,
    remaining_balance: updatedUser.balance,
    gate: 'open',
    payment_method: 'wallet'
  });
}

async function handleCryptoPayment(req, res, db, vehicle, normalized) {
  if (!blockchain.isBlockchainConnected()) {
    return res.status(503).json({
      error: 'Blockchain not available. Please use wallet payment or start local blockchain.',
      plate_number: normalized,
      gate: 'closed',
      payment_method: 'crypto'
    });
  }

  try {
    // Use user_id as account index (Hardhat provides 20 accounts)
    const accountIndex = ((vehicle.user_id - 1) % 19) + 1; // accounts 1-19 for users

    const result = await blockchain.payForParkingCrypto(accountIndex, normalized);

    // Record transaction in DB
    db.prepare(
      `INSERT INTO transactions (user_id, vehicle_id, plate_number, amount, type, status, message)
       VALUES (?, ?, ?, ?, 'entry', 'success', ?)`
    ).run(vehicle.user_id, vehicle.id, normalized, PARKING_FEE,
      `Gate opened - Crypto payment (TX: ${result.txHash.substring(0, 10)}...)`);

    const cryptoBalance = await blockchain.getCryptoBalance(accountIndex);

    res.json({
      message: 'Gate opened - Crypto payment successful',
      plate_number: normalized,
      fee_charged: result.fee + ' ETH',
      remaining_crypto_balance: cryptoBalance + ' ETH',
      tx_hash: result.txHash,
      block_number: result.blockNumber,
      gate: 'open',
      payment_method: 'crypto'
    });
  } catch (err) {
    db.prepare(
      `INSERT INTO transactions (user_id, vehicle_id, plate_number, amount, type, status, message)
       VALUES (?, ?, ?, ?, 'entry', 'failed', ?)`
    ).run(vehicle.user_id, vehicle.id, normalized, PARKING_FEE, 'Crypto payment failed: ' + err.message);

    res.status(402).json({
      error: err.message,
      plate_number: normalized,
      gate: 'closed',
      payment_method: 'crypto'
    });
  }
}

// POST /api/crypto/deposit - Deposit ETH to smart contract
router.post('/crypto/deposit', authenticateToken, (req, res) => {
  const { amount } = req.body;

  if (!blockchain.isBlockchainConnected()) {
    return res.status(503).json({ error: 'Blockchain not available' });
  }

  if (!amount || amount <= 0) {
    return res.status(400).json({ error: 'Amount must be positive' });
  }

  const accountIndex = ((req.user.id - 1) % 19) + 1;

  blockchain.depositForUser(accountIndex, amount)
    .then(async (result) => {
      const balance = await blockchain.getCryptoBalance(accountIndex);
      res.json({
        message: `Deposited ${amount} ETH to smart contract`,
        tx_hash: result.txHash,
        crypto_balance: balance + ' ETH'
      });
    })
    .catch(err => {
      res.status(500).json({ error: 'Deposit failed: ' + err.message });
    });
});

// GET /api/crypto/balance - Get crypto balance
router.get('/crypto/balance', authenticateToken, async (req, res) => {
  if (!blockchain.isBlockchainConnected()) {
    return res.json({ balance: '0', connected: false, error: 'Blockchain not available' });
  }

  try {
    const accountIndex = ((req.user.id - 1) % 19) + 1;
    const balance = await blockchain.getCryptoBalance(accountIndex);
    const address = await blockchain.getWalletAddress(accountIndex);
    res.json({ balance: balance + ' ETH', address, connected: true });
  } catch (err) {
    res.status(500).json({ error: err.message, connected: false });
  }
});

// GET /api/blockchain/status - Check blockchain connection
router.get('/blockchain/status', authenticateToken, (req, res) => {
  res.json({
    connected: blockchain.isBlockchainConnected(),
    network: 'localhost (Hardhat)'
  });
});

// GET /api/transactions/history
router.get('/transactions/history', authenticateToken, (req, res) => {
  const db = getDb();
  const transactions = db.prepare(
    `SELECT id, plate_number, amount, type, status, message, created_at
     FROM transactions
     WHERE user_id = ?
     ORDER BY created_at DESC
     LIMIT 50`
  ).all(req.user.id);

  res.json({ transactions });
});

module.exports = router;
