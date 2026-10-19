package hotel.repository;

import hotel.model.Booking;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class InMemoryBookingRepository implements BookingRepository {
    private List<Booking> bookings = new ArrayList<>();

    @Override
    public void save(Booking booking) {
        bookings.add(booking);
    }

    @Override
    public List<Booking> findByRoom(int roomNumber) {
        return bookings.stream()
                .filter(b -> b.getRoom().getNumber() == roomNumber)
                .collect(Collectors.toList());
    }

    @Override
    public List<Booking> findAll() {
        return new ArrayList<>(bookings);
    }
}
