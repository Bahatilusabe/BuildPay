package com.payroll.services;

import com.payroll.models.Payroll;
import com.payroll.repositories.PayrollRepository;
import models.Attendance;
import models.Employee;
import repositories.EmployeeRepository;
import services.AttendanceService;

import java.util.ArrayList;
import java.util.List;

public class PayrollService {

    private final PayrollCalculator calculator = new PayrollCalculator();
    private final PayrollRepository payrollRepo = new PayrollRepository();
    private final AttendanceService attendanceService = new AttendanceService();
    private final EmployeeRepository employeeRepo = new EmployeeRepository();

    /**
     * Compute and persist payroll for an employee for the given month (format: YYYY-MM).
     * allowance and deduction are external inputs (company policies, benefits, manual adjustments).
     */
    public Payroll computeAndSave(int employeeId, String month, double allowance, double deduction) {
        Employee emp = employeeRepo.findById(employeeId);
        if (emp == null) return null;

        // Load attendance for the month. Simplification: fetch by date range from month-01 to month-31
        String from = month + "-01";
        String to = month + "-31";
        List<Attendance> rows = attendanceService.listByDateRangeWithNames(from, to);

        // Filter attendance rows for the employee
        List<Attendance> filtered = new ArrayList<>();
        int daysWorked = 0;
        double totalOvertime = 0.0;
        for (Attendance a : rows) {
            if (a.getEmployeeId() == employeeId) {
                filtered.add(a);
                if ("PRESENT".equalsIgnoreCase(a.getStatus())) daysWorked++;
                totalOvertime += a.getOvertimeHours();
            }
        }

        double gross = calculator.calculateGrossSalary(emp.getBasicSalary(), daysWorked);
        double overtimePay = calculator.calculateOvertimePay(emp.getBasicSalary(), totalOvertime);
        double net = calculator.calculateNetSalary(emp.getBasicSalary(), daysWorked, totalOvertime, allowance, deduction);

        Payroll p = new Payroll();
        p.setEmployeeId(employeeId);
        p.setMonth(month);
        p.setDaysWorked(daysWorked);
        p.setOvertimeHours(totalOvertime);
        p.setAllowance(allowance);
        p.setDeduction(deduction);
        p.setGrossSalary(gross + overtimePay);
        p.setNetSalary(net);

        payrollRepo.save(p);
        return p;
    }
}
