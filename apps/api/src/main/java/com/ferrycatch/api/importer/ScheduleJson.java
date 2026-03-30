package com.ferrycatch.api.importer;

import java.util.List;

public record ScheduleJson(
        String operator,
        String from,
        String to,
        String variant,
        ServiceJson service,
        List<TripJson> tripTemplates
) {
    public record ServiceJson(String season, String validFrom, String validTo) {}
    public record TripJson(String departure, String arrival, Flags flags, List<StopJson> stops) {}
    public record Flags(Boolean noSundaysAndHolidays, Boolean onlySundaysAndHolidays, String activeUntil) {}
    public record StopJson(int seq, String name, String time) {}
}