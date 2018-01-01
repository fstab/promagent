package io.promagent.internal.examples.hooks;

import io.promagent.annotations.After;
import io.promagent.annotations.Hook;
import io.promagent.hookcontext.HookContext;
import io.promagent.internal.MethodCallCounter;
import io.promagent.internal.examples.classes.InstrumentedClass;

/**
 * Test hook with no @Before method.
 */
@Hook(instruments = "io.promagent.internal.examples.classes.InstrumentedClass")
public class OnlyAfterHook {

    public OnlyAfterHook(HookContext ctxt) {}

    @After(method = "objects")
    public void after(Object o, InstrumentedClass.Fruit f, InstrumentedClass.Orange x) {
        MethodCallCounter.observe(this, "after", o, f, x);
    }
}
