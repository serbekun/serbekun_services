package com.serbekun.ss.service.json;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TreeMap;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.InvalidPathException;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.PathNotFoundException;
import com.jayway.jsonpath.spi.json.JacksonJsonNodeJsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;

/**
 * Working with JSON documents: validate, format, minify, query and diff.
 * Stateless — a document arrives, an answer goes back, nothing is kept.
 * <p>
 * Two parsing decisions run through all five operations:
 * <ul>
 *   <li><b>Duplicate keys are an error.</b> RFC 8259 only says names
 *   <i>should</i> be unique, and a lenient parser quietly keeps the last one —
 *   which would make this formatter delete data while claiming to have
 *   reformatted it. A tool whose whole job is to tell you about your document
 *   has to say so instead.</li>
 *   <li><b>Numbers keep their value exactly.</b> Floating point text is parsed
 *   as {@code BigDecimal}, so a long decimal survives a round trip that
 *   {@code double} would round off. The text may still be normalised —
 *   {@code 1e2} comes back as {@code 1E+2} — but the value never changes.</li>
 * </ul>
 */
public class JsonService {

    /** Widest indent accepted, in spaces. */
    public static final int MAX_INDENT = 16;

    /** Indent used when the caller does not ask for one. */
    public static final int DEFAULT_INDENT = 2;

    private final ObjectMapper mapper;

    /** JSONPath evaluation that returns the matched values. */
    private final Configuration valueConfiguration;

    /** The same evaluation, returning where each match was found instead. */
    private final Configuration pathConfiguration;

    public JsonService() {
        this.mapper = configuredMapper();
        this.valueConfiguration = jsonPath(Option.ALWAYS_RETURN_LIST);
        this.pathConfiguration = jsonPath(Option.AS_PATH_LIST);
    }

    private static ObjectMapper configuredMapper() {
        ObjectMapper mapper = new ObjectMapper()
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION.mappedFeature());

