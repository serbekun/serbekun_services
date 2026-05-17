package com.serbekun.ss.core;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Token manager that binds a token to a resource identifier.
 * Each service/module should have its own instance of this class.
 */
public class LocalTokens {

    private final Map<String, String> tokens;
    private final Supplier<String> tokenSupplier;
    private final String prefix;

    /**
     * Creates a token manager with the default token generator (UUID)
     */
    public LocalTokens(String prefix) {
        this(new ConcurrentHashMap<>(), LocalTokens::defaultToken, prefix);
    }

    /**
     * Creates a token manager with an already existing map (e.g. loaded from database)
     */
    public LocalTokens(Map<String, String> initialTokens) {
        this(initialTokens, LocalTokens::defaultToken, "");
    }

    /**
     * Creates a token manager with an already existing map and a token prefix
     */
    public LocalTokens(Map<String, String> initialTokens, String prefix) {
        this(initialTokens, LocalTokens::defaultToken, prefix);
    }

    /**
     * Most flexible constructor — allows passing a custom map and a custom token generator
     */
    public LocalTokens(Map<String, String> initialTokens, Supplier<String> tokenSupplier, String prefix) {
        Map<String, String> safeInitial = (initialTokens == null) ? Map.of() : initialTokens;
        this.tokens = new ConcurrentHashMap<>(safeInitial);
        this.tokenSupplier = (tokenSupplier == null) ? LocalTokens::defaultToken : tokenSupplier;
        this.prefix = (prefix == null) ? "" : prefix;
    }

    /**
     * Generates a new unique token for the resource
     *
     * @param resourceId string identifier of the resource (must not be empty)
     * @return new token
     * @throws IllegalArgumentException if resourceId is null or empty/blank
     */
    public String generateToken(String resourceId) {
        if (resourceId == null || resourceId.isBlank()) {
            throw new IllegalArgumentException("resourceId must be non-empty");
        }
        String token;
        do {
            token = prefix + tokenSupplier.get();
        } while (tokens.putIfAbsent(token, resourceId) != null);
        return token;
    }

    /**
     * Generates a token using a UUID resource identifier
     */
    public String generateToken(UUID resourceId) {
        if (resourceId == null) {
            throw new IllegalArgumentException("resourceId must be non-null");
        }
        return generateToken(resourceId.toString());
    }

    /**
     * Checks whether the token grants access to the specified resource
     * 
     * @param token token 
     * @param resourceId resourceId string type
     * 
     * @return true if token has access false if not
     */
    public boolean hasAccess(String token, String resourceId) {
        if (token == null || token.isBlank() || resourceId == null || resourceId.isBlank()) {
            return false;
        }
        String stored = tokens.get(token);
        return resourceId.equals(stored);
    }

    /**
     * Checks access using a UUID resource identifier
     * 
     * @param token token 
     * @param resourceId resourceId UUID type
     * 
     * @return true if token has access false if not
     */
    public boolean hasAccess(String token, UUID resourceId) {
        if (resourceId == null) {
            return false;
        }
        return hasAccess(token, resourceId.toString());
    }

    /**
     * removes the token
     *
     * @return true — if the token existed and was removed
     */
    public boolean removeToken(String token) {
        return token != null && tokens.remove(token) != null;
    }

    /**
     * Returns a **copy** of all tokens (for logging, admin panel, debugging, etc.)
     * Important: returns a copy, not the original map
     */
    public Map<String, String> getAllTokensSnapshot() {
        return new ConcurrentHashMap<>(tokens);
    }

    /**
     * Clears all tokens
     */
    public void clear() {
        tokens.clear();
    }

    private static String defaultToken() {
        return UUID.randomUUID().toString();
    }
}
