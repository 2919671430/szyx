package com.szyx.ai.util;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.File;

public class FileUtils {

    public static File getLlmModelFile(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
        String path = prefs.getString("llm_model_path", "");
        if (!path.isEmpty()) {
            return new File(path);
        }
        // Fallback default
        return new File(context.getFilesDir(), "models/gemma-4-e4b-it-Q4_K_M.gguf");
    }

    public static String getLlmModelPath(Context context) {
        return getLlmModelFile(context).getAbsolutePath();
    }

    public static boolean isLlmModelExists(Context context) {
        File f = getLlmModelFile(context);
        return f.exists() && f.length() > 100 * 1024 * 1024;
    }

    public static boolean isAllModelsReady(Context context) {
        return isLlmModelExists(context);
    }

    public static File getSessionDir(Context context, long sessionId) {
        File dir = new File(context.getFilesDir(), "sessions/" + sessionId);
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    public static long getDirSize(File dir) {
        long size = 0;
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.isFile()) {
                        size += f.length();
                    } else {
                        size += getDirSize(f);
                    }
                }
            }
        }
        return size;
    }

    public static String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }
}
