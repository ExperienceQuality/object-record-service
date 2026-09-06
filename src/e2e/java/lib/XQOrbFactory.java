package lib;

import io.restassured.RestAssured;
import io.restassured.specification.RequestSpecification;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.function.UnaryOperator;

public final class XQOrbFactory {
    private XQOrbFactory() {
    }

    public static XQOrb create(XQOrbConfig config, XQTestContext testContext) {
        return create(config, testContext, UnaryOperator.identity());
    }

    public static XQOrb create(
            XQOrbConfig config,
            XQTestContext testContext,
            UnaryOperator<XQRest> decorator
    ) {
        Objects.requireNonNull(config, "config is required");
        Objects.requireNonNull(testContext, "testContext is required");
        Objects.requireNonNull(decorator, "decorator is required");

        RequestSpecification client = RestAssured
                .given()
                .baseUri(config.baseUri())
                .headers(config.defaultHeaders());

        XQRest rest = new LoggingXQRest(
                new DefaultXQRest(client),
                LoggerFactory.getLogger(testContext.testClass()),
                testContext
        );

        return new XQOrb(decorator.apply(rest), testContext);
    }

    public static XQOrb create(XQTestContext testContext) {
        return create(XQOrbConfig.fromEnvironment(), testContext);
    }
}
