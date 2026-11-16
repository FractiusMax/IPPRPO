package hotel;

import hotel.metrics.ApplicationMetrics;
import hotel.metrics.MetricsHttpServer;
import hotel.metrics.MetricsRegistry;
import hotel.model.Booking;
import hotel.model.Guest;
import hotel.model.Room;
import hotel.repository.BookingRepository;
import hotel.repository.InMemoryBookingRepository;
import hotel.service.BookingService;
import hotel.util.Config;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDate;


public final class Main {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    private static final String SERVE_ARGUMENT = "--serve";

    private static final String METRICS_PORT_KEY = "metrics.port";

    private static final String DEMO_INTERVAL_KEY = "metrics.demo.interval.seconds";

    private static final int DEFAULT_METRICS_PORT = 8081;

    private static final int DEFAULT_DEMO_INTERVAL_SECONDS = 5;

    private Main() {
    }


    public static void main(String[] args) {
        Config.print();

        ApplicationMetrics metrics = MetricsRegistry.getMetrics();
        PrometheusMeterRegistry registry = MetricsRegistry.getRegistry();
        BookingRepository repository = new InMemoryBookingRepository();
        BookingService service = new BookingService(repository, metrics);

        if (args.length > 0 && SERVE_ARGUMENT.equals(args[0])) {
            serve(service, registry);
        } else {
            runScenarios(service);
            printMetrics(registry);
        }
    }


    private static void serve(BookingService service, PrometheusMeterRegistry registry) {
        int port = Config.getInt(METRICS_PORT_KEY, DEFAULT_METRICS_PORT);
        int interval = Config.getInt(DEMO_INTERVAL_KEY, DEFAULT_DEMO_INTERVAL_SECONDS);

        MetricsHttpServer server = new MetricsHttpServer(registry, port);
        try {
            server.start();
        } catch (IOException e) {
            LOG.error("Не удалось запустить HTTP-сервер метрик на порту {}", port, e);
            return;
        }
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));

        LOG.info("Режим --serve: демонстрационные операции выполняются каждые {} с, "
                + "метрики доступны по адресу http://localhost:{}{}",
                interval, server.getPort(), MetricsHttpServer.METRICS_PATH);

        int cycle = 0;
        while (true) {
            cycle++;
            runDemoCycle(service, cycle);
            try {
                Thread.sleep(interval * 1000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOG.info("Приложение остановлено по прерыванию");
                server.stop();
                return;
            }
        }
    }


    private static void runScenarios(BookingService service) {
        Room room = new Room(101, "Стандарт");
        Guest ivan = new Guest(1, "Иванов");
        Guest petrov = new Guest(2, "Петров");

        Booking first = service.book(ivan, room,
                LocalDate.of(2026, 1, 10), LocalDate.of(2026, 1, 15));
        LOG.info("Создана бронь: {}", first);

        try {
            service.book(petrov, room,
                    LocalDate.of(2026, 1, 12), LocalDate.of(2026, 1, 18));
        } catch (IllegalStateException e) {
            LOG.warn("Бронирование отклонено: {}", e.getMessage());
        }

        try {
            service.book(petrov, room,
                    LocalDate.of(2026, 1, 20), LocalDate.of(2026, 1, 18));
        } catch (IllegalArgumentException e) {
            LOG.error("Бронирование не выполнено: {}", e.getMessage());
        }

        Booking second = service.book(petrov, room,
                LocalDate.of(2026, 1, 15), LocalDate.of(2026, 1, 20));
        LOG.info("Создана бронь: {}", second);
    }

    private static void runDemoCycle(BookingService service, int cycle) {
        LocalDate checkIn = LocalDate.of(2026, 1, 1).plusDays(10L * (cycle - 1));
        LocalDate checkOut = checkIn.plusDays(5);
        Room room = new Room(101, "Стандарт");
        Room deluxe = new Room(202, "Люкс");
        Guest first = new Guest(1, "Иванов");
        Guest second = new Guest(2, "Петров");

        LOG.info("--- Демонстрационный цикл {} (заезд {}, выезд {}) ---",
                cycle, checkIn, checkOut);

        service.book(first, room, checkIn, checkOut);
        service.book(first, deluxe, checkIn, checkOut);

        try {
            service.book(second, room, checkIn.plusDays(1), checkOut.plusDays(1));
        } catch (IllegalStateException e) {
            LOG.warn("Бронирование отклонено: {}", e.getMessage());
        }

        try {
            service.book(second, room, checkOut.plusDays(1), checkOut);
        } catch (IllegalArgumentException e) {
            LOG.error("Бронирование не выполнено: {}", e.getMessage());
        }
    }


    private static void printMetrics(PrometheusMeterRegistry registry) {
        LOG.info("--- Текущие метрики приложения (формат Prometheus) ---");
        for (String line : registry.scrape().split("\n")) {
            if (line.contains("_bucket")) {
                continue;
            }
            if (line.startsWith("application_") || line.startsWith("hotel_")) {
                LOG.info("{}", line);
            }
        }
    }
}
