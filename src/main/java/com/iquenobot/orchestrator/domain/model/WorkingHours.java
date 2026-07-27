package com.iquenobot.orchestrator.domain.model;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iquenobot.setting.domain.entity.Setting;
import lombok.extern.slf4j.Slf4j;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.EnumMap;
import java.util.Map;

/**
 * Domain model that evaluates whether the current time is within business hours.
 * Supports per-day schedules parsed from tenant Settings.
 */
@Slf4j
public class WorkingHours {

    private final Map<DayOfWeek, DaySchedule> schedule;
    private final ZoneId timezone;
    private final boolean enabled;
    private final String closedMessage;

    private WorkingHours(Builder builder) {
        this.schedule = builder.schedule;
        this.timezone = builder.timezone;
        this.enabled = builder.enabled;
        this.closedMessage = builder.closedMessage;
    }

    public boolean isEnabled() { return enabled; }
    public String getClosedMessage() { return closedMessage; }
    public Map<DayOfWeek, DaySchedule> getSchedule() { return schedule; }

    /**
     * Check if the current time in the tenant's timezone is within business hours.
     */
    public boolean isCurrentlyOpen() {
        if (!enabled) return true;

        ZonedDateTime now = ZonedDateTime.now(timezone);
        DayOfWeek today = now.getDayOfWeek();
        LocalTime currentTime = now.toLocalTime();

        DaySchedule daySchedule = schedule.get(today);
        if (daySchedule == null || !daySchedule.active) {
            return false;
        }

        return !currentTime.isBefore(daySchedule.start) && currentTime.isBefore(daySchedule.end);
    }

    /**
     * Build WorkingHours from Settings list and tenant timezone.
     */
    public static WorkingHours fromSettings(Map<String, Setting> settingsByKey, String timezone) {
        Builder builder = builder();
        builder.timezone(timezone != null ? ZoneId.of(timezone) : ZoneId.of("America/Guayaquil"));

        String enabledValue = getSettingValue(settingsByKey, "general", "business_hours_enabled", "true");
        builder.enabled(Boolean.parseBoolean(enabledValue));
        builder.closedMessage(getSettingValue(settingsByKey, "bot", "after_hours_message",
                "Estamos fuera de horario laboral. Te atenderemos en nuestro horario de atención."));

        String businessHoursJson = getSettingValue(settingsByKey, "general", "business_hours", null);
        if (businessHoursJson != null && !businessHoursJson.isBlank()) {
            builder.schedule(parseSchedule(businessHoursJson));
        } else {
            builder.schedule(defaultSchedule());
        }

        return builder.build();
    }

    public static Builder builder() {
        return new Builder();
    }

    private static Map<DayOfWeek, DaySchedule> parseSchedule(String json) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Map<String, String>> raw = mapper.readValue(json, new TypeReference<>() {});
            Map<DayOfWeek, DaySchedule> result = new EnumMap<>(DayOfWeek.class);

            Map<String, DayOfWeek> dayMapping = Map.of(
                    "monday", DayOfWeek.MONDAY,
                    "tuesday", DayOfWeek.TUESDAY,
                    "wednesday", DayOfWeek.WEDNESDAY,
                    "thursday", DayOfWeek.THURSDAY,
                    "friday", DayOfWeek.FRIDAY,
                    "saturday", DayOfWeek.SATURDAY,
                    "sunday", DayOfWeek.SUNDAY
            );

            for (Map.Entry<String, Map<String, String>> entry : raw.entrySet()) {
                DayOfWeek day = dayMapping.get(entry.getKey().toLowerCase());
                if (day != null && entry.getValue() != null) {
                    String startStr = entry.getValue().get("start");
                    String endStr = entry.getValue().get("end");
                    if (startStr != null && endStr != null) {
                        result.put(day, new DaySchedule(
                                LocalTime.parse(startStr),
                                LocalTime.parse(endStr),
                                true
                        ));
                    }
                }
            }

            return result.isEmpty() ? defaultSchedule() : result;
        } catch (Exception e) {
            log.warn("Failed to parse business hours JSON, using defaults: {}", e.getMessage());
            return defaultSchedule();
        }
    }

    private static Map<DayOfWeek, DaySchedule> defaultSchedule() {
        Map<DayOfWeek, DaySchedule> schedule = new EnumMap<>(DayOfWeek.class);
        DaySchedule standardDay = new DaySchedule(LocalTime.of(9, 0), LocalTime.of(18, 0), true);
        schedule.put(DayOfWeek.MONDAY, standardDay);
        schedule.put(DayOfWeek.TUESDAY, standardDay);
        schedule.put(DayOfWeek.WEDNESDAY, standardDay);
        schedule.put(DayOfWeek.THURSDAY, standardDay);
        schedule.put(DayOfWeek.FRIDAY, standardDay);
        return schedule;
    }

    private static String getSettingValue(Map<String, Setting> settings, String category, String key, String defaultValue) {
        Setting setting = settings.get(category + "." + key);
        return setting != null ? setting.getValue() : defaultValue;
    }

    public record DaySchedule(LocalTime start, LocalTime end, boolean active) {}

    public static class Builder {
        private Map<DayOfWeek, DaySchedule> schedule = defaultSchedule();
        private ZoneId timezone = ZoneId.of("America/Guayaquil");
        private boolean enabled = true;
        private String closedMessage = "Estamos fuera de horario laboral.";

        public Builder schedule(Map<DayOfWeek, DaySchedule> schedule) { this.schedule = schedule; return this; }
        public Builder timezone(ZoneId timezone) { this.timezone = timezone; return this; }
        public Builder enabled(boolean enabled) { this.enabled = enabled; return this; }
        public Builder closedMessage(String closedMessage) { this.closedMessage = closedMessage; return this; }

        public WorkingHours build() {
            return new WorkingHours(this);
        }
    }
}
