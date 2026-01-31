package com.ferrycatch.api.service;

import com.ferrycatch.api.controllers.TimetableController;
import com.ferrycatch.api.db.repo.RouteLookupRepository;
import com.ferrycatch.api.db.repo.StopTimeRepository;
import com.ferrycatch.api.db.repo.TripRepository;
import com.ferrycatch.api.dto.FerryDtos;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.stream.Collectors;

@Service
public class TimetableService {
    private final TripRepository tripRepo;
    private final StopTimeRepository stopRepo;
    private final RouteLookupRepository routeLookup;

    public TimetableService(TripRepository tripRepo, StopTimeRepository stopRepo, RouteLookupRepository routeLookup) {
        this.tripRepo = tripRepo;
        this.stopRepo = stopRepo;
        this.routeLookup = routeLookup;
    }

    public TimetableController.TimetableResponse getTimetable(String from, String to, String operatorOrNull, LocalDate date) {
        var routeId = routeLookup.findRouteId(from, to, operatorOrNull);

        var tripDtos = tripRepo.findTripsForDate(from, to, operatorOrNull, date).stream()
                .map(t -> {
                    var stops = stopRepo.findByTripId(t.tripId()).stream()
                            .map(s -> new FerryDtos.StopDto(s.stopName(), s.stopSequence(), s.time().toString()))
                            .toList();

                    return new FerryDtos.TripDto(
                            t.tripId(),
                            t.operator(),
                            t.from(),
                            t.to(),
                            t.departureTime().toString(),
                            t.arrivalTime().toString(),
                            stops
                    );
                })
                .toList();

        // operator: если не задан — берём из найденного маршрута/первого trip (чтобы фронту было удобно)
        String op = operatorOrNull;
        if (op == null || op.isBlank()) {
            op = tripDtos.stream().findFirst().map(FerryDtos.TripDto::operator).orElse("");
        }

        var routeDto = new TimetableController.RouteDto(routeId, from, to, op);
        return new TimetableController.TimetableResponse(routeDto, date.toString(), tripDtos);
    }
}
