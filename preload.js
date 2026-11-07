const { contextBridge, ipcRenderer } = require('electron');

contextBridge.exposeInMainWorld('BuildPayDatabase', {
  health: () => ipcRenderer.invoke('buildpay:database:health'),
  getOverview: () => ipcRenderer.invoke('buildpay:overview'),
  listEmployees: () => ipcRenderer.invoke('buildpay:employees:list'),
  createEmployee: (employee) => ipcRenderer.invoke('buildpay:employees:create', employee),
  updateEmployee: (id, employee) => ipcRenderer.invoke('buildpay:employees:update', id, employee),
  deleteEmployee: (id) => ipcRenderer.invoke('buildpay:employees:delete', id),
  listDepartments: () => ipcRenderer.invoke('buildpay:departments:list'),
  createDepartment: (values) => ipcRenderer.invoke('buildpay:departments:create', values),
  updateDepartment: (id, values) => ipcRenderer.invoke('buildpay:departments:update', id, values),
  deleteDepartment: (id) => ipcRenderer.invoke('buildpay:departments:delete', id),
  getEmployeeReport: (employeeId, period) => ipcRenderer.invoke('buildpay:reports:employee', employeeId, period),
  getProjectReport: (projectId) => ipcRenderer.invoke('buildpay:reports:project', projectId),
  getReportSummary: (period) => ipcRenderer.invoke('buildpay:reports:summary', period),
  listAttendance: (month) => ipcRenderer.invoke('buildpay:attendance:list', month),
  getAttendanceSummary: (period, employeeId) => ipcRenderer.invoke('buildpay:attendance:summary', period, employeeId),
  recordAttendance: (record) => ipcRenderer.invoke('buildpay:attendance:record', record),
  getDashboardSummary: (period) => ipcRenderer.invoke('buildpay:dashboard:summary', period),
  getDashboardActivities: (limit) => ipcRenderer.invoke('buildpay:dashboard:activities', limit),
  getDashboardProjects: () => ipcRenderer.invoke('buildpay:dashboard:projects'),
  listProjects: () => ipcRenderer.invoke('buildpay:projects:list'),
  createProject: (project) => ipcRenderer.invoke('buildpay:projects:create', project),
  updateProject: (id, project) => ipcRenderer.invoke('buildpay:projects:update', id, project),
  deleteProject: (id) => ipcRenderer.invoke('buildpay:projects:delete', id),
  listProjectFiles: (projectId, kind) => ipcRenderer.invoke('buildpay:projects:files:list', projectId, kind),
  saveProjectFile: (projectId, file) => ipcRenderer.invoke('buildpay:projects:files:save', projectId, file),
  deleteProjectFile: (id) => ipcRenderer.invoke('buildpay:projects:files:delete', id),
  readProjectFile: (id) => ipcRenderer.invoke('buildpay:projects:files:read', id),
  getSettings: () => ipcRenderer.invoke('buildpay:settings:get'),
  saveSettings: (values) => ipcRenderer.invoke('buildpay:settings:save', values),
  listPayroll: (period) => ipcRenderer.invoke('buildpay:payroll:list', period),
  getPayrollTrend: (limit) => ipcRenderer.invoke('buildpay:payroll:trend', limit),
  getPayroll: (id) => ipcRenderer.invoke('buildpay:payroll:get', id),
  getPayrollReport: (period) => ipcRenderer.invoke('buildpay:payroll:report', period),
  generatePayslip: (payrollId) => ipcRenderer.invoke('buildpay:payslip:generate', payrollId),
  generatePayslips: (period) => ipcRenderer.invoke('buildpay:payslips:generate', period),
  recordPayroll: (record) => ipcRenderer.invoke('buildpay:payroll:record', record)
});

window.addEventListener('DOMContentLoaded', () => {
  const replaceText = (selector, text) => {
    const element = document.getElementById(selector);
    if (element) element.innerText = text;
  };

  for (const dependency of ['chrome', 'node', 'electron']) {
    replaceText(`${dependency}-version`, process.versions[dependency]);
  }
});
