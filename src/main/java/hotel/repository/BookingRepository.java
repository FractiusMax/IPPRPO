package hotel.repository;

import hotel.model.Booking;

import java.util.List;

/**
 * Хранилище броней.
 */
public interface BookingRepository {

    void save(Booking booking);

    List<Booking> findByRoom(int roomNumber);

    List<Booking> findAll();
}
