/* (C) 2021 DragonSkulle */
package org.dragonskulle.network;

import java.util.AbstractQueue;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;

/**
 * The type Listenable queue.
 *
 * @param <E> the type parameter
 */
// from
// https://stackoverflow.com/questions/56336731/on-add-element-in-queue-call-a-listener-to-notify-queue-element-is-variable
public class ListenableQueue<E> extends AbstractQueue<E> {

    /**
     * The interface Listener.
     *
     * @param <E> the type parameter
     */
    interface Listener<E> {
        /**
         * On element added.
         *
         * @param element the element
         */
        void onElementAdded(E element);
    }

    /** The Delegate. */
    private final Queue<E> delegate; // backing queue
    /** The Listeners. */
    private final List<Listener<E>> listeners = new ArrayList<>();

    /**
     * Instantiates a new Listenable queue.
     *
     * @param delegate the delegate
     */
    public ListenableQueue(Queue<E> delegate) {
        this.delegate = delegate;
    }

    /**
     * Register listener listenable queue.
     *
     * @param listener the listener
     * @return the listenable queue
     */
    public ListenableQueue<E> registerListener(Listener<E> listener) {
        listeners.add(listener);
        return this;
    }

    @Override
    public boolean offer(E e) {
        // here, we put an element in the backing queue,
        // then notify listeners
        if (delegate.offer(e)) {
            listeners.forEach(listener -> listener.onElementAdded(e));
            return true;
        } else {
            return false;
        }
    }

    // following methods just delegate to backing instance
    @Override
    public E poll() {
        return delegate.poll();
    }

    @Override
    public E peek() {
        return delegate.peek();
    }

    @Override
    public int size() {
        return delegate.size();
    }

    @Override
    public Iterator<E> iterator() {
        return delegate.iterator();
    }
}
