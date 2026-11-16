package hotel.service;

/**
 * Номер уже забронирован на запрошенные даты (ЛР №6, вариант 7 "Отель").
 *
 * <p>Отдельный тип нужен, чтобы отличать штатный бизнес-отказ "номер занят"
 * от ошибки выполнения операции. Благодаря этому мониторинг может раздельно
 * учитывать попытку занять занятый номер и настоящую ошибку операции.</p>
 */
public class RoomAlreadyBookedException extends IllegalStateException {

    /**
     * Создаёт исключение с сообщением об ошибке.
     *
     * @param message описание ситуации
     */
    public RoomAlreadyBookedException(String message) {
        super(message);
    }
}
