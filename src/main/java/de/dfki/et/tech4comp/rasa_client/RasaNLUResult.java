package de.dfki.et.tech4comp.rasa_client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@RequiredArgsConstructor
public class RasaNLUResult {

    @NonNull
    protected String text;
    @NonNull
    protected String intent;
    @NonNull
    protected double confidence;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    protected final List<Entity> entities = new ArrayList<>();

    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    protected boolean correct;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    protected String intentSuggestion;

//    @JsonInclude(JsonInclude.Include.NON_EMPTY)
//    private String id;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Entity {
        String entity;
        int start;
        int end;
        double confidence_entity;
        String value;
    }
}
