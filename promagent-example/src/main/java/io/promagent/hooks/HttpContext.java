package io.promagent.hooks;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Store the HTTP context in a thread-local, so that we know which database call was triggered by which REST service.
 */
class HttpContext {

    static class Key<T> {}

    static final Key<String> HTTP_METHOD = new Key<>();
    static final Key<String> HTTP_PATH = new Key<>();

    private static final ThreadLocal<Map<Key, Object>> threadLocal = ThreadLocal.withInitial(HashMap::new);

    static <T> void put(Key<T> key, T value) {
        threadLocal.get().put(key, value);
    }

    @SuppressWarnings("unchecked")
    static <T> Optional<T> get(Key<T> key) {
        return Optional.ofNullable((T) threadLocal.get().get(key));
    }

    static void clear(Key... keys) {
        for (Key key : keys) {
            threadLocal.get().remove(key);
        }
    }
}
