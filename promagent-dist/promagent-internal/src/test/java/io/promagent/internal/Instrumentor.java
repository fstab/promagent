package io.promagent.internal;

import io.promagent.internal.examples.classes.InstrumentedClass;
import io.promagent.internal.examples.classes.InstrumentedMethods;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import org.junit.jupiter.api.Assertions;

import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

import static io.promagent.internal.Promagent.getInstruments;

/**
 * Create an instrumented version of {@link InstrumentedClass} for {@link DelegatorTest}.
 * The implementation is as close as possible to the instrumentation in {@link Promagent}.
 */
public class Instrumentor {

    /**
     * Returns a copy of {@link InstrumentedClass} which will be instrumented and loaded from a temporary class loader.
     */
    public static InstrumentedMethods instrument(SortedSet<HookMetadata> hookMetadata) throws Exception {
        Map<String, SortedSet<HookMetadata.MethodSignature>> instruments = getInstruments(hookMetadata);
        assertTestHooks(instruments);
        Set<HookMetadata.MethodSignature> instrumentedMethods = instruments.get(InstrumentedClass.class.getName());
        // For examples of byte buddy tests, see net.bytebuddy.asm.AdviceTest in the byte buddy source code.
        return new ByteBuddy()
                .redefine(InstrumentedClass.class)
                .visit(Advice.to(PromagentAdvice.class).on(Promagent.matchAnyMethodIn(instrumentedMethods)))
                .make()
                .load(InstrumentedClass.class.getClassLoader(), ClassLoadingStrategy.Default.CHILD_FIRST)
                .getLoaded()
                .newInstance();
    }

    /**
     * {@link Instrumentor} instruments exactly one class, which is hard-coded to be {@link InstrumentedClass}.
     * Make sure there is no hook with another instruments annotation.
     */
    private static void assertTestHooks(Map<String, SortedSet<HookMetadata.MethodSignature>> instruments) {
        String err = "expecting all test hooks to instrument " + InstrumentedClass.class.getName();
        Assertions.assertEquals(1, instruments.size(), err);
        Assertions.assertTrue(instruments.containsKey(InstrumentedClass.class.getName()), err);
    }
}
