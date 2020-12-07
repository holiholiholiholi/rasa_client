package de.dfki.et.tech4comp.rasa_client;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;


public class NLUDataTransfer {
    static int min_examples = 3; // set -1 to turn off the filter

    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.err.println("Usage intentsFileV1 output.yml");
            return;
        }
        transform(new File(args[0]), new File(args[1]));
    }

    private static void transform(@NonNull final File input, @NonNull final File output) throws IOException {
        NLUData2 nluData = transform(input);
        File dir = output.getParentFile();
        if (!dir.exists()) {
            System.out.println("Directory " + dir.getAbsolutePath() + " does not exist! will be created");
            dir.mkdirs();
        }
        FileUtils.write(output, getYamlMapper().writeValueAsString(nluData), "utf8");
        System.out.println("Written " + nluData.nlu.size() + " intents with "
                + nluData.nlu.stream().mapToInt(i -> i.examples.split("\n").length).sum()
                + " examples into file:" + output.getPath());
    }

    private static final String intentKey = "intent:";

    public static NLUData2 transform(@NonNull final File intentFileV1) throws IOException {
        List<String> examples = new ArrayList<>();
        List<Intent> intentList = new ArrayList<>();
        for (String line : FileUtils.readLines(intentFileV1, "utf8")) {
            line = line.trim();
            if (StringUtils.isEmpty(line)) {
                continue;
            }
            switch (line.charAt(0)) {
                case '#' -> {
                    int pos = line.indexOf(intentKey);
                    if (pos >= 0) {
                        if (!intentList.isEmpty()) {
                            intentList.get(intentList.size() - 1).examples = String.join("\n", examples);
                        }
                        Intent intent = new Intent();
                        intent.intent = line.substring(pos + intentKey.length());
                        examples.clear();
                        intentList.add(intent);
                    }
                }
                case '-' -> examples.add(line);
                default -> System.err.println("Unkown line:" + line);
            }
        }
        if (!intentList.isEmpty()) {
            intentList.get(intentList.size() - 1).examples = String.join("\n", examples);
        }
        if(min_examples>0){
            int orgsize = intentList.size();
            intentList.removeIf( i -> i.getExampleSize()<min_examples);
            System.out.println("Read intents: "+ orgsize);
            System.out.println("Removed intents with <"+min_examples+" examples: "+ (orgsize-intentList.size()));
        }
        return new NLUData2(intentList);
    }

    static ObjectMapper getYamlMapper() {
        YAMLFactory yamlFactory = new YAMLFactory();
        yamlFactory.enable(YAMLGenerator.Feature.LITERAL_BLOCK_STYLE);
//        yamlFactory.enable(YAMLGenerator.Feature.MINIMIZE_QUOTES);
        return new ObjectMapper(yamlFactory);
    }

    static void printTest() throws IOException {
        Intent intent1 = new Intent();
        intent1.intent = "intent1";
        intent1.examples = String.join("\n", List.of("- Grüß Gott!", "- Guten Tag!"));
        Intent intent2 = new Intent();
        intent2.intent = "intent2";
        intent2.examples = String.join("\n", List.of("- Schönen Abend noch", "- Auf Wiederschauen"));

        System.out.println(getYamlMapper().writeValueAsString(new NLUData2(intent1, intent2)));
    }

    @Data
    @NoArgsConstructor
    static class NLUData2 {
        String version = "2.0";
        final List<Intent> nlu = new ArrayList<>();

        public NLUData2(@NonNull Collection<? extends Intent> intents) {
            nlu.addAll(intents);
        }

        public NLUData2(@NonNull Intent... intents) {
            nlu.addAll(Arrays.asList(intents));
        }
    }

    @Data
    static class Intent {
        String intent;
        String examples;
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String synonym;

        @JsonIgnore
        public int getExampleSize(){
            if(StringUtils.isEmpty(examples)){
                return 0;
            }

            return examples.split("\n+").length;
        }
    }
}
