package com.serbekun.ss.repo.endpointaccesstokens;

import java.util.List;
import java.util.Map;

import com.serbekun.ss.service.auth.api.Endpoint;

/**
 * Interface for provide to {@link com.com.serbekun.ss.repo.links.LinksFileRepo}
 * File Repository class only read access
 */
public interface EndpointsAccessTokensReadInterface {
    /**
     * @return Return all endpoint token data
     */
    Map<String, List<Endpoint>> getEndpointsTokensData();
    
}