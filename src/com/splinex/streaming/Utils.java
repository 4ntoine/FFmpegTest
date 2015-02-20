package com.splinex.streaming;

public class Utils {
    public static Integer tryParse(String text) {
        try {
            return Integer.valueOf(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static int tryParse(String text, int defaultValue) {
        Integer res = tryParse(text);
        return res == null ? defaultValue : res;
    }
}
