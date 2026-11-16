package hotel.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

public final class ApplicationMetrics {

    /** Общее количество операций. */
    public static final String OPERATIONS = "application.operations";

    /** Количество успешных операций. */
    public static final String OPERATIONS_SUCCESS = "application.operations.success";

    /** Количество неуспешных операций. */
    public static final String OPERATIONS_ERROR = "application.operations.error";

    /** Длительность выполнения операции. */
    public static final String OPERATIONS_DURATION = "application.operations.duration";

    /** Количество выполненных бронирований номеров. */
    public static final String BOOKINGS = "hotel.bookings";

    /** Попытки забронировать уже занятый номер. */
    public static final String BOOKINGS_CONFLICT = "hotel.bookings.conflict";

    /** Ошибки выполнения операции бронирования. */
    public static final String BOOKINGS_ERROR = "hotel.bookings.error";

    /** Количество бронирований в разрезе типа номера. */
    public static final String BOOKINGS_BY_ROOM_TYPE = "hotel.bookings.by.room.type";

    /** Метка, по которой бронирования группируются по типу номера. */
    public static final String TAG_ROOM_TYPE = "room_type";

    private final MeterRegistry registry;

    private final Counter operations;

    private final Counter operationsSuccess;

    private final Counter operationsError;

    private final Timer operationsDuration;

    private final Counter bookings;

    private final Counter bookingsConflict;

    private final Counter bookingsError;

    /**
     * Создаёт метрики и регистрирует их в указанном реестре.
     *
     * @param registry реестр Micrometer, в котором регистрируются метрики
     */
    public ApplicationMetrics(MeterRegistry registry) {
        this.registry = registry;
        this.operations = Counter.builder(OPERATIONS)
                .description("Общее количество операций бронирования")
                .register(registry);
        this.operationsSuccess = Counter.builder(OPERATIONS_SUCCESS)
                .description("Количество успешных операций бронирования")
                .register(registry);
        this.operationsError = Counter.builder(OPERATIONS_ERROR)
                .description("Количество неуспешных операций бронирования")
                .register(registry);
        this.operationsDuration = Timer.builder(OPERATIONS_DURATION)
                .description("Длительность выполнения операции бронирования")
                .publishPercentileHistogram()
                .register(registry);
        this.bookings = Counter.builder(BOOKINGS)
                .description("Количество выполненных бронирований номеров")
                .register(registry);
        this.bookingsConflict = Counter.builder(BOOKINGS_CONFLICT)
                .description("Попытки забронировать уже занятый номер")
                .register(registry);
        this.bookingsError = Counter.builder(BOOKINGS_ERROR)
                .description("Ошибки выполнения операции бронирования")
                .register(registry);
    }

    /**
     * @return реестр, в котором зарегистрированы метрики
     */
    public MeterRegistry getRegistry() {
        return registry;
    }

    /**
     * Учитывает попытку выполнить операцию бронирования.
     */
    public void recordOperation() {
        operations.increment();
    }

    /**
     * Учитывает успешно выполненную операцию бронирования.
     */
    public void recordSuccess() {
        operationsSuccess.increment();
    }

    /**
     * Учитывает неуспешную операцию бронирования.
     */
    public void recordError() {
        operationsError.increment();
    }

    /**
     * Учитывает длительность выполнения операции бронирования.
     *
     * @param duration время, затраченное на операцию
     */
    public void recordDuration(Duration duration) {
        operationsDuration.record(duration.toMillis(), TimeUnit.MILLISECONDS);
    }

    /**
     * Учитывает успешное бронирование номера.
     *
     * @param roomType категория забронированного номера
     */
    public void recordBooking(String roomType) {
        bookings.increment();
        Counter.builder(BOOKINGS_BY_ROOM_TYPE)
                .tag(TAG_ROOM_TYPE, roomType)
                .description("Количество бронирований по типу номера")
                .register(registry)
                .increment();
    }

    /**
     * Учитывает попытку забронировать уже занятый номер.
     */
    public void recordBookingConflict() {
        bookingsConflict.increment();
    }

    /**
     * Учитывает ошибку выполнения операции бронирования.
     */
    public void recordBookingError() {
        bookingsError.increment();
    }
}
