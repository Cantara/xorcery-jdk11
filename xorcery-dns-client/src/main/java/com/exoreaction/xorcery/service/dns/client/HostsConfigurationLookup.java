package com.exoreaction.xorcery.service.dns.client;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.service.dns.client.api.DnsLookup;
import com.exoreaction.xorcery.util.Sockets;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class HostsConfigurationLookup
        implements DnsLookup {
    private final ObjectNode hosts;

    public HostsConfigurationLookup(Configuration configuration) {
        hosts = (ObjectNode) configuration.getJson("dns.client.hosts").orElseGet(JsonNodeFactory.instance::objectNode);
    }

    @Override
    public CompletableFuture<List<URI>> resolve(URI uri) {

        String host = uri.getHost();
        if (host == null) {
            host = uri.getAuthority();
        }
        if (host != null) {
            try {
                JsonNode lookup = hosts.get(host);
                if (lookup instanceof TextNode) {
                    URI newUri = toURI(lookup.textValue(), uri);
                    return CompletableFuture.completedFuture(List.of(newUri));
                } else if (lookup instanceof ArrayNode) {
                    ArrayNode an = (ArrayNode) lookup;
                    List<URI> addresses = new ArrayList<>();
                    for (JsonNode jsonNode : an) {
                        URI newUri = toURI(jsonNode.textValue(), uri);
                        addresses.add(newUri);
                    }
                    return CompletableFuture.completedFuture(addresses);
                }
            } catch (URISyntaxException e) {
                return CompletableFuture.failedFuture(e);
            }
        }
        return CompletableFuture.completedFuture(Collections.emptyList());
    }

    private URI toURI(String jsonText, URI uri) throws URISyntaxException {
        if (jsonText.contains("://"))
        {
            return URI.create(jsonText);
        } else
        {
            InetSocketAddress inetSocketAddress = Sockets.getInetSocketAddress(jsonText, uri.getPort());
            return new URI(uri.getScheme(), uri.getUserInfo(), inetSocketAddress.getAddress().getHostAddress(), inetSocketAddress.getPort(), uri.getPath(), uri.getQuery(), uri.getFragment());
        }
    }
}
