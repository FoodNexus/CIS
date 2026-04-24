package com.civicplatform.service;

import com.civicplatform.entity.Event;
import com.civicplatform.enums.EventStatus;
import com.civicplatform.enums.EventType;
import com.civicplatform.repository.EventRepository;
import jakarta.persistence.criteria.Path;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provides paginated event search using a constrained JQL-like query syntax.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EventSearchService {

    private static final Pattern CLAUSE_PATTERN = Pattern.compile(
            "^(\\w+)\\s*(=|!=|>=|<=|>|<|~)\\s*(.+)$",
            Pattern.CASE_INSENSITIVE
    );

    private final EventRepository eventRepository;

    /**
     * Searches events with optional JQL-like clauses joined by AND.
     */
    @Transactional(readOnly = true)
    public Page<Event> search(String jql, Pageable pageable) {
        Specification<Event> spec = Specification.where((root, query, cb) -> cb.conjunction());
        if (jql != null && !jql.isBlank()) {
            spec = spec.and(parseToSpec(jql));
        }
        return eventRepository.findAll(spec, pageable);
    }

    private Specification<Event> parseToSpec(String jql) {
        String[] clauses = jql.split("(?i)\\s+AND\\s+");
        List<Specification<Event>> out = new ArrayList<>();
        for (String raw : clauses) {
            String clause = raw.trim();
            if (clause.isEmpty()) {
                continue;
            }
            Matcher matcher = CLAUSE_PATTERN.matcher(clause);
            if (!matcher.matches()) {
                throw badRequest("Invalid JQL clause: " + clause);
            }
            String field = matcher.group(1).trim().toLowerCase(Locale.ROOT);
            String op = matcher.group(2).trim();
            String value = unquote(matcher.group(3).trim());
            out.add(toSpec(field, op, value));
        }
        return out.stream().reduce(Specification::and).orElse((root, query, cb) -> cb.conjunction());
    }

    private Specification<Event> toSpec(String field, String op, String value) {
        try {
            return switch (field) {
                case "status" -> enumSpec("status", EventStatus.valueOf(value.toUpperCase(Locale.ROOT)), op);
                case "type" -> enumSpec("type", EventType.valueOf(value.toUpperCase(Locale.ROOT)), op);
                case "organizerid" -> numberSpec("organizerId", Long.parseLong(value), op);
                case "title", "location" -> stringSpec(field, value, op);
                case "date" -> dateSpec("date", LocalDateTime.parse(value), op);
                default -> throw badRequest("Unsupported JQL field: " + field);
            };
        } catch (IllegalArgumentException ex) {
            throw badRequest("Invalid value '" + value + "' for field '" + field + "'");
        }
    }

    private Specification<Event> enumSpec(String field, Enum<?> value, String op) {
        if (!"=".equals(op) && !"!=".equals(op)) {
            throw badRequest("Enum fields support only = and !=");
        }
        return (root, query, cb) -> "=".equals(op) ? cb.equal(root.get(field), value) : cb.notEqual(root.get(field), value);
    }

    private Specification<Event> numberSpec(String field, Number value, String op) {
        return (root, query, cb) -> {
            Path<Number> path = root.get(field);
            return switch (op) {
                case "=" -> cb.equal(path, value);
                case "!=" -> cb.notEqual(path, value);
                case ">" -> cb.gt(path, value);
                case ">=" -> cb.ge(path, value);
                case "<" -> cb.lt(path, value);
                case "<=" -> cb.le(path, value);
                default -> throw badRequest("Unsupported numeric operator: " + op);
            };
        };
    }

    private Specification<Event> dateSpec(String field, LocalDateTime value, String op) {
        return (root, query, cb) -> {
            Path<LocalDateTime> path = root.get(field);
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

    private Specification<Event> stringSpec(String field, String value, String op) {
        if (!"=".equals(op) && !"~".equals(op)) {
            throw badRequest("String fields support only = and ~");
        }
        return (root, query, cb) -> {
            Path<String> path = root.get(field);
            if ("=".equals(op)) {
                return cb.equal(cb.lower(path), value.toLowerCase(Locale.ROOT));
            }
            return cb.like(cb.lower(path), "%" + value.toLowerCase(Locale.ROOT) + "%");
        };
    }

    private String unquote(String value) {
        if ((value.startsWith("'") && value.endsWith("'")) || (value.startsWith("\"") && value.endsWith("\""))) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private ResponseStatusException badRequest(String message) {
        log.debug("Invalid event search JQL: {}", message);
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
