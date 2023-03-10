package com.exoreaction.xorcery.service.dns;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.service.dns.client.DnsLookupService;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.xbill.DNS.TextParseException;

import java.net.URI;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Disabled
public class DnsLookupTest {

    private DnsLookupService dnsLookupService;

    @BeforeEach
    public void setup() {
        dnsLookupService = new DnsLookupService(new Configuration.Builder()
                .add("dns.client.nameservers", JsonNodeFactory.instance.arrayNode().add("localhost:8853"))
                .add("dns.client.search", List.of("xorcery.test.","_sub._https._tcp.xorcery.test.","_sub._http._tcp.xorcery.test."))
                .add("dns.client.hosts", JsonNodeFactory.instance.objectNode().put("_certificates", "https://127.0.0.1:8080/api/path"))
                .build(), () -> null);
    }

    @Test
    public void testDnsAFull() throws UnknownHostException, TextParseException, ExecutionException, InterruptedException {

        List<URI> hosts = dnsLookupService.resolve(URI.create("http://xorcery1.xorcery.test:80")).get();
        System.out.println(hosts);
    }

    @Test
    public void testDnsANoDomain() throws UnknownHostException, TextParseException, ExecutionException, InterruptedException {

        List<URI> hosts = dnsLookupService.resolve(URI.create("http://xorcery1:80")).get();
        System.out.println(hosts);
    }

    @Test
    public void testDnsSRV() throws UnknownHostException, TextParseException, ExecutionException, InterruptedException {
        List<URI> hosts = dnsLookupService.resolve(URI.create("srv://_analytics._sub._http._tcp.xorcery.test")).get();
        System.out.println(hosts);
    }

    @Test
    public void testDnsSRVHosts() throws UnknownHostException, TextParseException, ExecutionException, InterruptedException {
        List<URI> hosts = dnsLookupService.resolve(URI.create("srv://_certificates")).get();
        System.out.println(hosts);
    }

    @Test
    public void testDnsNoDomainSRV() throws UnknownHostException, TextParseException, ExecutionException, InterruptedException {
        List<URI> hosts = dnsLookupService.resolve(URI.create("srv://_analytics")).get();
        System.out.println(hosts);
    }

    @Test
    public void testDnsAList() throws UnknownHostException, TextParseException, ExecutionException, InterruptedException {

        List<URI> hosts = dnsLookupService.resolve(URI.create("http://analytics.xorcery.test")).get();
        System.out.println(hosts);
    }

    @Test
    public void testDnsANoDomainList() throws UnknownHostException, TextParseException, ExecutionException, InterruptedException {

        List<URI> hosts = dnsLookupService.resolve(URI.create("http://analytics")).get();
        System.out.println(hosts);
    }
}
