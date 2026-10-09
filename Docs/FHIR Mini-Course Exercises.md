# FHIR Mini-Course Exercises

Module 1:  
**Exercises**

- [x] Read the Patient, Observation and Encounter pages in the R4 spec. For each, note the required elements (cardinality 1..1) and the "must support" flags. Must Support is in the [US Core IG](https://build.fhir.org/ig/HL7/US-Core/): There are many Observation profiles, pick one. **Table of Contents \> Profiles\&Extensions \> Observation \> Simple Observation \> Must Support**.   
- [x] Open the Observation examples tab and find one each of valueQuantity, valueCodeableConcept and component (blood pressure).  
           "valueQuantity": {  
              "value": 107,  
              "unit": "mmHg",  
              "system": "http\://unitsofmeasure.org",  
              "code": "mm\[Hg\]"

      },

 "component": \[  
    { (truncated for length)  
**Observation-example-f206-staphylococcus.json (not found in bp example)**  
 "valueCodeableConcept": {  
    "coding": \[      {  
        "system": "[http\://snomed.info/sct](http://snomed.info/sct)",  
        "code": "3092008",  
        "display": "Staphylococcus aureus"  
      }    \]  },

- [x] Write a Patient JSON by hand with two identifiers, a name, gender, birth date and one extension. You will POST it in Module 2\.

{ "resourceType": "Patient", "identifier": \[ { "use": "official", "value": "abc" }, { "system": "def" } \], "name": \[ { "family": "Duck" }, { "given": \[ "Donald" \] } \], "gender": "male", "birthDate": "1980-01-01", "extension": \[ { "url": "http\://johnjleary.com", "\_valueBoolean": true } \] }

Module 2:

**Exercises (see Postman  localserver collection)**

- [x] Duplicate the Capabilities request for the hapi fhir server:  
      [`https://hapi.fhir.org/baseR4/metadata`](https://hapi.fhir.org/baseR4)  
      Use JSONPath to isolate Observation and compare between local and hapifhir. Note the interactions are the same, but there are additional search params for the hapi fhir server (leading \_, maybe others?)  
      The remaining exercises are only for the local server (hapifhir gets wiped periodically)

- [x] Create, read, update twice, vread version 1, then delete a Patient.   
      (Bump `If-Match` to `W/"2"` for the second update.)  
      Read it after delete and note the 410 GONE status code.  
      vread v1 or v2 and confirm the historic version is still there.  
      See [Appendix](https://docs.google.com/document/d/1ANyUdsWTjJvD1tB1QwQtYS_E76MbbCheKJMWoiZS3OA/edit?tab=t.0#heading=h.m5kfz4p910m3) for info about vread (version read)

- [x] Trigger three different errors and confirm the OperationOutcome:  
      bad JSON: 		400 Bad Request (send an invalid birthday value)  
      	Just adding a random key/value is allowed/ignored by lenient servers.  
      unknown element:	200 but with issue.severity value 'error'. You have  
      	to postpend /\$validate to the URL to see the issue, and \$validate in   
      	itself prevents the update from occurring.  
      stale `If-Match`:	409 Conflict (on update of a deleted record)

- [x] Create an Observation that references your Patient. (Use `"subject": {"reference": "Patient/{{patientId}}"}`.)

Module 3:

**Exercises** (against `https://hapi.fhir.org/baseR4`, which has plenty of data)  (See Postman hapifhir collection)

- [x] Find female patients born before 1960 whose family name starts with "Sm".  
      /Patient?gender=female\&family=Sm\&birthdate=lt1960

- [x] Find all heart-rate Observations over 100/min, newest first, 10 per page, and walk three pages by following `next` links.  
      \# there were only 18, so 2 pages returned  
      /Observation?code=[http\://loinc.org%7c8867-4](http://loinc.org%7c8867-4)\&value-quantity=gt100&\_count=10&\_sort=-date

- [x] Fetch one patient with all of their Conditions and Encounters in a single request.

      \# just get a random patient, but make it repeatable. May not find any conditions or encounters   
      /Patient?\_sort=\_id&\_count=1&\_revinclude=Condition:patient&\_revinclude=Encounter:patient

      \# out of scope for this lesson: ensure at least 1 condition\&encounter  
      /Patient?\_has:Condition:patient&\_has:Encounter:patient&\_sort=\_id&\_count=1&\_revinclude=Condition:patient&\_revinclude=Encounter:patient

- [x] Find Patients who have at least one MedicationRequest, using *`has`.*

      \# status=active ensures indirectly that a patient with at least one mediationRequest is found  
      /Patient?\_has:MedicationRequest:patient:status=active&\_count=1&\_sort=\_id&\_revinclude=MedicationRequest:patient

- [x] Pass an unsupported search parameter and see how the server reports it.

	Results in 400 Bad Request. HTTP header Prefer: handling=lenient does not change the outcome.