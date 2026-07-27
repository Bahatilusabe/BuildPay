package tools;

public class SalaryCalcRunner {
    public static void main(String[] args) {
        double basic = 50000;
        double overtime = 2000;
        double allowance = 1500;
        double deductions = 500;

        double net = utils.SalaryCalculator.calculateSalary(basic, overtime, allowance, deductions);
        System.out.println("Calculated salary: " + net);
    }
}
