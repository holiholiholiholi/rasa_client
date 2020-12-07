package de.dfki.et.tech4comp.converstation.annotation;

import de.dfki.et.tech4comp.converstation.CIntentRecognizer;
import de.dfki.et.tech4comp.converstation.ChatLogReader;
import de.dfki.et.tech4comp.converstation.Conversation;
import de.dfki.util.JsonUtils;
import lombok.NonNull;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.RichTextString;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class MessageSelector {
    static int number = 100;
    static int number4Intent = 10;

    public static void main(String args[]) throws Exception {
        List<Conversation> conversations = JsonUtils.readList(new File("target/conversations.jsonl"), Conversation.class);
        Map<String, Conversation> conversationMap = conversations.stream().collect(Collectors.toMap(Conversation::getId, c -> c));

        List<CIntentRecognizer.CNLUResult> cnluResults = JsonUtils.readList(new File("target/conversation_nlu_results_wo_duplicates.jsonl"), CIntentRecognizer.CNLUResult.class);
        System.out.println("Read cnlu results: " + cnluResults.size());

//        Collections.shuffle(cnluResults);
//        List<CIntentRecognizer.CNLUResult> selectedResults = cnluResults.subList(0, number);

        List<CIntentRecognizer.CNLUResult> selectedResults = select4Intent(cnluResults);
        saveCNLUResult(selectedResults, conversationMap, new File("target/conversations_selected.xlsx"));

    }

    static List<CIntentRecognizer.CNLUResult> select(@NonNull final Collection<CIntentRecognizer.CNLUResult> results) {
        List<CIntentRecognizer.CNLUResult> cnluResults = new ArrayList<>(results);
        Collections.shuffle(cnluResults);
        return cnluResults.subList(0, number);
    }

    static List<CIntentRecognizer.CNLUResult> select4Intent(@NonNull final Collection<CIntentRecognizer.CNLUResult> results) {
        Set<String> intents = results.stream().map(CIntentRecognizer.CNLUResult::getIntent).collect(Collectors.toSet());
        List<CIntentRecognizer.CNLUResult> selected = new ArrayList<>();
        for (String intent : intents) {
            List<CIntentRecognizer.CNLUResult> list = results.stream().filter(c -> intent.equals(c.getIntent())).collect(Collectors.toList());
            Collections.shuffle(list);
            selected.addAll(list.subList(0, number4Intent));
        }
        return selected;
    }


    static void saveCNLUResult(@NonNull final List<CIntentRecognizer.CNLUResult> selectedResults,
                               @NonNull final Map<String, Conversation> conversationMap,
                               @NonNull final File file) throws IOException {
        selectedResults.sort(Comparator.comparing(CIntentRecognizer.CNLUResult::getIntent));

        try (XSSFWorkbook workbook = new XSSFWorkbook(); FileOutputStream fo = new FileOutputStream(file)) {
            XSSFSheet sheet = workbook.createSheet("massage nlu");
            List<String> header = List.of("conversation id", "message index", "confidence", "intent", "text", "isCorrect", "intent suggestion");
            createHeader(workbook, sheet, header);

            XSSFCellStyle dcellStyle = workbook.createCellStyle();
            dcellStyle.setDataFormat(
                    workbook.getCreationHelper().createDataFormat().getFormat("0.00"));
            XSSFCellStyle textCellStyle = workbook.createCellStyle();
            textCellStyle.setWrapText(true);


            XSSFFont messageFont = workbook.createFont();
//            messageFont.setBold(true);
            messageFont.setColor(HSSFColor.HSSFColorPredefined.BLUE.getIndex());
            XSSFFont historyFont = workbook.createFont();
            historyFont.setColor(HSSFColor.HSSFColorPredefined.GREY_40_PERCENT.getIndex());

            int rowNum = 1;
            for (CIntentRecognizer.CNLUResult result : selectedResults) {
                Row row = sheet.createRow(rowNum++);
                int colNum = 0;
                row.createCell(colNum++).setCellValue(result.getConversationId());
                row.createCell(colNum++).setCellValue(result.getMessageIndex());
                Cell dcell = row.createCell(colNum++);
                dcell.setCellValue(result.getConfidence());
                dcell.setCellStyle(dcellStyle);
                row.createCell(colNum++).setCellValue(result.getIntent());
                Cell textCell = row.createCell(colNum++);
                textCell.setCellValue(getText(result, conversationMap, messageFont, historyFont));
                textCell.setCellStyle(textCellStyle);
            }
            sheet.autoSizeColumn(3);
            sheet.autoSizeColumn(4);
            workbook.write(fo);
        }
    }

    private static RichTextString getText(@NonNull final CIntentRecognizer.CNLUResult result,
                                          @NonNull final Map<String, Conversation> conversationMap,
                                          XSSFFont messageFont, XSSFFont historyFont) {
        XSSFRichTextString textString = new XSSFRichTextString();
        Conversation c = conversationMap.get(result.getConversationId());
        if (result.getMessageIndex() >= 2) {
            if (null != c) {
                Conversation.Message m1 = c.getMessages().get(result.getMessageIndex() - 2);
                textString.append(getIdString(m1) + "\t" + getHistoryString(m1.getText()) + "\n", historyFont);
                Conversation.Message m2 = c.getMessages().get(result.getMessageIndex() - 1);
                textString.append(getIdString(m2) + "\t" + getHistoryString(m2.getText()) + "\n", historyFont);
            }
        }
        textString.append("S:\t", historyFont);
        textString.append(result.getText().trim()+"\n" , messageFont);
        if(null != c) {
            if (result.getMessageIndex() < c.getMessages().size()-1){
                Conversation.Message next = c.getMessages().get(result.getMessageIndex()+1);
                textString.append(getIdString(next) + "\t" + getHistoryString(next.getText()), historyFont);
            }
        }
        return textString;
    }

    private static String getIdString(@NonNull Conversation.Message m) {
        if (ChatLogReader.BOT_HASH.contains(m.getUserId())) {
            return "B:";
        }
        return "S:";
    }

    private static String getHistoryString(@NonNull final String text) {
        String string = text.trim();
        if (string.contains("\n")) {
            string = string.split("\n")[0];
        }
        return string.length() >= 80 ? string.substring(0, 80) + " ... " : string;
    }

    private static void createHeader(XSSFWorkbook workbook, XSSFSheet sheet, List<String> header) {
        Row headerRow = sheet.createRow(0);

        XSSFFont font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 14);
        XSSFCellStyle style = workbook.createCellStyle();
        style.setFont(font);

        int colNum = 0;
        for (String h : header) {
            Cell cell = headerRow.createCell(colNum++);
            cell.setCellValue(h);
            cell.setCellStyle(style);
        }
        headerRow.setRowStyle(style);
    }
}