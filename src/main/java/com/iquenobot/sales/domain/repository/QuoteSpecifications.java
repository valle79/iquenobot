package com.iquenobot.sales.domain.repository;

import com.iquenobot.sales.domain.entity.Quote;
import com.iquenobot.sales.domain.model.QuoteSearchCriteria;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds the search Specification for quotes using the tenant-scoped criteria.
 */
public final class QuoteSpecifications {

    private QuoteSpecifications() {}

    public static Specification<Quote> withCriteria(QuoteSearchCriteria criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isFalse(root.get("deleted")));

            Join<Object, Object> contact = root.join("contact", JoinType.LEFT);

            if (criteria.query() != null && !criteria.query().isBlank()) {
                String like = "%" + criteria.query().trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("quoteNumber")), like),
                        cb.like(cb.lower(contact.get("fullName")), like),
                        cb.like(cb.lower(contact.get("phone")), like),
                        cb.like(cb.lower(contact.get("whatsappPhone")), like)
                ));
            }
            if (criteria.customer() != null && !criteria.customer().isBlank()) {
                predicates.add(cb.like(cb.lower(contact.get("fullName")),
                        "%" + criteria.customer().trim().toLowerCase() + "%"));
            }
            if (criteria.quoteNumber() != null && !criteria.quoteNumber().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("quoteNumber")),
                        "%" + criteria.quoteNumber().trim().toLowerCase() + "%"));
            }
            if (criteria.phone() != null && !criteria.phone().isBlank()) {
                String like = "%" + criteria.phone().trim() + "%";
                predicates.add(cb.or(
                        cb.like(contact.get("phone"), like),
                        cb.like(contact.get("whatsappPhone"), like),
                        cb.like(contact.get("normalizedPhone"), like)
                ));
            }
            if (criteria.dateFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), criteria.dateFrom()));
            }
            if (criteria.dateTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), criteria.dateTo()));
            }
            if (criteria.status() != null) {
                predicates.add(cb.equal(root.get("status"), criteria.status()));
            }
            if (criteria.botGenerated()) {
                predicates.add(cb.isNull(root.get("createdBy")));
            } else if (criteria.generatedBy() != null) {
                predicates.add(cb.equal(root.get("createdBy"), criteria.generatedBy()));
            }
            if (criteria.minTotal() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("total"), criteria.minTotal()));
            }
            if (criteria.maxTotal() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("total"), criteria.maxTotal()));
            }

            query.distinct(true);
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<Quote> notDeleted() {
        return (root, query, cb) -> cb.isFalse(root.get("deleted"));
    }
}
