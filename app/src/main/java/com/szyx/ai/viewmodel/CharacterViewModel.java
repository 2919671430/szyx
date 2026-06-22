package com.szyx.ai.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.szyx.ai.data.db.entity.CharacterEntity;
import com.szyx.ai.data.repository.CharacterRepository;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CharacterViewModel extends AndroidViewModel {

    private final CharacterRepository repository;
    private final LiveData<List<CharacterEntity>> allCharacters;
    private final ExecutorService executor;

    public CharacterViewModel(@NonNull Application application) {
        super(application);
        repository = new CharacterRepository(application);
        allCharacters = repository.getAllCharacters();
        executor = Executors.newSingleThreadExecutor();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executor.shutdownNow();
    }

    public LiveData<List<CharacterEntity>> getAllCharacters() {
        return allCharacters;
    }

    public LiveData<CharacterEntity> getCharacterById(long id) {
        return repository.getCharacterById(id);
    }

    public void insert(CharacterEntity character) {
        executor.execute(() -> repository.insert(character));
    }

    public void update(CharacterEntity character) {
        executor.execute(() -> repository.update(character));
    }

    public void delete(CharacterEntity character) {
        executor.execute(() -> repository.delete(character));
    }
}
