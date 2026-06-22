package com.szyx.ai.data.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.szyx.ai.data.db.AppDatabase;
import com.szyx.ai.data.db.dao.CharacterDao;
import com.szyx.ai.data.db.entity.CharacterEntity;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CharacterRepository {

    private final CharacterDao characterDao;
    private final ExecutorService executor;

    public CharacterRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        characterDao = db.characterDao();
        executor = Executors.newSingleThreadExecutor();
    }

    public LiveData<List<CharacterEntity>> getAllCharacters() {
        return characterDao.getAllCharacters();
    }

    public LiveData<CharacterEntity> getCharacterById(long id) {
        return characterDao.getCharacterById(id);
    }

    public CharacterEntity getCharacterByIdSync(long id) {
        return characterDao.getCharacterByIdSync(id);
    }

    public long insert(CharacterEntity character) {
        return characterDao.insert(character);
    }

    public void update(CharacterEntity character) {
        executor.execute(() -> characterDao.update(character));
    }

    public void delete(CharacterEntity character) {
        executor.execute(() -> characterDao.delete(character));
    }
}
