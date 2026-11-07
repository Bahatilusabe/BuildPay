document.addEventListener('DOMContentLoaded', () => {
  const databaseApi = window.BuildPayDatabase;
  const sidebarToggle = document.getElementById('sidebarToggle');
  const sidebarCollapseBtn = document.getElementById('sidebarCollapseBtn');
  const sidebar = document.querySelector('.sidebar');
  const dashMain = document.querySelector('.dash-main');
  const navItems = document.querySelectorAll('.nav-item');
  const resizeHandle = document.getElementById('sidebarResizeHandle');

  const savedSidebarWidth = localStorage.getItem('sidebarWidth');
  if (savedSidebarWidth && sidebar) {
    sidebar.style.width = savedSidebarWidth + 'px';
  }

  if (resizeHandle && sidebar) {
    let isResizing = false;
    let startX = 0;
    let startWidth = 0;

    resizeHandle.addEventListener('mousedown', (event) => {
      isResizing = true;
      startX = event.clientX;
      startWidth = sidebar.offsetWidth;
      resizeHandle.classList.add('active');
      document.body.style.userSelect = 'none';
      document.body.style.cursor = 'col-resize';
    });

    document.addEventListener('mousemove', (event) => {
      if (!isResizing) return;
      const diff = event.clientX - startX;
      const newWidth = Math.max(200, Math.min(420, startWidth + diff));
      sidebar.style.width = newWidth + 'px';
      localStorage.setItem('sidebarWidth', newWidth);
    });

    document.addEventListener('mouseup', () => {
      if (isResizing) {
        isResizing = false;
        resizeHandle.classList.remove('active');
        document.body.style.userSelect = '';
        document.body.style.cursor = '';
      }
    });
  }

  if (sidebarToggle && sidebar) {
    sidebarToggle.addEventListener('click', () => {
      sidebar.classList.toggle('collapsed');
      dashMain.classList.toggle('sidebar-collapsed');
    });
  }

  if (sidebarCollapseBtn && sidebar) {
    sidebarCollapseBtn.addEventListener('click', () => {
      sidebar.classList.toggle('collapsed');
      dashMain.classList.toggle('sidebar-collapsed');
    });
  }

  navItems.forEach((item) => {
    item.addEventListener('click', () => {
      navItems.forEach((nav) => nav.classList.remove('active'));
      item.classList.add('active');
    });
  });

  const reportTypeFilter = document.getElementById('reportTypeFilter');
  const reportPeriodFilter = document.getElementById('reportPeriodFilter');
  const reportDepartmentFilter = document.getElementById('reportDepartmentFilter');
  const reportProjectFilter = document.getElementById('reportProjectFilter');
  const reportEmployeeFilter = document.getElementById('reportEmployeeFilter');
  const reportEmployeeField = document.getElementById('reportEmployeeField');
  const generateBtn = document.getElementById('generateReportBtn');
  const exportBtn = document.getElementById('exportBtn');
  let activeReport = null;
  let databaseProjects = [];
  let databasePayroll = [];
  let databaseEmployees = [];
  let databasePayrollTrend = [];
  let databaseReportSummary = null;

  const periodCode = () => ({
    'May 2025': '2025-05',
    'April 2025': '2025-04',
    'Week of 05 May 2025': '2025-W19'
  }[reportPeriodFilter?.value] || '2025-05');

  const formatMoney = (value) => `KES ${Math.round(Number(value) || 0).toLocaleString('en-KE')}`;

  const setReportTable = (headers, rows) => {
    document.getElementById('reportTableHeaders').innerHTML = headers.map(header => `<th>${header}</th>`).join('');
    document.getElementById('reportTableBody').innerHTML = rows.length
      ? rows.map(row => `<tr>${row.map(cell => `<td>${cell ?? '-'}</td>`).join('')}</tr>`).join('')
      : `<tr><td colspan="${headers.length}">No data found for this report.</td></tr>`;
  };

  const setAnalytics = (values) => {
    values.forEach(([value, label], index) => {
      const slot = ['primary', 'secondary', 'tertiary'][index];
      document.querySelector(`[data-report-analytics-value="${slot}"]`).textContent = value;
      document.querySelector(`[data-report-analytics-label="${slot}"]`).textContent = label;
    });
  };

  const formatPeriodLabel = (period) => {
    if (/^\d{4}-W\d{2}$/.test(period)) return period.replace('-', ' ');
    if (/^\d{4}-\d{2}$/.test(period)) {
      return new Date(`${period}-01T00:00:00Z`).toLocaleString('en-GB', { month: 'short', year: '2-digit' });
    }
    return period;
  };

  const renderPayrollChart = (trend) => {
    const groups = Array.from(document.querySelectorAll('.bar-group'));
    const axis = document.querySelector('.chart-axis');
    const rows = trend.length ? trend.slice(-groups.length) : [];
    const maximum = Math.max(1, ...rows.flatMap(row => [row.grossPay, row.deductions, row.netPay].map(Number)));
    groups.forEach((group, index) => {
      const row = rows[index];
      group.dataset.month = row ? formatPeriodLabel(row.payPeriod) : '-';
      group.querySelector('.gross').style.height = row ? `${Math.max(3, (Number(row.grossPay) / maximum) * 100)}%` : '0%';
      group.querySelector('.deduction').style.height = row ? `${Math.max(3, (Number(row.deductions) / maximum) * 100)}%` : '0%';
      group.querySelector('.net').style.height = row ? `${Math.max(3, (Number(row.netPay) / maximum) * 100)}%` : '0%';
    });
    if (axis) axis.replaceChildren(...rows.map(row => {
      const label = document.createElement('span');
      label.textContent = formatPeriodLabel(row.payPeriod);
      return label;
    }));
  };

  const renderPayrollBreakdown = (rows) => {
    if (!rows.length) return;
    const gross = rows.reduce((sum, row) => sum + Number(row.gross_salary || row.totalEarnings || 0), 0);
    const deductions = rows.reduce((sum, row) => sum + Number(row.total_deductions || row.deductions || 0), 0);
    const net = rows.reduce((sum, row) => sum + Number(row.net_salary || 0), 0);
    document.getElementById('reportSecondaryTitle').textContent = 'Payroll Breakdown';
    document.getElementById('reportSecondaryValue').textContent = formatMoney(gross);
    document.querySelector('.attendance-legend').innerHTML = `
      <div class="legend-row"><span class="dot blue"></span><span class="legend-label">Gross pay</span><strong>${formatMoney(gross)}</strong></div>
      <div class="legend-row"><span class="dot orange"></span><span class="legend-label">Deductions</span><strong>${formatMoney(deductions)}</strong></div>
      <div class="legend-row"><span class="dot green"></span><span class="legend-label">Net pay</span><strong>${formatMoney(net)}</strong></div>`;
  };

  const renderEmployeeDetail = async () => {
    if (!databaseApi?.getEmployeeReport || !reportEmployeeFilter?.value) return;
    const report = await databaseApi.getEmployeeReport(reportEmployeeFilter.value, periodCode());
    if (!report) return;
    const { employee, attendance, payroll } = report;
    const payDays = Number(attendance.payDays || 0);
    const overtime = Number(attendance.overtimeHours || 0);
    const gross = payroll?.grossSalary || 0;
    const deductions = payroll?.totalDeductions || 0;
    const net = payroll?.netSalary || 0;
    document.querySelector('[data-report-kpi-title="employees"]').textContent = 'Employee';
    document.querySelector('[data-report-kpi-value="employees"]').textContent = employee.name;
    document.querySelector('[data-report-kpi-title="payroll"]').textContent = 'Net Pay';
    document.querySelector('[data-report-kpi-value="payroll"]').textContent = formatMoney(net);
    document.querySelector('[data-report-kpi-title="attendance"]').textContent = 'Pay Days';
    document.querySelector('[data-report-kpi-value="attendance"]').textContent = String(payDays);
    document.querySelector('[data-report-kpi-title="projects"]').textContent = 'Overtime';
    document.querySelector('[data-report-kpi-value="projects"]').textContent = `${overtime} hrs`;
    document.getElementById('reportChartTitle').textContent = `Employee Activity - ${report.period}`;
    document.getElementById('reportSecondaryTitle').textContent = 'Attendance Breakdown';
    document.getElementById('reportSecondaryValue').textContent = `${payDays} days`;
    document.getElementById('reportTableTitle').textContent = `${employee.name} - Detailed Payroll Report`;
    setReportTable(
      ['Employee', 'Code', 'Position', 'Department', 'Supervisor', 'Pay days', 'Overtime', 'Gross pay', 'Deductions', 'Net pay', 'Status'],
      [[employee.name, employee.employeeCode, employee.position || '-', employee.department || '-', employee.supervisorName || 'Not assigned', payDays, `${overtime} hrs`, formatMoney(gross), formatMoney(deductions), formatMoney(net), payroll?.status || 'No payroll']]
    );
    setAnalytics([
      [formatMoney(gross), 'Gross Pay'],
      [formatMoney(deductions), 'Deductions'],
      [formatMoney(net), 'Net Pay']
    ]);
    document.getElementById('reportAnalyticsTitle').textContent = 'Employee Financial Analysis';
    document.getElementById('reportAnalyticsNote').textContent = `${employee.position || 'Employee'} in ${employee.department || 'Unassigned'}; supervisor: ${employee.supervisorName || 'Not assigned'}. Period: ${report.period}.`;
    activeReport = {
      name: 'Employee Detail',
      headers: ['Employee', 'Code', 'Position', 'Department', 'Supervisor', 'Pay days', 'Overtime', 'Gross pay', 'Deductions', 'Net pay', 'Status'],
      rows: [[employee.name, employee.employeeCode, employee.position || '-', employee.department || '-', employee.supervisorName || 'Not assigned', payDays, `${overtime} hrs`, formatMoney(gross), formatMoney(deductions), formatMoney(net), payroll?.status || 'No payroll']]
    };
  };

  const renderProjectDetail = async () => {
    if (!databaseApi?.getProjectReport || !reportProjectFilter?.value || reportProjectFilter.value === 'All Projects') return;
    const report = await databaseApi.getProjectReport(reportProjectFilter.value);
    if (!report) return;
    const { project, employees, attendance, payroll } = report;
    const workerCount = employees.length || Number(project.workerCount || 0);
    document.querySelector('[data-report-kpi-title="employees"]').textContent = 'Workers';
    document.querySelector('[data-report-kpi-value="employees"]').textContent = String(workerCount);
    document.querySelector('[data-report-kpi-title="payroll"]').textContent = 'Project Budget';
    document.querySelector('[data-report-kpi-value="payroll"]').textContent = formatMoney(project.budget);
    document.querySelector('[data-report-kpi-title="attendance"]').textContent = 'Labor Cost';
    document.querySelector('[data-report-kpi-value="attendance"]').textContent = formatMoney(project.laborCost);
    document.querySelector('[data-report-kpi-title="projects"]').textContent = 'Progress';
    document.querySelector('[data-report-kpi-value="projects"]').textContent = `${project.progress || 0}%`;
    document.getElementById('reportChartTitle').textContent = `${project.name} - Project Analysis`;
    document.getElementById('reportSecondaryTitle').textContent = 'Workforce Activity';
    document.getElementById('reportSecondaryValue').textContent = `${attendance.payDays || 0} days`;
    document.getElementById('reportTableTitle').textContent = `${project.name} - Workforce`; 
    setReportTable(
      ['Project', 'Code', 'Location', 'Client', 'Manager', 'Status', 'Progress', 'Budget', 'Labor cost', 'Assigned workforce'],
      [[project.name, project.projectCode, project.location || '-', project.client || '-', project.manager || 'Not assigned', project.status, `${project.progress || 0}%`, formatMoney(project.budget), formatMoney(project.laborCost), employees.map(employee => employee.name).join('; ') || `${workerCount} workers recorded`]]
    );
    setAnalytics([
      [formatMoney(project.budget), 'Budget'],
      [formatMoney(project.laborCost), 'Labor Cost'],
      [formatMoney(payroll.netPayroll), 'Assigned Net Payroll']
    ]);
    document.getElementById('reportAnalyticsTitle').textContent = 'Project Financial Analysis';
    document.getElementById('reportAnalyticsNote').textContent = `${project.location || 'Location not recorded'}; client: ${project.client || 'Not recorded'}; manager: ${project.manager || 'Not assigned'}. ${attendance.overtimeHours || 0} overtime hours recorded.`;
    activeReport = {
      name: 'Project Detail',
      headers: ['Project', 'Code', 'Location', 'Client', 'Manager', 'Status', 'Progress', 'Budget', 'Labor cost', 'Assigned workforce'],
      rows: [[project.name, project.projectCode, project.location || '-', project.client || '-', project.manager || 'Not assigned', project.status, `${project.progress || 0}%`, formatMoney(project.budget), formatMoney(project.laborCost), employees.map(employee => employee.name).join('; ') || `${workerCount} workers recorded`]]
    };
  };

  const getFilterContext = () => {
    const period = reportPeriodFilter ? reportPeriodFilter.value : 'May 2025';
    const department = reportDepartmentFilter ? reportDepartmentFilter.value : 'All Departments';
    const project = reportProjectFilter ? reportProjectFilter.value : 'All Projects';
    return { period, department, project };
  };

  const adjustValue = (value, factor) => {
    const currencyMatch = String(value).match(/^(KES|USD|GBP)\s?([\d,.]+)([MK]?)$/);
    if (currencyMatch) {
      const multiplier = currencyMatch[3] === 'M' ? 1000000 : currencyMatch[3] === 'K' ? 1000 : 1;
      const amount = Number(currencyMatch[2].replaceAll(',', '')) * multiplier * factor;
      if (amount >= 1000000) return `${currencyMatch[1]} ${(amount / 1000000).toFixed(1)}M`;
      if (amount >= 1000) return `${currencyMatch[1]} ${(amount / 1000).toFixed(1)}K`;
      return `${currencyMatch[1]} ${Math.round(amount).toLocaleString()}`;
    }
    const percentMatch = String(value).match(/^(\d+(?:\.\d+)?)%$/);
    if (percentMatch) return `${Math.min(100, Math.round(Number(percentMatch[1]) * factor))}%`;
    if (/^\d[\d,]*$/.test(String(value))) return Math.max(0, Math.round(Number(String(value).replaceAll(',', '')) * factor)).toLocaleString();
    return value;
  };

  const renderReport = (reportName) => {
    const summary = databaseReportSummary;
    if (!summary) return;
    const payroll = summary.payroll || {};
    const attendance = summary.attendance || {};
    const employees = summary.employees || [];
    const projects = summary.projects || [];
    const attendanceTotal = Number(attendance.presentDays || 0) + Number(attendance.absentDays || 0) + Number(attendance.leaveDays || 0);
    const attendanceRate = attendanceTotal ? Math.round((Number(attendance.presentDays || 0) / attendanceTotal) * 100) : 0;
    const activeEmployees = employees.filter(employee => String(employee.status).toUpperCase() === 'ACTIVE').length;
    const departmentCount = new Set(employees.map(employee => employee.department).filter(Boolean)).size;
    const averageSalary = employees.length ? employees.reduce((sum, employee) => sum + Number(employee.salary || 0), 0) / employees.length : 0;
    const totalBudget = projects.reduce((sum, project) => sum + Number(project.budget || 0), 0);
    const totalLaborCost = projects.reduce((sum, project) => sum + Number(project.laborCost || 0), 0);
    const averageSpent = projects.length ? Math.round(projects.reduce((sum, project) => sum + (project.budget ? (Number(project.laborCost || 0) / Number(project.budget)) * 100 : 0), 0) / projects.length) : 0;
    const reportRowsByType = {
      'Payroll Summary': {
        headers: ['Employee', 'Gross Pay', 'Deductions', 'Net Pay'],
        rows: databasePayroll.map(row => [row.employeeName, formatMoney(row.gross_salary || row.totalEarnings), formatMoney(row.total_deductions || row.deductions), formatMoney(row.net_salary)])
      },
      'Attendance Summary': {
        headers: ['Employee', 'Present', 'Absent', 'Attendance Rate'],
        rows: employees.map(row => {
          const total = Number(row.presentDays || 0) + Number(row.absentDays || 0) + Number(row.leaveDays || 0);
          return [row.employeeName, `${row.presentDays || 0} days`, `${row.absentDays || 0} days`, `${total ? Math.round((Number(row.presentDays || 0) / total) * 100) : 0}%`];
        })
      },
      'Project Cost': {
        headers: ['Project', 'Budget', 'Labor Cost', 'Spent %'],
        rows: projects.map(row => [row.name, formatMoney(row.budget), formatMoney(row.laborCost), `${row.budget ? Math.round((Number(row.laborCost || 0) / Number(row.budget)) * 100) : 0}%`])
      },
      'Employee Summary': {
        headers: ['Employee', 'Department', 'Salary', 'Status'],
        rows: employees.map(row => [row.employeeName, row.department || '-', formatMoney(row.salary), row.status])
      },
      'Tax Summary': {
        headers: ['Tax Type', 'Amount'],
        rows: [['PAYE', formatMoney(payroll.paye)], ['Pension', formatMoney(payroll.nssf)], ['Health Insurance', formatMoney(payroll.shif)]]
      }
    };
    const reportRows = reportRowsByType[reportName] || reportRowsByType['Payroll Summary'];
    const kpisByType = {
      'Payroll Summary': {
        employees: ['Payroll Records', String(payroll.payrollRecords || 0)],
        payroll: ['Gross Payroll', formatMoney(payroll.grossPay)],
        attendance: ['Net Payroll', formatMoney(payroll.netPay)],
        projects: ['Pay Period', summary.period]
      },
      'Attendance Summary': {
        employees: ['Employees', String(employees.length)],
        payroll: ['Attendance Rate', `${attendanceRate}%`],
        attendance: ['Present Days', String(attendance.presentDays || 0)],
        projects: ['Late Arrivals', String(attendance.lateDays || 0)]
      },
      'Project Cost': {
        employees: ['Projects', String(projects.length)],
        payroll: ['Total Budget', formatMoney(totalBudget)],
        attendance: ['Labor Cost', formatMoney(totalLaborCost)],
        projects: ['Average Spent', `${averageSpent}%`]
      },
      'Employee Summary': {
        employees: ['Total Employees', String(employees.length)],
        payroll: ['Active Employees', String(activeEmployees)],
        attendance: ['Average Salary', formatMoney(averageSalary)],
        projects: ['Departments', String(departmentCount)]
      },
      'Tax Summary': {
        employees: ['Taxable Employees', String(payroll.payrollRecords || 0)],
        payroll: ['PAYE Tax', formatMoney(payroll.paye)],
        attendance: ['Pension', formatMoney(payroll.nssf)],
        projects: ['Health Insurance', formatMoney(payroll.shif)]
      }
    };
    const report = {
      chartTitle: reportName,
      secondaryTitle: reportName === 'Attendance Summary' ? 'Attendance Overview' : 'Payroll Breakdown',
      tableTitle: reportName,
      headers: reportRows.headers,
      rows: reportRows.rows,
      kpis: kpisByType[reportName] || kpisByType['Payroll Summary'],
      note: 'Values are loaded from the BuildPay database for the selected period.',
      analytics: reportName === 'Payroll Summary'
        ? [[formatMoney(payroll.grossPay), 'Gross Payroll'], [formatMoney(payroll.deductions), 'Deductions'], [formatMoney(payroll.netPay), 'Net Payroll']]
        : reportName === 'Project Cost'
        ? [[formatMoney(totalBudget), 'Total Budget'], [formatMoney(totalLaborCost), 'Labor Cost'], [`${averageSpent}%`, 'Average Spent']]
        : reportName === 'Tax Summary'
        ? [[formatMoney(payroll.paye), 'PAYE'], [formatMoney(payroll.nssf), 'Pension'], [formatMoney(payroll.shif), 'Health Insurance']]
        : [[String(employees.length), 'Employees'], [String(activeEmployees), 'Active'], [String(departmentCount), 'Departments']]
    };
    const context = getFilterContext();
    if (reportName === 'Payroll Summary') {
      renderPayrollChart(databasePayrollTrend);
      renderPayrollBreakdown(databasePayroll);
    }
    const scopeLabel = context.project === 'All Projects' ? context.department : context.project;
    activeReport = { name: reportName, ...report, context };
    Object.entries(report.kpis).forEach(([key, values]) => {
      const title = document.querySelector(`[data-report-kpi-title="${key}"]`);
      const value = document.querySelector(`[data-report-kpi-value="${key}"]`);
      if (title) title.textContent = values[0];
      if (value) value.textContent = values[1];
    });
    document.getElementById('reportChartTitle').textContent = `${report.chartTitle} - ${context.period}`;
    document.getElementById('reportSecondaryTitle').textContent = report.secondaryTitle;
    document.getElementById('reportSecondaryValue').textContent = reportName === 'Attendance Summary' ? `${attendanceRate}%` : formatMoney(payroll.grossPay);
    document.getElementById('reportTableTitle').textContent = `${report.tableTitle} - ${scopeLabel}`;
    document.getElementById('reportTableHeaders').innerHTML = report.headers.map((header) => `<th>${header}</th>`).join('');
    const filteredRows = context.project === 'All Projects'
      ? reportRows
      : reportRows.filter((row) => row[0] === context.project);
    const rowsToRender = filteredRows.length ? filteredRows : reportRows;
    const displayRows = rowsToRender;
    document.getElementById('reportTableBody').innerHTML = displayRows.map((row) => `<tr>${row.map((cell) => `<td>${cell}</td>`).join('')}</tr>`).join('');
    report.analytics.forEach(([value, label], index) => {
      const slot = ['primary', 'secondary', 'tertiary'][index];
      document.querySelector(`[data-report-analytics-value="${slot}"]`).textContent = value;
      document.querySelector(`[data-report-analytics-label="${slot}"]`).textContent = `${label} - ${scopeLabel}`;
    });
    document.getElementById('reportAnalyticsTitle').textContent = `${reportName} Analytics`;
    document.getElementById('reportAnalyticsNote').textContent = `${report.note} Period: ${context.period}. Department: ${context.department}. Project: ${context.project}.`;
    activeReport.headers = report.headers;
    activeReport.rows = displayRows;
  };

  const refreshAttendanceKpi = async () => {
    if (!databaseApi?.getAttendanceSummary) return;
    try {
      const periodMap = { 'May 2025': '2025-05', 'April 2025': '2025-04', 'Week of 05 May 2025': '2025-W19' };
      const summary = await databaseApi.getAttendanceSummary(periodMap[reportPeriodFilter?.value] || '2025-05');
      const total = (summary.presentDays || 0) + (summary.absentDays || 0) + (summary.leaveDays || 0) + (summary.lateDays || 0);
      const rate = total ? Math.round(((summary.presentDays || 0) / total) * 100) : 0;
      const attendanceValue = document.querySelector('[data-report-kpi-value="attendance"]');
      if (attendanceValue) attendanceValue.textContent = `${rate}%`;
    } catch (error) {
      console.error('Unable to load attendance summary for reports', error);
    }
  };

  const refreshDatabaseKpis = async () => {
    if (!databaseReportSummary) return;
    renderReport(reportTypeFilter?.value || 'Payroll Summary');
  };

  const refreshReportSummary = async () => {
    if (!databaseApi?.getReportSummary) return;
    try {
      databaseReportSummary = await databaseApi.getReportSummary(periodCode());
    } catch (error) {
      console.error('Unable to load report summary from SQLite', error);
    }
  };

  const refreshReportData = async () => {
    await Promise.all([refreshPayroll(), refreshReportSummary()]);
    renderSelectedReport();
  };

  const refreshProjects = async () => {
    if (!databaseApi?.listProjects || !reportProjectFilter) return;
    try {
      databaseProjects = await databaseApi.listProjects();
      reportProjectFilter.replaceChildren(new Option('All Projects', 'All Projects'));
      databaseProjects.forEach((project) => {
        reportProjectFilter.add(new Option(project.name, project.name));
      });
    } catch (error) {
      console.error('Unable to load projects for reports', error);
    }
  };

  const refreshEmployees = async () => {
    if (!databaseApi?.listEmployees || !reportEmployeeFilter) return;
    try {
      databaseEmployees = await databaseApi.listEmployees();
      reportEmployeeFilter.replaceChildren(new Option('Select employee', ''));
      databaseEmployees.forEach((employee) => {
        const name = employee.name || `${employee.firstName} ${employee.lastName}`;
        reportEmployeeFilter.add(new Option(`${name} (${employee.employeeCode})`, employee.employeeCode));
      });
      if (databaseEmployees.length) reportEmployeeFilter.value = databaseEmployees[0].employeeCode;
      if (reportDepartmentFilter) {
        const departments = [...new Set(databaseEmployees.map(employee => employee.department).filter(Boolean))].sort();
        reportDepartmentFilter.replaceChildren(new Option('All Departments', 'All Departments'));
        departments.forEach(department => reportDepartmentFilter.add(new Option(department, department)));
      }
    } catch (error) {
      console.error('Unable to load employees for reports', error);
    }
  };

  const refreshPayroll = async () => {
    if (!databaseApi?.listPayroll) return;
    try {
      databasePayroll = await databaseApi.listPayroll(periodCode());
      databasePayrollTrend = databaseApi.getPayrollTrend ? await databaseApi.getPayrollTrend(12) : [];
      renderPayrollChart(databasePayrollTrend);
    } catch (error) {
      console.error('Unable to load payroll for reports', error);
    }
  };

  const renderSelectedReport = async () => {
    const reportName = reportTypeFilter?.value || 'Payroll Summary';
    if (reportEmployeeField) reportEmployeeField.hidden = reportName !== 'Employee Detail';
    if (reportName === 'Employee Detail') {
      await renderEmployeeDetail();
      return;
    }
    if (reportName === 'Project Detail' && reportProjectFilter?.value !== 'All Projects') {
      await renderProjectDetail();
      return;
    }
    renderReport(reportName === 'Project Detail' ? 'Project Cost' : reportName);
  };

  if (reportTypeFilter) {
    reportTypeFilter.addEventListener('change', () => renderSelectedReport());
    renderSelectedReport();
    Promise.all([refreshProjects(), refreshEmployees(), refreshReportData()]).then(() => renderSelectedReport());
    refreshAttendanceKpi();
    refreshDatabaseKpis();
  }

  [reportPeriodFilter, reportDepartmentFilter, reportProjectFilter, reportEmployeeFilter].forEach((filter) => {
    if (filter) filter.addEventListener('change', () => {
      renderSelectedReport();
      refreshAttendanceKpi();
      refreshReportData();
    });
  });

  if (generateBtn) {
    generateBtn.addEventListener('click', () => {
      renderSelectedReport();
      generateBtn.textContent = 'Report Generated';
      generateBtn.classList.add('success-state');
      setTimeout(() => {
        generateBtn.textContent = 'Generate Report';
        generateBtn.classList.remove('success-state');
      }, 1800);
    });
  }

  if (exportBtn) {
    exportBtn.addEventListener('click', () => {
      if (!activeReport) renderReport(reportTypeFilter.value);
      const csvRows = [['Report', activeReport.name], activeReport.headers, ...activeReport.rows];
      const csv = csvRows.map((row) => row.map((cell) => `"${String(cell).replaceAll('"', '""')}"`).join(',')).join('\n');
      const link = document.createElement('a');
      link.href = 'data:text/csv;charset=utf-8,' + encodeURIComponent(csv);
      link.download = `buildpay-${activeReport.name.toLowerCase().replaceAll(' ', '-')}.csv`;
      link.click();
    });
  }
});
