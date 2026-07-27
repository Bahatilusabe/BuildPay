package services;

import models.Attendance;
import repositories.AttendanceRepository;

import java.util.List;

public class AttendanceService {
    private final AttendanceRepository repo = new AttendanceRepository();

    public Attendance addAttendance(Attendance a) {
        if (a == null) return null;
        if (a.getEmployeeId() <= 0) return null;
        if (a.getAttendanceDate() == null || a.getAttendanceDate().isBlank()) return null;
        if (a.getStatus() == null || a.getStatus().isBlank()) return null;
        return repo.create(a);
    }

    public boolean editAttendance(Attendance a) {
        if (a == null || a.getId() <= 0) return false;
        return repo.update(a);
    }

    public boolean removeAttendance(int id) {
        if (id <= 0) return false;
        return repo.delete(id);
    }

    public Attendance getAttendance(int id) { return repo.findById(id); }

    public List<Attendance> listAll() { return repo.findAll(); }

    public List<Attendance> listByEmployee(int employeeId) { return repo.findByEmployee(employeeId); }

    public List<Attendance> listByDateRange(String from, String to) { return repo.findByDateRange(from, to); }

    // joined results
    public List<Attendance> listAllWithNames() { return repo.findAllWithEmployeeNames(); }
    public List<Attendance> listByEmployeeWithNames(int employeeId) { return repo.findByEmployee(employeeId); }
    public List<Attendance> listByDateRangeWithNames(String from, String to) { return repo.findByDateRange(from, to); }
}

