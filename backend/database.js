const initSqlJs = require('sql.js');
const fs = require('fs');
const path = require('path');

const DB_PATH = path.join(__dirname, 'parking.db');

let db = null;

// Wrapper that provides a synchronous-looking API similar to better-sqlite3
class Database {
  constructor(sqliteDb) {
    this._db = sqliteDb;
    this._inTransaction = false;
  }

  // Save to disk
  save() {
    const data = this._db.export();
    const buffer = Buffer.from(data);
    fs.writeFileSync(DB_PATH, buffer);
  }

  // Execute raw SQL (no results)
  exec(sql) {
    this._db.run(sql);
    this.save();
  }

  // Prepare a statement-like object
  prepare(sql) {
    const self = this;
    return {
      // Get single row
      get(...params) {
        const stmt = self._db.prepare(sql);
        stmt.bind(params);
        let result = null;
        if (stmt.step()) {
          result = stmt.getAsObject();
        }
        stmt.free();
        return result;
      },
      // Get all rows
      all(...params) {
        const results = [];
        const stmt = self._db.prepare(sql);
        stmt.bind(params);
        while (stmt.step()) {
          results.push(stmt.getAsObject());
        }
        stmt.free();
        return results;
      },
      // Run (INSERT/UPDATE/DELETE)
      run(...params) {
        self._db.run(sql, params);
        const ridStmt = self._db.prepare("SELECT last_insert_rowid()");
        let lastId = 0;
        if (ridStmt.step()) {
          lastId = ridStmt.get()[0];
        }
        ridStmt.free();
        const changes = self._db.getRowsModified();
        // Only save to disk if not inside a transaction
        if (!self._inTransaction) {
          self.save();
        }
        return {
          lastInsertRowid: lastId,
          changes: changes
        };
      }
    };
  }

  // Transaction helper
  transaction(fn) {
    const self = this;
    return function (...args) {
      self._inTransaction = true;
      self._db.run('BEGIN TRANSACTION');
      try {
        const result = fn(...args);
        self._db.run('COMMIT');
        self._inTransaction = false;
        self.save();
        return result;
      } catch (e) {
        self._db.run('ROLLBACK');
        self._inTransaction = false;
        throw e;
      }
    };
  }

  close() {
    this.save();
    this._db.close();
  }
}

// Initialize and return database
async function initDatabase() {
  const SQL = await initSqlJs();

  let sqliteDb;
  if (fs.existsSync(DB_PATH)) {
    const fileBuffer = fs.readFileSync(DB_PATH);
    sqliteDb = new SQL.Database(fileBuffer);
  } else {
    sqliteDb = new SQL.Database();
  }

  db = new Database(sqliteDb);

  // Create tables
  db._db.run(`
    CREATE TABLE IF NOT EXISTS users (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      name TEXT NOT NULL,
      email TEXT UNIQUE NOT NULL,
      password TEXT NOT NULL,
      balance REAL DEFAULT 0.00,
      created_at DATETIME DEFAULT CURRENT_TIMESTAMP
    )
  `);

  db._db.run(`
    CREATE TABLE IF NOT EXISTS vehicles (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      user_id INTEGER NOT NULL,
      plate_number TEXT UNIQUE NOT NULL,
      created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
      FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
    )
  `);

  db._db.run(`
    CREATE TABLE IF NOT EXISTS transactions (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      user_id INTEGER NOT NULL,
      vehicle_id INTEGER NOT NULL,
      plate_number TEXT NOT NULL,
      amount REAL NOT NULL,
      type TEXT NOT NULL,
      status TEXT NOT NULL,
      message TEXT,
      created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
      FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
      FOREIGN KEY (vehicle_id) REFERENCES vehicles(id) ON DELETE CASCADE
    )
  `);

  db._db.run('CREATE INDEX IF NOT EXISTS idx_vehicles_plate ON vehicles(plate_number)');
  db._db.run('CREATE INDEX IF NOT EXISTS idx_vehicles_user ON vehicles(user_id)');
  db._db.run('CREATE INDEX IF NOT EXISTS idx_transactions_user ON transactions(user_id)');

  db.save();

  return db;
}

function getDb() {
  if (!db) {
    throw new Error('Database not initialized. Call initDatabase() first.');
  }
  return db;
}

module.exports = { initDatabase, getDb };
