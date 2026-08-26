package com.serbekun.ss.service.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Produces an RFC 6902 JSON Patch that turns one document into another.
 * <p>
 * Only {@code add}, {@code remove} and {@code replace} are emitted. {@code move}
 * and {@code copy} are optional in the RFC and only ever shorten a patch, never
 * change what it does, so leaving them out costs nothing but size.
 * <p>
 * Arrays are the interesting part. Two arrays of the same length are compared
 * element by element, which lets a change deep inside one element come out as a
 * single nested {@code replace} rather than a rewrite of the whole slot. When
 * the lengths differ, a longest-common-subsequence match works out what was
 * actually inserted or dropped, so adding one element to the front of a
 * thousand-element array is one operation and not a thousand.
 */
final class JsonDiff {

    /**
     * Largest LCS table this will build, in cells. Past it the arrays are
     * compared by index and the tail is added or removed — still a correct
     * patch, just a longer one than a full match would give.
     */
    private static final int MAX_LCS_CELLS = 1_000_000;

    private JsonDiff() {
    }

    /**
     * Builds the patch from {@code from} to {@code to}.
     *
     * @param from the document as it is
     * @param to the document as it should be
     * @return the operations, in the order they must be applied
     */
    static ArrayNode diff(JsonNode from, JsonNode to) {
        ArrayNode patch = JsonNodeFactory.instance.arrayNode();
        diff("", from, to, patch);
        return patch;
    }

    /**
     * Whether two nodes hold the same JSON value.
     * <p>
     * Numbers are compared by value rather than by type, because RFC 6902 says
     * {@code 1} and {@code 1.0} are the same number — Jackson's own
     * {@code equals} would call them different and report a change that is not
     * one.
     */
    static boolean sameValue(JsonNode a, JsonNode b) {
        if (a == null || b == null) {
            return a == b;
        }
        if (a.isNumber() && b.isNumber()) {
            return a.decimalValue().compareTo(b.decimalValue()) == 0;
        }
        if (a.getNodeType() != b.getNodeType()) {
            return false;
        }

        if (a.isObject()) {
            if (a.size() != b.size()) {
                return false;
            }
            var fields = a.fieldNames();
            while (fields.hasNext()) {
                String field = fields.next();
                if (!b.has(field) || !sameValue(a.get(field), b.get(field))) {
                    return false;
                }
            }
            return true;
        }

        if (a.isArray()) {
            if (a.size() != b.size()) {
                return false;
            }
            for (int i = 0; i < a.size(); i++) {
                if (!sameValue(a.get(i), b.get(i))) {
                    return false;
                }
            }
            return true;
        }

        return a.equals(b);
    }

    // region walk

    private static void diff(String path, JsonNode from, JsonNode to, ArrayNode patch) {
        if (sameValue(from, to)) {
            return;
        }

        if (from.isObject() && to.isObject()) {
            diffObjects(path, from, to, patch);
        } else if (from.isArray() && to.isArray()) {
            diffArrays(path, (ArrayNode) from, (ArrayNode) to, patch);
        } else {
            patch.add(operation("replace", path, to));
        }
    }

    private static void diffObjects(String path, JsonNode from, JsonNode to, ArrayNode patch) {
        var removed = from.fieldNames();
        while (removed.hasNext()) {
            String field = removed.next();
            if (!to.has(field)) {
                patch.add(operation("remove", path + "/" + escape(field), null));
            }
        }

        var fields = to.fieldNames();
        while (fields.hasNext()) {
            String field = fields.next();
            String child = path + "/" + escape(field);
            if (!from.has(field)) {
                patch.add(operation("add", child, to.get(field)));
            } else {
                diff(child, from.get(field), to.get(field), patch);
            }
        }
    }

    private static void diffArrays(String path, ArrayNode from, ArrayNode to, ArrayNode patch) {
        if (from.size() == to.size()) {
            for (int i = 0; i < from.size(); i++) {
                diff(path + "/" + i, from.get(i), to.get(i), patch);
            }
            return;
        }

        if ((long) from.size() * to.size() > MAX_LCS_CELLS) {
            diffArrayTails(path, from, to, patch);
            return;
        }

        diffArraysByLcs(path, from, to, patch);
    }

    /**
     * Matches the two arrays with an LCS table, then removes what is not in the
     * match and adds what is new.
     * <p>
     * Removals go from the highest index down and additions from the lowest up,
     * which is what keeps every index in the patch valid at the moment its own
     * operation is applied.
     */
    private static void diffArraysByLcs(String path, ArrayNode from, ArrayNode to, ArrayNode patch) {
        int rows = from.size();
        int columns = to.size();

        int[][] lengths = new int[rows + 1][columns + 1];
        for (int i = rows - 1; i >= 0; i--) {
            for (int j = columns - 1; j >= 0; j--) {
                lengths[i][j] = sameValue(from.get(i), to.get(j))
                    ? lengths[i + 1][j + 1] + 1
                    : Math.max(lengths[i + 1][j], lengths[i][j + 1]);
            }
        }

        // Walk the table forwards to label each element: kept, removed or added.
        boolean[] keptInFrom = new boolean[rows];
        boolean[] keptInTo = new boolean[columns];
        int i = 0;
        int j = 0;
        while (i < rows && j < columns) {
            if (sameValue(from.get(i), to.get(j))) {
                keptInFrom[i] = true;
                keptInTo[j] = true;
                i++;
                j++;
            } else if (lengths[i + 1][j] >= lengths[i][j + 1]) {
                i++;
            } else {
                j++;
            }
        }

        for (int index = rows - 1; index >= 0; index--) {
            if (!keptInFrom[index]) {
                patch.add(operation("remove", path + "/" + index, null));
            }
        }
        for (int index = 0; index < columns; index++) {
            if (!keptInTo[index]) {
                patch.add(operation("add", path + "/" + index, to.get(index)));
            }
        }
    }

    /** The fallback for arrays too large to match: compare in place, then fix the length. */
    private static void diffArrayTails(String path, ArrayNode from, ArrayNode to, ArrayNode patch) {
        int shared = Math.min(from.size(), to.size());
        for (int i = 0; i < shared; i++) {
            diff(path + "/" + i, from.get(i), to.get(i), patch);
        }

        for (int i = from.size() - 1; i >= shared; i--) {
            patch.add(operation("remove", path + "/" + i, null));
        }
        for (int i = shared; i < to.size(); i++) {
            // "-" is the RFC 6901 index that means "one past the end".
            patch.add(operation("add", path + "/-", to.get(i)));
        }
    }

    // endregion

    private static ObjectNode operation(String op, String path, JsonNode value) {
        ObjectNode node = JsonNodeFactory.instance.objectNode();
        node.put("op", op);
        node.put("path", path);
        if (value != null) {
            node.set("value", value.deepCopy());
        }
        return node;
    }

    /** RFC 6901 escaping: a literal {@code ~} and {@code /} in a key have to be spelled out. */
    private static String escape(String field) {
        return field.replace("~", "~0").replace("/", "~1");
    }
}
