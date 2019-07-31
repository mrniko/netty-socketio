package com.corundumstudio.socketio.misc;

import java.util.Iterator;

public class CompositeIterator<T> implements Iterator<T> {

    private Iterator<Iterator<T>> listIterator;
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
        return currentIterator.hasNext();
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
