package tools;

import models.Attendance;
import models.Employee;
import models.PayrollSummary;
import services.PayrollService;

import java.util.ArrayList;
import java.util.List;

public class PayrollDemo {
    public static void main(String[] args) {
        Employee emp = new Employee();
        emp.setId(1);
        emp.setFullName("Jane Doe");
        emp.setBasicSalary(50000);

        // Example: 20 days present, overtime total worth 5000 (we'll translate 5000 into hours)
        List<Attendance> atts = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            Attendance a = new Attendance();
            a.setStatus("PRESENT");
            a.setOvertimeHours(0);
            atts.add(a);
        }
        // compute what overtime hours produce roughly 5000 using payroll formula
        PayrollService svc = new PayrollService();
        double dailyRate = emp.getBasicSalary() / PayrollService.DAYS_IN_MONTH; // 1666.66
        // Solve overtimeHours * (dailyRate/8)*1.5 = 5000 => overtimeHours = 5000 / ((dailyRate/8)*1.5)
        double targetOvertimePay = 5000.0;
        double perHourPay = (dailyRate / PayrollService.STANDARD_HOURS_PER_DAY) * PayrollService.OVERTIME_MULTIPLIER;
        double neededOvertimeHours = targetOvertimePay / perHourPay;

        // attach overtime hours to one of the existing present days so we still have 20 present days
        if (!atts.isEmpty()) {
            atts.get(atts.size() - 1).setOvertimeHours(neededOvertimeHours);
        }

        PayrollSummary summary = svc.computeForEmployee(emp, atts);

        System.out.println("Example payroll computation:");
        System.out.println("Basic Salary = " + summary.getBasicSalary());
        System.out.println("Daily Rate = " + Math.round(summary.getDailyRate()));
        System.out.println("Days Present = " + summary.getDaysPresent());
        System.out.println("Overtime Pay = " + Math.round(summary.getOvertimePay()));
        System.out.println("Absent Deductions = " + Math.round(summary.getAbsentDeduction()));
        System.out.println("Final Salary (gross - absent) = " + Math.round(summary.getNetPay()));
    }
}
