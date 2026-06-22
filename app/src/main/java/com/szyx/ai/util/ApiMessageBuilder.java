package com.szyx.ai.util;

import android.content.Context;

import com.szyx.ai.data.db.entity.CharacterEntity;
import com.szyx.ai.data.db.entity.ChatSessionEntity;
import com.szyx.ai.data.db.entity.MessageEntity;
import com.szyx.ai.data.repository.ChatRepository;
import com.szyx.ai.engine.InferenceEngine;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds OpenAI-format messages array for API calls from the app's database.
 * Uses the unified InferenceEngine.Message type.
 */
public class ApiMessageBuilder {

    public static List<InferenceEngine.Message> buildMessages(Context context, long sessionId,
                                                               ChatRepository repo) {
        List<InferenceEngine.Message> messages = new ArrayList<>();

        ChatSessionEntity session = repo.getSessionByIdSync(sessionId);
        if (session == null) return messages;

        CharacterEntity character = repo.getCharacterByIdSync(session.characterId);
        if (character == null) return messages;

        // System prompt (character persona)
        StringBuilder systemPrompt = new StringBuilder();
        if (character.systemPrompt != null && !character.systemPrompt.isEmpty()) {
            systemPrompt.append(character.systemPrompt);
        }
        if (character.personality != null && !character.personality.isEmpty()) {
            if (systemPrompt.length() > 0) systemPrompt.append("\n");
            systemPrompt.append("Personality traits: ").append(character.personality);
        }
        if (systemPrompt.length() > 0) {
            messages.add(new InferenceEngine.Message("system", systemPrompt.toString()));
        }

        // Latest summary (if exists)
        MessageEntity summary = repo.getLatestSummarySync(sessionId);
        if (summary != null) {
            messages.add(new InferenceEngine.Message("system",
                    "[Previous conversation summary: " + summary.content + "]"));
        }

        // Recent messages (excluding hidden and summaries)
        List<MessageEntity> recentMessages = repo.getNonSummaryMessagesSync(sessionId);
        for (MessageEntity msg : recentMessages) {
            String role = "user".equals(msg.role) ? "user" : "assistant";
            messages.add(new InferenceEngine.Message(role, msg.content));
        }

        return messages;
    }
}
