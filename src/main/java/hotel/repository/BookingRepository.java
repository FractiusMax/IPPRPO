package hotel.repository;

import hotel.model.Booking;

import java.util.List;

public interface BookingRepository {
    void save(Booking booking);
    List<Booking> findByRoom(int roomNumber);
    List<Booking> findAll();
}
        