package com.github.haringat.jpromise.api.callbacks;

import com.github.haringat.jpromise.api.IPromise;

@FunctionalInterface
public interface ISuccessCallback<T, R> {
    public IPromise<R> proceed(T value);
}
