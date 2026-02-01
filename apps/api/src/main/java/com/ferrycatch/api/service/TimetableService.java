package com.ferrycatch.api.service;

import com.ferrycatch.api.controllers.TimetableController;
import com.ferrycatch.api.db.repo.StopTimeRepository;
import com.ferrycatch.api.db.repo.TripRepository;
import com.ferrycatch.api.dto.FerryDtos;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class TimetableService {
    private final TripRepository tripRepo;
    private final StopTimeRepository stopRepo;

    public TimetableService(TripRepository tripRepo, StopTimeRepository stopRepo) {
        this.tripRepo = tripRepo;
        this.stopRepo = stopRepo;
    }

    public TimetableController.TimetableResponse getTimetable(String from, String to, String operatorOrNull, LocalDate date) {
        var segs = tripRepo.findTripSegmentsForDate(from, to, operatorOrNull, date);

        var tripDtos = segs.stream()
                .map(s -> {
                    var segStops = stopRepo.findSegmentByTripId(s.tripId(), s.fromSeq(), s.toSeq()).stream()
                            .map(st -> new FerryDtos.StopDto(st.stopName(), st.stopSequence(), st.time().toString()))
                            .toList();

                    return new FerryDtos.TripDto(
                            s.tripId(),
                            s.operator(),
                            from,
                            to,
                            s.segmentDepartureTime().toString(),
                            s.segmentArrivalTime().toString(),
                            segStops
                    );
                })
                .toList();

        String op = operatorOrNull;
        if (op == null || op.isBlank()) {
            op = tripDtos.stream().findFirst().map(FerryDtos.TripDto::operator).orElse("");
        }

        // route.id теперь не обязателен; можно оставить null или сгенерировать позже как “pattern id”
        var routeDto = new TimetableController.RouteDto(null, from, to, op);

        return new TimetableController.TimetableResponse(routeDto, date.toString(), tripDtos);
    }
}
