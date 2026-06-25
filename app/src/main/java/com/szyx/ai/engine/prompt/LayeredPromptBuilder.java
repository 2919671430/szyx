package com.szyx.ai.engine.prompt;

import android.content.Context;

import com.szyx.ai.data.db.entity.CharacterEntity;
import com.szyx.ai.data.db.entity.ChatSessionEntity;
import com.szyx.ai.data.db.entity.LongTermMemoryEntity;
import com.szyx.ai.data.db.entity.MessageEntity;
import com.szyx.ai.data.db.entity.NumericalValueEntity;
import com.szyx.ai.data.db.entity.WorldBookEntity;
import com.szyx.ai.data.repository.ChatRepository;
import com.szyx.ai.engine.InferenceEngine;
import com.szyx.ai.util.TokenCounter;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds prompts using layered assembly with token budget allocation.
 * Order: System skeleton → User settings → LTM → World book → STM → Numerical → User message
 */
public class LayeredPromptBuilder {

    private static final int DEFAULT_CONTEXT_SIZE = 8192;
    private static final int RESERVED_FOR_GENERATION = 1024;

    /**
     * Build a complete layered prompt as a list of InferenceEngine.Message.
     */
    public static List<InferenceEngine.Message> buildMessages(Context context, long sessionId,
                                                               ChatRepository repo, String userText) {
        List<InferenceEngine.Message> messages = new ArrayList<>();

        ChatSessionEntity session = repo.getSessionByIdSync(sessionId);
        if (session == null) return messages;

        CharacterEntity character = repo.getCharacterByIdSync(session.characterId);
        if (character == null) return messages;

        int totalBudget = DEFAULT_CONTEXT_SIZE - RESERVED_FOR_GENERATION;

        // Layer 1: Built-in system skeleton (NEVER truncated)
        String systemSkeleton = buildSystemSkeleton(character);
        messages.add(new InferenceEngine.Message("system", systemSkeleton));

        // Layer 2: User world/character setting (NEVER truncated)
        String userSetting = buildUserSetting(character);
        if (!userSetting.isEmpty()) {
            messages.add(new InferenceEngine.Message("system", userSetting));
        }

        // Layer 3: LTM long-term memories (capped at MAX_LTM_TOKENS)
        if (character.ltmEnabled) {
            List<LongTermMemoryEntity> ltms = repo.getLTMsForSession(sessionId);
            if (!ltms.isEmpty()) {
                String ltmText = buildLTMText(ltms);
                messages.add(new InferenceEngine.Message("system",
                        "[长期记忆]\n" + ltmText));
            }
        }

        // Layer 4: World book entries (truncated by priority if needed)
        if (character.worldBookEnabled) {
            List<WorldBookEntry> triggered = findTriggeredEntries(
                    repo.getEnabledWorldBookEntries(session.characterId), userText);
            if (!triggered.isEmpty()) {
                String worldBookText = buildWorldBookText(triggered);
                messages.add(new InferenceEngine.Message("system",
                        "[世界设定]\n" + worldBookText));
            }
        }

        // Layer 5: Numerical values (if enabled)
        if (character.numericalEnabled) {
            List<NumericalValueEntity> values = repo.getNumericalValues(sessionId);
            if (!values.isEmpty()) {
                String numericalText = buildNumericalText(values);
                messages.add(new InferenceEngine.Message("system",
                        "[当前数值状态]\n" + numericalText));
            }
            // Add numerical output format instruction
            messages.add(new InferenceEngine.Message("system",
                    com.szyx.ai.engine.numerical.NumericalManager.getNumericalInstruction()));
        }

        // Layer 6: STM recent messages
        int stmRounds = character.stmRounds > 0 ? character.stmRounds : 20;
        List<MessageEntity> recentMessages;
        if (stmRounds > 0) {
            recentMessages = repo.getRecentNonSummaryMessagesSync(sessionId, stmRounds * 2);
        } else {
            recentMessages = repo.getNonSummaryMessagesSync(sessionId);
        }
        // Reverse to chronological order (getRecent returns DESC)
        java.util.Collections.reverse(recentMessages);

        for (MessageEntity msg : recentMessages) {
            String role = "user".equals(msg.role) ? "user" : "assistant";
            messages.add(new InferenceEngine.Message(role, msg.content));
        }

        // Layer 7: Supreme directive (injected before every user message)
        if (character.supremeDirective != null && !character.supremeDirective.isEmpty()) {
            messages.add(new InferenceEngine.Message("system",
                    "[最高准则] 以下规则优先级最高，必须严格遵守：\n" + character.supremeDirective));
        }

        // Layer 8: User latest message
        messages.add(new InferenceEngine.Message("user", userText));

        return messages;
    }

