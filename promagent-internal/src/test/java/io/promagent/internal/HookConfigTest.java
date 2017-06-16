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

package io.promagent.internal;

import io.promagent.agent.annotations.Before;
import io.promagent.agent.annotations.Hook;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class HookConfigTest {

    @Hook(instruments = "com.example.Some.class")
    private static class TestHook {

        @Before(method = "arrayArgs")
        void before(Object[] a, int[] b, String[] c) {}

        @Before(method = "noArgs")
        void before() {}

        @Before(method = "primitiveArgs")
        void before(boolean a, char b, byte c, short d, int f, float g, long h, double i) {}

        @Before(method = "boxedArgs")
        void before(Boolean a, Character b, Byte c, Short d, Integer f, Float g, Long h, Double i) {}
    }

    @Test
    void testServletHook() throws ClassNotFoundException {
        String expected = "" +
                "javax.servlet.Filter\n" +
                "  * doFilter(javax.servlet.ServletRequest, javax.servlet.ServletResponse)\n" +
                "  * service(javax.servlet.ServletRequest, javax.servlet.ServletResponse)\n" +
                "javax.servlet.Servlet\n" +
                "  * doFilter(javax.servlet.ServletRequest, javax.servlet.ServletResponse)\n" +
                "  * service(javax.servlet.ServletRequest, javax.servlet.ServletResponse)";
        String actual = HookConfig.of("io.promagent.internal.hooks.ServletHook").toString();
        Assertions.assertEquals(expected, actual);
    }

    @Test
    void testPrimitiveTypes() throws ClassNotFoundException {
        String expected = "" +
                "com.example.Some.class\n" +
                "  * arrayArgs(java.lang.Object[], int[], java.lang.String[])\n" +
                "  * boxedArgs(java.lang.Boolean, java.lang.Character, java.lang.Byte, java.lang.Short, java.lang.Integer, java.lang.Float, java.lang.Long, java.lang.Double)\n" +
                "  * noArgs()\n" +
                "  * primitiveArgs(boolean, char, byte, short, int, float, long, double)";
        String actual = HookConfig.of(TestHook.class.getName()).toString();
        Assertions.assertEquals(expected, actual);
    }

    @Test
    void testNoHook() throws ClassNotFoundException {
        // Use HookConfigTest as an example of a class that does not have any @Hook annotation.
        String result = HookConfig.of(HookConfigTest.class.getName()).toString();
        Assertions.assertEquals("", result);
    }

    @Test
    void testClassNotFound() throws ClassNotFoundException {
        Assertions.assertThrows(ClassNotFoundException.class, () -> HookConfig.of("io.promagent.internal.hooks.NonExistingHook"));
    }
}
