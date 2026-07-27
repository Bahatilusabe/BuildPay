#!/usr/bin/env bash
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
DB_PATH="$ROOT_DIR/database/payroll.db"
BACKUP_DIR="$ROOT_DIR/database/backups"
mkdir -p "$BACKUP_DIR"
TIMESTAMP=$(date +%Y%m%d%H%M%S)
if [ ! -f "$DB_PATH" ]; then
  echo "DB not found at $DB_PATH"
  exit 1
fi
BACKUP="$BACKUP_DIR/payroll.db.$TIMESTAMP.att.bak"
cp "$DB_PATH" "$BACKUP"
echo "Backup created: $BACKUP"
# check if table exists
TABLE_EXISTS=$(sqlite3 "$DB_PATH" "SELECT name FROM sqlite_master WHERE type='table' AND name='attendance';" )
if [ -n "$TABLE_EXISTS" ]; then
  echo "attendance table already exists."
  exit 0
fi
# create table
sqlite3 "$DB_PATH" <<'SQL'
CREATE TABLE attendance (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    employee_id INTEGER,
    attendance_date TEXT,
    status TEXT,
    overtime_hours REAL DEFAULT 0,
    FOREIGN KEY(employee_id) REFERENCES employees(id)
);
SQL

echo "attendance table created successfully."
exit 0
