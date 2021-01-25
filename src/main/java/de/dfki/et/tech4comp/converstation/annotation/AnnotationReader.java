package de.dfki.et.tech4comp.converstation.annotation;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.dfki.et.tech4comp.converstation.CIntentRecognizer;
import de.dfki.util.JsonUtils;
import lombok.Data;
import lombok.NonNull;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class AnnotationReader {
    static int cidCol = 0;
    static int midCol = 1;
    static int confCol = 2;
    static int intentCol = 3;
    static int textCol = 4;
    static int evalCol = 5;
    static int intentSuggestionCol = 6;

    static Set<String> allIntents = Arrays.stream(new String[]{"badbehavior",
            "biwi5ichwill",
            "biwi5zeige",
            "contact",
            "credit",
            "functions",
            "goodbye",
            "greet",
            "howareyou",
            "privacyanddata",
            "showtasks",
            "submission",
            "thanks",
            "tmitocar", "default"}).collect(Collectors.toSet());

    static Set<String> biwi5Intents = Set.of("biwi5ichwill", "biwi5zeige");

    public static void main(String args[]) throws Exception {
//        String dir = "/Users/lihong/projects/DFKI_ET/tech4comp/data/chatbot_biwi5/2020_10-11/annotation/round1/annotated_leipzip_20201223";
        String dir = "/Users/lihong/projects/DFKI_ET/tech4comp/data/chatbot_biwi5/2020_10-11/annotation/round1/annotated_leipzip_20210108";
        String files[] = {"intents_eval_20201210_fertigTA.xlsx", "intents_eval_lowConfidence_20201210_fertigNP.xlsx"};


        List<String> msMessageId = FileUtils.readLines(new File("/Users/lihong/projects/DFKI_ET/tech4comp/data/chatbot_biwi5/2020_10-11/message_multi_sents_id.txt"), "utf8");
        System.out.println("Read message id with multi sentence:" + msMessageId.size());
        Map<String, String> cnluResults = JsonUtils.readList(new File("/Users/lihong/projects/DFKI_ET/tech4comp/data/chatbot_biwi5/2020_10-11/conversation_nlu_results_wo_duplicates.jsonl"), CIntentRecognizer.CNLUResult.class)
                .stream()
                .collect(Collectors.toMap(c -> c.getConversationId() + "_" + c.getMessageIndex(),
                        CIntentRecognizer.CNLUResult::getText));
        System.out.println("Read cnlu results: " + cnluResults.size());
        List<IntentAnnotation> results = new ArrayList<>();



//        for(String f: files){
//            System.out.println("------------" + f + "-----------");
//            results.addAll(readGoldstandard(new File(dir,f),cnluResults,msMessageId));
//        }
//        JsonUtils.writeList(new File("target/goldstandard.jsonl"),results);
//        System.out.println("Get goldstandard intent annotation:"+ results.size());
//        for (String f : files) {
//            System.out.println("------------" + f + "-----------");
////            firstStatics(new File(dir, f));
//            checkSuggestion(new File(dir, f));
//            System.out.println("\n\n");
//
//        }

//        List<String> ids  = new ArrayList<>();
//        for(String f: files){
//            ids.addAll(getMessageId(new File(dir,f)));
//        }
//        System.out.println(ids.size());
//        FileUtils.writeLines(new File("target/annotated_ids.txt"), ids);
    }

    static List<IntentAnnotation> readGoldstandard(@NonNull final File file,
                                                   @NonNull final Map<String, String> textMap,
                                                   @NonNull final Collection<String> excludedIds) throws Exception {
        List<IntentAnnotation> results = new ArrayList<>();
        int excluded = 0,correct = 0, suggesion=0;
        try (XSSFWorkbook workbook = new XSSFWorkbook(file)) {
            XSSFSheet sheet = workbook.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);

                String id = getStringContent(row.getCell(cidCol)) + "_" + getIntContent(row.getCell(midCol));
                String text = textMap.get(id);
                if(null == text){
                    System.err.println("Can not find text with mid:"+id+" in row:"+i);
                    continue;
                }
                if(excludedIds.contains(id)){
                    excluded++;
                    continue;
                }
                String intent = getStringContent(row.getCell(intentCol));
                if (null == intent) {
                    System.err.println("Can not read intent in row:"+i);
                    continue;
                }

                String eval = getStringContent(row.getCell(evalCol));
                if ("correct".equalsIgnoreCase(eval)) {
                    results.add(new IntentAnnotation(text, intent));
                    correct++;
                } else  {
                    String suggestion = getStringContent(row.getCell(intentSuggestionCol));
                    if (null != suggestion && allIntents.contains(suggestion.toLowerCase(Locale.ROOT))) {
                        results.add(new IntentAnnotation(text, suggestion));
                        suggesion++;
                    }
                }
            }
        }
        System.out.println("Excluded: "+excluded);
        System.out.println("Correct: "+correct);
        System.out.println("Suggestion:"+suggesion);
        return results;
    }

    static List<String> getMessageId(@NonNull final File file) throws Exception {
        List<String> ids = new ArrayList<>();
        try (XSSFWorkbook workbook = new XSSFWorkbook(file)) {
            XSSFSheet sheet = workbook.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                String cid = getStringContent(row.getCell(cidCol));
                if (null == cid) {
                    continue;
                }
                String mid = getIntContent(row.getCell(midCol));
                if (null == mid) {
                    continue;
                }
                ids.add(cid + "_" + mid);
            }
        }
        return ids;
    }

    static void checkSuggestion(@NonNull final File file) throws Exception {
        Map<String, Integer> intentMap = new HashMap<>();

        try (XSSFWorkbook workbook = new XSSFWorkbook(file)) {
            XSSFSheet sheet = workbook.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (null == getStringContent(row.getCell(cidCol))) {
                    continue;
                }
                String intent = getStringContent(row.getCell(intentCol));
                if (null == intent) {
                    continue;
                }
                String eval = getStringContent(row.getCell(evalCol));
                if (null != eval && !"correct".equalsIgnoreCase(eval)) {
                    String suggestion = getStringContent(row.getCell(intentSuggestionCol));
                    if (null != suggestion && allIntents.contains(suggestion.toLowerCase(Locale.ROOT))) {
                        intentMap.put(intent, intentMap.getOrDefault(intent, 0) + 1);
                    }
                }
            }
        }
        for (String s : intentMap.keySet().stream().sorted().collect(Collectors.toList())) {
            System.out.println(s + ";" + intentMap.get(s));
        }
    }

    static void firstStatics(@NonNull final File file) throws Exception {
        Set<String> evalResults = new HashSet<>();
        int correct = 0, incorrect = 0, unknown = 0, evaled = 0, all = 0, hasSuggestion = 0;
        Map<String, int[]> intentEvalMap = new HashMap<>();
        Map<String, Map<String, Integer>> intentSuggestionMap = new HashMap<>();
        try (XSSFWorkbook workbook = new XSSFWorkbook(file)) {
            XSSFSheet sheet = workbook.getSheetAt(0);
//            System.out.println(sheet.getLastRowNum());
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (null == getStringContent(row.getCell(cidCol))) {
                    continue;
                }
                String intent = getStringContent(row.getCell(intentCol));
                if (null == intent) {
                    continue;
                }
                int[] evalArray = intentEvalMap.computeIfAbsent(intent, k -> new int[]{0, 0, 0, 0, 0, 0});
                evalArray[4] = evalArray[4] + 1;
                all++;
                String eval = getStringContent(row.getCell(evalCol));
                if (null != eval) {
                    evaled++;
                    evalArray[3] = evalArray[3] + 1;
                    if ("correct".equalsIgnoreCase(eval)) {
                        correct++;
                        evalArray[0] = evalArray[0] + 1;
                    } else if ("incorrect".equalsIgnoreCase(eval)) {
                        incorrect++;
                        evalArray[1] = evalArray[1] + 1;
                    } else {
                        evalArray[2] = evalArray[2] + 1;
                        unknown++;
                    }
                }
                evalResults.add(eval);
                if (!"correct".equalsIgnoreCase(eval)) {
                    String suggestion = getStringContent(row.getCell(intentSuggestionCol));
                    if (null != suggestion) {
                        Map<String, Integer> map = intentSuggestionMap.computeIfAbsent(intent, k -> new HashMap<>());
                        map.put(suggestion, map.getOrDefault(suggestion, 0) + 1);
                        hasSuggestion++;
                        evalArray[5] = evalArray[5] + 1;
                    }
                }
            }

        }
        System.out.println(evalResults);
        System.out.println("all:" + all + ", evaluated:" + evaled
                + ", correct:" + correct + ", incorrect:" + incorrect + ", unknown:" + unknown + ", hasSuggestion:" + hasSuggestion);
        System.out.println("precision:" + (double) correct / (double) evaled);
        System.out.println("Intent Nr:" + intentEvalMap.size());
        for (String s : intentEvalMap.keySet().stream().sorted().collect(Collectors.toList())) {
            System.out.print(s + ";");
            int[] evalArray = intentEvalMap.get(s);
            System.out.print(Arrays.stream(evalArray)
                    .mapToObj(String::valueOf).collect(Collectors.joining(";")));
            System.out.print(";" + (double) evalArray[0] / (double) evalArray[3]);
            System.out.print(";");
            if (intentSuggestionMap.containsKey(s)) {
                System.out.print(intentSuggestionMap.get(s).entrySet().stream()
                        .map(e -> e.getKey() + "(" + e.getValue() + ")")
                        .collect(Collectors.joining(",")));

            }
            System.out.println();
        }
    }

    private static String getStringContent(final Cell cell) {
        if (null == cell) {
            return null;
        }
        String string = cell.getStringCellValue();
        return StringUtils.isBlank(string) ? null : string.trim();
    }

    private static String getIntContent(final Cell cell) {
        if (null == cell) {
            return null;
        }
        int i = (int) cell.getNumericCellValue();
        return i + "";
    }

    @Data
    public static class IntentAnnotation {
        final String text;
        final String intent;
        public IntentAnnotation(@JsonProperty("text") final String text , @JsonProperty("intent") final String intent){
            this.text = text;
            this.intent = intent;
        }
    }

}
