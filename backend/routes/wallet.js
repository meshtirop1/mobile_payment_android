const express = require('express');
const { getDb } = require('../database');
const { authenticateToken } = require('../middleware/auth');

const router = express.Router();

// GET /api/wallet
router.get('/', authenticateToken, (req, res) => {
  const db = getDb();
  const user = db.prepare('SELECT balance FROM users WHERE id = ?').get(req.user.id);
  if (!user) {
    return res.status(404).json({ error: 'User not found' });
  }
  res.json({ balance: user.balance });
});

// POST /api/wallet/topup
router.post('/topup', authenticateToken, (req, res) => {
  const db = getDb();
  const { amount } = req.body;

  if (!amount || typeof amount !== 'number' || amount <= 0) {
    return res.status(400).json({ error: 'Amount must be a positive number' });
  }

  if (amount > 1000) {
    return res.status(400).json({ error: 'Maximum top-up amount is $1000' });
  }

  db.prepare('UPDATE users SET balance = balance + ? WHERE id = ?').run(amount, req.user.id);
  const user = db.prepare('SELECT balance FROM users WHERE id = ?').get(req.user.id);

  res.json({
    message: `Successfully added $${amount.toFixed(2)} to wallet`,
    balance: user.balance
  });
});

module.exports = router;
