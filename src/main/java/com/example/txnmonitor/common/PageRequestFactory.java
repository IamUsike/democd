package com.example.txnmonitor.common;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Locale;
import java.util.Set;

public final class PageRequestFactory {

	public static final int DEFAULT_PAGE = 0;
	public static final int DEFAULT_SIZE = 50;
	public static final int MAX_SIZE = 200;

	private PageRequestFactory() {
	}

	public static Pageable create(Integer page, Integer size, Sort sort) {
		int safePage = page == null || page < 0 ? DEFAULT_PAGE : page;
		int safeSize = size == null || size < 1 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
		return PageRequest.of(safePage, safeSize, sort);
	}

	public static Sort parseSort(String sort, String defaultProperty, Set<String> allowedProperties) {
		String property = defaultProperty;
		Sort.Direction direction = Sort.Direction.DESC;

		if (sort != null && !sort.isBlank()) {
			String[] parts = sort.split(",", 2);
			String candidate = parts[0].trim();
			if (allowedProperties.contains(candidate)) {
				property = candidate;
			}
			if (parts.length > 1 && "asc".equalsIgnoreCase(parts[1].trim())) {
				direction = Sort.Direction.ASC;
			}
		}

		return Sort.by(direction, property);
	}

	public static boolean hasText(String value) {
		return value != null && !value.isBlank();
	}

	public static String normalizeUpper(String value) {
		return value.trim().toUpperCase(Locale.ROOT);
	}
}
