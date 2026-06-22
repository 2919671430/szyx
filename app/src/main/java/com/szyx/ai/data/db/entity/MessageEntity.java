package com.szyx.ai.data.db.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "messages",
    foreignKeys = @ForeignKey(
        entity = ChatSessionEntity.class,
        parentColumns = "id",
        childColumns = "sessionId",
        onDelete = ForeignKey.CASCADE
    ),
    indices = @Index("sessionId"))
public class MessageEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public long sessionId;

    @NonNull
    public String role = "user";

    @NonNull
    public String content = "";

    public long timestamp;
    public int tokenCount;

    public int summarizedFromIndex;
    public int summarizedToIndex;
    public int summarizedMessageCount;

    public String imagePath;
    public boolean isEdited;
    public boolean isHidden;
}
