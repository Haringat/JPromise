package com.github.haringat.jpromise.api;

import com.github.haringat.jpromise.api.callbacks.ICleanUpCallback;
import com.github.haringat.jpromise.api.callbacks.IErrorCallback;
import com.github.haringat.jpromise.api.callbacks.IProgressCallback;
import com.github.haringat.jpromise.api.callbacks.ISuccessCallback;

public interface IPromise<T> {
    public T await() throws InterruptedException;
    public <R> IPromise<R> then(ISuccessCallback<T, R> onSuccess);
    public <R> IPromise<R> then(ISuccessCallback<T, R> onSuccess, IErrorCallback<R> onError);
    public <R> IPromise<R> then(ISuccessCallback<T, R> onSuccess, IErrorCallback<R> onError, IProgressCallback onProgress);
    public <R> IPromise<R> then(ISuccessCallback<T, R> onSuccess, IErrorCallback<R> onError, IProgressCallback onProgress, ICleanUpCallback onCancel);
    public <R> IPromise<R> onError(IErrorCallback<R> onError);
    public T getValue();
    public PromiseState getState();
    public IPromise<T> onCancel(ICleanUpCallback cleanUpCallback);
    public IPromise<T> onProgress(IProgressCallback onProgress);
    public IPromise<T> cancel();
}
