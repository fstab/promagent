package io.promagent.internal.examples.hooks;

import io.promagent.annotations.Before;
import io.promagent.annotations.Hook;
import io.promagent.hookcontext.HookContext;
import io.promagent.internal.MethodCallCounter;
import io.promagent.internal.examples.classes.InstrumentedClass;

/**
 * Test hook with no @After method
 */
@Hook(instruments = "io.promagent.internal.examples.classes.InstrumentedClass")
public class OnlyBeforeHook {

    public OnlyBeforeHook(HookContext ctxt) {}

    @Before(method = "objects")
    public void before(Object o, InstrumentedClass.Fruit f, InstrumentedClass.Orange x) {
        MethodCallCounter.observe(this, "before", o, f, x);
    }
}
