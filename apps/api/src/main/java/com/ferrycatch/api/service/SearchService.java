package com.ferrycatch.api.service;

import com.ferrycatch.api.db.repo.SearchRepository;
import com.ferrycatch.api.db.repo.StopTimeRepository;
import com.ferrycatch.api.dto.FerryDtos;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.OffsetDateTime;

@Service
public class SearchService {
    private final SearchRepository searchRepo;
    private final StopTimeRepository stopRepo;

    public SearchService(SearchRepository searchRepo, StopTimeRepository stopRepo) {
        this.searchRepo = searchRepo;
        this.stopRepo = stopRepo;
    }

    public SearchResponse findBest(String from, String to, String operatorOrNull) {
        var seg = searchRepo.findBestNextSegment(from, to, operatorOrNull);
        if (seg == null) return null;

        var stops = stopRepo.findSegmentByTripId(seg.tripId(), seg.fromSeq(), seg.toSeq()).stream()
                .map(s -> new FerryDtos.StopDto(s.stopName(), s.stopSequence(), s.time().toString()))
                .toList();

        var trip = new FerryDtos.TripDto(
                seg.tripId(),
                seg.operator(),
                from,
                to,
                seg.segmentDepartureTime().toString(),
                seg.segmentArrivalTime().toString(),
                stops
        );

        int minutesUntil = (int) Math.max(0, Duration.between(OffsetDateTime.now(), seg.segmentDepartureTime()).toMinutes());
        return new SearchResponse(trip, minutesUntil);
    }

    public record SearchResponse(FerryDtos.TripDto trip, int minutesUntil) {}
}
