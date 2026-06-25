package com.szyx.ai.data.db.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "chat_sessions",
    foreignKeys = @ForeignKey(
        entity = CharacterEntity.class,
        parentColumns = "id",
        childColumns = "characterId",
        onDelete = ForeignKey.CASCADE
    ),
    indices = @Index("characterId"))
public class ChatSessionEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public long characterId;

    @NonNull
    public String sessionName = "";

    public String description;
    public long createdAt;
    public long lastMessageAt;
    public int messageCount;
    public int condensationCount;
    public int condensedMessageCount;

    /** Model code: "local" for local LLM, "xiaomi" for Xiaomi API, etc. */
    @NonNull
    public String modelCode = "deepseek";
}
