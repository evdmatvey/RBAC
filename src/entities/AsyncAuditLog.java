package entities;

import utils.BackgroundExecutor;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class AsyncAuditLog {
    private final AuditLog delegate; // Используем существующий AuditLog
    private final BlockingQueue<AuditEntry> queue;
    private final BackgroundExecutor executor;
    private volatile boolean running = true;
    private CompletableFuture<Void> consumerFuture;

    public AsyncAuditLog(BackgroundExecutor executor) {
        this.delegate = new AuditLog();
        this.queue = new LinkedBlockingQueue<>();
        this.executor = executor;
        startConsumer();
    }

    private void startConsumer() {
        consumerFuture = executor.submit(() -> {
            while (running || !queue.isEmpty()) {
                try {
                    AuditEntry entry = queue.poll(100, TimeUnit.MILLISECONDS);
                    if (entry != null) {
                        // Используем существующий метод log для добавления в delegate
                        delegate.log(entry.action(), entry.performer(), entry.target(), entry.details());
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
    }

    // Асинхронное логирование - не блокирует вызывающий поток
    public CompletableFuture<Void> log(String action, String performer, String target, String details) {
        if (action == null || performer == null) {
            throw new IllegalArgumentException("Action and performer cannot be null");
        }

        String timestamp = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String normalizedTarget = target != null ? target : "";
        String normalizedDetails = details != null ? details : "";

        AuditEntry entry = new AuditEntry(timestamp, action, performer, normalizedTarget, normalizedDetails);

        return executor.submit(() -> {
            try {
                if (!queue.offer(entry, 1, TimeUnit.SECONDS)) {
                    // Если очередь переполнена, логируем синхронно
                    delegate.log(action, performer, target, details);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                delegate.log(action, performer, target, details);
            }
        });
    }

    // Делегируем методы существующего AuditLog
    public List<AuditEntry> getAll() {
        return delegate.getAll();
    }

    public List<AuditEntry> getByPerformer(String performer) {
        return delegate.getByPerformer(performer);
    }

    public List<AuditEntry> getByAction(String action) {
        return delegate.getByAction(action);
    }

    public void printLog() {
        delegate.printLog();
    }

    public void saveToFile(String filename) {
        delegate.saveToFile(filename);
    }

    public void shutdown() {
        running = false;
        if (consumerFuture != null) {
            consumerFuture.join();
        }
    }
}