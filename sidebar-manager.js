const DEFAULT_COMPANY_PROFILE = {
  companyName: 'BuildPay Construction Ltd',
  businessAddress: 'P.O. Box 12345, Nairobi, Kenya',
  email: 'info@buildpay.co.ke',
  phone: '+254 712 345 678',
  website: 'www.buildpay.co.ke',
  registrationNumber: 'CR12/34567',
  taxPin: 'A001234567B',
  businessType: 'Construction',
  yearEstablished: '2020',
  companyLogo: 'name_logo.png'
};

window.BuildPayCompany = {
  get() {
    try {
      return { ...DEFAULT_COMPANY_PROFILE, ...JSON.parse(localStorage.getItem('buildpayCompanyProfile')) };
    } catch (error) {
      return { ...DEFAULT_COMPANY_PROFILE };
    }
  },
  save(profile) {
    const company = { ...DEFAULT_COMPANY_PROFILE, ...profile };
    localStorage.setItem('buildpayCompanyProfile', JSON.stringify(company));
    this.apply(company);
    return company;
  },
  apply(profile = this.get()) {
    document.querySelectorAll('[data-company-field]').forEach((element) => {
      const value = profile[element.dataset.companyField];
      if (value === undefined) return;
      if (element.matches('input, textarea, select')) {
        element.value = value;
      } else {
        element.textContent = value;
      }
    });

    document.querySelectorAll('[data-company-logo]').forEach((element) => {
      element.src = profile.companyLogo || DEFAULT_COMPANY_PROFILE.companyLogo;
    });
  }
};

window.addEventListener('storage', (event) => {
  if (event.key === 'buildpayCompanyProfile') {
    window.BuildPayCompany.apply();
  }
});

const DEFAULT_SYSTEM_SETTINGS = {
  language: 'English (UK)',
  currency: 'KES - Kenyan Shilling',
  dateFormat: 'DD/MM/YYYY',
  timeFormat: '24 Hour',
  defaultModule: 'Projects',
  payPeriod: 'Monthly',
  overtimeRate: '1.5',
  payDay: '25',
  salaryBasis: 'Gross Salary',
  payeRate: '30',
  taxRelief: '16800',
  healthInsuranceRate: '2.75',
  pensionRate: '6'
};

window.BuildPaySettings = {
  get() {
    try {
      return { ...DEFAULT_SYSTEM_SETTINGS, ...JSON.parse(localStorage.getItem('buildpaySettings')) };
    } catch (error) {
      return { ...DEFAULT_SYSTEM_SETTINGS };
    }
  },
  save(settings) {
    const preferences = { ...this.get(), ...settings };
    localStorage.setItem('buildpaySettings', JSON.stringify(preferences));
    this.apply(preferences);
    return preferences;
  },
  apply(settings = this.get()) {
    const language = settings.language === 'Swahili' ? 'sw-KE' : settings.language === 'English (US)' ? 'en-US' : 'en-GB';
    document.documentElement.lang = language;
    document.documentElement.dataset.language = settings.language;
    document.documentElement.dataset.currency = settings.currency;
    document.documentElement.dataset.dateFormat = settings.dateFormat;
    document.documentElement.dataset.timeFormat = settings.timeFormat;
    document.documentElement.dataset.defaultModule = settings.defaultModule;
  },
  getCurrencyCode() {
    const currency = this.get().currency;
    return currency.startsWith('USD') ? 'USD' : currency.startsWith('GBP') ? 'GBP' : 'KES';
  },
  getLocale() {
    const language = this.get().language;
    return language === 'Swahili' ? 'sw-KE' : language === 'English (US)' ? 'en-US' : 'en-GB';
  },
  formatCurrency(value) {
    return new Intl.NumberFormat(this.getLocale(), {
      style: 'currency',
      currency: this.getCurrencyCode(),
      maximumFractionDigits: 0
    }).format(Math.round(Number(value) || 0));
  },
  formatDate(value) {
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return value;
    const parts = {
      day: String(date.getDate()).padStart(2, '0'),
      month: String(date.getMonth() + 1).padStart(2, '0'),
      year: String(date.getFullYear())
    };
    if (this.get().dateFormat === 'MM/DD/YYYY') return `${parts.month}/${parts.day}/${parts.year}`;
    if (this.get().dateFormat === 'YYYY-MM-DD') return `${parts.year}-${parts.month}-${parts.day}`;
    return `${parts.day}/${parts.month}/${parts.year}`;
  },
  formatDateTime(value) {
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return value;
    const time = date.toLocaleTimeString(this.getLocale(), {
      hour: '2-digit',
      minute: '2-digit',
      hour12: this.get().timeFormat === '12 Hour'
    });
    return `${this.formatDate(date)} ${time}`;
  }
};

