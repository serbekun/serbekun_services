package com.serbekun.http.handles;

import com.serbekun.service.resource.ResourcesService;
import com.serbekun.http.handles.v0.V0ImagesHttp;
import com.serbekun.http.handles.v0.V0JsonHttp;
import com.serbekun.http.handles.v0.V0LinksHttp;
import com.serbekun.http.handles.v0.V0PagesHttp;
import com.serbekun.service.auth.AuthService;
import com.serbekun.service.auth.Endpoints;
import com.serbekun.service.http.handles.v0.V0ApiJson;
import com.serbekun.service.http.handles.v0.V0Links;
import com.serbekun.service.http.handles.v0.V0Page;
import com.serbekun.service.http.handles.v0.V0ResourcesImages;

import io.javalin.Javalin;
import io.javalin.http.UnauthorizedResponse;

public class InitHandles {

    public void initHandles(Javalin svr, ResourcesService resourcesService, AuthService authService,
        // API handles services
        V0ApiJson v0ApiJson,
        V0ResourcesImages v0ResourcesImages,
        V0Page v0Page,
        V0Links v0Links

    ) {

        // init handles objects
        IndexHttp index = new IndexHttp(resourcesService);
        V0ImagesHttp Images = new V0ImagesHttp(resourcesService, v0ResourcesImages);
        V0JsonHttp json = new V0JsonHttp(v0ApiJson);
        V0PagesHttp pages = new V0PagesHttp(v0Page);
        V0LinksHttp links = new V0LinksHttp(v0Links);

        // tag endpoint for auth BEFORE global auth check
        svr.before("/", ctx -> ctx.attribute("endpoint", Endpoints.Index));
        svr.before("/v0/images/{name}", ctx -> ctx.attribute("endpoint", Endpoints.v0StaticImages));
        svr.before("/v0/api/json/{name}", ctx -> ctx.attribute("endpoint", Endpoints.v0ApiJson));
        svr.before("/v0/page/{name}", ctx -> ctx.attribute("endpoint", Endpoints.v0Page));

        // auth gate: runs after endpoint taggers above
        svr.before(ctx -> {
            Endpoints endpoint = ctx.attribute("endpoint");
            if (endpoint == null) {
                return;
            }

            String token = null;

            // 1. check Authorization header
            String authHeader = ctx.header("Authorization");
            if (authHeader != null) {
                if (authHeader.startsWith("Bearer ")) {
                    token = authHeader.substring(7).trim();
                } else if (authHeader.startsWith("Basic ")) {

                } else {

                    token = authHeader.trim();
                }
            }

            // if Authorization header doesn't exist use from url param value 
            if (token == null) {
                token = ctx.queryParam("token");

            }

            //    ?Authorization=Bearer eyJh...
            if (token == null) {
                String authInQuery = ctx.queryParam("Authorization");
                if (authInQuery != null && authInQuery.startsWith("Bearer ")) {
                    token = authInQuery.substring(7).trim();
                }
            }

            if (token == null || token.isBlank()) {
                throw new UnauthorizedResponse("Missing or invalid token");
            }

            if (!authService.checkAuth(endpoint, token)) {
                throw new UnauthorizedResponse("Unauthorized");
            }
            ctx.attribute("userToken", token);
        });

        svr.get("/", ctx -> index.main(ctx));

        svr.get("/v0/images/{name}", ctx -> Images.main(ctx));

        svr.get("/v0/api/json/{name}", ctx -> json.main(ctx));
        svr.get("/v0/page/{name}", ctx -> pages.main(ctx));

        svr.get("/v0/api/catalog/links", ctx -> links.main(ctx));
        svr.post("/v0/api/catalog/links", ctx -> links.main(ctx));
        svr.put("/v0/api/catalog/links/{uuid}", ctx -> links.main(ctx));
        svr.delete("/v0/api/catalog/links/{uuid}", ctx -> links.main(ctx));
    }
}
