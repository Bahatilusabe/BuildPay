package com.buildpay.model;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;

public class EmployeeModel {
    private final SimpleStringProperty id = new SimpleStringProperty();
    private final SimpleStringProperty name = new SimpleStringProperty();
    private final SimpleStringProperty position = new SimpleStringProperty();
    private final SimpleStringProperty department = new SimpleStringProperty();
    private final SimpleStringProperty phone = new SimpleStringProperty();
    private final SimpleDoubleProperty salary = new SimpleDoubleProperty();
    private final SimpleStringProperty status = new SimpleStringProperty();
    private final SimpleStringProperty photo = new SimpleStringProperty();

    public EmployeeModel(String id, String name, String position, String department, String phone, double salary, String status, String photo) {
        this.id.set(id);
        this.name.set(name);
        this.position.set(position);
        this.department.set(department);
        this.phone.set(phone);
        this.salary.set(salary);
        this.status.set(status);
        this.photo.set(photo);
    }

    public String getId() { return id.get(); }
    public String getName() { return name.get(); }
    public String getPosition() { return position.get(); }
    public String getDepartment() { return department.get(); }
    public String getPhone() { return phone.get(); }
    public double getSalary() { return salary.get(); }
    public String getStatus() { return status.get(); }
    public String getPhoto() { return photo.get(); }

    // Property getters for TableView bindings
    public SimpleStringProperty idProperty() { return id; }
    public SimpleStringProperty nameProperty() { return name; }
    public SimpleStringProperty positionProperty() { return position; }
    public SimpleStringProperty departmentProperty() { return department; }
    public SimpleStringProperty phoneProperty() { return phone; }
    public SimpleDoubleProperty salaryProperty() { return salary; }
    public SimpleStringProperty statusProperty() { return status; }
    public SimpleStringProperty photoProperty() { return photo; }
}
