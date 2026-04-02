package com.skuri.skuri_backend.infra.migration.cutover;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class CutoverBatchUtils {

    private CutoverBatchUtils() {
    }

    static <T> List<List<T>> partition(List<T> source, int chunkSize) {
        if (source.isEmpty()) {
            return List.of();
        }
        if (chunkSize <= 0) {
            throw new IllegalArgumentException("chunkSize는 1 이상이어야 합니다.");
        }
        if (source.size() <= chunkSize) {
            return List.of(List.copyOf(source));
        }

        List<List<T>> partitions = new ArrayList<>();
        for (int start = 0; start < source.size(); start += chunkSize) {
            int end = Math.min(start + chunkSize, source.size());
            partitions.add(Collections.unmodifiableList(source.subList(start, end)));
        }
        return Collections.unmodifiableList(partitions);
    }
}
