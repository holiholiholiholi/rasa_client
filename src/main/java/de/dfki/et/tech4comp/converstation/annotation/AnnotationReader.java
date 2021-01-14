package de.dfki.et.tech4comp.converstation.annotation;

import lombok.NonNull;
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

    public static void main(String args[]) throws Exception {
        String dir = "/Users/lihong/projects/DFKI_ET/tech4comp/data/chatbot_biwi5/2020_10-11/annotation/annotated_leipzip_20201223";
        String files[] = {"intents_eval_20201210_fertigTA.xlsx", "intents_eval_lowConfidence_20201210_fertigNP.xlsx"};
        for (String f : files) {
            System.out.println("------------" + f + "-----------");
//            firstStatics(new File(dir, f));
            checkSuggestion(new File(dir, f));
            System.out.println("\n\n");

        }

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


}
