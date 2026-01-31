package com.ferrycatch.api.controllers;

import com.ferrycatch.api.dto.FerryDtos.TripDto;
import com.ferrycatch.api.service.MockScheduleService;
import com.ferrycatch.api.service.TimetableService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@Tag(name = "Timetable API", description = "Timetable endpoints")
@RequestMapping("/api/v1")
public class TimetableController {

    private static final ZoneId ISTANBUL = ZoneId.of("Europe/Istanbul");

    private final TimetableService timetableService;

    public TimetableController(TimetableService timetableService) {
        this.timetableService = timetableService;
    }

    @Operation(
            operationId = "getTimetable",
            summary = "Get timetable for a route",
            description = "Returns trips for the day with full stop sequence for each trip."
    )
    @GetMapping("/timetable")
    public TimetableResponse timetable(
            @Parameter(description = "Start stop name/code", example = "Kınalıada", required = true)
            @RequestParam String from,
            @Parameter(description = "Destination stop name/code", example = "Bostancı", required = true)
            @RequestParam String to,
            @Parameter(description = "Ferry operator (optional)")
            @RequestParam(required = false) String operator,
            @Parameter(description = "Date (defaults to today) in yyyy-MM-dd", example = "2026-01-27")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        var d = (date == null) ? LocalDate.now(ISTANBUL) : date;
        return timetableService.getTimetable(from, to, operator, d);
    }

    public record RouteDto(UUID id, String from, String to, String operator) {}
    public record TimetableResponse(RouteDto route, String date, List<TripDto> trips) {}
}
