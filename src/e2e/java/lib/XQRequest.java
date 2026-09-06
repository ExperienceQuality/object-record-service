package lib;

import java.util.Map;

public record XQRequest(Object body, Map<String, ?> headers) {
    public XQRequest {
        headers = headers == null ? Map.of() : Map.copyOf(headers);
    }

    public static XQRequest withBody(Object body) {
        return new XQRequest(body, Map.of());
    }

    public static XQRequest withHeaders(Map<String, ?> headers) {
        return new XQRequest(null, headers);
    }

    public static XQRequest withBodyAndHeaders(Object body, Map<String, ?> headers) {
        return new XQRequest(body, headers);
    }
}
