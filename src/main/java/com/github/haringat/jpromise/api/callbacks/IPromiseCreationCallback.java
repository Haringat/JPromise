package com.github.haringat.jpromise.api.callbacks;

import com.github.haringat.jpromise.api.IStateManager;
import com.github.haringat.jpromise.api.LockedException;

@FunctionalInterface
public interface IPromiseCreationCallback<T> {
    public void process(IStateManager<T> stateManager) throws LockedException;
}
