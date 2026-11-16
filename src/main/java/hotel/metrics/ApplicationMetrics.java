package hotel.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

public final class ApplicationMetrics {

    public static final String OPERATIONS = "application.operations";

    public static final String OPERATIONS_SUCCESS = "application.operations.success";

    public static final String OPERATIONS_ERROR = "application.operations.error";

    public static final String OPERATIONS_DURATION = "application.operations.duration";

    public static final String BOOKINGS = "hotel.bookings";

    public static final String BOOKINGS_CONFLICT = "hotel.bookings.conflict";

    public static final String BOOKINGS_ERROR = "hotel.bookings.error";

    public static final String BOOKINGS_BY_ROOM_TYPE = "hotel.bookings.by.room.type";

    public static final String TAG_ROOM_TYPE = "room_type";

    private final MeterRegistry registry;

    private final Counter operations;

    private final Counter operationsSuccess;

    private final Counter operationsError;

    private final Timer operationsDuration;

    private final Counter bookings;

    private final Counter bookingsConflict;

    private final Counter bookingsError;


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


    public MeterRegistry getRegistry() {
        return registry;
    }


    public void recordOperation() {
        operations.increment();
    }


    public void recordSuccess() {
        operationsSuccess.increment();
    }


    public void recordError() {
        operationsError.increment();
    }


    public void recordDuration(Duration duration) {
        operationsDuration.record(duration.toMillis(), TimeUnit.MILLISECONDS);
    }


    public void recordBooking(String roomType) {
        bookings.increment();
        Counter.builder(BOOKINGS_BY_ROOM_TYPE)
                .tag(TAG_ROOM_TYPE, roomType)
                .description("Количество бронирований по типу номера")
                .register(registry)
                .increment();
    }


    public void recordBookingConflict() {
        bookingsConflict.increment();
    }


    public void recordBookingError() {
        bookingsError.increment();
    }
}
