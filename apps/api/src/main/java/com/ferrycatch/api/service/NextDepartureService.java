package com.ferrycatch.api.service;

import com.ferrycatch.api.db.repo.StopTimeRepository;
import com.ferrycatch.api.db.repo.TripRepository;
import com.ferrycatch.api.dto.FerryDtos;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.OffsetDateTime;

@Service
public class NextDepartureService {
    private final TripRepository tripRepo;
    private final StopTimeRepository stopRepo;

    public NextDepartureService(TripRepository tripRepo, StopTimeRepository stopRepo) {
        this.tripRepo = tripRepo;
        this.stopRepo = stopRepo;
    }

    public FerryDtos.TripDto getNextTrip(String from, String to, String operatorOrNull) {
        var trips = tripRepo.findNextTrips(from, to, operatorOrNull, 2);
        if (trips.isEmpty()) return null;

        var t = trips.get(0);
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
    }

    public int minutesUntil(FerryDtos.TripDto trip) {
        var dep = OffsetDateTime.parse(trip.departureTime());
        return (int) Math.max(0, Duration.between(OffsetDateTime.now(), dep).toMinutes());
    }
}
