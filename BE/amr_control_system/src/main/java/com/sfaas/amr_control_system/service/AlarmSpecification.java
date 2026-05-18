package com.sfaas.amr_control_system.service;

import com.sfaas.amr_control_system.entity.Alarm;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class AlarmSpecification {

    private AlarmSpecification() {
    }

    static Specification<Alarm> withFilters(
            String level,
            Boolean acknowledged,
            String sourceType,
            String sourceId,
            LocalDateTime from,
            LocalDateTime to
    ) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (level != null && !level.isBlank()) {
                predicates.add(criteriaBuilder.equal(
                        criteriaBuilder.lower(root.get("level")),
                        level.trim().toLowerCase(Locale.ROOT)
                ));
            }

            if (acknowledged != null) {
                predicates.add(criteriaBuilder.equal(root.get("acknowledged"), acknowledged));
            }

            if (sourceType != null && !sourceType.isBlank()) {
                predicates.add(criteriaBuilder.equal(
                        criteriaBuilder.lower(root.get("sourceType")),
                        sourceType.trim().toLowerCase(Locale.ROOT)
                ));
            }

            if (sourceId != null && !sourceId.isBlank()) {
                predicates.add(criteriaBuilder.equal(
                        criteriaBuilder.lower(root.get("sourceId")),
                        sourceId.trim().toLowerCase(Locale.ROOT)
                ));
            }

            if (from != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("occurredAt"), from));
            }

            if (to != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("occurredAt"), to));
            }

            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }
}
