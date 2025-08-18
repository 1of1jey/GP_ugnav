package util;

import java.time.LocalTime;
import java.util.Scanner;

public final class TimeUtil {
    public static LocalTime pickTime(Scanner sc) {
        System.out.print("Enter departure time HH:MM (24h), or blank for now: ");
        String s = sc.nextLine().trim();
        if (s.isEmpty()) return LocalTime.now();
        try {
            return LocalTime.parse(s);
        } catch (Exception e) {
            System.out.println("Invalid time. Using current time.");
            return LocalTime.now();
        }
    }
}


