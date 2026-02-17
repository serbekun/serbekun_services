package Services;

import Core.IdeasTexts;

public class IdeasTextService {
    
    private IdeasTexts ideasTexts;

    public IdeasTextService(IdeasTexts ideasTexts) {
        this.ideasTexts = ideasTexts;
    }

    public void AddIdeaText(String text) {
        ideasTexts.AddIdeaText(text);
    }
}
