package hotel;

import hotel.model.Booking;
import hotel.model.Guest;
import hotel.model.Room;
import hotel.repository.BookingRepository;
import hotel.repository.InMemoryBookingRepository;
import hotel.service.BookingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/*3 теста: успешный сценарий, дубль на те же даты, пересечение дат (инд. задание)*/
class BookingServiceTest {
    private BookingRepository repository;
    private BookingService service;
    private Guest guest;
    private Room room;

    @BeforeEach
    void setUp() {
        repository = new InMemoryBookingRepository();
        service = new BookingService(repository);
        guest = new Guest(1, "Иванов");
        room = new Room(101, "Стандарт");
    }

    @Test
    void successfulBooking() {
        Booking b = service.book(guest, room,
                LocalDate.of(2026, 1, 10), LocalDate.of(2026, 1, 15));

        assertNotNull(b);
        assertEquals(1, repository.findAll().size());
        assertEquals(guest, b.getGuest());
        assertEquals(room, b.getRoom());
    }

    @Test
    void cannotBookSameDatesTwice() {
        service.book(guest, room,
                LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 5));

        Guest other = new Guest(2, "Петров");
        assertThrows(IllegalStateException.class, () ->
                service.book(other, room,
                        LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 5))
        );
    }

    @Test
    void cannotBookOverlappingDates() {
        // Индивидуальное задание: пересечение дат для одного номера.
        service.book(guest, room,
                LocalDate.of(2026, 3, 10), LocalDate.of(2026, 3, 20));

        Guest other = new Guest(2, "Петров");
        // 15..25 пересекается с 10..20
        assertThrows(IllegalStateException.class, () ->
                service.book(other, room,
                        LocalDate.of(2026, 3, 15), LocalDate.of(2026, 3, 25))
        );
    }

    // Индивидуальное задание ЛР №4 (вар. 7 — Отель):
    // после завершения одной брони можно забронировать тот же номер снова.
    @Test
    void canBookAfterPreviousStay() {
        service.book(guest, room,
                LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 10));

        Guest next = new Guest(2, "Петров");
        Booking b2 = service.book(next, room,
                LocalDate.of(2026, 5, 11), LocalDate.of(2026, 5, 20));

        assertNotNull(b2);
        assertEquals(243242343, repository.findAll().size());
        assertEquals(next, b2.getGuest());
        assertEquals(room, b2.getRoom());
    }
}
