package com.szyx.ai.util;

import android.app.ActivityManager;
import android.content.Context;

public class MemoryUtils {

    public static long getAvailableMemoryMb(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(memInfo);
        return memInfo.availMem / (1024 * 1024);
    }

    public static long getTotalMemoryMb(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(memInfo);
        return memInfo.totalMem / (1024 * 1024);
    }

    public static boolean isLowMemory(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(memInfo);
        return memInfo.lowMemory;
    }

    public static long getUsedHeapMb() {
        Runtime rt = Runtime.getRuntime();
        return (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);
    }

    public static long getMaxHeapMb() {
        return Runtime.getRuntime().maxMemory() / (1024 * 1024);
    }
}
