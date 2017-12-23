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

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class HookMetadataTest {

    private HookMetadata.LexicographicalComparator<String, List<String>> comparator = new HookMetadata.LexicographicalComparator<>();

    @Test
    void testComparatorEqualSize() {
        List<String> list1 = Arrays.asList("a", "b", "c", "e");
        List<String> list2 = Arrays.asList("a", "b", "d", "e");
        assertEquals(-1, comparator.compare(list1, list2));
    }

    @Test
    void testComparatorDifferentSize() {
        List<String> list1 = Arrays.asList("a", "b", "c");
        List<String> list2 = Arrays.asList("a", "b");
        assertEquals(1, comparator.compare(list1, list2));
    }

    @Test
    void testComparatorEqual() {
        List<String> list1 = new ArrayList<>();
        List<String> list2 = new ArrayList<>();
        assertEquals(0, comparator.compare(list1, list2));
    }
}
