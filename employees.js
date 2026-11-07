// Employees page interactivity
document.addEventListener('DOMContentLoaded', () => {
  const databaseApi = window.BuildPayDatabase;
  const sidebarToggle = document.getElementById('sidebarToggle');
  const sidebarCollapseBtn = document.getElementById('sidebarCollapseBtn');
  const sidebar = document.querySelector('.sidebar');
  const dashMain = document.querySelector('.dash-main');
  const navItems = document.querySelectorAll('.nav-item');
  const resizeHandle = document.getElementById('sidebarResizeHandle');
  const searchInput = document.querySelector('.search-input');
  const filterSelects = document.querySelectorAll('.filter-select');
  const departmentFilter = document.getElementById('departmentFilter');
  const statusFilter = document.getElementById('statusFilter');
  const supervisorFilter = document.getElementById('supervisorFilter');
  const addEmployeeBtn = document.getElementById('addEmployeeBtn');
  const employeeModal = document.getElementById('employeeModal');
  const employeeViewModal = document.getElementById('employeeViewModal');
  const employeeForm = document.getElementById('employeeForm');
  const employeeModalTitle = document.getElementById('employeeModalTitle');
  const employeeModalDescription = document.getElementById('employeeModalDescription');
  const employeeFormSubmit = document.getElementById('employeeFormSubmit');
  const employeeNationalIdInput = document.getElementById('employeeNationalIdInput');
  const employeeNameInput = document.getElementById('employeeNameInput');
  const employeePositionInput = document.getElementById('employeePositionInput');
  const employeeDepartmentInput = document.getElementById('employeeDepartmentInput');
  const employeeSupervisorInput = document.getElementById('employeeSupervisorInput');
  const employeeSalaryInput = document.getElementById('employeeSalaryInput');
  const employeeStatusInput = document.getElementById('employeeStatusInput');
  const viewEmployeeId = document.getElementById('viewEmployeeId');
  const viewEmployeeName = document.getElementById('viewEmployeeName');
  const viewEmployeePosition = document.getElementById('viewEmployeePosition');
  const viewEmployeeDepartment = document.getElementById('viewEmployeeDepartment');
  const viewEmployeeSupervisor = document.getElementById('viewEmployeeSupervisor');
  const viewEmployeeSalary = document.getElementById('viewEmployeeSalary');
  const viewEmployeeStatus = document.getElementById('viewEmployeeStatus');
  let editingRow = null;
  let databaseEmployeeMode = false;
  let employeeRecords = [];

  // Load saved sidebar width from localStorage
  const savedSidebarWidth = localStorage.getItem('sidebarWidth');
  if (savedSidebarWidth && sidebar) {
    sidebar.style.width = savedSidebarWidth + 'px';
  }

  // Sidebar resize functionality
  if (resizeHandle && sidebar) {
    let isResizing = false;
    let startX = 0;
    let startWidth = 0;

    resizeHandle.addEventListener('mousedown', (e) => {
      isResizing = true;
      startX = e.clientX;
      startWidth = sidebar.offsetWidth;
      resizeHandle.classList.add('active');
      document.body.style.userSelect = 'none';
      document.body.style.cursor = 'col-resize';
    });

    document.addEventListener('mousemove', (e) => {
      if (!isResizing) return;

      const diff = e.clientX - startX;
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

  // Sidebar toggle functionality
  if (sidebarToggle && sidebar) {
    sidebarToggle.addEventListener('click', () => {
      sidebar.classList.toggle('collapsed');
      dashMain.classList.toggle('sidebar-collapsed');
    });
  }

  // Sidebar collapse button functionality
  if (sidebarCollapseBtn && sidebar) {
    sidebarCollapseBtn.addEventListener('click', () => {
      sidebar.classList.toggle('collapsed');
      dashMain.classList.toggle('sidebar-collapsed');
    });
  }

  // Navigation item handlers
  navItems.forEach(item => {
    item.addEventListener('click', () => {
      navItems.forEach(i => i.classList.remove('active'));
      item.classList.add('active');
      // Let default link navigation occur
    });
  });

  function bindActionIcon(icon) {
    icon.addEventListener('click', (e) => {
      e.preventDefault();
      const title = icon.getAttribute('title');
      const row = icon.closest('tr');
      const employeeId = row.querySelector('.cell-id').textContent;
      const employeeName = row.querySelector('.cell-name').textContent;

      if (title === 'Edit') {
        handleEditEmployee(employeeId, employeeName);
      } else if (title === 'View') {
        handleViewEmployee(employeeId, employeeName);
      } else if (title === 'Delete') {
        handleDeleteEmployee(employeeId, employeeName);
      }
    });
  }

  document.querySelectorAll('.action-icon').forEach(bindActionIcon);

  // Add employee button handler
  if (addEmployeeBtn) {
    addEmployeeBtn.addEventListener('click', () => {
      handleAddEmployee();
    });
  }

  // Handler functions
  function isEmployeeSupervisor(employeeId, records = employeeRecords) {
    return records.some(record => String(record.id) !== String(employeeId) && String(record.supervisorId) === String(employeeId));
  }

  function getEmployeeDisplayName(employee) {
    if (employee.name) return employee.name;
    if (employee.firstName || employee.lastName) {
      return `${employee.firstName || ''} ${employee.lastName || ''}`.trim();
    }
    return 'Unnamed Employee';
  }

  function handleEditEmployee(id, name) {
    const row = tableRows.find(employeeRow => employeeRow.querySelector('.cell-id').textContent.trim() === id);
    editingRow = row;
    openEmployeeModal('edit', row, name || row?.dataset.employeeName || '');
  }

  function handleViewEmployee(id, name) {
    const row = tableRows.find(employeeRow => employeeRow.querySelector('.cell-id').textContent.trim() === id);
    if (!row) return;

    viewEmployeeId.textContent = row.querySelector('.cell-id').textContent.trim();
    viewEmployeeName.textContent = row.dataset.employeeName || row.querySelector('.cell-name').textContent.trim();
    viewEmployeePosition.textContent = row.querySelector('.cell-position').textContent.trim();
    viewEmployeeDepartment.textContent = row.querySelector('.cell-department').textContent.trim();
    viewEmployeeSupervisor.textContent = row.dataset.supervisorName || 'Not assigned';
    viewEmployeeSalary.textContent = row.querySelector('.cell-salary').textContent.trim();
    viewEmployeeStatus.textContent = row.querySelector('.cell-status').textContent.trim();
    employeeViewModal.hidden = false;
  }

  async function handleDeleteEmployee(id, name) {
    if (confirm(`Are you sure you want to delete ${name} (${id})?`)) {
      const rowIndex = tableRows.findIndex(row => row.querySelector('.cell-id').textContent.trim() === id);
      const initialRowIndex = initialTableRows.findIndex(row => row.querySelector('.cell-id').textContent.trim() === id);
      if (rowIndex === -1) return;

      if (databaseEmployeeMode && databaseApi?.deleteEmployee) {
        const deleted = await databaseApi.deleteEmployee(tableRows[rowIndex].dataset.employeeId);
        if (!deleted) return;
      }

      tableRows[rowIndex].remove();
      tableRows.splice(rowIndex, 1);
      if (initialRowIndex !== -1) initialTableRows.splice(initialRowIndex, 1);
      totalRows = tableRows.length;
      refreshTable();
    }
  }

  function handleAddEmployee() {
    editingRow = null;
    openEmployeeModal('add');
  }

  function openEmployeeModal(mode, row = null, name = '') {
    employeeModalTitle.textContent = mode === 'edit' ? 'Edit Employee' : 'Add Employee';
    employeeModalDescription.textContent = mode === 'edit' ? `Update ${name}'s employee record.` : 'Create a new employee record.';
    employeeFormSubmit.textContent = mode === 'edit' ? 'Save Changes' : 'Add Employee';

    if (row) {
      employeeNameInput.value = row.dataset.employeeName || row.querySelector('.cell-name').textContent.trim();
      employeeNationalIdInput.value = row.dataset.nationalId || row.querySelector('.cell-id').textContent.trim();
      employeePositionInput.value = row.querySelector('.cell-position').textContent.trim();
      employeeDepartmentInput.value = row.querySelector('.cell-department').textContent.trim();
      employeeSalaryInput.value = row.querySelector('.cell-salary').textContent.replace(/,/g, '').trim();
      employeeStatusInput.value = row.querySelector('.cell-status').textContent.trim();
      populateSupervisorOptions(row.dataset.employeeId, row.dataset.supervisorId || '');
    } else {
      employeeForm.reset();
      employeeStatusInput.value = 'Active';
      populateSupervisorOptions('', '');
    }

    employeeModal.hidden = false;
    employeeNameInput.focus();
  }

  function populateSupervisorOptions(employeeId, selectedSupervisorId) {
    if (!employeeSupervisorInput) return;
    employeeSupervisorInput.innerHTML = '<option value="">No supervisor assigned</option>';
    employeeRecords
      .filter(employee => String(employee.id) !== String(employeeId))
      .forEach(employee => {
        const option = document.createElement('option');
        const employeeName = getEmployeeDisplayName(employee);
        const isSupervisor = isEmployeeSupervisor(employee.id, employeeRecords);
        option.value = employee.id;
        option.textContent = `${employeeName}${isSupervisor ? ' — Supervisor' : ''}${employee.employeeCode || employee.employeeNumber ? ` (${employee.employeeCode || employee.employeeNumber})` : ''}`;
        option.selected = String(employee.id) === String(selectedSupervisorId);
        employeeSupervisorInput.appendChild(option);
      });
  }

  function populateSupervisorFilter() {
    if (!supervisorFilter) return;
    const selectedValue = supervisorFilter.value;
    supervisorFilter.innerHTML = `
      <option value="all">All Supervisors</option>
      <option value="unassigned">No Supervisor Assigned</option>`;
    const supervisors = employeeRecords
      .filter(employee => isEmployeeSupervisor(employee.id, employeeRecords))
      .sort((left, right) => getEmployeeDisplayName(left).localeCompare(getEmployeeDisplayName(right)));
    supervisors.forEach(employee => {
      const option = document.createElement('option');
      option.value = employee.id;
      option.textContent = getEmployeeDisplayName(employee);
      supervisorFilter.appendChild(option);
    });
    supervisorFilter.value = Array.from(supervisorFilter.options).some(option => option.value === selectedValue)
      ? selectedValue
      : 'all';
  }

  function closeEmployeeModal() {
    employeeModal.hidden = true;
    editingRow = null;
  }

  function updateEmployeeRow(row, employee) {
    row.querySelector('.cell-id').textContent = employee.nationalId || employee.employeeCode || row.querySelector('.cell-id').textContent;
    row.dataset.employeeName = employee.name;
    const isSupervisor = isEmployeeSupervisor(row.dataset.employeeId, employeeRecords);
    row.querySelector('.cell-name').innerHTML = `
      <span class="employee-name-text">${employee.name}</span>
      ${isSupervisor ? '<span class="supervisor-tag">Supervisor</span>' : ''}
    `;
    row.querySelector('.cell-position').textContent = employee.position;
    row.querySelector('.cell-department').textContent = employee.department;
    row.querySelector('.cell-salary').textContent = employee.salary;
    row.querySelector('.cell-status').innerHTML = `<span class="status-badge ${getStatusClass(employee.status)}">${employee.status}</span>`;
    row.dataset.supervisorId = employee.supervisorId || '';
    row.dataset.supervisorName = employee.supervisorName || '';
    row.dataset.nationalId = employee.nationalId || '';
  }

  function createEmployeeRow(employee) {
    const employeeName = getEmployeeDisplayName(employee);
    const employeeNumber = employee.nationalId || employee.employeeCode || 'Not assigned';
    const isSupervisor = isEmployeeSupervisor(employee.id, employeeRecords);
    const row = document.createElement('tr');
    row.className = 'table-row';
    if (employee.id) row.dataset.employeeId = employee.id;
    row.dataset.employeeName = employeeName;
    row.dataset.nationalId = employee.nationalId || employee.employeeCode || '';
    row.dataset.supervisorId = employee.supervisorId || '';
    row.dataset.supervisorName = employee.supervisorName || '';
    row.innerHTML = `
      <td class="cell-id">${employeeNumber}</td>
      <td class="cell-photo"><div class="employee-avatar avatar-gradient-1">${getInitials(employeeName)}</div></td>
      <td class="cell-name">
        <span class="employee-name-text">${employeeName}</span>
        ${isSupervisor ? '<span class="supervisor-tag">Supervisor</span>' : ''}
      </td>
      <td class="cell-position">${employee.position}</td>
      <td class="cell-department">${employee.department}</td>
      <td class="cell-salary">${employee.salary}</td>
      <td class="cell-status"><span class="status-badge ${getStatusClass(employee.status)}">${employee.status}</span></td>
      <td class="cell-actions">
        <button class="action-icon" title="Edit" type="button"><span aria-hidden="true">&#9998;</span></button>
        <button class="action-icon" title="View" type="button"><span aria-hidden="true">&#128065;</span></button>
        <button class="action-icon" title="Delete" type="button"><span aria-hidden="true">&#128465;</span></button>
      </td>`;
    return row;
  }

  function getInitials(name) {
    return name.split(/\s+/).map(part => part[0]).join('').slice(0, 2).toUpperCase();
  }

  function getStatusClass(status) {
    return status === 'On Leave' ? 'leave' : status === 'Terminated' ? 'inactive' : 'active';
  }

  function capitalizeWords(value) {
    return value.toLowerCase().replace(/\b[a-z]/g, letter => letter.toUpperCase());
  }

  [employeeNameInput, employeePositionInput].forEach(input => {
    input.addEventListener('input', () => {
      input.value = capitalizeWords(input.value);
    });
  });

  if (employeeNationalIdInput) {
    employeeNationalIdInput.addEventListener('input', () => {
      employeeNationalIdInput.value = employeeNationalIdInput.value.replace(/\D/g, '').slice(0, 20);
    });
  }

  document.querySelectorAll('[data-close-employee-modal]').forEach(button => {
    button.addEventListener('click', closeEmployeeModal);
  });

  document.querySelectorAll('[data-close-employee-view]').forEach(button => {
    button.addEventListener('click', () => {
      employeeViewModal.hidden = true;
    });
  });

  employeeForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    const formData = new FormData(employeeForm);
    const employee = {
      nationalId: formData.get('nationalId').trim(),
      name: capitalizeWords(formData.get('name').trim()),
      position: capitalizeWords(formData.get('position').trim()),
      department: formData.get('department'),
      supervisorId: formData.get('supervisorId') || null,
      salary: Number(formData.get('salary')).toLocaleString('en-US'),
      status: formData.get('status'),
      supervisorName: employeeSupervisorInput?.selectedOptions[0]?.textContent.replace(/ \([^)]*\)$/, '') || ''
    };

    if (editingRow) {
      if (databaseEmployeeMode && databaseApi?.updateEmployee) {
        const nameParts = employee.name.split(/\s+/);
        let savedEmployee;
        try {
          savedEmployee = await databaseApi.updateEmployee(editingRow.dataset.employeeId, {
            firstName: nameParts.shift() || employee.name,
            lastName: nameParts.join(' ') || nameParts[0] || employee.name,
            nationalId: employee.nationalId,
            position: employee.position,
            department: employee.department,
            supervisorId: employee.supervisorId,
            salary: Number(formData.get('salary')),
            status: employee.status
          });
        } catch (error) {
          alert(error.message || 'Unable to update employee');
          return;
        }
        if (!savedEmployee) return;
        const recordIndex = employeeRecords.findIndex(record => String(record.id) === String(savedEmployee.id));
        if (recordIndex !== -1) employeeRecords[recordIndex] = savedEmployee;
        populateSupervisorFilter();
      }
      updateEmployeeRow(editingRow, employee);
      closeEmployeeModal();
      refreshTable();
    } else {
      let savedEmployee = employee;
      if (databaseEmployeeMode && databaseApi?.createEmployee) {
        const nameParts = employee.name.split(/\s+/);
        try {
          savedEmployee = await databaseApi.createEmployee({
            firstName: nameParts.shift() || employee.name,
            lastName: nameParts.join(' ') || nameParts[0] || employee.name,
            nationalId: employee.nationalId,
            position: employee.position,
            department: employee.department,
            supervisorId: employee.supervisorId,
            salary: Number(formData.get('salary')),
            status: employee.status
          });
        } catch (error) {
          alert(error.message || 'Unable to create employee');
          return;
        }
        if (!savedEmployee) return;
        savedEmployee.name = `${savedEmployee.firstName} ${savedEmployee.lastName}`;
        savedEmployee.salary = Number(savedEmployee.salary).toLocaleString('en-US');
        employeeRecords.push(savedEmployee);
        populateSupervisorFilter();
      }
      const newRow = createEmployeeRow(savedEmployee);
      document.querySelector('.employees-table tbody').appendChild(newRow);
      tableRows.push(newRow);
      initialTableRows.push(newRow);
      bindActionIcon(newRow.querySelector('[title="Edit"]'));
      bindActionIcon(newRow.querySelector('[title="View"]'));
      bindActionIcon(newRow.querySelector('[title="Delete"]'));
      totalRows = tableRows.length;
      currentSortedRows = [...initialTableRows];
      totalPages = Math.max(1, Math.ceil(currentSortedRows.length / ROWS_PER_PAGE));
      closeEmployeeModal();
      showPage(totalPages, currentSortedRows);
    }
  });

  // Pagination handlers - Responsive rows per page
  let ROWS_PER_PAGE = 15;
  let currentPage = 1;
  const tableRows = Array.from(document.querySelectorAll('.table-row'));
  const initialTableRows = [...tableRows];
  let totalRows = tableRows.length;
  let totalPages = Math.ceil(totalRows / ROWS_PER_PAGE);
  let currentSortedRows = null;

  function getFilteredRows() {
    const searchTerm = searchInput ? searchInput.value.trim().toLowerCase() : '';
    const department = departmentFilter ? departmentFilter.value.toLowerCase() : 'all';
    const status = statusFilter ? statusFilter.value.toLowerCase() : 'all';
    const supervisor = supervisorFilter ? supervisorFilter.value : 'all';

    return initialTableRows.filter(row => {
      const rowText = row.textContent.toLowerCase();
      const rowDepartment = row.querySelector('.cell-department').textContent.trim().toLowerCase();
      const rowStatus = row.querySelector('.cell-status').textContent.trim().toLowerCase();
      const rowSupervisorId = row.dataset.supervisorId || '';

      return (!searchTerm || rowText.includes(searchTerm))
        && (department === 'all' || rowDepartment === department)
        && (status === 'all' || rowStatus === status)
        && (supervisor === 'all'
          || (supervisor === 'unassigned' && !rowSupervisorId)
          || rowSupervisorId === supervisor);
    });
  }

  function sortTableRows(sortBy, rows = initialTableRows) {
    const sorted = [...rows];

    switch (sortBy) {
      case 'name-asc':
        sorted.sort((a, b) => {
          const nameA = a.querySelector('.cell-name').textContent.trim();
          const nameB = b.querySelector('.cell-name').textContent.trim();
          return nameA.localeCompare(nameB);
        });
        break;
      case 'name-desc':
        sorted.sort((a, b) => {
          const nameA = a.querySelector('.cell-name').textContent.trim();
          const nameB = b.querySelector('.cell-name').textContent.trim();
          return nameB.localeCompare(nameA);
        });
        break;
      case 'salary-asc':
        sorted.sort((a, b) => {
          const salaryA = parseInt(a.querySelector('.cell-salary').textContent.replace(/,/g, ''));
          const salaryB = parseInt(b.querySelector('.cell-salary').textContent.replace(/,/g, ''));
          return salaryA - salaryB;
        });
        break;
      case 'salary-desc':
        sorted.sort((a, b) => {
          const salaryA = parseInt(a.querySelector('.cell-salary').textContent.replace(/,/g, ''));
          const salaryB = parseInt(b.querySelector('.cell-salary').textContent.replace(/,/g, ''));
          return salaryB - salaryA;
        });
        break;
      case 'position-asc':
        sorted.sort((a, b) => {
          const posA = a.querySelector('.cell-position').textContent.trim();
          const posB = b.querySelector('.cell-position').textContent.trim();
          return posA.localeCompare(posB);
        });
        break;
    }

    return sorted;
  }

  function showPage(page, sortedRows = null) {
    const rows = sortedRows || tableRows;
    const pages = Math.max(1, Math.ceil(rows.length / ROWS_PER_PAGE));
    currentPage = Math.max(1, Math.min(page, pages));
    const startIdx = (currentPage - 1) * ROWS_PER_PAGE;
    const endIdx = startIdx + ROWS_PER_PAGE;

    // Show/hide rows based on current page
    document.querySelectorAll('.table-row').forEach(row => row.style.display = 'none');
    rows.slice(startIdx, endIdx).forEach(row => row.style.display = '');

    // Update pagination info
    const paginationInfo = document.querySelector('.pagination-info');
    const displayEnd = Math.min(currentPage * ROWS_PER_PAGE, rows.length);
    if (paginationInfo) {
      paginationInfo.textContent = `Showing ${startIdx + 1}-${displayEnd} of ${rows.length} entries`;
    }

    // Update pagination buttons
    updatePaginationButtons(pages);
  }

  function updatePaginationButtons(pages = totalPages) {
    const paginationBtns = document.querySelectorAll('.pagination-btn');
    paginationBtns.forEach(btn => {
      if (btn.classList.contains('prev')) {
        btn.disabled = currentPage === 1;
      } else if (btn.classList.contains('next')) {
        btn.disabled = currentPage === pages;
      } else {
        btn.classList.remove('active');
        const pageNumber = parseInt(btn.textContent.trim(), 10);
        btn.hidden = Number.isNaN(pageNumber) || pageNumber > pages;
        if (pageNumber === currentPage) {
          btn.classList.add('active');
        }
      }
    });
  }

  // Sorting functionality
  const sortSelect = document.getElementById('sortSelect');
  if (sortSelect) {
    sortSelect.addEventListener('change', (e) => {
      currentSortedRows = sortTableRows(e.target.value, getFilteredRows());
      showPage(1, currentSortedRows);
    });
  }

  function refreshTable() {
    const filteredRows = getFilteredRows();
    currentSortedRows = sortSelect
      ? sortTableRows(sortSelect.value, filteredRows)
      : filteredRows;
    totalPages = Math.max(1, Math.ceil(currentSortedRows.length / ROWS_PER_PAGE));
    showPage(1, currentSortedRows);
  }

  async function loadEmployeesFromDatabase() {
    if (!databaseApi?.listEmployees) return;

    try {
      const records = await databaseApi.listEmployees();
      if (!Array.isArray(records) || records.length === 0) return;
      employeeRecords = records;
      populateSupervisorFilter();

      const tableBody = document.querySelector('.employees-table tbody');
      tableRows.splice(0, tableRows.length);
      initialTableRows.splice(0, initialTableRows.length);
      tableBody.replaceChildren();

      records.forEach(record => {
        const row = createEmployeeRow({
          id: record.id,
          employeeCode: record.employeeCode || record.employeeNumber,
          nationalId: record.nationalId || '',
          name: record.name || `${record.firstName} ${record.lastName}`,
          position: record.position || '',
          department: record.department || '',
          supervisorId: record.supervisorId,
          supervisorName: record.supervisorName || '',
          salary: Number(record.salary || record.basicSalary || 0).toLocaleString('en-US'),
          status: record.status || 'Active'
        });
        tableBody.appendChild(row);
        tableRows.push(row);
        initialTableRows.push(row);
        row.querySelectorAll('.action-icon').forEach(bindActionIcon);
      });

      databaseEmployeeMode = true;
      totalRows = tableRows.length;
      totalPages = Math.max(1, Math.ceil(totalRows / ROWS_PER_PAGE));
      currentSortedRows = [...initialTableRows];
      showPage(1, currentSortedRows);
    } catch (error) {
      console.error('Unable to load employees from SQLite', error);
    }
  }

  if (searchInput) {
    searchInput.addEventListener('input', refreshTable);
  }

  [departmentFilter, statusFilter, supervisorFilter].forEach(select => {
    if (select) select.addEventListener('change', refreshTable);
  });

  const paginationBtns = document.querySelectorAll('.pagination-btn');
  paginationBtns.forEach(btn => {
    btn.addEventListener('click', (e) => {
      if (btn.classList.contains('prev')) {
        showPage(currentPage - 1, currentSortedRows);
      } else if (btn.classList.contains('next')) {
        showPage(currentPage + 1, currentSortedRows);
      } else if (!btn.classList.contains('active')) {
        const pageNum = parseInt(btn.textContent);
        if (!isNaN(pageNum)) {
          showPage(pageNum, currentSortedRows);
        }
      }
    });
  });

  // Handle window resize for responsive rows
  window.addEventListener('resize', () => {
    // Recalculate total pages if needed
    totalPages = Math.ceil(totalRows / ROWS_PER_PAGE);
    showPage(currentPage, currentSortedRows);
  });

  // Initialize pagination
  showPage(1);
  loadEmployeesFromDatabase();

  // Loading animation
  window.addEventListener('load', () => {
    const container = document.querySelector('.table-container');
    if (container) {
      container.style.opacity = '0';
      container.style.transform = 'translateY(20px)';
      setTimeout(() => {
        container.style.transition = 'all 0.6s cubic-bezier(0.22, 1, 0.36, 1)';
        container.style.opacity = '1';
        container.style.transform = 'translateY(0)';
      }, 100);
    }
  });
});
