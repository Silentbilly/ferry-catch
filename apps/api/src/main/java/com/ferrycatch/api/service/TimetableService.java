package com.ferrycatch.api.service;

import com.ferrycatch.api.controllers.TimetableController;
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

    public TimetableService(TripRepository tripRepo, StopTimeRepository stopRepo) {
        this.tripRepo = tripRepo;
        this.stopRepo = stopRepo;
    }

    public TimetableController.TimetableResponse getTimetable(String from, String to, String operatorOrNull, LocalDate date) {
        var trips = tripRepo.findTripsForDate(from, to, operatorOrNull, date);

        var tripDtos = trips.stream().map(t -> {
            var stops = stopRepo.findByTripId(t.tripId()).stream()
                    .map(s -> new FerryDtos.StopDto(s.stopName(), s.stopSequence(), s.time().toString()))
                    .collect(Collectors.toList());

            return new FerryDtos.TripDto(
                    t.tripId(),
                    t.operator(),
                    t.from(),
                    t.to(),
                    t.departureTime().toString(),
                    t.arrivalTime().toString(),
                    stops
            );
        }).collect(Collectors.toList());

        // separate query to get route_id can be added.
        var routeDto = new TimetableController.RouteDto(null, from, to, operatorOrNull == null ? "" : operatorOrNull);

        return new TimetableController.TimetableResponse(routeDto, date.toString(), tripDtos);
    }
}
