package de.dfki.et.tech4comp.converstation;

import de.dfki.util.JsonUtils;
import lombok.NonNull;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CNLUResultAnalyser {

    public static void main(String args[]) throws IOException {
        List<CIntentRecognizer.CNLUResult> cnluResults = JsonUtils.readList(new File("target/conversation_nlu_results.jsonl"), CIntentRecognizer.CNLUResult.class);
        System.out.println("Read cnlu results: " + cnluResults.size());

//        //check duplicates
//        Map<String, CIntentRecognizer.CNLUResult> textMap= new HashMap<>();
//        for(CIntentRecognizer.CNLUResult result: cnluResults){
//            String key = getText(result);
//            CIntentRecognizer.CNLUResult old = textMap.get(key);
//            if(null == old){
//                textMap.put(key,result);
//            }else {
//                if(!result.intent.equalsIgnoreCase(old.intent) || result.getConfidence()!= old.getConfidence()){
//                    System.out.println("xxxx!");
//                }
//            }
//        }

        System.out.println("Message with different texts: " +
                cnluResults.stream().map(CNLUResultAnalyser::getText).distinct().count());


        cnluResults = cnluResults.stream().distinct().collect(Collectors.toList());
        System.out.println("Remove duplicates. Remained:" + cnluResults.size());
        JsonUtils.writeList(new File("target/conversation_nlu_results_wo_duplicates.jsonl"), cnluResults);

        //print the intent statistics
        List<String> intents = cnluResults.stream().map(CIntentRecognizer.CNLUResult::getIntent).distinct().sorted().collect(Collectors.toList());
        intents.forEach(System.out::println);
        for (String intent : intents) {
            System.out.println(cnluResults.stream().filter(c -> intent.equals(c.intent)).count());
        }
        for (String intent : intents) {
            System.out.println(cnluResults.stream().filter(c -> intent.equals(c.intent))
                    .mapToDouble(CIntentRecognizer.CNLUResult::getConfidence).average().orElse(0));
        }

        //print the text statistics
        System.out.println("Lang texts(> 20 words):" +
                cnluResults.stream().filter(c -> c.getText().split(" +").length>20).count());
        System.out.println("Lang texts(> 30 words):" +
                cnluResults.stream().filter(c -> c.getText().split(" +").length>30).count());
//        System.out.println("Lang texts(> 50 words):" +
//                cnluResults.stream().filter(c -> c.getText().split(" +").length>50).count());
//        System.out.println("Lang texts(> 100 words):" +
//                cnluResults.stream().filter(c -> c.getText().split(" +").length>100).count());
    }

    private static String getText(@NonNull final CIntentRecognizer.CNLUResult result) {
        return result.text.trim().toLowerCase();
    }
}
