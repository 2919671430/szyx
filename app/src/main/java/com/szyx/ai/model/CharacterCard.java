package com.szyx.ai.model;

public class CharacterCard {
    public String name;
    public String description;
    public String personality;
    public String systemPrompt;
    public String firstMessage;
    public String[] tags;
    public String avatarPath;

    public CharacterCard() {}

    public CharacterCard(String name, String systemPrompt) {
        this.name = name;
        this.systemPrompt = systemPrompt;
    }
}
