const express = require('express');
const bcrypt = require('bcryptjs');
const { getDb } = require('../database');
const { generateToken } = require('../middleware/auth');

const router = express.Router();

// POST /api/register
router.post('/register', (req, res) => {
  const db = getDb();
  const { name, email, password } = req.body;

  if (!name || !email || !password) {
    return res.status(400).json({ error: 'Name, email, and password are required' });
  }

  if (password.length < 6) {
    return res.status(400).json({ error: 'Password must be at least 6 characters' });
  }

  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  if (!emailRegex.test(email)) {
    return res.status(400).json({ error: 'Invalid email format' });
  }

  const existing = db.prepare('SELECT id FROM users WHERE email = ?').get(email);
  if (existing) {
    return res.status(409).json({ error: 'Email already registered' });
  }

  const hashedPassword = bcrypt.hashSync(password, 10);
  const result = db.prepare(
    'INSERT INTO users (name, email, password, balance) VALUES (?, ?, ?, ?)'
  ).run(name, email, hashedPassword, 0.00);

  const user = db.prepare('SELECT id, name, email, balance FROM users WHERE id = ?').get(result.lastInsertRowid);
  const token = generateToken(user);

  res.status(201).json({
    message: 'Registration successful',
    token,
    user: { id: user.id, name: user.name, email: user.email, balance: user.balance }
  });
});

// POST /api/login
router.post('/login', (req, res) => {
  const db = getDb();
  const { email, password } = req.body;

  if (!email || !password) {
    return res.status(400).json({ error: 'Email and password are required' });
  }

  const user = db.prepare('SELECT * FROM users WHERE email = ?').get(email);
  if (!user) {
    return res.status(401).json({ error: 'Invalid email or password' });
  }

  const validPassword = bcrypt.compareSync(password, user.password);
  if (!validPassword) {
    return res.status(401).json({ error: 'Invalid email or password' });
  }

  const token = generateToken(user);

  res.json({
    message: 'Login successful',
    token,
    user: { id: user.id, name: user.name, email: user.email, balance: user.balance }
  });
});

module.exports = router;
