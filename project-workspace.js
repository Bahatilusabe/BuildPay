document.addEventListener('DOMContentLoaded', async () => {
  const api = window.BuildPayDatabase;
  const params = new URLSearchParams(window.location.search);
  const projectKey = params.get('project');
  const summary = document.getElementById('workspaceSummary');
  const documentList = document.getElementById('documentList');
  const photoGrid = document.getElementById('photoGrid');
  const documentCount = document.getElementById('documentCount');
  const photoCount = document.getElementById('photoCount');

  if (!api?.listProjects || !projectKey) {
    summary.textContent = 'Project was not selected.';
    return;
  }

  const projects = await api.listProjects();
  const project = projects.find(item => String(item.id) === String(projectKey) || item.projectCode === projectKey);
  if (!project) {
    summary.textContent = 'Project was not found in the database.';
    return;
  }

  document.getElementById('workspaceTitle').textContent = project.name;
  document.getElementById('workspaceSubtitle').textContent = `${project.projectCode} · ${project.location || 'Location not specified'}`;
  summary.innerHTML = `<div><strong>${project.status}</strong><span>${project.progress || 0}% complete</span></div><div><small>Manager</small><b>${project.manager || project.client || 'Not assigned'}</b></div><div><small>Budget</small><b>KES ${Number(project.budget || 0).toLocaleString('en-KE')}</b></div><div><small>Labor cost</small><b>KES ${Number(project.laborCost || 0).toLocaleString('en-KE')}</b></div>`;

  const renderFiles = async () => {
    const [documents, photos] = await Promise.all([api.listProjectFiles(project.id, 'DOCUMENT'), api.listProjectFiles(project.id, 'PHOTO')]);
    documentCount.textContent = `${documents.length} file${documents.length === 1 ? '' : 's'}`;
    photoCount.textContent = `${photos.length} photo${photos.length === 1 ? '' : 's'}`;
    documentList.replaceChildren();
    photoGrid.replaceChildren();
    if (!documents.length) documentList.innerHTML = '<p>No project documents uploaded yet.</p>';
    documents.forEach(file => {
      const row = document.createElement('div');
      row.className = 'workspace-file-row';
      row.innerHTML = `<span class="workspace-file-icon">${file.mimeType === 'application/pdf' ? 'PDF' : 'IMG'}</span><div><strong>${file.fileName}</strong><small>${new Date(file.createdAt).toLocaleString()}</small></div><button type="button" class="workspace-open-file">Open</button><button type="button" class="workspace-delete-file">Delete</button>`;
      row.querySelector('.workspace-open-file').addEventListener('click', async () => {
        const loaded = await api.readProjectFile(file.id);
        if (loaded?.dataUrl) window.open(loaded.dataUrl, '_blank');
      });
      row.querySelector('.workspace-delete-file').addEventListener('click', async () => {
        if (confirm(`Delete ${file.fileName}?`)) { await api.deleteProjectFile(file.id); renderFiles(); }
      });
      documentList.appendChild(row);
    });
    if (!photos.length) photoGrid.innerHTML = '<p>No progress photos uploaded yet.</p>';
    photos.forEach(file => {
      const figure = document.createElement('figure');
      figure.className = 'workspace-photo-card';
      figure.innerHTML = `<img alt="${file.fileName}"><figcaption><span>${file.fileName}</span><button type="button">Delete</button></figcaption>`;
      api.readProjectFile(file.id).then(loaded => { if (loaded?.dataUrl) figure.querySelector('img').src = loaded.dataUrl; });
      figure.querySelector('button').addEventListener('click', async () => { if (confirm(`Delete ${file.fileName}?`)) { await api.deleteProjectFile(file.id); renderFiles(); } });
      photoGrid.appendChild(figure);
    });
  };

  const upload = async (input, kind) => {
    for (const file of input.files) {
      const data = new Uint8Array(await file.arrayBuffer());
      await api.saveProjectFile(project.id, { kind, fileName: file.name, mimeType: file.type, data });
    }
    input.value = '';
    renderFiles();
  };
  document.getElementById('documentUpload').addEventListener('change', event => upload(event.target, 'DOCUMENT'));
  document.getElementById('photoUpload').addEventListener('change', event => upload(event.target, 'PHOTO'));
  renderFiles();
});
