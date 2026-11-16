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

/**
 * Точка входа приложения "Отель".
 *
 * <p>ЛР №6: приложение не только выполняет операции бронирования, но и предоставляет
 * метрики Prometheus по HTTP-адресу {@code /metrics}.</p>
 *
 * <p>Режимы запуска:</p>
 * <ul>
 *   <li>без аргументов — одноразовый прогон сценариев (успех, занятый номер, ошибка)
 *       и вывод текущих метрик в консоль;</li>
 *   <li>{@code --serve} — поднимает HTTP-сервер метрик и в цикле выполняет
 *       демонстрационные операции, чтобы Prometheus собирал живые значения.</li>
 * </ul>
 */
public final class Main {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    /** Аргумент запуска в режиме постоянно работающего сервера метрик. */
    private static final String SERVE_ARGUMENT = "--serve";

    /** Ключ конфигурации с портом HTTP-сервера метрик. */
    private static final String METRICS_PORT_KEY = "metrics.port";

    /** Ключ конфигурации с интервалом демонстрационного цикла в секундах. */
    private static final String DEMO_INTERVAL_KEY = "metrics.demo.interval.seconds";

    /** Порт HTTP-сервера метрик по умолчанию. */
    private static final int DEFAULT_METRICS_PORT = 8081;

    /** Интервал демонстрационного цикла по умолчанию, секунды. */
    private static final int DEFAULT_DEMO_INTERVAL_SECONDS = 5;

    private Main() {
    }

    /**
     * Основной метод приложения.
     *
     * @param args аргументы командной строки
     */
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

    /**
     * Режим постоянно работающего приложения: HTTP-сервер метрик и цикл операций.
     *
     * @param service  сервис бронирования
     * @param registry реестр метрик Prometheus
     */
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

    /**
     * Выполняет одноразовый набор сценариев: успех, занятый номер, ошибка.
     *
     * @param service сервис бронирования
     */
    private static void runScenarios(BookingService service) {
        Room room = new Room(101, "Стандарт");
        Guest ivan = new Guest(1, "Иванов");
        Guest petrov = new Guest(2, "Петров");

        // Сценарий 1 — успешная операция: сообщения уровня INFO.
        Booking first = service.book(ivan, room,
                LocalDate.of(2026, 1, 10), LocalDate.of(2026, 1, 15));
        LOG.info("Создана бронь: {}", first);

        // Сценарий 2 — проблемная ситуация: номер уже занят, уровень WARN.
        try {
            service.book(petrov, room,
                    LocalDate.of(2026, 1, 12), LocalDate.of(2026, 1, 18));
        } catch (IllegalStateException e) {
            LOG.warn("Бронирование отклонено: {}", e.getMessage());
        }

        // Сценарий 3 — ошибка выполнения операции: уровень ERROR.
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

    /**
     * Выполняет один демонстрационный цикл: успешное бронирование, попытку занять
     * уже занятый номер и операцию с некорректными данными.
     *
     * <p>Даты в каждом цикле сдвигаются, поэтому сценарии повторяются, а счётчики
     * метрик монотонно растут.</p>
     *
     * @param service сервис бронирования
     * @param cycle   номер цикла, начиная с 1
     */
    private static void runDemoCycle(BookingService service, int cycle) {
        LocalDate checkIn = LocalDate.of(2026, 1, 1).plusDays(10L * (cycle - 1));
        LocalDate checkOut = checkIn.plusDays(5);
        Room room = new Room(101, "Стандарт");
        Guest first = new Guest(1, "Иванов");
        Guest second = new Guest(2, "Петров");

        LOG.info("--- Демонстрационный цикл {} (заезд {}, выезд {}) ---",
                cycle, checkIn, checkOut);

        service.book(first, room, checkIn, checkOut);

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

    /**
     * Выводит в журнал текущие значения метрик приложения в формате Prometheus.
     *
     * @param registry реестр метрик Prometheus
     */
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
