package lib;

import java.util.Map;

public record XQOrbConfig(
        String baseUri,
        Map<String, ?> defaultHeaders
) {
    private static final String DEFAULT_BASE_URI = "http://localhost:8080";

    public XQOrbConfig {
        baseUri = requireText(baseUri, "baseUri");
        defaultHeaders = defaultHeaders == null ? Map.of() : Map.copyOf(defaultHeaders);
    }

    public static XQOrbConfig fromEnvironment() {
        String baseUri = System.getProperty(
                "xqorb.base-uri",
                System.getenv().getOrDefault("XQORB_BASE_URI", DEFAULT_BASE_URI)
        );

        return new XQOrbConfig(baseUri, Map.of());
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value;
    }
}
