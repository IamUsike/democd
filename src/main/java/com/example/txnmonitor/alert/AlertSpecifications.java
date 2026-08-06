package com.example.txnmonitor.alert;

import com.example.txnmonitor.common.PageRequestFactory;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class AlertSpecifications {

	private AlertSpecifications() {
	}

	public static Specification<Alert> withFilters(
			String sourceType,
			String sourceId,
			Collection<String> statuses,
			String severity,
			String accountId,
			String q,
			LocalDateTime createdFrom,
			LocalDateTime createdTo) {
		return (root, query, cb) -> {
			List<Predicate> predicates = new ArrayList<>();

			if (PageRequestFactory.hasText(sourceType)) {
				predicates.add(cb.equal(root.get("sourceType"), PageRequestFactory.normalizeUpper(sourceType)));
			}
			if (PageRequestFactory.hasText(sourceId)) {
				predicates.add(cb.equal(root.get("sourceId"), sourceId.trim()));
			}
			if (statuses != null && !statuses.isEmpty()) {
				predicates.add(root.get("status").in(statuses));
			}
			if (PageRequestFactory.hasText(severity)) {
				predicates.add(cb.equal(root.get("severity"), PageRequestFactory.normalizeUpper(severity)));
			}
			if (PageRequestFactory.hasText(accountId)) {
				predicates.add(cb.equal(root.get("accountId"), accountId.trim()));
			}
			if (createdFrom != null) {
				predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), createdFrom));
			}
			if (createdTo != null) {
				predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), createdTo));
			}
			if (PageRequestFactory.hasText(q)) {
				String pattern = "%" + q.trim().toLowerCase() + "%";
				predicates.add(cb.or(
						cb.like(cb.lower(root.get("accountId")), pattern),
						cb.like(cb.lower(root.get("sourceId")), pattern),
						cb.like(cb.lower(root.get("sourceName")), pattern),
						cb.like(cb.lower(root.get("ruleType")), pattern)));
			}

			return cb.and(predicates.toArray(Predicate[]::new));
		};
	}
}
