'use strict';

const columnExists = (database, columnName) => Boolean(database.prepare(`
  SELECT 1 FROM pragma_table_info('projects') WHERE name = ?
`).get(columnName));

module.exports = {
  version: 3,
  name: 'project-details',
  up(database) {
    if (!columnExists(database, 'manager')) {
      database.exec('ALTER TABLE projects ADD COLUMN manager TEXT');
    }
    if (!columnExists(database, 'labor_cost')) {
      database.exec('ALTER TABLE projects ADD COLUMN labor_cost REAL NOT NULL DEFAULT 0');
    }
    if (!columnExists(database, 'worker_count')) {
      database.exec('ALTER TABLE projects ADD COLUMN worker_count INTEGER NOT NULL DEFAULT 0');
    }

    const projects = [
      ['PRJ001', 'Skyline Apartments', 'Nairobi, Kenya', 'Skyline Holdings', '2025-01-15', '2025-12-20', 8500000, 'ACTIVE', 72, 'James Mwangi', 2450000, 42],
      ['PRJ002', 'Greenfield Mall', 'Kisumu, Kenya', 'Greenfield Group', '2025-02-10', '2026-01-30', 12000000, 'PLANNING', 35, 'Sarah Otieno', 3200000, 28],
      ['PRJ003', 'Riverside Towers', 'Mombasa, Kenya', 'Riverside Properties', '2025-03-05', '2026-12-15', 15900000, 'ON_HOLD', 60, 'David Kimani', 5400000, 54],
      ['PRJ004', 'Sunset Villas', 'Nakuru, Kenya', 'Sunset Homes', '2025-04-01', '2025-11-30', 6800000, 'ACTIVE', 48, 'Grace Wanjiku', 2100000, 36],
      ['PRJ005', 'The Horizon Estate', 'Eldoret, Kenya', 'Horizon Developers', '2025-05-12', '2026-02-28', 9500000, 'PLANNING', 25, 'Michael Njogu', 2850000, 30],
      ['PRJ006', 'City Center Complex', 'Thika, Kenya', 'City Center Ltd', '2024-01-10', '2024-12-10', 20000000, 'COMPLETED', 100, 'Peter Kamau', 7800000, 78]
    ];
    const insert = database.prepare(`
      INSERT INTO projects (
        project_code, name, location, client, start_date, expected_end_date,
        budget, status, progress, manager, labor_cost, worker_count
      ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    `);
    projects.forEach((project) => {
      if (!database.prepare('SELECT 1 FROM projects WHERE project_code = ?').get(project[0])) {
        insert.run(...project);
      }
    });
  }
};
