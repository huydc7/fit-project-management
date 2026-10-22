package com.example.quanlydetai.repository;

import jakarta.persistence.*;

import org.springframework.data.domain.*;

import java.text.Normalizer;
import java.util.*;

/** Shared binding/paging only. Domain queries stay in their own repositories. */
abstract class QuerySupport {
    @PersistenceContext protected EntityManager em;

    protected <T> Page<T> page(
            String select,
            String from,
            String order,
            String count,
            Class<T> type,
            Map<String, Object> params,
            Pageable page) {
        var query = em.createQuery(select + " " + from + " order by " + order, type);
        var totals =
                em.createQuery(
                        "select " + count + " " + from.replace("join fetch", "join"), Long.class);
        params.forEach(
                (key, value) -> {
                    query.setParameter(key, value);
                    totals.setParameter(key, value);
                });
        long total = totals.getSingleResult();
        var rows =
                query.setFirstResult(Math.toIntExact(page.getOffset()))
                        .setMaxResults(page.getPageSize())
                        .getResultList();
        return new PageImpl<>(rows, page, total);
    }

    protected static void equal(
            StringBuilder where,
            Map<String, Object> params,
            String path,
            String name,
            String value) {
        if (value != null && !value.isBlank()) {
            where.append(" and ").append(path).append("=:").append(name);
            params.put(name, value.strip());
        }
    }

    /** Escape LIKE metacharacters; preserve the previous Vietnamese accent-insensitive search. */
    protected static String contains(String keyword, Map<String, Object> params, String... paths) {
        if (keyword == null || keyword.isBlank()) return "1=1";
        String key =
                Normalizer.normalize(keyword.strip().toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                        .replaceAll("\\p{M}", "")
                        .replace('đ', 'd');
        params.put(
                "keyword",
                "%" + key.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%");
        String[][] accents = {
            {"[\u0300-\u036f]", ""},
            {"[àáảãạăằắẳẵặâầấẩẫậ]", "a"},
            {"[èéẻẽẹêềếểễệ]", "e"},
            {"[ìíỉĩị]", "i"},
            {"[òóỏõọôồốổỗộơờớởỡợ]", "o"},
            {"[ùúủũụưừứửữự]", "u"},
            {"[ỳýỷỹỵ]", "y"},
            {"đ", "d"}
        };
        var terms = new ArrayList<String>();
        for (String path : paths) {
            String expression = "lower(coalesce(" + path + ",''))";
            for (var pair : accents)
                expression =
                        "function('regexp_replace',"
                                + expression
                                + ",'"
                                + pair[0]
                                + "','"
                                + pair[1]
                                + "')";
            terms.add("cast(" + expression + " as String) like :keyword escape '!'");
        }
        return "(" + String.join(" or ", terms) + ")";
    }
}
