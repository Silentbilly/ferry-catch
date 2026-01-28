package com.ferrycatch.api.controllers;

import com.ferrycatch.api.dto.FerryDtos.TripDto;
import com.ferrycatch.api.service.MockScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@RestController
@Tag(name = "Next departure API", description = "Next departure lookup")
@RequestMapping("/api/v1")
public class NextController {

    private static final ZoneId ISTANBUL = ZoneId.of("Europe/Istanbul");
    private final MockScheduleService scheduleService;

    public NextController(MockScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @Operation(
            operationId = "getNextDeparture",
            summary = "Get next departure for a route",
            description = "Returns the next trip for given from/to (and optional operator), including full stop sequence."
    )
    @GetMapping("/next")
    public NextResponse next(
            @Parameter(description = "Start stop name/code", example = "Kınalıada", required = true)
            @RequestParam String from,
            @Parameter(description = "Destination stop name/code", example = "Bostancı", required = true)
            @RequestParam String to,
            @Parameter(description = "Ferry operator (e.g., Mavi Marmara, Şehir Hatları)")
            @RequestParam(required = false) String operator
    ) {
        var now = ZonedDateTime.now(ISTANBUL);
        TripDto trip = scheduleService.nextTrip(from, to, operator, now);
        var dep = ZonedDateTime.parse(trip.departureTime());

        return new NextResponse(trip, (int) Duration.between(now, dep).toMinutes());
    }

    public record NextResponse(TripDto trip, int minutesUntil) {}
}
