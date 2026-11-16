package hotel.service;

import hotel.metrics.ApplicationMetrics;
import hotel.metrics.MetricsRegistry;
import hotel.model.Booking;
import hotel.model.Guest;
import hotel.model.Room;
import hotel.repository.BookingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;


public class BookingService {

    private static final Logger LOG = LoggerFactory.getLogger(BookingService.class);

    private final BookingRepository repository;

    private final ApplicationMetrics metrics;

    public BookingService(BookingRepository repository) {
        this(repository, MetricsRegistry.getMetrics());
    }


    public BookingService(BookingRepository repository, ApplicationMetrics metrics) {
        this.repository = repository;
        this.metrics = metrics;
    }


    public Booking book(Guest guest, Room room, LocalDate checkIn, LocalDate checkOut) {
        LOG.info("Запрос на бронирование получен: гость={}, номер={}, заезд={}, выезд={}",
                guest, room, checkIn, checkOut);

        metrics.recordOperation();
        long startedAt = System.nanoTime();
        try {
            validateRequest(guest, room, checkIn, checkOut);

            if (isRoomOccupied(room, checkIn, checkOut)) {
                LOG.warn("Номер {} уже забронирован на запрошенные даты {} - {}",
                        room.getNumber(), checkIn, checkOut);
                throw new RoomAlreadyBookedException(
                        "Номер " + room.getNumber() + " уже забронирован на эти даты");
            }

            Booking booking = createBooking(guest, room, checkIn, checkOut);
            metrics.recordSuccess();
            metrics.recordBooking(room.getType());
            LOG.info("Бронь создана: {}", booking);
            return booking;
        } catch (RoomAlreadyBookedException e) {
            metrics.recordError();
            metrics.recordBookingConflict();
            throw e;
        } catch (IllegalArgumentException e) {
            metrics.recordError();
            metrics.recordBookingError();
            throw e;
        } catch (RuntimeException e) {
            LOG.error("Ошибка выполнения операции бронирования: гость={}, номер={}",
                    guest, room, e);
            metrics.recordError();
            metrics.recordBookingError();
            throw e;
        } finally {
            metrics.recordDuration(Duration.ofNanos(System.nanoTime() - startedAt));
        }
    }


    private Booking createBooking(Guest guest, Room room, LocalDate checkIn, LocalDate checkOut) {
        int id = repository.findAll().size() + 1;
        Booking booking = new Booking(id, guest, room, checkIn, checkOut);
        repository.save(booking);
        return booking;
    }


    private void validateRequest(Guest guest, Room room, LocalDate checkIn, LocalDate checkOut) {
        if (guest == null || room == null) {
            LOG.error("Некорректный запрос на бронирование: не указан гость или номер");
            throw new IllegalArgumentException("Гость и номер должны быть указаны");
        }
        if (checkIn == null || checkOut == null) {
            LOG.error("Некорректный запрос на бронирование: не указана дата заезда или выезда");
            throw new IllegalArgumentException("Даты заезда и выезда должны быть указаны");
        }
        if (!checkOut.isAfter(checkIn)) {
            LOG.error("Некорректный запрос на бронирование: дата выезда {} раньше даты заезда {}",
                    checkOut, checkIn);
            throw new IllegalArgumentException("Дата выезда должна быть позже даты заезда");
        }
    }


    private boolean isRoomOccupied(Room room, LocalDate checkIn, LocalDate checkOut) {
        List<Booking> existing = repository.findByRoom(room.getNumber());
        for (Booking booking : existing) {
            if (booking.overlapsWith(checkIn, checkOut)) {
                return true;
            }
        }
        return false;
    }
}
