package com.szyx.ai.util;

/**
 * Estimates token count for text.
 * Uses a simple heuristic: ~4 characters per token for English,
 * ~2 characters per token for CJK text.
 */
public class TokenCounter {

    public static int estimate(String text) {
        if (text == null || text.isEmpty()) return 0;

        int cjkCount = 0;
        int otherCount = 0;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (isCJK(c)) {
                cjkCount++;
            } else {
                otherCount++;
            }
        }

        // CJK: ~1.5 tokens per character, Other: ~0.25 tokens per character
        return (int) (cjkCount * 1.5 + otherCount * 0.25);
    }

    private static boolean isCJK(char c) {
        return (c >= '\u4E00' && c <= '\u9FFF') ||  // CJK Unified Ideographs
               (c >= '\u3400' && c <= '\u4DBF') ||  // CJK Extension A
               (c >= '\uF900' && c <= '\uFAFF') ||  // CJK Compatibility Ideographs
               (c >= '\u3000' && c <= '\u303F') ||  // CJK Symbols and Punctuation
               (c >= '\uFF00' && c <= '\uFFEF');     // Halfwidth and Fullwidth Forms
    }
}
