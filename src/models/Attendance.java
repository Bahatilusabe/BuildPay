package models;

public class Attendance {

    private int id;
    private int employeeId;
    private String attendanceDate; // ISO date string yyyy-MM-dd
    private String status; // PRESENT / ABSENT / LEAVE
    private double overtimeHours;
    // transient field populated by JOIN queries for display purposes
    private String employeeName;

    public Attendance() {}

    public Attendance(int id, int employeeId, String attendanceDate, String status, double overtimeHours) {
        this.id = id;
        this.employeeId = employeeId;
        this.attendanceDate = attendanceDate;
        this.status = status;
        this.overtimeHours = overtimeHours;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getEmployeeId() { return employeeId; }
    public void setEmployeeId(int employeeId) { this.employeeId = employeeId; }
    public String getAttendanceDate() { return attendanceDate; }
    public void setAttendanceDate(String attendanceDate) { this.attendanceDate = attendanceDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public double getOvertimeHours() { return overtimeHours; }
    public void setOvertimeHours(double overtimeHours) { this.overtimeHours = overtimeHours; }
    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }
}
