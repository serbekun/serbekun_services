package Handles;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import io.javalin.http.Context;

// import java.io.InputStream;
// import java.nio.charset.StandardCharsets;
// import java.util.HashMap;
// import java.util.Map;

public class Index {

    private static final Map<String, String> HTML_CACHE = new HashMap<>();

    /**
     * load files to jar
     */
    private static void load(String name) {
        try (InputStream is = Index.class
            .getClassLoader()
                .getResourceAsStream("html/" + name + ".html")) {
                    
                    if (is != null) {
                        HTML_CACHE.put(name,
                    new String(is.readAllBytes(), StandardCharsets.UTF_8));
                }
        } catch (Exception ignored) {}
    }

    /** resources in jar file */
    static {
        load("index");
    }
    
    /**
     * return index html
     */
    public void Main(Context ctx) {
        String html = HTML_CACHE.get("index");

        if (html == null) {
            ctx.status(404);
            return;
        }

        ctx.contentType("text/html; charset=utf-8");
        ctx.result(html);   
    }
}
