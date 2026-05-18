package com.sfaas.amr_control_system.service;

import com.sfaas.amr_control_system.entity.AmrTask;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class WorkHistoryTaskSpecification {

    private WorkHistoryTaskSpecification() {
    }

    static Specification<AmrTask> withFilters(
            String amrId,
            String taskType,
            LocalDateTime from,
            LocalDateTime to,
            String result
    ) {
        return (root, query, criteriaBuilder) -> {
            if (!Long.class.equals(query.getResultType()) && !long.class.equals(query.getResultType())) {
                root.fetch("amr", JoinType.LEFT);
                root.fetch("fromArea", JoinType.LEFT);
                root.fetch("toArea", JoinType.LEFT);
                query.distinct(true);
            }

            List<Predicate> predicates = new ArrayList<>();

            if (amrId != null && !amrId.isBlank()) {
                Integer parsedAmrId = AmrIdentifierHelper.parseAmrId(amrId);
                predicates.add(criteriaBuilder.equal(root.get("amr").get("amrId"), parsedAmrId));
            }

            if (taskType != null && !taskType.isBlank()) {
                predicates.add(criteriaBuilder.equal(
                        criteriaBuilder.lower(root.get("taskType")),
                        taskType.trim().toLowerCase(Locale.ROOT)
                ));
            }

            if (from != null) {
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.isNull(root.get("pickTime")),
                        criteriaBuilder.greaterThanOrEqualTo(root.get("pickTime"), from)
                ));
            }

            if (to != null) {
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.isNull(root.get("pickTime")),
                        criteriaBuilder.lessThanOrEqualTo(root.get("pickTime"), to)
                ));
            }

            Predicate resultPredicate = buildResultPredicate(root, criteriaBuilder, result);
            if (resultPredicate != null) {
                predicates.add(resultPredicate);
            }

            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static Predicate buildResultPredicate(
            jakarta.persistence.criteria.Root<AmrTask> root,
            jakarta.persistence.criteria.CriteriaBuilder criteriaBuilder,
            String result
    ) {
        if (result == null || result.isBlank()) {
            return null;
        }

        String normalized = result.trim().toLowerCase(Locale.ROOT);
        var statusPath = criteriaBuilder.lower(root.get("status"));

        return switch (normalized) {
            case "success" -> criteriaBuilder.or(
                    criteriaBuilder.like(statusPath, "%complete%"),
                    criteriaBuilder.like(statusPath, "%success%"),
                    criteriaBuilder.like(statusPath, "%완료%")
            );
            case "failed" -> criteriaBuilder.or(
                    criteriaBuilder.like(statusPath, "%fail%"),
                    criteriaBuilder.like(statusPath, "%error%"),
                    criteriaBuilder.like(statusPath, "%cancel%")
            );
            case "in_progress" -> criteriaBuilder.or(
                    criteriaBuilder.like(statusPath, "%progress%"),
                    criteriaBuilder.like(statusPath, "%진행%")
            );
            default -> criteriaBuilder.equal(statusPath, normalized);
        };
    }
}
