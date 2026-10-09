# HL7 FHIR Udemy Course Notes

Notes from the Udemy HL7 FHIR Mastery for Newbies and Pros course

**For medical records, the biggest issues are how to express complex elements and how to remove ambiguity**

Interoperability: How to interchange information and use it, via a shared language  
Levels of interoperability

1. Foundation: just be able to transfer data  
2. Structural: syntax, how the data is structured  
3. Meaning: semantic, medical terms to store info  
   1. SNOMED: med terms  
   2. LOINC: lab results  
   3. ICD: diagnosis  
   4. etc  
4. Organization: business rules, workflows, legal compliance

Standards allow data to be understood and utilized regardless of origin or destination. You need standard for:

1. Transport  
2. Privacy and security  
3. Content (structure and format)  
4. Identifiers  
5. Vocabulary and terminology

**Evolving standards:**  
EDI: electronic data interchange   
	Codes, mostly B2B  
70's, ad hoc, primitive, still in use by some shops  
X12: slightly better  
HL7 v2: Health Level 7\. more flexible, still primitive, outdated  
	XML doc  
	Has segments that contain fields, maintains backwards compatibility, can be expanded  
	Can be customized per business  
	XML schema to validate data  
	Hard to use, semantics not standard  
HL7 v3: failed UML-based Object oriented attempt  
	XML doc  
	Fixes ambiguities in v2, standardized framework  
	RIM: Reference Information Model: entity-role-participation-act  
	Still way too complex  
CDA: Clinical Document Architecture: XML-based, US-based, still in use  
	Came out of HL7  
	Markup standards for clinical docs:  
		Persistent  
		Stewardship  
		Authentication  
		Context  
		Wholeness  
	Header (metadata)  
	Body (clinical content in sections)  
	Levels:

1. header \+ pdf  
2. Like HTML  
3. templates, whole doc can be digitized

C-CDA: Consolidated CDA: XML-based, US-based, widely adopted  
A cda doc consists of a header and body.   
Lots of versions  
Header: metadata for classification and management  
Body: actual clinical doc, generic, constrained by versioned templates, e.g. Medication

