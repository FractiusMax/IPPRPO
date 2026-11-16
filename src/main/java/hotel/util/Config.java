package hotel.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.TreeSet;

/**
 * Загрузка и вывод конфигурации приложения из ресурса config.properties.
 */
public final class Config {

    private static final Logger LOG = LoggerFactory.getLogger(Config.class);

    private static final String RESOURCE_NAME = "config.properties";

    private static final Properties PROPS = new Properties();

    static {
        try (InputStream in = Config.class.getClassLoader()
                .getResourceAsStream(RESOURCE_NAME)) {
            if (in == null) {
                throw new IllegalStateException(
                        "Ресурс '" + RESOURCE_NAME + "' не найден в classpath");
            }
            PROPS.load(new InputStreamReader(in, StandardCharsets.UTF_8));
            LOG.info("Конфигурация загружена из ресурса '{}'", RESOURCE_NAME);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Не удалось прочитать ресурс '" + RESOURCE_NAME + "'", e);
        }
    }

    private Config() {
    }

    public static String get(String key) {
        return PROPS.getProperty(key);
    }

    public static String get(String key, String defaultValue) {
        return PROPS.getProperty(key, defaultValue);
    }

    /**
     * Возвращает числовое значение параметра конфигурации.
     *
     * @param key          имя параметра
     * @param defaultValue значение по умолчанию, если параметр отсутствует или не является числом
     * @return значение параметра конфигурации
     */
    public static int getInt(String key, int defaultValue) {
        String value = PROPS.getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            LOG.warn("Параметр '{}' имеет некорректное значение '{}', "
                    + "используется значение по умолчанию {}", key, value, defaultValue);
            return defaultValue;
        }
    }

    /**
     * Выводит содержимое конфигурации в журнал на уровне INFO.
     */
    public static void print() {
        LOG.info("[Конфигурация из config.properties]");
        for (String name : new TreeSet<>(PROPS.stringPropertyNames())) {
            LOG.info("  {} = {}", name, PROPS.getProperty(name));
        }
    }
}
