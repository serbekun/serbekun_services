package Services;

import Core.IdeasTexts;

public class IdeasTextsService {
    
    private IdeasTexts ideasTexts;

    public IdeasTextsService(IdeasTexts ideasTexts) {
        this.ideasTexts = ideasTexts;
    }

    public void AddIdeaText(String text) {
        ideasTexts.AddIdeaText(text);
    }
}
