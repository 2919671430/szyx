package com.szyx.ai.util;

import java.util.ArrayList;
import java.util.List;

public class AppLog {

    private static final int MAX_LINES = 500;
    private static final String[] buffer = new String[MAX_LINES];
    private static int head = 0;
    private static int count = 0;

    public static synchronized void d(String tag, String msg) {
        add("D/" + tag + ": " + msg);
    }

    public static synchronized void i(String tag, String msg) {
        add("I/" + tag + ": " + msg);
    }

    public static synchronized void w(String tag, String msg) {
        add("W/" + tag + ": " + msg);
    }

    public static synchronized void e(String tag, String msg) {
        add("E/" + tag + ": " + msg);
    }

    public static synchronized void e(String tag, String msg, Throwable t) {
        add("E/" + tag + ": " + msg + "\n" + android.util.Log.getStackTraceString(t));
    }

    private static void add(String line) {
        String time = new java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.US)
                .format(new java.util.Date());
        int index = (head + count) % MAX_LINES;
        if (count == MAX_LINES) {
            head = (head + 1) % MAX_LINES;
        } else {
            count++;
        }
        buffer[index] = time + " " + line;
    }

    public static synchronized String getAll() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(buffer[(head + i) % MAX_LINES]).append("\n");
        }
        return sb.toString();
    }

    public static synchronized List<String> getLines() {
        List<String> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            list.add(buffer[(head + i) % MAX_LINES]);
        }
        return list;
    }

    public static synchronized void clear() {
        head = 0;
        count = 0;
    }
}
