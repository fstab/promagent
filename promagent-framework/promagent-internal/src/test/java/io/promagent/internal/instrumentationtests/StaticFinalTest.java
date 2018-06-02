package io.promagent.internal.instrumentationtests;

import static io.promagent.internal.Promagent.getInstruments;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

import io.promagent.agent.ClassLoaderCache;
import io.promagent.hookcontext.MetricsStore;
import io.promagent.internal.Delegator;
import io.promagent.internal.HookMetadata;
import io.promagent.internal.Promagent;
import io.promagent.internal.PromagentAdvice;
import io.promagent.internal.instrumentationtests.classes.StaticFinalExample;
import io.promagent.internal.instrumentationtests.hooks.StaticFinalTestHook;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StaticFinalTest {

    private Object example;

    @BeforeEach
    void setUp() throws Exception {
        SortedSet<HookMetadata> hookMetadata = Util.loadHookMetadata(StaticFinalTestHook.class);
        ClassLoaderCache classLoaderCache = Util.mockClassLoaderCache();

        MetricsStore metricsStore = Util.mockMetricsStore();
        Delegator.init(hookMetadata, metricsStore, classLoaderCache);
        MethodCallCounter.reset();

        Map<String, SortedSet<HookMetadata.MethodSignature>> instruments = getInstruments(hookMetadata);
        Set<HookMetadata.MethodSignature> instrumentedMethods = instruments.get(StaticFinalExample.class.getName());
        example = new ByteBuddy()
                .redefine(StaticFinalExample.class)
                .visit(Advice.to(PromagentAdvice.class).on(Promagent.matchAnyMethodIn(instrumentedMethods)))
                .make()
                .load(this.getClass().getClassLoader(), ClassLoadingStrategy.Default.CHILD_FIRST)
                .getLoaded()
                .newInstance();
    }

    @Test
    void testPublicStaticMethod() throws Exception {
        int expectedTotalHookCalls = 0;
        for (String methodName : new String[] {
                "helloPublic",
                "helloPublicFinal",
                "helloPublicStatic",
                "helloPublicStaticFinal"
        }) {
            Method method = example.getClass().getMethod(methodName, String.class);
            method.invoke(example, "world");
            expectedTotalHookCalls++;
            MethodCallCounter.assertNumCalls(expectedTotalHookCalls, StaticFinalTestHook.class, "before", new Object[]{"world"});
            MethodCallCounter.assertNumCalls(expectedTotalHookCalls, StaticFinalTestHook.class, "after", new Object[]{"world"});
        }
    }
}
