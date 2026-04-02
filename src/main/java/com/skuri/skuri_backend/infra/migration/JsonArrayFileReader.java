package com.skuri.skuri_backend.infra.migration;

import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

public final class JsonArrayFileReader {

    private JsonArrayFileReader() {
    }

    public static <T> void read(Path path, ObjectMapper objectMapper, Class<T> elementType, Consumer<T> consumer) {
        ObjectReader objectReader = objectMapper.readerFor(elementType);
        try (var inputStream = Files.newInputStream(path);
             var parser = objectMapper.getFactory().createParser(inputStream)) {
            if (parser.nextToken() != JsonToken.START_ARRAY) {
                throw new IllegalArgumentException("JSON 최상위는 배열이어야 합니다: " + path);
            }

            while (parser.nextToken() != JsonToken.END_ARRAY) {
                consumer.accept(objectReader.readValue(parser));
            }
        } catch (IOException e) {
            throw new IllegalStateException("JSON 배열 파일을 읽는 중 실패했습니다: " + path, e);
        }
    }
}
