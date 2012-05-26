/**
 * Copyright 2012 Nikita Koksharov
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
import java.util.Iterator;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class JoinIteratorsTest {

    @Test
    public void testIterator() {
        List<Integer> list1 = Arrays.asList(1, 2);
        List<Integer> list2 = Arrays.asList(3, 4);
        Iterator<Integer> i = list1.iterator();
        Iterator<Integer> i2 = list1.iterator();
        JoinIterator<Integer> iterators = new JoinIterator<Integer>(i, i2);

        List<Integer> mainList = new ArrayList<Integer>();
        for (; iterators.hasNext();) {
            Integer integer = iterators.next();
            mainList.add(integer);
        }
        Assert.assertEquals(list1.size() + list2.size(), mainList.size());
        mainList.removeAll(list1);
        mainList.removeAll(list2);
        Assert.assertTrue(mainList.isEmpty());

    }

}
