package com.serbekun.ss.service.json;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the JSON Patch generator.
 * <p>
 * The load-bearing test is {@link #everyPatchAppliesBackToTheTarget()}: a patch
 * is only correct if applying it to the first document yields the second, so
 * these apply it, with an applier written here rather than the one that
 * produced it. Everything else checks that the patch is also <i>small</i> —
 * which is what makes a diff readable rather than merely valid.
 */
class JsonDiffTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static JsonNode json(String text) {
        try {
            return MAPPER.readTree(text);
        } catch (Exception e) {
            throw new IllegalArgumentException(text, e);
        }
    }

    private static ArrayNode diff(String from, String to) {
        return JsonDiff.diff(json(from), json(to));
    }

    // region correctness

    @Test
    void everyPatchAppliesBackToTheTarget() {
        List<String[]> pairs = List.of(
            new String[] {"{}", "{}"},
            new String[] {"{}", "{\"a\":1}"},
            new String[] {"{\"a\":1}", "{}"},
            new String[] {"{\"a\":1}", "{\"a\":2}"},
            new String[] {"{\"a\":{\"b\":{\"c\":1}}}", "{\"a\":{\"b\":{\"c\":2,\"d\":3}}}"},
            new String[] {"{\"a\":[1,2,3]}", "{\"a\":[1,2,3,4]}"},
            new String[] {"{\"a\":[1,2,3]}", "{\"a\":[0,1,2,3]}"},
            new String[] {"{\"a\":[1,2,3]}", "{\"a\":[1,3]}"},
            new String[] {"{\"a\":[1,2,3]}", "{\"a\":[]}"},
            new String[] {"[]", "[1,2,3]"},
            new String[] {"[1,2,3]", "[3,2,1]"},
            new String[] {"[{\"id\":1,\"v\":\"a\"},{\"id\":2,\"v\":\"b\"}]",
                          "[{\"id\":1,\"v\":\"z\"},{\"id\":2,\"v\":\"b\"}]"},
            new String[] {"[{\"id\":1},{\"id\":2},{\"id\":3}]", "[{\"id\":1},{\"id\":3}]"},
            new String[] {"{\"a\":1}", "[1,2]"},
            new String[] {"null", "{\"a\":null}"},
            new String[] {"{\"a\":{\"b\":1},\"c\":[1,{\"d\":2}]}", "{\"c\":[1,{\"d\":3},4],\"e\":true}"},
            new String[] {"{\"a/b\":1,\"c~d\":2}", "{\"a/b\":9,\"c~d\":2,\"e\":3}"});

        for (String[] pair : pairs) {
            JsonNode from = json(pair[0]);
            JsonNode to = json(pair[1]);
            JsonNode patched = apply(from.deepCopy(), JsonDiff.diff(from, to));

            assertThat(JsonDiff.sameValue(patched, to))
                .as("%s + patch == %s, got %s", pair[0], pair[1], patched)
                .isTrue();
        }
    }

    @Test
    void identicalDocumentsProduceNoOperations() {
        assertThat(diff("{\"a\":[1,2,{\"b\":null}]}", "{\"a\":[1,2,{\"b\":null}]}")).isEmpty();

        // Key order is not part of a JSON object's value.
        assertThat(diff("{\"a\":1,\"b\":2}", "{\"b\":2,\"a\":1}")).isEmpty();
    }

    @Test
    void numbersAreComparedByValueNotByType() {
        // RFC 6902 compares numbers by value: 1 and 1.0 are the same number,
        // and reporting a change between them would be a change that is not one.
        assertThat(diff("{\"a\":1}", "{\"a\":1.0}")).isEmpty();
        assertThat(diff("{\"a\":1e2}", "{\"a\":100}")).isEmpty();
        assertThat(diff("{\"a\":1}", "{\"a\":1.5}")).hasSize(1);
    }

    // endregion

    // region shape

    @Test
    void objectChangesAreNamedPreciselyAndNested() {
        ArrayNode patch = diff(
            "{\"keep\":1,\"gone\":2,\"deep\":{\"x\":1,\"y\":2}}",
            "{\"keep\":1,\"added\":3,\"deep\":{\"x\":9,\"y\":2}}");

        assertThat(patch).hasSize(3);
        assertThat(operations(patch)).containsExactlyInAnyOrder(
            "remove /gone",
            "add /added",
            // A change deep inside is one operation at its own path, not a
            // rewrite of everything above it.
            "replace /deep/x");
    }

    @Test
    void oneInsertionIntoALongArrayIsOneOperation() {
        StringBuilder from = new StringBuilder("[");
        // A value that appears nowhere else, so there is exactly one right answer:
        // with a repeated element the match could legitimately land either side of it.
        StringBuilder to = new StringBuilder("[-1,");
        for (int i = 0; i < 500; i++) {
            from.append(i).append(i == 499 ? "" : ",");
            to.append(i).append(i == 499 ? "" : ",");
        }
        ArrayNode patch = diff(from.append("]").toString(), to.append("]").toString());

        // Without a proper match this would be 501 operations.
        assertThat(patch).hasSize(1);
        assertThat(patch.get(0).get("op").asText()).isEqualTo("add");
        assertThat(patch.get(0).get("path").asText()).isEqualTo("/0");
    }

    @Test
    void oneRemovalFromTheMiddleIsOneOperation() {
        ArrayNode patch = diff("[\"a\",\"b\",\"c\",\"d\"]", "[\"a\",\"b\",\"d\"]");

        assertThat(patch).hasSize(1);
        assertThat(operations(patch)).containsExactly("remove /2");
    }

    @Test
    void aChangeInsideAnArrayElementStaysInsideThatElement() {
        // Same length, so the elements line up and the diff can go deeper than
        // "this slot is different".
        ArrayNode patch = diff(
            "[{\"id\":1,\"name\":\"one\"},{\"id\":2,\"name\":\"two\"}]",
            "[{\"id\":1,\"name\":\"ONE\"},{\"id\":2,\"name\":\"two\"}]");

        assertThat(patch).hasSize(1);
        assertThat(operations(patch)).containsExactly("replace /0/name");
        assertThat(patch.get(0).get("value").asText()).isEqualTo("ONE");
    }

    @Test
    void keysWithSlashesOrTildesAreEscapedInThePath() {
        ArrayNode patch = diff("{\"a/b\":1,\"c~d\":2}", "{\"a/b\":2,\"c~d\":3}");

        // RFC 6901: '/' is ~1 and '~' is ~0, or the path would parse as a step.
        assertThat(operations(patch)).containsExactlyInAnyOrder("replace /a~1b", "replace /c~0d");
    }

    @Test
    void aReplacedValueCarriesTheNewValueAndARemovalDoesNot() {
        ArrayNode patch = diff("{\"a\":1,\"b\":2}", "{\"a\":{\"deep\":true}}");

        for (JsonNode operation : patch) {
            if (operation.get("op").asText().equals("remove")) {
                assertThat(operation.has("value")).isFalse();
            } else {
                assertThat(operation.has("value")).isTrue();
            }
        }
        assertThat(operations(patch)).containsExactlyInAnyOrder("remove /b", "replace /a");
    }

    // endregion

    // region helpers

    private static List<String> operations(ArrayNode patch) {
        List<String> described = new java.util.ArrayList<>();
        patch.forEach(op -> described.add(op.get("op").asText() + " " + op.get("path").asText()));
        return described;
    }

    /**
     * A small RFC 6902 applier — add, remove and replace, which is all this
     * generator emits. Written here so the patch is checked against something
     * other than the code that produced it.
     */
    private static JsonNode apply(JsonNode document, ArrayNode patch) {
        JsonNode root = document;

        for (JsonNode operation : patch) {
            String op = operation.get("op").asText();
            String path = operation.get("path").asText();
            JsonNode value = operation.get("value");

            if (path.isEmpty()) {
                root = value;
                continue;
            }

            List<String> steps = steps(path);
            JsonNode parent = root;
            for (int i = 0; i < steps.size() - 1; i++) {
                parent = parent.isArray()
                    ? parent.get(Integer.parseInt(steps.get(i)))
                    : parent.get(steps.get(i));
            }
            String last = steps.get(steps.size() - 1);

            if (parent.isArray()) {
                ArrayNode array = (ArrayNode) parent;
                switch (op) {
                    case "add" -> {
                        if (last.equals("-")) {
                            array.add(value);
                        } else {
                            array.insert(Integer.parseInt(last), value);
                        }
                    }
                    case "remove" -> array.remove(Integer.parseInt(last));
                    case "replace" -> array.set(Integer.parseInt(last), value);
                    default -> throw new IllegalStateException("unexpected op " + op);
                }
            } else {
                ObjectNode object = (ObjectNode) parent;
                switch (op) {
                    case "add", "replace" -> object.set(last, value);
                    case "remove" -> object.remove(last);
                    default -> throw new IllegalStateException("unexpected op " + op);
                }
            }
        }

        return root;
    }

    private static List<String> steps(String pointer) {
        List<String> steps = new java.util.ArrayList<>();
        for (String step : pointer.substring(1).split("/", -1)) {
            steps.add(step.replace("~1", "/").replace("~0", "~"));
        }
        return steps;
    }

    // endregion
}
