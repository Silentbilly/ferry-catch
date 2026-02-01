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

    /**
     * Returns TripDto for the requested segment (from->to):
     * - departureTime/arrivalTime are segment times (at from/to stops)
     * - stops are only between from and to (inclusive)
     */
    public FerryDtos.TripDto getNextTrip(String from, String to, String operatorOrNull) {
        var segs = tripRepo.findNextTripSegments(from, to, operatorOrNull, 2);
        if (segs.isEmpty()) return null;

        var s = segs.get(0);

        var segStops = stopRepo.findSegmentByTripId(s.tripId(), s.fromSeq(), s.toSeq()).stream()
                .map(st -> new FerryDtos.StopDto(st.stopName(), st.stopSequence(), st.time().toString()))
                .toList();

        // Note: TripDto.from/to = user's selected from/to (segment), not route endpoints.
        return new FerryDtos.TripDto(
                s.tripId(),
                s.operator(),
                from,
                to,
                s.segmentDepartureTime().toString(),
                s.segmentArrivalTime().toString(),
                segStops
        );
    }

    public int minutesUntil(FerryDtos.TripDto trip) {
        var dep = OffsetDateTime.parse(trip.departureTime());
        return (int) Math.max(0, Duration.between(OffsetDateTime.now(), dep).toMinutes());
    }
}
