package org.example;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;

/**
 * Convenience class so the code can choose whether to connect to the local
 * or the hapi fhir server
 */
public     class MyContext {
    private String url;
    private final FhirContext ctx;
    private final IGenericClient client;
    public MyContext(String url, FhirContext ctx, IGenericClient client) {
        this.url = url;
        this.ctx = ctx;
        this.client = client;
    }
    public String getUrl() {
        return url;
    }
    public void setUrl(String url) {
        this.url = url;
    }
    public FhirContext getCtx() {
        return ctx;
    }
    public IGenericClient getClient() {
        return client;
    }
}