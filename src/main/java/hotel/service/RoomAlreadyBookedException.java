package hotel.service;


public class RoomAlreadyBookedException extends IllegalStateException {


    public RoomAlreadyBookedException(String message) {
        super(message);
    }
}