Can you locate the section in the C-CDA IG that talks about the difference between open and closed templates in the C-CDA specification?   
TOC \> Represenation of discrete data  
[https\://build.fhir.org/ig/HL7/CDA-ccda-2.1-sd/representation\_of\_discrete\_data.html](https://build.fhir.org/ig/HL7/CDA-ccda-2.1-sd/representation_of_discrete_data.html)  
Clinical data has be digitized.  
Templates constrain the clinical data, to facilitate digitizing.  
Open templates: Can be extended, per the needs of the setting, provider, patient care. Most templates are open.  
Closed templates: In C-CDA, only Estimated Date of Delivery and Medication Free Text Sig are closed. CDA has additional closed templates.

Could you explain how these templates affect the structure and flexibility of clinical documents?  
Open templates provide flexibility and reusability, so that providers can customize for setting, provider, and patient care.  
Flexibility means that anything in the spec can be included in any open template  
Closed templates restrict data to just what the template allows, for absolute predictability and zero variance.

OpenEHR: Open Electronic Health Record  
	HL7: To share clinical data  
	OpenEHR: How data is modeled and stored. Focus on persistence and structure  
	HL7 and OEHR share a lot, and overlap somewhat.   
	Modular architecture with core building blocks  
	Vendor-neutral, Technology-agnostic. Flexible and fits into many healthcare systems  
	2-level modeling approach (dual model): separation of general structure of health data from the clinical knowledge it represents

1. Reference model

   Basic objects and datatypes, and how they relate to each other  
   Simple, stable, foundational, defines classes and datatypes, no clinical content

2. Archetype model  
   Clinical concepts, structured and standardized, using archetypes and templates, for precise clinical meaning

	This addresses a common failing of single-model designs where clinical data is embedded in the db design,   
leading to interoperability problems.  
Semantic interoperability is achieved via archetypes, providing a consistent way to define and represent clinical concepts  
ADL: archetype model language, used by non-programmer SMEs  
Example archetypes: medication, blood pressure, etc  
Archetypes can be evolved as needed  
Each describes a piece of clinical data and the processes related to a particular medical use case.  
Archetype structure:

1. Description  
2. Definition  
3. Ontology  
4. Revision history

	Templates are compiled into an operation template (OPT) that can be combined with other templates to   
completely describe the components used at runtime, which is valuable for developers that may not be familiar  with the clinical picture.  
The OpenEHR standard is transparent for everyone \- nothing is proprietary.  
Clinicians and domain experts design new templates, and developers use them.  
OpenEHR builds record systems that are

1. Meaningful  
2. Structured  
3. Easily searchable

	Still, it requires expertise which is often lacking in the industry, and adopting it can be expensive since the entire   
	healthcare infrastructure has to be adapted.

HL7 FHIR Widely adopted worldwide  
	Fast Healthcare Interoperability Resources  
	This is also an open standard, built on REST APIs and JSON  
	Resources For Healthcare (RFH) 2010  
		Granular clinical concepts  
		Community wanted cloud, mobile, web integration, and integration via JSON (like facebook apps)  
		A resource can stand alone, or be combined with others for complex clinical data

1. RFH spec uses the vocabulary model described in core prinicples  
2. Modeling is based on RIM and maps to it (Regulatory Information Mgmt, which track clinical data for compliance)  
3. Based on CMETs and R-MIMs from HL7 v3  
   1. CMETS: healthcare training and medical education  
   2. R-MIMs: Refined Medical Information Model is part of CDA for interoperability  
4. Conformance model started with HL7, but changed to XML  
5. Simplifies the HL7 XML  
6. Datatypes are simple versions of HL7 v3  
7. CDA concept: text as a fallback for human comprehension

	RFH was renamed to FHIR  
	FHIR modelling differs from HL7 v2 and v3 in that it uses a composition model, and is resource-based, extensible, and flexible  
	Resources are linked together by references. In most real world use cases, multiple resources are combined and constrained as needed.  
	Core FHIR:

1. A set of specifications for healthcare data exchange  
2. What data is shared, how it is shared, and how it should be managed  
3. Supports multiple exchange paradigms:   
   1. REST  
   2. Messaging  
   3. Documents  
   4. Services  
4. Easy to understand and use, with support ecosystem

Note: Structural validation is the first and easiest step in JSON schema validation of FHIR. Several of the Java-based validators on the JSON-schema website are built on my JSON-Java lib.

FHIR Specification  
Browse to [https\://hl7.org/fhir/](https://hl7.org/fhir/) to view the spec.  
Newcomers should start with teh First Time Here pane, but we are going to explore other areas. Start with the Documentation top menu tab (Base Documentation)

1. Doc: Read all of the Background sections	  
2. Doc: JSON Format section  
3. Doc: Any section with an N annotation \= Normative, unlikely to change. TU \= Trial Use, experimental

Next, look at the Resource Types top menu tab. Pick any resource type that looks interesting.   
Annotations: number=maturity level, 0-6 (6=N)  
They all have a common format:

1. Header tab: links to related pages  
2. Scope/Boundaries/Backgound sections: How the resource is used in a clinical or business context  
3. References: How this resource is used with or by other resources  
4. Context: resource internals, elements and relationships  
5. Terminology bindings  
6. Search params

	Next, look at the Examples tope menu tab. This will show how others have used the resource.

Next DataTypes top menu tab. Every element in a resource is defined by a datatype.

FHIR Resource Types top menu item  
	These are the fundamental building blocks of FHIR. They consist of:

1. Unique id  
2. Metadata  
3. Other elements  
   	Uses inheritance model: 

   Base class: no elements or restriction, just a marker)  
   Resource class: id, meta, implicit rules, language)  
   DomainResource: text, contained, extension, modifierExtension  
   Most resources are built on DomainResource and grouped into modules like Admin, Clinical, Medications, etc (about a dozen). Some resources inherit directly from Resource, and they don't support any DomainResource properties. Ex: Bundle, Parameters, Binary  
   The Guide to Resources page helps you decide which resource to use, depending on your context.  
   Logical View of Resource Type: Structure menu tab  
   	Name: If the element name ends with X, then it is a union with multiple types  
   	Flags: extra rules, characteristics: N, TU, Modifer, MustSupport  
   	Card: cardinality  
   	Type: datatype of the resource element. Might be primitive, complex or user-defined.  
   	Description & Constraints: AKA Invariants. Business rules and constraints. There is also an indication of binding strength (e.g. preferred), regarding how strictly the resource must follow the vocabulary. Click the question mark icon in the upper right to see the resource definitions page for more info. This is worth reading\!  
     
   The JSON menu tab is also helpful, but is highly opaque to beginnners.  
   	

   FHIR Resource Creation Challenge Tips

   	Recall that each Resource Type and Data Type includes examples of how they are used.  Ex: Bundle. We will look in Examples for a batch or transaction bundle. Batch bundles are processed one-at-a-time. One can fail, but the others may succeed. Transaction bundles are all-or-none. Ex: bundle-request-transaction-complex (not found\!)

   	Or you can use the HAPI FHIR server. Select a Patient Resource, click search. THis will bring up some instances. Search for Observations, and identify a patient id. Then use that id to retrieve the patient info as a bundle. Select the Include and Reverse include options. Select the \_id for search, and enter the patient id found in subject \> reference \> Patient/(this value). This will return a bundle with a boatload of resources.

   

   FHIR Data Types

   	Contained Resources: Domain Resource has a property Contained, with a Resource value. Lets you embed one Resource inside another. Mostly useful when the nested Resource is just a tiny bit of information, when upgrading from a earlier version. This should only be done when the equivalent standalone Resource cant be created from the original data. There are constraints and limitations though.

   	Base class: Like Object, but has no elements or restriction.

   	Element class: extends Base. Has id and extension properties

   	DataType class: extends Element. Base for all data types. Main subclasses are: PrimitiveType and BackboneType (has a modifierExtension property) 

