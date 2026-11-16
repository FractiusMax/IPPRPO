package hotel.metrics;

import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;

/**
 * Глобальный реестр метрик приложения (ЛР №6).
 *
 * <p>Реестр Prometheus — компонент, который переводит метрики Micrometer в текстовый
 * формат Prometheus. Именно его содержимое отдаётся по HTTP на адресе {@code /metrics},
 * откуда метрики забирает Prometheus (scrape).</p>
 */
public final class MetricsRegistry {

    private static final PrometheusMeterRegistry REGISTRY =
            new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);

    private static final ApplicationMetrics METRICS = new ApplicationMetrics(REGISTRY);

    private MetricsRegistry() {
    }

    /**
     * @return реестр Prometheus, содержимое которого отдаётся на {@code /metrics}
     */
    public static PrometheusMeterRegistry getRegistry() {
        return REGISTRY;
    }

    /**
     * @return метрики приложения, зарегистрированные в глобальном реестре
     */
    public static ApplicationMetrics getMetrics() {
        return METRICS;
    }
}
