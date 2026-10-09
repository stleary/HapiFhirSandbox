# **FHIR REST API Mini-Course**

## **How to use this course**

Nine modules plus a capstone, about 15–20 hours total: one module per sitting, two or three sittings a week gets you through in about three weeks. It assumes you already know Java, Gradle, Spring Boot, REST and OAuth2 well, so it spends its time only on what is FHIR-specific.

The arc is deliberate: learn the data model first, then drive the raw HTTP API with curl so you see exactly what goes over the wire, then let the HAPI FHIR library do the work, then build a server of your own.

**Setup (30 minutes)**

1. JDK 17 or newer and compatible Gradle.  
2. A scratch Gradle project with these dependencies (HAPI FHIR 8.12.0 was the current release as of August 2026; check Maven Central for newer):

plugins {  
    id 'java'  
}

java {  
    toolchain {  
        languageVersion \= JavaLanguageVersion.of(17)  
    }  
}

repositories {  
    mavenCentral()  
}

ext {  
    hapiVersion \= '8.12.0'  
}

dependencies {  
    implementation "ca.uhn.hapi.fhir:hapi-fhir-structures-r4:\${hapiVersion}"  
    implementation "ca.uhn.hapi.fhir:hapi-fhir-client:\${hapiVersion}"

    // Module 6  
    implementation "ca.uhn.hapi.fhir:hapi-fhir-validation:\${hapiVersion}"  
    implementation "ca.uhn.hapi.fhir:hapi-fhir-validation-resources-r4:\${hapiVersion}"  
    implementation "ca.uhn.hapi.fhir:hapi-fhir-caching-caffeine:\${hapiVersion}"

    // Module 8  
    implementation "ca.uhn.hapi.fhir:hapi-fhir-server:\${hapiVersion}"  
}

