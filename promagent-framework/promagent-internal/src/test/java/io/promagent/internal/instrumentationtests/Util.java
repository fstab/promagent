package io.promagent.internal.instrumentationtests;

import io.promagent.agent.ClassLoaderCache;
import io.promagent.hookcontext.MetricsStore;
import io.promagent.internal.HookMetadata;
import io.promagent.internal.HookMetadataParser;
import org.mockito.Mockito;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.stream.Stream;

class Util {

    static SortedSet<HookMetadata> loadHookMetadata(Class<?>... hooks) throws IOException, ClassNotFoundException {
        List<Path> classesDir = new ArrayList<>();
        classesDir.add(Paths.get(Util.class.getProtectionDomain().getCodeSource().getLocation().getPath()));
        HookMetadataParser parser = new HookMetadataParser(classesDir);
        return parser.parse(className -> Stream.of(hooks).anyMatch(hookClass -> hookClass.getName().equals(className)));
    }

    static ClassLoaderCache mockClassLoaderCache() throws NoSuchFieldException, IllegalAccessException {
        ClassLoaderCache mockedClassLoaderCache = Mockito.mock(ClassLoaderCache.class);
        Mockito.when(mockedClassLoaderCache.currentClassLoader()).thenReturn(Thread.currentThread().getContextClassLoader());
        Field instance = ClassLoaderCache.class.getDeclaredField("instance");
        instance.setAccessible(true);
        instance.set(null, mockedClassLoaderCache);
        return mockedClassLoaderCache;
    }

    static MetricsStore mockMetricsStore() {
        return Mockito.mock(MetricsStore.class);
    }
}
