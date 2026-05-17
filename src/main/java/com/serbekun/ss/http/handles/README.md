**# How to Register a New Endpoint**

This directory contains the HTTP boilerplate for Javalin: routes are collected here, services are linked, and authorization is configured.

**## Current Project Path**

Currently, the application starts via:

1. `Main.java`
2. `InitHandles.java`
3. `EndpointAuthInitializer.java`

These are the classes that need to be updated if you are adding a new endpoint in the current architecture.

**## General Scheme**

Adding a new endpoint usually requires 3 steps:

1. Create an HTTP handler in `com.serbekun.ss.http.handles.v0` or another appropriate package.
2. Add the route to `InitHandles.java`.
3. If the endpoint must be protected, add it to `EndpointAuthInitializer.java`.

**## Step 1. Create the Handler**

Typically, an endpoint does not work directly with Javalin logic but calls a separate HTTP class.

Example:

```java
public class MyNewHttp {

    public void main(Context ctx) {
        ctx.result("ok");
    }
}
```

If the endpoint uses a service, pass it via the constructor, as done in:

- `ApiV0CipherAesHttp.java`
- `ApiV0CatalogsLinksHttp.java`

**## Step 2. Connect the Handler in `InitHandles`**

`InitHandles.initHandles(...)` collects all dependencies and registers `svr.get/post/put/delete(...)`.

What needs to be done:

1. Add the new handler to the `initHandles(...)` parameters if it uses a new service.
2. Create the handler object inside the method.
3. Add `svr.get(...)`, `svr.post(...)`, etc., with the required URL.

Example:

```java
MyNewHttp myNewHttp = new MyNewHttp(myService);
svr.get("/api/v0/my/new/endpoint", ctx -> myNewHttp.main(ctx));
```

If the endpoint has multiple HTTP methods, add them separately:

```java
svr.get("/api/v0/my/new/endpoint", ctx -> myNewHttp.main(ctx));
svr.post("/api/v0/my/new/endpoint", ctx -> myNewHttp.main(ctx));
```

**## Step 3. Register the Endpoint for Auth**

If the endpoint requires token verification, it must be added to `EndpointAuthInitializer.initHandlesAuthSetting(...)`.

Two things need to be done:

1. Create `Endpoint endpointMyNew = new Endpoint("/api/v0/my/new");`
2. Register it via `endpointRegistrar.register(endpointMyNew, true or false);`
3. Attach `svr.before(...)` to the same path so that the endpoint falls within the authorization context.

Example:

```java
Endpoint endpointMyNew = new Endpoint("/api/v0/my/new");
endpointRegistrar.register(endpointMyNew, true);

svr.before("/api/v0/my/new", ctx -> ctx.attribute("endpoint", endpointMyNew));
```

If the route is public, register it with `false`.

**## How to Choose the `requiresAuth` Value**

- `true` — the endpoint requires a token.
- `false` — the endpoint is open without authorization.

Currently, public routes in the project look like this:

- `/`
- `/static/v0/images/{name}`
- `/static/v0/json`
- `/static/v0/html/{name}`

Private routes:

- `/api/v0/cipher/aes`
- `/api/v0/cipher/aes/encrypt`
- `/api/v0/cipher/aes/decrypt`
- `/api/v0/catalogs/links`

**## Important Note on Matching Paths**

The URL in `svr.before(...)` and the URL in `svr.get/post/put/delete(...)` must be consistent.

If the endpoint has parameters, for example `/{uuid}`, usually:

1. `svr.get("/api/v0/items/{uuid}", ...)`
2. `svr.before("/api/v0/items/{uuid}", ...)`
3. `new Endpoint("/api/v0/items")` or another base key for auth, if the verification logic depends on the prefix.

See the current examples in:

- `LinkCatalogRoutes.java`
- `EndpointAuthInitializer.java`

**## If You Are Migrating Code to a New Layer**

There is already an alternative scheme in the repository:

- `RouteInitializer.java`
- `AuthInitializer.java`
- `StaticRoutes.java`
- `ApiV0Routes.java`

If the project begins using these instead of `InitHandles`, the instruction remains the same in essence:

1. Create a handler.
2. Connect it in the route aggregator.
3. Register the endpoint in the auth layer.

**## Checklist**

- Handler created.
- Route added to `InitHandles.java`.
- Endpoint registered in `EndpointAuthInitializer.java`.
- `requiresAuth = true` set for protected endpoints.
- Path in `before(...)` matches the actual route.