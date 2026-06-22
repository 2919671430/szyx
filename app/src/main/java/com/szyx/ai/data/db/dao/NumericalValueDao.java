package com.szyx.ai.data.db.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.szyx.ai.data.db.entity.NumericalValueEntity;

import java.util.List;

@Dao
public interface NumericalValueDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(NumericalValueEntity value);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    List<Long> insertAll(List<NumericalValueEntity> values);

    @Update
    void update(NumericalValueEntity value);

    @Delete
    void delete(NumericalValueEntity value);

    @Query("SELECT * FROM numerical_values WHERE sessionId = :sessionId")
    List<NumericalValueEntity> getValuesForSession(long sessionId);

    @Query("SELECT * FROM numerical_values WHERE sessionId = :sessionId AND name = :name LIMIT 1")
    NumericalValueEntity getValueByName(long sessionId, String name);

    @Query("DELETE FROM numerical_values WHERE sessionId = :sessionId")
    void deleteAllForSession(long sessionId);
}
