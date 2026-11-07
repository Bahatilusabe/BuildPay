'use strict';

const columnExists = (database, tableName, columnName) => database.prepare(`
  SELECT 1 FROM pragma_table_info(?) WHERE name = ?
`).get(tableName, columnName);

const addColumn = (database, tableName, columnName, definition) => {
  if (!columnExists(database, tableName, columnName)) {
    database.exec(`ALTER TABLE ${tableName} ADD COLUMN ${columnName} ${definition}`);
  }
};

module.exports = {
  version: 2,
  name: 'legacy-compatibility',
  up(database) {
    addColumn(database, 'company_profile', 'business_address', 'TEXT');
    addColumn(database, 'company_profile', 'email', 'TEXT');
    addColumn(database, 'company_profile', 'phone', 'TEXT');
    addColumn(database, 'company_profile', 'website', 'TEXT');
    addColumn(database, 'company_profile', 'registration_number', 'TEXT');
    addColumn(database, 'company_profile', 'tax_pin', 'TEXT');
    addColumn(database, 'company_profile', 'business_type', 'TEXT');
    addColumn(database, 'company_profile', 'year_established', 'INTEGER');
    addColumn(database, 'company_profile', 'logo_path', 'TEXT');

    addColumn(database, 'settings', 'setting_key', 'TEXT');
    addColumn(database, 'settings', 'setting_value', 'TEXT');
    if (columnExists(database, 'settings', 'key')) {
      database.exec('UPDATE settings SET setting_key = key WHERE setting_key IS NULL AND key IS NOT NULL');
    }
    if (columnExists(database, 'settings', 'value')) {
      database.exec('UPDATE settings SET setting_value = value WHERE setting_value IS NULL AND value IS NOT NULL');
    }
    database.exec(`
      CREATE UNIQUE INDEX IF NOT EXISTS idx_settings_setting_key_unique
        ON settings(setting_key);
    `);

    addColumn(database, 'employees', 'employee_number', 'TEXT');
    addColumn(database, 'employees', 'national_id', 'TEXT');
    addColumn(database, 'employees', 'phone', 'TEXT');
    addColumn(database, 'employees', 'email', 'TEXT');
    addColumn(database, 'employees', 'address', 'TEXT');
    addColumn(database, 'employees', 'gender', 'TEXT');
    addColumn(database, 'employees', 'date_of_birth', 'TEXT');
    addColumn(database, 'employees', 'hire_date', 'TEXT');
    addColumn(database, 'employees', 'department_id', 'INTEGER');
    addColumn(database, 'employees', 'employment_type', 'TEXT');
    addColumn(database, 'employees', 'basic_salary', 'REAL NOT NULL DEFAULT 0');
    addColumn(database, 'employees', 'bank_name', 'TEXT');
    addColumn(database, 'employees', 'bank_account', 'TEXT');
    addColumn(database, 'employees', 'photo', 'TEXT');
    database.exec(`
      UPDATE employees SET employee_number = employee_code WHERE employee_number IS NULL;
      UPDATE employees SET basic_salary = salary WHERE basic_salary = 0 AND salary IS NOT NULL;
    `);

    addColumn(database, 'attendance', 'project_id', 'INTEGER');
    addColumn(database, 'attendance', 'check_in', 'TEXT');
    addColumn(database, 'attendance', 'check_out', 'TEXT');
    addColumn(database, 'attendance', 'regular_hours', 'REAL NOT NULL DEFAULT 0');
    addColumn(database, 'attendance', 'overtime_hours', 'REAL NOT NULL DEFAULT 0');
    addColumn(database, 'attendance', 'updated_at', 'TEXT');
    database.exec(`UPDATE attendance SET updated_at = COALESCE(updated_at, created_at, CURRENT_TIMESTAMP)`);

    database.exec(`
      CREATE UNIQUE INDEX IF NOT EXISTS idx_employees_employee_number_unique
        ON employees(employee_number) WHERE employee_number IS NOT NULL;
      CREATE INDEX IF NOT EXISTS idx_employees_status ON employees(status);
      CREATE INDEX IF NOT EXISTS idx_attendance_employee_id ON attendance(employee_id);
      CREATE INDEX IF NOT EXISTS idx_attendance_date ON attendance(attendance_date);
      CREATE INDEX IF NOT EXISTS idx_attendance_project_id ON attendance(project_id);
    `);
  }
};
