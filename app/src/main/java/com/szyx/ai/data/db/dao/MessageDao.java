package com.szyx.ai.data.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.szyx.ai.data.db.entity.MessageEntity;

import java.util.List;

@Dao
public interface MessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(MessageEntity message);

    @Insert
    List<Long> insertAll(List<MessageEntity> messages);

    @Update
    void update(MessageEntity message);

    @Delete
    void delete(MessageEntity message);

    @Query("SELECT * FROM messages WHERE sessionId = :sessionId AND isHidden = 0 ORDER BY timestamp ASC")
    LiveData<List<MessageEntity>> getMessagesForSession(long sessionId);

    @Query("SELECT * FROM messages WHERE sessionId = :sessionId AND isHidden = 0 ORDER BY timestamp ASC")
    List<MessageEntity> getMessagesForSessionSync(long sessionId);

    @Query("SELECT * FROM messages WHERE sessionId = :sessionId AND role != 'summary' AND isHidden = 0 ORDER BY timestamp ASC")
    List<MessageEntity> getNonSummaryMessagesSync(long sessionId);

    @Query("SELECT * FROM messages WHERE sessionId = :sessionId AND role != 'summary' AND isHidden = 0 ORDER BY timestamp DESC LIMIT :limit")
    List<MessageEntity> getRecentNonSummaryMessagesSync(long sessionId, int limit);

    @Query("SELECT * FROM messages WHERE sessionId = :sessionId AND role = 'summary' ORDER BY timestamp DESC LIMIT 1")
    MessageEntity getLatestSummarySync(long sessionId);

    @Query("SELECT SUM(tokenCount) FROM messages WHERE sessionId = :sessionId AND isHidden = 0")
    Integer getTotalTokenCount(long sessionId);

    @Query("SELECT COUNT(*) FROM messages WHERE sessionId = :sessionId AND isHidden = 0 AND role != 'summary'")
    int getActiveMessageCount(long sessionId);

    @Query("UPDATE messages SET isHidden = 1 WHERE id IN (:messageIds)")
    void hideMessages(List<Long> messageIds);

    @Query("DELETE FROM messages WHERE sessionId = :sessionId")
    void deleteAllForSession(long sessionId);
}
