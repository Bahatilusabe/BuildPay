package services;

import models.Employee;
import repositories.EmployeeRepository;
import utils.ValidationUtil;

import java.util.List;
import utils.EventBus;

public class EmployeeService {
    private final EmployeeRepository repo = new EmployeeRepository();

    public Employee addEmployee(Employee e) {
        if (e == null) return null;
        if (e.getFullName() == null || e.getFullName().trim().isEmpty()) return null;
        if (e.getEmail() != null && !ValidationUtil.isEmail(e.getEmail())) return null;
        Employee created = repo.create(e);
        if (created != null) EventBus.publish("employees:changed");
        return created;
    }

    public boolean editEmployee(Employee e) {
        if (e == null || e.getId() <= 0) return false;
        if (e.getEmail() != null && !ValidationUtil.isEmail(e.getEmail())) return false;
        boolean ok = repo.update(e);
        if (ok) EventBus.publish("employees:changed");
        return ok;
    }

    public boolean deleteEmployee(int id) {
        if (id <= 0) return false;
        boolean ok = repo.delete(id);
        if (ok) EventBus.publish("employees:changed");
        return ok;
    }

    public Employee getEmployee(int id) {
        if (id <= 0) return null;
        return repo.findById(id);
    }

    public List<Employee> searchEmployees(String q) {
        if (q == null || q.trim().isEmpty()) return List.of();
        return repo.searchByName(q.trim());
    }
}
