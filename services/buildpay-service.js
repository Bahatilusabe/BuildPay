'use strict';

const { ipcMain } = require('electron');
const { EmployeeDao } = require('../database/employee-dao');
const { AttendanceDao } = require('../database/attendance-dao');
const { DashboardDao } = require('../database/dashboard-dao');
const { ProjectDao } = require('../database/project-dao');
const { PayrollDao } = require('../database/payroll-dao');
const { DepartmentDao } = require('../database/department-dao');
const { ReportDao } = require('../database/report-dao');
const fs = require('fs');
const path = require('path');

const registerBuildPayService = (database, getHealth, userDataPath) => {
  const employees = new EmployeeDao(database);
  const attendance = new AttendanceDao(database);
  const dashboard = new DashboardDao(database);
  const projects = new ProjectDao(database);
  const payroll = new PayrollDao(database);
  const departments = new DepartmentDao(database);
  const reports = new ReportDao(database);

  ipcMain.handle('buildpay:database:health', () => (
    typeof getHealth === 'function'
      ? getHealth()
      : { ok: Boolean(database && database.open) }
  ));

  ipcMain.handle('buildpay:overview', () => ({
    employeeCount: database.prepare("SELECT COUNT(*) AS count FROM employees WHERE status = 'Active'").get().count,
    payrollTotal: database.prepare(`
      SELECT COALESCE(SUM(net_salary), 0) AS total
      FROM payroll
      WHERE status != 'CANCELLED'
    `).get().total,
    attendanceCount: database.prepare('SELECT COUNT(*) AS count FROM attendance').get().count
  }));

  ipcMain.handle('buildpay:employees:list', () => employees.list());
  ipcMain.handle('buildpay:employees:create', (_event, employee) => employees.create(employee));
  ipcMain.handle('buildpay:employees:update', (_event, id, employee) => employees.update(id, employee));
  ipcMain.handle('buildpay:employees:delete', (_event, id) => employees.delete(id));
  ipcMain.handle('buildpay:departments:list', () => departments.list());
  ipcMain.handle('buildpay:departments:create', (_event, values) => departments.create(values));
  ipcMain.handle('buildpay:departments:update', (_event, id, values) => departments.update(id, values));
  ipcMain.handle('buildpay:departments:delete', (_event, id) => departments.delete(id));
  ipcMain.handle('buildpay:reports:employee', (_event, employeeId, period) => reports.employee(employeeId, period));
  ipcMain.handle('buildpay:reports:project', (_event, projectId) => reports.project(projectId));
  ipcMain.handle('buildpay:reports:summary', (_event, period) => reports.summary(period));

  ipcMain.handle('buildpay:attendance:list', (_event, month) => attendance.listByMonth(month));
  ipcMain.handle('buildpay:attendance:summary', (_event, period, employeeId) => {
    let resolvedEmployeeId = employeeId;
    if (employeeId) {
      const employee = database.prepare(
        'SELECT id FROM employees WHERE id = ? OR employee_code = ?'
      ).get(Number(employeeId) || 0, String(employeeId));
      resolvedEmployeeId = employee?.id;
    }
    return attendance.summary(period, resolvedEmployeeId);
  });

  ipcMain.handle('buildpay:attendance:record', (_event, record) => {
    const employee = database.prepare(
      'SELECT id FROM employees WHERE id = ? OR employee_code = ?'
    ).get(Number(record.employeeId) || 0, String(record.employeeId));
    if (!employee) throw new Error('Employee record was not found');
    const attendanceDate = String(record.attendanceDate || '');
    const parsedDate = new Date(`${attendanceDate}T00:00:00Z`);
    const isRealDate = /^\d{4}-\d{2}-\d{2}$/.test(attendanceDate)
      && !Number.isNaN(parsedDate.getTime())
      && parsedDate.toISOString().slice(0, 10) === attendanceDate;
    if (!isRealDate) {
      throw new Error('Attendance date must use YYYY-MM-DD format');
    }
    try {
      attendance.record({ ...record, employeeId: employee.id });
    } catch (error) {
      if (String(error.code || '').includes('SQLITE_CONSTRAINT')) {
        throw new Error('Attendance for this employee and date has already been recorded and cannot be edited.');
      }
      throw error;
    }
    return database.prepare(`
      SELECT * FROM attendance WHERE employee_id = ? AND attendance_date = ?
    `).get(employee.id, record.attendanceDate);
  });

  ipcMain.handle('buildpay:dashboard:summary', (_event, period) => dashboard.getSummary(period));
  ipcMain.handle('buildpay:dashboard:activities', (_event, limit) => dashboard.getRecentActivities(limit));
  ipcMain.handle('buildpay:dashboard:projects', () => dashboard.getProjectProgress());
  ipcMain.handle('buildpay:projects:list', () => projects.list());
  ipcMain.handle('buildpay:projects:create', (_event, project) => projects.create(project));
  ipcMain.handle('buildpay:projects:update', (_event, id, project) => projects.update(id, project));
  ipcMain.handle('buildpay:projects:delete', (_event, id) => projects.delete(id));
  ipcMain.handle('buildpay:projects:files:list', (_event, projectId, kind) => projects.listFiles(projectId, kind));
  ipcMain.handle('buildpay:projects:files:save', (_event, projectId, file) => {
    const projectDirectory = path.join(userDataPath, 'project-files', String(projectId));
    fs.mkdirSync(projectDirectory, { recursive: true });
    const safeName = path.basename(String(file.fileName || 'upload'));
    const storedName = `${Date.now()}-${safeName}`;
    const filePath = path.join(projectDirectory, storedName);
    fs.writeFileSync(filePath, Buffer.from(file.data));
    return projects.addFile(projectId, {
      kind: file.kind,
      fileName: safeName,
      mimeType: file.mimeType || 'application/octet-stream',
      filePath
    });
  });
  ipcMain.handle('buildpay:projects:files:delete', (_event, id) => projects.deleteFile(id));
  ipcMain.handle('buildpay:projects:files:read', (_event, id) => {
    const file = database.prepare('SELECT id, file_name AS fileName, mime_type AS mimeType, file_path AS filePath FROM project_files WHERE id = ?').get(Number(id));
    if (!file || !fs.existsSync(file.filePath)) return null;
    return { ...file, dataUrl: `data:${file.mimeType};base64,${fs.readFileSync(file.filePath).toString('base64')}` };
  });

  ipcMain.handle('buildpay:payroll:list', (_event, period) => payroll.list(period));
  ipcMain.handle('buildpay:payroll:trend', (_event, limit) => payroll.trend(limit));
  ipcMain.handle('buildpay:payroll:get', (_event, id) => payroll.get(id));
  ipcMain.handle('buildpay:payroll:report', (_event, period) => payroll.report(period));
  ipcMain.handle('buildpay:payslip:generate', (_event, payrollId) => payroll.generatePayslip(payrollId));
  ipcMain.handle('buildpay:payslips:generate', (_event, period) => payroll.generatePayslips(period));

  ipcMain.handle('buildpay:settings:get', () => ({
    company: database.prepare('SELECT * FROM company_profile WHERE id = 1').get() || {},
    settings: Object.fromEntries(database.prepare('SELECT setting_key, setting_value FROM settings').all().map(row => [row.setting_key, row.setting_value]))
  }));

  ipcMain.handle('buildpay:settings:save', (_event, values) => {
    const companyFields = ['companyName', 'businessAddress', 'email', 'phone', 'website', 'registrationNumber', 'taxPin', 'businessType', 'yearEstablished'];
    const columnMap = {
      companyName: 'company_name',
      businessAddress: 'business_address',
      email: 'email',
      phone: 'phone',
      website: 'website',
      registrationNumber: 'registration_number',
      taxPin: 'tax_pin',
      businessType: 'business_type',
      yearEstablished: 'year_established'
    };
    const current = database.prepare('SELECT id FROM company_profile WHERE id = 1').get();
    if (current) {
      companyFields.forEach(field => {
        if (values?.company?.[field] === undefined) return;
        database.prepare(`UPDATE company_profile SET ${columnMap[field]} = ?, updated_at = CURRENT_TIMESTAMP WHERE id = 1`).run(values.company[field]);
      });
    }
    if (values?.settings) {
      const setting = database.prepare(`
        INSERT INTO settings (setting_key, setting_value, updated_at)
        VALUES (?, ?, CURRENT_TIMESTAMP)
        ON CONFLICT(setting_key) DO UPDATE SET setting_value = excluded.setting_value, updated_at = CURRENT_TIMESTAMP
      `);
      Object.entries(values.settings).forEach(([key, value]) => setting.run(key, String(value)));
    }
    return { success: true };
  });

  ipcMain.handle('buildpay:payroll:record', (_event, record) => {
    return payroll.upsert(record);
  });
};

module.exports = { registerBuildPayService };
