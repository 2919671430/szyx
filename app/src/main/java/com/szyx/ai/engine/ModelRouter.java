package com.szyx.ai.engine;

import android.content.Context;
import android.content.SharedPreferences;

import com.szyx.ai.engine.api.CustomApiEngine;
import com.szyx.ai.engine.api.DeepSeekApiEngine;
import com.szyx.ai.engine.api.XiaomiApiEngine;
import com.szyx.ai.util.AppLog;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Routes inference requests to the appropriate engine based on model code.
 * Manages engine registration and lifecycle.
 */
public class ModelRouter {

    private static final String TAG = "ModelRouter";
    private static final Map<String, InferenceEngine> engines = new LinkedHashMap<>();

    static {
        engines.put("xiaomi", new XiaomiApiEngine());
        engines.put("deepseek", new DeepSeekApiEngine());
        engines.put("custom", new CustomApiEngine());
    }

    public static InferenceEngine getEngine(String modelCode) {
        if (modelCode == null || modelCode.isEmpty()) {
            modelCode = "xiaomi";
        }
        return engines.get(modelCode);
    }

    /** Get the default engine based on settings */
    public static InferenceEngine getDefaultEngine(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
        boolean useXiaomi = prefs.getBoolean("use_xiaomi_api", false);
        String apiKey = prefs.getString("xiaomi_api_key", "");

        if (useXiaomi && apiKey != null && !apiKey.isEmpty()) {
            return engines.get("xiaomi");
        }

        boolean useDeepSeek = prefs.getBoolean("use_deepseek_api", false);
        String deepSeekKey = prefs.getString("deepseek_api_key", "");
        if (useDeepSeek && deepSeekKey != null && !deepSeekKey.isEmpty()) {
            return engines.get("deepseek");
        }

        boolean useCustom = prefs.getBoolean("use_custom_api", false);
        String customKey = prefs.getString("custom_api_key", "");
        if (useCustom && customKey != null && !customKey.isEmpty()) {
            return engines.get("custom");
        }

        return engines.get("xiaomi");
    }

    public static Map<String, InferenceEngine> getAllEngines() {
        return engines;
    }

    public static InferenceEngine getEngineForSession(Context context, String sessionModelCode) {
        if (sessionModelCode != null && !sessionModelCode.isEmpty()) {
            InferenceEngine engine = engines.get(sessionModelCode);
            if (engine != null) return engine;
        }
        return getDefaultEngine(context);
    }
}
