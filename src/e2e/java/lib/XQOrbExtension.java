package lib;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;

public final class XQOrbExtension implements ParameterResolver {
    private static final ExtensionContext.Namespace NAMESPACE =
            ExtensionContext.Namespace.create(XQOrbExtension.class);
    private static final String ORB_KEY = XQOrb.class.getName();

    @Override
    public boolean supportsParameter(
            ParameterContext parameterContext,
            ExtensionContext extensionContext
    ) {
        return parameterContext.getParameter().getType() == XQOrb.class;
    }

    @Override
    public Object resolveParameter(
            ParameterContext parameterContext,
            ExtensionContext extensionContext
    ) {
        return extensionContext.getStore(NAMESPACE).getOrComputeIfAbsent(
                ORB_KEY,
                $ -> XQOrbFactory.create(toTestContext(extensionContext)),
                XQOrb.class
        );
    }

    private static XQTestContext toTestContext(ExtensionContext context) {
        return new XQTestContext(
                context.getDisplayName(),
                context.getUniqueId(),
                context.getTestClass().map(Class::getName).orElse("unknown"),
                context.getTestMethod().map(method -> method.getName()).orElse("unknown")
        );
    }
}
