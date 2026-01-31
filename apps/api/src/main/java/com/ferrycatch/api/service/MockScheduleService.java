package com.ferrycatch.api.service;

import com.ferrycatch.api.dto.FerryDtos;
import com.ferrycatch.api.dto.FerryDtos.StopDto;
import com.ferrycatch.api.dto.FerryDtos.TripDto;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class MockScheduleService {

    private static final ZoneId ISTANBUL = ZoneId.of("Europe/Istanbul");

    public TripDto nextTrip(String from, String to, String operator, ZonedDateTime now) {
        var op = normalizeOperator(operator);

        var depFrom = now.plusMinutes(30);
        var stops = buildStops(op, from, to, depFrom);
        var arrivalTime = ZonedDateTime.parse(stops.get(stops.size() - 1).time());

        return new TripDto(
                UUID.randomUUID(),
                op,
                from,
                to,
                depFrom.toOffsetDateTime().toString(),
                arrivalTime.toOffsetDateTime().toString(),
                stops
        );
    }

    public List<TripDto> timetableTrips(String from, String to, String operator, ZonedDateTime dayStart) {
        // 6 рейсов: 06:30, 08:00, 09:30, 11:00, 12:30, 14:00 (шаг 90 минут)
        return List.of(
                nextTripAt(from, to, operator, dayStart.plusMinutes(0)),
                nextTripAt(from, to, operator, dayStart.plusMinutes(90)),
                nextTripAt(from, to, operator, dayStart.plusMinutes(180)),
                nextTripAt(from, to, operator, dayStart.plusMinutes(270)),
                nextTripAt(from, to, operator, dayStart.plusMinutes(360)),
                nextTripAt(from, to, operator, dayStart.plusMinutes(450))
        );
    }

    private TripDto nextTripAt(String from, String to, String operator, ZonedDateTime departure) {
        var op = normalizeOperator(operator);

        // Строим стопы относительно departure
        var stops = buildStops(op, from, to, departure);
        var arrivalTime = ZonedDateTime.parse(stops.get(stops.size() - 1).time());

        return new TripDto(
                UUID.randomUUID(),
                op,
                from,
                to,
                departure.toOffsetDateTime().toString(),
                arrivalTime.toOffsetDateTime().toString(),
                stops
        );
    }

    private static String normalizeOperator(String operator) {
        if (operator == null || operator.isBlank()) return "Mavi Marmara";
        var v = operator.trim().toLowerCase(Locale.ROOT);
        if (v.contains("mavi")) return "Mavi Marmara";
        if (v.contains("şehir") || v.contains("sehir")) return "Şehir Hatları";
        return operator.trim();
    }

    private static List<StopDto> buildStops(String operator, String from, String to, ZonedDateTime depFrom) {
        return switch (operator) {
            case "Şehir Hatları" -> buildSehirHatlariStops(from, to, depFrom);
            case "Mavi Marmara" -> buildMaviMarmaraStops(from, to, depFrom);
            default -> buildMaviMarmaraStops(from, to, depFrom);
        };
    }

    private static List<StopDto> buildMaviMarmaraStops(String from, String to, ZonedDateTime depFrom) {
        var t1 = depFrom;
        var t2 = t1.plusMinutes(12);
        var t3 = t2.plusMinutes(18);
        return List.of(
                new StopDto(from, 1, t1.toString()),
                new StopDto("Burgazada", 2, t2.toString()),
                new StopDto(to, 3, t3.toString())
        );
    }

    private static List<StopDto> buildSehirHatlariStops(String from, String to, ZonedDateTime depFrom) {
        var t1 = depFrom;
        var t2 = t1.plusMinutes(10);
        var t3 = t2.plusMinutes(12);
        var t4 = t3.plusMinutes(14);
        var t5 = t4.plusMinutes(18);
        return List.of(
                new StopDto(from, 1, t1.toString()),
                new StopDto("Burgazada", 2, t2.toString()),
                new StopDto("Heybeliada", 3, t3.toString()),
                new StopDto("Büyükada", 4, t4.toString()),
                new StopDto(to, 5, t5.toString())
        );
    }

    public List<FerryDtos.RouteDto> listRoutes(String from, String to, String operator) {
        var all = List.of(
                new FerryDtos.RouteDto(UUID.fromString("00000000-0000-0000-0000-000000000101"), "Kınalıada", "Bostancı", "Mavi Marmara"),
                new FerryDtos.RouteDto(UUID.fromString("00000000-0000-0000-0000-000000000102"), "Kınalıada", "Kadıköy", "Mavi Marmara"),
                new FerryDtos.RouteDto(UUID.fromString("00000000-0000-0000-0000-000000000201"), "Kınalıada", "Bostancı", "Şehir Hatları"),
                new FerryDtos.RouteDto(UUID.fromString("00000000-0000-0000-0000-000000000202"), "Kınalıada", "Kadıköy", "Şehir Hatları")
        );

        return all.stream()
                .filter(r -> from == null || from.isBlank() || r.from().equalsIgnoreCase(from))
                .filter(r -> to == null || to.isBlank() || r.to().equalsIgnoreCase(to))
                .filter(r -> operator == null || operator.isBlank() || r.operator().equalsIgnoreCase(operator))
                .toList();
    }

    public List<FerryDtos.RouteWithNextDto> listRoutesWithNext(String from, String to, String operator) {
        var now = ZonedDateTime.now(ISTANBUL);

        var base = listRoutes(from, to, operator);

        return base.stream().map(r -> {
            var trip = nextTrip(r.from(), r.to(), r.operator(), now);

            var minutes = (int) java.time.Duration
                    .between(now, ZonedDateTime.parse(trip.departureTime()))
                    .toMinutes();

            return new FerryDtos.RouteWithNextDto(
                    r.id(),
                    r.from(),
                    r.to(),
                    r.operator(),
                    minutes,
                    trip.departureTime()
            );
        }).toList();
    }
}
