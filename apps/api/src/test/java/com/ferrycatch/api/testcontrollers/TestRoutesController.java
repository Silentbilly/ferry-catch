package com.ferrycatch.api.testcontrollers;

import com.ferrycatch.api.dto.FerryDtos;
import com.ferrycatch.api.service.RouteService;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Profile({"local", "test"})
@RestController
@RequestMapping("/test/api/v1")
public class TestRoutesController {

    private final RouteService routeService;

    public TestRoutesController(RouteService routeService) {
        this.routeService = routeService;
    }

    @GetMapping("/routes/raw")
    public List<FerryDtos.RouteDto> rawRoutes() {
        return routeService.listAllRoutes();
    }
}
