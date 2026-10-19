package hotel;

import hotel.model.Booking;
import hotel.model.Guest;
import hotel.model.Room;
import hotel.repository.BookingRepository;
import hotel.repository.InMemoryBookingRepository;
import hotel.service.BookingService;
import hotel.util.Config;

import java.time.LocalDate;

public class Main {
    public static void main(String[] args) {
        Config.print();

        BookingRepository repository = new InMemoryBookingRepository();
        BookingService service = new BookingService(repository);

        Room room = new Room(101, "Стандарт");
        Guest ivan = new Guest(1, "Иванов");
        Guest petrov = new Guest(2, "Петров");

        Booking b1 = service.book(ivan, room,
                LocalDate.of(2026, 1, 10), LocalDate.of(2026, 1, 15));
        System.out.println("Создана бронь: " + b1);

        // индивидуал квест
        try {
            service.book(petrov, room,
                    LocalDate.of(2026, 1, 12), LocalDate.of(2026, 1, 18));
        } catch (IllegalStateException e) {
            System.out.println("Ожидаемая ошибка: " + e.getMessage());
        }

        Booking b2 = service.book(petrov, room,
                LocalDate.of(2026, 1, 15), LocalDate.of(2026, 1, 20));
        System.out.println("Создана бронь: " + b2);

        System.out.println("Всего броней: " + repository.findAll().size());
    }
}
//scope
//как тесты показать так чтобы они не учитывались в джар
//жизненый цикл - порядок

