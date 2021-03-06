package com.github.haringat.jpromise;

import com.github.haringat.jpromise.api.IPromise;
import com.github.haringat.jpromise.api.IStateManager;
import com.github.haringat.jpromise.api.LockedException;
import com.github.haringat.jpromise.api.callbacks.*;
import com.github.haringat.jpromise.api.PromiseState;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class Promise<T> implements IPromise<T> {

    private PromiseState state;
    private T value;
    private Throwable cause = null;
    private Thread taskThread;
    private boolean cancelled = false;
    private List<ISuccessCallback<T,?>> resolveListeners = new ArrayList<>();
    private List<IErrorCallback<T>> rejectListeners = new ArrayList<>();
    private List<ICleanUpCallback> cancelListeners = new ArrayList<>();
    private List<IProgressCallback> progressListeners = new ArrayList<>();

    public Promise(final IPromiseCreationCallback<T> deferredTask) {
        final Promise<T> _this = this;
        this.state = PromiseState.PENDING;
        this.taskThread = new Thread(() -> {
            deferredTask.process(new IStateManager<T>() {
                @Override
                public void resolve(T value) {
                    //noinspection SynchronizeOnNonFinalField
                    synchronized (_this.state) {
                        if (_this.state != PromiseState.PENDING) {
                            throw new LockedException(_this.state, PromiseState.RESOLVED);
                        }
                        _this.state = PromiseState.RESOLVED;
                        _this.value = value;
                        for (ISuccessCallback<T, ?> listener : _this.resolveListeners) {
                            listener.proceed(value);
                        }
                    }
                }

                @Override
                public void reject(Throwable cause) {
                    //noinspection SynchronizeOnNonFinalField
                    synchronized (_this.state) {
                        if (_this.state != PromiseState.PENDING) {
                            throw new LockedException(_this.state, PromiseState.REJECTED);
                        }
                        _this.state = PromiseState.REJECTED;
                        _this.cause = cause;
                        for (IErrorCallback<T> listener : _this.rejectListeners) {
                            listener.handle(cause);
                        }
                    }
                }

                @Override
                public void notify(double progress) {
                    //noinspection SynchronizeOnNonFinalField
                    synchronized (_this.state) {
                        if (_this.state != PromiseState.PENDING) {
                            throw new LockedException(_this.state);
                        }
                        for (IProgressCallback listener : _this.progressListeners) {
                            listener.onProgress(progress);
                        }
                    }
                }

                @Override
                public void cancel() {
                    _this.cancel();
                }
            });
        });
        this.taskThread.start();
    }

    private Promise(Throwable cause) {
        this.state = PromiseState.REJECTED;
        this.cause = cause;
    }

    private Promise(T value) {
        this.state = PromiseState.RESOLVED;
        this.value = value;
    }

    @Override
    public T await() throws InterruptedException {
        this.taskThread.join();
        return this.value;
    }

    @Override
    public <R> IPromise<R> then(ISuccessCallback<T, R> onSuccess) {
        return this.then(onSuccess, Promise::reject, progress -> {}, () -> {});
    }

    @Override
    public <R> IPromise<R> then(ISuccessCallback<T, R> onSuccess, IErrorCallback<R> onError) {
        return this.then(onSuccess, onError, progress -> {}, () -> {});
    }

    @Override
    public <R> IPromise<R> then(ISuccessCallback<T, R> onSuccess, IErrorCallback<R> onError, IProgressCallback onProgress) {
        return this.then(onSuccess, onError, progress -> {}, () -> {});
    }

    @SuppressWarnings("unchecked")
    @Override
    public <R> IPromise<R> then(ISuccessCallback<T, R> onSuccess, IErrorCallback<R> onError, IProgressCallback onProgress, ICleanUpCallback onCancel) {
        switch (this.state) {
            case RESOLVED:
                return new Promise(stateManager -> {
                    try {
                        onSuccess.proceed(this.value).then((value1) -> {
                            stateManager.resolve(value1);
                            return Promise.resolve(value1);
                        }, (cause1) -> {
                            stateManager.reject(cause1);
                            return Promise.reject(cause1);
                        }, stateManager::notify, stateManager::cancel);
                    } catch(Throwable throwable) {
                        stateManager.reject(throwable);
                    }
                });
            case REJECTED:
                return new Promise(stateManager -> {
                    try {
                        onError.handle(this.cause).then((value1) -> {
                            stateManager.resolve(value1);
                            return Promise.resolve(value1);
                        }, (cause1) -> {
                            stateManager.reject(cause1);
                            return Promise.reject(cause1);
                        }, stateManager::notify, stateManager::cancel);
                    } catch(Throwable throwable) {
                        stateManager.reject(throwable);
                    }
                });
            case CANCELLED:
                onCancel.cleanup();
                return new Promise<>(IStateManager::cancel);
            case PENDING:
                return new Promise<>((IStateManager<R> stateManager) -> {
                    this.resolveListeners.add((T value) -> onSuccess.proceed(value).then((value1) -> {
                        stateManager.resolve(value1);
                        return Promise.resolve(value1);
                    }, cause -> {
                        stateManager.reject(cause);
                        return Promise.reject(cause);
                    }, stateManager::notify, stateManager::cancel));
                    this.rejectListeners.add((cause) -> onError.handle(cause).then((ISuccessCallback) (value) -> {
                        stateManager.resolve((R) value);
                        return Promise.resolve((R) value);
                    }, (Throwable cause1) -> {
                        stateManager.reject(cause1);
                        return Promise.reject(cause1);
                    }, stateManager::notify, stateManager::cancel));
                    this.progressListeners.add(progress -> {
                        onProgress.onProgress(progress);
                        stateManager.notify(progress);
                    });
                    this.cancelListeners.add(() -> {
                        onCancel.cleanup();
                        stateManager.cancel();
                    });
                });
            default:
                return Promise.reject(new Exception("Unknown promise state found."));
        }
    }

    @Override
    public <R> IPromise<R> onError(IErrorCallback<R> onError) {
        return this.then((value) -> {
            return (Promise<R>) Promise.resolve(value);
        }, Promise::reject, progress -> {}, () -> {});
    }

    @Override
    public IPromise<T> onCancel(ICleanUpCallback cleanUpCallback) {
        return this.then(Promise::resolve, Promise::reject, (progress) -> {}, cleanUpCallback);
    }

    @Override
    public IPromise<T> onProgress(IProgressCallback onProgress) {
        return this.then(Promise::resolve, Promise::reject, onProgress, () -> {});
    }

    @Override
    public IPromise<T> cancel() throws LockedException {
        //noinspection SynchronizeOnNonFinalField
        synchronized (this.state) {
            if (this.state != PromiseState.PENDING) {
                throw new LockedException(this.state, PromiseState.REJECTED);
            }
            this.state = PromiseState.REJECTED;
            this.cancelled = true;
            for(ICleanUpCallback listener: this.cancelListeners) {
                listener.cleanup();
            }
            return this;
        }
    }

    public T getValue() {
        return this.value;
    }

    public PromiseState getState() {
        return this.state;
    }

    public static <R> IPromise<R> resolve(R value) {
        return new Promise<>(value);
    }

    public static IPromise<Void> resolve() {
        return Promise.resolve(null);
    }

    public static <R> IPromise<R> reject(Throwable cause) {
        return new Promise<>(cause);
    }

    public static <R> IPromise<List<R>> all(IPromise<R>[] promises) {
        return new Promise<>(stateManager -> {
            List<R> values = new ArrayList<>();
            for (IPromise<R> promise: promises) {
                promise.then(value -> {
                    synchronized (values) {
                        values.add(value);
                        if (values.size() == promises.length) {
                            stateManager.resolve(values);
                        }
                    }
                    return Promise.resolve(value);
                }, cause -> {
                    stateManager.reject(cause);
                    return Promise.reject(cause);
                });
            }
        });
    }

    public static <R> IPromise<List<R>> all(Collection<IPromise<R>> promises) {
        //noinspection unchecked
        return Promise.all((IPromise<R>[]) promises.toArray());
    }

    public static <R> IPromise<R> race(IPromise<R>[] promises) {
        return new Promise<>(stateManager -> {
            for (IPromise<R> promise: promises) {
                promise.then(value -> {
                    //noinspection EmptyCatchBlock
                    try {
                        stateManager.resolve(value);
                    } catch (LockedException e) {}
                    return Promise.resolve(value);
                }, cause -> {
                    //noinspection EmptyCatchBlock
                    try {
                        stateManager.reject(cause);
                    } catch (LockedException e) {}
                    return Promise.reject(cause);
                });
            }
        });
    }

    public static <R> IPromise<R> race(Collection<IPromise<R>> promises) {
        //noinspection unchecked
        return Promise.race((IPromise<R>[]) promises.toArray());
    }

}
