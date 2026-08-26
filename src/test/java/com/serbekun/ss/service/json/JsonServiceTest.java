package com.serbekun.ss.service.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link JsonService}.
 * <p>
 * Two behaviours here are deliberate and would look like bugs without a test
 * saying so: duplicate keys are refused rather than silently collapsed, and
 * long decimals survive a round trip that {@code double} would round off.
 */
class JsonServiceTest {

    /** The document JSONPath examples are traditionally written against. */
    private static final String STORE = """
        {
          "store": {
            "book": [
              {"category": "reference", "author": "Nigel Rees", "title": "Sayings of the Century", "price": 8.95},
              {"category": "fiction", "author": "Evelyn Waugh", "title": "Sword of Honour", "price": 12.99},
              {"category": "fiction", "author": "Herman Melville", "title": "Moby Dick", "price": 8.99}
            ],
            "bicycle": {"color": "red", "price": 19.95}
          }
        }
        """;

    private final JsonService service = new JsonService();

    // region validate

    @Test
    void validDocumentsPassAndReportTheirSize() {
        JsonService.Validation result = service.validate("{\"a\": [1, 2, 3]}");

        assertThat(result.valid()).isTrue();
        assertThat(result.error()).isNull();
        assertThat(result.bytes()).isEqualTo(16);
    }

    @Test
    void anInvalidDocumentSaysWhereItStoppedBeingJson() {
        JsonService.Validation result = service.validate("{\n  \"a\": 1,\n  \"b\": \n}");

        assertThat(result.valid()).isFalse();
        assertThat(result.error()).isNotBlank();
        // The line is the point of the whole endpoint.
        assertThat(result.line()).isEqualTo(4);
        assertThat(result.column()).isNotNull();
    }

    @Test
    void anEmptyDocumentIsNotValidJson() {
        assertThat(service.validate("").valid()).isFalse();
        assertThat(service.validate("   ").error()).isEqualTo("the document is empty");
        assertThat(service.validate(null).valid()).isFalse();
    }

    @Test
    void duplicateKeysAreRefusedRatherThanCollapsed() {
        // A lenient parser keeps the last one, so a "formatter" would delete
        // data while reporting success. This tool has to say so instead.
        JsonService.Validation result = service.validate("{\"a\": 1, \"a\": 2}");

        assertThat(result.valid()).isFalse();
        assertThat(result.error()).containsIgnoringCase("duplicate");

        assertThatThrownBy(() -> service.format("{\"a\": 1, \"a\": 2}", null, false))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("uplicate");
    }

    // endregion

    // region format and minify

    @Test
    void formattingUsesTwoSpacesAndTheOrdinaryColonSpacing() {
        String formatted = service.format("{\"a\":1,\"b\":[1,2]}", null, false);

        assertThat(formatted).isEqualTo("""
            {
              "a": 1,
              "b": [
                1,
                2
              ]
            }""");
        // Jackson's own default is `"a" : 1`, which no other formatter writes.
        assertThat(formatted).doesNotContain("\" :");
    }

    @Test
    void theIndentCanBeWidenedOrMadeATab() {
        assertThat(service.format("{\"a\":1}", "4", false)).isEqualTo("{\n    \"a\": 1\n}");
        assertThat(service.format("{\"a\":1}", "tab", false)).isEqualTo("{\n\t\"a\": 1\n}");

        assertThatThrownBy(() -> service.format("{\"a\":1}", "0", false))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("between 1 and 16");
        assertThatThrownBy(() -> service.format("{\"a\":1}", "wide", false))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("number of spaces or 'tab'");
    }

    @Test
    void minifyingRemovesEveryAvoidableByte() {
        String minified = service.minify("""
            {
              "a" : 1,
              "b" : [ 1, 2 ]
            }
            """, false);

        assertThat(minified).isEqualTo("{\"a\":1,\"b\":[1,2]}");
    }

    @Test
    void sortingReordersKeysAtEveryDepthButLeavesArraysAlone() {
        String sorted = service.minify("{\"b\":1,\"a\":{\"z\":1,\"y\":[3,1,2]}}", true);

        // Array order is data; key order is not.
        assertThat(sorted).isEqualTo("{\"a\":{\"y\":[3,1,2],\"z\":1},\"b\":1}");
    }

    @Test
    void numbersKeepTheirValueThroughAFormat() {
        // A double would round this to 0.12345678901234568.
        String precise = "0.12345678901234567890123456789";
        assertThat(service.minify("{\"a\":" + precise + "}", false)).contains(precise);

        // Integers stay integers, and a trailing zero is part of the text, not noise.
        assertThat(service.minify("{\"a\":1,\"b\":1.0,\"c\":-0.5}", false))
            .isEqualTo("{\"a\":1,\"b\":1.0,\"c\":-0.5}");
    }

