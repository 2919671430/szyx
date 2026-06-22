package com.szyx.ai.engine.numerical;

import android.util.Log;

import com.szyx.ai.data.db.entity.NumericalValueEntity;
import com.szyx.ai.data.repository.ChatRepository;
import com.szyx.ai.util.AppLog;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Manages numerical values (affection, stats, resources) for character sessions.
 * Parses AI output for numerical changes and updates the database.
 *
 * Expected AI output format: [数值名+/-N] e.g. [好感+5] [修为-2] [金币+100]
 */
public class NumericalManager {

    private static final String TAG = "NumericalManager";

    // Pattern: [数值名+/-N] or 【数值名+/-N】
    private static final Pattern NUM_PATTERN =
            Pattern.compile("[\\[【]([^\\]+】]+?)([+\\-])(\\d+)[\\]】]");

    /**
     * Parse AI response text for numerical changes and update the database.
     * Returns the list of changes detected.
     */
    public static List<NumChange> parseAndUpdate(long sessionId, String aiResponse,
                                                   ChatRepository repo) {
        List<NumChange> changes = new ArrayList<>();
        Matcher matcher = NUM_PATTERN.matcher(aiResponse);

        while (matcher.find()) {
            String name = matcher.group(1).trim();
            String op = matcher.group(2);
            int delta = Integer.parseInt(matcher.group(3));
            if ("-".equals(op)) delta = -delta;

            // Get or create the numerical value
            NumericalValueEntity existing = repo.getNumericalValueByName(sessionId, name);
            if (existing == null) {
                // Auto-create with default values
                existing = new NumericalValueEntity();
                existing.sessionId = sessionId;
                existing.name = name;
                existing.currentValue = 50; // default starting value
                existing.maxValue = 100;
                existing.minValue = 0;
            }

            int oldValue = existing.currentValue;
            existing.currentValue += delta;

            // Clamp to min/max
            if (existing.maxValue > 0) {
                existing.currentValue = Math.min(existing.currentValue, existing.maxValue);
            }
            existing.currentValue = Math.max(existing.currentValue, existing.minValue);
            existing.lastUpdatedAt = System.currentTimeMillis();

            repo.insertNumericalValue(existing);

            changes.add(new NumChange(name, oldValue, existing.currentValue, delta));
            Log.i(TAG, "数值变化: " + name + " " + oldValue + " -> " + existing.currentValue +
                    " (" + (delta >= 0 ? "+" : "") + delta + ")");
        }

        if (!changes.isEmpty()) {
            AppLog.i(TAG, "检测到 " + changes.size() + " 项数值变化");
        }

        return changes;
    }

    /**
     * Strip numerical markers from AI response text (for display).
     */
    public static String stripNumericalMarkers(String text) {
        return text.replaceAll("[\\[【][^\\]+】]+?[+\\-]\\d+[\\]】]", "").trim();
    }

    /**
     * Get all numerical values formatted for prompt injection.
     */
    public static String formatForPrompt(long sessionId, ChatRepository repo) {
        List<NumericalValueEntity> values = repo.getNumericalValues(sessionId);
        if (values.isEmpty()) return "";

        StringBuilder sb = new StringBuilder("当前数值状态：\n");
        for (NumericalValueEntity v : values) {
            sb.append("- ").append(v.name).append(": ").append(v.currentValue);
            if (v.maxValue > 0) sb.append("/").append(v.maxValue);
            sb.append("\n");
        }
        return sb.toString();
    }

    /**
     * Get the prompt instruction for numerical output format.
     */
    public static String getNumericalInstruction() {
        return "\n数值变化规则：当你回复中涉及数值变化时，请使用 [数值名+/-N] 格式标注。" +
                "例如：好感度增加5点写作 [好感+5]，修为下降2点写作 [修为-2]。" +
                "多个数值变化可以连续标注。";
    }

    public static class NumChange {
        public final String name;
        public final int oldValue;
        public final int newValue;
        public final int delta;

        public NumChange(String name, int oldValue, int newValue, int delta) {
            this.name = name;
            this.oldValue = oldValue;
            this.newValue = newValue;
            this.delta = delta;
        }
    }
}
