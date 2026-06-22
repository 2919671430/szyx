package com.szyx.ai.data.db.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "world_books",
    foreignKeys = @ForeignKey(
        entity = CharacterEntity.class,
        parentColumns = "id",
        childColumns = "characterId",
        onDelete = ForeignKey.CASCADE
    ),
    indices = @Index("characterId"))
public class WorldBookEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public long characterId;

    /** Comma-separated trigger keywords */
    @NonNull
    public String keywords = "";

    /** Content to inject when triggered */
    @NonNull
    public String content = "";

    /** Priority: higher = injected first */
    public int priority;

    /** Whether this entry is enabled */
    public boolean enabled = true;

    public long createdAt;
}
