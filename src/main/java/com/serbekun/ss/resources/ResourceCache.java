package com.serbekun.ss.resources;

import java.nio.charset.Charset;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Thread-safe cache layer for resources.
 * Provides lazy loading via computeIfAbsent.
 */
public class ResourceCache {
    private final ResourceLoader loader;
    private final ConcurrentMap<String, byte[]> binaryCache = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, String> textCache = new ConcurrentHashMap<>();


    /**
     * Creates a new ResourceCache with the given ResourceLoader.
     *
     * @param loader the resource loader to use
     */
    public ResourceCache(ResourceLoader loader) {
        this.loader = loader;
    }

    /**
     * Gets binary data from cache, loading it lazily if not present.
     *
     * @param path the path to the resource
     * @return byte array or null if not found
     */
    public byte[] getBinary(String path) {
        byte[] cached = binaryCache.get(path);
        if (cached != null) {
            return cached;
        }

        byte[] data = loader.loadBinary(path);
        if (data != null) {
            binaryCache.put(path, data);
        }

        return data;
    }

    /**
     * Gets text data from cache, loading it lazily if not present.
     *
     * @param path the path to the resource
     * @param charset the charset to use
     * @return string or null if not found
     */
    public String getText(String path, Charset charset) {
        return textCache.computeIfAbsent(path, p -> loader.loadText(p, charset));
    }

    /**
     * Clears all cached resources.
     */
    public void clear() {
        binaryCache.clear();
        textCache.clear();
    }

    /**
     * Checks if a binary resource is cached.
     *
     * @param path the path to the resource
     * @return true if cached, false otherwise
     */
    public boolean isBinaryCached(String path) {
        return binaryCache.containsKey(path);
    }

    /**
     * Checks if a text resource is cached.
     *
     * @param path the path to the resource
     * @return true if cached, false otherwise
     */
    public boolean isTextCached(String path) {
        return textCache.containsKey(path);
    }

    /**
     * 
     * Check exist resource in jar file
     * 
     * @param path path to file
     * @return true exist, false don't exist
     */
    public boolean exists(String path) {
        return loader.exists(path);
    }
    
    // В ResourceCache
    public List<String> listResources(String basePath) {
        return loader.listResources(basePath);
    }
}