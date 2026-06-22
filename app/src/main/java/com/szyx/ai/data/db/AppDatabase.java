package com.szyx.ai.data.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.szyx.ai.data.db.dao.CharacterDao;
import com.szyx.ai.data.db.dao.LongTermMemoryDao;
import com.szyx.ai.data.db.dao.MessageDao;
import com.szyx.ai.data.db.dao.NumericalValueDao;
import com.szyx.ai.data.db.dao.SessionDao;
import com.szyx.ai.data.db.dao.WorldBookDao;
import com.szyx.ai.data.db.entity.CharacterEntity;
import com.szyx.ai.data.db.entity.ChatSessionEntity;
import com.szyx.ai.data.db.entity.LongTermMemoryEntity;
import com.szyx.ai.data.db.entity.MessageEntity;
import com.szyx.ai.data.db.entity.NumericalValueEntity;
import com.szyx.ai.data.db.entity.WorldBookEntity;

@Database(entities = {
    CharacterEntity.class,
    ChatSessionEntity.class,
    MessageEntity.class,
    LongTermMemoryEntity.class,
    WorldBookEntity.class,
    NumericalValueEntity.class
}, version = 3, exportSchema = true)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;

    public abstract CharacterDao characterDao();
    public abstract SessionDao sessionDao();
    public abstract MessageDao messageDao();
    public abstract LongTermMemoryDao longTermMemoryDao();
    public abstract WorldBookDao worldBookDao();
    public abstract NumericalValueDao numericalValueDao();

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "szyx_ai.db")
                        .fallbackToDestructiveMigration()
                        .build();
                }
            }
        }
        return INSTANCE;
    }
}
