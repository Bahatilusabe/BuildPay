'use strict';

class DashboardDao {
  constructor(database) {
    this.database = database;
  }

  getSummary(period = '2025-05') {
    const employeeCount = this.database.prepare(`
      SELECT COUNT(*) AS totalEmployees,
             SUM(CASE WHEN status = 'Active' OR status = 'ACTIVE' THEN 1 ELSE 0 END) AS activeEmployees
      FROM employees
    `).get();
    const attendance = this.database.prepare(`
      SELECT COUNT(*) AS totalDays,
             SUM(CASE WHEN status = 'PRESENT' THEN 1 ELSE 0 END) AS presentDays,
             SUM(CASE WHEN status = 'ABSENT' THEN 1 ELSE 0 END) AS absentDays,
             SUM(CASE WHEN status = 'LEAVE' THEN 1 ELSE 0 END) AS leaveDays,
             SUM(CASE WHEN status = 'LATE' THEN 1 ELSE 0 END) AS lateDays,
             COALESCE(SUM(overtime_hours), 0) AS overtimeHours
      FROM attendance
      WHERE substr(attendance_date, 1, 7) = ?
    `).get(period);
    const payroll = this.database.prepare(`
      SELECT COALESCE(SUM(gross_salary), 0) AS grossPayroll,
             COALESCE(SUM(total_deductions), 0) AS deductions,
             COALESCE(SUM(net_salary), 0) AS totalPayroll
      FROM payroll
      WHERE pay_period = ? AND status != 'CANCELLED'
    `).get(period);
    const legacyPayroll = this.database.prepare(`
      SELECT COALESCE(SUM(net_salary), 0) AS totalPayroll
      FROM payroll_records
      WHERE pay_period = ? AND status != 'Cancelled'
    `).get(period);
    const projects = this.database.prepare(`
      SELECT COUNT(*) AS activeProjects
      FROM projects
      WHERE status IN ('ACTIVE', 'Active')
    `).get();
    const pendingPayslips = this.database.prepare(`
      SELECT COUNT(*) AS pendingPayslips
      FROM payslips
      WHERE status IN ('PENDING', 'GENERATED')
    `).get();
    const totalDays = attendance.totalDays || 0;

    const canonicalPayrollCount = this.database.prepare(
      'SELECT COUNT(*) AS count FROM payroll WHERE pay_period = ?'
    ).get(period).count;

    return {
      totalEmployees: employeeCount.totalEmployees || 0,
      activeEmployees: employeeCount.activeEmployees || 0,
      totalPayroll: canonicalPayrollCount
        ? payroll.totalPayroll || 0
        : legacyPayroll.totalPayroll || 0,
      grossPayroll: canonicalPayrollCount ? payroll.grossPayroll || 0 : 0,
      deductions: canonicalPayrollCount ? payroll.deductions || 0 : 0,
      attendanceRate: totalDays ? Math.round(((attendance.presentDays || 0) / totalDays) * 100) : 0,
      presentDays: attendance.presentDays || 0,
      absentDays: attendance.absentDays || 0,
      leaveDays: attendance.leaveDays || 0,
      lateDays: attendance.lateDays || 0,
      activeProjects: projects.activeProjects || 0,
      pendingPayslips: pendingPayslips.pendingPayslips || 0,
      overtimeHours: attendance.overtimeHours || 0
    };
  }

  getRecentActivities(limit = 4) {
    return this.database.prepare(`
      SELECT action, module, description, created_at AS createdAt
      FROM activity_logs
      ORDER BY created_at DESC
      LIMIT ?
    `).all(limit);
  }

  getProjectProgress() {
    return this.database.prepare(`
      SELECT project_code AS projectCode, name, location, status, progress
      FROM projects
      ORDER BY updated_at DESC
      LIMIT 4
    `).all();
  }
}

module.exports = { DashboardDao };
