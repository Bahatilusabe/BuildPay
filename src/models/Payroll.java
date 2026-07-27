package models;

public class Payroll {
    private int id;
    private int employeeId;
    private String month;
    private int daysWorked;
    private double overtimeHours;
    private double allowance;
    private double deduction;
    private double grossSalary;
    private double netSalary;
    private String periodStart;
    private String periodEnd;
    private double gross;
    private double net;
    private String generatedAt;

    // getters and setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getEmployeeId() { return employeeId; }
    public void setEmployeeId(int employeeId) { this.employeeId = employeeId; }
    public String getMonth() { return month; }
    public void setMonth(String month) { this.month = month; }
    public int getDaysWorked() { return daysWorked; }
    public void setDaysWorked(int daysWorked) { this.daysWorked = daysWorked; }
    public double getOvertimeHours() { return overtimeHours; }
    public void setOvertimeHours(double overtimeHours) { this.overtimeHours = overtimeHours; }
    public double getAllowance() { return allowance; }
    public void setAllowance(double allowance) { this.allowance = allowance; }
    public double getDeduction() { return deduction; }
    public void setDeduction(double deduction) { this.deduction = deduction; }
    public double getGrossSalary() { return grossSalary; }
    public void setGrossSalary(double grossSalary) {
        this.grossSalary = grossSalary;
        this.gross = grossSalary;
    }
    public double getNetSalary() { return netSalary; }
    public void setNetSalary(double netSalary) {
        this.netSalary = netSalary;
        this.net = netSalary;
    }
    public String getPeriodStart() { return periodStart; }
    public void setPeriodStart(String periodStart) { this.periodStart = periodStart; }
    public String getPeriodEnd() { return periodEnd; }
    public void setPeriodEnd(String periodEnd) { this.periodEnd = periodEnd; }
    public double getGross() { return gross; }
    public void setGross(double gross) {
        this.gross = gross;
        this.grossSalary = gross;
    }
    public double getNet() { return net; }
    public void setNet(double net) {
        this.net = net;
        this.netSalary = net;
    }
    public String getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(String generatedAt) { this.generatedAt = generatedAt; }
}
