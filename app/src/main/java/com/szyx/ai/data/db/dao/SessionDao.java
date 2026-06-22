package com.szyx.ai.data.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.szyx.ai.data.db.entity.ChatSessionEntity;

import java.util.List;

@Dao
public interface SessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(ChatSessionEntity session);

    @Update
    void update(ChatSessionEntity session);

    @Delete
    void delete(ChatSessionEntity session);

    @Query("SELECT * FROM chat_sessions WHERE characterId = :characterId ORDER BY lastMessageAt DESC")
    LiveData<List<ChatSessionEntity>> getSessionsForCharacter(long characterId);

    @Query("SELECT * FROM chat_sessions WHERE id = :id")
    LiveData<ChatSessionEntity> getSessionById(long id);

    @Query("SELECT * FROM chat_sessions WHERE id = :id")
    ChatSessionEntity getSessionByIdSync(long id);

    @Query("UPDATE chat_sessions SET lastMessageAt = :timestamp, messageCount = :count WHERE id = :sessionId")
    void updateSessionMeta(long sessionId, long timestamp, int count);

    @Query("UPDATE chat_sessions SET condensationCount = condensationCount + 1, condensedMessageCount = condensedMessageCount + :replaced WHERE id = :sessionId")
    void incrementCondensation(long sessionId, int replaced);
}
