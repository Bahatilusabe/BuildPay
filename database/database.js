'use strict';

const fs = require('fs');
const path = require('path');
const Database = require('better-sqlite3');
const { seed } = require('./seed');
const migrations = [
  require('./migrations/001-initial-schema'),
  require('./migrations/002-legacy-compatibility'),
  require('./migrations/003-project-details'),
  require('./migrations/004-employee-supervisors'),
  require('./migrations/005-national-id-unique'),
  require('./migrations/006-project-workspace-files')
];

const ensureMigrationTable = (database) => {
  database.exec(`
    CREATE TABLE IF NOT EXISTS schema_migrations (
      version INTEGER PRIMARY KEY,
      name TEXT NOT NULL,
      applied_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
    );
  `);
};

const runMigrations = (database) => {
  ensureMigrationTable(database);

  const appliedVersions = new Set(
    database.prepare('SELECT version FROM schema_migrations ORDER BY version').all()
      .map((migration) => migration.version)
  );

  for (const migration of migrations.sort((left, right) => left.version - right.version)) {
    if (appliedVersions.has(migration.version)) continue;

    const applyMigration = database.transaction(() => {
      migration.up(database);
      database.prepare(`
        INSERT INTO schema_migrations (version, name)
        VALUES (?, ?)
      `).run(migration.version, migration.name);
    });

    applyMigration();
  }
};

const getDatabaseHealth = (database, databasePath) => ({
  ok: Boolean(database && database.open),
  path: databasePath,
  foreignKeys: database ? database.pragma('foreign_keys', { simple: true }) === 1 : false,
  userVersion: database ? database.pragma('user_version', { simple: true }) : 0,
  migrations: database
    ? database.prepare('SELECT version, name, applied_at AS appliedAt FROM schema_migrations ORDER BY version').all()
    : []
});

const initializeDatabase = (userDataPath) => {
  fs.mkdirSync(userDataPath, { recursive: true });
  const databasePath = path.join(userDataPath, 'buildpay.db');
  const database = new Database(databasePath);

  try {
    database.pragma('foreign_keys = ON');
    database.pragma('journal_mode = WAL');
    database.pragma('busy_timeout = 5000');
    runMigrations(database);
    seed(database);

    const health = getDatabaseHealth(database, databasePath);
    if (!health.ok || !health.foreignKeys) {
      throw new Error('SQLite database health check failed');
    }

    return {
      database,
      databasePath,
      health: () => getDatabaseHealth(database, databasePath),
      close: () => {
        if (database.open) database.close();
      }
    };
  } catch (error) {
    if (database.open) database.close();
    throw new Error(`Unable to initialize BuildPay database: ${error.message}`);
  }
};

module.exports = { initializeDatabase };
