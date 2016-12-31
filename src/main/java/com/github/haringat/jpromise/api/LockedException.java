package com.github.haringat.jpromise.api;

public class LockedException extends IllegalStateException {
    public LockedException(PromiseState oldState, PromiseState newState) {
        super("Tried to change state from " + oldState.name() + " to " + newState.name() + ".");
    }
    public LockedException(PromiseState oldState) {
        super("Promise is already locked at state " + oldState.name() + ".");
    }
}
