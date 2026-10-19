package hotel.service;

import hotel.model.Booking;
import hotel.model.Guest;
import hotel.model.Room;
import hotel.repository.BookingRepository;

import java.time.LocalDate;
import java.util.List;

/*DI*/
public class BookingService {
    private BookingRepository repository;

    public BookingService(BookingRepository repository) {
        this.repository = repository;
    }

    public Booking book(Guest guest, Room room, LocalDate checkIn, LocalDate checkOut) {
        List<Booking> existing = repository.findByRoom(room.getNumber());
        for (Booking b : existing) {
            if (b.overlapsWith(checkIn, checkOut)) {
                throw new IllegalStateException(
                        "Номер " + room.getNumber() + " уже забронирован на эти даты");
            }
        }
        int id = repository.findAll().size() + 1;
        Booking booking = new Booking(id, guest, room, checkIn, checkOut);
        repository.save(booking);
        return booking;
    }
}
