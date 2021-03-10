package de.dfki.et.tech4comp.rasa_client;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.dfki.et.tech4comp.converstation.annotation.AnnotationReader;
import de.dfki.util.JsonUtils;
import de.dfki.util.YamlUtils;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class NLUDataMerger {
    public static void main(String args[]) throws Exception {


//        String intentOriginal = "/Users/lihong/projects/DFKI_ET/tech4comp/rasa_2.1.0/server/data/nlu_de_default.yml";
        String intentOriginal = "/Users/lihong/projects/DFKI_ET/tech4comp/data/chatbot_biwi5_UL/intents/rasa2.x/intents+annoround1.yml";
        NLUDataTransfer.NLUData2 nluData = YamlUtils.read(intentOriginal, NLUDataTransfer.NLUData2.class);

        System.out.println("Read original nlu:" + nluData.nlu.size());
//        for (NLUDataTransfer.Intent intent : nluData.getNlu()) {
//            System.out.println("intent: " + intent.getIntent() + ", examples: " + intent.examples.split("\n").length);
////            System.out.println(intent.examples);
//        }

        String fileToAdd = "/Users/lihong/projects/DFKI_ET/tech4comp/data/chatbot_biwi5_UL" +
                "/log_2020_10-11/annotation/round2/annotated_leipzip/goldstandard.jsonl";
        List<AnnotationReader.IntentAnnotation> intentAnnotations =
                JsonUtils.readList(new File(fileToAdd), AnnotationReader.IntentAnnotation.class);
        Map<String, List<String>> exampleMap = new HashMap<>();
        intentAnnotations.forEach(i -> exampleMap.computeIfAbsent(i.getIntent(), k -> new ArrayList<>()).add(i.getText()));

        int updateIntents = 0, add = 0;
        for (NLUDataTransfer.Intent intent : nluData.getNlu()) {
            List<String> newExamples = exampleMap.get(intent.getIntent());
            if (newExamples == null) {
                continue;
            }
            updateIntents++;
            List<String> oldExamples = Arrays.stream(intent.examples.split("\n"))
                    .map(String::toLowerCase)
                    .collect(Collectors.toList());
            newExamples = newExamples.stream()
                    .filter(str -> !oldExamples.contains(str.toLowerCase(Locale.ROOT)))
                    .collect(Collectors.toList());
            if(!newExamples.isEmpty()) {
                add += newExamples.size();
                System.out.println("Added "+newExamples.size()+" examples to intent:"+intent.getIntent());
                intent.examples = intent.examples + "\n"
                        + newExamples.stream().map(s -> "- " + s).collect(Collectors.joining("\n"));
            }
        }

        System.out.println("update intents:" + updateIntents);
        System.out.println("added examples:"+add);
        ObjectMapper yamlMapper = NLUDataTransfer.getYamlMapper();
        FileUtils.write(new File("target/intent_new.yml"), yamlMapper.writeValueAsString(nluData), "utf8");
    }
}
