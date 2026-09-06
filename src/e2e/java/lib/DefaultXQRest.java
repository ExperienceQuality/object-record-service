package lib;

import io.restassured.response.Response;
import io.restassured.http.ContentType;
import io.restassured.RestAssured;
import io.restassured.specification.RequestSpecification;

import java.util.Objects;

public final class DefaultXQRest implements XQRest {
    private final RequestSpecification client;

    public DefaultXQRest(RequestSpecification client) {
        this.client = Objects.requireNonNull(client, "client is required");
    }

    @Override
    public XQResponse get(String url, XQRequest request) {
        Objects.requireNonNull(url, "url is required");
        Objects.requireNonNull(request, "request is required");

        Response response = RestAssured.given()
                .spec(client)
                .headers(request.headers())
                .when()
                .get(url);

        return new XQResponse(response);
    }

    @Override
    public XQResponse post(String url, XQRequest request) {
        Objects.requireNonNull(url, "url is required");
        Objects.requireNonNull(request, "request is required");

        RequestSpecification requestSpec = RestAssured.given()
                .spec(client)
                .headers(request.headers())
                .contentType(ContentType.JSON);
        if (request.body() != null) requestSpec.body(request.body());
        Response response = requestSpec.when().post(url);

        return new XQResponse(response);
    }

    @Override
    public XQResponse patch(String url, XQRequest request) {
        Objects.requireNonNull(url, "url is required");
        Objects.requireNonNull(request, "request is required");

        Response response = RestAssured.given()
                .spec(client)
                .headers(request.headers())
                .contentType(ContentType.JSON)
                .body(request.body())
                .when()
                .patch(url);

        return new XQResponse(response);
    }

    @Override
    public XQResponse put(String url, XQRequest request) {
        Objects.requireNonNull(url, "url is required");
        Objects.requireNonNull(request, "request is required");
        Response response = RestAssured.given()
                .spec(client).headers(request.headers()).contentType(ContentType.JSON)
                .body(request.body()).when().put(url);
        return new XQResponse(response);
    }
}
