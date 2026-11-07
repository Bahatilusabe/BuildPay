document.addEventListener('DOMContentLoaded', async () => {
  const databaseApi = window.BuildPayDatabase;
  const sidebarToggle = document.getElementById('sidebarToggle');
  const sidebarCollapseBtn = document.getElementById('sidebarCollapseBtn');
  const sidebar = document.getElementById('sidebar');
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
      const newWidth = Math.max(200, Math.min(500, startWidth + diff));
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
      if (dashMain) dashMain.classList.toggle('sidebar-collapsed');
    });
  }

  if (sidebarCollapseBtn && sidebar) {
    sidebarCollapseBtn.addEventListener('click', () => {
      sidebar.classList.toggle('collapsed');
      if (dashMain) dashMain.classList.toggle('sidebar-collapsed');
    });
  }

  navItems.forEach((item) => {
    item.addEventListener('click', () => {
      navItems.forEach((navItem) => navItem.classList.remove('active'));
      item.classList.add('active');
    });
  });

  const defaultPayroll = {
    employeeId: 'EMP001',
    employeeName: 'James Mwangi',
    position: 'Site Engineer',
    department: 'Engineering',
    payPeriod: 'May 2025',
    daysWorked: 26,
    overtimeHours: 12,
    basicSalary: 85000,
    overtimePay: 6000,
    transportAllowance: 5000,
    housingAllowance: 4000,
    totalEarnings: 100000,
    nssf: 1080,
    paye: 9000,
    advanceSalary: 5000,
    otherDeductions: 0,
    totalDeductions: 15080,
    netSalary: 84920
  };

  function loadSavedPayroll() {
    try {
      return { ...defaultPayroll, ...JSON.parse(localStorage.getItem('buildpaySavedPayroll')) };
    } catch (error) {
      return defaultPayroll;
    }
  }

  function formatAmount(value) {
    const amount = Math.round(Number(value) || 0);
    const locale = window.BuildPaySettings ? window.BuildPaySettings.getLocale() : 'en-KE';
    return amount.toLocaleString(locale);
  }

  const payroll = window.BuildPayPayroll
    ? window.BuildPayPayroll.calculate(loadSavedPayroll())
    : loadSavedPayroll();
  let payrollRecord = null;
  if (databaseApi?.listPayroll) {
    try {
      const selectedPayrollId = localStorage.getItem('buildpaySelectedPayrollId');
      const payrollRows = selectedPayrollId && databaseApi.getPayroll
        ? [await databaseApi.getPayroll(selectedPayrollId)]
        : await databaseApi.listPayroll('2025-05');
      const saved = payrollRows.find(Boolean);
      if (saved) {
        payrollRecord = saved;
        Object.assign(payroll, {
          employeeId: saved.employeeCode,
          employeeName: saved.employeeName,
          position: saved.position,
          department: saved.department,
          payPeriod: saved.pay_period,
          basicSalary: saved.basicSalary,
          overtimePay: saved.overtimePay,
          totalEarnings: saved.totalEarnings,
          totalDeductions: saved.totalDeductions,
          netSalary: saved.netSalary,
          deductions: saved.deductions
        });
        if (databaseApi.generatePayslip) {
          try {
            payrollRecord = await databaseApi.generatePayslip(saved.id) || saved;
          } catch (error) {
            console.error('Unable to record payslip release', error);
          }
        }
      }
      localStorage.removeItem('buildpaySelectedPayrollId');
      localStorage.removeItem('buildpayPayslipBatch');
    } catch (error) {
      console.error('Unable to load saved payroll for payslip', error);
    }
  }
  if (databaseApi?.getAttendanceSummary) {
    try {
      const summary = await databaseApi.getAttendanceSummary(
        payrollRecord?.pay_period || '2025-05',
        payroll.employeeId || 'EMP001'
      );
      payroll.daysWorked = summary.presentDays || 0;
      payroll.overtimeHours = summary.overtimeHours || 0;
    } catch (error) {
      console.error('Unable to load attendance summary for payslip', error);
    }
  }
  document.querySelectorAll('[data-payslip-field]').forEach((element) => {
    const value = payroll[element.dataset.payslipField];
    if (value === undefined) return;
    const monetaryFields = ['basicSalary', 'overtimePay', 'transportAllowance', 'housingAllowance', 'nssf', 'pension', 'healthInsurance', 'paye', 'advanceSalary', 'otherDeductions', 'totalEarnings', 'totalDeductions'];
    element.textContent = typeof value === 'number' && monetaryFields.includes(element.dataset.payslipField)
      ? (window.BuildPaySettings ? window.BuildPaySettings.formatCurrency(value) : formatAmount(value))
      : (typeof value === 'number' ? formatAmount(value) : value);
  });

  const netAmount = document.querySelector('[data-payslip-field="netSalary"]');
  if (netAmount) {
    netAmount.textContent = window.BuildPaySettings
      ? window.BuildPaySettings.formatCurrency(payroll.netSalary)
      : `KES ${formatAmount(payroll.netSalary)}`;
  }

  const overtimeLabel = document.querySelector('[data-payslip-label="overtime"]');
  if (overtimeLabel) overtimeLabel.textContent = `Overtime (${payroll.overtimeHours} hrs)`;

  window.addEventListener('storage', (event) => {
    if (event.key === 'buildpaySettings') window.location.reload();
  });

  const downloadBtn = document.getElementById('downloadPdfBtn');
  if (downloadBtn) {
    downloadBtn.addEventListener('click', () => {
      const preview = document.createElement('div');
      preview.className = 'payslip-preview-overlay';
      preview.innerHTML = `
        <div class="payslip-preview-dialog" role="dialog" aria-modal="true" aria-labelledby="payslipPreviewTitle">
          <div class="payslip-preview-header">
            <div>
              <span class="preview-eyebrow">Document preview</span>
              <h2 id="payslipPreviewTitle">Review payslip before download</h2>
            </div>
            <button class="preview-close-btn" type="button" aria-label="Close preview">&times;</button>
          </div>
          <div class="payslip-preview-content"></div>
          <div class="payslip-preview-actions">
            <button class="preview-cancel-btn" type="button">Cancel</button>
            <button class="preview-download-btn" type="button">Download PDF</button>
          </div>
        </div>`;

      const previewContent = preview.querySelector('.payslip-preview-content');
      const payslipCard = document.querySelector('.payslip-card');
      if (previewContent && payslipCard) previewContent.appendChild(payslipCard.cloneNode(true));

      const closePreview = () => preview.remove();
      preview.querySelector('.preview-close-btn').addEventListener('click', closePreview);
      preview.querySelector('.preview-cancel-btn').addEventListener('click', closePreview);
      preview.addEventListener('click', (event) => {
        if (event.target === preview) closePreview();
      });
      preview.querySelector('.preview-download-btn').addEventListener('click', () => {
        closePreview();
        window.print();
      });

      document.body.appendChild(preview);
      preview.querySelector('.preview-close-btn').focus();
    });
  }

  const printBtn = document.getElementById('printBtn');
  if (printBtn) {
    printBtn.addEventListener('click', () => {
      window.print();
    });
  }

  const reportBtn = document.getElementById('payrollReportBtn');
  if (reportBtn) {
    reportBtn.addEventListener('click', async () => {
      if (!databaseApi?.getPayrollReport) return;

      reportBtn.disabled = true;
      try {
        const rows = await databaseApi.getPayrollReport();
        const report = document.createElement('div');
        report.className = 'payslip-preview-overlay';
        const reportRows = rows.length
          ? rows.map((row) => `
              <tr>
                <td>${row.employeeCode || '-'}<small>${row.employeeName || '-'}</small></td>
                <td>${row.payPeriod || '-'}</td>
                <td>${window.BuildPaySettings ? window.BuildPaySettings.formatCurrency(row.netSalary) : `KES ${formatAmount(row.netSalary)}`}</td>
                <td><span class="report-status ${row.paymentStatus.toLowerCase()}">${row.paymentStatus}</span></td>
                <td>${row.releaseDate ? new Date(row.releaseDate.replace(' ', 'T') + (row.releaseDate.endsWith('Z') ? '' : 'Z')).toLocaleString() : '-'}</td>
              </tr>`).join('')
          : '<tr><td colspan="5" class="report-empty">No payroll records found.</td></tr>';
        report.innerHTML = `
          <div class="payslip-preview-dialog payroll-report-dialog" role="dialog" aria-modal="true" aria-labelledby="payrollReportTitle">
            <div class="payslip-preview-header">
              <div>
                <span class="preview-eyebrow">Database report</span>
                <h2 id="payrollReportTitle">Payroll payment report</h2>
              </div>
              <button class="preview-close-btn report-close-btn" type="button" aria-label="Close report">&times;</button>
            </div>
            <div class="payroll-report-summary"><strong>${rows.length}</strong> payroll record${rows.length === 1 ? '' : 's'} across all periods</div>
            <div class="payroll-report-table-wrap">
              <table class="payroll-report-table">
                <thead><tr><th>Employee</th><th>Period</th><th>Net salary</th><th>Payment</th><th>Payslip released</th></tr></thead>
                <tbody>${reportRows}</tbody>
              </table>
            </div>
          </div>`;
        const closeReport = () => report.remove();
        report.querySelector('.report-close-btn').addEventListener('click', closeReport);
        report.addEventListener('click', (event) => {
          if (event.target === report) closeReport();
        });
        document.body.appendChild(report);
        report.querySelector('.report-close-btn').focus();
      } catch (error) {
        console.error('Unable to load payroll report', error);
      } finally {
        reportBtn.disabled = false;
      }
    });
  }
});
