package com.github.haringat.jpromise.api.callbacks;

@FunctionalInterface
public interface IProgressCallback {
    public void onProgress(double progress);
}
