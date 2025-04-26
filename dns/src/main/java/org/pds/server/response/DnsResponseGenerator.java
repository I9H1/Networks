package org.pds.server.response;

import org.pds.server.request.DiscoverDnsRequest;
import org.pds.server.request.DnsRequest;
import org.pds.server.request.QueryDnsRequest;
import org.pds.server.request.RegisterDnsRequest;
import org.pds.util.DnsException;

import java.net.InetAddress;
import java.util.HashMap;

public class DnsResponseGenerator {

    private final InetAddress dnsServerAddress;
    private final HashMap<String, String> domainTable;

    public DnsResponseGenerator(
            InetAddress dnsServerAddress,
            HashMap<String, String> domainTable
    ) {
        this.dnsServerAddress = dnsServerAddress;
        this.domainTable = domainTable;
    }

    public DnsResponse getDnsResponse(DnsRequest request) {
        if (request instanceof DiscoverDnsRequest) {
            return new DiscoverDnsResponse(dnsServerAddress);
        } else if (request instanceof RegisterDnsRequest registerRequest) {
            domainTable.put(registerRequest.domainName(), registerRequest.address());
            return new RegisterDnsResponse(registerRequest.domainName(), registerRequest.address());
        } else if (request instanceof QueryDnsRequest queryRequest) {
            String address = domainTable.get(queryRequest.domainName());
            if (address == null) {
                throw new DnsException("Domain not found: " + queryRequest.domainName());
            }
            return new QueryDnsResponse(queryRequest.domainName(), address);
        }
        throw new DnsException("Unsupported request type");
    }
}
