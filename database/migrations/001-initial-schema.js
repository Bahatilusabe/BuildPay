'use strict';

module.exports = {
  version: 1,
  name: 'initial-schema',
  up(database) {
    database.exec(`
      CREATE TABLE IF NOT EXISTS company_profile (
        id INTEGER PRIMARY KEY CHECK (id = 1),
        company_name TEXT NOT NULL,
        business_address TEXT,
        email TEXT,
        phone TEXT,
        website TEXT,
        registration_number TEXT,
        tax_pin TEXT,
        business_type TEXT,
        year_established INTEGER,
        logo_path TEXT,
        updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
      );

      CREATE TABLE IF NOT EXISTS settings (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        setting_key TEXT NOT NULL UNIQUE,
        setting_value TEXT NOT NULL,
        updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
      );

      CREATE TABLE IF NOT EXISTS users (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        username TEXT NOT NULL UNIQUE,
        password_hash TEXT NOT NULL,
        full_name TEXT NOT NULL,
        role TEXT NOT NULL CHECK (role IN ('ADMIN', 'PAYROLL_OFFICER', 'MANAGER')),
        email TEXT UNIQUE,
        avatar TEXT,
        status TEXT NOT NULL DEFAULT 'ACTIVE',
        created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
        updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
      );

      CREATE TABLE IF NOT EXISTS departments (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        name TEXT NOT NULL UNIQUE,
        description TEXT,
        status TEXT NOT NULL DEFAULT 'ACTIVE',
        created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
        updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
      );

      CREATE TABLE IF NOT EXISTS employees (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        employee_number TEXT UNIQUE,
        employee_code TEXT NOT NULL UNIQUE,
        first_name TEXT NOT NULL,
        last_name TEXT NOT NULL,
        national_id TEXT,
        phone TEXT,
        email TEXT,
        address TEXT,
        gender TEXT,
        date_of_birth TEXT,
        hire_date TEXT,
        position TEXT,
        department_id INTEGER,
        department TEXT,
        employment_type TEXT,
        basic_salary REAL NOT NULL DEFAULT 0,
        salary REAL NOT NULL DEFAULT 0,
        bank_name TEXT,
        bank_account TEXT,
        status TEXT NOT NULL DEFAULT 'ACTIVE',
        photo TEXT,
        created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
        updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
        FOREIGN KEY (department_id) REFERENCES departments(id) ON DELETE SET NULL
      );

      CREATE TABLE IF NOT EXISTS projects (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        project_code TEXT NOT NULL UNIQUE,
        name TEXT NOT NULL,
        location TEXT,
        client TEXT,
        description TEXT,
        start_date TEXT,
        expected_end_date TEXT,
        budget REAL NOT NULL DEFAULT 0,
        status TEXT NOT NULL DEFAULT 'PLANNING',
        progress REAL NOT NULL DEFAULT 0,
        created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
        updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
      );

      CREATE TABLE IF NOT EXISTS employee_projects (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        employee_id INTEGER NOT NULL,
        project_id INTEGER NOT NULL,
        role TEXT,
        assigned_date TEXT,
        end_date TEXT,
        status TEXT NOT NULL DEFAULT 'ACTIVE',
        FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE,
        FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
        UNIQUE (employee_id, project_id, assigned_date)
      );

      CREATE TABLE IF NOT EXISTS attendance (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        employee_id INTEGER NOT NULL,
        project_id INTEGER,
        attendance_date TEXT NOT NULL,
        status TEXT NOT NULL,
        check_in TEXT,
        check_out TEXT,
        regular_hours REAL NOT NULL DEFAULT 0,
        overtime_hours REAL NOT NULL DEFAULT 0,
        notes TEXT,
        created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
        updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
        UNIQUE (employee_id, attendance_date),
        FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE,
        FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE SET NULL
      );

      CREATE TABLE IF NOT EXISTS payroll (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        employee_id INTEGER NOT NULL,
        pay_period TEXT NOT NULL,
        basic_salary REAL NOT NULL DEFAULT 0,
        overtime_amount REAL NOT NULL DEFAULT 0,
        allowances_amount REAL NOT NULL DEFAULT 0,
        gross_salary REAL NOT NULL DEFAULT 0,
        paye REAL NOT NULL DEFAULT 0,
        nssf REAL NOT NULL DEFAULT 0,
        shif REAL NOT NULL DEFAULT 0,
        other_deductions REAL NOT NULL DEFAULT 0,
        total_deductions REAL NOT NULL DEFAULT 0,
        net_salary REAL NOT NULL DEFAULT 0,
        status TEXT NOT NULL DEFAULT 'DRAFT',
        processed_by INTEGER,
        processed_at TEXT,
        created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
        updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
        FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE,
        FOREIGN KEY (processed_by) REFERENCES users(id) ON DELETE SET NULL,
        UNIQUE (employee_id, pay_period)
      );

      CREATE TABLE IF NOT EXISTS payroll_items (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        payroll_id INTEGER NOT NULL,
        type TEXT NOT NULL CHECK (type IN ('EARNING', 'DEDUCTION')),
        category TEXT,
        name TEXT NOT NULL,
        amount REAL NOT NULL DEFAULT 0,
        description TEXT,
        FOREIGN KEY (payroll_id) REFERENCES payroll(id) ON DELETE CASCADE
      );

      CREATE TABLE IF NOT EXISTS allowances (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        employee_id INTEGER NOT NULL,
        payroll_id INTEGER,
        name TEXT NOT NULL,
        amount REAL NOT NULL DEFAULT 0,
        type TEXT,
        description TEXT,
        created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
        FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE,
        FOREIGN KEY (payroll_id) REFERENCES payroll(id) ON DELETE CASCADE
      );

      CREATE TABLE IF NOT EXISTS deductions (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        employee_id INTEGER NOT NULL,
        payroll_id INTEGER,
        name TEXT NOT NULL,
        amount REAL NOT NULL DEFAULT 0,
        type TEXT,
        description TEXT,
        created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
        FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE,
        FOREIGN KEY (payroll_id) REFERENCES payroll(id) ON DELETE CASCADE
      );

      CREATE TABLE IF NOT EXISTS overtime (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        employee_id INTEGER NOT NULL,
        project_id INTEGER,
        date TEXT NOT NULL,
        hours REAL NOT NULL DEFAULT 0,
        rate REAL NOT NULL DEFAULT 0,
        amount REAL NOT NULL DEFAULT 0,
        approved_by INTEGER,
        status TEXT NOT NULL DEFAULT 'PENDING',
        created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
        FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE,
        FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE SET NULL,
        FOREIGN KEY (approved_by) REFERENCES users(id) ON DELETE SET NULL
      );

      CREATE TABLE IF NOT EXISTS payslips (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        payroll_id INTEGER NOT NULL,
        employee_id INTEGER NOT NULL,
        payslip_number TEXT NOT NULL UNIQUE,
        pay_period TEXT NOT NULL,
        generated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
        pdf_path TEXT,
        status TEXT NOT NULL DEFAULT 'GENERATED',
        FOREIGN KEY (payroll_id) REFERENCES payroll(id) ON DELETE CASCADE,
        FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE
      );

      CREATE TABLE IF NOT EXISTS activity_logs (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        user_id INTEGER,
        action TEXT NOT NULL,
        module TEXT NOT NULL,
        description TEXT,
        created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
      );

      CREATE TABLE IF NOT EXISTS payroll_records (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        employee_id INTEGER NOT NULL,
        pay_period TEXT NOT NULL,
        basic_salary REAL NOT NULL DEFAULT 0,
        overtime_pay REAL NOT NULL DEFAULT 0,
        allowances REAL NOT NULL DEFAULT 0,
        deductions REAL NOT NULL DEFAULT 0,
        net_salary REAL NOT NULL DEFAULT 0,
        status TEXT NOT NULL DEFAULT 'Draft',
        created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
        FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE
      );

      CREATE INDEX IF NOT EXISTS idx_employees_status ON employees(status);
      CREATE INDEX IF NOT EXISTS idx_attendance_employee_id ON attendance(employee_id);
      CREATE INDEX IF NOT EXISTS idx_attendance_date ON attendance(attendance_date);
      CREATE INDEX IF NOT EXISTS idx_payroll_employee_id ON payroll(employee_id);
      CREATE INDEX IF NOT EXISTS idx_payroll_period ON payroll(pay_period);
      CREATE INDEX IF NOT EXISTS idx_payroll_status ON payroll(status);
      CREATE INDEX IF NOT EXISTS idx_payslips_employee_id ON payslips(employee_id);
      CREATE INDEX IF NOT EXISTS idx_projects_status ON projects(status);
      CREATE INDEX IF NOT EXISTS idx_activity_logs_created_at ON activity_logs(created_at);
      CREATE INDEX IF NOT EXISTS idx_payroll_records_period ON payroll_records(pay_period);
    `);
  }
};