window.BuildPaySettings.apply();

window.addEventListener('storage', (event) => {
  if (event.key === 'buildpaySettings') window.BuildPaySettings.apply();
});

window.BuildPayPayroll = {
  calculate(payroll) {
    const settings = window.BuildPaySettings ? window.BuildPaySettings.get() : {};
    const basicSalary = Math.max(0, Number(payroll.basicSalary) || 0);
    const overtimeHours = Math.max(0, Number(payroll.overtimeHours) || 0);
    const transportAllowance = Math.max(0, Number(payroll.transportAllowance) || 0);
    const housingAllowance = Math.max(0, Number(payroll.housingAllowance) || 0);
    const overtimeRate = Math.max(1, Number(settings.overtimeRate) || 1.5);
    const hourlyRate = basicSalary / 26 / 8;
    const overtimePay = overtimeHours * hourlyRate * overtimeRate;
    const totalEarnings = basicSalary + overtimePay + transportAllowance + housingAllowance;
    const pension = totalEarnings * (Math.max(0, Number(settings.pensionRate) || 0) / 100);
    const healthInsurance = totalEarnings * (Math.max(0, Number(settings.healthInsuranceRate) || 0) / 100);
    const payeBeforeRelief = totalEarnings * (Math.max(0, Number(settings.payeRate) || 0) / 100);
    const paye = Math.max(0, payeBeforeRelief - Math.max(0, Number(settings.taxRelief) || 0));
    const advanceSalary = Math.max(0, Number(payroll.advanceSalary) || 0);
    const otherDeductions = Math.max(0, Number(payroll.otherDeductions) || 0);
    const totalDeductions = pension + healthInsurance + paye + advanceSalary + otherDeductions;

    return {
      ...payroll,
      overtimePay,
      pension,
      nssf: pension,
      healthInsurance,
      paye,
      totalEarnings,
      totalDeductions,
      netSalary: totalEarnings - totalDeductions,
      overtimeRate,
      payPeriod: settings.payPeriod || payroll.payPeriod
    };
  }
};

/**
 * Global Sidebar Navigation Manager
 * Loads and manages the reusable sidebar across all pages
 */

class SidebarManager {
  constructor() {
    this.sidebarContainer = null;
    this.sidebar = null;
    this.sidebarToggle = null;
    this.sidebarCollapseBtn = null;
    this.navItems = null;
    this.resizeHandle = null;
    this.currentPage = this.detectCurrentPage();
  }

  /**
   * Initialize the global sidebar
   */
  async init() {
    await this.loadSidebar();
    this.setupDOMReferences();
    this.setupEventListeners();
    this.setupResizeHandle();
    this.setupThemeToggle();
    window.BuildPayCompany.apply();
    this.setActivePage();
    this.loadSavedState();
  }

  /**
   * Load sidebar HTML from sidebar.html
   */
  async loadSidebar() {
    try {
      const response = await fetch('sidebar.html');
      const sidebarHTML = await response.text();

      const dashMain = document.querySelector('.dash-main');
      if (!dashMain) return;

      if (!dashMain.querySelector('#sidebar')) {
        dashMain.insertAdjacentHTML('afterbegin', sidebarHTML);
      }
    } catch (error) {
      console.error('Failed to load sidebar:', error);
    }
  }

  /**
   * Setup DOM element references
   */
  setupDOMReferences() {
    this.sidebarContainer = document.getElementById('sidebar');
    this.sidebar = this.sidebarContainer;
    this.sidebarToggle = document.getElementById('sidebarToggle');
    this.sidebarCollapseBtn = document.getElementById('sidebarCollapseBtn');
    this.navItems = document.querySelectorAll('.nav-item');
    this.resizeHandle = document.getElementById('sidebarResizeHandle');
    this.dashMain = document.querySelector('.dash-main');
    this.themeToggle = document.getElementById('themeToggle');
  }

  setupThemeToggle() {
    const savedTheme = localStorage.getItem('buildpayTheme');
    document.body.classList.toggle('light-theme', savedTheme === 'light');

    if (!this.themeToggle) return;

    this.themeToggle.setAttribute('aria-pressed', String(savedTheme === 'light'));
    this.themeToggle.addEventListener('click', () => {
      const isLight = document.body.classList.toggle('light-theme');
      localStorage.setItem('buildpayTheme', isLight ? 'light' : 'dark');
      this.themeToggle.setAttribute('aria-pressed', String(isLight));
    });
  }

