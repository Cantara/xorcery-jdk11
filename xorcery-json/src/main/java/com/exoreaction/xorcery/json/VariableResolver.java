package com.exoreaction.xorcery.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.*;

import java.io.StringReader;
import java.util.Iterator;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolve variables in JSON values.
 * <p>
 * Includes support for hierarchical lookups, recursive lookups, conditional lookups, and default values.
 * <p>
 * Syntax for strings with variables:
 * some {{ other.variable.name }} value
 * If the JSON source has a path other.variable.name that resolves to "foo" then the resulting value
 * is: "some foo value"
 * <p>
 * Default values are expressed with |, like so:
 * some {{ other.variable.name | bar }} value
 * If the JSON source tree does not have a path other.variable.name then the resulting value is: "some bar value"
 * <p>
 * Conditionals are expressed using ?, like so:
 * {{myflag ? valuea | valueb}}
 * If the setting "myflag" resolves to true, then valuea is resolved, otherwise it continues to use valueb. Note that
 * each step in the default value resolution chain may use a conditional to decide whether it is valid
 */
public class VariableResolver
        implements BiFunction<ObjectNode, ObjectNode, ObjectNode> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Resolve variables in root object values from source object. These may or may not be the same.
     *
     * @param source the first function argument
     * @param root   the second function argument
     * @return
     */
    @Override
    public ObjectNode apply(ObjectNode source, ObjectNode root) {
        return resolveObject(source, root);
    }

    private ObjectNode resolveObject(ObjectNode source, ObjectNode node) {
        ObjectNode result = node.objectNode();
        Iterator<String> names = node.fieldNames();
        while (names.hasNext()) {
            String next = names.next();
            JsonNode value = node.get(next);
            if (value instanceof TextNode) {
                TextNode textNode = (TextNode) value;
                try {
                    JsonNode newValue = resolveValue(source, textNode);
                    result.set(next, newValue);
                } catch (Throwable e) {
                    throw new IllegalArgumentException("Could not resolve variables for " + next + ":" + textNode.textValue(), e);
                }
            } else if (value instanceof ObjectNode) {
                ObjectNode object = (ObjectNode) value;
                result.set(next, resolveObject(source, object));
            } else if (value instanceof ArrayNode) {
                ArrayNode array = (ArrayNode) value;
                result.set(next, resolveArray(source, array));
            } else {
                result.set(next, value);
            }
        }
        return result;
    }

    private ArrayNode resolveArray(ObjectNode source, ArrayNode node) {
        ArrayNode result = node.arrayNode();
        for (JsonNode value : node) {
            if (value instanceof TextNode) {
                TextNode textNode = (TextNode) value;
                try {
                    JsonNode newValue = resolveValue(source, textNode);
                    result.add(newValue);
                } catch (Throwable e) {
                    throw new IllegalArgumentException("Could not resolve variables: " + textNode.textValue(), e);
                }
            } else if (value instanceof ObjectNode) {
                ObjectNode object = (ObjectNode) value;
                result.add(resolveObject(source, object));
            } else if (value instanceof ArrayNode) {
                ArrayNode array = (ArrayNode) value;
                result.add(resolveArray(source, array));
            } else {
                result.add(value);
            }
        }
        return result;
    }

    private JsonNode resolveValue(ObjectNode source, TextNode value) {
        String textValue = value.textValue();
        String newValue = "";
        Matcher matcher = Pattern.compile("\\{\\{([^}]+)}}").matcher(textValue);
        int idx = 0;
        while (matcher.find()) {
            String expression = matcher.group(1);
            JsonNode replacement = replaceExpression(source, expression);

            if (matcher.start() == 0 && matcher.end() == textValue.length()) {
                // Complete replacement found
                return replacement;
            } else {
                newValue += textValue.substring(idx, matcher.start()) + replacement.asText();
                idx = matcher.end();
            }
        }
        newValue += textValue.substring(idx);
        return source.textNode(newValue);
    }

    private JsonNode replaceExpression(ObjectNode source, String expression) {
        String[] expressions = expression.split("\\|");

        for (String expr : expressions) {
            if (expr.contains("?")) {
                String[] withConditional = expr.split("\\?");
                JsonNode resolvedConditional = lookupAndResolve(source, withConditional[0].trim())
                        .orElse(MissingNode.getInstance());
                if ((resolvedConditional instanceof BooleanNode) && resolvedConditional.booleanValue()) {
                    expr = withConditional[1];
                } else {
                    continue;
                }
            }

            expr = expr.trim();

            try {
                JsonNode value = objectMapper.readTree(new StringReader(expr));
                if (value instanceof TextNode) {
                    TextNode textNode = (TextNode) value;
                    JsonNode resolvedValue = lookupAndResolve(source, textNode.textValue()).orElse(textNode);
                    if (!resolvedValue.isMissingNode())
                        return resolvedValue;
                    else
                        return textNode;
                } else {
                    return value;
                }
            } catch (Throwable e) {
                JsonNode resolvedValue = lookupAndResolve(source, expr).orElse(null);
                if (resolvedValue != null && !resolvedValue.isMissingNode())
                    return resolvedValue;
            }
        }

        return MissingNode.getInstance();
    }

    private Optional<JsonNode> lookupAndResolve(ObjectNode source, String name) {
        return lookup(source, name)
                .map(result ->
                {
                    if (result instanceof TextNode) {
                        TextNode textResult = (TextNode) result;
                        return resolveValue(source, textResult);
                    } else if (result instanceof ObjectNode) {
                        ObjectNode objectResult = (ObjectNode) result;
                        return resolveObject(source, objectResult);
                    } else if (result instanceof ArrayNode) {
                        ArrayNode arrayResult = (ArrayNode) result;
                        return resolveArray(source, arrayResult);
                    } else {
                        return result;
                    }
                });
    }

    private Optional<JsonNode> lookup(ObjectNode source, String name) {
        ObjectNode context = source;
        String[] names = name.split("\\.");
        for (int i = 0; i < names.length - 1; i++) {
            JsonNode node = context.get(names[i]);

            // Might be an intermediary lookup
            if (node instanceof TextNode)
            {
                TextNode textNode = (TextNode) node;
                node = resolveValue(source, textNode);
            }

            if (node instanceof ObjectNode)
                context = (ObjectNode) node;
            else
                return Optional.empty();
        }
        return Optional.ofNullable(context.get(names[names.length - 1]));
    }
}