    @Test
    void writingRefusesADocumentThatDoesNotParse() {
        assertThatThrownBy(() -> service.format("{\"a\":}", null, false))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("invalid JSON at line 1");

        assertThatThrownBy(() -> service.minify("", false))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("the document is empty");
    }

    // endregion

    // region query — pointer

    @Test
    void aPointerFindsExactlyOneValue() {
        JsonService.QueryResult result = service.query(STORE, "/store/book/1/title", null);

        assertThat(result.syntax()).isEqualTo(JsonQuerySyntax.POINTER);
        assertThat(result.matches()).hasSize(1);
        assertThat(result.matches().get(0).asText()).isEqualTo("Sword of Honour");
        assertThat(result.paths()).containsExactly("/store/book/1/title");
    }

    @Test
    void aPointerThatMatchesNothingIsAnAnswer() {
        JsonService.QueryResult result = service.query(STORE, "/store/dvd", null);

        assertThat(result.matches()).isEmpty();
        assertThat(result.paths()).isEmpty();
    }

    @Test
    void theEmptyPointerIsTheWholeDocument() {
        JsonService.QueryResult result = service.query("{\"a\":1}", "", null);

        assertThat(result.matches()).hasSize(1);
        assertThat(result.matches().get(0).toString()).isEqualTo("{\"a\":1}");
    }

    @Test
    void aPointerThatIsNotAPointerIsRejected() {
        assertThatThrownBy(() -> service.query(STORE, "store/book", "pointer"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("must be empty or start with '/'");
    }

    // endregion

    // region query — jsonpath

    @Test
    void aJsonPathFindsEveryMatchAndSaysWhereEachWas() {
        JsonService.QueryResult result = service.query(STORE, "$.store.book[*].author", null);

        assertThat(result.syntax()).isEqualTo(JsonQuerySyntax.JSONPATH);
        assertThat(result.matches()).hasSize(3);
        assertThat(result.matches().get(0).asText()).isEqualTo("Nigel Rees");
        assertThat(result.paths()).containsExactly(
            "$['store']['book'][0]['author']",
            "$['store']['book'][1]['author']",
            "$['store']['book'][2]['author']");
    }

    @Test
    void aJsonPathCanFilter() {
        JsonService.QueryResult cheap = service.query(STORE, "$..book[?(@.price < 9)].title", null);

        assertThat(cheap.matches()).hasSize(2);
        assertThat(cheap.matches().toString()).contains("Moby Dick").doesNotContain("Sword of Honour");

        JsonService.QueryResult everyPrice = service.query(STORE, "$..price", null);
        assertThat(everyPrice.matches()).hasSize(4);
    }

    @Test
    void aJsonPathThatMatchesNothingIsAnAnswer() {
        JsonService.QueryResult result = service.query(STORE, "$.store.dvd[*].title", null);

        assertThat(result.matches()).isEmpty();
        assertThat(result.paths()).isEmpty();
    }

    @Test
    void anInvalidJsonPathIsRejected() {
        assertThatThrownBy(() -> service.query(STORE, "$.store.book[?(@.price", null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not a valid JSONPath");
    }

    // endregion

    // region syntax resolution

    @Test
    void theSyntaxIsWorkedOutFromTheExpressionUnlessItIsStated() {
        assertThat(service.query(STORE, "$.store.bicycle.color", null).syntax())
            .isEqualTo(JsonQuerySyntax.JSONPATH);
        assertThat(service.query(STORE, "/store/bicycle/color", null).syntax())
            .isEqualTo(JsonQuerySyntax.POINTER);

        // Saying which one wins over the guess.
        assertThat(service.query(STORE, "/store/bicycle/color", "pointer").matches().get(0).asText())
            .isEqualTo("red");

        assertThatThrownBy(() -> service.query(STORE, "/a", "xpath"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("unsupported syntax 'xpath'");
    }

    // endregion

    // region diff

    @Test
    void diffReportsWhetherTwoDocumentsAgree() {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode from = service.parse("{\"a\":1,\"b\":2}");
        JsonNode to = service.parse("{\"a\":1,\"b\":3}");

        JsonService.DiffResult changed = service.diff(from, to);
        assertThat(changed.equal()).isFalse();
        assertThat(changed.operations()).isEqualTo(1);
        assertThat(changed.patch().get(0).get("op").asText()).isEqualTo("replace");

        JsonService.DiffResult same = service.diff(from, from.deepCopy());
        assertThat(same.equal()).isTrue();
        assertThat(same.operations()).isZero();
        assertThat(mapper.createArrayNode()).isEqualTo(same.patch());
    }

    // endregion
}
