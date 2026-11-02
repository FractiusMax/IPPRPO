package hotel.model;

import java.util.Objects;

/**
 * Номер отеля: номер комнаты и её категория.
 */
public class Room {

    private final int number;
    private final String type;

    public Room(int number, String type) {
        this.number = number;
        this.type = type;
    }

    // НАРУШЕНИЕ ПРАВИЛА LineLength (демонстрация статического анализа): эта строка намеренно длиннее 120 символов, чтобы Checkstyle сообщил "Line is longer than allowed".

    public int getNumber() {
        return number;
    }

    public String getType() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Room)) {
            return false;
        }
        Room room = (Room) o;
        return number == room.number;
    }

    @Override
    public int hashCode() {
        return Objects.hash(number);
    }

    @Override
    public String toString() {
        return "Room{number=" + number + ", type='" + type + "'}";
    }
}
