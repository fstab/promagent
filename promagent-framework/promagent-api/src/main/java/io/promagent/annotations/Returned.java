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
 * A parameter annotated with @Returned can be used in a Hook's @After method to capture the return value of the instrumented method.
 *
 * Example: In order to instrument the following method:
 *
 * <pre>
 *     int sum(int a, int b) {...}
 * </pre>
 *
 * A Hook could use an @After method like this:
 *
 * <pre>
 *    {@literal @}After(method = "sum")
 *     void after(int a, int b, @Returned int sum) {...}
 * </pre>
 *
 * The parameter annotated with @Returned is optional, if the hook does not use the return value, the parameter can be omitted.
 * <p/>
 * If the instrumented method terminates exceptionally, the type's default value is assigned to the parameter,
 * i.e. {@code 0} for numeric types and {@code null} for reference types.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Returned {}
