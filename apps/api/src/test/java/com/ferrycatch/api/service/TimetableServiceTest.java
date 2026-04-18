package com.ferrycatch.api.service;

import com.ferrycatch.api.controllers.TimetableController;
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

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TimetableServiceTest {

    @Mock
    private TripRepository tripRepo;

    @Mock
    private StopTimeRepository stopRepo;

    @InjectMocks
    private TimetableService timetableService;

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static final LocalDate TEST_DATE = LocalDate.of(2026, 4, 20);

    private TripSegmentRow segment(UUID tripId, String operator,
                                   OffsetDateTime dept, OffsetDateTime arr,
                                   int fromSeq, int toSeq) {
        return new TripSegmentRow(tripId, UUID.randomUUID(), operator, dept, arr, fromSeq, toSeq);
    }

    private List<StopTimeRow> twoStopRows(String from, int fromSeq, OffsetDateTime deptTime,
                                           String to,   int toSeq,   OffsetDateTime arrTime) {
        return List.of(
                new StopTimeRow(fromSeq, from, deptTime),
                new StopTimeRow(toSeq,   to,   arrTime)
        );
    }

    private OffsetDateTime dept(int plusMinutes) {
        return OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(plusMinutes);
    }

    // =========================================================================
    // getTimetable
    // =========================================================================

    @Test
    void given_noTripsForDate_whenGetTimetable_thenReturnsEmptyTripsList() {
        when(tripRepo.findTripSegmentsForDate("PortA", "PortB", null, TEST_DATE))
                .thenReturn(Collections.emptyList());

        TimetableController.TimetableResponse result =
                timetableService.getTimetable("PortA", "PortB", null, TEST_DATE);

        assertThat(result).isNotNull();
        assertThat(result.trips()).isEmpty();
        assertThat(result.date()).isEqualTo(TEST_DATE.toString());
        verifyNoInteractions(stopRepo);
    }

    @Test
    void given_multipleTripsForDate_whenGetTimetable_thenAllTripsAreMapped() {
        UUID id1 = UUID.randomUUID(), id2 = UUID.randomUUID();
        OffsetDateTime d1 = dept(60),  a1 = dept(90);
        OffsetDateTime d2 = dept(120), a2 = dept(150);

        TripSegmentRow seg1 = segment(id1, "SeaLine", d1, a1, 1, 3);
        TripSegmentRow seg2 = segment(id2, "SeaLine", d2, a2, 1, 3);

        when(tripRepo.findTripSegmentsForDate("PortA", "PortB", "SeaLine", TEST_DATE))
                .thenReturn(List.of(seg1, seg2));
        when(stopRepo.findSegmentByTripId(id1, 1, 3)).thenReturn(twoStopRows("PortA", 1, d1, "PortB", 3, a1));
        when(stopRepo.findSegmentByTripId(id2, 1, 3)).thenReturn(twoStopRows("PortA", 1, d2, "PortB", 3, a2));

        TimetableController.TimetableResponse result =
                timetableService.getTimetable("PortA", "PortB", "SeaLine", TEST_DATE);

        assertThat(result.trips()).hasSize(2);
        assertThat(result.trips()).extracting(FerryDtos.TripDto::tripId)
                .containsExactly(id1, id2);
        assertThat(result.trips()).extracting(FerryDtos.TripDto::departureTime)
                .containsExactly(d1.toString(), d2.toString());
    }

    @Test
    void given_nullOperator_whenGetTimetable_thenOperatorDerivedFromFirstTrip() {
        UUID id = UUID.randomUUID();
        OffsetDateTime d = dept(30), a = dept(60);

        TripSegmentRow seg = segment(id, "AutoOp", d, a, 1, 2);

        when(tripRepo.findTripSegmentsForDate("PortA", "PortB", null, TEST_DATE))
                .thenReturn(List.of(seg));
        when(stopRepo.findSegmentByTripId(id, 1, 2))
                .thenReturn(twoStopRows("PortA", 1, d, "PortB", 2, a));

        TimetableController.TimetableResponse result =
                timetableService.getTimetable("PortA", "PortB", null, TEST_DATE);

        // operator must be taken from the first trip's operator field
        assertThat(result.route().operator()).isEqualTo("AutoOp");
        assertThat(result.trips().get(0).operator()).isEqualTo("AutoOp");
    }

    @Test
    void given_explicitOperator_whenGetTimetable_thenOperatorKeptAsIs() {
        UUID id = UUID.randomUUID();
        OffsetDateTime d = dept(30), a = dept(60);

        TripSegmentRow seg = segment(id, "ExplicitOp", d, a, 1, 2);

        when(tripRepo.findTripSegmentsForDate("PortA", "PortB", "ExplicitOp", TEST_DATE))
                .thenReturn(List.of(seg));
        when(stopRepo.findSegmentByTripId(id, 1, 2))
                .thenReturn(twoStopRows("PortA", 1, d, "PortB", 2, a));

        TimetableController.TimetableResponse result =
                timetableService.getTimetable("PortA", "PortB", "ExplicitOp", TEST_DATE);

        assertThat(result.route().operator()).isEqualTo("ExplicitOp");
    }

    // =========================================================================
    // getUpcoming
    // =========================================================================

    @Test
    void given_noUpcomingTrips_whenGetUpcoming_thenReturnsEmptyTripsList() {
        when(tripRepo.findNextTripSegments("PortA", "PortB", null, 5))
                .thenReturn(Collections.emptyList());

        TimetableController.TimetableResponse result =
                timetableService.getUpcoming("PortA", "PortB", null, 5);

        assertThat(result).isNotNull();
        assertThat(result.trips()).isEmpty();
        verifyNoInteractions(stopRepo);
    }

    @Test
    void given_limitOfThree_whenGetUpcoming_thenRepositoryCalledWithLimitThree() {
        int limit = 3;
        UUID id1 = UUID.randomUUID(), id2 = UUID.randomUUID(), id3 = UUID.randomUUID();
        OffsetDateTime d1 = dept(10),  a1 = dept(40);
        OffsetDateTime d2 = dept(70),  a2 = dept(100);
        OffsetDateTime d3 = dept(130), a3 = dept(160);

        TripSegmentRow s1 = segment(id1, "QuickFerry", d1, a1, 1, 2);
        TripSegmentRow s2 = segment(id2, "QuickFerry", d2, a2, 1, 2);
        TripSegmentRow s3 = segment(id3, "QuickFerry", d3, a3, 1, 2);

        when(tripRepo.findNextTripSegments("PortA", "PortB", "QuickFerry", limit))
                .thenReturn(List.of(s1, s2, s3));
        when(stopRepo.findSegmentByTripId(id1, 1, 2)).thenReturn(twoStopRows("PortA", 1, d1, "PortB", 2, a1));
        when(stopRepo.findSegmentByTripId(id2, 1, 2)).thenReturn(twoStopRows("PortA", 1, d2, "PortB", 2, a2));
        when(stopRepo.findSegmentByTripId(id3, 1, 2)).thenReturn(twoStopRows("PortA", 1, d3, "PortB", 2, a3));

        TimetableController.TimetableResponse result =
                timetableService.getUpcoming("PortA", "PortB", "QuickFerry", limit);

        // Verify the limit was passed exactly
        verify(tripRepo).findNextTripSegments("PortA", "PortB", "QuickFerry", limit);
        assertThat(result.trips()).hasSize(3);
    }

    @Test
    void given_nullOperator_whenGetUpcoming_thenOperatorDerivedFromFirstTrip() {
        UUID id = UUID.randomUUID();
        OffsetDateTime d = dept(20), a = dept(50);

        TripSegmentRow seg = segment(id, "DerivedOp", d, a, 1, 2);

        when(tripRepo.findNextTripSegments("PortA", "PortB", null, 10))
                .thenReturn(List.of(seg));
        when(stopRepo.findSegmentByTripId(id, 1, 2))
                .thenReturn(twoStopRows("PortA", 1, d, "PortB", 2, a));

        TimetableController.TimetableResponse result =
                timetableService.getUpcoming("PortA", "PortB", null, 10);

        assertThat(result.route().operator()).isEqualTo("DerivedOp");
    }

    @Test
    void given_explicitOperator_whenGetUpcoming_thenOperatorKeptAsIs() {
        UUID id = UUID.randomUUID();
        OffsetDateTime d = dept(20), a = dept(50);

        TripSegmentRow seg = segment(id, "FixedOp", d, a, 1, 2);

        when(tripRepo.findNextTripSegments("PortA", "PortB", "FixedOp", 10))
                .thenReturn(List.of(seg));
        when(stopRepo.findSegmentByTripId(id, 1, 2))
                .thenReturn(twoStopRows("PortA", 1, d, "PortB", 2, a));

        TimetableController.TimetableResponse result =
                timetableService.getUpcoming("PortA", "PortB", "FixedOp", 10);

        assertThat(result.route().operator()).isEqualTo("FixedOp");
    }
}
