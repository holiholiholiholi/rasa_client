package de.dfki.et.tech4comp.converstation;

import de.dfki.util.JsonUtils;
import lombok.NonNull;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Examples:
 * 2020-11-01T14:48:13.747Z c9990b5eab3fcc89c96b3c7150e5f797: Hochladen von Schreibaufgaben
 * 2020-11-01T14:48:16.745Z 5aba89fded6909f14eb1f5de3047bd89: Im aktuellen Themenblock gibt es drei Schreibaufgaben.
 * Ich zeige dir mal alle drei. Wenn du für eine Aufgabe die Lösung abgeben möchtest,
 * kannst du mich danach fragen ...
 */
public class ChatLogReader {
    public final static Set<String> BOT_HASH = Set.of("5aba89fded6909f14eb1f5de3047bd89","92d53aacd80de523cbaa3c138898b898");

    public static void main(String args[]) throws IOException {
        File dir = new File("/Users/lihong/projects/DFKI_ET/tech4comp/data/chatbot_biwi5/2020_10-11/chatlog");
        int fnumber = 0, emptyFiles = 0;
        List<Conversation> conversations = new ArrayList<>();
        for (File f : dir.listFiles(n -> n.getName().endsWith("txt"))) {
            System.out.println(f.getName());
            fnumber++;
            Conversation c = convert(getFileBaseName(f), FileUtils.readLines(f, "utf8"));
            if (c.messages.isEmpty()) {
                emptyFiles++;
            } else {
                System.out.println("Read conversation with posters: " + c.messages.size());
//                System.out.println(" -- " + c.messages.get(0));
            }
            conversations.add(c);
        }
        System.out.println("Read files: " + fnumber);
        System.out.println("Empty files: " + emptyFiles);
// System.out.println(new ObjectMapper().writeValueAsString(conversations));
        conversations.removeIf(c -> c.messages.isEmpty());
        System.out.println("Get conversations: " + conversations.size());
        System.out.println("Averaged posters: " + conversations.stream().mapToInt(Conversation::size).average().orElse(0));
        System.out.println("Max posters:" + conversations.stream().mapToInt(c -> c.messages.size()).max().orElse(0));
        System.out.println(" -- " + conversations.stream().sorted(Comparator.comparingInt(Conversation::size).reversed()).map(Conversation::getId).findFirst().orElse(null));
        System.out.println("Min posters:" + conversations.stream().mapToInt(c -> c.messages.size()).min().orElse(0));
        System.out.println(" -- " + conversations.stream().sorted(Comparator.comparingInt(Conversation::size)).map(Conversation::getId).findFirst().orElse(null));
        System.out.println("Users: " + conversations.stream().flatMap(c -> c.messages.stream()).map(Conversation.Message::getUserId).distinct().count());
        System.out.println("Conversation with more than 3 Users: " + conversations.stream().filter( c -> c.messages.stream().map(Conversation.Message::getUserId).distinct().count()>2).count());
        System.out.println(" -- examples: "+ conversations.stream().filter( c -> c.messages.stream().map(Conversation.Message::getUserId).distinct().count()>2).findFirst().get().id);
        JsonUtils.writeList(new File("target/conversations.jsonl"), conversations);

    }

    static Conversation convert(final String id, @NonNull final List<String> lines) {
        List<Conversation.Message> posters = new ArrayList<>();
        List<String> textLine = new ArrayList<>();
        for (String line : lines) {
//            System.err.println(line);
            if (isFirstLine(line)) {
//                System.err.println("First line");
                if (!textLine.isEmpty() && !posters.isEmpty()) {
                    posters.get(posters.size() - 1).text = String.join("\n", textLine);
                }
                textLine.clear();
                String[] tokens = readFirstLine(line);
                posters.add(null == tokens ? new Conversation.Message() : new Conversation.Message(tokens[0], tokens[1], tokens[2]));
                if(null!=tokens){
                    textLine.add(tokens[2]);
                }
            } else {
                textLine.add(line);
            }

        }
        if (!textLine.isEmpty() && !posters.isEmpty()) {
            posters.get(posters.size() - 1).text = String.join("\n", textLine);
        }
        return new Conversation(id, posters);
    }

    static String getFileBaseName(@NonNull final File file) {
        String name = file.getName();
        int pos = name.indexOf('.');
        return pos < 0 ? name : name.substring(0, pos);
    }

    static boolean isFirstLine(@NonNull final String line) {
        //TODO: check datum format!
        return line.startsWith("2020-");
    }

    static String[] readFirstLine(@NonNull final String line) {
        int pos = line.indexOf(" ");
        if (pos <= 0) {
            return null;
        }
        String date = line.substring(0, pos);
        pos++;
        int pos2 = line.indexOf(": ", pos);
        if (pos2 <= 0) {
            return null;
        }
        String id = line.substring(pos, pos2);
        pos2 += 2;
        String text = pos2 >= line.length() ? "" : line.substring(pos2);
        return new String[]{date, id, text};
    }



}
