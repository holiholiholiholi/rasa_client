package de.dfki.et.tech4comp.rasa_client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import lombok.Data;
import lombok.NonNull;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

public class RasaNLUClient {
    private HttpClient httpClient;
    private String host = "localhost";
    private int port = 5005;
    private static final String task = "model/parse";

    public RasaNLUClient() {
        httpClient = getHttpClient();
    }

    public RasaNLUClient(@NonNull final String host, @NonNull final int port) {
        this();
        this.host = host;
        this.port = port;

    }

    public RasaNLUResult parse(@NonNull final String text) throws IOException, InterruptedException {
        HttpResponse<String> response = httpClient.send(getHttpRequest(text), HttpResponse.BodyHandlers.ofString());
        return readResult(response);
    }

    public String getParseURI() {

        return "http://" + host + ":" + port + "/" + task;
    }


    private static HttpClient getHttpClient() {
        return HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofMinutes(10))
//                .proxy(ProxySelector.of(new InetSocketAddress("proxy.example.com", 80)))
//                .authenticator(Authenticator.getDefault())
                .build();
    }

    private HttpRequest getHttpRequest(@NonNull final String text) throws JsonProcessingException {
        ParseRequestBody requestBody = new ParseRequestBody(text);
        return HttpRequest.newBuilder()
                .uri(URI.create(getParseURI()))
                .timeout(Duration.ofMinutes(2))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(new ObjectMapper().writeValueAsString(requestBody)))
                .build();
    }

    private static RasaNLUResult readResult(@NonNull final HttpResponse<String> response) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(response.body());
        JsonNode result = root.get("intent");

        String text = root.get("text").asText();
        String intent = result.get("name").asText();
        double confidence = result.get("confidence").asDouble();

        RasaNLUResult nluResult = new RasaNLUResult(text, intent, confidence);


        JsonNode node = root.get("entities");
        if (node != null) {
            ObjectReader reader = mapper.readerFor(new TypeReference<List<RasaNLUResult.Entity>>() {
            });
            try {
                nluResult.getEntities().addAll(reader.readValue(node));
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        return nluResult;
    }

    @Data
    static class ParseRequestBody {
        final String text;
    }

    public static void main(String args[]) throws IOException, InterruptedException {
        String testString = "Was passiert mit meinen Daten?";
        RasaNLUClient rasaClient = new RasaNLUClient();
        RasaNLUResult result = rasaClient.parse(testString);
        System.out.println(new ObjectMapper().writeValueAsString(result));
    }
}
