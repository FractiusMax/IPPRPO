package hotel.model;

import java.time.LocalDate;

/**
 * Бронь номера: гость, номер и период проживания.
 */
public class Booking {

    private final int id;
    private final Guest guest;
    private final Room room;
    private final LocalDate checkIn;
    private final LocalDate checkOut;

    public Booking(int id, Guest guest, Room room, LocalDate checkIn, LocalDate checkOut) {
        if (checkIn == null || checkOut == null) {
            throw new IllegalArgumentException("Даты не могут быть null");
        }
        if (!checkOut.isAfter(checkIn)) {
            throw new IllegalArgumentException("Дата выезда должна быть позже даты заезда");
        }
        this.id = id;
        this.guest = guest;
        this.room = room;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
    }

    public int getId() {
        return id;
    }

    public Guest getGuest() {
        return guest;
    }

    public Room getRoom() {
        return room;
    }

    public LocalDate getCheckIn() {
        return checkIn;
    }

    public LocalDate getCheckOut() {
        return checkOut;
    }

    /**
     * Проверяет, пересекается ли период бронирования с заданным периодом.
     *
     * @param otherIn  дата заезда другого периода
     * @param otherOut дата выезда другого периода
     * @return true, если периоды пересекаются
     */
    public boolean overlapsWith(LocalDate otherIn, LocalDate otherOut) {
        return checkIn.isBefore(otherOut) && otherIn.isBefore(checkOut);
    }

    @Override
    public String toString() {
        return "Booking{id=" + id
                + ", guest=" + guest
                + ", room=" + room
                + ", checkIn=" + checkIn
                + ", checkOut=" + checkOut + "}";
    }
}
