package org.example;

import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.rest.gclient.StringClientParam;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import org.hl7.fhir.r4.model.*;

import java.util.ArrayList;
import java.util.List;

public class Module3Exercises {


    /**
     * Searches for patients by some criteria
     * @param myContext server context
     */
    public static void findPatients(MyContext myContext) {
        // Find female patients born before 1960 whose family name starts with "Sm".
        Bundle responseBundle = myContext.getClient().search()
                .forResource(Patient.class)
                .where(Patient.GENDER.exactly().code("female"))
                .where(Patient.BIRTHDATE.before().day("1960-01-01"))
                .where(Patient.FAMILY.matches().value("Sm"))   // actually does a startsWith() caseInsensitive
                .returnBundle(Bundle.class)
                .execute();

        // Extract a list of matching Patient resources
        List<Patient> patients = responseBundle.getEntry().stream()
                .map(entry -> (Patient) entry.getResource())
                .toList();

        System.out.println("\n\nFound " + patients.size() + " patients matching criteria.");

    }

    /**
     * Finds observations by some criteria
     * @param myContext server context
     */
    static void findObservations(MyContext myContext) {
        // Find all heart-rate Observations over 100/min,
        // newest first, 10 per page, and walk three pages by following next links.
        // /Observation?code=http://loinc.org%7c8867-4&value-quantity=gt100&_count=10&_sort=-date
        Bundle responseBundle = myContext.getClient().search()
                .forResource(Observation.class)
                .where(Observation.CODE.exactly().systemAndCode("http://loinc.org", "8867-4"))
                .where(Observation.VALUE_QUANTITY.greaterThan().number(100).andNoUnits())
                .sort().descending(Observation.DATE)
                .count(10)
                .returnBundle(Bundle.class)
                .execute();


        int page = 0;
        while (page < 3) {
            List<Observation> heartRateObservations = responseBundle.getEntry().stream()
                    .map(entry -> (Observation) entry.getResource())
                    .toList();

            for (Observation obs : heartRateObservations) {
                System.out.println("Observation ID: " + obs.getIdPart());
                System.out.println("Status: " + obs.getStatus());

                // Check if the observation contains a numerical Quantity value
                if (obs.hasValueQuantity()) {
                    Quantity value = obs.getValueQuantity();
                    System.out.println("Heart Rate: " + value.getValue() + " " + value.getUnit()); // e.g., 72 /min
                }
            }

            if (responseBundle.getLink("next") != null) {
                page += 1;
                if (page < 3) {
                    System.out.println("GETTING NEXT PAGE: " + page);
                    responseBundle = myContext.getClient().loadPage()
                            .next(responseBundle)
                            .execute();
                }
            } else {
                break;
            }
        }
    }

    /**
     * FInds a patient with all associated conditions and encounters
     * @param myContext server context
     */
    static void findPatCondEnc(MyContext myContext) {
        // Fetch one patient with all of their Conditions and Encounters in a single request.
        // Find all heart-rate Observations over 100/min,
        // newest first, 10 per page, and walk three pages by following next links.
        // /Observation?code=http://loinc.org%7c8867-4&value-quantity=gt100&_count=10&_sort=-date


        Bundle responseBundle = myContext.getClient().search()
                .forResource(Patient.class)

                // 1. Convert the reverse chaining (_has) filters using custom parameters
                .where(new StringClientParam("_has:Condition:patient:clinical-status").matches().value("active"))
                .where(new StringClientParam("_has:Encounter:patient:status").matches().value("finished"))

                // 2. Map the reverse includes (_revinclude)
                // Note: HAPI handles _revinclude automatically when using the Include object inside .revInclude()
                .revInclude(new Include("Condition:patient"))
                .revInclude(new Include("Encounter:patient"))

                // 3. Map sorting criteria (_sort=_id)
                .sort().ascending(new StringClientParam("_id")) // Or use .descending() if needed

                // 4. Map the response page size (_count=1)
                // .count(1)

                // 5. Structure execution format
                .returnBundle(Bundle.class)
                .execute();

// Extract from bundles as normal
        System.out.println("Direct Encounters found: " + responseBundle.getEntry().size());
        System.out.println("Direct Conditions found: " + responseBundle.getEntry().size());
        // report the results
        Patient patient = null;
        List<Encounter> encounters = new ArrayList<>();
        List<Condition> conditions = new ArrayList<>();

        for (Bundle.BundleEntryComponent entry : responseBundle.getEntry()) {
            if (entry.getResource() instanceof Patient) {
                patient = (Patient) entry.getResource();
            } else if (entry.getResource() instanceof Encounter) {
                encounters.add((Encounter) entry.getResource());
            } else if (entry.getResource() instanceof Condition) {
                conditions.add((Condition) entry.getResource());
            }
        }

        System.out.println("\n\nTarget Patient: " + (patient != null ? patient.getNameFirstRep().getNameAsSingleString() : "Not Found"));
        System.out.println("Total Encounters loaded: " + encounters.size());
        System.out.println("Total Conditions loaded: " + conditions.size());
    }

