package com.szyx.ai.data.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.szyx.ai.data.db.AppDatabase;
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

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatRepository {

    private final SessionDao sessionDao;
    private final MessageDao messageDao;
    private final CharacterDao characterDao;
    private final LongTermMemoryDao ltmDao;
    private final WorldBookDao worldBookDao;
    private final NumericalValueDao numericalDao;
    private final ExecutorService executor;

    public ChatRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        sessionDao = db.sessionDao();
        messageDao = db.messageDao();
        characterDao = db.characterDao();
        ltmDao = db.longTermMemoryDao();
        worldBookDao = db.worldBookDao();
        numericalDao = db.numericalValueDao();
        executor = Executors.newFixedThreadPool(2);
    }

    // ---- Session ----

    public LiveData<List<ChatSessionEntity>> getSessionsForCharacter(long characterId) {
        return sessionDao.getSessionsForCharacter(characterId);
    }

    public long createSession(long characterId, String name) {
        ChatSessionEntity session = new ChatSessionEntity();
        session.characterId = characterId;
        session.sessionName = name;
        session.createdAt = System.currentTimeMillis();
        session.lastMessageAt = System.currentTimeMillis();
        session.messageCount = 0;
        long sessionId = sessionDao.insert(session);

        // Auto-send character's first message if available
        CharacterEntity character = characterDao.getCharacterByIdSync(characterId);
        if (character != null && character.firstMessage != null && !character.firstMessage.isEmpty()) {
            MessageEntity firstMsg = new MessageEntity();
            firstMsg.sessionId = sessionId;
            firstMsg.role = "assistant";
            firstMsg.content = character.firstMessage;
            firstMsg.timestamp = System.currentTimeMillis();
            firstMsg.tokenCount = com.szyx.ai.util.TokenCounter.estimate(character.firstMessage);
            messageDao.insert(firstMsg);
            session.messageCount = 1;
            session.lastMessageAt = firstMsg.timestamp;
            sessionDao.update(session);
        }

        return sessionId;
    }

    public void deleteSession(ChatSessionEntity session) {
        executor.execute(() -> sessionDao.delete(session));
    }

    public void deleteSessionSync(ChatSessionEntity session) {
        sessionDao.delete(session);
    }

    public long duplicateSession(long sourceSessionId, String newName) {
        ChatSessionEntity source = sessionDao.getSessionByIdSync(sourceSessionId);
        ChatSessionEntity newSession = new ChatSessionEntity();
        newSession.characterId = source.characterId;
        newSession.sessionName = newName;
        newSession.modelCode = source.modelCode;
        newSession.createdAt = System.currentTimeMillis();
        newSession.lastMessageAt = System.currentTimeMillis();
        newSession.messageCount = 0;
        long newSessionId = sessionDao.insert(newSession);

        List<MessageEntity> sourceMessages = messageDao.getMessagesForSessionSync(sourceSessionId);
        java.util.ArrayList<MessageEntity> copies = new java.util.ArrayList<>(sourceMessages.size());
        for (MessageEntity src : sourceMessages) {
            MessageEntity copy = new MessageEntity();
            copy.sessionId = newSessionId;
            copy.role = src.role;
            copy.content = src.content;
            copy.imagePath = src.imagePath;
            copy.tokenCount = src.tokenCount;
            copy.isHidden = src.isHidden;
            copy.timestamp = src.timestamp;
            copies.add(copy);
        }
        messageDao.insertAll(copies);
        newSession.messageCount = sourceMessages.size();
        newSession.lastMessageAt = System.currentTimeMillis();
        sessionDao.update(newSession);
        return newSessionId;
    }

    public ChatSessionEntity getSessionByIdSync(long sessionId) {
        return sessionDao.getSessionByIdSync(sessionId);
    }

    // ---- Messages ----

    public LiveData<List<MessageEntity>> getMessagesForSession(long sessionId) {
        return messageDao.getMessagesForSession(sessionId);
    }

    public List<MessageEntity> getMessagesForSessionSync(long sessionId) {
        return messageDao.getMessagesForSessionSync(sessionId);
    }

    public List<MessageEntity> getNonSummaryMessagesSync(long sessionId) {
        return messageDao.getNonSummaryMessagesSync(sessionId);
    }

    public List<MessageEntity> getRecentNonSummaryMessagesSync(long sessionId, int limit) {
        return messageDao.getRecentNonSummaryMessagesSync(sessionId, limit);
    }

    public MessageEntity getLatestSummarySync(long sessionId) {
        return messageDao.getLatestSummarySync(sessionId);
    }

    public long insertMessage(MessageEntity message) {
        return messageDao.insert(message);
    }

    public void deleteMessage(MessageEntity message) {
        executor.execute(() -> messageDao.delete(message));
    }

    public void hideMessages(List<Long> messageIds) {
        executor.execute(() -> messageDao.hideMessages(messageIds));
    }

    public Integer getTotalTokenCount(long sessionId) {
        return messageDao.getTotalTokenCount(sessionId);
    }

    public int getActiveMessageCount(long sessionId) {
        return messageDao.getActiveMessageCount(sessionId);
    }

    public void updateSessionMeta(long sessionId) {
        executor.execute(() -> {
            ChatSessionEntity session = sessionDao.getSessionByIdSync(sessionId);
            if (session != null) {
                session.lastMessageAt = System.currentTimeMillis();
                session.messageCount = messageDao.getActiveMessageCount(sessionId);
                sessionDao.update(session);
            }
        });
    }

    public void incrementCondensation(long sessionId, int replacedCount) {
        sessionDao.incrementCondensation(sessionId, replacedCount);
    }

    public void updateSession(ChatSessionEntity session) {
        executor.execute(() -> sessionDao.update(session));
    }

    // ---- Character ----

    public CharacterEntity getCharacterByIdSync(long characterId) {
        return characterDao.getCharacterByIdSync(characterId);
    }

    // ---- Long Term Memory ----

    public long insertLTM(LongTermMemoryEntity memory) {
        return ltmDao.insert(memory);
    }

    public List<LongTermMemoryEntity> getLTMsForSession(long sessionId) {
        return ltmDao.getMemoriesForSession(sessionId);
    }

    public int getLTMCount(long sessionId) {
        return ltmDao.getMemoryCount(sessionId);
    }

    // ---- World Book ----

    public List<WorldBookEntity> getEnabledWorldBookEntries(long characterId) {
        return worldBookDao.getEnabledEntriesForCharacter(characterId);
    }

    public long insertWorldBookEntry(WorldBookEntity entry) {
        return worldBookDao.insert(entry);
    }

    public List<WorldBookEntity> getAllWorldBookEntries(long characterId) {
        return worldBookDao.getAllEntriesForCharacter(characterId);
    }

    public void deleteWorldBookEntry(WorldBookEntity entry) {
        executor.execute(() -> worldBookDao.delete(entry));
    }

    // ---- Numerical Values ----

    public List<NumericalValueEntity> getNumericalValues(long sessionId) {
        return numericalDao.getValuesForSession(sessionId);
    }

    public NumericalValueEntity getNumericalValueByName(long sessionId, String name) {
        return numericalDao.getValueByName(sessionId, name);
    }

    public void insertNumericalValue(NumericalValueEntity value) {
        executor.execute(() -> numericalDao.insert(value));
    }

    public void updateNumericalValue(NumericalValueEntity value) {
        executor.execute(() -> numericalDao.update(value));
    }

    // ---- Session model code ----

    public void updateSessionModelCode(long sessionId, String modelCode) {
        executor.execute(() -> {
            ChatSessionEntity session = sessionDao.getSessionByIdSync(sessionId);
            if (session != null) {
                session.modelCode = modelCode;
                sessionDao.update(session);
            }
        });
    }
}