        // The default node factory strips trailing zeros, which would quietly
        // rewrite 1.0 as 1 — a change to the document this was asked only to
        // reformat.
        mapper.setNodeFactory(JsonNodeFactory.withExactBigDecimals(true));
        return mapper;
    }

    private Configuration jsonPath(Option option) {
        return Configuration.builder()
            .jsonProvider(new JacksonJsonNodeJsonProvider(mapper))
            .mappingProvider(new JacksonMappingProvider(mapper))
            .options(option)
            .build();
    }

    // region validate

    /**
     * The outcome of checking a document.
     *
     * @param valid whether it parsed
     * @param error what was wrong, or null when nothing was
     * @param line the 1-based line the problem is on, or null
     * @param column the 1-based column, or null
     * @param bytes the size of the document as UTF-8
     */
    public record Validation(boolean valid, String error, Integer line, Integer column, long bytes) {
    }

    /**
     * Checks whether a document is JSON, and says where it stops being JSON if
     * it is not. A document that does not parse is an answer, not a failure —
     * telling a caller where the comma is missing is the entire job.
     *
     * @param document the text to check
     * @return what was found
     */
    public Validation validate(String document) {
        long bytes = document == null ? 0 : document.getBytes(StandardCharsets.UTF_8).length;

        if (document == null || document.isBlank()) {
            return new Validation(false, "the document is empty", null, null, bytes);
        }

        try {
            mapper.readTree(document);
            return new Validation(true, null, null, null, bytes);
        } catch (JsonProcessingException e) {
            JsonLocation location = e.getLocation();
            return new Validation(
                false,
                firstLine(e.getOriginalMessage()),
                location == null ? null : location.getLineNr(),
                location == null ? null : location.getColumnNr(),
                bytes);
        }
    }

    // endregion

    // region format and minify

    /**
     * Re-writes a document with indentation.
     *
     * @param document the text to format
     * @param indent the number of spaces, or {@code tab}; blank means
     *               {@value #DEFAULT_INDENT} spaces
     * @param sortKeys true to put every object's keys in order, which is what
     *                 makes two documents comparable line by line
     * @return the formatted document
     * @throws IllegalArgumentException when the document does not parse, or the
     *         indent is not a width this can write
     */
    public String format(String document, String indent, boolean sortKeys) {
        JsonNode root = parse(document);
        if (sortKeys) {
            root = sorted(root);
        }

        DefaultIndenter indenter = new DefaultIndenter(indentString(indent), "\n");
        DefaultPrettyPrinter printer = new StandardPrettyPrinter();
        printer.indentObjectsWith(indenter);
        printer.indentArraysWith(indenter);

        try {
            return mapper.writer(printer).writeValueAsString(root);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to write the formatted document", e);
        }
    }

    /**
     * Re-writes a document with every avoidable byte removed.
     *
     * @param document the text to minify
     * @param sortKeys true to put every object's keys in order
     * @return the minified document
     * @throws IllegalArgumentException when the document does not parse
     */
    public String minify(String document, boolean sortKeys) {
        JsonNode root = parse(document);
        if (sortKeys) {
            root = sorted(root);
        }

        try {
            return mapper.writeValueAsString(root);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to write the minified document", e);
        }
    }

    /** Resolves the indent width, accepting a tab as well as a number of spaces. */
    private static String indentString(String indent) {
        if (indent == null || indent.isBlank()) {
            return " ".repeat(DEFAULT_INDENT);
        }

        String normalized = indent.strip().toLowerCase(Locale.ROOT);
        if (normalized.equals("tab") || normalized.equals("\t")) {
            return "\t";
        }

        int width;
        try {
            width = Integer.parseInt(normalized);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("indent must be a number of spaces or 'tab'");
        }
        if (width < 1 || width > MAX_INDENT) {
            throw new IllegalArgumentException(
                "indent must be between 1 and " + MAX_INDENT + " spaces, or 'tab'");
        }
        return " ".repeat(width);
    }

    /** Rebuilds every object in the tree with its keys in order. */
    private static JsonNode sorted(JsonNode node) {
        if (node.isObject()) {
            TreeMap<String, JsonNode> fields = new TreeMap<>();
            node.fields().forEachRemaining(field -> fields.put(field.getKey(), sorted(field.getValue())));

            ObjectNode sorted = JsonNodeFactory.instance.objectNode();
            fields.forEach(sorted::set);
            return sorted;
        }

        if (node.isArray()) {
            // Array order is data, not presentation: only the objects inside are sorted.
            ArrayNode sorted = JsonNodeFactory.instance.arrayNode(node.size());
            node.forEach(element -> sorted.add(sorted(element)));
            return sorted;
        }

        return node;
    }

    /**
     * Jackson writes {@code "key" : value} by default; every other formatter in
     * the world writes {@code "key": value}, and a formatter that produces an
     * unfamiliar house style is one nobody uses.
     */
    private static final class StandardPrettyPrinter extends DefaultPrettyPrinter {

        private StandardPrettyPrinter() {
        }

        private StandardPrettyPrinter(StandardPrettyPrinter base) {
            super(base);
        }

        @Override
        public DefaultPrettyPrinter createInstance() {
            return new StandardPrettyPrinter(this);
        }

        @Override
        public void writeObjectFieldValueSeparator(JsonGenerator generator) throws IOException {
            generator.writeRaw(": ");
        }
    }

    // endregion

    // region query

    /**
     * What a query found.
     *
     * @param expression the expression as written by the caller
     * @param syntax which language it was read as
     * @param matches the values found, in document order
     * @param paths where each match was found, in the same syntax as the query
     */
    public record QueryResult(String expression, JsonQuerySyntax syntax, ArrayNode matches, List<String> paths) {
    }

    /**
     * Pulls values out of a document with a JSON Pointer or a JSONPath.
     * <p>
     * Finding nothing is a normal answer — an empty list, not an error — because
     * "is there anything at this path" is a question worth asking.
     *
     * @param document the document to search
     * @param expression the pointer or path
     * @param syntax which language the expression is in; blank to work it out
     * @return the matches and where they were found
     * @throws IllegalArgumentException when the document does not parse, or the
     *         expression is not valid in its syntax
     */
    public QueryResult query(String document, String expression, String syntax) {
        if (expression == null) {
            throw new IllegalArgumentException("an expression is required");
        }

        JsonNode root = parse(document);
        JsonQuerySyntax resolved = JsonQuerySyntax.resolve(syntax, expression);

        return resolved == JsonQuerySyntax.POINTER
            ? byPointer(root, expression.strip())
            : byJsonPath(root, expression.strip());
    }

    private static QueryResult byPointer(JsonNode root, String expression) {
        JsonPointer pointer;
        try {
            pointer = JsonPointer.compile(expression);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                "'" + expression + "' is not a valid JSON Pointer — it must be empty or start with '/'");
        }

        ArrayNode matches = JsonNodeFactory.instance.arrayNode();
        List<String> paths = new ArrayList<>();

        JsonNode found = root.at(pointer);
        if (!found.isMissingNode()) {
            matches.add(found);
            paths.add(expression);
        }

        return new QueryResult(expression, JsonQuerySyntax.POINTER, matches, paths);
    }

    private QueryResult byJsonPath(JsonNode root, String expression) {
        JsonPath path;
        try {
            path = JsonPath.compile(expression);
        } catch (InvalidPathException e) {
            throw new IllegalArgumentException("'" + expression + "' is not a valid JSONPath");
        }

        ArrayNode matches;
        try {
            matches = JsonPath.using(valueConfiguration).parse(root).read(path);
        } catch (PathNotFoundException e) {
            matches = JsonNodeFactory.instance.arrayNode();
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("the JSONPath could not be evaluated: " + e.getMessage());
        }

        List<String> paths = new ArrayList<>();
        try {
            JsonNode found = JsonPath.using(pathConfiguration).parse(root).read(path);
            found.forEach(node -> paths.add(node.asText()));
        } catch (PathNotFoundException e) {
            // No matches, so no paths either — already the empty list.
        }

        return new QueryResult(expression, JsonQuerySyntax.JSONPATH, matches, paths);
    }

    // endregion

    // region diff

    /**
     * The difference between two documents.
     *
     * @param patch the RFC 6902 operations that turn the first into the second
     * @param operations how many operations that took
     * @param equal whether the two documents hold the same value
     */
    public record DiffResult(ArrayNode patch, int operations, boolean equal) {
    }

    /**
     * Works out what changed between two documents, as an RFC 6902 JSON Patch.
     *
     * @param from the document as it is
     * @param to the document as it should be
     * @return the patch, which applied to {@code from} yields {@code to}
     */
    public DiffResult diff(JsonNode from, JsonNode to) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("both 'from' and 'to' are required");
        }

        ArrayNode patch = JsonDiff.diff(from, to);
        return new DiffResult(patch, patch.size(), patch.isEmpty());
    }

    /**
     * Reads a document, or says where it stopped being JSON.
     *
     * @param document the text to read
     * @return the parsed tree
     * @throws IllegalArgumentException when it does not parse
     */
    public JsonNode parse(String document) {
        if (document == null || document.isBlank()) {
            throw new IllegalArgumentException("the document is empty");
        }

        try {
            return mapper.readTree(document);
        } catch (JsonProcessingException e) {
            JsonLocation location = e.getLocation();
            String where = location == null
                ? ""
                : " at line " + location.getLineNr() + ", column " + location.getColumnNr();
            throw new IllegalArgumentException("invalid JSON" + where + ": " + firstLine(e.getOriginalMessage()));
        }
    }

    // endregion

    /** Jackson appends the source and location to its messages; the first line is the reason. */
    private static String firstLine(String message) {
        if (message == null) {
            return "the document could not be parsed";
        }
        int newline = message.indexOf('\n');
        return (newline < 0 ? message : message.substring(0, newline)).strip();
    }
}
