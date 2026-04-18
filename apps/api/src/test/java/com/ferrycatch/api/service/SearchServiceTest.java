package com.ferrycatch.api.service;

import com.ferrycatch.api.db.record.StopTimeRow;
import com.ferrycatch.api.db.record.TripSegmentRow;
import com.ferrycatch.api.db.repo.SearchRepository;
import com.ferrycatch.api.db.repo.StopTimeRepository;
import com.ferrycatch.api.dto.FerryDtos;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SearchServiceTest {

    @Mock
    private SearchRepository searchRepo;

    @Mock
    private StopTimeRepository stopRepo;

    @InjectMocks
    private SearchService searchService;

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
    // findBest
    // =========================================================================

    @Test
    void given_noSegmentFound_whenFindBest_thenReturnsNull() {
        when(searchRepo.findBestNextSegment("PortA", "PortB", null))
                .thenReturn(null);

        SearchService.SearchResponse result = searchService.findBest("PortA", "PortB", null);

        assertThat(result).isNull();
        verify(searchRepo).findBestNextSegment("PortA", "PortB", null);
        verifyNoInteractions(stopRepo);
    }

    @Test
    void given_segmentFound_whenFindBest_thenSearchResponseHasCorrectTripFields() {
        UUID tripId = UUID.randomUUID();
        OffsetDateTime dept = OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(15);
        OffsetDateTime arr  = dept.plusMinutes(45);

        TripSegmentRow seg = segment(tripId, "BlueLine", dept, arr, 1, 4);

        when(searchRepo.findBestNextSegment("PortA", "PortB", "BlueLine"))
                .thenReturn(seg);
        when(stopRepo.findSegmentByTripId(tripId, 1, 4))
                .thenReturn(List.of(
                        stopTimeRow("PortA",    1, dept),
                        stopTimeRow("MidStop",  2, dept.plusMinutes(10)),
                        stopTimeRow("PortB",    4, arr)
                ));

        SearchService.SearchResponse result = searchService.findBest("PortA", "PortB", "BlueLine");

        assertThat(result).isNotNull();

        FerryDtos.TripDto trip = result.trip();
        assertThat(trip.tripId()).isEqualTo(tripId);
        assertThat(trip.operator()).isEqualTo("BlueLine");
        assertThat(trip.from()).isEqualTo("PortA");
        assertThat(trip.to()).isEqualTo("PortB");
        assertThat(trip.departureTime()).isEqualTo(dept.toString());
        assertThat(trip.arrivalTime()).isEqualTo(arr.toString());
        assertThat(trip.stops()).hasSize(3);

        // minutesUntil should reflect a future departure (~15 min)
        assertThat(result.minutesUntil()).isGreaterThan(0).isLessThanOrEqualTo(15);
    }

    @Test
    void given_pastDeparture_whenFindBest_thenMinutesUntilIsZero() {
        UUID tripId = UUID.randomUUID();
        OffsetDateTime dept = OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(20);
        OffsetDateTime arr  = dept.plusMinutes(30);

        TripSegmentRow seg = segment(tripId, "OldFerry", dept, arr, 1, 2);

        when(searchRepo.findBestNextSegment("PortA", "PortB", "OldFerry"))
                .thenReturn(seg);
        when(stopRepo.findSegmentByTripId(tripId, 1, 2))
                .thenReturn(List.of(
                        stopTimeRow("PortA", 1, dept),
                        stopTimeRow("PortB", 2, arr)
                ));

        SearchService.SearchResponse result = searchService.findBest("PortA", "PortB", "OldFerry");

        assertThat(result).isNotNull();
        assertThat(result.minutesUntil()).isEqualTo(0);
    }
}
