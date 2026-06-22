package com.szyx.ai.util;

import android.content.Context;

import com.szyx.ai.data.db.entity.CharacterEntity;
import com.szyx.ai.data.db.entity.ChatSessionEntity;
import com.szyx.ai.data.db.entity.MessageEntity;
import com.szyx.ai.data.repository.ChatRepository;

import java.util.List;

/**
 * Builds the full prompt for LLM generation, incorporating:
 * - Character system prompt (Gemma format)
 * - Latest summary (if any)
 * - Recent non-summary messages
 */
public class PromptBuilder {

    public static String buildPrompt(Context context, long sessionId, ChatRepository repo) {
        ChatSessionEntity session = repo.getSessionByIdSync(sessionId);
        if (session == null) return "<start_of_turn>model\n";
        CharacterEntity character = repo.getCharacterByIdSync(session.characterId);
        if (character == null) return "<start_of_turn>model\n";

        StringBuilder prompt = new StringBuilder();

        // System prompt (character persona) in Gemma format
        prompt.append("<start_of_turn>system\n");
        prompt.append(character.systemPrompt != null ? character.systemPrompt : "").append("\n");
        if (character.personality != null && !character.personality.isEmpty()) {
            prompt.append("Personality traits: ").append(character.personality).append("\n");
        }
        prompt.append("<end_of_turn>\n");

        // Latest summary (if exists)
        MessageEntity summary = repo.getLatestSummarySync(sessionId);
        if (summary != null) {
            prompt.append("<start_of_turn>system\n");
            prompt.append("[Previous conversation summary: ").append(summary.content).append("]\n");
            prompt.append("<end_of_turn>\n");
        }

        // Recent messages (excluding hidden and summaries)
        List<MessageEntity> recentMessages = repo.getNonSummaryMessagesSync(sessionId);
        for (MessageEntity msg : recentMessages) {
            String role = "user".equals(msg.role) ? "user" : "model";
            prompt.append("<start_of_turn>").append(role).append("\n");
            prompt.append(msg.content).append("\n");
            prompt.append("<end_of_turn>\n");
        }

        // Start assistant turn
        prompt.append("<start_of_turn>model\n");

        return prompt.toString();
    }
}
