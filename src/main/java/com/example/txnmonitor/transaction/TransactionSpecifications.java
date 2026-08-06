package com.example.txnmonitor.transaction;

import com.example.txnmonitor.common.PageRequestFactory;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * JPA Specifications for {@code GET /transactions}.
 * {@code afterId} supports delta polling (only newer transaction ids).
 * {@code q} fuzzy-matches account, source, payee, description, and id text.
 */
public final class TransactionSpecifications {

	private TransactionSpecifications() {
	}

	public static Specification<Transaction> withFilters(
			String sourceType,
			String sourceId,
			String accountId,
			String q,
			LocalDateTime from,
			LocalDateTime to,
			Long afterId) {
		return (root, query, cb) -> {
			List<Predicate> predicates = new ArrayList<>();

			if (PageRequestFactory.hasText(sourceType)) {
				predicates.add(cb.equal(root.get("sourceType"), PageRequestFactory.normalizeUpper(sourceType)));
			}
			if (PageRequestFactory.hasText(sourceId)) {
				predicates.add(cb.equal(root.get("sourceId"), sourceId.trim()));
			}
			if (PageRequestFactory.hasText(accountId)) {
				predicates.add(cb.equal(root.get("accountId"), accountId.trim()));
			}
			if (from != null) {
				predicates.add(cb.greaterThanOrEqualTo(root.get("timestamp"), from));
			}
			if (to != null) {
				predicates.add(cb.lessThanOrEqualTo(root.get("timestamp"), to));
			}
			// Live feed: only rows newer than the client's last known id.
			if (afterId != null) {
				predicates.add(cb.greaterThan(root.get("transactionId"), afterId));
			}
			if (PageRequestFactory.hasText(q)) {
				String pattern = "%" + q.trim().toLowerCase() + "%";
				predicates.add(cb.or(
						cb.like(cb.lower(root.get("accountId")), pattern),
						cb.like(cb.lower(root.get("sourceId")), pattern),
						cb.like(cb.lower(root.get("sourceName")), pattern),
						cb.like(cb.lower(root.get("payeeId")), pattern),
						cb.like(cb.lower(cb.coalesce(root.get("payeeName"), "")), pattern),
						cb.like(cb.lower(cb.coalesce(root.get("description"), "")), pattern),
						cb.like(cb.lower(root.get("transactionId").as(String.class)), pattern)));
			}

			return cb.and(predicates.toArray(Predicate[]::new));
		};
	}
}
