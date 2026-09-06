package lib;

import org.slf4j.Logger;

import java.util.Objects;

public final class LoggingXQRest implements XQRest {
    private final XQRest delegate;
    private final Logger logger;
    private final XQTestContext testContext;

    public LoggingXQRest(XQRest delegate, Logger logger, XQTestContext testContext) {
        this.delegate = Objects.requireNonNull(delegate, "delegate is required");
        this.logger = Objects.requireNonNull(logger, "logger is required");
        this.testContext = Objects.requireNonNull(testContext, "testContext is required");
    }

    @Override
    public XQResponse get(String url, XQRequest request) {
        logger.info("[{}] GET {}", testContext.displayName(), url);

        try {
            XQResponse response = delegate.get(url, request);
            logger.info("[{}] GET {} returned {}", testContext.displayName(), url,
                    response.raw().statusCode());
            return response;
        } catch (RuntimeException error) {
            logger.error("[{}] GET {} failed", testContext.displayName(), url, error);
            throw error;
        }
    }

    @Override
    public XQResponse post(String url, XQRequest request) {
        logger.info("[{}] POST {}", testContext.displayName(), url);

        try {
            XQResponse response = delegate.post(url, request);
            logger.info("[{}] POST {} returned {}", testContext.displayName(), url,
                    response.raw().statusCode());
            return response;
        } catch (RuntimeException error) {
            logger.error("[{}] POST {} failed", testContext.displayName(), url, error);
            throw error;
        }
    }

    @Override
    public XQResponse patch(String url, XQRequest request) {
        logger.info("[{}] PATCH {}", testContext.displayName(), url);
        try {
            XQResponse response = delegate.patch(url, request);
            logger.info("[{}] PATCH {} returned {}", testContext.displayName(), url,
                    response.raw().statusCode());
            return response;
        } catch (RuntimeException error) {
            logger.error("[{}] PATCH {} failed", testContext.displayName(), url, error);
            throw error;
        }
    }

    @Override
    public XQResponse put(String url, XQRequest request) {
        logger.info("[{}] PUT {}", testContext.displayName(), url);
        try {
            XQResponse response = delegate.put(url, request);
            logger.info("[{}] PUT {} returned {}", testContext.displayName(), url,
                    response.raw().statusCode());
            return response;
        } catch (RuntimeException error) {
            logger.error("[{}] PUT {} failed", testContext.displayName(), url, error);
            throw error;
        }
    }
}
