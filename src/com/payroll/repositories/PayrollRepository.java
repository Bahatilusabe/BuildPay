package com.payroll.repositories;

import com.payroll.models.Payroll;
import config.DatabaseConfig;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PayrollRepository {

    public void save(Payroll p) {

        String sql = """
            INSERT INTO payroll(
                employee_id,
                month,
                days_worked,
                overtime_hours,
                allowance,
                deduction,
                gross_salary,
                net_salary,
                created_at
            )
            VALUES(?,?,?,?,?,?,?,?,datetime('now'))
        """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, p.getEmployeeId());
            ps.setString(2, p.getMonth());
            ps.setInt(3, p.getDaysWorked());
            ps.setDouble(4, p.getOvertimeHours());
            ps.setDouble(5, p.getAllowance());
            ps.setDouble(6, p.getDeduction());
            ps.setDouble(7, p.getGrossSalary());
            ps.setDouble(8, p.getNetSalary());

            ps.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Payroll findByEmployeeAndMonth(int employeeId, String month) {
        String sql = "SELECT * FROM payroll WHERE employee_id = ? AND month = ? ORDER BY created_at DESC LIMIT 1";
        try (Connection conn = DatabaseConfig.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, employeeId);
            ps.setString(2, month);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<Payroll> listByMonth(String month) {
        List<Payroll> out = new ArrayList<>();
        String sql = "SELECT * FROM payroll WHERE month = ? ORDER BY created_at DESC";
        try (Connection conn = DatabaseConfig.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, month);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(mapRow(rs));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return out;
    }

    private Payroll mapRow(ResultSet rs) throws SQLException {
        Payroll p = new Payroll();
        p.setId(rs.getInt("id"));
        p.setEmployeeId(rs.getInt("employee_id"));
        try { p.setMonth(rs.getString("month")); } catch (SQLException ignore) { p.setMonth(rs.getString("payroll_month")); }
        p.setDaysWorked(rs.getInt("days_worked"));
        p.setOvertimeHours(rs.getDouble("overtime_hours"));
        p.setAllowance(rs.getDouble("allowance"));
        p.setDeduction(rs.getDouble("deduction"));
        p.setGrossSalary(rs.getDouble("gross_salary"));
        p.setNetSalary(rs.getDouble("net_salary"));
        return p;
    }
}
