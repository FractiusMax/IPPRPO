package hotel.metrics;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

/**
 * HTTP-сервер, который отдаёт метрики приложения по адресу {@code /metrics} (ЛР №6).
 *
 * <p>Prometheus не принимает метрики «пуш»-ом: он периодически опрашивает приложение
 * по HTTP (scrape). Поэтому приложение должно предоставить специальный endpoint.
 * Здесь используется встроенный в JDK HTTP-сервер {@code com.sun.net.httpserver},
 * чтобы не добавлять в проект тяжёлый веб-фреймворк.</p>
 */
public final class MetricsHttpServer {

    /** Путь endpoint'а с метриками. */
    public static final String METRICS_PATH = "/metrics";

    /** Тип содержимого в формате Prometheus (text exposition format 0.0.4). */
    public static final String CONTENT_TYPE = "text/plain; version=0.0.4; charset=utf-8";

    private static final Logger LOG = LoggerFactory.getLogger(MetricsHttpServer.class);

    private final PrometheusMeterRegistry registry;

    private final int port;

    private HttpServer server;

    /**
     * Создаёт сервер метрик.
     *
     * @param registry реестр Prometheus, из которого берётся содержимое endpoint'а
     * @param port     порт, на котором будет работать сервер (0 — выбрать свободный)
     */
    public MetricsHttpServer(PrometheusMeterRegistry registry, int port) {
        this.registry = registry;
        this.port = port;
    }

    /**
     * Запускает HTTP-сервер в отдельном потоке.
     *
     * @throws IOException если порт занят или сервер не удалось запустить
     */
    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext(METRICS_PATH, this::handleMetrics);
        server.createContext("/", this::handleIndex);
        server.start();
        LOG.info("Endpoint метрик доступен по адресу http://localhost:{}{}",
                getPort(), METRICS_PATH);
    }

    /**
     * Останавливает HTTP-сервер.
     */
    public void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
            LOG.info("HTTP-сервер метрик остановлен");
        }
    }

    /**
     * @return фактический порт сервера
     */
    public int getPort() {
        if (server == null) {
            return port;
        }
        return server.getAddress().getPort();
    }

    /**
     * Обрабатывает запрос к {@code /metrics}: отдаёт метрики в формате Prometheus.
     *
     * @param exchange обмен данными с клиентом
     * @throws IOException если ответ не удалось записать
     */
    private void handleMetrics(HttpExchange exchange) throws IOException {
        try {
            String body = registry.scrape();
            byte[] data = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", CONTENT_TYPE);
            exchange.sendResponseHeaders(200, data.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(data);
            }
            LOG.debug("Отданы метрики Prometheus, {} байт", data.length);
        } finally {
            exchange.close();
        }
    }

    /**
     * Обрабатывает запрос к корневому адресу: краткая справка о доступных метриках.
     *
     * @param exchange обмен данными с клиентом
     * @throws IOException если ответ не удалось записать
     */
    private void handleIndex(HttpExchange exchange) throws IOException {
        try {
            String body = "Hotel System — метрики Prometheus\r\n"
                    + "--------------------------------------------------------------\r\n"
                    + "GET /metrics — метрики в формате Prometheus (text exposition 0.0.4)\r\n";
            byte[] data = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
            exchange.sendResponseHeaders(200, data.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(data);
            }
        } finally {
            exchange.close();
        }
    }
}
