package com.ferrycatch.api.db.record;

import java.time.OffsetDateTime;
import java.util.UUID;

public record TripSegmentRow(
        UUID tripId,
        UUID routeId,
        String operator,
        OffsetDateTime segmentDepartureTime,
        OffsetDateTime segmentArrivalTime,
        int fromSeq,
        int toSeq
) {}
