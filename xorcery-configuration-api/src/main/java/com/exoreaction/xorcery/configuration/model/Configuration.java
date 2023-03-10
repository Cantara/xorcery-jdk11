package com.exoreaction.xorcery.configuration.model;

import com.exoreaction.xorcery.builders.With;
import com.exoreaction.xorcery.json.VariableResolver;
import com.exoreaction.xorcery.json.model.JsonElement;
import com.exoreaction.xorcery.util.Resources;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * @author rickardoberg
 * @since 20/04/2022
 */
public final class Configuration
        implements JsonElement {

    private static final ObjectMapper mapper = new ObjectMapper();
    private final ObjectNode json;

    /**
     *
     */
    public Configuration(ObjectNode json) {
        this.json = json;
    }

    public static Configuration empty() {
        return new Configuration(JsonNodeFactory.instance.objectNode());
    }

    public static final class Builder
            implements With<Builder> {
        private final ObjectNode builder;

        public Builder(ObjectNode builder) {
            this.builder = builder;
        }

        public Builder() {
            this(JsonNodeFactory.instance.objectNode());
        }

        ObjectNode navigateToParentOfPropertyNameThenAdd(ObjectNode node, String name, JsonNode value) {
            int i = name.indexOf(".");
            if (i == -1) {
                // name is the last element in path to navigate, navigation complete.
                node.set(name, value);
                return node;
            }
            String childElement = name.substring(0, i);
            String remainingName = name.substring(i + 1);
            JsonNode child = node.get(childElement);
            ObjectNode childObjectNode;
            if (child == null || child.isNull()) {
                // non-existent json-node, need to create child object node
                childObjectNode = node.putObject(childElement);
            } else {
                // existing json-node, ensure that it can be navigated through
                if (!child.isObject()) {
                    throw new RuntimeException("Attempted to navigate through json key that already exists, but is not a json-object that support navigation.");
                }
                childObjectNode = (ObjectNode) child;
            }
            // TODO support arrays, for now we only support object navigation
            return navigateToParentOfPropertyNameThenAdd(childObjectNode, remainingName, value);
        }

        public Builder add(String name, JsonNode value) {
            navigateToParentOfPropertyNameThenAdd(builder, name, value);
            return this;
        }

        public Builder add(String name, String value) {
            return add(name, builder.textNode(value));
        }

        public Builder add(String name, long value) {
            return add(name, builder.numberNode(value));
        }

        public Builder add(String name, Object value) {
            return add(name, mapper.valueToTree(value));
        }

        public Builder addSystemProperties(String nodeName) {
            ObjectNode system = builder.objectNode();
            for (Map.Entry<Object, Object> systemProperty : System.getProperties().entrySet()) {
                system.set(systemProperty.getKey().toString().replace('.', '_'), builder.textNode(systemProperty.getValue().toString()));
            }
            builder.set(nodeName, system);
            return this;
        }

        public Builder addEnvironmentVariables(String nodeName) {
            ObjectNode env = builder.objectNode();
            System.getenv().forEach((key, value) -> env.set(key.replace('.', '_'), env.textNode(value)));
            builder.set(nodeName, env);
            return this;
        }

        public Configuration build() {
            // Resolve any references
            return new Configuration(new VariableResolver().apply(builder, builder));
        }

        public ObjectNode builder() {
            return builder;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            var that = (Builder) obj;
            return Objects.equals(this.builder, that.builder);
        }

        @Override
        public int hashCode() {
            return Objects.hash(builder);
        }

        @Override
        public String toString() {
            return "Builder[" +
                   "builder=" + builder + ']';
        }

    }

    public Configuration getConfiguration(String name) {
        return getJson(name)
                .map(ObjectNode.class::cast).map(Configuration::new)
                .orElseGet(() -> new Configuration(JsonNodeFactory.instance.objectNode()));
    }

    public List<Configuration> getConfigurations(String name) {
        return getJson(name)
                .map(ArrayNode.class::cast)
                .map(a -> JsonElement.getValuesAs(a, Configuration::new))
                .orElseGet(Collections::emptyList);
    }

    /**
     * Retrieve a configuration value that represents a resource path URL.
     * It will try to resolve the name using ClassLoader.getSystemResource(path), to find the actual
     * path of the resource, given the current classpath and modules.
     *
     * @param name
     * @return
     */
    public Optional<URL> getResourceURL(String name) {
        return getString(name).flatMap(Resources::getResource);
    }

    public Builder asBuilder() {
        return new Builder(json);
    }

    @Override
    public ObjectNode json() {
        return json;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (Configuration) obj;
        return Objects.equals(this.json, that.json);
    }

    @Override
    public int hashCode() {
        return Objects.hash(json);
    }

    @Override
    public String toString() {
        return "Configuration[" +
               "json=" + json + ']';
    }

}

