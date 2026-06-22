package com.szyx.ai.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.szyx.ai.data.db.entity.ChatSessionEntity;
import com.szyx.ai.data.repository.ChatRepository;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SessionViewModel extends AndroidViewModel {

    private final ChatRepository repository;
    private final ExecutorService executor;

    public SessionViewModel(@NonNull Application application) {
        super(application);
        repository = new ChatRepository(application);
        executor = Executors.newSingleThreadExecutor();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executor.shutdownNow();
    }

    public LiveData<List<ChatSessionEntity>> getSessions(long characterId) {
        return repository.getSessionsForCharacter(characterId);
    }

    public void createSession(long characterId, String name) {
        executor.execute(() -> repository.createSession(characterId, name));
    }

    public void deleteSession(ChatSessionEntity session) {
        executor.execute(() -> repository.deleteSessionSync(session));
    }

    public void duplicateSession(long sourceSessionId, String newName) {
        executor.execute(() -> repository.duplicateSession(sourceSessionId, newName));
    }

    public void update(ChatSessionEntity session) {
        executor.execute(() -> repository.updateSession(session));
    }
}
