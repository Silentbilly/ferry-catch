package com.ferrycatch.api.service;

import com.ferrycatch.api.db.record.StopTimeRow;
import com.ferrycatch.api.db.record.TripSegmentRow;
import com.ferrycatch.api.db.repo.StopTimeRepository;
import com.ferrycatch.api.db.repo.TripRepository;
import com.ferrycatch.api.dto.FerryDtos;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NextDepartureServiceTest {

    @Mock
    private TripRepository tripRepo;

    @Mock
    private StopTimeRepository stopRepo;

    @InjectMocks
    private NextDepartureService nextDepartureService;

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private TripSegmentRow segment(UUID tripId, String operator,
                                   OffsetDateTime dept, OffsetDateTime arr,
                                   int fromSeq, int toSeq) {
        return new TripSegmentRow(tripId, UUID.randomUUID(), operator, dept, arr, fromSeq, toSeq);
    }

    private StopTimeRow stopTimeRow(String name, int seq, OffsetDateTime time) {
        return new StopTimeRow(seq, name, time);
    }

    // =========================================================================
    // getNextTrip
    // =========================================================================

    @Test
    void given_noTripsFound_whenGetNextTrip_thenReturnsNull() {
        when(tripRepo.findNextTripSegments("PortA", "PortB", null, 2))
                .thenReturn(Collections.emptyList());

        FerryDtos.TripDto result = nextDepartureService.getNextTrip("PortA", "PortB", null);

        assertThat(result).isNull();
        verify(tripRepo).findNextTripSegments("PortA", "PortB", null, 2);
        verifyNoInteractions(stopRepo);
    }

    @Test
    void given_tripFound_whenGetNextTrip_thenReturnsTripDtoWithCorrectFields() {
        UUID tripId = UUID.randomUUID();
        OffsetDateTime dept = OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(10);
        OffsetDateTime arr  = dept.plusMinutes(30);

        TripSegmentRow seg = segment(tripId, "FastFerry", dept, arr, 1, 3);

        when(tripRepo.findNextTripSegments("PortA", "PortB", "FastFerry", 2))
                .thenReturn(List.of(seg));
        when(stopRepo.findSegmentByTripId(tripId, 1, 3))
                .thenReturn(List.of(
                        stopTimeRow("PortA", 1, dept),
                        stopTimeRow("PortB", 3, arr)
                ));

        FerryDtos.TripDto result = nextDepartureService.getNextTrip("PortA", "PortB", "FastFerry");

        assertThat(result).isNotNull();
        assertThat(result.tripId()).isEqualTo(tripId);
        assertThat(result.operator()).isEqualTo("FastFerry");
        assertThat(result.from()).isEqualTo("PortA");
        assertThat(result.to()).isEqualTo("PortB");
        assertThat(result.departureTime()).isEqualTo(dept.toString());
        assertThat(result.arrivalTime()).isEqualTo(arr.toString());
        assertThat(result.stops()).hasSize(2);
        assertThat(result.stops().get(0).stopName()).isEqualTo("PortA");
        assertThat(result.stops().get(1).stopName()).isEqualTo("PortB");

        verify(stopRepo).findSegmentByTripId(tripId, 1, 3);
    }

    @Test
    void given_nullOperator_whenGetNextTrip_thenPassesNullToRepository() {
        UUID tripId = UUID.randomUUID();
        OffsetDateTime dept = OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(5);
        OffsetDateTime arr  = dept.plusMinutes(25);

        TripSegmentRow seg = segment(tripId, "SeaLine", dept, arr, 1, 2);

        when(tripRepo.findNextTripSegments("PortA", "PortB", null, 2))
                .thenReturn(List.of(seg));
        when(stopRepo.findSegmentByTripId(tripId, 1, 2))
                .thenReturn(List.of(
                        stopTimeRow("PortA", 1, dept),
                        stopTimeRow("PortB", 2, arr)
                ));

        FerryDtos.TripDto result = nextDepartureService.getNextTrip("PortA", "PortB", null);

        assertThat(result).isNotNull();
        assertThat(result.operator()).isEqualTo("SeaLine");
        // null was forwarded as-is
        verify(tripRepo).findNextTripSegments("PortA", "PortB", null, 2);
    }

    // =========================================================================
    // minutesUntil
    // =========================================================================

    @Test
    void given_futureDeparture_whenMinutesUntil_thenReturnsPositiveMinutes() {
        OffsetDateTime future = OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(5);
        FerryDtos.TripDto trip = new FerryDtos.TripDto(
                UUID.randomUUID(), "FastFerry", "PortA", "PortB",
                future.toString(),
                future.plusMinutes(30).toString(),
                Collections.emptyList()
        );

        int minutes = nextDepartureService.minutesUntil(trip);

        // 5 minutes ahead; truncation may give 4 in the last sub-minute, so allow 1..5
        assertThat(minutes).isGreaterThan(0).isLessThanOrEqualTo(5);
    }

    @Test
    void given_pastDeparture_whenMinutesUntil_thenReturnsZero() {
        OffsetDateTime past = OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(10);
        FerryDtos.TripDto trip = new FerryDtos.TripDto(
                UUID.randomUUID(), "FastFerry", "PortA", "PortB",
                past.toString(),
                past.plusMinutes(30).toString(),
                Collections.emptyList()
        );

        int minutes = nextDepartureService.minutesUntil(trip);

        assertThat(minutes).isEqualTo(0);
    }
}
