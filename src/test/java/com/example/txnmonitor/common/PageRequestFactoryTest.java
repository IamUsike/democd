package com.example.txnmonitor.common;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PageRequestFactoryTest {

	@Test
	void create_nullPageAndSize_usesDefaults() {
		Pageable pageable = PageRequestFactory.create(null, null, Sort.by("createdAt").descending());

		assertEquals(0, pageable.getPageNumber());
		assertEquals(50, pageable.getPageSize());
	}

	@Test
	void create_sizeAboveMax_clampsToMax() {
		Pageable pageable = PageRequestFactory.create(1, 999, Sort.unsorted());

		assertEquals(1, pageable.getPageNumber());
		assertEquals(200, pageable.getPageSize());
	}

	@Test
	void parseSort_validAsc_usesDirection() {
		Sort sort = PageRequestFactory.parseSort("severity,asc", "createdAt", Set.of("createdAt", "severity"));

		assertEquals("severity", sort.iterator().next().getProperty());
		assertTrue(sort.getOrderFor("severity").isAscending());
	}

	@Test
	void parseSort_unknownProperty_fallsBackToDefault() {
		Sort sort = PageRequestFactory.parseSort("hacked,desc", "createdAt", Set.of("createdAt", "severity"));

		assertEquals("createdAt", sort.iterator().next().getProperty());
	}
}
