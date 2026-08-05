package com.iquenobot.orchestrator.domain.model;

import com.iquenobot.setting.domain.entity.Setting;

import java.util.Map;

/**
 * Domain model that aggregates bot-related configuration from tenant Settings.
 * Used by DecisionEngine strategies to determine bot behavior.
 */
public class BotConfiguration {

    private final boolean enabled;
    private final boolean autoReply;
    private final String aiProvider;
    private final String systemPrompt;
    private final double temperature;
    private final boolean humanHandoffEnabled;
    private final String fallbackMessage;
    private final boolean botResumeEnabled;
    private final int botResumeDelayMinutes;
    private final int maxHumanIdleMinutes;
    private final Map<String, Object> extraSettings;

    private BotConfiguration(Builder builder) {
        this.enabled = builder.enabled;
        this.autoReply = builder.autoReply;
        this.aiProvider = builder.aiProvider;
        this.systemPrompt = builder.systemPrompt;
        this.temperature = builder.temperature;
        this.humanHandoffEnabled = builder.humanHandoffEnabled;
        this.fallbackMessage = builder.fallbackMessage;
        this.botResumeEnabled = builder.botResumeEnabled;
        this.botResumeDelayMinutes = builder.botResumeDelayMinutes;
        this.maxHumanIdleMinutes = builder.maxHumanIdleMinutes;
        this.extraSettings = builder.extraSettings;
    }

    public boolean isEnabled() { return enabled; }
    public boolean isAutoReply() { return autoReply; }
    public String getAiProvider() { return aiProvider; }
    public String getSystemPrompt() { return systemPrompt; }
    public double getTemperature() { return temperature; }
    public boolean isHumanHandoffEnabled() { return humanHandoffEnabled; }
    public String getFallbackMessage() { return fallbackMessage; }
    public boolean isBotResumeEnabled() { return botResumeEnabled; }
    public int getBotResumeDelayMinutes() { return botResumeDelayMinutes; }
    public int getMaxHumanIdleMinutes() { return maxHumanIdleMinutes; }
    public Map<String, Object> getExtraSettings() { return extraSettings; }

    public boolean isAiAvailable() {
        return enabled && aiProvider != null && !aiProvider.isBlank() && !"NONE".equalsIgnoreCase(aiProvider);
    }

    /**
     * Build BotConfiguration from a list of Setting entities.
     */
    public static BotConfiguration fromSettings(Map<String, Setting> settingsByKey) {
        Builder builder = builder();

        builder.enabled(getBooleanSetting(settingsByKey, "ai", "enabled", true));
        builder.autoReply(getBooleanSetting(settingsByKey, "ai", "auto_reply", false));
        builder.aiProvider(getStringSetting(settingsByKey, "ai", "provider", "NONE"));
        builder.systemPrompt(getStringSetting(settingsByKey, "ai", "system_prompt", ""));
        builder.temperature(getDoubleSetting(settingsByKey, "ai", "temperature", 0.7));
        builder.humanHandoffEnabled(getBooleanSetting(settingsByKey, "bot", "human_handoff", true));
        builder.fallbackMessage(getStringSetting(settingsByKey, "bot", "fallback_message",
                "Lo siento, voy a conectarte con un agente humano."));
        builder.botResumeEnabled(getBooleanSetting(settingsByKey, "bot", "bot_resume_enabled", true));
        builder.botResumeDelayMinutes(getIntSetting(settingsByKey, "bot", "bot_resume_delay_minutes", 2));
        builder.maxHumanIdleMinutes(getIntSetting(settingsByKey, "bot", "max_human_idle_minutes", 15));

        return builder.build();
    }

    /**
     * Returns a default configuration when no settings exist.
     */
    public static BotConfiguration defaults() {
        return builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    private static String getStringSetting(Map<String, Setting> settings, String category, String key, String defaultValue) {
        Setting setting = settings.get(category + "." + key);
        return setting != null ? setting.getValue() : defaultValue;
    }

    private static boolean getBooleanSetting(Map<String, Setting> settings, String category, String key, boolean defaultValue) {
        Setting setting = settings.get(category + "." + key);
        if (setting == null) return defaultValue;
        return Boolean.parseBoolean(setting.getValue());
    }

    private static double getDoubleSetting(Map<String, Setting> settings, String category, String key, double defaultValue) {
        Setting setting = settings.get(category + "." + key);
        if (setting == null) return defaultValue;
        try {
            return Double.parseDouble(setting.getValue());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static int getIntSetting(Map<String, Setting> settings, String category, String key, int defaultValue) {
        Setting setting = settings.get(category + "." + key);
        if (setting == null) return defaultValue;
        try {
            return Integer.parseInt(setting.getValue().trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static class Builder {
        private boolean enabled = false;
        private boolean autoReply = false;
        private String aiProvider = "NONE";
        private String systemPrompt = "";
        private double temperature = 0.7;
        private boolean humanHandoffEnabled = true;
        private String fallbackMessage = "Lo siento, voy a conectarte con un agente humano.";
        private boolean botResumeEnabled = true;
        private int botResumeDelayMinutes = 2;
        private int maxHumanIdleMinutes = 15;
        private Map<String, Object> extraSettings = Map.of();

        public Builder enabled(boolean enabled) { this.enabled = enabled; return this; }
        public Builder autoReply(boolean autoReply) { this.autoReply = autoReply; return this; }
        public Builder aiProvider(String aiProvider) { this.aiProvider = aiProvider; return this; }
        public Builder systemPrompt(String systemPrompt) { this.systemPrompt = systemPrompt; return this; }
        public Builder temperature(double temperature) { this.temperature = temperature; return this; }
        public Builder humanHandoffEnabled(boolean humanHandoffEnabled) { this.humanHandoffEnabled = humanHandoffEnabled; return this; }
        public Builder fallbackMessage(String fallbackMessage) { this.fallbackMessage = fallbackMessage; return this; }
        public Builder botResumeEnabled(boolean botResumeEnabled) { this.botResumeEnabled = botResumeEnabled; return this; }
        public Builder botResumeDelayMinutes(int botResumeDelayMinutes) { this.botResumeDelayMinutes = botResumeDelayMinutes; return this; }
        public Builder maxHumanIdleMinutes(int maxHumanIdleMinutes) { this.maxHumanIdleMinutes = maxHumanIdleMinutes; return this; }
        public Builder extraSettings(Map<String, Object> extraSettings) { this.extraSettings = extraSettings; return this; }

        public BotConfiguration build() {
            return new BotConfiguration(this);
        }
    }
}
