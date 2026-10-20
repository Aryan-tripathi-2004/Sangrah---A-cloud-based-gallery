package com.example.Media.application.service.interfaces;

/**
 * Service contract for periodic garbage collection of soft-deleted media assets.
 * Acts as a resilient fallback for in-memory asynchronous worker failures, ensuring
 * physical storage binaries are purged even if JVM crashes or task queues dropped tasks.
 */
public interface IMediaStorageGarbageCollectionService {

    /**
     * Scheduled job execution that sweeps soft-deleted media assets past retention window
     * and physically purges their underlying files from storage.
     */
    void purgeOrphanedStorageFiles();

    /**
     * Executes an on-demand garbage collection sweep and returns the total number of
     * storage keys successfully purged.
     *
     * @return count of storage keys purged
     */
    int sweepOrphanedStorageFiles();
}
