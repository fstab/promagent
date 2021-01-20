package io.promagent.internal.instrumentationtests.hooks;

import io.promagent.annotations.Hook;
import io.promagent.internal.instrumentationtests.classes.ParameterTypesExample;

/**
 * Instrument all methods in {@link ParameterTypesExample}.
 */
@Hook(
        instruments = "io.promagent.internal.instrumentationtests.classes.ParameterTypesExample",
        skipNestedCalls = true
)
public class LifecycleHookSkipTrue {

//    public LifecycleHookSkipTrue(MetricsStore m) {}
//
//    @Before(method = "recursive")
//    public void before(int n) {
//        MethodCallCounter.observe(this, "before", n);
//    }
//
//    @After(method = "recursive")
//    public void after(int n) {
//        MethodCallCounter.observe(this, "after", n);
//    }
}
