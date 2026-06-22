package com.szyx.ai.engine.api;

import android.util.Log;

import com.szyx.ai.engine.InferenceEngine;
import com.szyx.ai.engine.llm.StreamingCallback;
import com.szyx.ai.util.AppLog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import okhttp3.sse.EventSources;

public class DeepSeekApiEngine implements InferenceEngine {

    private static final String TAG = "DeepSeekApiEngine";
    private static final String API_URL = "https://api.deepseek.com/v1/chat/completions";
    private static final String MODEL = "deepseek-v4-flash";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient client;
    private final AtomicReference<EventSource> activeEventSource = new AtomicReference<>();
    private volatile boolean stopped = false;

    public DeepSeekApiEngine() {
        client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(300, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    @Override
    public String getCode() { return "deepseek"; }

    @Override
    public String getDisplayName() { return "DeepSeek V4 Flash"; }

    @Override
    public boolean requiresApiKey() { return true; }

    @Override
    public String generate(List<Message> messages, float temperature, int maxTokens,
                           StreamingCallback callback) {
        stopped = false;
        StringBuilder result = new StringBuilder();

        try {
            JSONObject body = buildRequestBody(messages, temperature);
            Request request = new Request.Builder()
                    .url(API_URL)
                    .addHeader("Authorization", "Bearer " + getApiKey())
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(body.toString(), JSON))
                    .build();

            Log.i(TAG, "Calling DeepSeek API with " + messages.size() + " messages");
            AppLog.i(TAG, "调用 DeepSeek API，消息数: " + messages.size());

            final boolean[] completed = {false};
            final Exception[] error = {null};

            EventSourceListener listener = new EventSourceListener() {
                @Override
                public void onOpen(EventSource eventSource, Response response) {
                    Log.i(TAG, "SSE connection opened");
                }

                @Override
                public void onEvent(EventSource eventSource, String id, String type, String data) {
                    if (stopped) {
                        eventSource.cancel();
                        return;
                    }

                    if ("[DONE]".equals(data.trim())) {
                        completed[0] = true;
                        return;
                    }

                    try {
                        JSONObject json = new JSONObject(data);
                        JSONArray choices = json.getJSONArray("choices");
                        if (choices.length() > 0) {
                            JSONObject choice = choices.getJSONObject(0);
                            JSONObject delta = choice.optJSONObject("delta");
                            if (delta != null && delta.has("content")
                                    && !delta.isNull("content")) {
                                String content = delta.getString("content");
                                result.append(content);
                                boolean shouldContinue = callback.onToken(content);
                                if (!shouldContinue) {
                                    stopped = true;
                                    eventSource.cancel();
                                }
                            }
                        }
                    } catch (JSONException e) {
                        Log.w(TAG, "Failed to parse SSE chunk: " + data, e);
                    }
                }

                @Override
                public void onClosed(EventSource eventSource) {
                    completed[0] = true;
                }

                @Override
                public void onFailure(EventSource eventSource, Throwable t, Response response) {
                    if (t != null) {
                        error[0] = new Exception(t);
                    } else if (response != null && !response.isSuccessful()) {
                        error[0] = new Exception("HTTP " + response.code());
                    }
                    completed[0] = true;
                }
            };

            EventSource eventSource = EventSources.createFactory(client)
                    .newEventSource(request, listener);
            activeEventSource.set(eventSource);

            while (!completed[0] && !stopped) {
                Thread.sleep(100);
            }

            if (error[0] != null) {
                callback.onError(error[0]);
                return null;
            }

            String fullText = result.toString();
            callback.onComplete(fullText);
            return fullText;

        } catch (InterruptedException e) {
            Log.i(TAG, "Generation interrupted");
            return result.length() > 0 ? result.toString() : null;
        } catch (Exception e) {
            Log.e(TAG, "API call failed", e);
            AppLog.e(TAG, "API 调用失败: " + e.getMessage());
            callback.onError(e);
            return null;
        }
    }

    @Override
    public void requestStop() {
        stopped = true;
        EventSource es = activeEventSource.getAndSet(null);
        if (es != null) {
            es.cancel();
        }
    }

    private String getApiKey() {
        return apiKeyHolder;
    }

    private static volatile String apiKeyHolder = "";

    public static void setApiKey(String key) {
        apiKeyHolder = key != null ? key : "";
    }

    private JSONObject buildRequestBody(List<Message> messages, float temperature) throws JSONException {
        JSONObject body = new JSONObject();
        body.put("model", MODEL);
        body.put("stream", true);
        body.put("temperature", (double) temperature);

        JSONArray msgArray = new JSONArray();
        for (Message msg : messages) {
            JSONObject msgObj = new JSONObject();
            msgObj.put("role", msg.role);
            msgObj.put("content", msg.content);
            msgArray.put(msgObj);
        }
        body.put("messages", msgArray);

        return body;
    }
}
