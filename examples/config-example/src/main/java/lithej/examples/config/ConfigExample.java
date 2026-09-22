package lithej.examples.config;

import java.util.Map;
import lithej.config.Config;

/**
 * Demonstrates {@link Config}: layering system properties, environment variables,
 * and fixed defaults, with explicit first-layer-wins precedence.
 *
 * <p>Try: {@code java -Dapp.port=9090 -jar config-example.jar} to see the system
 * property win over the default.
 */
public final class ConfigExample {

    private ConfigExample() {
    }

    public static void main(String[] args) {
        Config config = Config.empty()
                .withSystemProperties()
                .withEnvironmentVariables()
                .withMap(Map.of("app.name", "config-example"))
                .withDefault("app.port", "8080")
                .withDefault("app.name", "unnamed-app");

        System.out.println("app.name = " + config.require("app.name"));
        System.out.println("app.port = " + config.getInt("app.port", 8080));
        System.out.println("app.debug = " + config.getBoolean("app.debug", false));
        System.out.println("app.missing = " + config.getOrDefault("app.missing", "(not set anywhere)"));
    }
}
