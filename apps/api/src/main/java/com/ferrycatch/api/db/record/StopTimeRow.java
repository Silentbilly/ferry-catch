package com.ferrycatch.api.db.record;

import java.time.OffsetDateTime;

public record StopTimeRow(
        int stopSequence,
        String stopName,
        OffsetDateTime time
) {}
