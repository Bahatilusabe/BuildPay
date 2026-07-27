package models;

public class Employee {

    private int id;
    private String employeeNumber;
    private String fullName;
    private String gender;
    private String phone;
    private String email;
    private String position;
    private String department;
    private double basicSalary;
    private String status;
    private String photoPath;

    public Employee() {}

    public Employee(int id, String employeeNumber, String fullName, String gender, String phone, String email,
                    String position, String department, double basicSalary, String status) {
        this.id = id;
        this.employeeNumber = employeeNumber;
        this.fullName = fullName;
        this.gender = gender;
        this.phone = phone;
        this.email = email;
        this.position = position;
        this.department = department;
        this.basicSalary = basicSalary;
        this.status = status;
    }

    // Getters and setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getEmployeeNumber() { return employeeNumber; }
    public void setEmployeeNumber(String employeeNumber) { this.employeeNumber = employeeNumber; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public double getBasicSalary() { return basicSalary; }
    public void setBasicSalary(double basicSalary) { this.basicSalary = basicSalary; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getPhotoPath() { return photoPath; }
    public void setPhotoPath(String photoPath) { this.photoPath = photoPath; }

    @Override
    public String toString() {
        return (fullName != null && !fullName.isBlank()) ? fullName : employeeNumber != null ? employeeNumber : "<employee>";
    }

}
