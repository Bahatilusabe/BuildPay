'use strict';

class PayrollDao {
  constructor(database) {
    this.database = database;
  }

  resolveEmployee(employeeId) {
    return this.database.prepare(
      'SELECT id, employee_code AS employeeCode, first_name AS firstName, last_name AS lastName, position, department, basic_salary AS basicSalary FROM employees WHERE id = ? OR employee_code = ?'
    ).get(Number(employeeId) || 0, String(employeeId));
  }

  list(period) {
    const rows = this.database.prepare(`
      SELECT p.*, e.employee_code AS employeeCode,
             e.first_name || ' ' || e.last_name AS employeeName,
             e.position, e.department
      FROM payroll p
      JOIN employees e ON e.id = p.employee_id
      WHERE (? IS NULL OR p.pay_period = ?)
      ORDER BY p.updated_at DESC, p.id DESC
    `).all(period || null, period || null);
    return rows.map(row => this.withLegacyFields(row));
  }

  trend(limit = 12) {
    return this.database.prepare(`
      SELECT pay_period AS payPeriod,
             COALESCE(SUM(gross_salary), 0) AS grossPay,
             COALESCE(SUM(total_deductions), 0) AS deductions,
             COALESCE(SUM(net_salary), 0) AS netPay,
             COUNT(*) AS employeeCount
      FROM payroll
      GROUP BY pay_period
      ORDER BY pay_period DESC
      LIMIT ?
    `).all(Math.max(1, Math.min(24, Number(limit) || 12))).reverse();
  }

  get(id) {
    const row = this.database.prepare(`
      SELECT p.*, e.employee_code AS employeeCode,
             e.first_name || ' ' || e.last_name AS employeeName,
             e.position, e.department
      FROM payroll p
      JOIN employees e ON e.id = p.employee_id
      WHERE p.id = ?
    `).get(Number(id));
    return row ? this.withLegacyFields(row) : null;
  }

  report(period) {
    const rows = this.database.prepare(`
      SELECT p.id, p.pay_period AS payPeriod, p.net_salary AS netSalary,
             p.status, p.processed_at AS processedAt, p.updated_at AS updatedAt,
             e.employee_code AS employeeCode,
             e.first_name || ' ' || e.last_name AS employeeName,
             e.position, e.department,
             latestPayslip.payslip_number AS payslipNumber,
             latestPayslip.generated_at AS payslipReleasedAt,
             COALESCE(SUM(a.regular_hours), 0) AS regularHours,
             COALESCE(SUM(a.overtime_hours), 0) AS overtimeHours
      FROM payroll p
      JOIN employees e ON e.id = p.employee_id
      LEFT JOIN payslips latestPayslip ON latestPayslip.id = (
        SELECT MAX(ps.id) FROM payslips ps WHERE ps.payroll_id = p.id
      )
      LEFT JOIN attendance a ON a.employee_id = p.employee_id
        AND substr(a.attendance_date, 1, 7) = p.pay_period
      WHERE (? IS NULL OR p.pay_period = ?)
      GROUP BY p.id, latestPayslip.id
      ORDER BY p.pay_period DESC, p.updated_at DESC, p.id DESC
    `).all(period || null, period || null);

    return rows.map(row => ({
      ...row,
      paymentStatus: ['PAID', 'COMPLETED'].includes(String(row.status).toUpperCase()) ? 'PAID' : 'PENDING',
      releaseDate: row.payslipReleasedAt || row.processedAt || row.updatedAt
    }));
  }

  generatePayslip(payrollId) {
    const payroll = this.get(payrollId);
    if (!payroll) throw new Error('Payroll record was not found');

    const payslipNumber = `PS-${payroll.pay_period.replace(/[^0-9A-Z]/gi, '')}-${payroll.employeeCode}`;
    const existing = this.database.prepare(
      'SELECT id FROM payslips WHERE payroll_id = ? ORDER BY id DESC LIMIT 1'
    ).get(payroll.id);
    if (existing) {
      this.database.prepare(`
        UPDATE payslips
        SET payslip_number = ?, generated_at = CURRENT_TIMESTAMP, status = 'GENERATED'
        WHERE id = ?
      `).run(payslipNumber, existing.id);
    } else {
      this.database.prepare(`
        INSERT INTO payslips (payroll_id, employee_id, payslip_number, pay_period, generated_at, status)
        VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, 'GENERATED')
      `).run(payroll.id, payroll.employee_id, payslipNumber, payroll.pay_period);
    }

    return this.get(payroll.id);
  }

