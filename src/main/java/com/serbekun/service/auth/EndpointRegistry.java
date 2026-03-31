package com.serbekun.service.auth;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.serbekun.service.auth.api.*;

/**
 * class for contain Map of <endpoint, requires auth>
 */
public class EndpointRegistry implements EndpointRegistrar, EndpointAuthProvider {
    

    /**
     * endpoints map
     */
    private static final Map<Endpoint, Boolean> endpoints = new ConcurrentHashMap<>();
   
    public EndpointRegistry() {}
   
    /**
     * 
     * register new endpoint for auth can be check endpoint need auth or not
     * 
     * @param endpoint endpoint name
     * @param requiresAuth true endpoint requires auth false not requires auth
     */
    @Override
    public void register(Endpoint endpoint, boolean requiresAuth) {
        endpoints.put(endpoint, requiresAuth);
    }

    /**
     * 
     * return requires Auth
     * 
     * @return true if required, false if not
     */
    @Override
    public boolean requiresAuth(Endpoint endpoint) {
        return endpoints.getOrDefault(endpoint, false);
    }
}
