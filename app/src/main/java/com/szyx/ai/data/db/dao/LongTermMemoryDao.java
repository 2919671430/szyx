package com.szyx.ai.data.db.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.szyx.ai.data.db.entity.LongTermMemoryEntity;

import java.util.List;

@Dao
public interface LongTermMemoryDao {
    @Insert
    long insert(LongTermMemoryEntity memory);

    @Update
    void update(LongTermMemoryEntity memory);

    @Delete
    void delete(LongTermMemoryEntity memory);

    @Query("SELECT * FROM long_term_memories WHERE sessionId = :sessionId ORDER BY importance DESC, createdAt ASC")
    List<LongTermMemoryEntity> getMemoriesForSession(long sessionId);

    @Query("SELECT * FROM long_term_memories WHERE sessionId = :sessionId AND tag = :tag ORDER BY importance DESC")
    List<LongTermMemoryEntity> getMemoriesByTag(long sessionId, String tag);

    @Query("SELECT COUNT(*) FROM long_term_memories WHERE sessionId = :sessionId")
    int getMemoryCount(long sessionId);

    @Query("DELETE FROM long_term_memories WHERE sessionId = :sessionId")
    void deleteAllForSession(long sessionId);
}
