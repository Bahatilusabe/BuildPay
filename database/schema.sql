-- Schema for payroll app (SQLite)

CREATE TABLE IF NOT EXISTS users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT UNIQUE,
    password TEXT,
    role TEXT
);
CREATE TABLE IF NOT EXISTS employees (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    employee_number TEXT UNIQUE,
    full_name TEXT NOT NULL,
    gender TEXT,
    phone TEXT,
    email TEXT,
    position TEXT,
    department TEXT,
    basic_salary REAL,
    status TEXT
);

CREATE TABLE IF NOT EXISTS attendance (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    employee_id INTEGER,
    attendance_date DATE,
    status TEXT,
    overtime_hours INTEGER,
    FOREIGN KEY(employee_id) REFERENCES employees(id)
);

CREATE TABLE IF NOT EXISTS payroll (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    employee_id INTEGER,
    month TEXT,
    days_worked INTEGER,
    overtime_hours REAL,
    allowance REAL,
    deduction REAL,
    gross_salary REAL,
    net_salary REAL,
    created_at TEXT,
    FOREIGN KEY(employee_id) REFERENCES employees(id)
);
