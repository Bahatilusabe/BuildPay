package utils;

public class SalaryCalculator {
    public static double calculateNet(double gross) {
        // placeholder: simple tax deduction
        double tax = gross * 0.2;
        return gross - tax;
    }

    public static double calculateSalary(
            double basicSalary,
            double overtime,
            double allowance,
            double deductions) {

        return basicSalary
                + overtime
                + allowance
                - deductions;
    }
}
