package com.serbekun.ss.service.auth;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

import com.serbekun.ss.service.auth.api.Endpoint;
import com.serbekun.ss.service.auth.api.EndpointAuthProvider;
import com.serbekun.ss.service.auth.api.EndpointRegistrar;

/**
 * class for contain Map of <endpoint, requires auth>
 */
public class EndpointRegistry implements EndpointRegistrar, EndpointAuthProvider {

    /**
     * Endpoints map
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
