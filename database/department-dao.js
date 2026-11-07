'use strict';

class DepartmentDao {
  constructor(database) {
    this.database = database;
  }

  list() {
    return this.database.prepare(`
      SELECT d.id, d.name, d.description, d.status,
             COUNT(e.id) AS employeeCount
      FROM departments d
      LEFT JOIN employees e ON e.department = d.name
      GROUP BY d.id
      ORDER BY d.name
    `).all();
  }

  create(values) {
    const name = String(values?.name || '').trim();
    if (!name) throw new Error('Department name is required');
    const result = this.database.prepare(`
      INSERT INTO departments (name, description, status)
      VALUES (?, ?, 'ACTIVE')
    `).run(name, String(values?.description || '').trim() || null);
    return this.list().find(department => department.id === Number(result.lastInsertRowid));
  }

  update(id, values) {
    const name = String(values?.name || '').trim();
    if (!name) throw new Error('Department name is required');

    const existing = this.database.prepare('SELECT name FROM departments WHERE id = ?').get(Number(id));
    if (!existing) throw new Error('Department was not found');

    const update = this.database.transaction(() => {
      this.database.prepare(`
        UPDATE departments
        SET name = ?, description = ?, updated_at = CURRENT_TIMESTAMP
        WHERE id = ?
      `).run(name, String(values?.description || '').trim() || null, Number(id));
      this.database.prepare(`
        UPDATE employees
        SET department = ?, updated_at = CURRENT_TIMESTAMP
        WHERE department = ?
      `).run(name, existing.name);
    });
    update();
    return this.list().find(department => department.id === Number(id));
  }

  delete(id) {
    const department = this.database.prepare('SELECT name FROM departments WHERE id = ?').get(Number(id));
    if (!department) throw new Error('Department was not found');

    const employeeCount = this.database.prepare(
      'SELECT COUNT(*) AS count FROM employees WHERE department = ?'
    ).get(department.name).count;
    if (employeeCount > 0) {
      throw new Error('Department cannot be deleted while employees are assigned to it');
    }

    return this.database.prepare('DELETE FROM departments WHERE id = ?').run(Number(id)).changes > 0;
  }
}

module.exports = { DepartmentDao };
