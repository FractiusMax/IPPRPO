package hotel;

import hotel.model.Booking;
import hotel.model.Guest;
import hotel.model.Room;
import hotel.repository.BookingRepository;
import hotel.repository.InMemoryBookingRepository;
import hotel.service.BookingService;
import hotel.util.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;

/**
 * Точка входа приложения "Отель".
 *
 * <p>Демонстрирует все три уровня журналирования, предусмотренные заданием:</p>
 * <ul>
 *   <li>INFO — успешное бронирование;</li>
 *   <li>WARN — попытка занять уже забронированный номер;</li>
 *   <li>ERROR — ошибка выполнения операции.</li>
 * </ul>
 */
public final class Main {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    private Main() {
    }

    public static void main(String[] args) {
        Config.print();

        BookingRepository repository = new InMemoryBookingRepository();
        BookingService service = new BookingService(repository);

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

        LOG.info("Всего броней: {}", repository.findAll().size());
    }
}