1. PrimitiveType: For primitives. Has a single simple property. Derived classes are mostly simple classes but can contain multiple attribututes. In JSON, this is represented with a \_ prefix. Always written lowercase  
2. BackboneType: For complex types. Can contain multiple properties. Always written TitleCase

	FHIR Extensibility Mechanism: Via DomainResource  and Element classes' extension property. Extensions can be nested indefinitely. In JSON, extensions have an \_prefix as the key value. Not frequently used in core FHIR, but often found in conversions from other standards. First check if the Observation resource has the type you need. Modifer extensions are for items e.g. Do Not Perform, due to a patient condition. There are no modifier extensions in the core FHIR.  
		  
FHIR S	earch Capabilities.   
	FHIR defines data exchange types with Paradigms (REST, etc). REST is most widely used. Interactions are used to access and manage resources. FHIR servers provide a CapabilityStatement which outlines what the server can do. Use a capabilities request to find out. FHIR provides 3 levels:

1. Resource instances  
2. Type level for a resource  
3. System level

	FHIR defines special HTTP headers for conditional requests, that are processed based on the outcomes of the conditional headers. EG conditional create   
	FHIR has system level interactions for batch and transaction for the usual reasons.  
FHIR Search Parameters. Search is a big deal, basically a sublanguage

1. They can be complex with specifying search params. SOme params are optional (curly brace), some are required (square bracket\].Search param types may be token, string, etc.   
2. Modifiers alow you to change a param's default behavior. ex: :missing, :contains, etc.   
3. Prefixes can adjust behavior of ordered types: eq, gt, etc  
4. Common params: Resources specify a list of allowed search params.  
5. Search result modifiers: \_contained, \_count, etc

FHIR Advanced Search Features  
	You often have to combine multiple search params to get the result. Frankly, you can just brute force all of these with multiple searches and processing the results. Complex queries are starting to look like SQL queries.  
	Chaining: Follow relationships between resources, especially if linked.  
	Reverse Chaining: Find instances of a resource by applying criteria to other resources that link back to the searched resource. Uses the \_has search param. e.g. find the patients with a specific condition that have a specified practitioner. Many FHIR servers DO NOT support reverse chaining.  
	Link Resources forward and back: 

1. Forward: Add related resource types to the results. Uses \_include and \_revinclude and \* wildcard char. Can raise security and performance concerns, so FHIR server may reject the request. The iterate modifier may be used also.   
2. Back: Inverse of forward. Also has security and performance issues. 

FHIR Resource Validation  
	How to be sure the instance you retrieved is a valid resource. Why does it even matter? For interoperability, maintain data quality, catch issues early. What to validate? Syntax of the resource, expected structure, terminology (valid codes, etc), references, and custom business rules.

1. Design validation: Writing a resource: download json schema (fhir.schema.json) configure vscode or Intellij to use the schema (look for tutorials lol) Intellij has helper plugins as well.  
2. Instance validation: After importing a resource from a 3rd party: download and use a hl7 validator or use the online one. Use an offline validator if you have PII in the resource.  
3. Application validation: testing the app: AEGIS Touchstone platform. for testing healthcare systems. look it up online

	Test  your app with a large number of resources, even up to millions. Generate synthetic (deidentified or anonymized) data. Synthea does this. 

FHIR Terminologies: Coded information	  
	Ensure semantic consistency, especialliy across healthcare systems. Terms:

1. Vocabulary: collection of encoded data  
2. Concept domain: named set of concepts  
3. Code system: standardized concepts or symbols (SNOMED CT, LOINC, etc). Version-dependent  
4. Value set: list of valid concepts to use in a context

	Binding: datatypes the involve coding elements. Binding strength indicates how to apply: Required, Extensible, Preferred. Can get difficult when porting or upgrading versions.

Related standards:  
	SMART on FHIR extends for different systems. OpenID/OAuth2.0 JWT  
	CDS Hooks: integrate using webhooks for EHR, etc  
	IHE: improve info exchange, using IHE MObile profiles  
	OMOP: data standardization to analyze large scale data

FHIR in practice:  
	IG profiles  
	IHE

	

	

