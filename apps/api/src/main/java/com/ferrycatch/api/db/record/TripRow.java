package com.ferrycatch.api.db.record;

import java.time.OffsetDateTime;
import java.util.UUID;

public record TripRow(
        UUID tripId,
        String operator,
        String from,
        String to,
        OffsetDateTime departureTime,
        OffsetDateTime arrivalTime
) {
}
