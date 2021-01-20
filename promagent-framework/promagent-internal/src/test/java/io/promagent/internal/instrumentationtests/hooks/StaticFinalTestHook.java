package io.promagent.internal.instrumentationtests.hooks;

import io.promagent.annotations.Hook;

@Hook(instruments = "io.promagent.internal.instrumentationtests.classes.StaticFinalExample")
public class StaticFinalTestHook {

//    public StaticFinalTestHook(MetricsStore m) {}
//
//    @Before(method = {
//            "helloPublic",
//            "helloPublicFinal",
//            "helloPublicStatic",
//            "helloPublicStaticFinal"
//    })
//    public void before(String name) {
//        MethodCallCounter.observe(this, "before", name);
//    }
//
//    @After(method = {
//            "helloPublic",
//            "helloPublicFinal",
//            "helloPublicStatic",
//            "helloPublicStaticFinal"
//    })
//    public void after(String name) {
//        MethodCallCounter.observe(this, "after", name);
//    }
}
