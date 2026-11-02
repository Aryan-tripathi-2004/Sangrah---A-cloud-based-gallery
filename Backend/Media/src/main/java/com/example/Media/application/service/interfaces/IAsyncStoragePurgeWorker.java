package com.example.Media.application.service.interfaces;

import java.util.List;

/**
 * Service contract for asynchronous physical storage purge operations.
 * Decouples sequential disk and remote storage I/O from HTTP worker threads and database transactions.
 */
public interface IAsyncStoragePurgeWorker {

    /**
     * Asynchronously purges a list of storage keys from the underlying storage provider.
     *
     * @param storageKeys list of storage keys / paths to purge
     */
    void purgeStorageKeysAsync(List<String> storageKeys);

    /**
     * Asynchronously purges a single storage key from the underlying storage provider.
     *
     * @param storageKey storage key / path to purge
     */
    void purgeStorageKeyAsync(String storageKey);
}
