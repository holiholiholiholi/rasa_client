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


public class AnnotationReader2 {


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

    public static void main(String args[]) throws Exception {
        File dir = new File("/Users/lihong/projects/DFKI_ET/tech4comp/data/chatbot_biwi5_UL/log_2020_10-11");
        File annotationDir = new File(dir, "annotation/round2/annotated_leipzip");

        String file = "conversations_selected_lowConfidence2.xlsx";

        Map<String, String> idTextMap =
                JsonUtils.readList(new File(dir, "conversation_nlu_results_wo_duplicates.jsonl"),
                        CIntentRecognizer.CNLUResult.class)
                        .stream()
                        .collect(Collectors.toMap(c -> c.getConversationId() + "_" + c.getMessageIndex(),
                                CIntentRecognizer.CNLUResult::getText));
        System.out.println("Read cnlu results: " + idTextMap.size());

        File inputFile = new File(annotationDir, file);
        System.out.println("\n=============================");
        firstStatics(inputFile);
        System.out.println("\n=============================");
        System.out.println("check clear Suggestion");
        checkSuggestion(inputFile);
        System.out.println("\n=============================");
        List<String> multiSentIds = getMultiSentence(inputFile);
        System.out.println("Found messages with multi intents:" + multiSentIds.size());
        multiSentIds.forEach(id -> System.out.println(idTextMap.get(id)));
        System.out.println("\nSave message with multi intents into file...");
        FileUtils.writeLines(new File(annotationDir, "multiIntents.txt"),
                multiSentIds.stream().map(id -> id + "\t" + idTextMap.get(id)).collect(Collectors.toList()));

        System.out.println("\n=============================");
        List<String> evaldIds = getMessageId(inputFile);
        System.out.println("Save message ids (" + evaldIds.size() + ") into file ...");
        FileUtils.writeLines(new File(annotationDir, "annotated_ids.txt"), evaldIds);

        System.out.println("\n=============================");
        List<AnnotationConstant.IntentAnnotation> gold = readGoldstandard(inputFile, idTextMap, null);
        System.out.println("Save goldstandard data (" + gold.size() + ") into file...");
        JsonUtils.writeList(new File(annotationDir, "goldstandard.jsonl"), gold);
    }

    static List<String> getMultiSentence(@NonNull final File file) throws Exception {
        List<String> ids = new ArrayList<>();

        try (XSSFWorkbook workbook = new XSSFWorkbook(file)) {
            XSSFSheet sheet = workbook.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (StringUtils.isEmpty(AnnotationConstant.getStringContent(row.getCell(AnnotationConstant.evalCol)))
                        &&
                        StringUtils.isNoneEmpty(AnnotationConstant.getStringContent(row.getCell(AnnotationConstant.hasMultiSentence)))) {
                    String id = AnnotationConstant.getId(row);
                    if (null != id) {
                        ids.add(id);
                    }
                }
            }
        }
        return ids;
    }



    static List<AnnotationConstant.IntentAnnotation> readGoldstandard(@NonNull final File file,
                                                                      @NonNull final Map<String, String> textMap,
                                                                      final Collection<String> excludedIds) throws Exception {
        List<AnnotationConstant.IntentAnnotation> results = new ArrayList<>();
        int excluded = 0, correct = 0, suggesion = 0;
        try (XSSFWorkbook workbook = new XSSFWorkbook(file)) {
            XSSFSheet sheet = workbook.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);

                String id = AnnotationConstant.getId(row);
                String text = textMap.get(id);
                if (null == text) {
                    System.err.println("Can not find text with mid:" + id + " in row:" + i);
                    continue;
                }
                if (null != excludedIds && excludedIds.contains(id)) {
                    excluded++;
                    continue;
                }
                String intent = AnnotationConstant.getStringContent(row.getCell(AnnotationConstant.intentCol));
                if (null == intent) {
                    System.err.println("Can not read intent in row:" + i);
                    continue;
                }

                String eval = AnnotationConstant.getStringContent(row.getCell(AnnotationConstant.evalCol));
                if ("correct".equalsIgnoreCase(eval)) {
                    results.add(new AnnotationConstant.IntentAnnotation(text, intent));
                    correct++;
                } else {
                    String suggestion = AnnotationConstant.getStringContent(row.getCell(AnnotationConstant.intentSuggestionCol));
                    if (null != suggestion && allIntents.contains(suggestion.toLowerCase(Locale.ROOT))) {
                        results.add(new AnnotationConstant.IntentAnnotation(text, suggestion));
                        suggesion++;
                    }
                }
            }
        }
        System.out.println("Excluded: " + excluded);
        System.out.println("Correct: " + correct);
        System.out.println("Suggestion:" + suggesion);
        return results;
    }

    static List<String> getMessageId(@NonNull final File file) throws Exception {
        List<String> ids = new ArrayList<>();
        try (XSSFWorkbook workbook = new XSSFWorkbook(file)) {
            XSSFSheet sheet = workbook.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                String id = AnnotationConstant.getId(row);
                if (null != id) {
                    ids.add(id);
                }
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
                if (null == AnnotationConstant.getStringContent(row.getCell(AnnotationConstant.cidCol))) {
                    continue;
                }
                String intent = AnnotationConstant.getStringContent(row.getCell(AnnotationConstant.intentCol));
                if (null == intent) {
                    continue;
                }
                String eval = AnnotationConstant.getStringContent(row.getCell(AnnotationConstant.evalCol));
                if (null != eval && !"correct".equalsIgnoreCase(eval)) {
                    String suggestion = AnnotationConstant.getStringContent(row.getCell(AnnotationConstant.intentSuggestionCol));
                    if (null != suggestion && allIntents.contains(suggestion.toLowerCase(Locale.ROOT))) {
                        intentMap.put(intent, intentMap.getOrDefault(intent, 0) + 1);
                    }
                }
            }
        }
        for (String s : intentMap.keySet().stream().sorted().collect(Collectors.toList())) {
            System.out.println(s + ";" + intentMap.get(s));
        }
        System.out.println("ALL: " + intentMap.values().stream().mapToInt(Integer::intValue).sum());
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
                if (null == AnnotationConstant.getStringContent(row.getCell(AnnotationConstant.cidCol))) {
                    continue;
                }
                String intent = AnnotationConstant.getStringContent(row.getCell(AnnotationConstant.intentCol));
                if (null == intent) {
                    continue;
                }
                int[] evalArray = intentEvalMap.computeIfAbsent(intent, k -> new int[]{0, 0, 0, 0, 0, 0});
                evalArray[4] = evalArray[4] + 1;
                all++;
                String eval = AnnotationConstant.getStringContent(row.getCell(AnnotationConstant.evalCol));
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
                    String suggestion = AnnotationConstant.getStringContent(row.getCell(AnnotationConstant.intentSuggestionCol));
                    if (null != suggestion) {
                        Map<String, Integer> map = intentSuggestionMap.computeIfAbsent(intent, k -> new HashMap<>());
                        map.put(suggestion, map.getOrDefault(suggestion, 0) + 1);
                        hasSuggestion++;
                        evalArray[5] = evalArray[5] + 1;
                    }
                }
            }

        }
        System.out.println("annotation for evaluation:" + evalResults);
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


}
