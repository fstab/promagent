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

import io.promagent.annotations.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;

class HookMetadataParserTest {

    private HookMetadataParser parser;

    @BeforeEach
    void setUp() {
        List<Path> classesDir = new ArrayList<>();
        classesDir.add(Paths.get(this.getClass().getProtectionDomain().getCodeSource().getLocation().getPath()));
        this.parser = new HookMetadataParser(classesDir);
    }

    @Hook(instruments = {
            "javax.servlet.Servlet",
            "javax.servlet.Filter"
    })
    private static class ServletTestHook {

        @Before(method = {"service", "doFilter"})
        public void before(ServletRequest request, ServletResponse response) {}

        @After(method = {"service", "doFilter"})
        public void after(ServletRequest request, ServletResponse response) throws Exception {}
    }

    @Hook(instruments = "com.example.Some")
    private static class PrimitiveTypesTestHook {

        @Before(method = "arrayArgs")
        void before(Object[] a, int[] b, String[] c) {}

        @Before(method = "noArgs")
        void before() {}

        @Before(method = "primitiveArgs")
        void before(boolean a, char b, byte c, short d, int f, float g, long h, double i) {}

        @Before(method = "boxedArgs")
        void before(Boolean a, Character b, Byte c, Short d, Integer f, Float g, Long h, Double i) {}
    }

    @Hook(instruments = "com.example.ReturnThrown")
    private static class ReturnedAndThrownTestHook {
        @After(method = "div")
        void after(int a, int b, @Returned int result, @Thrown Throwable exception) {}
    }

    @Test
    void testServletHook() throws ClassNotFoundException, IOException {
        String expected = ServletTestHook.class.getName() + " instruments [javax.servlet.Filter, javax.servlet.Servlet]:\n" +
                "  * doFilter(javax.servlet.ServletRequest, javax.servlet.ServletResponse)\n" +
                "  * service(javax.servlet.ServletRequest, javax.servlet.ServletResponse)";
        SortedSet<HookMetadata> result = parser.parse(className -> className.equals(ServletTestHook.class.getName()));
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals(expected, result.first().toString());
    }

    @Test
    void testPrimitiveTypes() throws ClassNotFoundException, IOException {
        String expected = PrimitiveTypesTestHook.class.getName() + " instruments [com.example.Some]:\n" +
                "  * arrayArgs(java.lang.Object[], int[], java.lang.String[])\n" +
                "  * boxedArgs(java.lang.Boolean, java.lang.Character, java.lang.Byte, java.lang.Short, java.lang.Integer, java.lang.Float, java.lang.Long, java.lang.Double)\n" +
                "  * noArgs()\n" +
                "  * primitiveArgs(boolean, char, byte, short, int, float, long, double)";
        SortedSet<HookMetadata> result = parser.parse(className -> className.equals(PrimitiveTypesTestHook.class.getName()));
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals(expected, result.first().toString());
    }

    @Test
    void testReturnedAndThrown() throws IOException, ClassNotFoundException {
        String expected = ReturnedAndThrownTestHook.class.getName() + " instruments [com.example.ReturnThrown]:\n" +
                "  * div(int, int)";
        SortedSet<HookMetadata> result = parser.parse(className -> className.equals(ReturnedAndThrownTestHook.class.getName()));
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals(expected, result.first().toString());
    }

    @Test
    void testNoHook() throws ClassNotFoundException, IOException {
        // Use HookMetadataParserTest as an example of a class that does not have any @Hook annotation.
        SortedSet<HookMetadata> result = parser.parse(className -> className.equals(HookMetadataParserTest.class.getName()));
        Assertions.assertTrue(result.isEmpty());
    }
}
