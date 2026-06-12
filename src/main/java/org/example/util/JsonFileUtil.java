package org.example.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class JsonFileUtil {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    private JsonFileUtil() {}

    public static <T> List<T> readList(File file, Class<T> clazz) {
        if (!file.exists() || file.length() == 0) {
            return new ArrayList<>();
        }
        try {
            return MAPPER.readValue(
                    file,
                    MAPPER.getTypeFactory().constructCollectionType(List.class, clazz));
        } catch (IOException e) {
            throw new RuntimeException("JSON 파일 읽기 실패: " + file.getPath(), e);
        }
    }

    public static <T> void writeList(File file, List<T> list) {
        if (file.getParentFile() != null) {
            file.getParentFile().mkdirs();
        }
        try {
            MAPPER.writeValue(file, list);
        } catch (IOException e) {
            throw new RuntimeException("JSON 파일 쓰기 실패: " + file.getPath(), e);
        }
    }
}
