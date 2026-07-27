#!/usr/bin/env bash
set -euo pipefail

DB=database/payroll.db
BACKUP_DIR=database/backups
mkdir -p "$BACKUP_DIR"

timestamp=$(date +%Y%m%d%H%M%S)
backup="$BACKUP_DIR/payroll.db.$timestamp.bak"
cp "$DB" "$backup" || { echo "Failed to backup $DB"; exit 1; }
echo "Backup created: $backup"

echo "Checking/creating payroll table..."
sqlite3 "$DB" <<'SQL'
CREATE TABLE IF NOT EXISTS payroll (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    employee_id INTEGER,
    month TEXT,
    days_worked INTEGER,
    overtime_hours REAL,
    allowance REAL,
    deduction REAL,
    gross_salary REAL,
    net_salary REAL,
    created_at TEXT,
    FOREIGN KEY(employee_id) REFERENCES employees(id)
);
SQL

echo "Migration complete."
