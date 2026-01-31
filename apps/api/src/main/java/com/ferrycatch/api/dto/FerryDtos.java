package com.ferrycatch.api.dto;

import java.util.List;
import java.util.UUID;

public final class FerryDtos {
    private FerryDtos() {}

    public record StopDto(
            String stopName,
            int sequence,
            String time
    ) {}

    public record TripDto(
            UUID tripId,
            String operator,
            String from,
            String to,
            String departureTime,
            String arrivalTime,
            List<StopDto> stops
    ) {}

    public record RouteDto(
            UUID id,
            String from,
            String to,
            String operator
    ) {}

    public record RouteWithNextDto(
            UUID id,
            String from,
            String to,
            String operator,
            Integer nextMinutesUntil,
            String nextDepartureTime
    ) {}
}
