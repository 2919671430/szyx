package com.szyx.ai.model;

public class ChatMessage {
    public static final int TYPE_USER = 0;
    public static final int TYPE_ASSISTANT = 1;
    public static final int TYPE_SUMMARY = 2;

    public long id;
    public int type;
    public String content;
    public long timestamp;
    public boolean isStreaming;

    public ChatMessage(int type, String content) {
        this.type = type;
        this.content = content;
        this.timestamp = System.currentTimeMillis();
    }
}
