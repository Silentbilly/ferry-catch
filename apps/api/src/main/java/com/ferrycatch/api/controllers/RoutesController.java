package com.ferrycatch.api.controllers;

import com.ferrycatch.api.dto.FerryDtos.RouteDto;
import com.ferrycatch.api.service.MockScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Routes API", description = "Available routes")
@RestController
@RequestMapping("/api/v1")
public class RoutesController {

    private final MockScheduleService scheduleService;

    public RoutesController(MockScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @Operation(
            operationId = "listRoutes",
            summary = "List available routes",
            description = "Returns available routes. Optional filters: from, to, operator."
    )
    @GetMapping("/routes")
    public List<RouteDto> routes(
            @Parameter(description = "Start stop name/code (optional)", example = "Kınalıada")
            @RequestParam(required = false) String from,

            @Parameter(description = "Destination stop name/code (optional)", example = "Bostancı")
            @RequestParam(required = false) String to,

            @Parameter(description = "Ferry operator (optional)")
            @RequestParam(required = false) String operator
    ) {
        return scheduleService.listRoutes(from, to, operator);
    }
}
