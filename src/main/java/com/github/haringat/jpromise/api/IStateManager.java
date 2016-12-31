package com.github.haringat.jpromise.api;

public interface IStateManager<T> {
    public void resolve(T value) throws LockedException;
    public void reject(Throwable cause) throws LockedException;
    public void notify(double progress) throws LockedException;
    public void cancel();

}
