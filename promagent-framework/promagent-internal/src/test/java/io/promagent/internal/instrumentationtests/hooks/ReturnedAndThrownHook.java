package io.promagent.internal.instrumentationtests.hooks;

import io.promagent.annotations.*;
import io.promagent.hookcontext.MetricsStore;
import io.promagent.internal.instrumentationtests.MethodCallCounter;
import io.promagent.internal.instrumentationtests.classes.Fruit;

import java.util.List;

@Hook(instruments = "io.promagent.internal.instrumentationtests.classes.ReturnedAndThrownExample")
public class ReturnedAndThrownHook {

    public ReturnedAndThrownHook(MetricsStore m) {}

    @Before(method = "returnVoid")
    public void beforeVoid(Fruit f) {
        MethodCallCounter.observe(this, "beforeVoid", f);
    }

    @Before(method = "returnVoid")
    public void afterVoid(Fruit f) {
        MethodCallCounter.observe(this, "afterVoid", f);
    }

    @Before(method = "returnPrimitive")
    public void beforePrimitive(Fruit.Orange o) {
        MethodCallCounter.observe(this, "beforePrimitive", o);
    }

    @After(method = "returnPrimitive")
    public void afterPrimitive(Fruit.Orange o, @Returned int i) {
        MethodCallCounter.observe(this, "afterPrimitive", o, i);
    }

    @Before(method = "returnObject")
    public void beforeObject() {
        MethodCallCounter.observe(this, "beforeObject");
    }

    @After(method = "returnObject")
    public void afterObject(@Returned Fruit f) {
        MethodCallCounter.observe(this, "afterObject", f);
    }

    @Before(method = "returnArray")
    public void beforeArray(int... params) {
        MethodCallCounter.observe(this, "beforeArray", new Object[]{params});
    }

    @After(method = "returnArray")
    public void afterArray(@Returned int[] ret, int... params) {
        MethodCallCounter.observe(this, "afterArray", ret, params);
    }

    @Before(method = "returnGenerics")
    public <T extends Fruit> void beforeGenerics(T fruit) {
        MethodCallCounter.observe(this, "beforeGenerics", fruit);
    }

    @After(method = "returnGenerics")
    public <T extends Fruit> void afterGenerics(T fruit, @Returned List<T> ret) {
        MethodCallCounter.observe(this, "afterGenerics", fruit, ret);
    }

    @Before(method = "throwsRuntimeException")
    public void beforeThrowsRuntimeException(int a, Fruit.Orange o) {
        MethodCallCounter.observe(this, "beforeThrowsRuntimeException", a, o);
    }

    @After(method = "throwsRuntimeException")
    public void afterThrowsRuntimeException(int a, Fruit.Orange o, @Returned String ret, @Thrown Throwable e) {
        MethodCallCounter.observe(this, "afterThrowsRuntimeException", a, o, ret, e);
    }

    @Before(method = "throwsCheckedException")
    public void beforeThrowsCheckedException() {
        MethodCallCounter.observe(this, "beforeThrowsCheckedException");
    }

    @After(method = "throwsCheckedException")
    public void afterThrowsCheckedException(@Returned int ret, @Thrown Throwable e) {
        MethodCallCounter.observe(this, "afterThrowsCheckedException", ret, e);
    }
}
