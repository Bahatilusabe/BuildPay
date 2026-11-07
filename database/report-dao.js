'use strict';

class ReportDao {
  constructor(database) {
    this.database = database;
  }

  periodBounds(period) {
    const weeklyMatch = String(period || '').match(/^(\d{4})-W(\d{2})$/);
    if (weeklyMatch) {
      const year = Number(weeklyMatch[1]);
      const week = Number(weeklyMatch[2]);
      const januaryFourth = new Date(Date.UTC(year, 0, 4));
      const monday = new Date(januaryFourth);
      monday.setUTCDate(januaryFourth.getUTCDate() - (januaryFourth.getUTCDay() || 7) + 1 + ((week - 1) * 7));
      const sunday = new Date(monday);
      sunday.setUTCDate(monday.getUTCDate() + 6);
      return { start: monday.toISOString().slice(0, 10), end: sunday.toISOString().slice(0, 10) };
    }
    return { start: `${period}-01`, end: `${period}-31` };
  }

  employee(employeeId, period) {
    const employee = this.database.prepare(`
      SELECT e.id, e.employee_code AS employeeCode,
             e.first_name || ' ' || e.last_name AS name,
             e.position, e.department, e.salary, e.status,
             e.hire_date AS hireDate,
             supervisor.first_name || ' ' || supervisor.last_name AS supervisorName
      FROM employees e
      LEFT JOIN employees supervisor ON supervisor.id = e.supervisor_id
      WHERE e.id = ? OR e.employee_code = ?
    `).get(Number(employeeId) || 0, String(employeeId));
    if (!employee) return null;

    const bounds = this.periodBounds(period);
    const attendance = this.database.prepare(`
      SELECT COUNT(*) AS totalDays,
             COALESCE(SUM(CASE WHEN status IN ('PRESENT', 'LATE') THEN 1 ELSE 0 END), 0) AS payDays,
             COALESCE(SUM(CASE WHEN status = 'ABSENT' THEN 1 ELSE 0 END), 0) AS absentDays,
             COALESCE(SUM(CASE WHEN status = 'LEAVE' THEN 1 ELSE 0 END), 0) AS leaveDays,
             COALESCE(SUM(regular_hours), 0) AS regularHours,
             COALESCE(SUM(overtime_hours), 0) AS overtimeHours
      FROM attendance
      WHERE employee_id = ? AND attendance_date BETWEEN ? AND ?
    `).get(employee.id, bounds.start, bounds.end);

    const payroll = this.database.prepare(`
      SELECT id, pay_period AS payPeriod, basic_salary AS basicSalary,
             overtime_amount AS overtimeAmount, allowances_amount AS allowancesAmount,
             gross_salary AS grossSalary, paye, nssf, shif,
             other_deductions AS otherDeductions, total_deductions AS totalDeductions,
             net_salary AS netSalary, status, processed_at AS processedAt,
             updated_at AS updatedAt
      FROM payroll
      WHERE employee_id = ? AND pay_period = ?
    `).get(employee.id, period) || null;

    return { employee, attendance, payroll, period };
  }

  project(projectId) {
    const project = this.database.prepare(`
      SELECT id, project_code AS projectCode, name, location, client, description,
             start_date AS startDate, expected_end_date AS expectedEndDate,
             budget, status, progress, manager, labor_cost AS laborCost,
             worker_count AS workerCount
      FROM projects
      WHERE id = ? OR project_code = ? OR name = ?
    `).get(Number(projectId) || 0, String(projectId), String(projectId));
    if (!project) return null;

    const employees = this.database.prepare(`
      SELECT e.employee_code AS employeeCode,
             e.first_name || ' ' || e.last_name AS name,
             e.position, e.department, ep.role, ep.status AS assignmentStatus
      FROM employee_projects ep
      JOIN employees e ON e.id = ep.employee_id
      WHERE ep.project_id = ?
      ORDER BY e.last_name, e.first_name
    `).all(project.id);

    const attendance = this.database.prepare(`
      SELECT COUNT(*) AS attendanceEntries,
             COALESCE(SUM(CASE WHEN a.status IN ('PRESENT', 'LATE') THEN 1 ELSE 0 END), 0) AS payDays,
             COALESCE(SUM(a.regular_hours), 0) AS regularHours,
             COALESCE(SUM(a.overtime_hours), 0) AS overtimeHours
      FROM attendance a
      WHERE a.project_id = ?
    `).get(project.id);

    const payroll = this.database.prepare(`
      SELECT COUNT(*) AS payrollRecords,
             COALESCE(SUM(p.gross_salary), 0) AS grossPayroll,
             COALESCE(SUM(p.total_deductions), 0) AS totalDeductions,
             COALESCE(SUM(p.net_salary), 0) AS netPayroll
      FROM payroll p
      JOIN employee_projects ep ON ep.employee_id = p.employee_id
      WHERE ep.project_id = ?
    `).get(project.id);

    return { project, employees, attendance, payroll };
  }

  summary(period) {
    const bounds = this.periodBounds(period);
    const employees = this.database.prepare(`
      SELECT e.employee_code AS employeeCode,
             e.first_name || ' ' || e.last_name AS employeeName,
             e.department, e.salary, e.status,
             COALESCE(SUM(CASE WHEN a.status IN ('PRESENT', 'LATE') THEN 1 ELSE 0 END), 0) AS presentDays,
             COALESCE(SUM(CASE WHEN a.status = 'ABSENT' THEN 1 ELSE 0 END), 0) AS absentDays,
             COALESCE(SUM(CASE WHEN a.status = 'LEAVE' THEN 1 ELSE 0 END), 0) AS leaveDays,
             COALESCE(SUM(a.overtime_hours), 0) AS overtimeHours
      FROM employees e
      LEFT JOIN attendance a ON a.employee_id = e.id
        AND a.attendance_date BETWEEN ? AND ?
      GROUP BY e.id
      ORDER BY e.last_name, e.first_name
    `).all(bounds.start, bounds.end);

    const payroll = this.database.prepare(`
      SELECT COALESCE(SUM(gross_salary), 0) AS grossPay,
             COALESCE(SUM(total_deductions), 0) AS deductions,
             COALESCE(SUM(net_salary), 0) AS netPay,
             COALESCE(SUM(paye), 0) AS paye,
             COALESCE(SUM(nssf), 0) AS nssf,
             COALESCE(SUM(shif), 0) AS shif,
             COUNT(*) AS payrollRecords
      FROM payroll
      WHERE pay_period = ?
    `).get(period);

    const projects = this.database.prepare(`
      SELECT id, project_code AS projectCode, name, budget, labor_cost AS laborCost,
             status, progress, manager, location, client
      FROM projects
      ORDER BY name
    `).all();

    const attendance = this.database.prepare(`
      SELECT COUNT(*) AS totalEntries,
             COALESCE(SUM(CASE WHEN status IN ('PRESENT', 'LATE') THEN 1 ELSE 0 END), 0) AS presentDays,
             COALESCE(SUM(CASE WHEN status = 'ABSENT' THEN 1 ELSE 0 END), 0) AS absentDays,
             COALESCE(SUM(CASE WHEN status = 'LEAVE' THEN 1 ELSE 0 END), 0) AS leaveDays,
             COALESCE(SUM(CASE WHEN status = 'LATE' THEN 1 ELSE 0 END), 0) AS lateDays
      FROM attendance
      WHERE attendance_date BETWEEN ? AND ?
    `).get(bounds.start, bounds.end);

    return { period, employees, payroll, projects, attendance };
  }
}

module.exports = { ReportDao };
