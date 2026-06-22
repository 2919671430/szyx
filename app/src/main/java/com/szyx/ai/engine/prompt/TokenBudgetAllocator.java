package com.szyx.ai.engine.prompt;

import com.szyx.ai.util.TokenCounter;

import java.util.List;

/**
 * Allocates token budget across prompt layers by priority.
 * Higher priority layers are never truncated. Lower priority layers get remaining budget.
 *
 * Priority order (highest first):
 * 1. System skeleton (built-in rules) - NEVER truncated
 * 2. User world/character setting - NEVER truncated
 * 3. LTM long-term memories - NEVER truncated (but capped at maxLtmTokens)
 * 4. World book entries - truncated by priority if needed
 * 5. STM recent messages - truncated from oldest
 * 6. User latest message - NEVER truncated
 */
public class TokenBudgetAllocator {

    private final int totalBudget;

    // Fixed allocations (never truncated)
    private int systemSkeletonTokens = 0;
    private int userSettingTokens = 0;
    private int ltmTokens = 0;
    private int latestMessageTokens = 0;

    // Flexible allocations (truncated if needed)
    private int worldBookTokens = 0;
    private int stmTokens = 0;

    // Limits
    private static final int MAX_LTM_TOKENS = 1500;
    private static final int MAX_WORLD_BOOK_TOKENS = 2000;

    public TokenBudgetAllocator(int totalBudget) {
        this.totalBudget = totalBudget;
    }

    /**
     * Calculate token allocations for each layer.
     * @return Allocation result with token counts per layer
     */
    public Allocation allocate(int systemSkeleton, int userSetting, int ltm,
                               int worldBook, int stm, int latestMessage) {
        systemSkeletonTokens = systemSkeleton;
        userSettingTokens = userSetting;
        ltmTokens = Math.min(ltm, MAX_LTM_TOKENS);
        latestMessageTokens = latestMessage;

        // Fixed total
        int fixedTotal = systemSkeletonTokens + userSettingTokens + ltmTokens + latestMessageTokens;

        // Remaining budget for flexible layers
        int remaining = totalBudget - fixedTotal;
        if (remaining < 0) remaining = 0;

        // World book gets up to MAX_WORLD_BOOK_TOKENS from remaining
        worldBookTokens = Math.min(worldBook, Math.min(remaining, MAX_WORLD_BOOK_TOKENS));
        remaining -= worldBookTokens;

        // STM gets the rest
        stmTokens = Math.min(stm, remaining);

        return new Allocation(systemSkeletonTokens, userSettingTokens, ltmTokens,
                worldBookTokens, stmTokens, latestMessageTokens);
    }

    /**
     * Truncate a list of text items to fit within a token budget, removing from the start.
     * Returns the items that fit within the budget.
     */
    public static List<String> truncateFromStart(List<String> items, int maxTokens) {
        int totalTokens = 0;
        for (String item : items) {
            totalTokens += TokenCounter.estimate(item);
        }

        if (totalTokens <= maxTokens) return items;

        // Remove from start until we fit
        while (items.size() > 1 && totalTokens > maxTokens) {
            String removed = items.remove(0);
            totalTokens -= TokenCounter.estimate(removed);
        }
        return items;
    }

    /**
     * Truncate a list of text items to fit within a token budget, removing from the end.
     */
    public static List<String> truncateFromEnd(List<String> items, int maxTokens) {
        int totalTokens = 0;
        for (String item : items) {
            totalTokens += TokenCounter.estimate(item);
        }

        if (totalTokens <= maxTokens) return items;

        while (items.size() > 1 && totalTokens > maxTokens) {
            String removed = items.remove(items.size() - 1);
            totalTokens -= TokenCounter.estimate(removed);
        }
        return items;
    }

    public static class Allocation {
        public final int systemSkeleton;
        public final int userSetting;
        public final int ltm;
        public final int worldBook;
        public final int stm;
        public final int latestMessage;

        public Allocation(int systemSkeleton, int userSetting, int ltm,
                         int worldBook, int stm, int latestMessage) {
            this.systemSkeleton = systemSkeleton;
            this.userSetting = userSetting;
            this.ltm = ltm;
            this.worldBook = worldBook;
            this.stm = stm;
            this.latestMessage = latestMessage;
        }

        public int total() {
            return systemSkeleton + userSetting + ltm + worldBook + stm + latestMessage;
        }
    }
}
