package Core;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * core class that managing ideas texts
 */
public class IdeasText {
    
    private List<String> ideasTexts;
    private ObjectMapper mapper;

    private String ideasTextsFilePath;

    public void IdeasText(String ideasTextsFilePath) {

        this.ideasTexts = new ArrayList<>();
        this.mapper = new ObjectMapper();

        this.ideasTextsFilePath = ideasTextsFilePath;
        LoadFromFile();
    }

    public void AddIdeaText(String text) {
        ideasTexts.add(text);
        SaveToFile();
    }
    /**
     * Loads messages from the JSON file into memory
     */
    private synchronized void LoadFromFile() {
        File file = new File(this.ideasTextsFilePath);
        if (!file.exists() || file.length() == 0) {
            this.ideasTexts.clear();
            return;
        }

        try {
            // Read the whole file into a string.
            String json = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);

            // Deserialize the JSON array into List<String> using Jackson
            this.ideasTexts = mapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (IOException e) {
            System.err.println("FAILED TO LOAD FILE: " + this.ideasTextsFilePath);
            e.printStackTrace();
            this.ideasTexts.clear();
        }
    }

    /**
     * Saves current in-memory messages to the JSON file
     */
    private synchronized void SaveToFile() {
        try {
            // Ensure parent directories exist
            File file = new File(this.ideasTextsFilePath);
            if (file.getParentFile() != null) {
                Files.createDirectories(file.getParentFile().toPath());
            }

            mapper.writeValue(file, this.ideasTextsFilePath);
        } catch (IOException e) {
            System.err.println("FAILED TO SAVE FILE" + e.getMessage());
        }
    }



}
