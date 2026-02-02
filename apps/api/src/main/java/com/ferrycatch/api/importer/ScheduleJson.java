package com.ferrycatch.api.importer;

import java.util.List;

public record ScheduleJson(List<RouteJson> routes) {
    public record RouteJson(String from, String to, String operator, List<TripJson> trips) {}
    public record TripJson(String departure, String arrival, Flags flags, List<StopJson> stops) {}
    public record Flags(Boolean noSundaysAndHolidays, Boolean onlySundaysAndHolidays, String activeUntil) {}
    public record StopJson(int seq, String name, String time) {}
}
