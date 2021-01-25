package de.dfki.et.tech4comp.converstation.annotation;

import de.dfki.et.tech4comp.rasa_client.RasaNLUClient;
import de.dfki.et.tech4comp.rasa_client.RasaNLUResult;
import de.dfki.util.JsonUtils;
import org.annotation.brat.BratEntity;
import org.annotation.brat.BratSentence;
import org.annotation.brat.BratUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class EntityRecognizerTest {
    public static void main(String args[]) throws Exception {
        String goldFile = "/Users/lihong/projects/DFKI_ET/tech4comp/data/chatbot_biwi5/2020_10-11/" +
                "annotation/round1/annotated_leipzip_20201223/goldstandard.jsonl";
        List<AnnotationReader.IntentAnnotation> annotations =
                JsonUtils.readList(new File(goldFile), AnnotationReader.IntentAnnotation.class);
        System.out.println("Read message with intents: " + annotations.size());
        annotations.removeIf(a -> a.intent.equalsIgnoreCase("default"));
        System.out.println("Removed intent default. Get messages: " + annotations.size());

        RasaNLUClient rasaNLUClient = new RasaNLUClient();
        List<RasaNLUResult> results = new ArrayList<>();
        int entityMessage = 0;
        for (AnnotationReader.IntentAnnotation annotation : annotations) {
            RasaNLUResult result = rasaNLUClient.parse(annotation.text);
            result.setIntent(annotation.intent);
            result.setConfidence(1.0);
            result.setCorrect(true);
            results.add(result);

            if (!result.getEntities().isEmpty()) {
                entityMessage++;
            }
        }
        System.out.println("Found messages with entities: " + results.size());

        JsonUtils.writeList(new File("target/entity_results.jsonl"), results);

        List<BratSentence> bratSentences = new ArrayList<>();
        for (RasaNLUResult result : results) {
            BratSentence sentence = new BratSentence();
            sentence.setText(result.getText() + " {" + result.getIntent() + "}");
            sentence.setStartPosition(0);
            sentence.setEndPosition(sentence.getText().length());
            bratSentences.add(sentence);
            if (!result.getEntities().isEmpty()) {
                Set<String> savedEntities = new HashSet<>();
                for (RasaNLUResult.Entity entity : result.getEntities()) {
                    String id = entity.getStart() + "_" + entity.getEnd() + "_" + entity.getEntity();
                    if (savedEntities.contains(id)) {
                        continue;
                    }
                    savedEntities.add(id);
                    BratEntity bratEntity = new BratEntity(entity.getEntity(),
                            result.getText().substring(entity.getStart(), entity.getEnd()));
                    bratEntity.setStartPosition(entity.getStart());
                    bratEntity.setEndPosition(entity.getEnd());
                    sentence.getEntities().add(bratEntity);
                }
            }
        }

        BratUtils.saveAnnotation(bratSentences, "entity_anno", "\n\n", 50,
                new File("target/brat_output"), true);
    }
}
