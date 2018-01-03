package io.promagent.internal.instrumentationtests;

import io.promagent.internal.HookMetadata;
import io.promagent.internal.Promagent;
import io.promagent.internal.PromagentAdvice;
import io.promagent.internal.instrumentationtests.classes.ParameterTypesExample;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;

import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

import static io.promagent.internal.Promagent.getInstruments;

/**
 * Create an instrumented version of {@link ParameterTypesExample} for {@link ParameterTypesTest}.
 * The implementation is as close as possible to the instrumentation in {@link Promagent}.
 */
public class Instrumentor {

    /**
     * Returns a copy of {@link ParameterTypesExample} which will be instrumented and loaded from a temporary class loader.
     */
    public static <T> T instrument(Class<? extends T> classToBeInstrumented, SortedSet<HookMetadata> hookMetadata) throws Exception {
        Map<String, SortedSet<HookMetadata.MethodSignature>> instruments = getInstruments(hookMetadata);
        Set<HookMetadata.MethodSignature> instrumentedMethods = instruments.get(classToBeInstrumented.getName());
        // For examples of byte buddy tests, see net.bytebuddy.asm.AdviceTest in the byte buddy source code.
        return new ByteBuddy()
                .redefine(classToBeInstrumented)
                .visit(Advice.to(PromagentAdvice.class).on(Promagent.matchAnyMethodIn(instrumentedMethods)))
                .make()
                .load(Instrumentor.class.getClassLoader(), ClassLoadingStrategy.Default.CHILD_FIRST)
                .getLoaded()
                .newInstance();
    }
}
