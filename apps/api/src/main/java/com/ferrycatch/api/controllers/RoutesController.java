package com.ferrycatch.api.controllers;

import com.ferrycatch.api.dto.FerryDtos;
import com.ferrycatch.api.service.RouteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Routes API", description = "Route patterns (not segments)")
@RestController
@RequestMapping("/api/v1")
public class RoutesController {

    private final RouteService routeService;

    public RoutesController(RouteService routeService) {
        this.routeService = routeService;
    }

    @Operation(
            operationId = "listRoutePatterns",
            summary = "List route patterns",
            description = "Returns route patterns. Use /search for from/to segment lookup."
    )
    @GetMapping("/routes")
    public List<FerryDtos.RouteDto> routes() {
        return routeService.listAllRoutes();
    }
}
