'use strict';

module.exports = {
  version: 6,
  name: 'project-workspace-files',
  up(database) {
    database.exec(`
      CREATE TABLE IF NOT EXISTS project_files (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        project_id INTEGER NOT NULL,
        kind TEXT NOT NULL CHECK (kind IN ('DOCUMENT', 'PHOTO')),
        file_name TEXT NOT NULL,
        mime_type TEXT NOT NULL,
        file_path TEXT NOT NULL,
        created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
        FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
      );
      CREATE INDEX IF NOT EXISTS idx_project_files_project_id ON project_files(project_id);
    `);
  }
};