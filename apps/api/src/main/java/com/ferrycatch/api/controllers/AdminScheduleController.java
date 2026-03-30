package com.ferrycatch.api.controllers;

import com.ferrycatch.api.importer.MaterializeService;
import com.ferrycatch.api.importer.ScheduleImportService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.SneakyThrows;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Admin Schedule API")
@RestController
@RequestMapping("/api/v1/admin/schedule")
public class AdminScheduleController {

    private final ScheduleImportService importService;
    private final MaterializeService materializeService;

    public AdminScheduleController(ScheduleImportService importService, MaterializeService materializeService) {
        this.importService = importService;
        this.materializeService = materializeService;
    }

    public record ImportRequest(
            String resource
    ) {
    }

    public record MaterializeRequest(
            Integer days
    ) {
    }

    public record ImportAndMaterializeRequest(
            String resource,
            Integer days
    ) {
    }

    @SneakyThrows
    @PostMapping("/import")
    public String importSchedule(@RequestBody(required = false) ImportRequest request) {
        String resource = (request == null || isBlank(request.resource()))
                ? "schedule/spring_2026"
                : request.resource().trim();

        importService.importFromClasspath(resource);
        return "OK";
    }

    @PostMapping("/materialize")
    public String materializeFromToday(@RequestBody(required = false) MaterializeRequest request) {
        int days = (request == null || request.days() == null) ? 7 : request.days();
        materializeService.refreshFromToday(days);
        return "OK";
    }

    @SneakyThrows
    @PostMapping("/import-and-materialize")
    public String importAndMaterialize(@RequestBody(required = false) ImportAndMaterializeRequest request) {
        String resource = (request == null || isBlank(request.resource()))
                ? "schedule/spring_2026"
                : request.resource().trim();

        int days = (request == null || request.days() == null) ? 90 : request.days();

        importService.importFromClasspath(resource);
        materializeService.refreshFromToday(days);
        return "OK";
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}