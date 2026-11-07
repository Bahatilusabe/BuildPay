document.addEventListener('DOMContentLoaded', () => {
  const databaseApi = window.BuildPayDatabase;
  const sidebarToggle = document.getElementById('sidebarToggle');
  const projectsToggle = document.getElementById('projectsToggle');
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

  const toggleSidebar = () => {
    if (!sidebar) return;
    sidebar.classList.toggle('collapsed');
    if (dashMain) dashMain.classList.toggle('sidebar-collapsed');
  };

  if (sidebarToggle) sidebarToggle.addEventListener('click', toggleSidebar);
  if (projectsToggle) projectsToggle.addEventListener('click', toggleSidebar);
  if (sidebarCollapseBtn) sidebarCollapseBtn.addEventListener('click', toggleSidebar);

  navItems.forEach((item) => {
    item.addEventListener('click', () => {
      navItems.forEach((navItem) => navItem.classList.remove('active'));
      item.classList.add('active');
    });
  });

  const searchInput = document.getElementById('projectSearchInput');
  const getCards = () => document.querySelectorAll('.project-card-item');

  if (searchInput) {
    searchInput.addEventListener('input', (event) => {
      const term = event.target.value.trim().toLowerCase();

      getCards().forEach((card) => {
        const text = [
          card.dataset.name,
          card.dataset.location,
          card.dataset.id
        ].join(' ').toLowerCase();

        card.style.display = text.includes(term) ? '' : 'none';
      });
    });
  }

  const statusFilter = document.getElementById('projectStatusFilter');
  if (statusFilter) {
    statusFilter.addEventListener('change', (event) => {
      const value = event.target.value;
      getCards().forEach((card) => {
        const visible = value === 'all' || card.dataset.status === value;
        card.style.display = visible ? '' : 'none';
      });
    });
  }

  const newProjectButton = document.querySelector('.new-project-button');
  const projectCreateModal = document.getElementById('projectCreateModal');
  const newProjectForm = document.getElementById('newProjectForm');
  const projectGrid = document.getElementById('projectGrid');
  const projectViewModal = document.getElementById('projectViewModal');
  const projectViewHero = document.getElementById('projectViewHero');
  const projectViewTitle = document.getElementById('projectViewTitle');
  const projectViewSubtitle = document.getElementById('projectViewSubtitle');
  const projectViewDetails = document.getElementById('projectViewDetails');
  const projectViewEditButton = document.getElementById('projectViewEditButton');
  const projectViewEditPanel = document.getElementById('projectViewEditPanel');
  const projectViewEditForm = document.getElementById('projectViewEditPanel');
  let activeProjectCard = null;
  let activeProject = null;

  const openProjectView = (card) => {
    if (!projectViewModal) return;
    activeProjectCard = card;
    activeProject = card.projectData || null;
    const name = card.dataset.name || card.querySelector('h3')?.textContent || 'Project';
    const id = card.dataset.id || '';
    const image = card.querySelector('.projects-template-image');
    const title = card.querySelector('h3')?.textContent || name;
    const status = card.querySelector('.projects-template-status-row span')?.textContent || 'PROJECT';
    const location = card.querySelector('p')?.textContent.replace(/♟.*$/, '').trim() || 'Location not specified';
    const manager = card.querySelector('p span')?.textContent.replace('♟', '').trim() || 'Manager not specified';
    const progress = card.querySelector('.projects-template-progress strong')?.textContent || '0%';
    const meta = [...card.querySelectorAll('.projects-template-meta span')].map((item) => ({ label: item.querySelector('small')?.textContent || '', value: item.querySelector('b')?.textContent || '' }));
    const dates = [...card.querySelectorAll('footer span')].map((item) => item.textContent.replace('▣', '').trim());
    projectViewTitle.textContent = title;
    projectViewSubtitle.textContent = `${id} · ${status}`;
    projectViewHero.style.backgroundImage = image ? getComputedStyle(image).backgroundImage : '';
    projectViewDetails.innerHTML = `<div class="project-view-detail-highlight"><strong>${progress}</strong><span>Overall progress</span></div><div class="project-view-detail-grid"><span><small>Location</small><b>${location}</b></span><span><small>Manager</small><b>${manager}</b></span>${meta.map((item) => `<span><small>${item.label}</small><b>${item.value}</b></span>`).join('')}<span><small>Starting date</small><b>${dates[0] || 'Not specified'}</b></span><span><small>Completion date</small><b>${dates[1] || 'Not specified'}</b></span></div>`;
    projectViewModal.hidden = false;
    if (projectViewEditPanel) projectViewEditPanel.hidden = true;
  };

  document.addEventListener('dblclick', (event) => {
    const card = event.target.closest('.project-card-item');
    if (card) {
      openProjectView(card);
    }
  });

  document.querySelectorAll('[data-close-project-view]').forEach((button) => {
    button.addEventListener('click', () => { if (projectViewModal) projectViewModal.hidden = true; });
  });

  if (projectViewEditButton) {
    projectViewEditButton.addEventListener('click', () => {
      if (!activeProjectCard || !projectViewEditPanel) return;
      projectViewEditPanel.hidden = false;
      document.getElementById('projectEditManager').value = activeProjectCard.querySelector('.projects-template-card-content > p span')?.textContent.replace('♟', '').trim() || '';
      document.getElementById('projectEditProgress').value = parseInt(activeProjectCard.querySelector('.projects-template-progress strong')?.textContent, 10) || 0;
      document.getElementById('projectEditCompletion').value = '';
    });
  }

  const cancelProjectEdit = document.getElementById('projectViewCancelEdit');
  if (cancelProjectEdit) cancelProjectEdit.addEventListener('click', () => { projectViewEditPanel.hidden = true; });

  if (projectViewEditForm) {
    projectViewEditForm.addEventListener('submit', async (event) => {
      event.preventDefault();
      if (!activeProjectCard || !activeProject || !databaseApi?.updateProject) return;
      const manager = document.getElementById('projectEditManager').value.trim();
      const progress = Math.max(0, Math.min(100, Number(document.getElementById('projectEditProgress').value) || 0));
      const completion = document.getElementById('projectEditCompletion').value;
      await databaseApi.updateProject(activeProject.id, {
        ...activeProject,
        manager,
        progress,
        expectedEndDate: completion || activeProject.expectedEndDate
      });
      if (projectViewEditPanel) projectViewEditPanel.hidden = true;
      if (projectViewModal) projectViewModal.hidden = true;
      await loadProjectsFromDatabase();
    });
  }

  const projectEditBlueprint = document.getElementById('projectEditBlueprint');
  if (projectEditBlueprint) projectEditBlueprint.addEventListener('change', () => {
    const [file] = projectEditBlueprint.files;
    if (!file || !file.type.startsWith('image/')) return;
    const reader = new FileReader();
    reader.addEventListener('load', () => {
      document.getElementById('projectBlueprintPreview').style.backgroundImage = `url("${reader.result}")`;
      document.getElementById('projectBlueprintPreview').classList.add('has-uploaded-blueprint');
    });
    reader.readAsDataURL(file);
  });

  document.getElementById('projectPlansShortcut')?.addEventListener('click', () => {
    if (activeProject?.id) window.location.href = `project-plan.html?project=${encodeURIComponent(activeProject.id)}`;
  });
  document.getElementById('projectDocsShortcut')?.addEventListener('click', () => {
    if (activeProject?.id) window.location.href = `project-documentation.html?project=${encodeURIComponent(activeProject.id)}`;
  });

  const formatKes = (value) => `KES ${Number(value || 0).toLocaleString('en-KE')}`;
  const statusClass = { active: 'status-active', planning: 'status-planning', 'on-hold': 'status-onhold', completed: 'status-completed' };
  const statusLabel = { active: 'ACTIVE', planning: 'PLANNING', 'on-hold': 'ON HOLD', completed: 'COMPLETED' };
  const progressClass = (value) => `project-progress-${Math.max(0, Math.min(100, Number(value) || 0))}`;

  if (newProjectButton && projectCreateModal) {
    newProjectButton.addEventListener('click', () => {
      projectCreateModal.hidden = false;
      projectCreateModal.querySelector('input[name="name"]').focus();
    });
  }

  document.querySelectorAll('[data-close-project-modal]').forEach((button) => {
    button.addEventListener('click', () => {
      if (projectCreateModal) projectCreateModal.hidden = true;
    });
  });

  if (newProjectForm && projectGrid) {
    newProjectForm.addEventListener('submit', async (event) => {
      event.preventDefault();
      const values = Object.fromEntries(new FormData(newProjectForm));
      const status = values.status;
      const progress = Math.max(0, Math.min(100, Number(values.progress) || 0));
      if (databaseApi?.createProject) {
        try {
          await databaseApi.createProject({
            projectCode: values.id,
            name: values.name,
            location: values.location,
            client: values.manager,
            manager: values.manager,
            startDate: values.startDate,
            expectedEndDate: values.completionDate,
            budget: values.budget,
            laborCost: values.labor,
            workerCount: values.workers,
            status: String(status).toUpperCase().replace('-', '_'),
            progress
          });
        } catch (error) {
          console.error('Unable to save project to SQLite', error);
          return;
        }
      }
      projectCreateModal.hidden = true;
      newProjectForm.reset();
      await loadProjectsFromDatabase();
    });
  }

  const loadProjectsFromDatabase = async () => {
    if (!databaseApi?.listProjects || !projectGrid) return;
    try {
      const projects = await databaseApi.listProjects();
      if (!Array.isArray(projects)) return;
      const statusClasses = { ACTIVE: 'status-active', PLANNING: 'status-planning', ON_HOLD: 'status-onhold', COMPLETED: 'status-completed' };
      projectGrid.replaceChildren();
      projects.forEach((project) => {
        const status = String(project.status || 'PLANNING').toUpperCase();
        const card = document.createElement('article');
        card.className = 'projects-template-card project-card-item';
        card.dataset.status = status.toLowerCase().replace('_', '-');
        card.dataset.name = project.name;
        card.dataset.location = project.location || '';
        card.dataset.id = project.projectCode;
        card.projectData = project;
        card.innerHTML = `<div class="projects-template-image skyline-image"></div><div class="projects-template-card-content"><div class="projects-template-status-row"><span class="${statusClasses[status] || 'status-planning'}">${status.replace('_', ' ')}</span><button type="button" aria-label="${project.name} actions">...</button></div><div class="projects-template-title-row"><h3>${project.name}</h3><b>${project.projectCode}</b></div><p>⌖ ${project.location || 'Location not specified'} <span>♟ ${project.manager || project.client || 'Manager not specified'}</span></p><div class="projects-template-progress"><i><em></em></i><strong>${project.progress || 0}%</strong></div><div class="projects-template-meta"><span><small>Budget</small><b>${formatKes(project.budget)}</b></span><span><small>Labor Cost</small><b>${formatKes(project.laborCost)}</b></span><span><small>Workers</small><b>${project.workerCount || 0}</b></span></div><footer><span>▣ ${project.startDate || 'Start not set'}</span><span>▣ ${project.expectedEndDate || 'End not set'}</span></footer></div>`;
        const fill = card.querySelector('.projects-template-progress em');
        if (fill) fill.style.width = `${project.progress || 0}%`;
        projectGrid.appendChild(card);
      });
    } catch (error) {
      console.error('Unable to load projects from SQLite', error);
    }
  };

  loadProjectsFromDatabase();
});