    /**
     * Build the built-in system skeleton - non-modifiable rules.
     */
    private static String buildSystemSkeleton(CharacterEntity character) {
        StringBuilder sb = new StringBuilder();

        sb.append("你是一个AI角色扮演助手。请严格遵守以下规则：\n");
        sb.append("1. 始终保持角色身份，不要跳出角色或承认自己是AI\n");
        sb.append("2. 根据角色的性格和设定进行回复，保持人设一致性\n");
        sb.append("3. 回复应自然流畅，符合角色的语气和风格\n");

        if (character.forceOptionsEnabled) {
            sb.append("\n【强制格式要求 - 最高优先级】你的每次回复必须以2-4个剧情选项结尾，这是系统硬性规定，无论任何情况都不可省略。\n");
            sb.append("选项格式规则：\n");
            sb.append("- 每个选项必须用中文方括号【】包裹\n");
            sb.append("- 每个选项独占一行\n");
            sb.append("- 选项内容必须是具体的剧情行动方向，不能是笼统的描述\n");
            sb.append("- 最后一行除了选项外不要写其他内容\n");
            sb.append("正确示例：\n");
            sb.append("【跟她走进那扇门】\n");
            sb.append("【先检查一下背包里的物品】\n");
            sb.append("【大声呼喊看有没有人回应】\n");
            sb.append("【拒绝她的邀请，转身离开】\n");
        }

        if (character.constraintLevel >= 1) {
            sb.append("4. 不要擅自改变剧情走向，除非用户明确引导\n");
            sb.append("5. 保持世界观的一致性，不要引入矛盾设定\n");
        }
        if (character.constraintLevel >= 2) {
            sb.append("6. 严格按照角色设定行动，不要添加未定义的能力或背景\n");
            sb.append("7. 对于数值变化，严格按照规则执行\n");
        }

        if (character.outputStyle != null && !character.outputStyle.isEmpty()) {
            sb.append("\n输出风格要求：").append(character.outputStyle).append("\n");
        }

        return sb.toString();
    }

    /**
     * Build user-defined world and character setting.
     */
    private static String buildUserSetting(CharacterEntity character) {
        StringBuilder sb = new StringBuilder();

        if (character.systemPrompt != null && !character.systemPrompt.isEmpty()) {
            sb.append(character.systemPrompt).append("\n");
        }
        if (character.personality != null && !character.personality.isEmpty()) {
            sb.append("性格特征：").append(character.personality).append("\n");
        }
        if (character.description != null && !character.description.isEmpty()) {
            sb.append("角色描述：").append(character.description).append("\n");
        }
        if (character.worldSetting != null && !character.worldSetting.isEmpty()) {
            sb.append("\n世界观设定：\n").append(character.worldSetting).append("\n");
        }

        return sb.toString().trim();
    }

    /**
     * Build LTM text from structured memory entries.
     */
    private static String buildLTMText(List<LongTermMemoryEntity> ltms) {
        StringBuilder sb = new StringBuilder();
        for (LongTermMemoryEntity ltm : ltms) {
            sb.append("[").append(ltm.tag).append("] ").append(ltm.content).append("\n");
        }
        return sb.toString().trim();
    }

    /**
     * Find world book entries triggered by keywords in the user text.
     */
    private static List<WorldBookEntry> findTriggeredEntries(List<WorldBookEntity> entries,
                                                              String userText) {
        List<WorldBookEntry> triggered = new ArrayList<>();
        String lowerText = userText.toLowerCase();

        for (WorldBookEntity entry : entries) {
            if (entry.keywords == null || entry.keywords.isEmpty()) continue;
            String[] keywords = entry.keywords.split(",");
            for (String keyword : keywords) {
                if (lowerText.contains(keyword.trim().toLowerCase())) {
                    triggered.add(new WorldBookEntry(entry.content, entry.priority));
                    break;
                }
            }
        }

        // Sort by priority (highest first)
        triggered.sort((a, b) -> Integer.compare(b.priority, a.priority));
        return triggered;
    }

    /**
     * Build world book text from triggered entries.
     */
    private static String buildWorldBookText(List<WorldBookEntry> entries) {
        StringBuilder sb = new StringBuilder();
        for (WorldBookEntry entry : entries) {
            sb.append(entry.content).append("\n\n");
        }
        return sb.toString().trim();
    }

    /**
     * Build numerical values text.
     */
    private static String buildNumericalText(List<NumericalValueEntity> values) {
        StringBuilder sb = new StringBuilder();
        for (NumericalValueEntity v : values) {
            sb.append(v.name).append(": ").append(v.currentValue);
            if (v.maxValue > 0) {
                sb.append("/").append(v.maxValue);
            }
            sb.append("\n");
        }
        return sb.toString().trim();
    }

    /** Internal holder for world book entries with priority */
    private static class WorldBookEntry {
        final String content;
        final int priority;
        WorldBookEntry(String content, int priority) {
            this.content = content;
            this.priority = priority;
        }
    }
}
