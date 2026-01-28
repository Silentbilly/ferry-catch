package com.ferrycatch.api.controllers;

import com.ferrycatch.api.dto.FerryDtos.TripDto;
import com.ferrycatch.api.service.MockScheduleService;
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

@RestController
@Tag(name = "Timetable API", description = "Timetable endpoints")
@RequestMapping("/api/v1")
public class TimetableController {

    private static final ZoneId ISTANBUL = ZoneId.of("Europe/Istanbul");
    private final MockScheduleService scheduleService;

    public TimetableController(MockScheduleService scheduleService) {
        this.scheduleService = scheduleService;
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

        var dayStart = ZonedDateTime.now(ISTANBUL)
                .withYear(d.getYear()).withMonth(d.getMonthValue()).withDayOfMonth(d.getDayOfMonth())
                .withHour(6).withMinute(30).withSecond(0).withNano(0);

        List<TripDto> trips = scheduleService.timetableTrips(from, to, operator, dayStart);

        return new TimetableResponse(
                new RouteDto("00000000-0000-0000-0000-000000000001", from, to,
                        operator == null || operator.isBlank() ? "Mavi Marmara" : operator),
                d.toString(),
                trips
        );
    }

    public record RouteDto(String id, String from, String to, String operator) {}
    public record TimetableResponse(RouteDto route, String date, List<TripDto> trips) {}
}
