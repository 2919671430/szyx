package com.szyx.ai.engine.llm;

public interface StreamingCallback {
    boolean onToken(String token);
    void onComplete(String fullText);
    void onError(Exception e);
}
