package ru.it_spectrum.ai.playwright.mcp.playwright;

import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class PlaywrightDispatcher {

    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "playwright-mcp-runtime");
        thread.setDaemon(false);
        return thread;
    });

    public <T> T call(Callable<T> callable) {
        try {
            return executor.submit(callable).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for Playwright runtime", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw new IllegalStateException("Playwright runtime call failed", cause);
        }
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdownNow();
    }
}
