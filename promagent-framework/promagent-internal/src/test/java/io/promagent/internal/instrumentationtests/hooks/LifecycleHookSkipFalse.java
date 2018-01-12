package io.promagent.internal.instrumentationtests.hooks;

import io.promagent.annotations.After;
import io.promagent.annotations.Before;
import io.promagent.annotations.Hook;
import io.promagent.hookcontext.MetricsStore;
import io.promagent.internal.instrumentationtests.MethodCallCounter;
import io.promagent.internal.instrumentationtests.classes.Fruit;
import io.promagent.internal.instrumentationtests.classes.Fruit.Orange;
import io.promagent.internal.instrumentationtests.classes.ParameterTypesExample;

/**
 * Instrument all methods in {@link ParameterTypesExample}.
 */
@Hook(
        instruments = "io.promagent.internal.instrumentationtests.classes.ParameterTypesExample",
        skipNestedCalls = false
)
public class LifecycleHookSkipFalse {

    public LifecycleHookSkipFalse(MetricsStore m) {}

    @Before(method = "recursive")
    public void before(int n) {
        MethodCallCounter.observe(this, "before", n);
    }

    @After(method = "recursive")
    public void after(int n) {
        MethodCallCounter.observe(this, "after", n);
    }
}
