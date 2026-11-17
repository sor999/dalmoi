package com.sor999.dalmoi.global.response;

import java.util.List;
import org.springframework.data.domain.Slice;

public record PageResponse<T>(List<T> items, int page, int size, boolean hasNext) {

    public static <T> PageResponse<T> from(Slice<T> slice) {
        return new PageResponse<>(slice.getContent(), slice.getNumber(), slice.getSize(), slice.hasNext());
    }
}