  /**
   * Setup all event listeners
   */
  setupEventListeners() {
    // Sidebar toggle button
    if (this.sidebarToggle && this.sidebar) {
      this.sidebarToggle.addEventListener('click', (event) => {
        event.stopImmediatePropagation();
        this.sidebar.classList.toggle('collapsed');
        if (this.dashMain) {
          this.dashMain.classList.toggle('sidebar-collapsed');
        }
        localStorage.setItem('sidebarCollapsed', this.sidebar.classList.contains('collapsed'));
      });
    }

    // Sidebar collapse button (from within sidebar)
    if (this.sidebarCollapseBtn && this.sidebar) {
      this.sidebarCollapseBtn.addEventListener('click', () => {
        this.sidebar.classList.toggle('collapsed');
        if (this.dashMain) {
          this.dashMain.classList.toggle('sidebar-collapsed');
        }
        localStorage.setItem('sidebarCollapsed', this.sidebar.classList.contains('collapsed'));
      });
    }

    // Navigation items - update active state on click
    this.navItems.forEach(item => {
      item.addEventListener('click', (e) => {
        // Only prevent default if we're clicking on the same page
        const href = item.getAttribute('href');
        const currentFile = this.getCurrentFileName();
        if (href === currentFile) {
          e.preventDefault();
          return;
        }

        this.setActiveItem(item);
        localStorage.setItem('lastPage', href);
      });
    });
  }

  /**
   * Setup sidebar resize handle
   */
  setupResizeHandle() {
    if (!this.resizeHandle || !this.sidebar) return;

    let isResizing = false;
    let startX = 0;
    let startWidth = 0;

    this.resizeHandle.addEventListener('mousedown', (e) => {
      isResizing = true;
      startX = e.clientX;
      startWidth = this.sidebar.offsetWidth;
      this.resizeHandle.classList.add('active');
      document.body.style.userSelect = 'none';
      document.body.style.cursor = 'col-resize';
    });

    document.addEventListener('mousemove', (e) => {
      if (!isResizing) return;

      const diff = e.clientX - startX;
      const newWidth = Math.max(200, Math.min(500, startWidth + diff));
      
      this.sidebar.style.width = newWidth + 'px';
      localStorage.setItem('sidebarWidth', newWidth);
    });

    document.addEventListener('mouseup', () => {
      if (isResizing) {
        isResizing = false;
        this.resizeHandle.classList.remove('active');
        document.body.style.userSelect = '';
        document.body.style.cursor = '';
      }
    });
  }

  /**
   * Set the active navigation item based on current page
   */
  setActivePage() {
    const currentFile = this.getCurrentFileName();
    this.navItems.forEach(item => {
      item.classList.remove('active');
      const href = item.getAttribute('href');
      if (href === currentFile) {
        item.classList.add('active');
      }
    });
  }

  /**
   * Set a specific item as active
   */
  setActiveItem(item) {
    this.navItems.forEach(i => i.classList.remove('active'));
    item.classList.add('active');
  }

  /**
   * Get the current page filename
   */
  getCurrentFileName() {
    const pathname = window.location.pathname;
    return pathname.substring(pathname.lastIndexOf('/') + 1) || 'index.html';
  }

  /**
   * Detect current page from URL
   */
  detectCurrentPage() {
    const fileName = this.getCurrentFileName();
    return fileName.replace('.html', '');
  }

  /**
   * Load saved sidebar state from localStorage
   */
  loadSavedState() {
    // Restore sidebar width
    const savedWidth = localStorage.getItem('sidebarWidth');
    if (savedWidth && this.sidebar) {
      this.sidebar.style.width = savedWidth + 'px';
    }

  }

  /**
   * Handle progress fills on Dashboard
   */
  setupProgressBars() {
    const progressFills = document.querySelectorAll('.progress-fill[data-progress]');
    progressFills.forEach(fill => {
      const progress = fill.dataset.progress;
      fill.style.width = progress + '%';
    });
  }
}

/**
 * Initialize sidebar when DOM is ready
 */
document.addEventListener('DOMContentLoaded', async () => {
  const sidebarManager = new SidebarManager();
  await sidebarManager.init();
  
  // Setup page-specific functionality
  sidebarManager.setupProgressBars();
});
