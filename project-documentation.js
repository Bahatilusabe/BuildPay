document.addEventListener('DOMContentLoaded', async () => {
  const api = window.BuildPayDatabase;
  const projectKey = new URLSearchParams(window.location.search).get('project');
  const summary = document.getElementById('projectDocumentationSummary');
  const list = document.getElementById('documentationList');
  const count = document.getElementById('documentationCount');
  const uploadInput = document.getElementById('documentationUpload');

  const escapeHtml = value => String(value || '').replace(/[&<>'"]/g, character => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', "'": '&#39;', '"': '&quot;' }[character]));
  if (!api?.listProjects || !projectKey) {
    summary.textContent = 'Project was not selected.';
    return;
  }
  const projects = await api.listProjects();
  const project = projects.find(item => String(item.id) === String(projectKey) || item.projectCode === projectKey);
  if (!project) {
    summary.textContent = 'Project was not found.';
    return;
  }

  document.getElementById('projectDocumentationTitle').textContent = `${project.name} · Documentation`;
  document.getElementById('projectDocumentationSubtitle').textContent = `${project.projectCode} · ${project.location || 'Location not specified'}`;
  summary.innerHTML = `<h2>${escapeHtml(project.name)}</h2><div class="project-resource-stat"><strong>${escapeHtml(project.status)}</strong><small>Status</small></div><div class="project-resource-stat"><strong>${project.progress || 0}%</strong><small>Overall progress</small></div><div class="project-resource-stat"><strong>${escapeHtml(project.manager || 'Not assigned')}</strong><small>Project manager</small></div>`;

  const fileLabel = file => {
    const extension = file.fileName.includes('.') ? file.fileName.split('.').pop().toUpperCase() : 'FILE';
    return extension.slice(0, 5);
  };
  const render = async () => {
    const files = await api.listProjectFiles(project.id, 'DOCUMENT');
    count.textContent = `${files.length} file${files.length === 1 ? '' : 's'}`;
    list.replaceChildren();
    if (!files.length) {
      list.innerHTML = '<p class="project-resource-empty">No project documents uploaded yet.</p>';
      return;
    }
    files.forEach(file => {
      const row = document.createElement('div');
      row.className = 'project-resource-file';
      row.innerHTML = `<span class="project-resource-file-icon">${fileLabel(file)}</span><div class="project-resource-file-info"><strong title="${escapeHtml(file.fileName)}">${escapeHtml(file.fileName)}</strong><small>${escapeHtml(file.mimeType)} · ${new Date(file.createdAt).toLocaleString()}</small></div><button type="button" class="project-open-file">Open</button><button type="button" class="project-download-file">Download</button><button type="button" class="project-delete-file">Delete</button>`;
      row.querySelector('.project-open-file').addEventListener('click', async () => {
        const loaded = await api.readProjectFile(file.id);
        if (loaded?.dataUrl) window.open(loaded.dataUrl, '_blank');
      });
      row.querySelector('.project-download-file').addEventListener('click', async () => {
        const loaded = await api.readProjectFile(file.id);
        if (!loaded?.dataUrl) return;
        const link = document.createElement('a');
        link.href = loaded.dataUrl;
        link.download = file.fileName;
        link.click();
      });
      row.querySelector('.project-delete-file').addEventListener('click', async () => {
        if (confirm(`Delete ${file.fileName}?`)) { await api.deleteProjectFile(file.id); render(); }
      });
      list.appendChild(row);
    });
  };

  uploadInput.addEventListener('change', async event => {
    for (const file of event.target.files) {
      const data = new Uint8Array(await file.arrayBuffer());
      await api.saveProjectFile(project.id, { kind: 'DOCUMENT', fileName: file.name, mimeType: file.type, data });
    }
    event.target.value = '';
    render();
  });
  render();
});
