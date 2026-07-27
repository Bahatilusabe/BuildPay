package utils;

import models.Employee;

import java.util.List;

public class ExportUtil {
    public static void exportEmployeesToPdf(List<Employee> employees) {
        // TODO: integrate with a PDF library (iText, Apache PDFBox). Stub for now.
        System.out.println("Exporting " + employees.size() + " employees to PDF (stub)");
    }

    public static void exportEmployeesToExcel(List<Employee> employees) {
        // TODO: integrate with Apache POI. Stub for now.
        System.out.println("Exporting " + employees.size() + " employees to Excel (stub)");
    }
}
