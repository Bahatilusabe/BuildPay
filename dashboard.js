// Dashboard interactivity
document.addEventListener('DOMContentLoaded', () => {
  const databaseApi = window.BuildPayDatabase;
  const sidebarToggle = document.getElementById('sidebarToggle');
  const sidebarCollapseBtn = document.getElementById('sidebarCollapseBtn');
  const sidebar = document.querySelector('.sidebar');
  const dashMain = document.querySelector('.dash-main');
  const navItems = document.querySelectorAll('.nav-item');
  const projectCards = document.querySelectorAll('.project-card');
  const progressFills = document.querySelectorAll('.progress-fill[data-progress]');
  const resizeHandle = document.getElementById('sidebarResizeHandle');

  // Load saved sidebar width from localStorage
  const savedSidebarWidth = localStorage.getItem('sidebarWidth');
  if (savedSidebarWidth && sidebar) {
    sidebar.style.width = savedSidebarWidth + 'px';
  }

  // Set progress bar widths from data attributes
  progressFills.forEach(fill => {
    const progress = fill.dataset.progress;
    fill.style.width = progress + '%';
  });

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

  // Update active nav item on click (allows navigation)
  navItems.forEach(item => {
    item.addEventListener('click', () => {
      navItems.forEach(i => i.classList.remove('active'));
      item.classList.add('active');
      // Let default link navigation occur
    });
  });

  projectCards.forEach(card => {
    const detailBtn = card.querySelector('.btn-small');
    if (detailBtn) {
      detailBtn.addEventListener('click', () => {
        const title = card.querySelector('h3').textContent;
        alert(`Viewing details for: ${title}`);
      });
    }
  });

  const formatCurrency = (value) => `KES ${Math.round(Number(value) || 0).toLocaleString('en-KE')}`;

  const navigate = (page) => {
    window.location.href = page;
  };

  const wireDashboardActions = () => {
    document.querySelector('.template-user-button')?.addEventListener('click', () => navigate('settings.html'));
    document.querySelector('.template-card-heading button')?.addEventListener('click', () => navigate('reports.html'));
    document.querySelector('.template-attendance-overview .template-card-heading button')?.addEventListener('click', () => navigate('attendance.html'));
    document.querySelector('.template-payroll-card .template-card-heading button')?.addEventListener('click', () => navigate('payroll.html'));
    document.querySelector('.template-cost .template-card-heading button')?.addEventListener('click', () => navigate('reports.html'));
    document.querySelector('.template-activity .template-card-heading a')?.addEventListener('click', (event) => {
      event.preventDefault();
      navigate('reports.html');
    });
    document.querySelector('.template-projects .template-card-heading a')?.addEventListener('click', (event) => {
      event.preventDefault();
      navigate('projects.html');
    });
    document.querySelectorAll('.template-project-row').forEach((row) => {
      row.addEventListener('click', () => navigate('projects.html'));
      row.style.cursor = 'pointer';
    });
  };

  const notificationButton = document.querySelector('.template-icon-button');
  if (notificationButton) {
    notificationButton.addEventListener('click', async () => {
      const activities = databaseApi?.getDashboardActivities
        ? await databaseApi.getDashboardActivities(5)
        : [];
      const message = activities.length
        ? activities.map(activity => `${activity.module}: ${activity.description || activity.action}`).join('\n')
        : 'No recent database activity.';
      alert(message);
    });
  }

  wireDashboardActions();

  const buildChartPath = (values, maximum) => {
    if (!values.length) return 'M0 190';
    const width = 600;
    const height = 170;
    return values.map((value, index) => {
      const x = values.length === 1 ? width / 2 : (index / (values.length - 1)) * width;
      const y = height - ((Number(value) || 0) / Math.max(1, maximum)) * height;
      return `${index ? 'L' : 'M'}${x.toFixed(1)} ${Math.max(8, y).toFixed(1)}`;
    }).join(' ');
  };

  const renderPayrollTrend = (trend) => {
    const rows = trend.slice(-5);
    const maximum = Math.max(1, ...rows.flatMap(row => [row.grossPay, row.deductions, row.netPay].map(Number)));
    const svg = document.querySelector('.template-line-chart svg');
    if (svg) {
      svg.querySelector('.line-gross').setAttribute('d', buildChartPath(rows.map(row => row.grossPay), maximum));
      svg.querySelector('.line-deductions').setAttribute('d', buildChartPath(rows.map(row => row.deductions), maximum));
      svg.querySelector('.line-net').setAttribute('d', buildChartPath(rows.map(row => row.netPay), maximum));
    }
    const labels = document.querySelector('.template-line-chart .chart-x-labels');
    if (labels) labels.innerHTML = rows.map(row => `<span>${row.payPeriod}</span>`).join('');
    const yLabels = document.querySelector('.template-line-chart .chart-y-labels');
    if (yLabels) {
      const steps = [1, 0.833, 0.666, 0.5, 0.333, 0.166, 0];
      yLabels.innerHTML = steps.map(step => `<span>${formatCurrency(maximum * step)}</span>`).join('');
    }
    const bars = document.querySelector('.template-bar-chart');
    if (bars) bars.innerHTML = rows.map(row => {
      const payrollHeight = Math.max(3, (Number(row.grossPay) / maximum) * 100);
      const netHeight = Math.max(3, (Number(row.netPay) / maximum) * 100);
      return `<div><i style="height:${payrollHeight}%"></i><i class="bar-green" style="height:${netHeight}%"></i><small>${row.payPeriod}</small></div>`;
    }).join('');
  };

  const syncDashboardFromDatabase = async () => {
    if (!databaseApi?.getDashboardSummary) return;

    try {
      const summary = await databaseApi.getDashboardSummary('2025-05');
      const kpiValues = document.querySelectorAll('.template-kpi strong');
      [summary.totalEmployees, formatCurrency(summary.totalPayroll), `${summary.attendanceRate}%`, summary.activeProjects]
        .forEach((value, index) => {
          if (kpiValues[index]) kpiValues[index].textContent = value;
        });

      const quickValues = document.querySelectorAll('.template-quick-list b');
      [summary.activeProjects, summary.activeEmployees, formatCurrency(summary.totalPayroll), `${summary.attendanceRate}%`, summary.pendingPayslips, summary.overtimeHours]
        .forEach((value, index) => {
          if (quickValues[index]) quickValues[index].textContent = value;
        });

      const miniValues = document.querySelectorAll('.template-mini-stats b');
      if (miniValues[0]) miniValues[0].textContent = formatCurrency(summary.totalPayroll);
      if (miniValues[1]) miniValues[1].textContent = `${summary.attendanceRate}%`;

      const attendanceValue = document.querySelector('.template-donut strong');
      if (attendanceValue) attendanceValue.textContent = `${summary.attendanceRate}%`;
      const attendanceCounts = [summary.presentDays, summary.absentDays, summary.leaveDays, summary.lateDays];
      document.querySelectorAll('.template-attendance-list b').forEach((element, index) => {
        element.textContent = attendanceCounts[index] || 0;
      });

      const costValue = document.querySelector('.template-cost-donut strong');
      if (costValue) costValue.textContent = formatCurrency(summary.totalPayroll);

      const trend = databaseApi.getPayrollTrend ? await databaseApi.getPayrollTrend(5) : [];
      renderPayrollTrend(trend);

      const reportSummary = databaseApi.getReportSummary
        ? await databaseApi.getReportSummary('2025-05')
        : null;
      if (reportSummary) {
        const costTotal = Number(reportSummary.payroll?.grossPay || 0);
        if (costValue) costValue.textContent = formatCurrency(costTotal);
        const costList = document.querySelector('.template-cost-list');
        const laborCost = (reportSummary.projects || []).reduce((sum, project) => sum + Number(project.laborCost || 0), 0);
        const budget = (reportSummary.projects || []).reduce((sum, project) => sum + Number(project.budget || 0), 0);
        if (costList) costList.innerHTML = `<span><i class="dot-blue"></i>Labor Cost - <b>${formatCurrency(laborCost)}</b></span><span><i class="dot-violet"></i>Payroll Cost - <b>${formatCurrency(reportSummary.payroll?.netPay)}</b></span><span><i class="dot-orange"></i>Budget Remaining - <b>${formatCurrency(Math.max(0, budget - laborCost))}</b></span>`;
      }

      const projects = await databaseApi.getDashboardProjects();
      const projectRows = document.querySelectorAll('.template-project-row');
      if (!projects.length) {
        projectRows.forEach(row => { row.style.display = 'none'; });
      } else {
        projectRows.forEach((row, index) => {
          const project = projects[index];
          if (!project) {
            row.style.display = 'none';
            return;
          }
          row.style.display = '';
          const name = row.querySelector('strong');
          const location = row.querySelector('small');
          const status = row.querySelector('.template-status');
          const progress = row.querySelector('.project-progress b');
          const fill = row.querySelector('.progress-fill');
          if (name) name.textContent = project.name;
          if (location) location.textContent = project.location || 'Location not specified';
          if (status) status.textContent = project.status;
          if (progress) progress.textContent = `${project.progress || 0}%`;
          if (fill) fill.style.width = `${project.progress || 0}%`;
        });
      }

      const activities = await databaseApi.getDashboardActivities(4);
      const notificationDot = document.querySelector('.notification-dot');
      if (notificationDot) notificationDot.textContent = String(activities.length);
      const activityList = document.querySelector('.template-activity-list');
      if (activityList) {
        activityList.innerHTML = activities.length
          ? activities.map(activity => `<span><i>◆</i><b>${activity.description || activity.action}</b><small>${activity.module}</small></span>`).join('')
          : '<span><i>◆</i><b>No recorded activity</b><small>SQLite</small></span>';
      }
    } catch (error) {
      console.error('Unable to sync dashboard from SQLite', error);
    }
  };

  syncDashboardFromDatabase();

  // Simulate loading animation
  window.addEventListener('load', () => {
    const cards = document.querySelectorAll('.metric-card, .project-card');
    cards.forEach((card, index) => {
      card.style.opacity = '0';
      card.style.transform = 'translateY(20px)';
      setTimeout(() => {
        card.style.transition = 'all 0.6s cubic-bezier(0.22, 1, 0.36, 1)';
        card.style.opacity = '1';
        card.style.transform = 'translateY(0)';
      }, index * 50);
    });
  });
});
