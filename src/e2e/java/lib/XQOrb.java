package lib;

import java.util.Objects;

public final class XQOrb {
    private final XQRest rest;
    private final XQTestContext testContext;

    public XQOrb(XQRest rest, XQTestContext testContext) {
        this.rest = Objects.requireNonNull(rest, "rest is required");
        this.testContext = Objects.requireNonNull(testContext, "testContext is required");
    }

    public XQRest rest() {
        return rest;
    }

    public XQTestContext testContext() {
        return testContext;
    }
}
