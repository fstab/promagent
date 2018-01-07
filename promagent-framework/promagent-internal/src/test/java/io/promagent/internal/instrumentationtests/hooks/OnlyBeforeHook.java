package io.promagent.internal.instrumentationtests.hooks;

import io.promagent.annotations.Before;
import io.promagent.annotations.Hook;
import io.promagent.hookcontext.MetricsStore;
import io.promagent.internal.instrumentationtests.MethodCallCounter;
import io.promagent.internal.instrumentationtests.classes.Fruit;

/**
 * Test hook with no @After method
 */
@Hook(instruments = "io.promagent.internal.instrumentationtests.classes.ParameterTypesExample")
public class OnlyBeforeHook {

    public OnlyBeforeHook(MetricsStore m) {}

    @Before(method = "objects")
    public void before(Object o, Fruit f, Fruit.Orange x) {
        MethodCallCounter.observe(this, "before", o, f, x);
    }
}
