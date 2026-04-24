package com.civicplatform.service;

import com.civicplatform.entity.EventCitizenInvitation;
import com.civicplatform.enums.InvitationTier;
import com.civicplatform.enums.MatchStatus;
import com.civicplatform.enums.UserType;
import com.civicplatform.repository.EventCitizenInvitationRepository;
import jakarta.persistence.criteria.JoinType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

/**
 * Supports paginated invitation search with a constrained JQL-style syntax.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EventInvitationSearchService {

    private static final Pattern CLAUSE_PATTERN = Pattern.compile(
            "^(\\w+)\\s*(=|!=|>=|<=|>|<|~)\\s*(.+)$",
            Pattern.CASE_INSENSITIVE
    );

    private final EventCitizenInvitationRepository invitationRepository;

    /**
     * Applies event-scoped search using supported JQL clauses joined by AND.
     */
    @Transactional(readOnly = true)
    public Page<EventCitizenInvitation> searchEventInvitations(Long eventId, String jql, Pageable pageable) {
        Specification<EventCitizenInvitation> spec = Specification.where(
                (root, query, cb) -> cb.equal(root.get("event").get("id"), eventId)
        );
        if (jql != null && !jql.isBlank()) {
            spec = spec.and(parseToSpecification(jql));
        }
        return invitationRepository.findAll(spec, pageable);
    }

    private Specification<EventCitizenInvitation> parseToSpecification(String jql) {
        String[] clauses = jql.split("(?i)\\s+AND\\s+");
        List<Specification<EventCitizenInvitation>> specs = new ArrayList<>();

        for (String rawClause : clauses) {
            String clause = rawClause.trim();
            if (clause.isEmpty()) {
                continue;
            }
            Matcher matcher = CLAUSE_PATTERN.matcher(clause);
            if (!matcher.matches()) {
                throw badRequest("Invalid JQL clause: " + clause);
            }
            String field = matcher.group(1).trim().toLowerCase(Locale.ROOT);
            String op = matcher.group(2).trim();
            String value = unwrapQuoted(matcher.group(3).trim());
            specs.add(toSpecification(field, op, value));
        }

        return specs.stream().reduce(Specification::and).orElse((root, query, cb) -> cb.conjunction());
    }

    private Specification<EventCitizenInvitation> toSpecification(String field, String op, String value) {
        try {
            return switch (field) {
                case "status" -> enumSpec("status", MatchStatus.valueOf(value.toUpperCase(Locale.ROOT)), op);
                case "tier", "invitationtier" -> enumSpec("invitationTier", InvitationTier.valueOf(value.toUpperCase(Locale.ROOT)), op);
                case "compositerate" -> numberSpec("compositeRate", Double.parseDouble(value), op);
                case "matchscore" -> numberSpec("matchScore", Double.parseDouble(value), op);
                case "priorityfollowup" -> boolSpec("priorityFollowup", Boolean.parseBoolean(value), op);
                case "invitedat" -> dateSpec("invitedAt", LocalDateTime.parse(value), op);
                case "citizenid" -> numberSpec("citizen.id", Long.parseLong(value), op);
                case "citizenusertype" -> enumSpec("citizen.userType", UserType.valueOf(value.toUpperCase(Locale.ROOT)), op);
                case "citizenname" -> containsSpec("citizen.userName", value, op);
                default -> throw badRequest("Unsupported JQL field: " + field);
            };
        } catch (IllegalArgumentException ex) {
            throw badRequest("Invalid JQL value for field '" + field + "': " + value);
        }
    }

    private Specification<EventCitizenInvitation> enumSpec(String field, Enum<?> value, String op) {
        if (!"=".equals(op) && !"!=".equals(op)) {
            throw badRequest("Field supports only = and != operators");
        }
        return (root, query, cb) -> {
            var path = resolvePath(root, field);
            return "=".equals(op) ? cb.equal(path, value) : cb.notEqual(path, value);
        };
    }

    private Specification<EventCitizenInvitation> boolSpec(String field, boolean value, String op) {
        if (!"=".equals(op) && !"!=".equals(op)) {
            throw badRequest("Boolean field supports only = and != operators");
        }
        return (root, query, cb) -> {
            var path = resolvePath(root, field);
            return "=".equals(op) ? cb.equal(path, value) : cb.notEqual(path, value);
        };
    }

    private Specification<EventCitizenInvitation> numberSpec(String field, Number value, String op) {
        return (root, query, cb) -> {
            var path = resolvePath(root, field);
            return switch (op) {
                case "=" -> cb.equal(path, value);
                case "!=" -> cb.notEqual(path, value);
                case ">" -> cb.gt(path.as(Number.class), value);
                case ">=" -> cb.ge(path.as(Number.class), value);
                case "<" -> cb.lt(path.as(Number.class), value);
                case "<=" -> cb.le(path.as(Number.class), value);
                default -> throw badRequest("Unsupported numeric operator: " + op);
            };
        };
    }

    private Specification<EventCitizenInvitation> dateSpec(String field, LocalDateTime value, String op) {
        return (root, query, cb) -> {
            var path = resolvePath(root, field).as(LocalDateTime.class);
            return switch (op) {
                case "=" -> cb.equal(path, value);
                case "!=" -> cb.notEqual(path, value);
                case ">" -> cb.greaterThan(path, value);
                case ">=" -> cb.greaterThanOrEqualTo(path, value);
                case "<" -> cb.lessThan(path, value);
                case "<=" -> cb.lessThanOrEqualTo(path, value);
                default -> throw badRequest("Unsupported date operator: " + op);
            };
        };
    }

    private Specification<EventCitizenInvitation> containsSpec(String field, String value, String op) {
        if (!"~".equals(op) && !"=".equals(op)) {
            throw badRequest("String field supports only = and ~ operators");
        }
        return (root, query, cb) -> {
            var path = resolvePath(root, field).as(String.class);
            if ("=".equals(op)) {
                return cb.equal(cb.lower(path), value.toLowerCase(Locale.ROOT));
            }
            return cb.like(cb.lower(path), "%" + value.toLowerCase(Locale.ROOT) + "%");
        };
    }

    private jakarta.persistence.criteria.Path<?> resolvePath(jakarta.persistence.criteria.Root<EventCitizenInvitation> root, String dotPath) {
        String[] parts = dotPath.split("\\.");
        if (parts.length == 1) {
            return root.get(parts[0]);
        }
        if ("citizen".equals(parts[0])) {
            return root.join("citizen", JoinType.LEFT).get(parts[1]);
        }
        if ("event".equals(parts[0])) {
            return root.join("event", JoinType.LEFT).get(parts[1]);
        }
        throw badRequest("Unsupported path: " + dotPath);
    }

    private String unwrapQuoted(String value) {
        if ((value.startsWith("'") && value.endsWith("'")) || (value.startsWith("\"") && value.endsWith("\""))) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private ResponseStatusException badRequest(String message) {
        log.debug("Invalid invitation JQL: {}", message);
        return new ResponseStatusException(BAD_REQUEST, message);
    }
}
