const express = require('express');
const { getDb } = require('../database');
const { authenticateToken } = require('../middleware/auth');

const router = express.Router();

// GET /api/vehicles
router.get('/', authenticateToken, (req, res) => {
  const db = getDb();
  const vehicles = db.prepare(
    'SELECT id, plate_number, created_at FROM vehicles WHERE user_id = ? ORDER BY created_at DESC'
  ).all(req.user.id);
  res.json({ vehicles });
});

// POST /api/vehicles
router.post('/', authenticateToken, (req, res) => {
  const db = getDb();
  const { plate_number } = req.body;

  if (!plate_number) {
    return res.status(400).json({ error: 'Plate number is required' });
  }

  const normalized = plate_number.trim().toUpperCase();

  if (normalized.length < 2 || normalized.length > 15) {
    return res.status(400).json({ error: 'Plate number must be 2-15 characters' });
  }

  const existing = db.prepare('SELECT id, user_id FROM vehicles WHERE plate_number = ?').get(normalized);
  if (existing) {
    if (existing.user_id === req.user.id) {
      return res.status(409).json({ error: 'You have already registered this vehicle' });
    }
    return res.status(409).json({ error: 'This plate number is already registered to another user' });
  }

  const result = db.prepare(
    'INSERT INTO vehicles (user_id, plate_number) VALUES (?, ?)'
  ).run(req.user.id, normalized);

  res.status(201).json({
    message: 'Vehicle added successfully',
    vehicle: { id: result.lastInsertRowid, plate_number: normalized }
  });
});

// DELETE /api/vehicles/:id
router.delete('/:id', authenticateToken, (req, res) => {
  const db = getDb();
  const vehicle = db.prepare(
    'SELECT id FROM vehicles WHERE id = ? AND user_id = ?'
  ).get(parseInt(req.params.id), req.user.id);

  if (!vehicle) {
    return res.status(404).json({ error: 'Vehicle not found' });
  }

  db.prepare('DELETE FROM vehicles WHERE id = ?').run(parseInt(req.params.id));
  res.json({ message: 'Vehicle removed successfully' });
});

module.exports = router;
