document.addEventListener('DOMContentLoaded', () => {
  const databaseApi = window.BuildPayDatabase;
  const companyFields = document.querySelectorAll('input[data-company-field], textarea[data-company-field], select[data-company-field]');
  const preferenceFields = document.querySelectorAll('[data-setting-key]');

  const saveCompanyFields = () => {
    const company = {};
    companyFields.forEach((field) => {
      company[field.dataset.companyField] = field.value.trim();
    });
    if (databaseApi?.saveSettings) databaseApi.saveSettings({ company });
    else if (window.BuildPayCompany) window.BuildPayCompany.save(company);
  };

  companyFields.forEach((field) => {
    field.addEventListener(field.tagName === 'SELECT' ? 'change' : 'input', saveCompanyFields);
  });

  const savedPreferences = window.BuildPaySettings ? window.BuildPaySettings.get() : {};
  preferenceFields.forEach((field) => {
    const savedValue = savedPreferences[field.dataset.settingKey];
    if (savedValue !== undefined) field.value = savedValue;

    field.addEventListener('change', () => {
      const preferences = {};
      preferenceFields.forEach((preferenceField) => {
        preferences[preferenceField.dataset.settingKey] = preferenceField.value;
      });
      if (window.BuildPaySettings) {
        window.BuildPaySettings.save(preferences);
      } else {
        localStorage.setItem('buildpaySettings', JSON.stringify(preferences));
      }
      if (databaseApi?.saveSettings) databaseApi.saveSettings({ settings: preferences });
    });
  });

  if (databaseApi?.getSettings) {
    databaseApi.getSettings().then(({ company = {}, settings = {} }) => {
      const companyMap = {
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
      companyFields.forEach((field) => {
        const value = company[companyMap[field.dataset.companyField]];
        if (value !== undefined && value !== null) field.value = value;
      });
      preferenceFields.forEach((field) => {
        if (settings[field.dataset.settingKey] !== undefined) field.value = settings[field.dataset.settingKey];
      });
    }).catch((error) => console.error('Unable to load settings from SQLite', error));
  }

  const sidebarToggle = document.getElementById('sidebarToggle');
  const sidebarCollapseBtn = document.getElementById('sidebarCollapseBtn');
  const settingsToggle = document.getElementById('settingsToggle');
  const sidebar = document.querySelector('.sidebar');
  const dashMain = document.querySelector('.dash-main');
  const navItems = document.querySelectorAll('.nav-item');
  const settingsNavItems = document.querySelectorAll('.settings-nav-item');
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

  const toggleSidebar = () => {
    if (!sidebar) return;
    sidebar.classList.toggle('collapsed');
    if (dashMain) {
      dashMain.classList.toggle('sidebar-collapsed');
    }
  };

  if (sidebarToggle) sidebarToggle.addEventListener('click', toggleSidebar);
  if (sidebarCollapseBtn) sidebarCollapseBtn.addEventListener('click', toggleSidebar);
  if (settingsToggle) settingsToggle.addEventListener('click', toggleSidebar);

  navItems.forEach((item) => {
    item.addEventListener('click', () => {
      navItems.forEach((navItem) => navItem.classList.remove('active'));
      item.classList.add('active');
    });
  });

  settingsNavItems.forEach((item) => {
    item.addEventListener('click', () => {
      settingsNavItems.forEach((navItem) => navItem.classList.remove('active'));
      item.classList.add('active');
    });
  });

  const tabButtons = document.querySelectorAll('.settings-tab');
  const tabPanels = document.querySelectorAll('[data-settings-panel]');
  const tabPanelNames = ['general', 'payroll', 'employees', 'tax', 'preferences'];
  tabButtons.forEach((tab) => {
    tab.addEventListener('click', () => {
      const selectedIndex = Array.from(tabButtons).indexOf(tab);
      const selectedPanel = tabPanelNames[selectedIndex];
      tabButtons.forEach((button) => button.classList.remove('active'));
      tab.classList.add('active');
      tabPanels.forEach((panel) => {
        panel.classList.toggle('active', panel.dataset.settingsPanel === selectedPanel);
      });
    });
  });

  const departmentSettingsList = document.getElementById('departmentSettingsList');
  const departmentCreateForm = document.getElementById('departmentCreateForm');
  const departmentSettingsStatus = document.getElementById('departmentSettingsStatus');
  let departments = [];

  const setDepartmentStatus = (message, isError = false) => {
    if (!departmentSettingsStatus) return;
    departmentSettingsStatus.textContent = message;
    departmentSettingsStatus.classList.toggle('error', isError);
  };

  const renderDepartments = (departments) => {
    if (!departmentSettingsList) return;
    departmentSettingsList.replaceChildren();
    if (!departments.length) {
      departmentSettingsList.innerHTML = '<p class="settings-empty-state">No departments found.</p>';
      return;
    }
    departments.forEach((department) => {
      const row = document.createElement('div');
      row.className = 'department-setting-row';

      const fields = document.createElement('div');
      fields.className = 'department-setting-fields';
      const nameLabel = document.createElement('label');
      nameLabel.textContent = 'Department name';
      const nameInput = document.createElement('input');
      nameInput.type = 'text';
      nameInput.value = department.name;
      nameLabel.appendChild(nameInput);
      const descriptionLabel = document.createElement('label');
      descriptionLabel.textContent = 'Description';
      const descriptionInput = document.createElement('input');
      descriptionInput.type = 'text';
      descriptionInput.value = department.description || '';
      descriptionLabel.appendChild(descriptionInput);
      fields.append(nameLabel, descriptionLabel);

      const actions = document.createElement('div');
      actions.className = 'department-setting-meta';
      const count = document.createElement('span');
      count.textContent = `${department.employeeCount} employee${department.employeeCount === 1 ? '' : 's'}`;
      const saveButton = document.createElement('button');
      saveButton.type = 'button';
      saveButton.className = 'text-button department-save-btn';
      saveButton.textContent = 'Save';
      const deleteButton = document.createElement('button');
      deleteButton.type = 'button';
      deleteButton.className = 'text-button danger department-delete-btn';
      deleteButton.textContent = 'Delete';
      actions.append(count, saveButton, deleteButton);
      row.append(fields, actions);

      saveButton.addEventListener('click', async () => {
        try {
          const updated = await databaseApi.updateDepartment(department.id, {
            name: nameInput.value,
            description: descriptionInput.value
          });
          if (updated) {
            count.textContent = `${updated.employeeCount} employee${updated.employeeCount === 1 ? '' : 's'}`;
            const index = departments.findIndex(item => item.id === updated.id);
            if (index !== -1) departments[index] = updated;
          }
          setDepartmentStatus(`${updated.name} updated successfully.`);
        } catch (error) {
          console.error('Unable to update department', error);
          setDepartmentStatus(error.message || 'Unable to update department.', true);
        }
      });
      deleteButton.addEventListener('click', async () => {
        if (!confirm(`Delete ${department.name}? Departments with employees cannot be deleted.`)) return;
        try {
          if (await databaseApi.deleteDepartment(department.id)) row.remove();
          setDepartmentStatus(`${department.name} deleted successfully.`);
        } catch (error) {
          console.error('Unable to delete department', error);
          setDepartmentStatus(error.message || 'Unable to delete department.', true);
        }
      });
      departmentSettingsList.appendChild(row);
    });
  };

  if (departmentCreateForm) {
    departmentCreateForm.addEventListener('submit', async (event) => {
      event.preventDefault();
      const formData = new FormData(departmentCreateForm);
      try {
        const created = await databaseApi.createDepartment({
          name: formData.get('name'),
          description: formData.get('description')
        });
        if (created) {
          departments = [...departments, created].sort((left, right) => left.name.localeCompare(right.name));
          renderDepartments(departments);
          departmentCreateForm.reset();
          setDepartmentStatus(`${created.name} added successfully.`);
        }
      } catch (error) {
        console.error('Unable to create department', error);
        setDepartmentStatus(error.message || 'Unable to create department.', true);
      }
    });
  }

  if (departmentSettingsList && databaseApi?.listDepartments) {
    databaseApi.listDepartments().then((loadedDepartments) => {
      departments = loadedDepartments;
      renderDepartments(departments);
    }).catch((error) => {
      console.error('Unable to load departments', error);
      departmentSettingsList.innerHTML = '<p class="settings-empty-state">Unable to load departments.</p>';
      setDepartmentStatus(error.message || 'Unable to connect to the employee settings database.', true);
    });
  } else if (departmentSettingsList) {
    departmentSettingsList.innerHTML = '<p class="settings-empty-state">Database connection is unavailable.</p>';
    setDepartmentStatus('Database connection is unavailable.', true);
  }

  const companyLogoInput = document.getElementById('companyLogoInput');
  const logoUploadControl = document.getElementById('logoUploadControl');
  const logoUploadName = document.getElementById('logoUploadName');
  if (companyLogoInput && window.BuildPayCompany) {
    const loadCompanyLogo = (file) => {
      if (!file || !file.type.startsWith('image/')) return;

      const reader = new FileReader();
      reader.addEventListener('load', () => {
        window.BuildPayCompany.save({ companyLogo: reader.result });
        if (logoUploadName) logoUploadName.textContent = file.name;
      });
      reader.readAsDataURL(file);
    };

    companyLogoInput.addEventListener('change', () => loadCompanyLogo(companyLogoInput.files[0]));
    if (logoUploadControl) {
      logoUploadControl.addEventListener('dragover', (event) => {
        event.preventDefault();
        logoUploadControl.classList.add('is-dragging');
      });
      logoUploadControl.addEventListener('dragleave', () => logoUploadControl.classList.remove('is-dragging'));
      logoUploadControl.addEventListener('drop', (event) => {
        event.preventDefault();
        logoUploadControl.classList.remove('is-dragging');
        loadCompanyLogo(event.dataTransfer.files[0]);
      });
    }
  }

  const backupModal = document.getElementById('backupModal');
  const backupRestoreBtn = document.getElementById('backupRestoreBtn');
  const editInformationBtn = document.getElementById('editInformationBtn');
  const createBackupBtn = document.getElementById('createBackupBtn');
  const downloadBackupBtn = document.getElementById('downloadBackupBtn');
  const restoreBackupInput = document.getElementById('restoreBackupInput');
  const backupStatus = document.getElementById('backupStatus');
  const backupActivityList = document.getElementById('backupActivityList');
  const clearBackupActivityBtn = document.getElementById('clearBackupActivityBtn');
  const backupStorageKey = 'buildpayLatestBackup';
  const backupActivityKey = 'buildpayBackupActivity';

  const getBackupActivity = () => {
    try {
      return JSON.parse(localStorage.getItem(backupActivityKey) || '[]');
    } catch (error) {
      return [];
    }
  };

  const renderBackupActivity = () => {
    if (!backupActivityList) return;
    const activity = getBackupActivity();
    backupActivityList.innerHTML = activity.length
      ? activity.map((entry) => `<li><strong>${entry.action}</strong><span>${entry.time}</span></li>`).join('')
      : '<li class="backup-activity-empty">No backup activity yet.</li>';
  };

  const recordBackupActivity = (action) => {
    const time = window.BuildPaySettings ? window.BuildPaySettings.formatDateTime(new Date()) : new Date().toLocaleString('en-GB');
    const activity = [{ action, time }, ...getBackupActivity()].slice(0, 20);
    localStorage.setItem(backupActivityKey, JSON.stringify(activity));
    renderBackupActivity();
  };

  const createBackup = () => {
    const data = {};
    Object.keys(localStorage).forEach((key) => {
      if (key !== backupStorageKey && key !== backupActivityKey) data[key] = localStorage.getItem(key);
    });
    const backup = { app: 'BuildPay', version: 1, createdAt: new Date().toISOString(), data };
    localStorage.setItem(backupStorageKey, JSON.stringify(backup));
    const createdAt = window.BuildPaySettings
      ? window.BuildPaySettings.formatDateTime(backup.createdAt)
      : new Date(backup.createdAt).toLocaleString('en-GB');
    if (backupStatus) backupStatus.textContent = `Backup created ${createdAt}.`;
    recordBackupActivity('Backup created');
    if (downloadBackupBtn) downloadBackupBtn.disabled = false;
  };

  const downloadLatestBackup = () => {
    const backup = localStorage.getItem(backupStorageKey);
    if (!backup) return;
    const link = document.createElement('a');
    link.href = URL.createObjectURL(new Blob([backup], { type: 'application/json' }));
    link.download = `buildpay-backup-${new Date().toISOString().slice(0, 10)}.json`;
    link.click();
    URL.revokeObjectURL(link.href);
    recordBackupActivity('Backup downloaded');
  };

  const restoreBackup = (file) => {
    const reader = new FileReader();
    reader.addEventListener('load', () => {
      try {
        const backup = JSON.parse(reader.result);
        if (backup.app !== 'BuildPay' || !backup.data || typeof backup.data !== 'object') throw new Error('Invalid backup');
        Object.entries(backup.data).forEach(([key, value]) => localStorage.setItem(key, value));
        recordBackupActivity('Backup restored');
        window.location.reload();
      } catch (error) {
        if (backupStatus) backupStatus.textContent = 'Restore failed: choose a valid BuildPay backup file.';
      }
    });
    reader.readAsText(file);
  };

  if (backupRestoreBtn && backupModal) backupRestoreBtn.addEventListener('click', () => {
    backupModal.hidden = false;
    renderBackupActivity();
  });
  if (editInformationBtn) editInformationBtn.addEventListener('click', () => tabButtons[0].click());
  document.querySelectorAll('[data-close-backup-modal]').forEach((button) => {
    button.addEventListener('click', () => { backupModal.hidden = true; });
  });
  if (createBackupBtn) createBackupBtn.addEventListener('click', createBackup);
  if (downloadBackupBtn) {
    downloadBackupBtn.disabled = !localStorage.getItem(backupStorageKey);
    downloadBackupBtn.addEventListener('click', downloadLatestBackup);
  }
  if (restoreBackupInput) restoreBackupInput.addEventListener('change', () => {
    const [file] = restoreBackupInput.files;
    if (file) restoreBackup(file);
    restoreBackupInput.value = '';
  });
  if (clearBackupActivityBtn) clearBackupActivityBtn.addEventListener('click', () => {
    localStorage.removeItem(backupActivityKey);
    renderBackupActivity();
  });
});
