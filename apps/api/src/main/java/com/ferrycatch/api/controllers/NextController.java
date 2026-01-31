package com.ferrycatch.api.controllers;

import com.ferrycatch.api.dto.FerryDtos.TripDto;
import com.ferrycatch.api.service.MockScheduleService;
import com.ferrycatch.api.service.NextDepartureService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@RestController
@Tag(name = "Next departure API", description = "Next departure lookup")
@RequestMapping("/api/v1")
public class NextController {

    private final NextDepartureService nextService;

    public NextController(NextDepartureService nextService) {
        this.nextService = nextService;
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
        TripDto trip = nextService.getNextTrip(from, to, operator);
        if (trip == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No upcoming trips"); // [web:1253]
        return new NextResponse(trip, nextService.minutesUntil(trip));
    }

    public record NextResponse(TripDto trip, int minutesUntil) {}
}
