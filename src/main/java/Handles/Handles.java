package Handles;

import io.javalin.Javalin;

import Services.IdeasTextsService;

public class Handles {
    
    /**
     * Init server api and resources endpoints 
    */
    public static void InitHandles(Javalin svr, IdeasTextsService ideasTextsService) {

        svr.get("/", ctx -> new Index().Main(ctx));
        svr.post("/v0/api/ideas_text", ctx -> IdeasTextsHandles.Main(ctx, ideasTextsService));

    }

}
