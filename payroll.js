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

  const payrollInputs = document.querySelectorAll('[data-payroll-field]');
  const payrollOutputs = document.querySelectorAll('[data-payroll-output]');
  const overtimeLabel = document.querySelector('[data-payroll-label="overtime"]');
  const resetBtn = document.querySelector('.btn-reset');
  const employeeSelect = document.getElementById('payrollEmployeeSelect');
  const generateAllBtn = document.querySelector('.generate-all-btn');
  const frequencySelect = document.getElementById('payrollFrequencySelect');
  const monthInput = document.getElementById('payrollMonthInput');
  const weekInput = document.getElementById('payrollWeekInput');
  const monthBox = document.getElementById('payrollMonthBox');
  const weekBox = document.getElementById('payrollWeekBox');
  let employees = [];
  let selectedEmployee = null;

  const isoWeekCode = (value) => {
    const date = new Date(`${value}T00:00:00Z`);
    const day = date.getUTCDay() || 7;
    date.setUTCDate(date.getUTCDate() + 4 - day);
    const yearStart = new Date(Date.UTC(date.getUTCFullYear(), 0, 1));
    const week = Math.ceil((((date - yearStart) / 86400000) + 1) / 7);
    return `${date.getUTCFullYear()}-W${String(week).padStart(2, '0')}`;
  };

  const getPeriod = () => {
    const frequency = frequencySelect?.value || window.BuildPaySettings?.get().payPeriod || 'Monthly';
    if (frequency === 'Weekly') {
      const start = weekInput?.value || '2025-05-05';
      return { code: isoWeekCode(start), label: `Week of ${start}` };
    }
    const month = monthInput?.value || '2025-05';
    const date = new Date(`${month}-01T00:00:00Z`);
    return { code: month, label: date.toLocaleString('en-GB', { month: 'long', year: 'numeric' }) };
  };

  const refreshPeriodControls = () => {
    const weekly = frequencySelect?.value === 'Weekly';
    if (monthBox) monthBox.hidden = weekly;
    if (weekBox) weekBox.hidden = !weekly;
    updatePayroll();
  };
  const defaultPayrollValues = {
    basicSalary: 85000,
    daysWorked: 26,
    overtimeHours: 12,
    transportAllowance: 5000,
    housingAllowance: 4000,
    nssf: 0,
    paye: 0,
    advanceSalary: 5000,
    otherDeductions: 0
  };

  function getPayrollValue(field) {
    const input = document.querySelector(`[data-payroll-field="${field}"]`);
    return Math.max(0, Number(input.value) || 0);
  }

  function formatCurrency(value) {
    if (window.BuildPaySettings) return window.BuildPaySettings.formatCurrency(value);
    return `KES ${Math.round(value).toLocaleString('en-KE')}`;
  }

  function updatePayroll() {
    const calculatedPayroll = calculateCurrentPayroll();

    payrollOutputs.forEach((output) => {
      output.textContent = formatCurrency(calculatedPayroll[output.dataset.payrollOutput]);
    });
    if (overtimeLabel) overtimeLabel.textContent = `Overtime (${calculatedPayroll.overtimeHours} hrs × ${calculatedPayroll.overtimeRate})`;
    const payrollPeriod = document.querySelector('[data-payroll-period]');
    if (payrollPeriod) payrollPeriod.textContent = getPeriod().label;
    const pensionInput = document.querySelector('[data-payroll-field="nssf"]');
    const payeInput = document.querySelector('[data-payroll-field="paye"]');
    if (pensionInput) pensionInput.value = Math.round(calculatedPayroll.pension);
    if (payeInput) payeInput.value = Math.round(calculatedPayroll.paye);
  }

  function calculateCurrentPayroll() {
    const values = {};
    payrollInputs.forEach((input) => {
      values[input.dataset.payrollField] = getPayrollValue(input.dataset.payrollField);
    });
    const calculated = window.BuildPayPayroll ? window.BuildPayPayroll.calculate(values) : values;
    calculated.payPeriod = getPeriod().label;
    return calculated;
  }

  async function loadAttendanceIntoPayroll() {
    if (!databaseApi?.getAttendanceSummary) return;
    try {
      const summary = await databaseApi.getAttendanceSummary(getPeriod().code, selectedEmployee?.employeeCode || 'EMP001');
      const daysWorked = document.querySelector('[data-payroll-field="daysWorked"]');
      const overtimeHours = document.querySelector('[data-payroll-field="overtimeHours"]');
      if (daysWorked && summary) daysWorked.value = summary.presentDays || 0;
      if (overtimeHours && summary) overtimeHours.value = summary.overtimeHours || 0;
      updatePayroll();
    } catch (error) {
      console.error('Unable to load attendance summary for payroll', error);
    }
  }

  window.addEventListener('storage', (event) => {
    if (event.key === 'buildpaySettings') updatePayroll();
  });

  payrollInputs.forEach((input) => input.addEventListener('input', updatePayroll));
  if (resetBtn) {
    resetBtn.addEventListener('click', () => {
      payrollInputs.forEach((input) => {
        input.value = defaultPayrollValues[input.dataset.payrollField];
      });
      updatePayroll();
    });
  }
  updatePayroll();
  if (frequencySelect) {
    frequencySelect.value = window.BuildPaySettings?.get().payPeriod === 'Weekly' ? 'Weekly' : 'Monthly';
    frequencySelect.addEventListener('change', refreshPeriodControls);
  }
  [monthInput, weekInput].forEach(input => input?.addEventListener('change', () => {
    updatePayroll();
    loadEmployeePayroll(selectedEmployee?.employeeCode);
  }));
  refreshPeriodControls();

  if (databaseApi?.listPayroll) {
    try {
      employees = databaseApi.listEmployees ? await databaseApi.listEmployees() : [];
      selectedEmployee = employees.find(record => record.employeeCode === 'EMP001') || employees[0];
      if (employeeSelect && employees.length) {
        employeeSelect.replaceChildren(...employees.map(record => {
          const option = document.createElement('option');
          option.value = record.employeeCode;
          option.textContent = `${record.name || `${record.firstName} ${record.lastName}`} (${record.employeeCode})`;
          return option;
        }));
        employeeSelect.value = selectedEmployee?.employeeCode || '';
      }
      const employee = selectedEmployee;
      if (employee) {
        const name = document.querySelector('.employee-name');
        const position = document.querySelector('.employee-meta:not(.dept)');
        const department = document.querySelector('.employee-meta.dept');
        const code = document.querySelector('.employee-id');
        if (name) name.textContent = `${employee.firstName} ${employee.lastName}`;
        if (position) position.textContent = employee.position || '';
        if (department) department.textContent = `${employee.department || ''} Department`;
        if (code) code.textContent = employee.employeeCode;
        const basicSalary = document.querySelector('[data-payroll-field="basicSalary"]');
        if (basicSalary) basicSalary.value = employee.salary || 0;
      }
      const saved = (await databaseApi.listPayroll(getPeriod().code))
        .find(record => record.employeeCode === selectedEmployee?.employeeCode);
      if (saved) {
        const values = {
          basicSalary: saved.basicSalary,
          overtimeHours: saved.overtimeHours || 0,
          transportAllowance: saved.transportAllowance || 0,
          housingAllowance: saved.housingAllowance || 0,
          nssf: saved.nssf || 0,
          paye: saved.paye || 0,
          advanceSalary: saved.advanceSalary || 0,
          otherDeductions: saved.otherDeductions || 0
        };
        Object.entries(values).forEach(([field, value]) => {
          const input = document.querySelector(`[data-payroll-field="${field}"]`);
          if (input && value !== undefined) input.value = value;
        });
        updatePayroll();
      }
    } catch (error) {
      console.error('Unable to load saved payroll from SQLite', error);
    }
  }

  async function loadEmployeePayroll(employeeCode) {
    selectedEmployee = employees.find(record => record.employeeCode === employeeCode) || employees[0];
    if (!selectedEmployee) return;
    document.querySelector('.employee-name').textContent = selectedEmployee.name || `${selectedEmployee.firstName} ${selectedEmployee.lastName}`;
    document.querySelector('.employee-meta:not(.dept)').textContent = selectedEmployee.position || '';
    document.querySelector('.employee-meta.dept').textContent = `${selectedEmployee.department || ''} Department`;
    document.querySelector('.employee-id').textContent = selectedEmployee.employeeCode;
    const basicSalary = document.querySelector('[data-payroll-field="basicSalary"]');
    if (basicSalary) basicSalary.value = selectedEmployee.salary || selectedEmployee.basicSalary || 0;
    const saved = (await databaseApi.listPayroll(getPeriod().code)).find(record => record.employeeCode === selectedEmployee.employeeCode);
    if (saved) {
      const values = { basicSalary: saved.basicSalary, nssf: saved.nssf || 0, paye: saved.paye || 0 };
      Object.entries(values).forEach(([field, value]) => {
        const input = document.querySelector(`[data-payroll-field="${field}"]`);
        if (input) input.value = value;
      });
    }
    await loadAttendanceIntoPayroll();
    updatePayroll();
  }

  if (employeeSelect) employeeSelect.addEventListener('change', () => loadEmployeePayroll(employeeSelect.value));

  async function saveCurrentPayroll() {
    const payroll = {
      employeeId: selectedEmployee?.employeeCode || 'EMP001',
      employeeName: selectedEmployee?.name || 'James Mwangi',
      position: selectedEmployee?.position || 'Site Engineer',
      department: selectedEmployee?.department || 'Engineering',
      payPeriod: getPeriod().code,
      basicSalary: getPayrollValue('basicSalary'),
      daysWorked: getPayrollValue('daysWorked'),
      overtimeHours: getPayrollValue('overtimeHours'),
      overtimePay: 0,
      transportAllowance: getPayrollValue('transportAllowance'),
      housingAllowance: getPayrollValue('housingAllowance'),
      nssf: 0,
      paye: 0,
      advanceSalary: getPayrollValue('advanceSalary'),
      otherDeductions: getPayrollValue('otherDeductions')
    };
    const calculatedPayroll = window.BuildPayPayroll
      ? window.BuildPayPayroll.calculate(payroll)
      : payroll;
    if (!databaseApi?.recordPayroll) {
      localStorage.setItem('buildpaySavedPayroll', JSON.stringify(calculatedPayroll));
      return { calculatedPayroll, saved: null };
    }
    const saved = await databaseApi.recordPayroll({
      ...calculatedPayroll,
      employeeId: payroll.employeeId,
      payPeriod: payroll.payPeriod,
      netSalary: calculatedPayroll.netSalary,
      shif: calculatedPayroll.healthInsurance || calculatedPayroll.shif || 0,
      deductions: calculatedPayroll.totalDeductions,
      allowances: (calculatedPayroll.transportAllowance || 0) + (calculatedPayroll.housingAllowance || 0),
      status: 'Draft'
    });
    if (saved?.id) localStorage.setItem('buildpaySelectedPayrollId', String(saved.id));
    localStorage.setItem('buildpaySavedPayroll', JSON.stringify(calculatedPayroll));
    return { calculatedPayroll, saved };
  }

  const saveBtn = document.querySelector('.btn-save');
  if (saveBtn) {
    saveBtn.addEventListener('click', async () => {
      try {
        await saveCurrentPayroll();
      } catch (error) {
        console.error('Unable to save payroll to SQLite', error);
      }
      window.location.href = 'payslip.html';
    });
  }

  const generateBtn = document.querySelector('.generate-btn');

  await loadAttendanceIntoPayroll();
  if (generateBtn) {
    generateBtn.addEventListener('click', async () => {
      await saveCurrentPayroll();
      window.location.href = 'payslip.html';
    });
  }

  if (generateAllBtn) {
    generateAllBtn.addEventListener('click', async () => {
      if (!databaseApi?.generatePayslips) return;
      try {
        await saveCurrentPayroll();
        const savedRows = await databaseApi.generatePayslips(getPeriod().code);
        localStorage.setItem('buildpayPayslipBatch', JSON.stringify(savedRows.map(row => row.id)));
        window.location.href = 'payslip.html';
      } catch (error) {
        console.error('Unable to save and generate all payslips', error);
      }
    });
  }
});
