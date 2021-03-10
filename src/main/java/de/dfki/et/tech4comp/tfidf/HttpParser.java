package de.dfki.et.tech4comp.tfidf;

import lombok.NonNull;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class HttpParser {
    public static void main(String args[]) throws IOException {
        File dir = new File("/Users/lihong/projects/DFKI_ET/tech4comp/data/TestBed_UD/folien_txt");

        Map<String, Integer> tmap = new HashMap<>();
        for (File f : dir.listFiles(f -> f.getName().endsWith(".txt"))) {
            List<String> texts = FileUtils.readLines(f, "utf8");
            for(int i=0;i<texts.size();i++){
                String l = texts.get(i).trim();
                int pos = l.indexOf("http");
                if(pos<0 ){
                    continue;
                }
                int space = l.indexOf(' ',pos);
                if(space>pos || l.endsWith("/") || i== texts.size()-1){
                    if(space>pos) {
                        System.out.println(l.substring(pos, space));
                    }else{
                        System.out.println(l.substring(pos));
                    }
                }else{
                    int last = l.lastIndexOf("/");
                    String file = l.substring(last);
                    if(!file.contains(".")){
                        String next =texts.get(i+1);
                        System.out.println(l.substring(pos)+next.split(" ")[0]);
                    }
                }
            }

        }

    }
}
