package com.serbekun.ss.http.middleware;

import io.javalin.Javalin;

/**
 * Forces UTF-8 for every response, independent of the server's default
 * JVM charset. ctx.result(String) encodes with responseCharset(), which
 * falls back to Charset.defaultCharset() when no charset is set on the
 * response. On a server running under a C/POSIX locale (or a pre-Java-18
 * JRE) that default is US-ASCII, so non-ASCII characters (─ — → …,
 * cyrillic) get written as '?'. Setting it up front keeps static assets
 * and API JSON correct regardless of the host locale.
 */
public class CharsetMiddleware {

    public void register(Javalin server) {
        server.before(ctx -> ctx.res().setCharacterEncoding("UTF-8"));
    }
}