    /**
     * Finds patients with at least one med request
     * @param myContext server context
     */
    static void patMedRequest(MyContext myContext) {
        // Find Patients who have at least one MedicationRequest, using has.

        Bundle responseBundle = myContext.getClient().search()
                .forResource(Patient.class)

                // 1. Convert the reverse chaining (_has) filters using custom parameters
                .where(new StringClientParam("_has:MedicationRequest:patient:status").matches().value("active"))

                // 2. Map the reverse includes (_revinclude)
                // Note: HAPI handles _revinclude automatically when using the Include object inside .revInclude()
                .revInclude(new Include("MedicationRequest:patient"))

                // 3. Map sorting criteria (_sort=_id)
                .sort().ascending(new StringClientParam("_id")) // Or use .descending() if needed

                // 4. Map the response page size (_count=1)
                // .count(1)

                // 5. Structure execution format
                .returnBundle(Bundle.class)
                .execute();

// Extract from bundles as normal
        System.out.println("Direct MedicationRequests found: " + responseBundle.getEntry().size());
        // report the results
        Patient patient = null;
        List<MedicationRequest> medicationRequests = new ArrayList<>();

        for (Bundle.BundleEntryComponent entry : responseBundle.getEntry()) {
            if (entry.getResource() instanceof Patient) {
                patient = (Patient) entry.getResource();
            } else if (entry.getResource() instanceof MedicationRequest) {
                medicationRequests.add((MedicationRequest) entry.getResource());
            }
        }

        System.out.println("\n\nTarget Patient: " + (patient != null ? patient.getNameFirstRep().getNameAsSingleString() : "Not Found"));
        System.out.println("Total MedicationRequests loaded: " + medicationRequests.size());
    }

    /**
     * Attempt to find a patient with an unsupported search param
     * @param myContext server context
     */
    static void unsupportedSearch(MyContext myContext) {
        // Find Patients but use an unsupported search param

        try {
            Bundle responseBundle = myContext.getClient().search()
                    .forResource(Patient.class)

                    // 1. Convert the reverse chaining (_has) filters using custom parameters
                    .where(new StringClientParam("_has:MedicationRequest:patient:status:abc").matches().value("active"))
                    // Note: HAPI handles _revinclude automatically when using the Include object inside .revInclude()
                    .revInclude(new Include("MedicationRequest:patient"))
                    .sort().ascending(new StringClientParam("_id")) // Or use .descending() if needed
                    .returnBundle(Bundle.class)
                    .execute();
            throw new RuntimeException("Search should have failed");
        } catch (InvalidRequestException e) {
            if (e.getMessage().contains("400 Bad Request")) {
                System.out.println("\n\n400 Bad Request: " + e.getMessage());
            } else {
                throw new RuntimeException("Unexpected error: " + e.getMessage());
            }
        }

    }
}
