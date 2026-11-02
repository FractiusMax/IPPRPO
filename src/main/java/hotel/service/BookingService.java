package hotel.service;

import hotel.model.Booking;
import hotel.model.Guest;
import hotel.model.Room;
import hotel.repository.BookingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.List;


public class BookingService {

    private static final Logger LOG = LoggerFactory.getLogger(BookingService.class);

    private final BookingRepository repository;

    public BookingService(BookingRepository repository) {
        this.repository = repository;
    }

    /**
     * Выполняет бронирование номера.
     *
     * @param guest    гость, для которого оформляется бронь
     * @param room     бронируемый номер
     * @param checkIn  дата заезда
     * @param checkOut дата выезда
     * @return созданная бронь
     * @throws IllegalArgumentException если параметры запроса некорректны
     * @throws IllegalStateException    если номер уже забронирован на эти даты
     */
    public Booking book(Guest guest, Room room, LocalDate checkIn, LocalDate checkOut) {
        LOG.info("Запрос на бронирование получен: гость={}, номер={}, заезд={}, выезд={}",
                guest, room, checkIn, checkOut);

        validateRequest(guest, room, checkIn, checkOut);

        if (isRoomOccupied(room, checkIn, checkOut)) {
            LOG.warn("Номер {} уже забронирован на запрошенные даты {} - {}",
                    room.getNumber(), checkIn, checkOut);
            throw new IllegalStateException(
                    "Номер " + room.getNumber() + " уже забронирован на эти даты");
        }

        try {
            int id = repository.findAll().size() + 1;
            Booking booking = new Booking(id, guest, room, checkIn, checkOut);
            repository.save(booking);
            LOG.info("Бронь создана: {}", booking);
            return booking;
        } catch (RuntimeException e) {
            LOG.error("Ошибка выполнения операции бронирования: гость={}, номер={}",
                    guest, room, e);
            throw e;
        }
    }

    /**
     * Проверяет корректность входных данных бронирования.
     *
     * @param guest    гость
     * @param room     номер
     * @param checkIn  дата заезда
     * @param checkOut дата выезда
     */
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

    /**
     * Проверяет, свободен ли номер на запрошенный период.
     *
     * @param room     номер
     * @param checkIn  дата заезда
     * @param checkOut дата выезда
     * @return true, если на этот период уже есть пересекающаяся бронь
     */
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
