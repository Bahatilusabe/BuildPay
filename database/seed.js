'use strict';

const hasColumn = (database, tableName, columnName) => Boolean(database.prepare(`
  SELECT 1 FROM pragma_table_info(?) WHERE name = ?
`).get(tableName, columnName));

const seed = (database) => {
  const seedTransaction = database.transaction(() => {
    database.prepare(`
      INSERT INTO company_profile (
        id, company_name, business_address, email, phone, website,
        registration_number, tax_pin, business_type, year_established, logo_path
      ) VALUES (
        1, @companyName, @businessAddress, @email, @phone, @website,
        @registrationNumber, @taxPin, @businessType, @yearEstablished, @logoPath
      ) ON CONFLICT(id) DO NOTHING
    `).run({
      companyName: 'BuildPay Construction Ltd',
      businessAddress: 'P.O. Box 12345, Nairobi, Kenya',
      email: 'info@buildpay.co.ke',
      phone: '+254 712 345 678',
      website: 'www.buildpay.co.ke',
      registrationNumber: 'CR12/34567',
      taxPin: 'A001234567B',
      businessType: 'Construction',
      yearEstablished: 2020,
      logoPath: 'name_logo.png'
    });

    const legacySettings = hasColumn(database, 'settings', 'key') && hasColumn(database, 'settings', 'value');
    const settings = legacySettings
      ? database.prepare(`
          INSERT INTO settings (setting_key, setting_value, "key", "value")
          VALUES (?, ?, ?, ?)
          ON CONFLICT(setting_key) DO NOTHING
        `)
      : database.prepare(`
          INSERT INTO settings (setting_key, setting_value)
          VALUES (?, ?)
          ON CONFLICT(setting_key) DO NOTHING
        `);
    [
      ['company_name', 'BuildPay Construction Ltd'],
      ['company_address', 'P.O. Box 12345, Nairobi, Kenya'],
      ['company_email', 'info@buildpay.co.ke'],
      ['company_phone', '+254 712 345 678'],
      ['currency', 'KES'],
      ['theme', 'light'],
      ['payroll_year', String(new Date().getFullYear())],
      ['financial_year', String(new Date().getFullYear())]
    ].forEach(([key, value]) => {
      if (legacySettings) settings.run(key, value, key, value);
      else settings.run(key, value);
    });

    const department = database.prepare(`
      INSERT INTO departments (name, description)
      VALUES (?, ?)
      ON CONFLICT(name) DO NOTHING
    `);
    [
      ['Engineering', 'Engineering and technical staff'],
      ['Construction', 'Construction site staff'],
      ['Management', 'Company management and administration'],
      ['Operations', 'Operations and support staff'],
      ['Safety', 'Safety and compliance staff']
    ].forEach(([name, description]) => department.run(name, description));

    if (database.prepare('SELECT COUNT(*) AS count FROM employees').get().count === 0) {
      const demoEmployees = database.prepare(`
        INSERT INTO employees (
          employee_number, employee_code, first_name, last_name,
          position, department, basic_salary, salary, status
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'Active')
      `);
      [
        ['EMP001', 'EMP001', 'James', 'Mwangi', 'Site Engineer', 'Engineering', 85000],
        ['EMP002', 'EMP002', 'Peter', 'Ochieng', 'Mason', 'Construction', 45000],
        ['EMP003', 'EMP003', 'Daniel', 'Omondi', 'Carpenter', 'Construction', 40000],
        ['EMP004', 'EMP004', 'Samuel', 'Kariuki', 'Steel Fixer', 'Construction', 42000],
        ['EMP005', 'EMP005', 'John', 'Maina', 'Project Manager', 'Management', 120000],
        ['EMP006', 'EMP006', 'Amina', 'Mohamed', 'Safety Officer', 'Safety', 55000],
        ['EMP007', 'EMP007', 'Evans', 'Mutua', 'Electrician', 'Engineering', 62000],
        ['EMP008', 'EMP008', 'Susan', 'Cheruiyot', 'Plumber', 'Construction', 48000],
        ['EMP009', 'EMP009', 'Martin', 'Kipchoge', 'Labor Supervisor', 'Construction', 58000],
        ['EMP010', 'EMP010', 'Rose', 'Lemboi', 'HR Manager', 'Management', 95000]
      ].forEach(([number, code, firstName, lastName, position, departmentName, salary]) => {
        demoEmployees.run(number, code, firstName, lastName, position, departmentName, salary, salary);
      });
    }

    if (database.prepare('SELECT COUNT(*) AS count FROM attendance').get().count === 0) {
      const attendanceEmployees = database.prepare('SELECT id FROM employees ORDER BY id').all();
      const attendanceInsert = database.prepare(`
        INSERT INTO attendance (
          employee_id, attendance_date, status, regular_hours, overtime_hours
        ) VALUES (?, ?, ?, ?, ?)
      `);
      const attendanceMonth = '2025-05';
      const attendanceStatuses = ['PRESENT', 'PRESENT', 'PRESENT', 'ABSENT', 'PRESENT', 'LEAVE', 'PRESENT'];
      attendanceEmployees.forEach((employee, employeeIndex) => {
        for (let day = 1; day <= 31; day += 1) {
          const status = attendanceStatuses[(day + employeeIndex) % attendanceStatuses.length];
          const overtimeHours = status === 'PRESENT' && day % 10 === 0 ? 2 : 0;
          attendanceInsert.run(
            employee.id,
            `${attendanceMonth}-${String(day).padStart(2, '0')}`,
            status,
            status === 'ABSENT' || status === 'LEAVE' ? 0 : 8,
            overtimeHours
          );
        }
      });
    }

    if (database.prepare('SELECT COUNT(*) AS count FROM projects').get().count === 0) {
      const projectInsert = database.prepare(`
        INSERT INTO projects (
          project_code, name, location, client, start_date,
          expected_end_date, budget, status, progress, manager, labor_cost, worker_count
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
      `);
      [
        ['PRJ001', 'Skyline Apartments', 'Nairobi, Kenya', 'Skyline Holdings', '2025-01-15', '2025-12-20', 8500000, 'ACTIVE', 72, 'James Mwangi', 2450000, 42],
        ['PRJ002', 'Greenfield Mall', 'Kisumu, Kenya', 'Greenfield Group', '2025-02-10', '2026-01-30', 12000000, 'PLANNING', 35, 'Sarah Otieno', 3200000, 28],
        ['PRJ003', 'Riverside Towers', 'Mombasa, Kenya', 'Riverside Properties', '2025-03-05', '2026-12-15', 15900000, 'ON_HOLD', 60, 'David Kimani', 5400000, 54],
        ['PRJ004', 'Sunset Villas', 'Nakuru, Kenya', 'Sunset Homes', '2025-04-01', '2025-11-30', 6800000, 'ACTIVE', 48, 'Grace Wanjiku', 2100000, 36],
        ['PRJ005', 'The Horizon Estate', 'Eldoret, Kenya', 'Horizon Developers', '2025-05-12', '2026-02-28', 9500000, 'PLANNING', 25, 'Michael Njogu', 2850000, 30],
        ['PRJ006', 'City Center Complex', 'Thika, Kenya', 'City Center Ltd', '2024-01-10', '2024-12-10', 20000000, 'COMPLETED', 100, 'Peter Kamau', 7800000, 78]
      ].forEach(project => projectInsert.run(...project));
    }
  });

  seedTransaction();
};

module.exports = { seed };
