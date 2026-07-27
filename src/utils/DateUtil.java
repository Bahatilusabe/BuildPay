package utils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class DateUtil {
    public static String format(LocalDate date) {
        return date.format(DateTimeFormatter.ISO_DATE);
    }
}
