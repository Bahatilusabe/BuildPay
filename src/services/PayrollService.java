package services;

import models.Attendance;
import models.Employee;
import models.Payroll;
import models.PayrollSummary;
import repositories.PayrollRepository;

import java.util.List;

/**
 * PayrollService computes payroll for a given employee for a period using attendance records.
 * Rules used (configurable via constants):
 * - Daily rate = basicSalary / DAYS_IN_MONTH
 * - Overtime pay = overtimeHours * (dailyRate / STANDARD_HOURS_PER_DAY) * OVERTIME_MULTIPLIER
 * - Each ABSENT day deducts dailyRate
 */
public class PayrollService {

    // configurable constants (could later be injected)
    public static final int DAYS_IN_MONTH = 30;
    public static final double STANDARD_HOURS_PER_DAY = 8.0;
    public static final double OVERTIME_MULTIPLIER = 1.5; // 50% extra per overtime hour

    private final AttendanceService attendanceService = new AttendanceService();
    private final PayrollRepository payrollRepository = new PayrollRepository();

    public PayrollSummary computeForEmployee(Employee employee, List<Attendance> attendanceList) {
        PayrollSummary sum = new PayrollSummary();
        if (employee == null) return sum;

        double basic = employee.getBasicSalary();
        double dailyRate = basic / DAYS_IN_MONTH;

        int present = 0;
        int absent = 0;
        int leave = 0;
        double overtimeHours = 0.0;

        if (attendanceList != null) {
            for (Attendance a : attendanceList) {
                if (a == null) continue;
                String status = (a.getStatus() == null) ? "" : a.getStatus().toUpperCase();
                switch (status) {
                    case "PRESENT":
                        present++; break;
                    case "ABSENT":
                        absent++; break;
                    case "LEAVE":
                        leave++; break;
                    default:
                        // treat unknown as absent
                        absent++; break;
                }
                overtimeHours += a.getOvertimeHours();
            }
        }

        double overtimePay = overtimeHours * (dailyRate / STANDARD_HOURS_PER_DAY) * OVERTIME_MULTIPLIER;
        double absentDeduction = absent * dailyRate;

        double grossFromDays = present * dailyRate; // salary earned from present days
        double gross = grossFromDays + overtimePay; // allowances/deductions not considered here

        // net pay: we don't apply taxes here; reuse existing utils.SalaryCalculator if desired
        double net = gross - absentDeduction;

        sum.setBasicSalary(basic);
        sum.setDailyRate(dailyRate);
        sum.setDaysPresent(present);
        sum.setDaysAbsent(absent);
        sum.setDaysLeave(leave);
        sum.setTotalOvertimeHours(overtimeHours);
        sum.setOvertimePay(overtimePay);
        sum.setAbsentDeduction(absentDeduction);
        sum.setGrossPay(gross);
        sum.setNetPay(net);

        return sum;
    }

    public Payroll computeAndSave(Employee employee, String month, double allowance, double deduction) {
        if (employee == null || month == null || month.isBlank()) return null;
        List<Attendance> attendance = attendanceService.listByDateRange(month + "-01", month + "-31");
        PayrollSummary summary = computeForEmployee(employee, attendanceForEmployee(attendance, employee.getId()));

        double gross = summary.getGrossPay() + allowance;
        double net = gross - deduction;

        Payroll payroll = new Payroll();
        payroll.setEmployeeId(employee.getId());
        payroll.setMonth(month);
        payroll.setDaysWorked(summary.getDaysPresent());
        payroll.setOvertimeHours(summary.getTotalOvertimeHours());
        payroll.setAllowance(allowance);
        payroll.setDeduction(deduction);
        payroll.setGrossSalary(gross);
        payroll.setNetSalary(net);
        payroll.setPeriodStart(month + "-01");
        payroll.setPeriodEnd(month + "-31");
        return payrollRepository.save(payroll);
    }

    public PayrollSummary computeForEmployeeAndMonth(Employee employee, String month) {
        if (employee == null || month == null || month.isBlank()) return new PayrollSummary();
        List<Attendance> attendance = attendanceService.listByDateRange(month + "-01", month + "-31");
        return computeForEmployee(employee, attendanceForEmployee(attendance, employee.getId()));
    }

    private List<Attendance> attendanceForEmployee(List<Attendance> attendance, int employeeId) {
        if (attendance == null || attendance.isEmpty()) return List.of();
        return attendance.stream()
                .filter(row -> row != null && row.getEmployeeId() == employeeId)
                .toList();
    }

}
