package com.payroll.services;

public class PayrollCalculator {

    private static final double OVERTIME_RATE = 1.5;

    public double calculateDailyRate(double basicSalary) {
        return basicSalary / 30.0;
    }

    public double calculateGrossSalary(double basicSalary, int daysWorked) {
        return (basicSalary / 30.0) * daysWorked;
    }

    public double calculateOvertimePay(double basicSalary, double overtimeHours) {
        double hourlyRate = (basicSalary / 30.0) / 8.0;
        return overtimeHours * hourlyRate * OVERTIME_RATE;
    }

    public double calculateNetSalary(
            double basicSalary,
            int daysWorked,
            double overtimeHours,
            double allowance,
            double deduction
    ) {

        double gross = calculateGrossSalary(basicSalary, daysWorked);
        double overtime = calculateOvertimePay(basicSalary, overtimeHours);

        return gross + overtime + allowance - deduction;
    }
}
