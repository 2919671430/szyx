package com.szyx.ai.viewmodel;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.szyx.ai.data.db.entity.CharacterEntity;
import com.szyx.ai.data.db.entity.ChatSessionEntity;
import com.szyx.ai.data.db.entity.MessageEntity;
import com.szyx.ai.data.db.entity.NumericalValueEntity;
import com.szyx.ai.data.repository.ChatRepository;
import com.szyx.ai.engine.InferenceEngine;
import com.szyx.ai.engine.ModelRouter;
import com.szyx.ai.engine.api.CustomApiEngine;
import com.szyx.ai.engine.api.DeepSeekApiEngine;
import com.szyx.ai.engine.api.XiaomiApiEngine;
import com.szyx.ai.engine.memory.MemoryManager;
import com.szyx.ai.engine.numerical.NumericalManager;
import com.szyx.ai.engine.prompt.LayeredPromptBuilder;
import com.szyx.ai.engine.llm.StreamingCallback;
import com.szyx.ai.util.TokenCounter;

import android.util.Log;
import com.szyx.ai.util.AppLog;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class ChatViewModel extends AndroidViewModel {

    private static final String TAG = "ChatViewModel";

    private final ChatRepository chatRepo;
    private final MemoryManager memoryManager;
    private final ExecutorService executor;
    private final ExecutorService ltmExecutor;
    private static final long GENERATION_TIMEOUT_SECONDS = 600;

    private final MutableLiveData<Long> sessionId = new MutableLiveData<>();
    private LiveData<List<MessageEntity>> messages;

    private final MutableLiveData<Boolean> isGenerating = new MutableLiveData<>(false);
    private final MutableLiveData<String> streamingText = new MutableLiveData<>("");
    private final MutableLiveData<String> errorText = new MutableLiveData<>();
    private final MutableLiveData<String> characterName = new MutableLiveData<>("");
    private final MutableLiveData<String> characterAvatarPath = new MutableLiveData<>(null);
    private final MutableLiveData<List<NumericalValueEntity>> numericalValues = new MutableLiveData<>();
    private final MutableLiveData<String> currentModelName = new MutableLiveData<>("--");
    private final MutableLiveData<List<String>> numericalHistory = new MutableLiveData<>();

    private final AtomicBoolean generatingGuard = new AtomicBoolean(false);
    private final StringBuilder streamingBuffer = new StringBuilder();
    private volatile InferenceEngine activeEngine;

    public ChatViewModel(@NonNull Application application) {
        super(application);
        chatRepo = new ChatRepository(application);
        memoryManager = new MemoryManager();
        executor = Executors.newSingleThreadExecutor();
        ltmExecutor = Executors.newSingleThreadExecutor();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executor.shutdownNow();
        ltmExecutor.shutdownNow();
    }

    public void setSession(long sessionId) {
        this.sessionId.setValue(sessionId);
        messages = chatRepo.getMessagesForSession(sessionId);
        loadCharacterInfo(sessionId);
        loadNumericalValues(sessionId);
        loadModelName(sessionId);
    }

    public LiveData<List<MessageEntity>> getMessages() { return messages; }
    public LiveData<Boolean> getIsGenerating() { return isGenerating; }
    public LiveData<String> getStreamingText() { return streamingText; }
    public LiveData<String> getErrorText() { return errorText; }
    public LiveData<String> getCharacterName() { return characterName; }
    public LiveData<String> getCharacterAvatarPath() { return characterAvatarPath; }
    public LiveData<List<NumericalValueEntity>> getNumericalValues() { return numericalValues; }
    public LiveData<String> getCurrentModelName() { return currentModelName; }
    public LiveData<List<String>> getNumericalHistory() { return numericalHistory; }

    private void loadCharacterInfo(long sid) {
        executor.execute(() -> {
            ChatSessionEntity session = chatRepo.getSessionByIdSync(sid);
            if (session == null) return;
            CharacterEntity character = chatRepo.getCharacterByIdSync(session.characterId);
            if (character != null) {
                characterName.postValue(character.name);
                characterAvatarPath.postValue(character.avatarPath);
            }
        });
    }

    private void loadNumericalValues(long sid) {
        executor.execute(() -> {
            List<NumericalValueEntity> values = chatRepo.getNumericalValues(sid);
            numericalValues.postValue(values);
        });
    }

    private void loadModelName(long sid) {
        executor.execute(() -> {
            ChatSessionEntity session = chatRepo.getSessionByIdSync(sid);
            if (session != null) {
                currentModelName.postValue(getModelDisplayName(session.modelCode));
            }
        });
    }

    private static String getModelDisplayName(String modelCode) {
        if (modelCode == null) return "小米";
        switch (modelCode) {
            case "xiaomi": return "小米";
            case "deepseek": return "DeepSeek";
            case "custom": return "自定义";
            default: return "小米";
        }
    }

    public void sendMessage(String userText) {
        if (!generatingGuard.compareAndSet(false, true)) return;

        final Long sid = sessionId.getValue();
        if (sid == null || sid <= 0) {
            generatingGuard.set(false);
            return;
        }

        executor.execute(() -> {
            MessageEntity userMsg = new MessageEntity();
            userMsg.sessionId = sid;
            userMsg.role = "user";
            userMsg.content = userText;
            userMsg.timestamp = System.currentTimeMillis();
            userMsg.tokenCount = TokenCounter.estimate(userText);
            chatRepo.insertMessage(userMsg);

            isGenerating.postValue(true);
            synchronized (streamingBuffer) {
                streamingBuffer.setLength(0);
            }
            streamingText.postValue("");

            try {
                ChatSessionEntity session = chatRepo.getSessionByIdSync(sid);
                if (session == null) return;

                CharacterEntity character = chatRepo.getCharacterByIdSync(session.characterId);
                if (character == null) return;

                SharedPreferences settings = getApplication().getSharedPreferences("settings", Context.MODE_PRIVATE);
                InferenceEngine engine = ModelRouter.getEngineForSession(getApplication(), session.modelCode);

                if (engine == null) {
                    errorText.postValue("没有可用的推理引擎");
                    return;
                }

                // Set API key for cloud engines
                if (engine.requiresApiKey()) {
                    String engineCode = engine.getCode();
                    if ("deepseek".equals(engineCode)) {
                        String apiKey = settings.getString("deepseek_api_key", "");
                        if (apiKey == null || apiKey.isEmpty()) {
                            errorText.postValue("请先在设置中配置 DeepSeek API Key");
                            return;
                        }
                        InferenceEngine.setDeepSeekApiKey(apiKey);
                    } else if ("custom".equals(engineCode)) {
                        String apiUrl = settings.getString("custom_api_url", "");
                        String modelName = settings.getString("custom_model_name", "");
                        String apiKey = settings.getString("custom_api_key", "");
                        if (apiUrl == null || apiUrl.isEmpty()) {
                            errorText.postValue("请先在设置中配置自定义 API 地址");
                            return;
                        }
                        CustomApiEngine.setApiUrl(apiUrl);
                        CustomApiEngine.setModelName(modelName);
                        CustomApiEngine.setApiKey(apiKey);
                    } else {
                        String apiKey = settings.getString("xiaomi_api_key", "");
                        if (apiKey == null || apiKey.isEmpty()) {
                            errorText.postValue("请先在设置中配置 API Key");
                            return;
                        }
                        InferenceEngine.setApiKey(apiKey);
                    }
                }

                // Check and extract LTM (async, non-blocking)
                if (character.ltmEnabled) {
                    final InferenceEngine ltmEngine = engine;
                    final String ltmApiKey;
                    String engineCode = engine.getCode();
                    if ("deepseek".equals(engineCode)) {
                        ltmApiKey = settings.getString("deepseek_api_key", "");
                    } else if ("custom".equals(engineCode)) {
                        ltmApiKey = settings.getString("custom_api_key", "");
                    } else {
                        ltmApiKey = settings.getString("xiaomi_api_key", "");
                    }
                    ltmExecutor.execute(() -> {
                        try {
                            memoryManager.checkAndExtractLTM(getApplication(), sid, chatRepo, ltmEngine, ltmApiKey);
                        } catch (Exception e) {
                            AppLog.w(TAG, "LTM 提取失败（不影响对话）: " + e.getMessage());
                        }
                    });
                }

                // Check and perform condensation if needed (before building prompt)
                com.szyx.ai.engine.condensation.CondensationManager condensationManager =
                        new com.szyx.ai.engine.condensation.CondensationManager();
                condensationManager.checkAndCondense(getApplication(), sid, chatRepo, engine);

                List<InferenceEngine.Message> promptMessages =
                        LayeredPromptBuilder.buildMessages(getApplication(), sid, chatRepo, userText);

                AppLog.i(TAG, "使用引擎: " + engine.getDisplayName() + "，消息层数: " + promptMessages.size());

                activeEngine = engine;
                final Thread watchdog = new Thread(() -> {
                    try {
                        Thread.sleep(GENERATION_TIMEOUT_SECONDS * 1000);
                        AppLog.w(TAG, "生成超时！已请求停止");
                        errorText.postValue("生成超时（" + GENERATION_TIMEOUT_SECONDS + "秒），已自动停止");
                        engine.requestStop();
                    } catch (InterruptedException ignored) {}
                });
                watchdog.setDaemon(true);
                watchdog.start();

                AppLog.i(TAG, "开始生成回复...");
                float temperature = settings.getFloat("temperature", 0.7f);
                String result = null;
                int maxRetries = 3;
                Exception lastError = null;

                for (int attempt = 1; attempt <= maxRetries; attempt++) {
                    try {
                        result = engine.generate(promptMessages, temperature, 512,
                                new StreamingCallback() {
                                    @Override
                                    public boolean onToken(String token) {
                                        synchronized (streamingBuffer) {
                                            streamingBuffer.append(token);
                                            streamingText.postValue(streamingBuffer.toString());
                                        }
                                        return true;
                                    }

                                    @Override
                                    public void onComplete(String fullText) {}

                                    @Override
                                    public void onError(Exception e) {
                                        Log.e(TAG, "Generation error", e);
                                        AppLog.e(TAG, "生成错误: " + e.getMessage());
                                    }
                                });
                        if (result != null && !result.isEmpty()) break;
                    } catch (Exception e) {
                        lastError = e;
                        if (attempt < maxRetries) {
                            long delay = (long) Math.pow(2, attempt) * 1000;
                            AppLog.w(TAG, "第 " + attempt + " 次请求失败，" + delay/1000 + "秒后重试: " + e.getMessage());
                            synchronized (streamingBuffer) {
                                streamingBuffer.setLength(0);
                                streamingText.postValue("重试中 (" + attempt + "/" + maxRetries + ")...");
                            }
                            try { Thread.sleep(delay); } catch (InterruptedException ignored) {}
                        }
                    }
                }

                watchdog.interrupt();

                if (result == null && lastError != null) {
                    errorText.postValue("请求失败（已重试" + maxRetries + "次）：" + lastError.getMessage());
                }

                AppLog.i(TAG, "生成完成，结果长度: " + (result != null ? result.length() : 0));

                if (result != null && !result.isEmpty() && character.numericalEnabled) {
                    List<NumericalManager.NumChange> changes =
                            NumericalManager.parseAndUpdate(sid, result, chatRepo);
                    if (!changes.isEmpty()) {
                        loadNumericalValues(sid);
                        List<String> history = new java.util.ArrayList<>();
                        for (NumericalManager.NumChange c : changes) {
                            history.add(c.name + ": " + c.oldValue + " → " + c.newValue +
                                    " (" + (c.delta >= 0 ? "+" : "") + c.delta + ")");
                        }
                        numericalHistory.postValue(history);
                    }
                }

                if (result != null && !result.isEmpty()) {
                    MessageEntity assistantMsg = new MessageEntity();
                    assistantMsg.sessionId = sid;
                    assistantMsg.role = "assistant";
                    assistantMsg.content = result;
                    assistantMsg.timestamp = System.currentTimeMillis();
                    assistantMsg.tokenCount = TokenCounter.estimate(result);
                    chatRepo.insertMessage(assistantMsg);
                    chatRepo.updateSessionMeta(sid);
                    AppLog.i(TAG, "助手消息已保存");
                } else {
                    AppLog.w(TAG, "生成结果为空");
                    errorText.postValue("生成结果为空");
                }

            } catch (Throwable e) {
                Log.e(TAG, "sendMessage error", e);
                AppLog.e(TAG, "发送消息错误: " + e.getClass().getSimpleName() + ": " + e.getMessage());
                errorText.postValue("生成错误：" + e.getClass().getSimpleName() + ": " + e.getMessage());
            } finally {
                isGenerating.postValue(false);
                generatingGuard.set(false);
                activeEngine = null;
                synchronized (streamingBuffer) {
                    streamingBuffer.setLength(0);
                }
                streamingText.postValue("");
            }
        });
    }

    public void stopGeneration() {
        InferenceEngine engine = activeEngine;
        if (engine != null) {
            engine.requestStop();
        }
    }

    public void switchModel(String modelCode) {
        Long sid = sessionId.getValue();
        if (sid != null && sid > 0) {
            chatRepo.updateSessionModelCode(sid, modelCode);
            currentModelName.postValue(getModelDisplayName(modelCode));
            AppLog.i(TAG, "已切换模型为: " + modelCode);
        }
    }

    public void deleteMessage(MessageEntity message) {
        executor.execute(() -> chatRepo.deleteMessage(message));
    }

    public void regenerateLastResponse() {
        Long sid = sessionId.getValue();
        if (sid == null || sid <= 0) return;
        executor.execute(() -> {
            List<MessageEntity> msgs = chatRepo.getNonSummaryMessagesSync(sid);
            if (msgs.isEmpty()) return;
            MessageEntity lastMsg = msgs.get(msgs.size() - 1);
            if ("assistant".equals(lastMsg.role)) {
                String lastUserText = null;
                for (int i = msgs.size() - 2; i >= 0; i--) {
                    if ("user".equals(msgs.get(i).role)) {
                        lastUserText = msgs.get(i).content;
                        break;
                    }
                }
                if (lastUserText != null) {
                    chatRepo.deleteMessage(lastMsg);
                    sendMessage(lastUserText);
                }
            }
        });
    }
}
