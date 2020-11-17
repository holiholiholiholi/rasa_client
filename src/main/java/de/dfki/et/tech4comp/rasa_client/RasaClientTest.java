package de.dfki.et.tech4comp.rasa_client;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.List;

public class RasaClientTest {
    public static void main(String[] args) throws IOException, InterruptedException {
//        String testString = "Was passiert mit meinen Daten?";
//        System.out.println(parse(testString));
        File nluFile = new File("target/nlu.yml");
        ObjectMapper mapper = NLUDataTransfer.getYamlMapper();
        NLUDataTransfer.NLUData2 nluData = mapper.readValue(nluFile, NLUDataTransfer.NLUData2.class);
        System.out.println("read intents:" + nluData.nlu.size());
        int sentenceNr = 0, correct = 0, good = 0;
        double minConfidence = 1.0;
        double threshold = 0.9;
        for (NLUDataTransfer.Intent intent : nluData.nlu) {
            System.out.println("\n--------\nIntent: " + intent.intent);
            List<String> sentences = readSentences(intent.examples);
//            sentences.forEach(System.out::println);
            for (String sentence : sentences) {
                sentenceNr++;
                NLUResult result = parse(sentence);
                if (intent.intent.equalsIgnoreCase(result.intent)) {
                    correct++;
                    if (result.confidence >= threshold) {
                        good++;
                    }
                    if(result.confidence< minConfidence){
                        minConfidence = result.confidence;
                    }
                }
                else{
                    System.out.println("Error: "+sentence+"; "+result);
                }
            }
        }
        System.out.println("Parsed sentences: " + sentenceNr
                + ", correct: " + correct + ", good (>=" + threshold + "):" + good);
        System.out.println("lowest confidence: "+ minConfidence);
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


    public static NLUResult parse(@NonNull final String text) throws IOException, InterruptedException {
        HttpResponse<String> response = httpClient.send(getHttpRequest(text), HttpResponse.BodyHandlers.ofString());
        return readResult(response);
    }

    public static String getParseURI() {
        String host = "localhost";
        String port = "5005";
        String task = "model/parse";
        return "http://" + host + ":" + port + "/" + task;
    }

    static HttpClient httpClient = getHttpClient();

    static HttpClient getHttpClient() {
        return HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofMinutes(10))
//                .proxy(ProxySelector.of(new InetSocketAddress("proxy.example.com", 80)))
//                .authenticator(Authenticator.getDefault())
                .build();
    }

    static HttpRequest getHttpRequest(@NonNull final String text) throws JsonProcessingException {
        ParseRequestBody requestBody = new ParseRequestBody(text);
        return HttpRequest.newBuilder()
                .uri(URI.create(getParseURI()))
                .timeout(Duration.ofMinutes(2))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(new ObjectMapper().writeValueAsString(requestBody)))
                .build();
    }

    static NLUResult readResult(@NonNull final HttpResponse<String> response) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode result = mapper.readTree(response.body()).get("intent");
        NLUResult nluResult = new NLUResult();
        nluResult.intent = result.get("name").asText();
        nluResult.confidence = result.get("confidence").asDouble();
        return nluResult;
    }

    @Data
    static class NLUResult {
        String intent;
        double confidence;

        public String toString() {
            return "intent:" + intent + ", confidence:" + confidence;
        }
    }

    @Data
    static class ParseRequestBody {
        final String text;
    }
}
