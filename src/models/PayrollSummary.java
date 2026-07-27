package models;

public class PayrollSummary {
    private double basicSalary;
    private double dailyRate;
    private int daysPresent;
    private int daysAbsent;
    private int daysLeave;
    private double totalOvertimeHours;
    private double overtimePay;
    private double absentDeduction;
    private double grossPay;
    private double netPay;

    public PayrollSummary() {}

    // getters & setters
    public double getBasicSalary() { return basicSalary; }
    public void setBasicSalary(double basicSalary) { this.basicSalary = basicSalary; }
    public double getDailyRate() { return dailyRate; }
    public void setDailyRate(double dailyRate) { this.dailyRate = dailyRate; }
    public int getDaysPresent() { return daysPresent; }
    public void setDaysPresent(int daysPresent) { this.daysPresent = daysPresent; }
    public int getDaysAbsent() { return daysAbsent; }
    public void setDaysAbsent(int daysAbsent) { this.daysAbsent = daysAbsent; }
    public int getDaysLeave() { return daysLeave; }
    public void setDaysLeave(int daysLeave) { this.daysLeave = daysLeave; }
    public double getTotalOvertimeHours() { return totalOvertimeHours; }
    public void setTotalOvertimeHours(double totalOvertimeHours) { this.totalOvertimeHours = totalOvertimeHours; }
    public double getOvertimePay() { return overtimePay; }
    public void setOvertimePay(double overtimePay) { this.overtimePay = overtimePay; }
    public double getAbsentDeduction() { return absentDeduction; }
    public void setAbsentDeduction(double absentDeduction) { this.absentDeduction = absentDeduction; }
    public double getGrossPay() { return grossPay; }
    public void setGrossPay(double grossPay) { this.grossPay = grossPay; }
    public double getNetPay() { return netPay; }
    public void setNetPay(double netPay) { this.netPay = netPay; }

    @Override
    public String toString() {
        return "PayrollSummary{" +
                "basicSalary=" + basicSalary +
                ", dailyRate=" + dailyRate +
                ", daysPresent=" + daysPresent +
                ", daysAbsent=" + daysAbsent +
                ", daysLeave=" + daysLeave +
                ", totalOvertimeHours=" + totalOvertimeHours +
                ", overtimePay=" + overtimePay +
                ", absentDeduction=" + absentDeduction +
                ", grossPay=" + grossPay +
                ", netPay=" + netPay +
                '}';
    }
}
