package com.kodiers.genaijavaspring.rag.service;

import com.kodiers.genaijavaspring.rag.config.data.RagConfigData;
import com.kodiers.genaijavaspring.rag.exception.RagException;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.util.Map;
import java.util.concurrent.*;

@Slf4j
@Component
public class PdfWatcherService implements SmartLifecycle {

    private final RagIngestionService ingestion;
    private final RagConfigData ragConfig;
    private final Executor watchLoop;
    private final ScheduledExecutorService debounce;

    private volatile boolean running = false;
    private java.nio.file.WatchService watchService;
    private final Map<Path, ScheduledFuture<?>> scheduled = new ConcurrentHashMap<>();


    public PdfWatcherService(RagIngestionService ingestion,
                             RagConfigData ragConfig,
                             @Qualifier("traceableWatchLoopExecutor") Executor watchLoop,
                             @Qualifier("traceableScheduledExecutorService") ScheduledExecutorService debounce) {
        this.ingestion = ingestion;
        this.ragConfig = ragConfig;
        this.watchLoop = watchLoop;
        this.debounce = debounce;
    }

    @Override
    public boolean isAutoStartup() {
        return false;
    }

    @Override
    public void start() {
        doStart();
    }

    private void doStart() {
        try {
            Path dir = resolveWatchedDir();       // the directory, not the *.pdf glob
            log.info("Starting PDF folder watcher for dir: {}", dir);
            this.watchService = dir.getFileSystem().newWatchService();
            dir.register(watchService,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_MODIFY,
                    StandardWatchEventKinds.ENTRY_DELETE);
            running = true;
            watchLoop.execute(() -> {
                try {
                    log.info("PDF watcher loop started for dir: {}", dir);
                    this.watchDir();
                } catch (Exception e) {
                    log.error("PDF watcher loop terminated with error", e);
                }
            });
        } catch (Exception e) {
            throw new RagException("Failed to start PDF watcher", e);
        }
    }

    private Path resolveWatchedDir() {
        // ragConfig.getPdf().getPath() is like: file:./rag/qa-over-internal-docs/*.pdf
        String pattern = ragConfig.getPdf().getPath().replaceFirst("^file:", "");
        Path path = Paths.get(pattern).toAbsolutePath().normalize();
        // If a glob is present, watch its parent; otherwise if a file is given, watch its parent
        if (pattern.contains("*") || !Files.isDirectory(path)) {
            return path.getParent();
        }
        return path;
    }

    private void watchDir() {
        while (running) {
            try {
                var key = watchService.take();
                Path dir = (Path) key.watchable();
                for (var event : key.pollEvents()) {
                    var kind = event.kind();
                    if (kind == StandardWatchEventKinds.OVERFLOW) {
                        continue;
                    }

                    @SuppressWarnings("unchecked")
                    Path relative = ((WatchEvent<Path>) event).context();
                    Path path = dir.resolve(relative);

                    // Only care about .pdf files
                    if (!relative.toString().toLowerCase().endsWith(".pdf")) {
                        continue;
                    }

                    // Debounce per file (e.g., 1s)
                    scheduled.compute(path, (p, pending) -> {
                        if (pending != null) pending.cancel(false);  // cancel any timer already queued for this file
                        return debounce.schedule(() -> handleEvent(kind, p), 1, TimeUnit.SECONDS); // schedule a new one and run only if no new events for 1s
                    });
                }
                boolean valid = key.reset();
                if (!valid) {
                    log.warn("WatchKey no longer valid for {}", dir);
                    break; // or try to re-register the directory
                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                log.warn("Watcher is interrupted!");
                break;
            } catch (Exception e) {
                log.warn("Error in watcher loop!", e);
            }
        }
    }

    private void handleEvent(WatchEvent.Kind<?> kind, Path path) {
        try {
            log.info("Received event:{} -> for file:{}", kind.name(), path.getFileName());
            if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                // remove from store
                ingestion.deleteBySource(path.getFileName().toString());
            } else {
                // create/modify → upsert this one file
                ingestion.upsertOneByPath(path);
            }
        } catch (Exception e) {
            log.error("Could not handle event:{} -> for file:{}", kind.name(), path.getFileName());
        }
    }

    @Override
    public void stop() {
        running = false;
        try {
            if (watchService != null) {
                watchService.close();
            }
        } catch (IOException ignored) {
        } finally {
            debounce.shutdownNow();
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }
}
