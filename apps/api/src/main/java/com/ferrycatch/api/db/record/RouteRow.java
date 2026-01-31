package com.ferrycatch.api.db.record;

import java.util.UUID;

public record RouteRow(
        UUID id,
        String from,
        String to,
        String operator
) {}
