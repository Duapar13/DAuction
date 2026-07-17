package com.duapar.dauction.util;

public final class TimeFormat {

    private TimeFormat() {
    }

    public static String remaining(long millis) {
        if (millis <= 0) {
            return "expiré";
        }
        long totalMinutes = millis / 60_000;
        long days = totalMinutes / (60 * 24);
        long hours = (totalMinutes / 60) % 24;
        long minutes = totalMinutes % 60;

        if (days > 0) {
            return days + "j " + hours + "h";
        }
        if (hours > 0) {
            return hours + "h " + minutes + "m";
        }
        return minutes + "m";
    }
}
