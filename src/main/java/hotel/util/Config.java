package hotel.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public class Config {

    private static String RESOURCE_NAME = "config.properties";
    private static Properties PROPS = new Properties();

    static {
        try (InputStream in = Config.class.getClassLoader()
                .getResourceAsStream(RESOURCE_NAME)) {
            if (in == null) {
                throw new IllegalStateException(
                        "Ресурс '" + RESOURCE_NAME + "' не найден в classpath");
            }
            PROPS.load(new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Не удалось прочитать ресурс '" + RESOURCE_NAME + "'", e);
        }
    }

    private Config() { }

    public static String get(String key) {
        return PROPS.getProperty(key);
    }

    public static String get(String key, String defaultValue) {
        return PROPS.getProperty(key, defaultValue);
    }

    public static void print() {
        System.out.println("[Конфигурация из config.properties]");
        for (String name : PROPS.stringPropertyNames()) {
            System.out.println("  " + name + " = " + PROPS.getProperty(name));
        }
        System.out.println();
    }
}
