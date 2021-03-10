package de.dfki.et.tech4comp.converstation.annotation;

import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.util.*;

public class EvalFromAnnotation {
    public static void main(String args[]) throws Exception {

        File dir = new File("/Users/lihong/projects/DFKI_ET/tech4comp/data/chatbot_biwi5_UL/log_2020_10-11/annotation");
        File file1 = new File(dir, "round1/annotated_leipzip_20201223/intents_eval_20201210_fertigTA.xlsx");
        File file2 = new File(dir, "round1/annotated_leipzip_20201223/intents_eval_lowConfidence_20201210_fertigNP.xlsx");
        File file3 = new File(dir, "round2/annotated_leipzip/conversations_selected_lowConfidence2.xlsx");
        List<File> inputFiles = List.of(file1, file2, file3);
        System.out.println("==============for intents================");
        eval4Intents(inputFiles);
        System.out.println("===============for confidence================");
        eval4Confidence(inputFiles);
    }

    static void eval4Intents(@NonNull final Collection<File> inputFiles) throws Exception {
        Map<String, AnnotationConstant.EvalSet> map = new HashMap<>();
        for (File f : inputFiles) {
            try (XSSFWorkbook workbook = new XSSFWorkbook(f)) {
                XSSFSheet sheet = workbook.getSheetAt(0);
                for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                    Row row = sheet.getRow(i);

                    String eval = AnnotationConstant.getStringContent(row.getCell(AnnotationConstant.evalCol));
                    if (StringUtils.isEmpty(eval)) {
                        continue;
                    }

                    String intent = AnnotationConstant.getStringContent(row.getCell(AnnotationConstant.intentCol));
                    if (null == intent) {
                        System.err.println("Can not read intent in row:" + i);
                        continue;
                    }

                    map.computeIfAbsent(intent, k -> new AnnotationConstant.EvalSet())
                            .addOne(eval.equalsIgnoreCase("correct"));

                }
            }
        }
        int correct = map.values().stream().mapToInt(AnnotationConstant.EvalSet::getCorrect).sum();
        int incorrect = map.values().stream().mapToInt(AnnotationConstant.EvalSet::getIncorrect).sum();
        int all = correct + incorrect;
        System.out.println("Evaluated: " + all + "-- correct: " + correct + " incorrect:" + incorrect
                + ", precision: " + AnnotationConstant.formatPrecision(AnnotationConstant.getPrecision(correct, incorrect)));
        map.keySet().stream().sorted().forEach(k -> {
            AnnotationConstant.EvalSet eval = map.get(k);
            System.out.println(k + ";" + eval.correct + ";" + eval.incorrect + ";" +
                    AnnotationConstant.formatPrecision(eval.getPrecision()));
        });
    }

    static String getConfidencekey(final double c) {
        if (c >= 0.99) {
            return "[0.99 , 1]";
        }
        if(c>=0.9){
            return "[0.9, 0.99)";
        }
        if (c < 0.4) {
            return "(0, 0.4)";
        }
        int i = (int) (c * 10);
        return "[0." + i + ", 0." + (i + 1) + ")";
    }

    static void eval4Confidence(@NonNull final Collection<File> inputFiles) throws Exception {
        Map<String, AnnotationConstant.EvalSet> map = new HashMap<>();
        for (File f : inputFiles) {
            try (XSSFWorkbook workbook = new XSSFWorkbook(f)) {
                XSSFSheet sheet = workbook.getSheetAt(0);
                for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                    Row row = sheet.getRow(i);

                    String eval = AnnotationConstant.getStringContent(row.getCell(AnnotationConstant.evalCol));
                    if (StringUtils.isEmpty(eval)) {
                        continue;
                    }
                    Double confidence = AnnotationConstant.getDoubleContent(row.getCell(AnnotationConstant.confCol));
                    if(confidence== null){
                        continue;
                    }
                    String key = getConfidencekey(confidence);
                    map.computeIfAbsent(key, k -> new AnnotationConstant.EvalSet())
                            .addOne(eval.equalsIgnoreCase("correct"));
                }
            }
        }
        int correct = map.values().stream().mapToInt(AnnotationConstant.EvalSet::getCorrect).sum();
        int incorrect = map.values().stream().mapToInt(AnnotationConstant.EvalSet::getIncorrect).sum();
        int all = correct + incorrect;
        System.out.println("Evaluated: " + all + "-- correct: " + correct + " incorrect:" + incorrect
                + ", precision: " + AnnotationConstant.formatPrecision(AnnotationConstant.getPrecision(correct, incorrect)));
        map.keySet().stream().sorted(Comparator.reverseOrder()).forEach(k -> {
            AnnotationConstant.EvalSet eval = map.get(k);
            System.out.println(k + ";" + eval.correct + ";" + eval.incorrect + ";" +(eval.incorrect+eval.correct)+";"+
                    AnnotationConstant.formatPrecision(eval.getPrecision()));
        });
    }
}
