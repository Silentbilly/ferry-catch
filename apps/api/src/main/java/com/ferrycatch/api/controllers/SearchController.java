package com.ferrycatch.api.controllers;

import com.ferrycatch.api.service.SearchService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Tag(name = "Search API", description = "Find best next segment for arbitrary from/to")
@RestController
@RequestMapping("/api/v1")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping("/search")
    public SearchService.SearchResponse search(
            @Parameter(example = "Kınalıada") @RequestParam String from,
            @Parameter(example = "Bostancı") @RequestParam String to,
            @RequestParam(required = false) String operator
    ) {
        var res = searchService.findBest(from, to, operator);
        if (res == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No upcoming trips");
        return res;
    }
}
