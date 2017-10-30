// Copyright 2017 The Promagent Authors
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package io.promagent.hookcontext;

import java.util.Map;
import java.util.Optional;

public class TypeSafeThreadLocal {

    private final ThreadLocal<Map<String, Object>> threadLocal;

    public TypeSafeThreadLocal(ThreadLocal<Map<String, Object>> threadLocal) {
        this.threadLocal = threadLocal;
    }

    public <T> void put(Key<T> key, T value) {
        threadLocal.get().put(key.keyString, value);
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(Key<T> key) {
        return Optional.ofNullable((T) threadLocal.get().get(key.keyString));
    }

    public void clear(Key... keys) {
        for (Key key : keys) {
            threadLocal.get().remove(key.keyString);
        }
    }
}
