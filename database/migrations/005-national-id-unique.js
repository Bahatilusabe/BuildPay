'use strict';

module.exports = {
  version: 5,
  name: 'national-id-unique',
  up(database) {
    database.exec(`
      CREATE UNIQUE INDEX IF NOT EXISTS idx_employees_national_id_unique
      ON employees(national_id)
      WHERE national_id IS NOT NULL AND national_id <> '';
    `);
  }
};