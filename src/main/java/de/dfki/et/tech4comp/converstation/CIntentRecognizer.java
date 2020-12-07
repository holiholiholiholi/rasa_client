package de.dfki.et.tech4comp.converstation;

import de.dfki.et.tech4comp.rasa_client.RasaClientTest;
import de.dfki.util.JsonUtils;
import lombok.Data;
import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class CIntentRecognizer {
    public static void main(String args[]) throws Exception {
        File cFile = new File("target/conversations.jsonl");
        List<Conversation> conversations = JsonUtils.readList(cFile, Conversation.class);
        System.out.println("Read conversations: " + conversations.size());
        List<CNLUResult> results = new ArrayList<>();
        int userPoster = 0, noTextPoster = 0, botText = 0;
        int cCounter = 0;
        for (Conversation c : conversations) {
            cCounter++;
            for (int i = 0; i < c.messages.size(); i++) {
                Conversation.Message p = c.messages.get(i);
//                if(p.userId.equalsIgnoreCase(ChatLogReader.BOT_HASH)){
                if (ChatLogReader.BOT_HASH.contains(p.userId)) {
                    botText++;
                    continue;
                }
                userPoster++;
                if (StringUtils.isBlank(p.text) || isNumber(p.text) || toIgnore(p.text)) {
                    noTextPoster++;
                    continue;
                }
                RasaClientTest.NLUResult result = RasaClientTest.parse(p.text);
                CNLUResult cnluResult = new CNLUResult();
                cnluResult.confidence = result.getConfidence();
                cnluResult.intent = result.getIntent();
                cnluResult.conversationId = c.id;
                cnluResult.messageIndex = i;
                cnluResult.text = p.text;
                results.add(cnluResult);
            }
            if (cCounter % 50 == 0) {
                System.out.println("Parsed conversations ..." + cCounter);
//                break;
            }
        }
        System.out.println("Bot messages: "+botText);
        System.out.println("Read posters: " + userPoster);
        System.out.println("NO-Text posters: " + noTextPoster);
        System.out.println("Parsed Posters: " + results.size());
        JsonUtils.writeList(new File("target/conversation_nlu_results.jsonl"), results);
    }

    private static boolean toIgnore(String text) {
        String string = text.trim().toLowerCase();
        if (string.endsWith(".") || string.endsWith("?") || string.endsWith("!")) {
            string = string.substring(0, string.length() - 1);
        }
        if (string.startsWith("schreibaufgabe")) {
            int last = string.charAt(string.length() - 1);
            if (last >= 48 && last <= 57) {
//                System.out.println(text);
                return true;
            }
        }
        return false;
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
    public static class CNLUResult {
        String conversationId;
        int messageIndex;
        String text;
        String intent;
        double confidence;

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