  generatePayslips(period) {
    const payrollIds = this.database.prepare(`
      SELECT id FROM payroll
      WHERE (? IS NULL OR pay_period = ?)
      ORDER BY id
    `).all(period || null, period || null).map(row => row.id);
    const generateAll = this.database.transaction(() => {
      payrollIds.forEach(payrollId => this.generatePayslip(payrollId));
    });
    generateAll();
    return payrollIds.map(payrollId => this.get(payrollId));
  }

  upsert(record) {
    const employee = this.resolveEmployee(record.employeeId);
    if (!employee) throw new Error('Employee record was not found');

    const values = {
      employeeId: employee.id,
      payPeriod: String(record.payPeriod || '').trim(),
      basicSalary: Number(record.basicSalary) || 0,
      overtimeAmount: Number(record.overtimePay ?? record.overtimeAmount) || 0,
      allowancesAmount: Number(record.allowances ?? record.allowancesAmount) || 0,
      grossSalary: Number(record.totalEarnings ?? record.grossSalary) || 0,
      paye: Number(record.paye) || 0,
      nssf: Number(record.nssf) || 0,
      shif: Number(record.shif) || 0,
      otherDeductions: Number(record.otherDeductions) || 0,
      totalDeductions: Number(record.deductions ?? record.totalDeductions) || 0,
      netSalary: Number(record.netSalary) || 0,
      status: String(record.status || 'DRAFT').toUpperCase()
    };
    if (!values.payPeriod) throw new Error('Pay period is required');

    const transaction = this.database.transaction(() => {
      this.database.prepare(`
        INSERT INTO payroll (
          employee_id, pay_period, basic_salary, overtime_amount,
          allowances_amount, gross_salary, paye, nssf, shif,
          other_deductions, total_deductions, net_salary, status, updated_at
        ) VALUES (
          @employeeId, @payPeriod, @basicSalary, @overtimeAmount,
          @allowancesAmount, @grossSalary, @paye, @nssf, @shif,
          @otherDeductions, @totalDeductions, @netSalary, @status, CURRENT_TIMESTAMP
        )
        ON CONFLICT(employee_id, pay_period) DO UPDATE SET
          basic_salary = excluded.basic_salary,
          overtime_amount = excluded.overtime_amount,
          allowances_amount = excluded.allowances_amount,
          gross_salary = excluded.gross_salary,
          paye = excluded.paye,
          nssf = excluded.nssf,
          shif = excluded.shif,
          other_deductions = excluded.other_deductions,
          total_deductions = excluded.total_deductions,
          net_salary = excluded.net_salary,
          status = excluded.status,
          updated_at = CURRENT_TIMESTAMP
      `).run(values);

      this.database.prepare(
        'DELETE FROM payroll_records WHERE employee_id = ? AND pay_period = ?'
      ).run(values.employeeId, values.payPeriod);
      this.database.prepare(`
        INSERT INTO payroll_records (
          employee_id, pay_period, basic_salary, overtime_pay,
          allowances, deductions, net_salary, status
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
      `).run(
        values.employeeId,
        values.payPeriod,
        values.basicSalary,
        values.overtimeAmount,
        values.allowancesAmount,
        values.totalDeductions,
        values.netSalary,
        values.status === 'DRAFT' ? 'Draft' : values.status
      );

      this.database.prepare(`
        INSERT INTO activity_logs (action, module, description)
        VALUES ('UPSERT', 'Payroll', ?)
      `).run(`Payroll saved for ${employee.employeeCode} - ${values.payPeriod}`);
    });
    transaction();

    const saved = this.database.prepare(
      'SELECT id FROM payroll WHERE employee_id = ? AND pay_period = ?'
    ).get(values.employeeId, values.payPeriod);
    return this.get(saved.id);
  }

  withLegacyFields(row) {
    return {
      ...row,
      overtimePay: row.overtime_amount,
      allowances: row.allowances_amount,
      deductions: row.total_deductions,
      totalEarnings: row.gross_salary,
      overtimeAmount: row.overtime_amount
    };
  }
}

module.exports = { PayrollDao };
