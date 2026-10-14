package org.example;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import ca.uhn.fhir.rest.client.interceptor.LoggingInterceptor;
import org.hl7.fhir.r4.model.Resource;

public class Util {
    /**
     * Sets up the local and hapi fhir clients
     * @param url url for fhir server
     * @return MyContext instance for the selected url
     */
    static MyContext setupFhirClient(String url) {

        FhirContext ctx = FhirContext.forR4();
        ctx.getRestfulClientFactory().setSocketTimeout(30_000);
        // By default the client GETs /metadata before its first call; skip that for known servers
        ctx.getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);

        // client = ctx.newRestfulGenericClient("http://localhost:8080/fhir");
        IGenericClient client = ctx.newRestfulGenericClient(url);
        client.registerInterceptor(new LoggingInterceptor(true));     // logs requests/responses
        // client.registerInterceptor(new BearerTokenAuthInterceptor(token));  // Module 7
        // In Spring Boot, make FhirContext and IGenericClient @Beans.
        return new MyContext(url, ctx, client);
    }

    /**
     * Converts the MethodOutcome response to a CRUD operation to a descriptive string for logging
     * @param ctx the FHIR server context
     * @param outcome the MethodOutcome to be converted
     * @return a string that contains the outcome content
     */
    static String methodOutcomeToString(FhirContext ctx, MethodOutcome outcome) {
        StringBuilder sb = new StringBuilder();
        sb.append("--- MethodOutcome Details ---\n");

        // 1. Get the HTTP status code
        sb.append("HTTP Status: ").append(outcome.getResponseStatusCode()).append("\n");

        // 2. Get the unique Resource ID (includes resource type, ID, and version)
        if (outcome.getId() != null) {
            sb.append("Resource ID: ").append(outcome.getId().getValue()).append("\n");
        } else {
            sb.append("Resource ID: null\n");
        }

        // 3. Serialize the actual FHIR resource payload if present
        if (outcome.getResource() != null) {
            sb.append("Resource Payload (JSON):\n");
            String jsonPayload = ctx.newJsonParser()
                    .setPrettyPrint(true)
                    .encodeResourceToString((Resource) outcome.getResource());
            sb.append(jsonPayload).append("\n");
        } else if (outcome.getOperationOutcome() != null) {
            // Some servers return an OperationOutcome with warning/error messages instead of the resource
            sb.append("OperationOutcome Payload (JSON):\n");
            String jsonPayload = ctx.newJsonParser()
                    .setPrettyPrint(true)
                    .encodeResourceToString(outcome.getOperationOutcome());
            sb.append(jsonPayload).append("\n");
        } else {
            sb.append("Resource Payload: [Empty/Null Response Body]\n");
        }

        sb.append("----------------------------");
        return sb.toString();
    }

    /**
     * Convenience method to return a monotonic increasing numeric string for the patient identifier
     * (this is not the patient id field)
     */
    private static String id = "12340";
    static String getNextPatientId() {
        int i = Integer.parseInt(id) + 1;
        id = Integer.toString(i);
        return id;
    }

}
