package de.dfki.et.tech4comp.rasa_client;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import lombok.Data;
import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class RasaClientTest {
    public static void main(String[] args) throws IOException, InterruptedException {
//        String testString = "Was passiert mit meinen Daten?";
//        System.out.println(parse(testString));


        //test with training sentences!
//        String path = "target/nlu.yml";
//        String path = "/Users/lihong/projects/DFKI_ET/tech4comp/rasa_2.0.6/server/data/test_data.yml";
//        String path = "/Users/lihong/projects/DFKI_ET/tech4comp/rasa_2.0.6/server/data/training_data.yml";
//        String path = "/Users/lihong/projects/DFKI_ET/tech4comp/rasa_2.1.0/server/data/nlu_de_default.yml";
//        String path = "/Users/lihong/projects/DFKI_ET/tech4comp/rasa_2.1.0/server/data/nlu_big.yml";
//        String path = "/Users/lihong/projects/DFKI_ET/tech4comp/rasa_2.1.0/server/data/nlu_test2/test_data.yml";
        String path = "/Users/lihong/projects/DFKI_ET/tech4comp/rasa_2.3.0/server/data/intent/bitbot_ud_converted.yml";
        File nluFile = new File(path);
        ObjectMapper mapper = NLUDataTransfer.getYamlMapper();
        NLUDataTransfer.NLUData2 nluData = mapper.readValue(nluFile, NLUDataTransfer.NLUData2.class);
        System.out.println("read intents:" + nluData.nlu.stream().filter(i -> i.intent != null).count());

        RasaNLUClient rasaNLUClient = new RasaNLUClient();

        double threshold = 0.9;
        List<EvalResult> results = new ArrayList<>();
        for (NLUDataTransfer.Intent intent : nluData.nlu) {
            if (StringUtils.isEmpty(intent.intent)) {
                //not intent object
                continue;
            }
//            System.out.println("\n--------\nIntent: " + intent.intent);
            List<String> sentences = readSentences(intent.examples);
            System.out.println("Intent: "+intent.intent+", examples: "+sentences.size());
//            sentences.forEach(System.out::println);
            for (String sentence : sentences) {
                EvalResult evalResult = new EvalResult();
                evalResult.text = sentence;
                evalResult.definedIntent = intent.intent;

                RasaNLUResult result = rasaNLUClient.parse(sentence);
                evalResult.recogIntent = result.intent;
                evalResult.confidence = result.confidence;
                evalResult.correct = intent.intent.equalsIgnoreCase(result.intent);
                results.add(evalResult);
            }
        }
        System.out.println("Parsed sentences: " + results.size()
                + ", correct: " + results.stream().filter(EvalResult::isCorrect).count()
                + ", good (>=" + threshold + "):"
                + results.stream().filter(EvalResult::isCorrect).filter(r -> r.confidence >= threshold).count());
        System.out.println("lowest confidence (all):"
                + results.stream().mapToDouble(EvalResult::getConfidence).min().orElse(0));
        System.out.printf("lowest confidence (correct): %s%n",
                results.stream()
                        .filter(EvalResult::isCorrect)
                        .sorted(Comparator.comparingDouble(EvalResult::getConfidence))
                        .map(r -> r.getDefinedIntent() + "/" + r.getRecogIntent() + ", " + r.getConfidence() + ", " + r.getText())
                        .findFirst().orElse(null));

        System.err.println("Errors!");
        results.stream().filter(r -> !r.isCorrect())
                .sorted(Comparator.comparing(EvalResult::getDefinedIntent))
                .forEach(r -> System.err.println("-- " + r.getDefinedIntent() + " <-> "
                        + r.getRecogIntent() + ", " + r.confidence + ", " + r.getText()));


    }

    static List<String> readSentences(String examples) {
        String[] sentences = examples.split("\n");
        List<String> results = new ArrayList<>();
        for (String sentence : sentences) {
            sentence = sentence.trim();
            if (StringUtils.isEmpty(sentence)) {
                continue;
            }
            if (sentence.charAt(0) == '-') {
                sentence = sentence.substring(1);
            }
            sentence = sentence.replace('[', ' ')
                    .replace(']', ' ')
                    .replaceAll("\\{[^\\{\\}]*\\}", " ");
            results.add(sentence);

        }
        return results;
    }

    @Data
    static class EvalResult {
        String text;
        boolean correct;
        double confidence;
        String definedIntent;
        String recogIntent;
    }
}
