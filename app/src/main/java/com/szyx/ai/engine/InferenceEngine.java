package com.szyx.ai.engine;

import com.szyx.ai.engine.llm.StreamingCallback;

import java.util.List;

/**
 * Unified inference interface for all LLM backends.
 * Implementations: XiaomiApiEngine, DeepSeekApiEngine, CustomApiEngine.
 */
public interface InferenceEngine {

    /** Unique code identifying this engine: "xiaomi", "deepseek", "custom", etc. */
    String getCode();

    /** Human-readable name for UI display */
    String getDisplayName();

    /**
     * Generate a response from a list of messages.
     * @param messages Conversation messages in order
     * @param temperature Sampling temperature
     * @param maxTokens Maximum tokens to generate
     * @param callback Streaming callback for token-by-token output
     * @return Full generated text, or null on failure
     */
    String generate(List<Message> messages, float temperature, int maxTokens,
                    StreamingCallback callback);

    /** Request the engine to stop generation */
    void requestStop();

    /** Whether this engine requires an API key */
    boolean requiresApiKey();

    /** Set the API key for Xiaomi engine */
    static void setApiKey(String key) {
        com.szyx.ai.engine.api.XiaomiApiEngine.setApiKey(key);
    }

    /** Set the API key for DeepSeek engine */
    static void setDeepSeekApiKey(String key) {
        com.szyx.ai.engine.api.DeepSeekApiEngine.setApiKey(key);
    }

    /** Simple message container */
    class Message {
        public final String role; // "system", "user", "assistant"
        public final String content;

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }
}
