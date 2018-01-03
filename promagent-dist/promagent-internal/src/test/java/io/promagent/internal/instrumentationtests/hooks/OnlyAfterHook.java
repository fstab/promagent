package io.promagent.internal.instrumentationtests.hooks;

import io.promagent.annotations.After;
import io.promagent.annotations.Hook;
import io.promagent.hookcontext.MetricsStore;
import io.promagent.internal.instrumentationtests.MethodCallCounter;
import io.promagent.internal.instrumentationtests.classes.Fruit;

/**
 * Test hook with no @Before method.
 */
@Hook(instruments = "io.promagent.internal.instrumentationtests.classes.ParameterTypesExample")
public class OnlyAfterHook {

    public OnlyAfterHook(MetricsStore m) {}

    @After(method = "objects")
    public void after(Object o, Fruit f, Fruit.Orange x) {
        MethodCallCounter.observe(this, "after", o, f, x);
    }
}
