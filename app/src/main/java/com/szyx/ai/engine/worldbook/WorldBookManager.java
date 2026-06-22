package com.szyx.ai.engine.worldbook;

import com.szyx.ai.data.db.entity.WorldBookEntity;
import com.szyx.ai.data.repository.ChatRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages world book entries - keyword-triggered knowledge injection.
 * Simple mode: user provides keywords + content, system handles matching automatically.
 */
public class WorldBookManager {

    /**
     * Add a new world book entry from simple text input.
     * Parses "keyword1,keyword2 | content" format.
     */
    public static long addEntryFromText(long characterId, String input, ChatRepository repo) {
        String[] parts = input.split("\\|", 2);
        if (parts.length < 2) return -1;

        WorldBookEntity entry = new WorldBookEntity();
        entry.characterId = characterId;
        entry.keywords = parts[0].trim();
        entry.content = parts[1].trim();
        entry.priority = 5;
        entry.enabled = true;
        entry.createdAt = System.currentTimeMillis();
        return repo.insertWorldBookEntry(entry);
    }

    /**
     * Add a world book entry with explicit parameters.
     */
    public static long addEntry(long characterId, String keywords, String content,
                                int priority, ChatRepository repo) {
        WorldBookEntity entry = new WorldBookEntity();
        entry.characterId = characterId;
        entry.keywords = keywords;
        entry.content = content;
        entry.priority = priority;
        entry.enabled = true;
        entry.createdAt = System.currentTimeMillis();
        return repo.insertWorldBookEntry(entry);
    }

    /**
     * Find triggered entries for a given user message.
     */
    public static List<WorldBookEntity> findTriggered(long characterId, String userText,
                                                       ChatRepository repo) {
        List<WorldBookEntity> allEntries = repo.getEnabledWorldBookEntries(characterId);
        List<WorldBookEntity> triggered = new ArrayList<>();
        String lowerText = userText.toLowerCase();

        for (WorldBookEntity entry : allEntries) {
            if (entry.keywords == null || entry.keywords.isEmpty()) continue;
            String[] keywords = entry.keywords.split(",");
            for (String keyword : keywords) {
                if (lowerText.contains(keyword.trim().toLowerCase())) {
                    triggered.add(entry);
                    break;
                }
            }
        }

        // Sort by priority (highest first)
        triggered.sort((a, b) -> Integer.compare(b.priority, a.priority));
        return triggered;
    }
}
