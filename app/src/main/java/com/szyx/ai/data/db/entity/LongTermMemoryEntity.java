package com.szyx.ai.data.db.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "long_term_memories",
    foreignKeys = @ForeignKey(
        entity = ChatSessionEntity.class,
        parentColumns = "id",
        childColumns = "sessionId",
        onDelete = ForeignKey.CASCADE
    ),
    indices = @Index("sessionId"))
public class LongTermMemoryEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public long sessionId;

    /** Tags: "plot", "affection", "choice", "event", "custom" */
    @NonNull
    public String tag = "event";

    @NonNull
    public String content = "";

    public int importance; // 1-10, higher = more important
    public long createdAt;
    public int roundNumber; // which conversation round this was extracted from
}
