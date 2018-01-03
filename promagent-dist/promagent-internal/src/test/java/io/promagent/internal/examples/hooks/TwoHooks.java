package io.promagent.internal.examples.hooks;

import io.promagent.annotations.Before;
import io.promagent.annotations.Hook;
import io.promagent.hookcontext.MetricsStore;
import io.promagent.internal.MethodCallCounter;
import io.promagent.internal.examples.classes.Fruit;

/**
 * Two hooks instrumenting the same class.
 */
public class TwoHooks {

    @Hook(instruments = "io.promagent.internal.examples.classes.InstrumentedClass")
    public static class HookOne {

        public HookOne(MetricsStore m) {}

        @Before(method = "objects")
        public void before(Object o, Fruit f, Fruit.Orange x) {
            MethodCallCounter.observe(this, "before", o, f, x);
        }
    }

    @Hook(instruments = "io.promagent.internal.examples.classes.InstrumentedClass")
    public static class HookTwo {

        public HookTwo(MetricsStore m) {}

        @Before(method = "objects")
        public void before(Object o, Fruit f, Fruit.Orange x) {
            MethodCallCounter.observe(this, "before", o, f, x);
        }
    }
}
