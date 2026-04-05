const bcrypt = require('bcryptjs');
const { initDatabase, getDb } = require('./database');

async function seed() {
  await initDatabase();
  const db = getDb();

  console.log('Seeding database with sample data...');

  // Clear existing data
  db.exec('DELETE FROM transactions');
  db.exec('DELETE FROM vehicles');
  db.exec('DELETE FROM users');

  // Create sample users
  const password = bcrypt.hashSync('password123', 10);

  const user1 = db.prepare(
    'INSERT INTO users (name, email, password, balance) VALUES (?, ?, ?, ?)'
  ).run('John Doe', 'john@example.com', password, 50.00);

  const user2 = db.prepare(
    'INSERT INTO users (name, email, password, balance) VALUES (?, ?, ?, ?)'
  ).run('Jane Smith', 'jane@example.com', password, 10.00);

  const user3 = db.prepare(
    'INSERT INTO users (name, email, password, balance) VALUES (?, ?, ?, ?)'
  ).run('Bob Wilson', 'bob@example.com', password, 1.00);

  // Add vehicles
  const v1 = db.prepare('INSERT INTO vehicles (user_id, plate_number) VALUES (?, ?)').run(user1.lastInsertRowid, 'ABC1234');
  const v2 = db.prepare('INSERT INTO vehicles (user_id, plate_number) VALUES (?, ?)').run(user1.lastInsertRowid, 'XYZ5678');
  const v3 = db.prepare('INSERT INTO vehicles (user_id, plate_number) VALUES (?, ?)').run(user2.lastInsertRowid, 'DEF9012');
  const v4 = db.prepare('INSERT INTO vehicles (user_id, plate_number) VALUES (?, ?)').run(user3.lastInsertRowid, 'LOW0001');

  // Add sample transactions
  db.prepare(
    `INSERT INTO transactions (user_id, vehicle_id, plate_number, amount, type, status, message)
     VALUES (?, ?, 'ABC1234', 2.00, 'entry', 'success', 'Gate opened - Entry allowed')`
  ).run(user1.lastInsertRowid, v1.lastInsertRowid);

  db.prepare(
    `INSERT INTO transactions (user_id, vehicle_id, plate_number, amount, type, status, message)
     VALUES (?, ?, 'DEF9012', 2.00, 'entry', 'success', 'Gate opened - Entry allowed')`
  ).run(user2.lastInsertRowid, v3.lastInsertRowid);

  console.log('Sample data seeded successfully!');
  console.log('');
  console.log('Test accounts:');
  console.log('  john@example.com / password123  (balance: $50.00, vehicles: ABC1234, XYZ5678)');
  console.log('  jane@example.com / password123  (balance: $10.00, vehicles: DEF9012)');
  console.log('  bob@example.com  / password123  (balance: $1.00,  vehicles: LOW0001)');

  db.close();
}

seed().catch(err => {
  console.error('Seeding failed:', err);
  process.exit(1);
});
