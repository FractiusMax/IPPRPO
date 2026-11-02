package hotel;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import hotel.model.Booking;
import hotel.model.Guest;
import hotel.model.Room;
import hotel.repository.BookingRepository;
import hotel.repository.InMemoryBookingRepository;
import hotel.service.BookingService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ЛР №5: проверка журналирования предметной области.
 *
 * <p>Тест перехватывает события Logback и убеждается, что сервис бронирования
 * формирует сообщения всех требуемых уровней: INFO, WARN и ERROR.</p>
 */
class LoggingScenarioTest {

    private static final String LOGGER_NAME = "hotel.service.BookingService";

    private ch.qos.logback.classic.Logger serviceLogger;
    private ListAppender<ILoggingEvent> appender;
    private BookingService service;

    @BeforeEach
    void setUp() {
        service = new BookingService(new InMemoryBookingRepository());

        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        serviceLogger = context.getLogger(LOGGER_NAME);
        appender = new ListAppender<>();
        appender.start();
        serviceLogger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        serviceLogger.detachAppender(appender);
        appender.stop();
    }

    @Test
    void successfulBookingIsLoggedWithInfoLevel() {
        Booking booking = service.book(new Guest(1, "Иванов"), new Room(101, "Стандарт"),
                LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 5));

        assertEquals(2, appender.list.size());
        for (Level level : levels()) {
            assertEquals(Level.INFO, level);
        }
        assertTrue(messages().get(0).contains("Запрос на бронирование получен"));
        assertTrue(messages().get(1).contains("Бронь создана"));
        assertEquals(1, booking.getId());
    }

    @Test
    void occupiedRoomIsLoggedWithWarnLevel() {
        Room room = new Room(101, "Стандарт");
        service.book(new Guest(1, "Иванов"), room,
                LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 10));

        appender.list.clear();
        assertThrows(IllegalStateException.class, () -> service.book(new Guest(2, "Петров"), room,
                LocalDate.of(2026, 4, 5), LocalDate.of(2026, 4, 15)));

        // INFO — запрос получен, WARN — номер занят.
        assertEquals(2, appender.list.size());
        assertEquals(Level.INFO, levels().get(0));
        assertEquals(Level.WARN, levels().get(1));
        assertTrue(messages().get(1).contains("уже забронирован"));
    }

    @Test
    void invalidRequestIsLoggedWithErrorLevel() {
        assertThrows(IllegalArgumentException.class,
                () -> service.book(new Guest(1, "Иванов"), new Room(101, "Стандарт"),
                        LocalDate.of(2026, 4, 10), LocalDate.of(2026, 4, 1)));

        assertTrue(levels().contains(Level.ERROR));
        assertTrue(messages().stream()
                .anyMatch(message -> message.contains("Некорректный запрос на бронирование")));
    }

    @Test
    void storageFailureIsLoggedWithErrorLevel() {
        BookingService failing = new BookingService(new FailingRepository());

        assertThrows(RuntimeException.class,
                () -> failing.book(new Guest(1, "Иванов"), new Room(101, "Стандарт"),
                        LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 5)));

        assertTrue(levels().contains(Level.ERROR));
        assertTrue(messages().stream()
                .anyMatch(message -> message.contains("Ошибка выполнения операции бронирования")));
    }

    private List<Level> levels() {
        List<Level> result = new ArrayList<>();
        for (ILoggingEvent event : appender.list) {
            result.add(event.getLevel());
        }
        return result;
    }

    private List<String> messages() {
        List<String> result = new ArrayList<>();
        for (ILoggingEvent event : appender.list) {
            result.add(event.getFormattedMessage());
        }
        return result;
    }

    /**
     * Тестовое хранилище, которое всегда отказывает при сохранении —
     * источник ошибки уровня ERROR.
     */
    private static final class FailingRepository implements BookingRepository {

        @Override
        public void save(Booking booking) {
            throw new IllegalStateException("Хранилище недоступно");
        }

        @Override
        public List<Booking> findByRoom(int roomNumber) {
            return Collections.emptyList();
        }

        @Override
        public List<Booking> findAll() {
            return Collections.emptyList();
        }
    }
}
