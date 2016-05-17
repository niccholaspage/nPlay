package com.nicholasnassar.nplay;

public abstract class AsyncConstruction<E> {
    private E next;

    public AsyncConstruction() {
        next = create();
    }

    public E get() {
        if (next == null) {
            return create();
        } else {
            E ret = next;

            next = null;

            new Thread(() -> next = create()).start();

            return ret;
        }
    }

    public abstract E create();
}
