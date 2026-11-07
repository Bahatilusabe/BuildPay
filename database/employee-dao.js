'use strict';

class EmployeeDao {
  constructor(database) {
    this.database = database;
    this.listStatement = database.prepare(`
          SELECT employees.id, employees.employee_code AS employeeCode, employees.employee_number AS employeeNumber,
            employees.first_name AS firstName, employees.last_name AS lastName,
            employees.national_id AS nationalId,
            employees.first_name || ' ' || employees.last_name AS name,
              employees.position, employees.department, employees.salary, employees.status,
            employees.supervisor_id AS supervisorId,
            supervisor.first_name || ' ' || supervisor.last_name AS supervisorName,
              employees.created_at AS createdAt, employees.updated_at AS updatedAt
          FROM employees
          LEFT JOIN employees supervisor ON supervisor.id = employees.supervisor_id
      ORDER BY employees.last_name, employees.first_name
    `);
    this.insertStatement = database.prepare(`
      INSERT INTO employees (employee_code, national_id, first_name, last_name, position, department, salary, status, supervisor_id)
      VALUES (@employeeCode, @nationalId, @firstName, @lastName, @position, @department, @salary, @status, @supervisorId)
    `);
    this.updateStatement = database.prepare(`
      UPDATE employees
      SET first_name = @firstName,
          last_name = @lastName,
          employee_code = @employeeCode,
          national_id = @nationalId,
          position = @position,
          department = @department,
          salary = @salary,
          basic_salary = @salary,
          status = @status,
          supervisor_id = @supervisorId,
          updated_at = CURRENT_TIMESTAMP
      WHERE id = @id
    `);
    this.deleteStatement = database.prepare('DELETE FROM employees WHERE id = ?');
  }

  list() {
    return this.listStatement.all();
  }

  create(employee) {
    const nationalId = String(employee.nationalId || '').trim();
    if (!/^\d{6,20}$/.test(nationalId)) throw new Error('National ID must contain 6 to 20 digits');
    const employeeCode = nationalId;
    const result = this.insertStatement.run({
      employeeCode,
      nationalId,
      firstName: employee.firstName,
      lastName: employee.lastName,
      position: employee.position || null,
      department: employee.department || null,
      salary: Number(employee.salary) || 0,
      status: employee.status || 'Active',
      supervisorId: employee.supervisorId ? Number(employee.supervisorId) : null
    });
    return this.list().find(record => record.id === Number(result.lastInsertRowid));
  }

  update(id, employee) {
    if (Number(employee.supervisorId) === Number(id)) {
      throw new Error('An employee cannot supervise themselves');
    }
    const nationalId = String(employee.nationalId || '').trim();
    if (!/^\d{6,20}$/.test(nationalId)) throw new Error('National ID must contain 6 to 20 digits');
    this.updateStatement.run({
      id: Number(id),
      employeeCode: nationalId,
      nationalId,
      firstName: employee.firstName,
      lastName: employee.lastName,
      position: employee.position || null,
      department: employee.department || null,
      salary: Number(employee.salary) || 0,
      status: employee.status || 'Active',
      supervisorId: employee.supervisorId ? Number(employee.supervisorId) : null
    });
    return this.list().find(record => record.id === Number(id));
  }

  delete(id) {
    return this.deleteStatement.run(Number(id)).changes > 0;
  }
}

module.exports = { EmployeeDao };
