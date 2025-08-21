/**
 * Copyright (c) 2012-2023 Nikita Koksharov
 *
 * <p>Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.corundumstudio.socketio.misc;

import java.util.Iterator;

public class CompositeIterator<T> implements Iterator<T> {

  private final Iterator<Iterator<T>> listIterator;
  private Iterator<T> currentIterator;

  public CompositeIterator(Iterator<Iterator<T>> listIterator) {
    this.currentIterator = null;
    this.listIterator = listIterator;
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
    return true; // can only be reached when currentIterator.hasNext() is true
  }

  @Override
  public T next() {
    hasNext();
    return currentIterator.next();
  }

  @Override
  public void remove() {
    currentIterator.remove();
  }
}
