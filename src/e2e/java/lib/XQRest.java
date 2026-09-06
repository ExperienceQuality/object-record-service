package lib;

public interface XQRest {
    default XQResponse get(String url) {
        return get(url, XQRequest.withHeaders(java.util.Map.of()));
    }

    XQResponse get(String url, XQRequest request);

    XQResponse post(String url, XQRequest request);

    XQResponse patch(String url, XQRequest request);

    XQResponse put(String url, XQRequest request);
}
