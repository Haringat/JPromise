package com.github.haringat.jpromise.api.callbacks;

import com.github.haringat.jpromise.api.IPromise;

@FunctionalInterface
public interface IErrorCallback<T> {
    public IPromise<T> handle(Throwable cause);
}
