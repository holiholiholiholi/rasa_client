package de.dfki.et.tech4comp.converstation.anaylzer;

import de.dfki.et.tech4comp.converstation.CIntentRecognizer;
import de.dfki.et.tech4comp.converstation.Conversation;
import de.dfki.util.JsonUtils;
import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.CoreSentence;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

@Slf4j
public class SentenceSplitter {
    static StanfordCoreNLP pipeline = getPipeline();

    public static void main(String args[]) throws Exception {
//        File dir = new File("/Users/lihong/projects/DFKI_ET/tech4comp/data/chatbot_biwi5/2020_10-11");
        File dir = new File("target");
        List<CIntentRecognizer.CNLUResult> cnluResults = JsonUtils.readList(new File(dir, "conversation_nlu_results_wo_duplicates.jsonl"), CIntentRecognizer.CNLUResult.class);
        System.out.println("Read cnlu results: " + cnluResults.size());

        cnluResults.removeIf(c -> c.getText().split(" +").length > 30);
        System.out.println("Removed lang texts! Remained: " + cnluResults.size());

        System.out.println("To parsed messages " + cnluResults.size());
        List<CIntentRecognizer.CNLUResult> singleSentence = new ArrayList<>();
        List<MultiSMessage> multiSentences = new ArrayList<>();
        for (CIntentRecognizer.CNLUResult result : cnluResults) {
            List<Sentence> sentences = getSentences(result.getText());
            if (sentences.size() > 1) {
                MultiSMessage message = new MultiSMessage();
                message.conversationId = result.getConversationId();
                message.messageIndex = result.getMessageIndex();
                message.text = result.getText();
                message.sentences = sentences;
                multiSentences.add(message);
            } else {
                singleSentence.add(result);
            }
        }
        System.out.println("get messages with >=2 sentences: " + multiSentences.size());
        JsonUtils.writeList(new File("target/conversation_nlu_results_wo_duplicates_singleSentence.jsonl"), singleSentence);
        JsonUtils.writeList(new File("target/message_w_multisentences.jsonl"), multiSentences);
    }

    private static List<Sentence> getSentences(final String message) {
        String text = message.replaceAll("[0-9].", "  ");

        CoreDocument document = new CoreDocument(text);
        pipeline.annotate(document);
        List<CoreSentence> sentences = document.sentences();
        return sentences.stream()
                .map(s -> new Sentence(s.charOffsets().first,
                        s.charOffsets().second,
                        message.substring(s.charOffsets().first, s.charOffsets().second)))
                .collect(Collectors.toList());
    }

    public static StanfordCoreNLP getPipeline() {
        Properties props = new Properties();
        props.setProperty("tokenize.language", "de");
        // set the list of annotators to run
        props.setProperty("annotators", "tokenize,ssplit");
        return new StanfordCoreNLP(props);
    }

    @Data
    static class MultiSMessage {
        String conversationId;
        int messageIndex;
        String text;
        List<Sentence> sentences;
    }

    @Data
    static class Sentence {
        final int start;
        final int end;
        final String text;
    }
}
