package io.promagent.internal.instrumentationtests.hooks;

import io.promagent.annotations.Hook;

/**
 * Test hook with no @Before method.
 */
@Hook(instruments = "io.promagent.internal.instrumentationtests.classes.ParameterTypesExample")
public class OnlyAfterHook {

//    public OnlyAfterHook(MetricsStore m) {}
//
//    @After(method = "objects")
//    public void after(Object o, Fruit f, Fruit.Orange x) {
//        MethodCallCounter.observe(this, "after", o, f, x);
//    }
}
