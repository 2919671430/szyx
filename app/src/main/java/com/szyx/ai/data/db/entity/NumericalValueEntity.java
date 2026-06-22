package com.szyx.ai.data.db.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "numerical_values",
    foreignKeys = @ForeignKey(
        entity = ChatSessionEntity.class,
        parentColumns = "id",
        childColumns = "sessionId",
        onDelete = ForeignKey.CASCADE
    ),
    indices = @Index("sessionId"))
public class NumericalValueEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public long sessionId;

    @NonNull
    public String name = ""; // e.g. "好感", "修为", "金币"

    public int currentValue;
    public int maxValue; // 0 = no max
    public int minValue; // 0 = no min

    public long lastUpdatedAt;
}
