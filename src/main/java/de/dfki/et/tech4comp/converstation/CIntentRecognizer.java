package de.dfki.et.tech4comp.converstation;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.dfki.et.tech4comp.rasa_client.RasaNLUClient;
import de.dfki.et.tech4comp.rasa_client.RasaNLUResult;
import de.dfki.util.JsonUtils;
import lombok.Data;
import lombok.NonNull;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class CIntentRecognizer {
    public static void main(String args[]) throws Exception {
        File cFile = new File("target/conversations.jsonl");
        List<Conversation> conversations = JsonUtils.readList(cFile, Conversation.class);
        Set<String> ignoredText = new HashSet<>();
        System.out.println("Read conversations: " + conversations.size());

        List<CNLUResult> results = new ArrayList<>();
        int userPoster = 0, noTextPoster = 0, botText = 0, langText = 0;
        int cCounter = 0;
        RasaNLUClient rasaNLUClient = new RasaNLUClient();
        for (Conversation c : conversations) {
            cCounter++;
            String lastIntent = null;
            for (int i = 0; i < c.messages.size(); i++) {
                Conversation.Message p = c.messages.get(i);
//                if(p.userId.equalsIgnoreCase(ChatLogReader.BOT_HASH)){
                if (ChatLogReader.BOT_HASH.contains(p.userId)) {
                    botText++;
                    continue;
                }
                userPoster++;
                if (StringUtils.isBlank(p.text)) {
                    noTextPoster++;
                    continue;
                }

                if (isNumber(p.text) || toIgnore(p.text) || toIgnore2(p.text, lastIntent)) {
                    noTextPoster++;
//                    System.out.println("Ignored(Task Number): "+ p.text);
                    ignoredText.add(p.text.trim());
                    continue;
                }
                if (p.text.trim().split(" +").length >= 50) {
                    langText++;
                    continue;
                }

                CNLUResult cnluResult = new CNLUResult(rasaNLUClient.parse(p.text));
                cnluResult.conversationId = c.id;
                cnluResult.messageIndex = i;
                results.add(cnluResult);

                lastIntent = cnluResult.getIntent();
            }
            if (cCounter % 50 == 0) {
                System.out.println("Parsed conversations ..." + cCounter);
//                break;
            }
        }
        System.out.println("Bot messages: " + botText);
        System.out.println("Read posters: " + userPoster);
        System.out.println("No-Text/Task Number posters: " + noTextPoster);
        System.out.println("Lang Text posters:" + langText);
        System.out.println("Parsed Posters: " + results.size());
        JsonUtils.writeList(new File("target/conversation_nlu_results.jsonl"), results);
        FileUtils.writeLines(new File("target/ignoredMessages.txt"),
                ignoredText.stream().sorted().collect(Collectors.toList()));
    }

    private static boolean toIgnore(String text) {
        String string = text.trim().toLowerCase();
        if (string.endsWith(".") || string.endsWith("?") || string.endsWith("!")) {
            string = string.substring(0, string.length() - 1);
        }
        if (string.startsWith("schreibaufgabe") || string.startsWith("text")) {
            int last = string.charAt(string.length() - 1);
            if (last >= 48 && last <= 57) {
//                System.out.println(text);
                return true;
            }
        }
        return false;
    }

    private static boolean toIgnore2(@NonNull final String text, final String lastIntent) {
        if (StringUtils.isEmpty(lastIntent)) {
            return false;
        }
        String tmp = text.replaceAll("[^0-9]", "");
        return lastIntent.contains("showtasks") && StringUtils.isNotEmpty(tmp);
    }

    public static boolean isNumber(@NonNull final String text) {
        try {
            int i = Integer.parseInt(text);
            return true;
        } catch (Exception e) {

        }
        return false;
    }

    @Data
    public static class CNLUResult extends RasaNLUResult {
        String conversationId;
        int messageIndex;

        public CNLUResult(@JsonProperty("text") final String text,
                          @JsonProperty("intent") final String intent,
                          @JsonProperty("confidence") final double confidence) {
            super(text, intent, confidence);
        }

        public CNLUResult(final RasaNLUResult r) {
            this(r.getText(), r.getIntent(), r.getConfidence());
            this.intentSuggestion = r.getIntentSuggestion();
            this.correct = r.isCorrect();
            this.entities.addAll(r.getEntities());
        }

        @Override
        public int hashCode() {
            return text.trim().toLowerCase().hashCode() + intent.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof CNLUResult)) {
                return false;
            }
            if (this == o) {
                return true;
            }
            if (null == text || null == intent) {
                return false;
            }
            return text.trim().equalsIgnoreCase(((CNLUResult) o).text.trim()) &&
                    intent.equals(((CNLUResult) o).intent);
        }
    }
}
