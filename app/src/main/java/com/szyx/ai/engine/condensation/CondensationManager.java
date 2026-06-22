package com.szyx.ai.engine.condensation;

import android.content.Context;
import android.util.Log;

import com.szyx.ai.data.db.entity.MessageEntity;
import com.szyx.ai.data.repository.ChatRepository;
import com.szyx.ai.engine.InferenceEngine;
import com.szyx.ai.engine.llm.StreamingCallback;
import com.szyx.ai.util.TokenCounter;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages automatic condensation of chat history.
 * When the active conversation exceeds the context window,
 * older messages are summarized and replaced with a single summary message.
 */
public class CondensationManager {

    private static final String TAG = "CondensationMgr";
    private static final int MAX_CONTEXT_TOKENS = 8192;
    private static final float CONDENSATION_THRESHOLD = 0.80f;
    private static final int MIN_RECENT_MESSAGES = 10;
    private static final int MAX_SUMMARIZE_BATCH = 30;

    /**
     * Check if condensation is needed and perform it if so.
     * @return true if condensation was performed
     */
    public boolean checkAndCondense(Context context, long sessionId,
                                     ChatRepository repo, InferenceEngine engine) {
        Integer totalTokens = repo.getTotalTokenCount(sessionId);
        if (totalTokens == null) totalTokens = 0;

        if (totalTokens < MAX_CONTEXT_TOKENS * CONDENSATION_THRESHOLD) {
            return false;
        }

        Log.i(TAG, "Condensation triggered: " + totalTokens + " tokens");

        List<MessageEntity> messages = repo.getNonSummaryMessagesSync(sessionId);
        if (messages.size() <= MIN_RECENT_MESSAGES) {
            return false;
        }

        int endIndex = messages.size() - MIN_RECENT_MESSAGES;
        int startIndex = Math.max(0, endIndex - MAX_SUMMARIZE_BATCH);

        List<MessageEntity> toSummarize = new ArrayList<>(messages.subList(startIndex, endIndex));

        String summaryPrompt = buildSummaryPrompt(toSummarize);
        List<InferenceEngine.Message> promptMessages = new ArrayList<>();
        promptMessages.add(new InferenceEngine.Message("user", summaryPrompt));

        final String[] summaryResult = {null};
        String summary = engine.generate(promptMessages, 0.3f, 256, new StreamingCallback() {
            @Override
            public boolean onToken(String token) {
                if (summaryResult[0] == null) summaryResult[0] = "";
                summaryResult[0] += token;
                return true;
            }
            @Override
            public void onComplete(String fullText) {
                summaryResult[0] = fullText;
            }
            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Summary generation failed", e);
            }
        });

        if (summary == null || summary.isEmpty()) {
            Log.w(TAG, "Summary generation returned empty");
            return false;
        }

        MessageEntity summaryMsg = new MessageEntity();
        summaryMsg.sessionId = sessionId;
        summaryMsg.role = "summary";
        summaryMsg.content = summary;
        summaryMsg.timestamp = toSummarize.get(0).timestamp;
        summaryMsg.tokenCount = TokenCounter.estimate(summary);
        summaryMsg.summarizedFromIndex = startIndex;
        summaryMsg.summarizedToIndex = endIndex;
        summaryMsg.summarizedMessageCount = toSummarize.size();

        List<Long> idsToHide = new ArrayList<>();
        for (MessageEntity m : toSummarize) {
            idsToHide.add(m.id);
        }
        repo.hideMessages(idsToHide);
        repo.insertMessage(summaryMsg);
        repo.incrementCondensation(sessionId, toSummarize.size());

        Log.i(TAG, "Condensed " + toSummarize.size() + " messages into summary");
        return true;
    }

    private String buildSummaryPrompt(List<MessageEntity> messages) {
        StringBuilder sb = new StringBuilder();
        sb.append("请简洁地总结以下对话，保留关键事实、决定、情感基调和重要细节。使用第三人称。\n\n");

        for (MessageEntity msg : messages) {
            String role = "user".equals(msg.role) ? "User" : "Assistant";
            sb.append(role).append(": ").append(msg.content).append("\n\n");
        }

        return sb.toString();
    }
}
