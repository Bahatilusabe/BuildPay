-- Seed data for payroll app

INSERT OR IGNORE INTO users (username, password, role) VALUES ('admin', 'admin123', 'ADMIN');

INSERT INTO employees (employee_number, full_name, gender, phone, email, position, department, basic_salary, status) VALUES
('EMP001', 'Alice Smith', 'F', '0710000001', 'alice@example.com', 'Developer', 'Engineering', 80000, 'ACTIVE'),
('EMP002', 'Bob Johnson', 'M', '0710000002', 'bob@example.com', 'Designer', 'Product', 70000, 'ACTIVE');

-- Example attendance (uses new columns)
INSERT INTO attendance (employee_id, attendance_date, status, overtime_hours) VALUES
(1, '2026-06-01', 'PRESENT', 0),
(2, '2026-06-01', 'PRESENT', 0);
