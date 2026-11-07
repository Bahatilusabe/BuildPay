'use strict';

class AttendanceDao {
  constructor(database) {
    this.database = database;
  }

  periodBounds(period) {
    const weeklyMatch = String(period || '').match(/^(\d{4})-W(\d{2})$/);
    if (weeklyMatch) {
      const year = Number(weeklyMatch[1]);
      const week = Number(weeklyMatch[2]);
      const januaryFourth = new Date(Date.UTC(year, 0, 4));
      const monday = new Date(januaryFourth);
      monday.setUTCDate(januaryFourth.getUTCDate() - (januaryFourth.getUTCDay() || 7) + 1 + ((week - 1) * 7));
      const sunday = new Date(monday);
      sunday.setUTCDate(monday.getUTCDate() + 6);
      return {
        start: monday.toISOString().slice(0, 10),
        end: sunday.toISOString().slice(0, 10)
      };
    }
    return {
      start: `${period}-01`,
      end: `${period}-31`
    };
  }

  listByMonth(month) {
    return this.database.prepare(`
      SELECT a.id,
             e.id AS employeeId,
             e.employee_code AS employeeCode,
             e.first_name || ' ' || e.last_name AS employeeName,
             e.department,
             a.attendance_date AS attendanceDate,
             a.status,
             a.check_in AS checkIn,
             a.check_out AS checkOut,
             a.regular_hours AS regularHours,
             a.overtime_hours AS overtimeHours,
             a.notes
      FROM employees e
      LEFT JOIN attendance a ON a.employee_id = e.id
        AND substr(a.attendance_date, 1, 7) = ?
      ORDER BY e.last_name, e.first_name, a.attendance_date
    `).all(month);
  }

  record(record) {
    const result = this.database.prepare(`
      INSERT INTO attendance (
        employee_id, project_id, attendance_date, status,
        check_in, check_out, regular_hours, overtime_hours, notes, updated_at
      ) VALUES (
        @employeeId, @projectId, @attendanceDate, @status,
        @checkIn, @checkOut, @regularHours, @overtimeHours, @notes, CURRENT_TIMESTAMP
      )
    `).run({
      employeeId: Number(record.employeeId),
      projectId: record.projectId ? Number(record.projectId) : null,
      attendanceDate: record.attendanceDate,
      status: record.status,
      checkIn: record.checkIn || null,
      checkOut: record.checkOut || null,
      regularHours: Number(record.regularHours) || 0,
      overtimeHours: Number(record.overtimeHours) || 0,
      notes: record.notes || null
    });
    return result.changes > 0;
  }

  upsert(record) {
    return this.record(record);
  }

  summary(period, employeeId = null) {
    const whereEmployee = employeeId ? 'AND employee_id = @employeeId' : '';
    const bounds = this.periodBounds(period);
    return this.database.prepare(`
      SELECT
        COUNT(*) AS totalDays,
        SUM(CASE WHEN status = 'PRESENT' THEN 1 ELSE 0 END) AS presentDays,
        SUM(CASE WHEN status = 'ABSENT' THEN 1 ELSE 0 END) AS absentDays,
        SUM(CASE WHEN status = 'LEAVE' THEN 1 ELSE 0 END) AS leaveDays,
        SUM(CASE WHEN status = 'LATE' THEN 1 ELSE 0 END) AS lateDays,
        COALESCE(SUM(overtime_hours), 0) AS overtimeHours
      FROM attendance
      WHERE attendance_date BETWEEN @start AND @end ${whereEmployee}
    `).get({ ...bounds, ...(employeeId ? { employeeId: Number(employeeId) } : {}) });
  }
}

module.exports = { AttendanceDao };
