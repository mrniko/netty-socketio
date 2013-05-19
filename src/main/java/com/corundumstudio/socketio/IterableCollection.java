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

import java.util.AbstractCollection;
import java.util.Iterator;

public class IterableCollection<T> extends AbstractCollection<T> {

    private final Iterable<T> iterable;
    private final Iterable<T> sizeIterable;

    public IterableCollection(CompositeIterable<T> iterable) {
        this.iterable = iterable;
        this.sizeIterable = new CompositeIterable<T>(iterable);
    }

    @Override
    public Iterator<T> iterator() {
        return iterable.iterator();
    }

    @Override
    public int size() {
        Iterator<T> iterator = sizeIterable.iterator();
        int count = 0;
        while (iterator.hasNext()) {
          iterator.next();
          count++;
        }
        return count;
    }

}
