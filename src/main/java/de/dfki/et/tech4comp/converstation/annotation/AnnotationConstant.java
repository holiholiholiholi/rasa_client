package de.dfki.et.tech4comp.converstation.annotation;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;

import java.text.NumberFormat;
import java.util.Locale;

public class AnnotationConstant {
    static int cidCol = 0;
    static int midCol = 1;
    static int confCol = 2;
    static int intentCol = 3;
    static int textCol = 4;
    static int evalCol = 5;
    static int intentSuggestionCol = 6;
    static int hasMultiSentence = 7;

    static NumberFormat nf = NumberFormat.getInstance(new Locale("en", "US"));

    static String formatPrecision(final double p){
        nf.setMaximumFractionDigits(2);
        return nf.format(p*100)+"%";
    }
    static double getPrecision(final int correct, final int incorrect){
        return ((double) correct) / (correct + incorrect);
    }
    static String getStringContent(final Cell cell) {
        if (null == cell) {
            return null;
        }
        String string = cell.getStringCellValue();
        return StringUtils.isBlank(string) ? null : string.trim();
    }

    static String getIntContent(final Cell cell) {
        if (null == cell) {
            return null;
        }
        int i = (int) cell.getNumericCellValue();
        return i + "";
    }

    static Double getDoubleContent(final Cell cell) {
        if (null == cell) {
            return null;
        }
        return cell.getNumericCellValue();
    }

    static String getId(@NonNull final Row row) {
        String cid = AnnotationConstant.getStringContent(row.getCell(AnnotationConstant.cidCol));
        if (null == cid) {
            return null;
        }
        String mid = AnnotationConstant.getIntContent(row.getCell(AnnotationConstant.midCol));
        if (null == mid) {
            return null;
        }
        return cid + "_" + mid;
    }


    @Data
    public static class IntentAnnotation {
        final String text;
        final String intent;

        public IntentAnnotation(@JsonProperty("text") final String text, @JsonProperty("intent") final String intent) {
            this.text = text;
            this.intent = intent;
        }
    }

    @Data
    public static class EvalSet {
        int correct = 0;
        int incorrect = 0;

        public void addOne(boolean isCorrect) {
            if (isCorrect) {
                correct += 1;
            } else {
                incorrect += 1;
            }
        }

        public double getPrecision() {
            return AnnotationConstant.getPrecision(correct,incorrect);
        }
    }
}
