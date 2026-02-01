package com.ferrycatch.api.controllers;

import com.ferrycatch.api.db.repo.StopsRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Stops API", description = "Available stops for UI selection")
@RestController
@RequestMapping("/api/v1")
public class StopsController {

    private final StopsRepository repo;

    public StopsController(StopsRepository repo) {
        this.repo = repo;
    }

    @GetMapping("/stops")
    public List<String> stops(@RequestParam(required = false) String operator) {
        return repo.listStops(operator);
    }
}
