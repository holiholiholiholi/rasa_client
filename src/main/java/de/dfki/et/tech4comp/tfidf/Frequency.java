package de.dfki.et.tech4comp.tfidf;

import lombok.NonNull;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class Frequency {
    public static void main(String args[]) throws IOException {
        File dir = new File("/Users/lihong/projects/DFKI_ET/tech4comp/data/TestBed_UD/folien_txt");

        Map<String, Integer> tmap = new HashMap<>();
        for (File f : dir.listFiles(f -> f.getName().endsWith(".txt"))) {
            String text = FileUtils.readFileToString(f, "utf8");
            count(getWords(text), tmap);
        }
        System.out.println("words with high frequency in all folien.");
        printMapInt(tmap, 10);

        System.out.println("========================");
        List<Map<String,Integer>> documents = new ArrayList<>();
        for (File f : dir.listFiles(f -> f.getName().endsWith(".txt"))) {
            Map<String, Integer> map = new HashMap<>();
            String text = FileUtils.readFileToString(f, "utf8");
            count(getWords(text), map);
            documents.add(map);
        }
        int N = documents.size();
        for(int i=0;i<documents.size();i++){
            int docN = i+1;
            Map<String,Integer> words = documents.get(i);
            Map<String,Double> tfidf =
                    words.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e->
                            (double)e.getValue() *
                                    Math.log((double) N/ documents.stream().filter(m-> m.containsKey(e.getKey())).count())));
            System.out.println("------ words with high F in doc:"+docN+"--------------");
            printMapDouble(tfidf, 10);
            System.out.println();
        }

    }

    public static List<String> getWords(@NonNull final String text) {
        String[] tokens = text.split("( +)|(\n+)");
        return Arrays.stream(tokens)
                .filter(StringUtils::isNoneEmpty)
                .filter(s -> Character.isUpperCase(s.charAt(0)))
                .filter(s -> s.length()>3)
                .collect(Collectors.toList());
    }

    public static void printMapInt(Map<String, Integer> map, int top) {
        Comparator<Map.Entry<?, Integer>> c = Comparator.comparingInt(Map.Entry::getValue);
        map.entrySet().stream().sorted(c.reversed()).limit(top)
                .forEach(e -> System.out.println(e.getKey() + ", " + e.getValue()));
    }

    public static void printMapDouble(Map<String, Double> map, int top) {
        Comparator<Map.Entry<?, Double>> c = Comparator.comparingDouble(Map.Entry::getValue);
        map.entrySet().stream().sorted(c.reversed()).limit(top)
                .forEach(e -> System.out.println(e.getKey() + ", " + e.getValue()));
    }

    public static void count(@NonNull final Collection<String> tokens, @NonNull final Map<String, Integer> map) {
        tokens.forEach(t -> map.put(t, map.getOrDefault(t, 0) + 1));
    }
}
