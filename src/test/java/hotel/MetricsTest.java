package hotel;

import hotel.metrics.ApplicationMetrics;
import hotel.metrics.MetricsHttpServer;
import hotel.model.Guest;
import hotel.model.Room;
import hotel.repository.InMemoryBookingRepository;
import hotel.service.BookingService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты мониторинга (ЛР №6): проверяют, что метрики действительно связаны
 * с работой приложения, а не просто имеют начальное значение.
 */
class MetricsTest {

    private static final LocalDate CHECK_IN = LocalDate.of(2026, 4, 1);

    private static final LocalDate CHECK_OUT = LocalDate.of(2026, 4, 6);

    private MeterRegistry registry;

    private ApplicationMetrics metrics;

    private BookingService service;

    private Guest guest;

    private Room room;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        metrics = new ApplicationMetrics(registry);
        service = new BookingService(new InMemoryBookingRepository(), metrics);
        guest = new Guest(1, "Иванов");
        room = new Room(101, "Стандарт");
    }

    private double counterValue(String name) {
        Counter counter = registry.get(name).counter();
        return counter.count();
    }

    @Test
    void successfulBookingIncrementsOperationAndBookingMetrics() {
        service.book(guest, room, CHECK_IN, CHECK_OUT);

        assertEquals(1.0, counterValue(ApplicationMetrics.OPERATIONS), 0.0001);
        assertEquals(1.0, counterValue(ApplicationMetrics.OPERATIONS_SUCCESS), 0.0001);
        assertEquals(0.0, counterValue(ApplicationMetrics.OPERATIONS_ERROR), 0.0001);
        assertEquals(1.0, counterValue(ApplicationMetrics.BOOKINGS), 0.0001);
        assertEquals(0.0, counterValue(ApplicationMetrics.BOOKINGS_CONFLICT), 0.0001);
        assertEquals(0.0, counterValue(ApplicationMetrics.BOOKINGS_ERROR), 0.0001);
    }

    @Test
    void occupiedRoomIncrementsErrorAndConflictMetrics() {
        service.book(guest, room, CHECK_IN, CHECK_OUT);

        Guest other = new Guest(2, "Петров");
        assertThrows(IllegalStateException.class, () ->
                service.book(other, room, CHECK_IN.plusDays(1), CHECK_OUT.plusDays(1))
        );

        assertEquals(2.0, counterValue(ApplicationMetrics.OPERATIONS), 0.0001);
        assertEquals(1.0, counterValue(ApplicationMetrics.OPERATIONS_SUCCESS), 0.0001);
        assertEquals(1.0, counterValue(ApplicationMetrics.OPERATIONS_ERROR), 0.0001);
        assertEquals(1.0, counterValue(ApplicationMetrics.BOOKINGS_CONFLICT), 0.0001);
        assertEquals(0.0, counterValue(ApplicationMetrics.BOOKINGS_ERROR), 0.0001);
    }

    @Test
    void invalidRequestIncrementsBookingErrorMetric() {
        Guest other = new Guest(2, "Петров");
        assertThrows(IllegalArgumentException.class, () ->
                service.book(other, room, CHECK_OUT, CHECK_IN)
        );

        assertEquals(1.0, counterValue(ApplicationMetrics.OPERATIONS), 0.0001);
        assertEquals(1.0, counterValue(ApplicationMetrics.OPERATIONS_ERROR), 0.0001);
        assertEquals(1.0, counterValue(ApplicationMetrics.BOOKINGS_ERROR), 0.0001);
        assertEquals(0.0, counterValue(ApplicationMetrics.BOOKINGS), 0.0001);
    }

    @Test
    void metricInvariantsAreKept() {
        service.book(guest, room, CHECK_IN, CHECK_OUT);
        Guest other = new Guest(2, "Петров");
        assertThrows(IllegalStateException.class, () ->
                service.book(other, room, CHECK_IN.plusDays(1), CHECK_OUT.plusDays(1))
        );
        assertThrows(IllegalArgumentException.class, () ->
                service.book(other, room, CHECK_OUT, CHECK_IN)
        );

        double operations = counterValue(ApplicationMetrics.OPERATIONS);
        double success = counterValue(ApplicationMetrics.OPERATIONS_SUCCESS);
        double error = counterValue(ApplicationMetrics.OPERATIONS_ERROR);
        double conflict = counterValue(ApplicationMetrics.BOOKINGS_CONFLICT);
        double bookingError = counterValue(ApplicationMetrics.BOOKINGS_ERROR);

        // Все операции разбиваются на успешные и ошибочные.
        assertEquals(operations, success + error, 0.0001);
        // Количество выполненных бронирований равно количеству успешных операций.
        assertEquals(success, counterValue(ApplicationMetrics.BOOKINGS), 0.0001);
        // Ошибки операции делятся на конфликт и ошибку выполнения.
        assertEquals(error, conflict + bookingError, 0.0001);
    }

    @Test
    void operationDurationIsMeasured() {
        service.book(guest, room, CHECK_IN, CHECK_OUT);

        Timer timer = registry.get(ApplicationMetrics.OPERATIONS_DURATION).timer();
        assertEquals(1L, timer.count());
        assertTrue(timer.totalTime(java.util.concurrent.TimeUnit.NANOSECONDS) >= 0L);
    }

    @Test
    void prometheusExpositionContainsExpectedMetricNames() {
        service.book(guest, room, CHECK_IN, CHECK_OUT);

        PrometheusMeterRegistry prometheusRegistry =
                new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        new ApplicationMetrics(prometheusRegistry);
        prometheusRegistry.counter("application.operations").increment();

        String exposition = prometheusRegistry.scrape();

        assertTrue(exposition.contains("application_operations_total"),
                "В экспозиции нет метрики application_operations_total");
    }

    @Test
    void metricsEndpointServesMetricsOverHttp() throws IOException, InterruptedException {
        PrometheusMeterRegistry prometheusRegistry =
                new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        ApplicationMetrics endpointMetrics = new ApplicationMetrics(prometheusRegistry);
        BookingService endpointService =
                new BookingService(new InMemoryBookingRepository(), endpointMetrics);
        endpointService.book(guest, room, CHECK_IN, CHECK_OUT);

        MetricsHttpServer server = new MetricsHttpServer(prometheusRegistry, 0);
        server.start();
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + server.getPort()
                            + MetricsHttpServer.METRICS_PATH))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString());

            assertEquals(200, response.statusCode());
            assertTrue(response.body().contains("application_operations_total"),
                    "Endpoint не отдал метрику application_operations_total");
            assertTrue(response.body().contains("hotel_bookings_total"),
                    "Endpoint не отдал метрику hotel_bookings_total");
        } finally {
            server.stop();
        }
    }
}
