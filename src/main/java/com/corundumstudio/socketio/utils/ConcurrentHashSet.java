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
package com.corundumstudio.socketio.utils;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Adapter for ConcurrentHashMap, a-la jdk 1.6+ Collections.newSetFromMap(new ConcurrentHashMap....) idiom
 *
 * @author radu.cirstoiu@gmail.com
 */
public class ConcurrentHashSet<E> extends AbstractSet<E> implements Set<E> {
    private final Map<E, Boolean> backingMap;
    private final Set<E> backingMapSet;

    public ConcurrentHashSet() {
        backingMap = new ConcurrentHashMap<E, Boolean>();
        backingMapSet = backingMap.keySet();
    }

    public int size() {
        return backingMap.size();
    }

    public boolean isEmpty() {
        return backingMap.isEmpty();
    }

    public boolean contains(Object o) {
        return backingMap.containsKey(o);
    }

    public Iterator<E> iterator() {
        return backingMapSet.iterator();
    }

    public void clear() {
        backingMap.clear();
    }

    public Object[] toArray() {
        return backingMapSet.toArray();
    }

    public <T> T[] toArray(T[] a) {
        return backingMapSet.toArray(a);
    }

    public boolean add(E e) {
        return backingMap.put(e, Boolean.TRUE) == null;
    }

    public boolean remove(Object o) {
        return backingMap.remove(o) != null;
    }

    public boolean containsAll(Collection<?> c) {
        return backingMapSet.containsAll(c);
    }

    public boolean removeAll(Collection<?> c) {
        return backingMapSet.removeAll(c);
    }

    public boolean retainAll(Collection<?> c) {
        return backingMapSet.retainAll(c);
    }

    public String toString() {
        return backingMapSet.toString();
    }

    public int hashCode() {
        return backingMapSet.hashCode();
    }

    public boolean equals(Object o) {
        return o == this || backingMapSet.equals(o);
    }

}