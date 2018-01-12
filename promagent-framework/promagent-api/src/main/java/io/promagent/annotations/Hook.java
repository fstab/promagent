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

package io.promagent.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The @Hook annotation indicates that the annotated class is a Hook.
 * The parameter {@link Hook#instruments()} defines which classes or interfaces are instrumented by that Hook.
 * The method annotations {@link Before} and {@link After} define which methods should be instrumented within that class or interface.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Hook {

    /**
     * List of classes or interfaces to be instrumented.
     */
    String[] instruments();

    /**
     * If true, nested calls are skipped.
     * Nested means that the instrumented method calls another method that is instrumented with the same hook.
     * In most cases, you would only be interested in the outer call, so the default is {@code true}.
     * If set to {@code false}, nested calls will also be instrumented.
     * For nested calls, the same Hook instance is re-used.
     * For outer calls, a new Hook instance is created.
     */
    boolean skipNestedCalls() default true;
}
