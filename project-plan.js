document.addEventListener('DOMContentLoaded', async () => {
  const api = window.BuildPayDatabase;
  const projectKey = new URLSearchParams(window.location.search).get('project');
  const summary = document.getElementById('projectPlanSummary');
  const gallery = document.getElementById('planGallery');
  const count = document.getElementById('planCount');
  const uploadInput = document.getElementById('planUpload');

  const escapeHtml = value => String(value || '').replace(/[&<>'"]/g, character => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', "'": '&#39;', '"': '&quot;' }[character]));
  const getProject = async () => {
    if (!api?.listProjects || !projectKey) return null;
    const projects = await api.listProjects();
    return projects.find(item => String(item.id) === String(projectKey) || item.projectCode === projectKey);
  };

  const project = await getProject();
  if (!project) {
    summary.textContent = 'Project was not found.';
    return;
  }

  document.getElementById('projectPlanTitle').textContent = `${project.name} · Project Plan`;
  document.getElementById('projectPlanSubtitle').textContent = `${project.projectCode} · ${project.location || 'Location not specified'}`;
  summary.innerHTML = `<h2>${escapeHtml(project.name)}</h2><div class="project-resource-stat"><strong>${escapeHtml(project.status)}</strong><small>Status</small></div><div class="project-resource-stat"><strong>${project.progress || 0}%</strong><small>Overall progress</small></div><div class="project-resource-stat"><strong>${escapeHtml(project.manager || 'Not assigned')}</strong><small>Project manager</small></div>`;

  const render = async () => {
    const files = await api.listProjectFiles(project.id, 'PHOTO');
    count.textContent = `${files.length} image${files.length === 1 ? '' : 's'}`;
    gallery.replaceChildren();
    if (!files.length) {
      gallery.innerHTML = '<p class="project-resource-empty">No project images uploaded yet.</p>';
      return;
    }
    files.forEach(file => {
      const figure = document.createElement('figure');
      figure.className = 'project-plan-card';
      figure.innerHTML = `<img alt="${escapeHtml(file.fileName)}"><figcaption><span title="${escapeHtml(file.fileName)}">${escapeHtml(file.fileName)}</span><button type="button">Delete</button></figcaption>`;
      api.readProjectFile(file.id).then(loaded => { if (loaded?.dataUrl) figure.querySelector('img').src = loaded.dataUrl; });
      figure.querySelector('img').addEventListener('dblclick', async () => {
        const loaded = await api.readProjectFile(file.id);
        if (loaded?.dataUrl) window.open(loaded.dataUrl, '_blank');
      });
      figure.querySelector('button').addEventListener('click', async () => {
        if (confirm(`Delete ${file.fileName}?`)) { await api.deleteProjectFile(file.id); render(); }
      });
      gallery.appendChild(figure);
    });
  };

  uploadInput.addEventListener('change', async event => {
    for (const file of event.target.files) {
      const data = new Uint8Array(await file.arrayBuffer());
      await api.saveProjectFile(project.id, { kind: 'PHOTO', fileName: file.name, mimeType: file.type, data });
    }
    event.target.value = '';
    render();
  });
  render();
});
