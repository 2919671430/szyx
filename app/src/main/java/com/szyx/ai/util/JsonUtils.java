package com.szyx.ai.util;

import com.szyx.ai.data.db.entity.CharacterEntity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class JsonUtils {

    public static String exportCharacter(CharacterEntity character) {
        try {
            JSONObject root = new JSONObject();
            root.put("spec", "szyx_card_v1");

            JSONObject data = new JSONObject();
            data.put("name", character.name);
            data.put("description", character.description != null ? character.description : "");
            data.put("personality", character.personality != null ? character.personality : "");
            data.put("systemPrompt", character.systemPrompt);
            data.put("firstMessage", character.firstMessage != null ? character.firstMessage : "");
            data.put("tags", character.tags != null ? character.tags : "");

            // SillyTavern compatible fields
            data.put("mes_example_prompt", character.systemPrompt);
            data.put("first_mes", character.firstMessage != null ? character.firstMessage : "");
            data.put("world_setting", character.worldSetting != null ? character.worldSetting : "");
            data.put("output_style", character.outputStyle != null ? character.outputStyle : "");
            data.put("creator_notes", "Exported from SZYX AI");

            // Tags as array for SillyTavern compatibility
            if (character.tags != null && !character.tags.isEmpty()) {
                String[] tagArr = character.tags.split(",");
                JSONArray tagsJson = new JSONArray();
                for (String tag : tagArr) {
                    tagsJson.put(tag.trim());
                }
                data.put("tags_array", tagsJson);
            }

            JSONObject meta = new JSONObject();
            meta.put("version", 1);
            meta.put("createdAt", character.createdAt);

            root.put("data", data);
            root.put("metadata", meta);

            return root.toString(2);
        } catch (JSONException e) {
            return null;
        }
    }

    public static CharacterEntity importCharacter(String json) throws JSONException {
        JSONObject root = new JSONObject(json);
        JSONObject data = root.optJSONObject("data");
        if (data == null) data = root;

        CharacterEntity c = new CharacterEntity();
        c.name = data.getString("name");
        c.description = data.optString("description", "");
        c.personality = data.optString("personality", "");
        c.systemPrompt = data.optString("systemPrompt", data.optString("mes_example_prompt", ""));
        c.firstMessage = data.optString("firstMessage", data.optString("first_mes", ""));

        // Handle tags as array or string
        Object tagsObj = data.opt("tags");
        if (tagsObj instanceof JSONArray) {
            JSONArray tagsArr = (JSONArray) tagsObj;
            List<String> tagList = new ArrayList<>();
            for (int i = 0; i < tagsArr.length(); i++) {
                tagList.add(tagsArr.getString(i));
            }
            c.tags = String.join(",", tagList);
        } else {
            c.tags = data.optString("tags", "");
        }

        c.createdAt = System.currentTimeMillis();
        c.updatedAt = System.currentTimeMillis();
        c.rawJson = json;
        return c;
    }
}
