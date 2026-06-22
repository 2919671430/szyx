package com.szyx.ai.engine.memory;

import android.content.Context;

import com.szyx.ai.data.db.entity.CharacterEntity;
import com.szyx.ai.data.db.entity.ChatSessionEntity;
import com.szyx.ai.data.db.entity.LongTermMemoryEntity;
import com.szyx.ai.data.db.entity.MessageEntity;
import com.szyx.ai.data.repository.ChatRepository;
import com.szyx.ai.engine.InferenceEngine;
import com.szyx.ai.engine.llm.StreamingCallback;
import com.szyx.ai.util.AppLog;
import com.szyx.ai.util.TokenCounter;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages dual memory system:
 * - STM (Short-Term Memory): Recent N rounds of conversation
 * - LTM (Long-Term Memory): Structured memory entries extracted from conversation
 *
 * LTM extraction happens automatically when conversation gets long enough.
 */
public class MemoryManager {

    private static final String TAG = "MemoryManager";
    private static final int LTM_EXTRACTION_THRESHOLD = 30; // Extract LTM every 30 messages
    private static final int LTM_BATCH_SIZE = 10; // Summarize 10 messages at a time

    /**
     * Check if LTM extraction should happen and perform it.
     * Called before each generation.
     */
    public void checkAndExtractLTM(Context context, long sessionId, ChatRepository repo,
                                    InferenceEngine engine, String apiKey) {
        ChatSessionEntity session = repo.getSessionByIdSync(sessionId);
        if (session == null) return;

        CharacterEntity character = repo.getCharacterByIdSync(session.characterId);
        if (character == null || !character.ltmEnabled) return;

        int messageCount = repo.getActiveMessageCount(sessionId);
        int existingLTMCount = repo.getLTMCount(sessionId);

        // Extract LTM if we have enough new messages since last extraction
        int messagesSinceLastLTM = messageCount - (existingLTMCount * LTM_BATCH_SIZE);
        if (messagesSinceLastLTM >= LTM_EXTRACTION_THRESHOLD) {
            extractLTM(sessionId, repo, engine, apiKey);
        }
    }

    /**
     * Extract long-term memories from recent conversation.
     */
    private void extractLTM(long sessionId, ChatRepository repo,
                            InferenceEngine engine, String apiKey) {
        AppLog.i(TAG, "开始提取长期记忆...");

        // Get recent non-summary messages
        List<MessageEntity> messages = repo.getNonSummaryMessagesSync(sessionId);
        if (messages.size() < LTM_BATCH_SIZE) return;

        // Take the oldest batch that hasn't been summarized yet
        int existingLTMCount = repo.getLTMCount(sessionId);
        int startIndex = existingLTMCount * LTM_BATCH_SIZE;
        if (startIndex >= messages.size()) return;

        int endIndex = Math.min(startIndex + LTM_BATCH_SIZE, messages.size());
        List<MessageEntity> batch = messages.subList(startIndex, endIndex);

        // Build extraction prompt
        StringBuilder conversationText = new StringBuilder();
        for (MessageEntity msg : batch) {
            String role = "user".equals(msg.role) ? "用户" : "AI";
            conversationText.append(role).append(": ").append(msg.content).append("\n");
        }

        String extractionPrompt = "请从以下对话中提取关键信息，按以下格式输出（每条一行）：\n" +
                "[剧情] 关键剧情发展\n" +
                "[好感] 角色关系变化\n" +
                "[抉择] 用户做出的重要选择\n" +
                "[事件] 重要事件记录\n" +
                "\n如果没有某类信息，跳过该分类。每条信息简洁明了，不超过50字。\n\n" +
                "对话内容：\n" + conversationText.toString();

        List<InferenceEngine.Message> extractionMessages = new ArrayList<>();
        extractionMessages.add(new InferenceEngine.Message("user", extractionPrompt));

        if (engine.requiresApiKey()) {
            InferenceEngine.setApiKey(apiKey);
        }

        String result = engine.generate(extractionMessages, 0.3f, 512, new StreamingCallback() {
            @Override public boolean onToken(String token) { return true; }
            @Override public void onComplete(String fullText) {}
            @Override public void onError(Exception e) {
                AppLog.e(TAG, "LTM 提取失败: " + e.getMessage());
            }
        });

        if (result == null || result.isEmpty()) return;

        // Parse and save LTM entries
        String[] lines = result.split("\n");
        int roundNumber = (int) batch.get(batch.size() - 1).id; // Use last message ID as round marker

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("请从") || line.startsWith("对话")) continue;

            String tag = "event"; // default
            String content = line;

            if (line.startsWith("[剧情]")) {
                tag = "plot";
                content = line.substring(line.indexOf(']') + 1).trim();
            } else if (line.startsWith("[好感]")) {
                tag = "affection";
                content = line.substring(line.indexOf(']') + 1).trim();
            } else if (line.startsWith("[抉择]")) {
                tag = "choice";
                content = line.substring(line.indexOf(']') + 1).trim();
            } else if (line.startsWith("[事件]")) {
                tag = "event";
                content = line.substring(line.indexOf(']') + 1).trim();
            }

            if (!content.isEmpty()) {
                LongTermMemoryEntity ltm = new LongTermMemoryEntity();
                ltm.sessionId = sessionId;
                ltm.tag = tag;
                ltm.content = content;
                ltm.importance = tag.equals("choice") ? 8 : (tag.equals("plot") ? 7 : 5);
                ltm.createdAt = System.currentTimeMillis();
                ltm.roundNumber = roundNumber;
                repo.insertLTM(ltm);
            }
        }

        AppLog.i(TAG, "长期记忆提取完成");
    }

    /**
     * Get STM messages with configurable round limit.
     */
    public static List<MessageEntity> getSTMMessages(ChatRepository repo, long sessionId,
                                                      int stmRounds) {
        if (stmRounds <= 0) {
            return repo.getNonSummaryMessagesSync(sessionId);
        }
        List<MessageEntity> recent = repo.getRecentNonSummaryMessagesSync(sessionId, stmRounds * 2);
        java.util.Collections.reverse(recent);
        return recent;
    }
}
