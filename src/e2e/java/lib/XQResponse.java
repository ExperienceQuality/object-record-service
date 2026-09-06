package lib;

import io.restassured.response.Response;
import net.javacrumbs.jsonunit.core.Option;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;

public record XQResponse(Response raw) {
    public XQResponse {
        if (raw == null) {
            throw new NullPointerException("raw response is required");
        }
    }

    public XQResponse shouldHaveStatus(int expected) {
        assertThat(raw.statusCode()).isEqualTo(expected);
        return this;
    }

    public XQResponse shouldEqual(String expectedJson) {
        assertThatJson(raw.asString()).isEqualTo(expectedJson);
        return this;
    }

    public XQResponse shouldMatch(String expectedJson) {
        assertThatJson(raw.asString())
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo(expectedJson);
        return this;
    }

    public XQResponse shouldBeJsonArray() {
        assertThatJson(raw.asString()).isArray();
        return this;
    }

}
