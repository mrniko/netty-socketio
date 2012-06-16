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
import java.util.Iterator;
import java.util.List;

public class CompositeIterable<T> implements Iterable<T>, Iterator<T> {

    private List<Iterable<T>> iterablesList;
    private Iterable<T>[] iterables;

    private Iterator<Iterator<T>> listIterator;
    private Iterator<T> currentIterator;

    public CompositeIterable(List<Iterable<T>> iterables) {
        this.iterablesList = iterables;
    }

    public CompositeIterable(Iterable<T> ... iterables) {
        this.iterables = iterables;
    }

    @Override
    public Iterator<T> iterator() {
        List<Iterator<T>> iterators = new ArrayList<Iterator<T>>();
        if (iterables != null) {
            for (Iterable<T> iterable : iterables) {
                iterators.add(iterable.iterator());
            }
        } else {
            for (Iterable<T> iterable : iterablesList) {
                iterators.add(iterable.iterator());
            }
        }
        listIterator = iterators.iterator();
        return this;
    }

    @Override
    public boolean hasNext() {
        if (currentIterator == null || !currentIterator.hasNext()) {
            while (listIterator.hasNext()) {
                Iterator<T> iterator = listIterator.next();
                if (iterator.hasNext()) {
                    currentIterator = iterator;
                    return true;
                }
            }
            return false;
        }
        return currentIterator.hasNext();
    }

    @Override
    public T next() {
        return currentIterator.next();
    }

    @Override
    public void remove() {
        currentIterator.remove();
    }

}