3. Two servers to practice against:  
   * The public HAPI test server at https\://hapi.fhir.org/baseR4. It is shared, public and periodically wiped, so never put real patient data on it.  
   * Your own local server (from Module 2 on):   
     docker run \-it \-p 8080:8080 hapiproject/hapi:latest  
     base URL   
     [http\://localhost:8080/fhir](http://localhost:8080/fhir)  
     Use this whenever you want predictable data.  
4. Keep the spec open in a tab: [hl7.org/fhir/R4](https://hl7.org/fhir/R4/). Every resource page has the JSON shape, the search parameters and examples; you will live there.

Each module ends with exercises. Do them; FHIR only clicks once you have fought a few 400 responses yourself.

---

## **Module 1 — The FHIR mental model (2 hours)**

FHIR is a catalog of about 145 standardized JSON/XML document types ("resources") plus a uniform REST API over them. Learn the shapes first; the API is simple once the data makes sense.

**Core concepts**

* **Resource**: the unit of exchange. Every resource has resourceType, a server-assigned id, and meta (versionId, lastUpdated, profile). The workhorses: Patient, Practitioner, Organization, Encounter, Observation, Condition, MedicationRequest, AllergyIntolerance, Coverage, ExplanationOfBenefit, Claim.  
* **Datatypes**: primitives (string, date, dateTime, code, uri) and complex types you will see everywhere: Identifier (system \+ value, e.g. an MRN or member ID), CodeableConcept (one or more Codings, each system \+ code \+ display), Quantity, Period, Reference, HumanName.  
* **Identity vs. identifiers**: id is the server's key for the resource; identifier is a business key from the outside world. Real integrations match on identifiers, not ids.  
* **References**: resources link by {"reference": "Patient/123"}. That is how FHIR builds a graph out of flat documents; search leans on it heavily.  
* **Terminology**: codes come from external systems identified by URI, e.g. LOINC (http\://loinc.org) for lab and vital observations, SNOMED CT (http\://snomed.info/sct) for conditions, RxNorm for drugs. ValueSets bind an element to an allowed set of codes.  
* **Extensions**: any element can carry extension: \[{url, value\[x\]}\]. This is how FHIR stays small while still carrying local data (race and ethnicity in US Core are extensions, for example).  
* **Choice types (\[x\])**: Observation.value\[x\] becomes valueQuantity, valueString, valueCodeableConcept and so on. The suffix names the type; only one appears.  
* **Profiles and Implementation Guides (IGs)**: a profile constrains a base resource (makes fields required, fixes codes, adds extensions). An IG is a published bundle of profiles, rules and examples. In the US you rarely deal with "raw" FHIR; you deal with US Core and IGs built on it (Module 9).  
* **Versions**: R4 (4.0.1) is what production US systems run and what regulations name. R4B is a minor update, R5 exists, R6 is in development. Learn R4.

**A minimal Observation to read carefully**

{  
  "resourceType": "Observation",  
  "status": "final",  
  "category": \[{ "coding": \[{  
    "system": "http\://terminology.hl7.org/CodeSystem/observation-category",  
    "code": "vital-signs" }\] }\],  
  "code": { "coding": \[{  
    "system": "http\://loinc.org", "code": "8867-4", "display": "Heart rate" }\] },  
  "subject": { "reference": "Patient/123" },  
  "effectiveDateTime": "2026-09-30T09:15:00-05:00",  
  "valueQuantity": { "value": 72, "unit": "beats/minute",  
    "system": "http\://unitsofmeasure.org", "code": "/min" }  
}  
Notice the pattern that repeats everywhere: meaning is carried by (system, code) pairs, not by free text, and units are UCUM codes.

**Exercises**

- [ ] Read the Patient, Observation and Encounter pages in the R4 spec. For each, note the required elements (cardinality 1..1) and the "must support" flags. Must Support is in the [US Core IG](https://build.fhir.org/ig/HL7/US-Core/):   
        
- [ ] Open the Observation examples tab and find one each of valueQuantity,

- [ ] Write a Patient JSON by hand with two identifiers, a name, gender, birth date and one extension. You will POST it in Module 2\.

---

## **Module 2 — The REST API over raw HTTP (2 hours)**

FHIR's REST API is a small, fixed set of interactions applied uniformly to every resource type, all relative to a base URL. Once you know them for Patient, you know them for everything.

| Interaction | HTTP | Success |
| :---- | :---- | :---- |
| capabilities | GET \[base\]/metadata | 200 \+ CapabilityStatement |
| read | GET \[base\]/Patient/123 | 200, or 404 / 410 (deleted) |
| vread | GET \[base\]/Patient/123/\_history/2 | 200 |
| create | POST \[base\]/Patient | 201 \+ Location header |
| update | PUT \[base\]/Patient/123 | 200 (or 201 if the server allows client ids) |
| patch | PATCH \[base\]/Patient/123 (JSON Patch or FHIRPath Patch) | 200 |
| delete | DELETE \[base\]/Patient/123 | 200 or 204 |
| history | GET \[base\]/Patient/123/\_history | 200 \+ Bundle |
| search | GET \[base\]/Patient?family=smith | 200 \+ Bundle (Module 3\) |

**Headers that matter**

* Accept: application/fhir+json   
  Content-Type: application/fhir+json   
  	Or use ?\_format=json in a browser.  
* Prefer: return=representation   
  	asks the server to echo the stored resource on create/update;   
  	return=minimal returns just headers.  
* Versioning: responses carry ETag: W/"3". Send   
  If-Match: W/"3" on PUT to get optimistic locking; a stale version gets 412\.  
  See [Appendix](#appendix) at the end of this doc for more info about If-Match.  
* Conditional create: If-None-Exist: identifier=urn:example:mrn|12345 makes POST idempotent on a business key.

**Errors are resources too.** A 4xx/5xx body is an OperationOutcome with issue\[\] (severity, code, diagnostics, expression). Always log it; it usually tells you exactly which element was wrong.

**Important note about errors:** Both the local and hapifhir servers are lenient by default. They will allow all kinds of errors that a production server might not. This can make triggering an error in some of the exercises a little more tricky, and will be noted as needed.

**Set up Postman**

1. Start the Docker Desktop app.  
2. Start the local server:   
   `docker run -p 8080:8080 hapiproject/hapi:latest`  
   You may have to give it permission to download hapiproject image.  
3. Use the Postman desktop app, since the web version can't reach `localhost` without the desktop agent.  
4. Create a collection named "FHIR Module 2". On its Variables tab, add   
   `base` \= `http://localhost:8080/fhir`

**Try it**

Save each of these as a request in the collection.

- [ ]  **Capabilities**  
      `GET {{base}}/metadata`  
      In the response, find `fhirVersion`, then expand `rest[0].resource` to see the supported resource types. To find a resource, e.g. Observation, install the jsonpath plugin for intellij, paste the json response into a json file, then search for:  
      \$.rest\[\*\].resource\[?(@.type \== 'Observation')\]  
       Search using context-click \> evaluate JSONPath  in the json file OR Edit \> Find \> evaluate JSONPath

- [ ] **Create.** `POST {{base}}/Patient` with the `Content-Type` and `Prefer` headers above. Under Body \> raw \> JSON, paste the Patient you wrote in Module 1\. Under Scripts \> Post-response (the Tests tab in older versions), add the line below to capture the new id. Send, then check for 201 and the `Location` header.  
      m.collectionVariables.set("patientId", pm.response.json().id);

- [ ] **Read.** `GET {{base}}/Patient/{{patientId}}`

- [ ] **Update.** `PUT {{base}}/Patient/{{patientId}}`   
      Headers:  
      `Content-Type: application/fhir+json`  
      `If-Match: W/"1"`  
      The body is raw / json, with your Patient JSON containing one change, plus add the key/value `"id": "{{patientId}}"`. The body id must match the URL or the server returns 400; Postman resolves variables inside raw bodies.

- [ ] **History.** `GET {{base}}/Patient/{{patientId}}/_history`. Look at `resource.meta` in each entry for the version number.

You should get 200 or 201 response for each request.

**CURL equivalent** (from an earlier version of this doc)

BASE=http\://localhost:8080/fhir  
curl \-s \$BASE/metadata | jq '.fhirVersion, .rest\[0\].resource\[\].type' | head

curl \-si \-X POST \$BASE/Patient \\  
  \-H 'Content-Type: application/fhir+json' \\  
  \-H 'Prefer: return=representation' \\  
  \-d @patient.json            \# the one you wrote in Module 1

curl \-s \$BASE/Patient/\<id\> | jq  
curl \-si \-X PUT \$BASE/Patient/\<id\> \-H 'Content-Type: application/fhir+json' \\  
  \-H 'If-Match: W/"1"' \-d @patient-updated.json  
curl \-s \$BASE/Patient/\<id\>/\_history | jq '.entry\[\].resource.meta'

**Exercises**

- [ ] Duplicate the Capabilities request for the hapi fhir server:  
      [`https://hapi.fhir.org/baseR4/metadata`](https://hapi.fhir.org/baseR4)  
      Use JSONPath to isolate Observation and compare between local and hapifhir. Note the interactions are the same, but there are additional search params for the hapi fhir server (leading \_, maybe others?)  
      The remaining exercises are only for the local server (hapifhir gets wiped periodically)

- [ ] Create, read, update twice, vread version 1, then delete a Patient.   
      (Bump `If-Match` to `W/"2"` for the second update.)  
      Read it after delete and note the 410 GONE status code.  
      vread v1 or v2 and confirm the historic version is still there.  
      See [Appendix](#appendix) for info about vread (version read)

- [ ] Trigger three different errors and confirm the OperationOutcome:  
      bad JSON: 		400 Bad Request (send an invalid birthday value)  
      	Just adding a random key/value is allowed/ignored by lenient servers.  
      unknown element:	200 but with issue.severity value 'error'. You have  
      	to postpend /\$validate to the URL to see the issue, and \$validate in   
      	itself prevents the update from occurring.  
      stale `If-Match`:	409 Conflict (on update of a deleted record)

- [ ] Create an Observation that references your Patient. (Use `"subject": {"reference": "Patient/{{patientId}}"}`.)

The spec, the HAPI docs, and the later modules still show examples in curl, but Postman's Import accepts a pasted curl command and converts it to a request. For instance, the curl line in Module 4 becomes a `POST {{base}}` with the transaction Bundle as the raw body.

---

## **Module 3 — Search in depth (2–3 hours)**

Search is where most real client code lives and where FHIR is least like ordinary REST: parameters are defined per resource, typed, and composable. Every search returns a `Bundle` of type `searchset`.

**Building a search string, step by step**

Every search URL has the same anatomy:

\[base\]/\[ResourceType\]?\[name\]\[:modifier\]=\[prefix\]\[value\]\[,value\]&\[name\]=...&\[\_result params\]

1. **Pick the resource type you want back.** Want Observations? Search `Observation`, even if your criterion is about the patient (use chaining for that).  
2. **Look up the parameter names; never guess them.** Search parameter names are not element names: `Observation.effectiveDateTime` is searched as `date`, `Patient.name.family` as `family`. Two places to look:  
   * The spec: every resource page ends with a Search Parameters table listing name, type and the FHIRPath expression it indexes (e.g. [Observation search parameters](https://hl7.org/fhir/R4/observation.html#search)). The [search parameter registry](https://hl7.org/fhir/R4/searchparameter-registry.html) lists them all on one page.  
   * The server: `GET [base]/metadata`, then `rest[0].resource[type=Observation].searchParam[]`. This is the authority, since servers may support only some parameters or add custom ones (IGs like US Core define their own).  
3. **Note each parameter's type.** The type (string, token, date, reference, quantity, composite) decides the value syntax, the allowed modifiers and whether prefixes apply. See the table below.  
4. **Write each criterion, then join them.** `&` between different criteria (AND); comma inside one value (OR); repeat a name for AND on the same parameter (`date=ge2026-01-01&date=lt2026-02-01` is a range).  
5. **Add result parameters last**: `_count`, `_sort`, `_include`, `_revinclude`, `_elements`, `_summary`.  
6. **URL-encode the values.** `|` becomes `%7C`, a space `%20`, and especially `+` in a timezone offset becomes `%2B` (an unencoded `+` is read as a space and the date breaks). Commas, `$` and `|` that are part of a literal value are escaped with a backslash, e.g. `\,` ([escaping rules](https://hl7.org/fhir/R4/search.html#escaping)).

**Worked example**: "Heart-rate readings over 100/min for patients named Smith since January, newest first, with the patient included":

GET \[base\]/Observation

  ?code=http\://loinc.org%7C8867-4                 token: system|code

  \&value-quantity=gt100                            quantity with prefix

  \&subject:Patient.family=smith                    chained reference \-\> string

  \&date=ge2026-01-01                               date with prefix

  &\_sort=-date&\_count=20

  &\_include=Observation:subject

(Shown on several lines for reading; send it as one line.)

**GET vs POST.** Long queries, or ones carrying identifiers you'd rather not leave in access logs, can go as `POST [base]/Observation/_search` with the same parameters in an `application/x-www-form-urlencoded` body. Results are identical.

**Tooling tips**

* Postman: enter each parameter as a row in the Params tab rather than typing the URL, and check the generated URL for `|` and `+`; encode them yourself if Postman leaves them raw.  
* curl: `curl -G "$BASE/Observation" --data-urlencode 'code=http://loinc.org|8867-4' --data-urlencode 'date=ge2026-01-01'` handles encoding for you.  
* When a search returns more than you expected, look at the `self` link in the result Bundle. Servers echo only the parameters they actually applied, so a missing one means it was silently ignored. Add `Prefer: handling=strict` to get an error instead.  
* In Java, HAPI's fluent `search()` builder (Module 5\) generates these strings for you. Turn on `LoggingInterceptor` to see the URL it built; it is a great way to learn the syntax.

**Reading**: the [Search chapter of the R4 spec](https://hl7.org/fhir/R4/search.html) is long but is the definitive reference. Read the sections on parameter types, modifiers and prefixes first, then chaining and `_include`.

**Parameter types and their syntax**

| Type | Example | Notes |
| ----- | ----- | ----- |
| string | `Patient?family=smi` | Case- and accent-insensitive starts-with by default; `:exact`, `:contains` |
| token | `Observation?code=http://loinc.org|8867-4` | `system|code`; also `|code` or `code`; `:not`, `:text` |
| date | `Observation?date=ge2026-01-01&date=lt2026-07-01` | Prefixes `eq ne gt lt ge le sa eb ap`; repeating \= AND |
| reference | `Observation?subject=Patient/123` | Or `patient=123` shortcut |
| quantity | `Observation?value-quantity=gt100|http://unitsofmeasure.org|mg` | prefix \+ value \+ system \+ code |
| composite | `Observation?code-value-quantity=8867-4$gt100` | Two params matched on the same element |

Comma \= OR within one parameter (`status=final,amended`); repeating a parameter \= AND.

**Result shaping and navigation**

* `_count=50`, `_sort=-date`, `_summary=count` (just the total), `_elements=id,name`.  
* `_include`: pull referenced resources into the same bundle, `Observation?patient=123&_include=Observation:performer`.  
* `_revinclude`: pull resources that point at the matches, `Patient?_id=123&_revinclude=Observation:subject`.  
* Chaining walks a reference forward: `Observation?subject:Patient.family=smith`.  
* Reverse chaining (`_has`) filters by who points at you: `Patient?_has:Observation:subject:code=8867-4`.  
* `:missing=true|false` on any parameter.  
* Paging: follow `Bundle.link` where `relation` is `next`. Treat that URL as opaque; never build page URLs yourself. `Bundle.total` is optional and often absent.  
* In a searchset, `entry.search.mode` is `match` or `include`, so you can tell hits from included extras.

**Exercises** (against `https://hapi.fhir.org/baseR4`, which has plenty of data)

- [ ] Find female patients born before 1960 whose family name starts with "Sm".

- [ ] Find all heart-rate Observations over 100/min, newest first, 10 per page, and walk three pages by following `next` links.

- [ ] Fetch one patient with all of their Conditions and Encounters in a single request.

- [ ] Find Patients who have at least one MedicationRequest, using *`has`.*

- [ ] Pass an unsupported search parameter and see how the server reports it.

## **Module 4 — Bundles, transactions and batches (1.5 hours)**

A transaction Bundle posted to the base URL is FHIR's unit of atomic, multi-resource work: all entries succeed or none do. A batch runs the same way but each entry succeeds or fails on its own.

**Bundle types you will meet**: searchset and history (responses), transaction and batch (requests), transaction-response / batch-response, document and message (specialized exchange formats, rarely needed for REST work).

**The key trick: temporary ids.** Inside a transaction, give a new resource a fullUrl of urn:uuid:... and reference that urn from other entries. The server assigns real ids and rewrites the references.

{  
  "resourceType": "Bundle",  
  "type": "transaction",  
  "entry": \[  
    {  
      "fullUrl": "urn:uuid:0f3c6e0a-1111-4d7a-9a55-2b8e5c1f0001",  
      "resource": { "resourceType": "Patient",  
        "identifier": \[{ "system": "urn:example:mrn", "value": "12345" }\],  
        "name": \[{ "family": "Testperson", "given": \["Ada"\] }\] },  
      "request": { "method": "POST", "url": "Patient",  
                   "ifNoneExist": "identifier=urn:example:mrn|12345" }  
    },  
    {  
      "resource": { "resourceType": "Observation", "status": "final",  
        "code": { "coding": \[{ "system": "http\://loinc.org", "code": "8867-4" }\] },  
        "subject": { "reference": "urn:uuid:0f3c6e0a-1111-4d7a-9a55-2b8e5c1f0001" },  
        "valueQuantity": { "value": 72, "system": "http\://unitsofmeasure.org", "code": "/min" } },  
      "request": { "method": "POST", "url": "Observation" }  
    }  
  \]  
}

POST it to your local server with curl \-X POST \$BASE \-H 'Content-Type: application/fhir+json' \-d @tx.json (note: the base URL, not /Bundle) OR Just use Postman. POSTing to /Bundle just stores the Bundle as a document, which is a classic beginner mistake.

Lots more detail about Identifier vs Id:

**The identifier**

"identifier": \[{ "system": "urn:example:mrn", "value": "12345" }\]

An identifier is a business key that comes from the outside world, as opposed to `id`, which the FHIR server assigns. Think of it as the difference between a database surrogate key and a natural key. The same person can have many identifiers: a hospital medical record number (MRN), a health plan member ID, a driver's license number. Each FHIR server holding that person will give them a different `id`.

It has two parts:

* **`system`** is a URI naming who issued the number. A bare value like "12345" is meaningless on its own, since many organizations could issue that same number; the system makes the pair globally unique. Real systems are things like `http://hl7.org/fhir/sid/us-npi` for provider NPIs, or an organization-specific URL or `urn:oid:...` for a hospital's MRNs or a payer's member IDs.  
* **`value`** is the number itself, unique within that system.

`urn:example:mrn` is a made-up system for practice. The `urn:example:` prefix is a convention for "not real, don't resolve this".

**Why it matters in this example:** the request carries `"ifNoneExist": "identifier=urn:example:mrn|12345"`. That tells the server to create this Patient only if no Patient already has that system|value pair, and otherwise to use the existing Patient record. That's what makes the transaction safe to re-run. Matching on identifiers rather than ids is how real integrations avoid duplicate patients, because the sender never knows the receiver's ids.

You'll also see optional parts: `type` (a coded kind, e.g. `MR` for medical record number), `use` (`official`, `secondary`, …), `period` and `assigner`. They're useful, but `system` \+ `value` is what does the matching.

**Worth knowing**

* The server processes entries in a defined order (DELETE, POST, PUT/PATCH, GET), not file order.  
* Conditional references like "reference": "Patient?identifier=urn:example:mrn|12345" let you link to an existing resource by business key, if the server supports them.  
* Conditional create (ifNoneExist) plus conditional references make loads idempotent, which is exactly what you want for re-runnable data feeds.

**Exercises**

* ☐ POST the transaction above twice. Confirm the second run does not create a duplicate Patient (look at response.status for each entry).  
* ☐ Break one entry on purpose and confirm nothing from the transaction was saved. Then send the same bundle as batch and compare.  
* ☐ Build a transaction that creates a Patient, an Encounter and two Observations that reference both.

## **Module 5 — The HAPI FHIR client in Java (3 hours)**

HAPI FHIR gives you a typed model class for every resource (org.hl7.fhir.r4.model.\*), a parser, and a fluent generic client that maps one-to-one onto the interactions you just did with curl.

**FhirContext and the client**

FhirContext is expensive to build (it scans the whole model) and thread-safe, so create one per FHIR version per application. Clients are cheap.

FhirContext ctx \= FhirContext.forR4();  
ctx.getRestfulClientFactory().setSocketTimeout(30\_000);  
// By default the client GETs /metadata before its first call; skip that for known servers  
ctx.getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);

IGenericClient client \= ctx.newRestfulGenericClient("http\://localhost:8080/fhir");  
client.registerInterceptor(new LoggingInterceptor(true));     // logs requests/responses  
// client.registerInterceptor(new BearerTokenAuthInterceptor(token));  // Module 7  
In Spring Boot, make FhirContext and IGenericClient @Beans.

**CRUD**

Patient p \= new Patient();  
p.addIdentifier().setSystem("urn:example:mrn").setValue("12345");  
p.addName().setFamily("Testperson").addGiven("Ada");  
p.setGender(Enumerations.AdministrativeGender.FEMALE);  
p.setBirthDateElement(new DateType("1970-04-12"));

MethodOutcome created \= client.create()  
    .resource(p)  
    .conditional().where(Patient.IDENTIFIER.exactly()  
        .systemAndIdentifier("urn:example:mrn", "12345"))   // If-None-Exist  
    .prefer(PreferReturnEnum.REPRESENTATION)  
    .execute();  
IIdType id \= created.getId();          // e.g. Patient/1001/\_history/1

Patient read \= client.read().resource(Patient.class)  
    .withId(id.getIdPart()).execute();

read.getNameFirstRep().addGiven("M");  
client.update().resource(read)  
    .withAdditionalHeader("If-Match", "W/\\"" \+ read.getMeta().getVersionId() \+ "\\"")  
    .execute();

client.delete().resourceById(new IdType("Patient", id.getIdPart())).execute();  
**Search and paging**

Bundle page \= client.search()  
    .forResource(Observation.class)  
    .where(Observation.CODE.exactly().systemAndCode("http\://loinc.org", "8867-4"))  
    .and(Observation.DATE.afterOrEquals().day("2026-01-01"))  
    .include(Observation.INCLUDE\_PATIENT)  
    .sort().descending(Observation.DATE)  
    .count(50)  
    .returnBundle(Bundle.class)  
    .execute();

List\<Observation\> all \= new ArrayList\<\>();  
while (true) {  
    all.addAll(BundleUtil.toListOfResourcesOfType(ctx, page, Observation.class));  
    if (page.getLink(IBaseBundle.LINK\_NEXT) \== null) break;  
    page \= client.loadPage().next(page).execute();  
}  
Note that toListOfResourcesOfType returns included resources too if they match the type; filter on entry.getSearch().getMode() when that matters.

**Transactions**

Bundle tx \= new Bundle().setType(Bundle.BundleType.TRANSACTION);  
String patientUrn \= IdType.newRandomUuid().getValue();   // "urn:uuid:..."  
tx.addEntry().setFullUrl(patientUrn).setResource(p)  
  .getRequest().setMethod(Bundle.HTTPVerb.POST).setUrl("Patient");

Observation hr \= new Observation();  
hr.setStatus(Observation.ObservationStatus.FINAL);  
hr.getCode().addCoding().setSystem("http\://loinc.org").setCode("8867-4");  
hr.setSubject(new Reference(patientUrn));  
hr.setValue(new Quantity().setValue(72)  
    .setSystem("http\://unitsofmeasure.org").setCode("/min"));  
tx.addEntry().setResource(hr)  
  .getRequest().setMethod(Bundle.HTTPVerb.POST).setUrl("Observation");

Bundle resp \= client.transaction().withBundle(tx).execute();  
resp.getEntry().forEach(e \-\> System.out.println(e.getResponse().getLocation()));  
**Parsing and errors**

* ctx.newJsonParser().setPrettyPrint(true).encodeResourceToString(r) and parseResource(Patient.class, json). Handy for fixtures in tests.  
* Server errors surface as subclasses of BaseServerResponseException: ResourceNotFoundException (404), ResourceGoneException (410), InvalidRequestException (400), UnprocessableEntityException (422), PreconditionFailedException (412). getOperationOutcome() gives you the parsed body.  
* The model is mutable and not thread-safe; don't share resource instances across threads.

**Exercises**

* ☐ Redo every Module 2 and Module 3 exercise in Java.  
* ☐ Write PatientRepository with findByMrn(String), upsert(Patient) (conditional create/update) and observations(String patientId, String loincCode) that pages through everything.  
* ☐ Write a JUnit test that forces a 412 with a stale If-Match and asserts on the OperationOutcome.

## **Module 6 — Validation, profiles and operations (2 hours)**

A resource that parses is not necessarily valid. Validation checks cardinality, datatypes, invariants (FHIRPath rules), terminology bindings and, most importantly in practice, conformance to a profile such as US Core.

**Validating in Java**

NpmPackageValidationSupport npm \= new NpmPackageValidationSupport(ctx);  
npm.loadPackageFromClasspath("classpath:package/hl7.fhir.us.core-6.1.0.tgz");  // download from packages.fhir.org

ValidationSupportChain chain \= new ValidationSupportChain(  
    npm,  
    new DefaultProfileValidationSupport(ctx),  
    new CommonCodeSystemsTerminologyService(ctx),  
    new InMemoryTerminologyServerValidationSupport(ctx),  
    new SnapshotGeneratingValidationSupport(ctx));

FhirValidator validator \= ctx.newValidator();  
validator.registerValidatorModule(new FhirInstanceValidator(chain));

p.getMeta().addProfile("http\://hl7.org/fhir/us/core/StructureDefinition/us-core-patient");  
ValidationResult result \= validator.validateWithResult(p);  
result.getMessages().forEach(m \-\>  
    System.out.println(m.getSeverity() \+ " " \+ m.getLocationString() \+ " " \+ m.getMessage()));  
Build the validator once; it is slow to warm up. Pick the US Core version your target system actually declares (check its CapabilityStatement), not just the newest.

Alternatives: the official HL7 command-line validator (validator\_cli.jar, from the hapifhir/org.hl7.fhir.core releases) is great for checking sample files, and many servers expose POST \[base\]/Patient/\$validate.

**Operations** are RPC-style calls named with a \$, invoked at system, type or instance level, taking and returning a Parameters resource (or a Bundle). The ones worth knowing:

* Patient/123/\$everything: everything in the patient's compartment, as a Bundle.  
* \$validate: server-side validation.  
* ValueSet/\$expand, CodeSystem/\$lookup, ValueSet/\$validate-code: terminology services.  
* \$export: Bulk Data, asynchronous NDJSON export of whole populations. Big in payer and analytics work.  
* \$member-match (Da Vinci HRex): payer-to-payer member matching.

Bundle everything \= client.operation()  
    .onInstance(new IdType("Patient", "123"))  
    .named("\$everything")  
    .withNoParameters(Parameters.class)  
    .returnResourceType(Bundle.class)  
    .execute();  
**Exercises**

* ☐ Validate your Module 1 Patient against base R4, then against US Core Patient. List what US Core adds.  
* ☐ Make a US Core–conformant Patient that passes with zero errors, including the race and ethnicity extensions.  
* ☐ Call \$everything on a patient you loaded with a transaction.  
* ☐ Read the Bulk Data Access spec ([hl7.org/fhir/uv/bulkdata](https://hl7.org/fhir/uv/bulkdata/)) and run a \$export against your local HAPI server (kick off, poll the Content-Location, download the NDJSON).

## **Module 7 — Security: SMART on FHIR and OAuth2 (2 hours)**

FHIR itself says almost nothing about auth; SMART App Launch (v2.x) is the OAuth2/OIDC profile the industry settled on. You already know OAuth2, so focus on what SMART adds: discovery, FHIR-shaped scopes, launch context, and a JWT-based flow for server-to-server work.

**Discovery**: GET \[base\]/.well-known/smart-configuration returns authorization\_endpoint, token\_endpoint, supported scopes and capabilities.

**Scopes** follow context/Resource.permissions:

* Context: patient/ (one patient's data), user/ (whatever the signed-in user may see), system/ (backend, no user).  
* SMART v2 permissions are letters from cruds (create, read, update, delete, search): patient/Observation.rs. SMART v1 used .read / .write; you will see both in the wild.  
* Extras: launch/patient (ask for a patient to be picked), openid fhirUser (who is logged in), offline\_access (refresh token).  
* v2 scopes can be narrowed by search params, e.g. patient/Observation.rs?category=laboratory.

**Standalone launch** (a user opens your app directly)

1. Discover endpoints.  
2. Redirect to authorization\_endpoint with response\_type=code, client\_id, redirect\_uri, scope, state, PKCE code\_challenge (S256, required in v2) and aud=\<FHIR base URL\>. Forgetting aud is the most common failure.  
3. Exchange the code at token\_endpoint. The response carries access\_token plus launch context such as patient: "123".  
4. Call the FHIR API with Authorization: Bearer ... (BearerTokenAuthInterceptor in HAPI).

**EHR launch** is the same, except the EHR opens your app with iss and launch parameters, and you pass launch back in step 2\.

**Backend Services** (system-to-system, no user; typical for payers and data pipelines)

1. Register your public key (JWKS) with the server ahead of time.  
2. Build a short-lived JWT (iss \= sub \= client\_id, aud \= token endpoint, jti, exp ≤ 5 minutes) signed with RS384 or ES384.  
3. POST grant\_type=client\_credentials, client\_assertion\_type=urn:ietf:params:oauth:client-assertion-type:jwt-bearer, client\_assertion=\<jwt\>, scope=system/Patient.rs system/Coverage.rs.  
4. Use the token; refresh by repeating step 2–3 (no refresh tokens).

In Java, Spring Security's OAuth2 client handles auth code \+ PKCE; Nimbus JOSE+JWT (already on Spring Security's classpath) signs the Backend Services assertion.

**Exercises**

* ☐ Use the SMART sandbox at [launch.smarthealthit.org](https://launch.smarthealthit.org) to do a standalone launch from a small Spring Boot app, then read the launched patient's Observations.  
* ☐ Request a narrower scope set and confirm you get 403s outside it.  
* ☐ Implement a Backend Services token client with Nimbus: generate an RSA key pair, publish the JWKS, sign the assertion. The same sandbox supports Backend Services testing.

## **Module 8 — Building a FHIR server with HAPI and Spring Boot (2–3 hours)**

HAPI offers two server styles. The **Plain Server** is a servlet where you write resource providers and own the storage: this is the "FHIR facade" pattern organizations use to put a FHIR API in front of an existing database. The **JPA Server** is a complete, persisted, fully searchable server (what you ran in Docker); you configure it rather than code it.

**A facade in Spring Boot** (add hapi-fhir-server)

@Component  
public class PatientProvider implements IResourceProvider {  
    private final Map\<String, Patient\> store \= new ConcurrentHashMap\<\>();

    @Override public Class\<Patient\> getResourceType() { return Patient.class; }

    @Read  
    public Patient read(@IdParam IdType id) {  
        Patient p \= store.get(id.getIdPart());  
        if (p \== null) throw new ResourceNotFoundException(id);  
        return p;  
    }

    @Create  
    public MethodOutcome create(@ResourceParam Patient p) {  
        String id \= UUID.randomUUID().toString();  
        p.setId(new IdType("Patient", id, "1"));  
        store.put(id, p);  
        return new MethodOutcome(p.getIdElement()).setCreated(true);  
    }

    @Search  
    public List\<Patient\> byFamily(@RequiredParam(name \= Patient.SP\_FAMILY) StringParam family) {  
        String f \= family.getValue().toLowerCase();  
        return store.values().stream()  
            .filter(p \-\> p.getName().stream()  
                .anyMatch(n \-\> n.getFamily() \!= null && n.getFamily().toLowerCase().startsWith(f)))  
            .toList();  
    }  
}

@Configuration  
class FhirServerConfig {  
    @Bean FhirContext fhirContext() { return FhirContext.forR4(); }

    @Bean  
    ServletRegistrationBean\<RestfulServer\> fhirServlet(FhirContext ctx, PatientProvider patients) {  
        RestfulServer server \= new RestfulServer(ctx);  
        server.registerProvider(patients);  
        server.registerInterceptor(new ResponseHighlighterInterceptor()); // pretty HTML in browsers  
        return new ServletRegistrationBean\<\>(server, "/fhir/\*");  
    }  
}  
HAPI generates /fhir/metadata from your annotations, handles content negotiation, \_format, \_pretty, errors as OperationOutcome, and Bundle wrapping. Swap the map for a Spring Data repository and a mapper, and you have a real facade.

**Where to go next on the server side**

* @Update, @Delete, @History, @Operation for custom \$operations, and IBundleProvider for server-side paging of large result sets.  
* Interceptors (@Hook(Pointcut.SERVER\_INCOMING\_REQUEST\_PRE\_HANDLED) etc.) for auth, audit and tenant routing; AuthorizationInterceptor for rule-based access tied to SMART scopes.  
* The JPA starter project ([github.com/hapifhir/hapi-fhir-jpaserver-starter](https://github.com/hapifhir/hapi-fhir-jpaserver-starter)) when you want a full server backed by Postgres.

**Exercises**

* ☐ Build the facade above and point your Module 5 client at it. Every client test should pass for the interactions you implemented.  
* ☐ Add @Update with version checking (return 412 on a stale If-Match) and an Observation provider with a patient search parameter.  
* ☐ Add a validating interceptor (RequestValidatingInterceptor) using your Module 6 validator so bad resources are rejected with 422\.

## **Module 9 — The real-world US landscape (1.5 hours)**

In production you almost never implement "FHIR"; you implement specific Implementation Guides that federal rules point to. Knowing which IG applies to which exchange is most of what separates a FHIR-literate developer from a productive one.

**The regulatory drivers**

* **ONC/ASTP certification** requires certified EHRs to expose US Core profiles through SMART-secured APIs, which is why US Core is the baseline everything else builds on.  
* **CMS Interoperability and Patient Access rule (CMS-9115-F, 2020\)** made payers (Medicare Advantage, Medicaid/CHIP, ACA exchange plans) publish a Patient Access API and a Provider Directory API.  
* **CMS Interoperability and Prior Authorization rule (CMS-0057-F, 2024\)** adds a Provider Access API, a Payer-to-Payer API and a Prior Authorization API, and adds prior-auth data to Patient Access. Its operational rules took effect January 1, 2026; the four FHIR APIs are due January 1, 2027 ([CMS fact sheet](https://www.cms.gov/newsroom/fact-sheets/cms-interoperability-prior-authorization-final-rule-cms-0057-f)).

**The IGs you will actually meet**

| IG | What it covers | Main resources |
| :---- | :---- | :---- |
| [US Core](https://hl7.org/fhir/us/core/) | Baseline clinical data (USCDI) for the US | Patient, Condition, Observation, MedicationRequest, Encounter |
| [CARIN Blue Button](https://hl7.org/fhir/us/carin-bb/) | Claims and EOB data for consumer apps (Patient Access) | ExplanationOfBenefit, Coverage |
| [Da Vinci PDex](https://hl7.org/fhir/us/davinci-pdex/) | Payer clinical data exchange, Provider Access, Payer-to-Payer | US Core resources, \$member-match, Bulk \$export |
| [Da Vinci Plan-Net](https://hl7.org/fhir/us/davinci-pdex-plan-net/) | Provider directories | Practitioner, PractitionerRole, Organization, InsurancePlan |
| [Da Vinci CRD](https://hl7.org/fhir/us/davinci-crd/) | Coverage requirements discovery inside the EHR | CDS Hooks \+ FHIR |
| [Da Vinci DTR](https://hl7.org/fhir/us/davinci-dtr/) | Documentation templates and rules | Questionnaire, QuestionnaireResponse, CQL |
| [Da Vinci PAS](https://hl7.org/fhir/us/davinci-pas/) | Prior authorization submission | Claim, ClaimResponse (Claim/\$submit) |
| [Bulk Data](https://hl7.org/fhir/uv/bulkdata/) | Population-level NDJSON export | \$export |

CRD, DTR and PAS together form the Prior Authorization API workflow; PDex and CARIN BB cover most of the payer data-sharing APIs.

**Exercises**

* ☐ Skim the US Core "Guidance" and "Must Support" pages; they explain rules you will be held to.  
* ☐ Read one CARIN BB ExplanationOfBenefit example end to end and map each part to a real claim concept (billed, allowed, paid, member liability).  
* ☐ Trace the prior auth flow: which IG is used at order time (CRD), to gather documentation (DTR), and to submit (PAS).

## **Capstone — A member health summary service (4–6 hours)**

Build one Spring Boot application that exercises every module: a facade server in front of your own data, fed and read by a HAPI client, validated against US Core and protected by SMART.

* ☐ **Load**: a CLI that reads a CSV of members and vitals and posts them as idempotent transactions (conditional create on member ID) to your local JPA server.  
* ☐ **Serve**: a facade that exposes Patient, Coverage and Observation (read \+ search by patient, code, date) backed by Postgres through Spring Data.  
* ☐ **Validate**: reject non–US Core resources on write with 422 and a useful OperationOutcome.  
* ☐ **Secure**: require a bearer token; map SMART v2 scopes (patient/Observation.rs, system/\*.rs) onto AuthorizationInterceptor rules.  
* ☐ **Operate**: implement Patient/{id}/\$summary returning a Bundle with the patient, active coverage and the last 5 vitals.  
* ☐ **Test**: integration tests with Testcontainers running hapiproject/hapi plus your service, driven by the HAPI generic client.

If you can build this comfortably, you can hold your own on any Java FHIR team.

## **Reference shelf**

* [FHIR R4 specification](https://hl7.org/fhir/R4/): the resource pages, [RESTful API](https://hl7.org/fhir/R4/http.html) and [Search](https://hl7.org/fhir/R4/search.html) chapters.  
* [HAPI FHIR documentation](https://hapifhir.io/hapi-fhir/docs/): client, plain server, validation, interceptors.  
* [SMART App Launch](https://hl7.org/fhir/smart-app-launch/) and the [SMART sandbox](https://launch.smarthealthit.org).  
* [chat.fhir.org](https://chat.fhir.org): the community Zulip. The \#hapi and \#implementers streams answer questions fast.  
* [Simplifier.net](https://simplifier.net) and [packages.fhir.org](https://packages.fhir.org): browse and download IG packages.  
* [CMS-0057-F fact sheet](https://www.cms.gov/newsroom/fact-sheets/cms-interoperability-prior-authorization-final-rule-cms-0057-f).

## **Appendix** {#appendix}

### **If-Match:**

`If-Match` is FHIR's optimistic locking: it tells the server to apply a write only if the resource is still at the version you last saw.

1. **The server versions every resource.** Each create or update bumps `meta.versionId`, and responses carry it in a header as `ETag: W/"1"`. The `W/` marks it as a weak ETag, which is the form FHIR uses for version ids.  
2. **You echo the version back.** On a PUT, you send `If-Match: W/"1"`, meaning "update this only if the current version is 1".  
3. **The server compares.** If the current version is still 1, the update goes through and the resource becomes version 2\. If someone else updated it in the meantime, the server rejects your request and changes nothing.

The header exists to prevent lost updates: two clients read version 1, both edit, both PUT. Without `If-Match`, the second write silently overwrites the first. With it, the second writer gets an error and has to re-read, merge, and retry. If you've used JPA's `@Version`, it's the same idea carried over HTTP.

### **vread:**

vread is "version read": it fetches one specific historical version of a resource instead of the current one. The URL is the normal read with `/_history/{versionId}` appended:

GET {{base}}/Patient/{{patientId}}/\_history/1

After your two updates, the Patient is at version 3\. A plain read (`GET {{base}}/Patient/{{patientId}}`) returns version 3, while the vread above returns the resource exactly as it was when you first created it, without the changes from either update. Check `meta.versionId` in the response to confirm which version you got.

The three related requests, side by side:

| Request | Returns |
| ----- | ----- |
| `GET .../Patient/{id}` | The current version |
| `GET .../Patient/{id}/_history/1` | Version 1 only (vread) |
| `GET .../Patient/{id}/_history` | A Bundle of all versions, newest first |

After you finish the exercise's delete step, try the vread again. It shows how FHIR treats deletion: the server keeps the history even though the current resource is gone.

