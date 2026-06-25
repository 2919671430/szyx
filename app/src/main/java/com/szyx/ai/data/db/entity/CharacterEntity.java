package com.szyx.ai.data.db.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "characters")
public class CharacterEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    public String name = "";

    public String avatarPath;

    @NonNull
    public String systemPrompt = "";

    public String personality;
    public String description;
    public String firstMessage;
    public String tags;

    public long createdAt;
    public long updatedAt;

    public String rawJson;

    /** World setting / lore text (free-form) */
    public String worldSetting;

    /** Output style preference (e.g. "third person", "verbose", "concise") */
    public String outputStyle;

    /** Constraint level: 0=relaxed, 1=standard, 2=strict */
    public int constraintLevel = 1;

    /** Whether numerical system is enabled for this character */
    public boolean numericalEnabled;

    /** Whether world book module is enabled */
    public boolean worldBookEnabled = true;

    /** Whether LTM is enabled */
    public boolean ltmEnabled = true;

    /** Number of STM rounds to keep (0 = all) */
    public int stmRounds = 20;

    /** Supreme directive - injected before every user message to enforce writing rules */
    public String supremeDirective;

    /** Whether force-options generation is enabled */
    public boolean forceOptionsEnabled = true;
}
