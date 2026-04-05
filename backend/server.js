const express = require('express');
const cors = require('cors');
const { initDatabase } = require('./database');
const { initBlockchain } = require('./blockchain');

const authRoutes = require('./routes/auth');
const vehicleRoutes = require('./routes/vehicles');
const walletRoutes = require('./routes/wallet');
const parkingRoutes = require('./routes/parking');

const app = express();
const PORT = process.env.PORT || 3000;

// Middleware
app.use(cors());
app.use(express.json());

// Request logging
app.use((req, res, next) => {
  console.log(`${new Date().toISOString()} ${req.method} ${req.path}`);
  next();
});

// Routes
app.use('/api', authRoutes);
app.use('/api/vehicles', vehicleRoutes);
app.use('/api/wallet', walletRoutes);
app.use('/api', parkingRoutes);

// Health check
app.get('/api/health', (req, res) => {
  res.json({ status: 'ok', message: 'Smart Parking API is running' });
});

// 404 handler
app.use((req, res) => {
  res.status(404).json({ error: 'Endpoint not found' });
});

// Error handler
app.use((err, req, res, next) => {
  console.error('Server error:', err);
  res.status(500).json({ error: 'Internal server error' });
});

// Initialize database and blockchain, then start server
async function start() {
  await initDatabase();
  await initBlockchain();

  app.listen(PORT, '0.0.0.0', () => {
    console.log(`\nSmart Parking API running on http://0.0.0.0:${PORT}`);
    console.log('\nEndpoints:');
    console.log('  POST /api/register');
    console.log('  POST /api/login');
    console.log('  GET  /api/vehicles');
    console.log('  POST /api/vehicles');
    console.log('  GET  /api/wallet');
    console.log('  POST /api/wallet/topup');
    console.log('  POST /api/simulate-entry         (body: plate_number, payment_method: wallet|crypto)');
    console.log('  GET  /api/transactions/history');
    console.log('  POST /api/crypto/deposit          (body: amount in ETH)');
    console.log('  GET  /api/crypto/balance');
    console.log('  GET  /api/blockchain/status');
  });
}

start().catch(err => {
  console.error('Failed to start server:', err);
  process.exit(1);
});
