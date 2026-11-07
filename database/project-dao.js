'use strict';

class ProjectDao {
  constructor(database) {
    this.database = database;
  }

  list() {
    return this.database.prepare(`
      SELECT id, project_code AS projectCode, name, location, client, description,
             start_date AS startDate, expected_end_date AS expectedEndDate,
             budget, status, progress, manager, labor_cost AS laborCost,
             worker_count AS workerCount, created_at AS createdAt, updated_at AS updatedAt
      FROM projects
      ORDER BY updated_at DESC, name
    `).all();
  }

  create(project) {
    const result = this.database.prepare(`
      INSERT INTO projects (
        project_code, name, location, client, description,
        start_date, expected_end_date, budget, status, progress, manager, labor_cost, worker_count
      ) VALUES (@projectCode, @name, @location, @client, @description, @startDate, @expectedEndDate, @budget, @status, @progress, @manager, @laborCost, @workerCount)
    `).run({
      projectCode: project.projectCode,
      name: project.name,
      location: project.location || null,
      client: project.client || null,
      description: project.description || null,
      startDate: project.startDate || null,
      expectedEndDate: project.expectedEndDate || null,
      budget: Number(project.budget) || 0,
      status: project.status || 'PLANNING',
      progress: Math.max(0, Math.min(100, Number(project.progress) || 0)),
      manager: project.manager || null,
      laborCost: Number(project.laborCost) || 0,
      workerCount: Math.max(0, Number(project.workerCount) || 0)
    });
    return this.database.prepare(`
      SELECT id, project_code AS projectCode, name, location, client, description,
             start_date AS startDate, expected_end_date AS expectedEndDate,
             budget, status, progress, manager, labor_cost AS laborCost,
             worker_count AS workerCount
      FROM projects WHERE id = ?
    `).get(result.lastInsertRowid);
  }

  update(id, project) {
    this.database.prepare(`
      UPDATE projects
      SET name = @name, location = @location, client = @client, description = @description,
          start_date = @startDate, expected_end_date = @expectedEndDate, budget = @budget,
          status = @status, progress = @progress, manager = @manager,
          labor_cost = @laborCost, worker_count = @workerCount, updated_at = CURRENT_TIMESTAMP
      WHERE id = @id
    `).run({
      id: Number(id),
      name: project.name,
      location: project.location || null,
      client: project.client || null,
      description: project.description || null,
      startDate: project.startDate || null,
      expectedEndDate: project.expectedEndDate || null,
      budget: Number(project.budget) || 0,
      status: project.status || 'PLANNING',
      progress: Math.max(0, Math.min(100, Number(project.progress) || 0)),
      manager: project.manager || null,
      laborCost: Number(project.laborCost) || 0,
      workerCount: Math.max(0, Number(project.workerCount) || 0)
    });
    return this.database.prepare('SELECT * FROM projects WHERE id = ?').get(Number(id));
  }

  delete(id) {
    return this.database.prepare('DELETE FROM projects WHERE id = ?').run(Number(id)).changes > 0;
  }

  listFiles(projectId, kind) {
    return this.database.prepare(`
      SELECT id, project_id AS projectId, kind, file_name AS fileName,
             mime_type AS mimeType, file_path AS filePath, created_at AS createdAt
      FROM project_files
      WHERE project_id = ? AND (? IS NULL OR kind = ?)
      ORDER BY created_at DESC, id DESC
    `).all(Number(projectId), kind || null, kind || null);
  }

  addFile(projectId, file) {
    const result = this.database.prepare(`
      INSERT INTO project_files (project_id, kind, file_name, mime_type, file_path)
      VALUES (?, ?, ?, ?, ?)
    `).run(Number(projectId), file.kind, file.fileName, file.mimeType, file.filePath);
    return this.database.prepare(`
      SELECT id, project_id AS projectId, kind, file_name AS fileName,
             mime_type AS mimeType, file_path AS filePath, created_at AS createdAt
      FROM project_files WHERE id = ?
    `).get(result.lastInsertRowid);
  }

  deleteFile(id) {
    return this.database.prepare('DELETE FROM project_files WHERE id = ?').run(Number(id)).changes > 0;
  }
}

module.exports = { ProjectDao };
