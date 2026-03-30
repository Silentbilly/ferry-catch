package com.ferrycatch.api.controllers;

import com.ferrycatch.api.importer.MaterializeService;
import com.ferrycatch.api.importer.ScheduleImportService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.SneakyThrows;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

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

    @SneakyThrows
    @PostMapping("/import")
    public String importSchedule(@RequestParam(defaultValue = "schedule/spring_2026") String resource) {
        importService.importFromClasspath(resource);
        return "OK";
    }

    @PostMapping("/materialize")
    public String materialize(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(defaultValue = "90") int days
    ) {
        var s = (start == null) ? LocalDate.now() : start;
        materializeService.materialize(s, days);
        return "OK";
    }
}
