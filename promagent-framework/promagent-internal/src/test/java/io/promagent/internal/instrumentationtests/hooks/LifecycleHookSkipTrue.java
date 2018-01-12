package io.promagent.internal.instrumentationtests.hooks;

import io.promagent.annotations.After;
import io.promagent.annotations.Before;
import io.promagent.annotations.Hook;
import io.promagent.hookcontext.MetricsStore;
import io.promagent.internal.instrumentationtests.MethodCallCounter;
import io.promagent.internal.instrumentationtests.classes.Fruit;
import io.promagent.internal.instrumentationtests.classes.Fruit.Orange;
import io.promagent.internal.instrumentationtests.classes.ParameterTypesExample;

import java.util.List;

/**
 * Instrument all methods in {@link ParameterTypesExample}.
 */
@Hook(
        instruments = "io.promagent.internal.instrumentationtests.classes.ParameterTypesExample",
        skipNestedCalls = true
)
public class LifecycleHookSkipTrue {

    public LifecycleHookSkipTrue(MetricsStore m) {}

    @Before(method = "recursive")
    public void before(int n) {
        MethodCallCounter.observe(this, "before", n);
    }

    @After(method = "recursive")
    public void after(int n) {
        MethodCallCounter.observe(this, "after", n);
    }
}
