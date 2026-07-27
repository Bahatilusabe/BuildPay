#!/usr/bin/env bash
# Safe migration: backup DB, then add photo_path column if missing
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
BACKUP="$BACKUP_DIR/payroll.db.$TIMESTAMP.bak"
cp "$DB_PATH" "$BACKUP"
echo "Backup created: $BACKUP"
# check if column exists
COL_EXISTS=$(sqlite3 "$DB_PATH" "PRAGMA table_info(employees);" | awk -F"|" '{print $2}' | grep -x "photo_path" || true)
if [ -n "$COL_EXISTS" ]; then
  echo "Column photo_path already exists. No migration needed."
  exit 0
fi
# perform migration: add column
echo "Altering employees table to add photo_path column..."
sqlite3 "$DB_PATH" "ALTER TABLE employees ADD COLUMN photo_path TEXT;"
echo "Migration applied successfully."
exit 0
