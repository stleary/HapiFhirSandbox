# FHIR Learning Guide

FHIR \= Fast Healthcare Interoperability Resources

Here's everything from both answers in one list, in the order I'd learn it. Since BCBS MN runs Medicare Advantage and Minnesota Medicaid plans, CMS-0057-F applies, and for Medicaid managed care plans the API deadlines apply to the rating period beginning on or after January 1, 2027\. So everything below is aimed at that work. [health-samurai](https://www.health-samurai.io/docs/payerbox/compliance/cms-0057)

**1\. Regulatory context (a couple of hours)**

* Read the CMS fact sheet for CMS-0057-F. You should be able to name the four APIs (Patient Access, Provider Access, Payer-to-Payer, and Prior Authorization) and say what each one is for.  [https\://www\.cms.gov/newsroom/fact-sheets/cms-interoperability-prior-authorization-final-rule-cms-0057-f](https://www.cms.gov/newsroom/fact-sheets/cms-interoperability-prior-authorization-final-rule-cms-0057-f)  
* Know that it builds on the earlier CMS-9115-F rule, which created the original Patient Access and Provider Directory APIs.

**2\. FHIR fundamentals (an evening or two) (see below)**

* Resources, references between resources, Bundles, and the RESTful API (read, search, create, update, `_include`/`_revinclude`).  
* Terminology bindings: LOINC, SNOMED CT, RxNorm, plus the claims code systems you'll see in EOBs, such as ICD-10, CPT/HCPCS, and NDC.  
* Extensions, profiles, and Implementation Guides.  
* Use FHIR R4 throughout.

**3\. Hands-on with the API (a few hours)**

* Use curl or Postman against the public HAPI test server (`https://hapi.fhir.org/baseR4`).  
* Try searches, `$everything`, and reading references until the JSON feels familiar.

**4\. HAPI FHIR in Java**

* The client side: `FhirContext.forR4()`, `IGenericClient`, and fluent searches that return typed model objects.  
* The server side: run `hapi-fhir-jpaserver-starter` locally with Docker.  
* Load test data into it. Synthea works for clinical data, but also load the example resources from the CARIN and PDex guides, since Synthea's claims data is thin.

**5\. The foundations every payer API sits on**

* **US Core:** the baseline US profiles.  
* **SMART on FHIR:** OAuth2 scopes and authorization, especially the standalone launch pattern members use through third-party apps. Backend services authorization is used for system-to-system access.  
* **Bulk Data (`$export`):** used by Provider Access and Payer-to-Payer to move data for many members at once.  
* **Validation:** using the HAPI validator to check resources against profiles.

**6\. The payer Implementation Guides (the core of the job)**

* **CARIN Blue Button:** claims data for the Patient Access API. Study `ExplanationOfBenefit` in depth, along with `Coverage` and `Patient`.  
* **Da Vinci PDex:** member data exchange for Provider Access and Payer-to-Payer, plus the prior-authorization data that now has to appear in Patient Access.  
* **Da Vinci PDex Plan-Net:** the provider directory (`Practitioner`, `PractitionerRole`, `Organization`, `InsurancePlan`).  
* **Da Vinci CRD, DTR, and PAS:** the prior authorization workflow. CRD tells the provider whether prior auth is needed, DTR gathers the documentation, and PAS submits the request. Understand how the three connect end to end.  
* **X12 278:** conceptually only. It's the legacy prior-auth transaction that PAS maps to, and payers still process it.

**7\. A practice project**

* Build a Spring Boot service that pulls a member's claims history (EOBs) from your local HAPI server and summarizes it.  
* For a stretch goal, add a Plan-Net provider directory search or a mock PAS request.

**8\. Community and ongoing learning**

* chat.fhir.org (Zulip). The Da Vinci and CARIN streams are the most relevant.  
* FHIR DevDays talks on YouTube.

**What to deprioritize**

* EHR app launch and clinical workflow apps, such as building apps that run inside Epic.  
* Deep clinical profiling beyond US Core.  
* FHIR R5 and R6.

For the interview, steps 1, 2, and the overview level of step 6 give you the most credibility for the least time. The hands-on steps matter more once you're on the job.

---

**Step 2 FHIR fundamentals:**  
The best place is the FHIR specification itself at **hl7.org/fhir/R4**. It's free, authoritative, and better written than most specs. With your background, reading the right pages in the right order will get you through the fundamentals in an evening or two.

**Reading order (all under [`https://hl7.org/fhir/R4/`](https://hl7.org/fhir/R4/)):**

1. `overview-dev.html`: the overview written for developers. Start here.  
2. `resource.html`: what every resource has in common (id, meta, narrative, contained resources).  
3. `datatypes.html`: skim it. Pay attention to CodeableConcept, Coding, Identifier, Reference, and Period, which appear everywhere, including claims.  
4. `references.html`: how resources link to each other.  
5. `http.html`: the RESTful API. Read this one carefully.  
6. `search.html`: search parameters, chaining, `_include`/`_revinclude`, and paging. It's long, but it's the part you'll use most.  
7. `bundle.html`: search results, transactions, and batches.  
8. `terminologies.html`: code systems, value sets, and bindings.  
9. `extensibility.html`, then `profiling.html`: extensions and profiles, which are the basis of every Implementation Guide you'll study later.  
10. `explanationofbenefit.html`: look at the resource and its examples as a preview of the payer world.

Keep the public HAPI test server open while you read and run each concept as a real request. That makes it stick much faster than reading alone.

**Supplements, if you want them:**

* **FHIR DevDays talks on YouTube.** Firely runs DevDays, and many introductory talks are free.  
* **HAPI FHIR documentation (hapifhir.io).** Useful once you move to the Java step.  
* **chat.fhir.org.** Its "implementers" stream is a good place to ask when the spec is unclear.

**A paid option, which I'd skip for now:** HL7 International runs an official FHIR Fundamentals course, taught by HL7 Certified Educators and covering the spec's structure, resources, profiles, extensions, and APIs. It's cohort-based and runs about four weeks, from July 9 to August 6 in the 2026 session, and the regular price is about \$1,000. It's good, but it's slower and more expensive than self-study, and you likely don't need the hand-holding.

