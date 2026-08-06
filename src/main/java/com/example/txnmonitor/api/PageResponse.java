package com.example.txnmonitor.api;

import org.springframework.data.domain.Page;

import java.util.List;

public record PageResponse<T>(
		List<T> items,
		long totalCount,
		int page,
		int size,
		boolean hasNext) {

	public static <T> PageResponse<T> from(Page<T> page) {
		return new PageResponse<>(
				page.getContent(),
				page.getTotalElements(),
				page.getNumber(),
				page.getSize(),
				page.hasNext());
	}

	public static <T> PageResponse<T> of(List<T> items, long totalCount, int page, int size) {
		boolean hasNext = (long) (page + 1) * size < totalCount;
		return new PageResponse<>(items, totalCount, page, size, hasNext);
	}
}
