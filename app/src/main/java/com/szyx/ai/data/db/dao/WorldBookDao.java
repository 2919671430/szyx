package com.szyx.ai.data.db.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.szyx.ai.data.db.entity.WorldBookEntity;

import java.util.List;

@Dao
public interface WorldBookDao {
    @Insert
    long insert(WorldBookEntity entry);

    @Update
    void update(WorldBookEntity entry);

    @Delete
    void delete(WorldBookEntity entry);

    @Query("SELECT * FROM world_books WHERE characterId = :characterId AND enabled = 1 ORDER BY priority DESC")
    List<WorldBookEntity> getEnabledEntriesForCharacter(long characterId);

    @Query("SELECT * FROM world_books WHERE characterId = :characterId ORDER BY priority DESC")
    List<WorldBookEntity> getAllEntriesForCharacter(long characterId);

    @Query("DELETE FROM world_books WHERE characterId = :characterId")
    void deleteAllForCharacter(long characterId);
}
