package com.szyx.ai.data.db;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

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
}, version = 4, exportSchema = true)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;

    public abstract CharacterDao characterDao();
    public abstract SessionDao sessionDao();
    public abstract MessageDao messageDao();
    public abstract LongTermMemoryDao longTermMemoryDao();
    public abstract WorldBookDao worldBookDao();
    public abstract NumericalValueDao numericalValueDao();

    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("DROP TABLE IF EXISTS generated_images");
        }
    };

    static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE characters ADD COLUMN worldSetting TEXT");
            db.execSQL("ALTER TABLE characters ADD COLUMN outputStyle TEXT");
            db.execSQL("ALTER TABLE characters ADD COLUMN constraintLevel INTEGER NOT NULL DEFAULT 1");
            db.execSQL("ALTER TABLE characters ADD COLUMN numericalEnabled INTEGER NOT NULL DEFAULT 0");
            db.execSQL("ALTER TABLE characters ADD COLUMN worldBookEnabled INTEGER NOT NULL DEFAULT 1");
            db.execSQL("ALTER TABLE characters ADD COLUMN ltmEnabled INTEGER NOT NULL DEFAULT 1");
            db.execSQL("ALTER TABLE characters ADD COLUMN stmRounds INTEGER NOT NULL DEFAULT 20");
            db.execSQL("ALTER TABLE characters ADD COLUMN supremeDirective TEXT");
            db.execSQL("ALTER TABLE chat_sessions ADD COLUMN modelCode TEXT NOT NULL DEFAULT 'xiaomi'");
            db.execSQL("CREATE TABLE IF NOT EXISTS long_term_memories (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, sessionId INTEGER NOT NULL, tag TEXT, content TEXT, importance INTEGER NOT NULL, createdAt INTEGER NOT NULL, roundNumber INTEGER NOT NULL)");
            db.execSQL("CREATE TABLE IF NOT EXISTS world_books (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, characterId INTEGER NOT NULL, keywords TEXT, content TEXT, priority INTEGER NOT NULL, enabled INTEGER NOT NULL, createdAt INTEGER NOT NULL)");
            db.execSQL("CREATE TABLE IF NOT EXISTS numerical_values (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, sessionId INTEGER NOT NULL, name TEXT, currentValue REAL NOT NULL, maxValue REAL NOT NULL, minValue REAL NOT NULL, lastUpdatedAt INTEGER NOT NULL)");
        }
    };

    static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE characters ADD COLUMN forceOptionsEnabled INTEGER NOT NULL DEFAULT 1");
        }
    };

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "szyx_ai.db")
                        .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                        .build();
                }
            }
        }
        return INSTANCE;
    }
}
