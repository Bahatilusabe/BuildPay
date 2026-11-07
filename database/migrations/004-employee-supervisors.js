'use strict';

const columnExists = (database, tableName, columnName) => Boolean(database.prepare(`
  SELECT 1 FROM pragma_table_info(?) WHERE name = ?
`).get(tableName, columnName));

module.exports = {
  version: 4,
  name: 'employee-supervisors',
  up(database) {
    if (!columnExists(database, 'employees', 'supervisor_id')) {
      database.exec('ALTER TABLE employees ADD COLUMN supervisor_id INTEGER REFERENCES employees(id) ON DELETE SET NULL');
    }
    database.exec('CREATE INDEX IF NOT EXISTS idx_employees_supervisor_id ON employees(supervisor_id)');
  }
};