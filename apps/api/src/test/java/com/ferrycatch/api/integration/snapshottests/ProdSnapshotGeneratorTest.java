package com.ferrycatch.api.integration.snapshottests;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.ferrycatch.api.integration.config.SnapshotProperties;
import com.ferrycatch.api.integration.config.SnapshotTestConfig;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = SnapshotTestConfig.class)
@TestPropertySource(properties = {
        "ferrycatch.snapshot.prod-base-url=https://ferrynow.app",
        "ferrycatch.snapshot.date=2026-03-11",
        "ferrycatch.snapshot.output-dir=src/test/resources/snapshots"
})
@Tag("integration")
class ProdSnapshotGeneratorTest {

    @Autowired
    private RestTemplate prodRestTemplate;

    @Autowired
    private SnapshotProperties props;

    private final ObjectMapper om = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    @Test
    void generateSnapshotsFromProd() throws IOException {
        if (props.prodBaseUrl() == null || props.prodBaseUrl().isBlank()) {
            throw new IllegalStateException("ferrycatch.snapshot.prod-base-url is not configured");
        }

        Path snapshotRoot = Path.of(props.outputDir());
        Path timetablesDir = snapshotRoot.resolve("timetables");

        Files.createDirectories(snapshotRoot);
        Files.createDirectories(timetablesDir);

        RouteApi[] routesResponse = prodRestTemplate.getForObject("/api/v1/routes", RouteApi[].class);

        if (routesResponse == null || routesResponse.length == 0) {
            throw new IllegalStateException("Prod /api/v1/routes returned empty list");
        }

        List<RouteSnapshot> routes = Arrays.stream(routesResponse)
                .map(r -> new RouteSnapshot(
                        decode(r.operator()),
                        decode(r.from()),
                        decode(r.to()),
                        blankToNull(decode(r.variant()))
                ))
                .sorted(Comparator.comparing(RouteSnapshot::operator)
                        .thenComparing(RouteSnapshot::from)
                        .thenComparing(RouteSnapshot::to)
                        .thenComparing(r -> r.variant() == null ? "" : r.variant()))
                .toList();

        om.writeValue(snapshotRoot.resolve("routes-prod.json").toFile(), routes);

        for (RouteSnapshot route : routes) {
            String url = UriComponentsBuilder.fromPath("/api/v1/timetable")
                    .queryParam("from", route.from())
                    .queryParam("to", route.to())
                    .queryParamIfPresent("operator", optional(route.operator()))
                    .queryParam("date", props.date())
                    .toUriString();

            TimetableApiResponse body = prodRestTemplate.getForObject(url, TimetableApiResponse.class);
            String raw = prodRestTemplate.getForObject(url, String.class);

            System.out.println(raw);
            System.out.println(url);

            if (body == null) {
                throw new IllegalStateException("Null timetable for " + routeKey(route));
            }

            TimetableSnapshot snapshot = normalize(body);
            om.writeValue(timetablesDir.resolve(snapshotFileName(route)).toFile(), snapshot);
        }
    }

    private TimetableSnapshot normalize(TimetableApiResponse body) {
        RouteSnapshot route = new RouteSnapshot(
                body.route().operator(),
                body.route().from(),
                body.route().to(),
                blankToNull(body.route().variant())
        );

        List<TripSnapshot> trips = body.trips().stream()
                .map(t -> new TripSnapshot(
                        t.departureTime(),
                        t.arrivalTime(),
                        t.stops().stream()
                                .map(s -> new StopSnapshot(s.stopName(), s.sequence(), s.time()))
                                .sorted(Comparator.comparingInt(StopSnapshot::sequence))
                                .toList()
                ))
                .sorted(Comparator.comparing(TripSnapshot::departureTime)
                        .thenComparing(TripSnapshot::arrivalTime))
                .toList();

        return new TimetableSnapshot(route, trips);
    }

    private String decode(String s) {
        if (s == null || s.isBlank()) {
            return s;
        }
        return URLDecoder.decode(s, StandardCharsets.UTF_8);
    }

    private Optional<String> optional(String s) {
        return (s == null || s.isBlank()) ? Optional.empty() : Optional.of(s);
    }

    private String blankToNull(String s) {
        if (s == null) return null;
        String v = s.trim();
        return v.isEmpty() ? null : v;
    }

    private String snapshotFileName(RouteSnapshot route) {
        String base = slug(route.operator()) + "__" + slug(route.from()) + "__" + slug(route.to());
        if (route.variant() != null) {
            base += "__" + slug(route.variant());
        }
        return base + ".json";
    }

