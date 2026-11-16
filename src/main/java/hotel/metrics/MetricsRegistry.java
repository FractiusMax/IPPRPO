package hotel.metrics;

import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;


public final class MetricsRegistry {

    private static final PrometheusMeterRegistry REGISTRY =
            new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);

    private static final ApplicationMetrics METRICS = new ApplicationMetrics(REGISTRY);

    private MetricsRegistry() {
    }


    public static PrometheusMeterRegistry getRegistry() {
        return REGISTRY;
    }


    public static ApplicationMetrics getMetrics() {
        return METRICS;
    }
}
