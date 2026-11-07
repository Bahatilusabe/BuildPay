// Attendance page interactivity
document.addEventListener('DOMContentLoaded', () => {
  const databaseApi = window.BuildPayDatabase;
  const sidebarToggle = document.getElementById('sidebarToggle');
  const sidebarCollapseBtn = document.getElementById('sidebarCollapseBtn');
  const sidebar = document.querySelector('.sidebar');
  const dashMain = document.querySelector('.dash-main');
  const navItems = document.querySelectorAll('.nav-item');
  const resizeHandle = document.getElementById('sidebarResizeHandle');
  const monthPicker = document.getElementById('monthPicker');
  const attendanceSearch = document.getElementById('attendanceSearch');
  const departmentFilter = document.getElementById('departmentFilter');
  const markAttendanceBtn = document.getElementById('markAttendanceBtn');
  const attendanceEntryPanel = document.getElementById('attendanceEntryPanel');
  const attendanceEntryTitle = document.getElementById('attendanceEntryTitle');
  const attendanceEntryDescription = document.getElementById('attendanceEntryDescription');
  const attendanceEmployeeInput = document.getElementById('attendanceEmployeeInput');
  const attendanceDayInput = document.getElementById('attendanceDayInput');
  const attendanceStatusInput = document.getElementById('attendanceStatusInput');
  const attendanceOvertimeInput = document.getElementById('attendanceOvertimeInput');
  const applyAttendanceEntry = document.getElementById('applyAttendanceEntry');
  const attendanceBody = document.querySelector('.attendance-table tbody');
  let attendanceRows = Array.from(document.querySelectorAll('.attendance-row'));
  let dayCells = Array.from(document.querySelectorAll('.attendance-table td.day-col'));
  const attendancePrevBtn = document.getElementById('attendancePrevBtn');
  const attendanceNextBtn = document.getElementById('attendanceNextBtn');
  const attendancePageNumber = document.getElementById('attendancePageNumber');
  const attendancePaginationInfo = document.getElementById('attendancePaginationInfo');
  const ATTENDANCE_ROWS_PER_PAGE = 17;
  let attendancePage = 1;
  const recordedAttendance = new Set();
  let selectedAttendanceCell = null;
  let databaseEmployeeMode = false;

  if (monthPicker && !monthPicker.value) {
    monthPicker.value = new Date().toISOString().slice(0, 10);
  }

  function getWeekDates(value = monthPicker?.value) {
    const date = new Date(`${value}T00:00:00Z`);
    const day = date.getUTCDay() || 7;
    date.setUTCDate(date.getUTCDate() - day + 1);
    return Array.from({ length: 7 }, (_, index) => {
      const current = new Date(date);
      current.setUTCDate(date.getUTCDate() + index);
      return current.toISOString().slice(0, 10);
    });
  }

  function getWeekLabel() {
    const dates = getWeekDates();
    return `${dates[0]} to ${dates[6]}`;
  }

  addAdditionalAttendanceRows();
  attendanceRows = Array.from(document.querySelectorAll('.attendance-row'));
  dayCells = Array.from(document.querySelectorAll('.attendance-table td.day-col'));
  attendanceRows.forEach(row => {
    if (!row.dataset.employeeId) row.dataset.employeeId = row.querySelector('.id-col')?.textContent.trim() || '';
  });
  populateAttendanceEntryOptions();

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

  // Mark Attendance button
  if (markAttendanceBtn) {
    markAttendanceBtn.addEventListener('click', () => {
      if (!getWeekDates().length || !hasUnsavedWeeklyAttendance()) {
        alert('All attendance and overtime records for this week have already been saved, or no records are ready to save.');
        return;
      }

      if (confirm(`Save attendance and overtime for ${getWeekLabel()}? Once saved, these daily records cannot be changed.`)) {
        saveSelectedWeek();
      }
    });
  }

  // Month picker change event
  if (monthPicker) {
    monthPicker.addEventListener('change', (e) => {
      const selectedMonth = e.target.value.slice(0, 7);
      applyAttendanceLock();
      updateMarkAttendanceButton();
      loadAttendanceFromDatabase(selectedMonth);
    });
  }

  // Department filter change
  if (departmentFilter) {
    departmentFilter.addEventListener('change', (e) => {
      const department = e.target.value;
      filterAttendanceByDepartment(department);
    });
  }

  if (attendanceSearch) {
    attendanceSearch.addEventListener('input', () => {
      attendancePage = 1;
      renderAttendancePage();
    });
  }

  // Day cell click for marking attendance
  function bindDayCell(cell) {
    cell.addEventListener('click', () => {
      const row = cell.closest('tr');
      const attendanceDate = cell.dataset.date;
      if (!attendanceDate || isAttendanceRecorded(row, attendanceDate)) return;
      openAttendanceEntry(cell);
    });
  }

  dayCells.forEach(bindDayCell);

  // Filter attendance by department
  function filterAttendanceByDepartment(department) {
    attendancePage = 1;
    renderAttendancePage(department);
  }

  function renderAttendancePage(department = departmentFilter.value) {
    const searchTerm = attendanceSearch ? attendanceSearch.value.trim().toLowerCase() : '';
    const filteredRows = attendanceRows.filter(row => {
      const matchesDepartment = department === 'All Departments' || row.dataset.department === department || !row.dataset.department;
      const matchesSearch = !searchTerm || row.textContent.toLowerCase().includes(searchTerm);
      return matchesDepartment && matchesSearch;
    });
    const totalPages = Math.max(1, Math.ceil(filteredRows.length / ATTENDANCE_ROWS_PER_PAGE));
    attendancePage = Math.min(attendancePage, totalPages);
    const start = (attendancePage - 1) * ATTENDANCE_ROWS_PER_PAGE;
    const end = Math.min(start + ATTENDANCE_ROWS_PER_PAGE, filteredRows.length);

    attendanceRows.forEach(row => row.style.display = 'none');
    filteredRows.slice(start, end).forEach(row => row.style.display = '');
    attendancePaginationInfo.textContent = filteredRows.length
      ? `Showing ${start + 1}-${end} of ${filteredRows.length} employees`
      : 'No employees found';
    attendancePageNumber.textContent = attendancePage;
    attendancePrevBtn.disabled = attendancePage === 1;
    attendanceNextBtn.disabled = attendancePage === totalPages;
  }

  function addAdditionalAttendanceRows() {
    const additionalEmployees = [
      ['EMP011', 'Brian Otieno', 'Engineering'],
      ['EMP012', 'Mercy Njeri', 'Construction'],
      ['EMP013', 'David Karanja', 'Management'],
      ['EMP014', 'Grace Wambui', 'Safety'],
      ['EMP015', 'Eric Kiptoo', 'Construction'],
      ['EMP016', 'Naomi Akinyi', 'Engineering'],
      ['EMP017', 'Felix Odhiambo', 'Management'],
      ['EMP018', 'Irene Chebet', 'Safety'],
      ['EMP019', 'Martin Kamau', 'Construction']
    ];
    const statuses = [
      ['status-present', 'Present', 'P'], ['status-present', 'Present', 'P'],
      ['status-absent', 'Absent', 'A'], ['status-present', 'Present', 'P'],
      ['status-present', 'Present', 'P'], ['status-leave', 'Leave', 'L'],
      ['status-present', 'Present', 'P'], ['status-present', 'Present', 'P'],
      ['status-present', 'Present', 'P'], ['status-overtime', 'Overtime', 'O'],
      ['status-present', 'Present', 'P'], ['status-present', 'Present', 'P'],
      ['status-present', 'Present', 'P'], ['status-present', 'Present', 'P'],
      ['status-absent', 'Absent', 'A'], ['status-present', 'Present', 'P'],
      ['status-present', 'Present', 'P'], ['status-present', 'Present', 'P'],
      ['status-present', 'Present', 'P'], ['status-present', 'Present', 'P'],
      ['status-present', 'Present', 'P'], ['status-present', 'Present', 'P'],
      ['status-present', 'Present', 'P'], ['status-present', 'Present', 'P'],
      ['status-present', 'Present', 'P'], ['status-present', 'Present', 'P'],
      ['status-present', 'Present', 'P'], ['status-present', 'Present', 'P'],
      ['status-present', 'Present', 'P'], ['status-present', 'Present', 'P'],
      ['status-weekend', 'Weekend Off', 'W']
    ];

    additionalEmployees.forEach(([id, name, department], index) => {
      const row = document.createElement('tr');
      row.className = 'attendance-row';
      row.dataset.department = department;
      const cells = statuses.map(([className, title, text], dayIndex) => `<td class="day-col ${className}" data-day="${dayIndex + 1}" title="${title}">${text}</td>`).join('');
      row.innerHTML = `<td class="sticky-col id-col">${id}</td><td class="sticky-col employee-col">${name}</td>${cells}<td class="stat-col stat-present">26</td><td class="stat-col stat-absent">2</td><td class="stat-col stat-leave">1</td><td class="stat-col stat-overtime">1</td>`;
      attendanceBody.appendChild(row);
      row.querySelectorAll('.day-col').forEach(bindDayCell);
    });
  }

  function updateRowStats(row) {
    const counts = {
      present: row.querySelectorAll('.day-col.status-present').length,
      absent: row.querySelectorAll('.day-col.status-absent').length,
      leave: row.querySelectorAll('.day-col.status-leave').length,
      overtime: row.querySelectorAll('.day-col.status-overtime').length
    };

    row.querySelector('.stat-present').textContent = counts.present;
    row.querySelector('.stat-absent').textContent = counts.absent;
    row.querySelector('.stat-leave').textContent = counts.leave;
    row.querySelector('.stat-overtime').textContent = counts.overtime;
  }

  function updateAllRowStats() {
    attendanceRows.forEach(updateRowStats);
  }

  function getSelectedDate() {
    return monthPicker?.value || '';
  }

  function getAttendanceKey(employeeId, attendanceDate) {
    return `${employeeId}|${attendanceDate}`;
  }

  function isAttendanceRecorded(row, attendanceDate) {
    return recordedAttendance.has(getAttendanceKey(row.dataset.employeeId || row.querySelector('.id-col').textContent, attendanceDate));
  }

  function hasUnsavedWeeklyAttendance() {
    const weekDates = getWeekDates();
    return attendanceRows.some(row => weekDates.some(date => row.querySelector(`[data-date="${date}"]`) && !isAttendanceRecorded(row, date)));
  }

  function applySelectedDateVisibility() {
    const weekDates = getWeekDates();
    const weekDateSet = new Set(weekDates);
    document.querySelectorAll('.attendance-table thead th.day-col').forEach((cell, index) => {
      cell.dataset.date = weekDates[index] || '';
    });
    document.querySelectorAll('.attendance-table th.day-col, .attendance-table td.day-col').forEach(cell => {
      cell.style.display = weekDateSet.has(cell.dataset.date) ? '' : 'none';
    });
    document.querySelectorAll('.attendance-table thead th.day-col').forEach((cell, index) => {
      const date = weekDates[index];
      if (date) cell.textContent = `${date.slice(8, 10)} ${new Date(`${date}T00:00:00Z`).toLocaleDateString('en-GB', { weekday: 'short' })}`;
    });
  }

  function applyAttendanceLock() {
    const weekDates = new Set(getWeekDates());
    dayCells.forEach(cell => {
      const row = cell.closest('tr');
      const attendanceDate = cell.dataset.date;
      const locked = !weekDates.has(attendanceDate) || isAttendanceRecorded(row, attendanceDate);
      cell.classList.toggle('attendance-locked', locked);
      cell.setAttribute('aria-disabled', locked ? 'true' : 'false');
      cell.title = locked && isAttendanceRecorded(row, attendanceDate)
        ? `${cell.title.replace(' (Saved)', '')} (Saved)`
        : cell.title.replace(' (Saved)', '');
    });
    applySelectedDateVisibility();
  }

  function updateMarkAttendanceButton() {
    const locked = !hasUnsavedWeeklyAttendance();
     markAttendanceBtn.disabled = locked || !getSelectedDate() || !databaseEmployeeMode;
    markAttendanceBtn.innerHTML = locked ? 'Week Recorded' : '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 5v14M5 12h14"></path></svg> Save Weekly Attendance';
  }

  attendancePrevBtn.addEventListener('click', () => {
    attendancePage -= 1;
    renderAttendancePage();
  });

  attendanceNextBtn.addEventListener('click', () => {
    attendancePage += 1;
    renderAttendancePage();
  });

  function setCellStatus(cell, title, text, className) {
    ['status-present', 'status-absent', 'status-leave', 'status-overtime', 'status-weekend'].forEach(status => cell.classList.remove(status));
    cell.classList.add(className);
    cell.title = title;
    const statusText = cell.querySelector('.attendance-status-text');
    if (statusText) statusText.textContent = text;
    else cell.textContent = text;
  }

  function openAttendanceEntry(cell) {
    selectedAttendanceCell = cell;
    const row = cell.closest('tr');
    const employeeName = row.querySelector('.employee-col')?.textContent.trim() || 'Employee';
    populateAttendanceEntryOptions(row.dataset.employeeId, cell.dataset.date);
    attendanceEntryTitle.textContent = `${employeeName} · ${cell.dataset.date}`;
    attendanceEntryDescription.textContent = 'Choose the work status and enter overtime hours, then apply the entry.';
    const statusMap = { 'status-present': 'PRESENT', 'status-absent': 'ABSENT', 'status-leave': 'LEAVE', 'status-overtime': 'PRESENT', 'status-weekend': 'WEEKEND' };
    attendanceStatusInput.value = statusMap[Array.from(cell.classList).find(className => className.startsWith('status-'))] || 'PRESENT';
    attendanceOvertimeInput.value = cell.dataset.overtimeHours || '0';
    attendanceEntryPanel.hidden = false;
    attendanceEntryPanel.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
    attendanceOvertimeInput.focus();
  }

  function populateAttendanceEntryOptions(selectedEmployeeId = '', selectedDate = '') {
    const weekDates = getWeekDates();
    attendanceEmployeeInput.replaceChildren();
    attendanceRows.forEach(row => {
      const option = document.createElement('option');
      option.value = row.dataset.employeeId || row.querySelector('.id-col')?.textContent.trim() || '';
      option.textContent = row.querySelector('.employee-col')?.textContent.trim() || option.value;
      attendanceEmployeeInput.appendChild(option);
    });
    attendanceDayInput.replaceChildren();
    weekDates.forEach(date => {
      const option = document.createElement('option');
      option.value = date;
      option.textContent = new Date(`${date}T00:00:00Z`).toLocaleDateString('en-GB', { weekday: 'short', day: '2-digit', month: 'short' });
      attendanceDayInput.appendChild(option);
    });
    if (selectedEmployeeId && [...attendanceEmployeeInput.options].some(option => option.value === String(selectedEmployeeId))) {
      attendanceEmployeeInput.value = String(selectedEmployeeId);
    }
    if (selectedDate && [...attendanceDayInput.options].some(option => option.value === selectedDate)) {
      attendanceDayInput.value = selectedDate;
    }
    if (!attendanceEmployeeInput.value && attendanceEmployeeInput.options.length) attendanceEmployeeInput.selectedIndex = 0;
    if (!attendanceDayInput.value && attendanceDayInput.options.length) attendanceDayInput.selectedIndex = 0;
  }

  function getSelectedEntryCell() {
    const row = attendanceRows.find(item => String(item.dataset.employeeId) === String(attendanceEmployeeInput.value));
    return row?.querySelector(`.day-col[data-date="${attendanceDayInput.value}"]`) || null;
  }

  attendanceEmployeeInput.addEventListener('change', () => {
    selectedAttendanceCell = getSelectedEntryCell();
    if (selectedAttendanceCell) openAttendanceEntry(selectedAttendanceCell);
  });

  attendanceDayInput.addEventListener('change', () => {
    selectedAttendanceCell = getSelectedEntryCell();
    if (selectedAttendanceCell) openAttendanceEntry(selectedAttendanceCell);
  });

  applyAttendanceEntry.addEventListener('click', () => {
    selectedAttendanceCell = getSelectedEntryCell();
    if (!selectedAttendanceCell) return;
    const row = selectedAttendanceCell.closest('tr');
    if (isAttendanceRecorded(row, selectedAttendanceCell.dataset.date)) {
      attendanceEntryDescription.textContent = 'This employee and day are already saved and cannot be changed.';
      return;
    }
    const overtimeHours = Math.max(0, Math.min(24, Number(attendanceOvertimeInput.value) || 0));
    const status = attendanceStatusInput.value;
    const statusConfig = {
      PRESENT: ['Present', 'P', 'status-present'],
      ABSENT: ['Absent', 'A', 'status-absent'],
      LEAVE: ['Leave', 'L', 'status-leave'],
      LATE: ['Late', 'L', 'status-overtime'],
      WEEKEND: ['Weekend Off', 'W', 'status-weekend']
    }[status];
    setCellStatus(selectedAttendanceCell, statusConfig[0], statusConfig[1], statusConfig[2]);
    if (overtimeHours > 0) {
      selectedAttendanceCell.dataset.overtimeHours = String(overtimeHours);
    } else {
      delete selectedAttendanceCell.dataset.overtimeHours;
    }
    updateRowStats(selectedAttendanceCell.closest('tr'));
    attendanceEntryDescription.textContent = `Entry prepared. It will be permanently saved when you save the weekly attendance.`;
  });

  async function saveSelectedWeek() {
    if (!databaseApi?.recordAttendance) {
      attendanceEntryDescription.textContent = 'Attendance database is unavailable. Restart the application before saving.';
      alert('Attendance could not be saved because the database connection is unavailable.');
      return;
    }
    if (!databaseEmployeeMode) {
      attendanceEntryDescription.textContent = 'Employee records are still loading. Please wait and try again.';
      alert('Employee records are still loading. Please try saving again in a moment.');
      return;
    }
    try {
      let savedCount = 0;
      for (const row of attendanceRows) {
        for (const cell of row.querySelectorAll('.day-col')) {
          const attendanceDate = cell.dataset.date;
          if (!attendanceDate || isAttendanceRecorded(row, attendanceDate)) continue;
          if (!row.dataset.employeeId) throw new Error('An employee record is missing its database ID. Reload the attendance page.');
          const statusTitle = cell.title.replace(' (Saved)', '');
          const statusMap = { Present: 'PRESENT', Absent: 'ABSENT', Leave: 'LEAVE', Late: 'LATE', Overtime: 'PRESENT', 'Weekend Off': 'PRESENT' };
          await databaseApi.recordAttendance({
            employeeId: row.dataset.employeeId,
            attendanceDate,
            status: statusMap[statusTitle] || 'PRESENT',
            regularHours: statusTitle === 'Absent' || statusTitle === 'Leave' || statusTitle === 'Weekend Off' ? 0 : 8,
            overtimeHours: statusTitle === 'Overtime' ? Number(cell.dataset.overtimeHours || 0) : 0
          });
          recordedAttendance.add(getAttendanceKey(row.dataset.employeeId, attendanceDate));
          savedCount += 1;
        }
      }
      if (!savedCount) {
        attendanceEntryDescription.textContent = 'There are no new attendance entries to save for this week.';
        return;
      }
      applyAttendanceLock();
      updateMarkAttendanceButton();
      alert(`Attendance and overtime for ${getWeekLabel()} were recorded successfully.`);
    } catch (error) {
      console.error('Unable to save attendance', error);
      attendanceEntryDescription.textContent = error.message || 'Unable to save attendance.';
      alert(error.message || 'Unable to save attendance');
    }
  }

  async function loadAttendanceFromDatabase(month = getSelectedDate().slice(0, 7)) {
    if (!databaseApi?.listAttendance || !month || !databaseApi?.listEmployees) return;

    try {
      const weekDates = getWeekDates();
      const months = [...new Set(weekDates.map(date => date.slice(0, 7)))];
      const [employees, ...attendanceSets] = await Promise.all([
        databaseApi.listEmployees(),
        ...months.map(item => databaseApi.listAttendance(item))
      ]);
      const records = attendanceSets.flat();
      recordedAttendance.clear();
      records.forEach(record => {
        if (record.attendanceDate) recordedAttendance.add(getAttendanceKey(record.employeeId, record.attendanceDate));
      });

      const recordsByEmployee = new Map();
      records.forEach(record => {
        if (!recordsByEmployee.has(record.employeeId)) {
          recordsByEmployee.set(record.employeeId, new Map());
        }
        if (record.attendanceDate) {
          recordsByEmployee.get(record.employeeId).set(record.attendanceDate, record);
        }
      });

      attendanceBody.replaceChildren();
      const statusConfig = {
        PRESENT: ['status-present', 'Present', 'P'],
        ABSENT: ['status-absent', 'Absent', 'A'],
        LEAVE: ['status-leave', 'Leave', 'L'],
        LATE: ['status-overtime', 'Late', 'L']
      };
      employees.forEach(employee => {
        const row = document.createElement('tr');
        row.className = 'attendance-row';
        row.dataset.department = employee.department || '';
        row.dataset.employeeId = employee.id;
        let cells = '';
        weekDates.forEach(date => {
          const record = recordsByEmployee.get(employee.id)?.get(date);
          const config = record?.overtimeHours > 0
            ? ['status-overtime', 'Overtime', 'O']
            : (statusConfig[record?.status] || ['status-present', 'Present', 'P']);
          cells += `<td class="day-col ${config[0]}" data-date="${date}" title="${config[1]}"${record?.overtimeHours ? ` data-overtime-hours="${record.overtimeHours}"` : ''}>${config[2]}${record?.overtimeHours ? ` <small class="attendance-overtime-mark">+${record.overtimeHours}h OT</small>` : ''}</td>`;
        });
        const employeeName = employee.name || `${employee.firstName} ${employee.lastName}`;
        row.innerHTML = `<td class="sticky-col id-col">${employee.employeeCode}</td><td class="sticky-col employee-col">${employeeName}</td>${cells}<td class="stat-col stat-present">0</td><td class="stat-col stat-absent">0</td><td class="stat-col stat-leave">0</td><td class="stat-col stat-overtime">0</td>`;
        attendanceBody.appendChild(row);
        row.querySelectorAll('.day-col').forEach(bindDayCell);
      });

      databaseEmployeeMode = true;
      attendanceRows = Array.from(document.querySelectorAll('.attendance-row'));
      dayCells = Array.from(document.querySelectorAll('.attendance-table td.day-col'));
      attendanceRows.forEach(row => {
        row.dataset.employeeId = row.dataset.employeeId || row.querySelector('.id-col')?.textContent.trim() || '';
      });
      populateAttendanceEntryOptions(attendanceEmployeeInput.value, attendanceDayInput.value);
      updateAllRowStats();
      applyAttendanceLock();
      updateMarkAttendanceButton();
      renderAttendancePage();
    } catch (error) {
      console.error('Unable to load attendance from SQLite', error);
    }
  }
  const initialWeekDates = getWeekDates();
  document.querySelectorAll('.attendance-table thead th.day-col').forEach((cell, index) => {
    cell.dataset.date = initialWeekDates[index] || '';
  });
  document.querySelectorAll('.attendance-table tbody').forEach(body => {
    body.querySelectorAll('.attendance-row').forEach(row => {
      row.querySelectorAll('.day-col').forEach((cell, index) => {
        if (index < initialWeekDates.length) cell.dataset.date = initialWeekDates[index];
      });
    });
  });
  updateAllRowStats();
  applyAttendanceLock();
  updateMarkAttendanceButton();
  renderAttendancePage();
  loadAttendanceFromDatabase(getSelectedDate().slice(0, 7));

  // Loading animation
  window.addEventListener('load', () => {
    const container = document.querySelector('.attendance-table-container');
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