    private String routeKey(RouteSnapshot route) {
        return route.operator() + "|" + route.from() + "|" + route.to()
                + (route.variant() == null ? "" : "|" + route.variant());
    }

    private String slug(String s) {
        return s.toLowerCase()
                .replace("ş", "s")
                .replace("ı", "i")
                .replace("ğ", "g")
                .replace("ü", "u")
                .replace("ö", "o")
                .replace("ç", "c")
                .replace(" ", "-")
                .replace("→", "-")
                .replaceAll("[^a-z0-9-]+", "")
                .replaceAll("-+", "-");
    }

    public record RouteSnapshot(String operator, String from, String to, String variant) {}
    public record TimetableSnapshot(RouteSnapshot route, List<TripSnapshot> trips) {}
    public record TripSnapshot(String departureTime, String arrivalTime, List<StopSnapshot> stops) {}
    public record StopSnapshot(String stopName, int sequence, String time) {}

    public record RouteApi(String id, String from, String to, String operator, String variant) {}
    public record TimetableApiResponse(RouteApi route, String date, List<TripApi> trips) {}
    public record TripApi(String tripId, String operator, String from, String to,
                          String departureTime, String arrivalTime, List<StopApi> stops) {}
    public record StopApi(String stopName, int sequence, String time) {}


    @Test
    void debugProps() {
        System.out.println("prodBaseUrl = " + props.prodBaseUrl());
    }

    /**
     * Тогда, скорее всего, разница не в самом endpoint, а в том, как именно твой snapshot generator делает запрос по сравнению с ручным запросом. Если ручной вызов и Swagger дают нормальный ответ, а generator сохраняет encoded JSON, надо сравнить не только URL, но и заголовки, клиент, возможные cookies, редиректы и точный raw response для каждого способа. Разные request headers и разные backend instances действительно могут давать разное поведение.
     *
     * Что это сужает
     * Теперь уже почти исключено, что проблема в TimetableService.
     * Остаются в основном такие варианты:
     *
     * generator попадает на другой instance/другую версию за балансировщиком,
     * ​
     *
     * generator отправляет запрос иначе, чем браузер/Swagger, например с другими headers,
     *
     * ты в одном случае смотришь прямой ответ, а в другом — уже нормализованный/кэшированный результат.
     *
     * Что проверить прямо сейчас
     * Сделай в generator запрос через exchange() и выведи:
     *
     * финальный URL,
     *
     * status code,
     *
     * все response headers,
     *
     * raw body.
     *
     * Пример:
     *
     * java
     * HttpHeaders headers = new HttpHeaders();
     * headers.setAccept(List.of(MediaType.APPLICATION_JSON));
     *
     * ResponseEntity<String> response = prodRestTemplate.exchange(
     *         url,
     *         HttpMethod.GET,
     *         new HttpEntity<>(headers),
     *         String.class
     * );
     *
     * System.out.println("URL = " + url);
     * System.out.println("STATUS = " + response.getStatusCode());
     * System.out.println("HEADERS = " + response.getHeaders());
     * System.out.println("RAW = " + response.getBody());
     * Это важно, потому что сейчас надо сравнивать не DTO, а сырой сетевой ответ для generator path. Работа через RestTemplate.exchange() с явными headers — стандартный способ проверить отличие клиентского запроса.
     *
     * Ещё полезнее
     * Сравни raw body для двух способов на один и тот же маршрут:
     *
     * браузер/Postman/curl,
     *
     * prodRestTemplate.
     *
     * Если только prodRestTemplate получает encoded JSON, тогда смотри:
     *
     * interceptors,
     *
     * custom message converters,
     *
     * proxy/CDN behavior,
     *
     * cookies/session affinity.
     *
     * Практический ход
     * Я бы даже временно проверил тот же URL через обычный curl:
     *
     * bash
     * curl -i "https://ferrynow.app/api/v1/timetable?from=Bostanc%C4%B1&to=K%C4%B1nal%C4%B1ada&operator=Mavi%20Marmara&date=2026-03-11"
     * и сравнил это с raw body из prodRestTemplate. Если они разные, значит у тебя либо разные backend responses по клиенту, либо разная маршрутизация запроса.
     *
     * Мой текущий вывод
     * Если пересъём snapshot снова даёт encoded значения, а ручной запрос — нет, то проблема уже не в коде контроллера/сервиса, а в разнице между каналами запроса.
     * Сейчас лучший следующий шаг — снять response headers + raw body именно из generator и сравнить с curl/браузером один-в-один.
     *
     * Покажи потом response.getHeaders() и RAW из exchange() — по ним уже можно будет прицельно понять, это балансировщик, кэш или клиентский путь.
     */
}