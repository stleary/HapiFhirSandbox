package org.example;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.PreferReturnEnum;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.*;

public class Module4Exercises {

    /**
     * Executes a transaction to create a patient with an observation
     * @param myContext server context
     */
    static void doTransaction(MyContext myContext) {
        Patient p = new Patient();
        p.addIdentifier().setSystem("urn:example:mrn").setValue(Util.getNextPatientId());
        p.addName().setFamily("Testperson").addGiven("Ada");

        Bundle tx = new Bundle().setType(Bundle.BundleType.TRANSACTION);
        String patientUrn = IdType.newRandomUuid().getValue();   // "urn:uuid:..."
        tx.addEntry().setFullUrl(patientUrn).setResource(p)
                .getRequest().setMethod(Bundle.HTTPVerb.POST).setUrl("Patient");

        Observation hr = new Observation();
        hr.setStatus(Observation.ObservationStatus.FINAL);
        hr.getCode().addCoding().setSystem("http://loinc.org").setCode("8867-4");
        hr.setSubject(new Reference(patientUrn));
        hr.setValue(new Quantity().setValue(72)
                .setSystem("http://unitsofmeasure.org").setCode("/min"));
        tx.addEntry().setResource(hr)
                .getRequest().setMethod(Bundle.HTTPVerb.POST).setUrl("Observation");

        Bundle resp = myContext.getClient().transaction().withBundle(tx).execute();
        resp.getEntry().forEach(e -> System.out.println(e.getResponse().getLocation()));

    }


    /**
     * Executes a bundle read by some criteria
     * @param myContext server context
     */
    // doesn't find anything yet in local host
    static void doBundleRead(MyContext myContext) {
        Bundle page = myContext.getClient().search()
                .forResource(Observation.class)
                .where(Observation.CODE.exactly().systemAndCode("http://loinc.org", "8867-4"))
                // .and(Observation.DATE.afterOrEquals().day("2026-01-01"))
                .include(Observation.INCLUDE_PATIENT)
                .sort().descending(Observation.DATE)
                .count(50)
                .returnBundle(Bundle.class)
                .execute();

        // List<Observation> all = new ArrayList<>();
//        while (true) {
//            all.addAll(BundleUtil.toListOfResourcesOfType(ctx, page, Observation.class));
//            if (page.getLink(IBaseBundle.LINK_NEXT) == null) break;
//            page = client.loadPage().next(page).execute();
//        }
        System.out.println("Here is the first page of the list:");
        String jsonString = myContext.getCtx().newJsonParser().setPrettyPrint(true).encodeResourceToString(page);
        System.out.println(jsonString);

    }

    /**
     * Executes a set of successive related crud operations
     * @param myContext server context
     */
    static void doCrudOps(MyContext myContext) {
        Patient p = new Patient();
        String patientId = Util.getNextPatientId();
        p.addIdentifier().setSystem("urn:example:mrn").setValue(patientId);
        p.addName().setFamily("Testperson").addGiven("Ada");
        p.setGender(Enumerations.AdministrativeGender.FEMALE);
        p.setBirthDateElement(new DateType("1970-04-12"));

        MethodOutcome created = myContext.getClient().create()
                .resource(p)
                .conditional().where(Patient.IDENTIFIER.exactly()
                        .systemAndIdentifier("urn:example:mrn", patientId))   // If-None-Exist
                .prefer(PreferReturnEnum.REPRESENTATION)
                .execute();
        IIdType id = created.getId();          // e.g. Patient/1001/_history/1

        Patient read = myContext.getClient().read().resource(Patient.class)
                .withId(id.getIdPart()).execute();

        read.getNameFirstRep().addGiven("M");
        myContext.getClient().update().resource(read)
                .withAdditionalHeader("If-Match", "W/\"" + read.getMeta().getVersionId() + "\"")
                .execute();

        myContext.getClient().delete().resourceById(new IdType("Patient", id.getIdPart())).execute();
    }
}
