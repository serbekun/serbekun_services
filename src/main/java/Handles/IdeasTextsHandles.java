package Handles;

import io.javalin.http.Context;

// import dto
import Handles.dto.IdeasTextsRequest;

// import services
import Services.IdeasTextsService;

public class IdeasTextsHandles {

    public static void Main(Context ctx, IdeasTextsService ideasTextService) {

        IdeasTextsRequest req = ctx.bodyAsClass(IdeasTextsRequest.class);

        ideasTextService.AddIdeaText(req.text);
        
        return;
    }
}