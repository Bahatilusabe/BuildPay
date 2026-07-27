package models;

public class Payslip {
    private int id;
    private int payrollId;
    private String content; // for PDF or HTML content reference

    // getters and setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getPayrollId() { return payrollId; }
    public void setPayrollId(int payrollId) { this.payrollId = payrollId; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
