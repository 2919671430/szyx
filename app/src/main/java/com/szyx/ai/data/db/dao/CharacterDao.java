package com.szyx.ai.data.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.szyx.ai.data.db.entity.CharacterEntity;

import java.util.List;

@Dao
public interface CharacterDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(CharacterEntity character);

    @Update
    void update(CharacterEntity character);

    @Delete
    void delete(CharacterEntity character);

    @Query("SELECT * FROM characters ORDER BY updatedAt DESC")
    LiveData<List<CharacterEntity>> getAllCharacters();

    @Query("SELECT * FROM characters WHERE id = :id")
    LiveData<CharacterEntity> getCharacterById(long id);

    @Query("SELECT * FROM characters WHERE id = :id")
    CharacterEntity getCharacterByIdSync(long id);

    @Query("SELECT * FROM characters WHERE tags LIKE '%' || :tag || '%'")
    LiveData<List<CharacterEntity>> getCharactersByTag(String tag);

    @Query("SELECT COUNT(*) FROM characters")
    LiveData<Integer> getCharacterCount();
}
