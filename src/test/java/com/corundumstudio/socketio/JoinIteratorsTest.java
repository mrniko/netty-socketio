/**
 * Copyright (c) 2012-2023 Nikita Koksharov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.corundumstudio.socketio;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import com.corundumstudio.socketio.misc.CompositeIterable;

public class JoinIteratorsTest {

    @Test
    public void testIterator() {
        List<Integer> list1 = Arrays.asList(1, 2);
        List<Integer> list2 = Arrays.asList(3, 4);
        CompositeIterable<Integer> iterators = new CompositeIterable<Integer>(list1, list2);

        // for nomemory test
        for (Integer integer : iterators) {
        }

        List<Integer> mainList = new ArrayList<Integer>();
        for (Integer integer : iterators) {
            mainList.add(integer);
        }
        assertEquals(list1.size() + list2.size(), mainList.size());
        mainList.removeAll(list1);
        mainList.removeAll(list2);
        assertTrue(mainList.isEmpty());

    }

}
