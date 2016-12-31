package com.github.haringat.jpromise;

import org.junit.Test;
import static org.assertj.core.api.Assertions.*;

public class PromiseSpec {
    @Test(timeout = 10000L)
    public void async() throws Throwable {
        // hacky, but JUnit does not natively support asynchronous tests...
        final Throwable[] t = new Throwable[1];
        Thread main = Thread.currentThread();
        new Promise<String>((stateManager) -> {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                stateManager.reject(e);
            }
            stateManager.resolve("Hallo12");
        }).then((String s) -> {
            try {
                assertThat(s).matches("Hallo12");
                main.interrupt();
            } catch(Throwable throwable) {
                t[0] = throwable;
                main.interrupt();
            }
            return Promise.resolve();
        });
        try {
            //noinspection InfiniteLoopStatement
            while (true) {
                Thread.sleep(100);
            }
        } catch (InterruptedException e) {
            if (t[0] != null) {
                throw t[0];
            }
        }
    }

    @Test
    public void chain() {
        Promise.resolve(12).then((Integer value) -> {
            return Promise.resolve("Hallo " + value);
        }).then(value -> {
            fail("aaa");
            return null;
        });

    }
}
