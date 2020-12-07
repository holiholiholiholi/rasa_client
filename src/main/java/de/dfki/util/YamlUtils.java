package de.dfki.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.NonNull;

import java.io.IOException;
import java.io.InputStream;

public final class YamlUtils {
    public static ObjectMapper getYamlMapper() {
        return new ObjectMapper(new YAMLFactory());
    }

    public static <T> T read(@NonNull final  String file, @NonNull final Class<? extends T> cls) throws IOException{
        try(InputStream is = IOUtils.openStream(file)){
            return getYamlMapper().readValue(is, cls);
        }
    }

}
