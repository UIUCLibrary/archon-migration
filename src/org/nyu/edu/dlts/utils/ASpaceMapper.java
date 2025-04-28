package org.nyu.edu.dlts.utils;

import org.apache.commons.lang.WordUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.apache.commons.text.similarity.JaroWinklerSimilarity;


import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

/**
 * Created by IntelliJ IDEA.
 * User: nathan
 * Date: 9/5/12
 * Time: 1:41 PM
 *
 * Class to map AT data model to ASPace JSON data model
 */
public class ASpaceMapper {
    // String used when mapping access class to groups
    public static final String ACCESS_CLASS_PREFIX = "_AccessClass_";

    // The utility class used to map to ASpace Enums
    private ASpaceEnumUtil enumUtil = new ASpaceEnumUtil();

    // used to map vocabularies to ASpace vocabularies
    public String vocabularyURI = "/vocabularies/1";

    // these store the ids of all accessions, resources, and digital objects loaded so we can
    // check for uniqueness before copying them to the ASpace backend
    private HashSet<String> digitalObjectIDs = new HashSet<String>();
    private HashSet<String> accessionIDs = new HashSet<String>();
    private HashSet<String> resourceIDs = new HashSet<String>();
    private HashSet<String> eadIDs = new HashSet<String>();

    private final String[] aSpaceExtents = enumUtil.getAllASpaceExtentTypes();

    // variable to keep track of filenames and their ids to make sure we have unique names
    private HashSet<String> digitalObjectFilenames = new HashSet<String>();
    private HashMap<String, String> fileIDsToFilenamesMap = new HashMap<String, String>();

    // some code used for testing
    private boolean makeUnique = false;

    // initialize the random string generators for use when unique ids are needed
    private RandomString randomString = new RandomString(3);
    private RandomString randomStringLong = new RandomString(6);

    // used to store errors
    private ASpaceCopyUtil aspaceCopyUtil;

    // used when generating errors
    private String currentCollectionRecordIdentifier;

    // date formatter used to convert date string to date object
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd");

    // booleans used to convert some bbcode to html or blanks
    private boolean bbcodeToHTML = false;
    private boolean bbcodeToBlank = true;

    // boolean to specify whether to publish the notes
    private Boolean publishRecord = false;

    // variable to store the base uri for digital objects
    private String digitalObjectBaseURI = "";

    //for appending to ids to aid in testing
    private String appendTestIdentifier = "";

    /**
     *  Main constructor
     */
    public ASpaceMapper() { }

    /**
     * Constructor that takes an aspace copy util object
     * @param aspaceCopyUtil
     */
    public ASpaceMapper(ASpaceCopyUtil aspaceCopyUtil) {
        this.aspaceCopyUtil = aspaceCopyUtil;
    }

    /**
     * Set the option for either converting some bbcode to html
     *
     * @param option
     */
    public void setBBCodeOption(String option) {
        if (option.equals("-bbcode_html")) {
            bbcodeToHTML = true;
            bbcodeToBlank = false;
        } else {
            bbcodeToHTML = false;
            bbcodeToBlank = true;
        }
    }

    /**
     * Method to set the hash map that holds the dynamic enums
     *
     * @param dynamicEnums
     */
    public void setASpaceDynamicEnums(HashMap<String, JSONObject> dynamicEnums) {
        enumUtil.setASpaceDynamicEnums(dynamicEnums);
    }

    /**
     * Method to set the base URI for digital objects
     *
     * @param baseURI
     */
    public void setDigitalObjectBaseURI(String baseURI) {
        digitalObjectBaseURI = baseURI;
    }

    /**
     * Method to set a string to create unique identifiers for testing
     *
     * @param testString
     */
    public void setAppendTestIdentifier(String testString) {
        appendTestIdentifier = "_" + testString;
    }

    /**
     * Method to return the enum util
     * @return
     */
    public ASpaceEnumUtil getEnumUtil() {
        return enumUtil;
    }

    /**
     * This method is used to map AR lookup list values into a dynamic enum.
     *
     * @param enumList
     * @return
     */
    public ArrayList<JSONObject> mapEnumList(JSONObject enumList, String endpoint) throws Exception {
        // first we get the correct dynamic enum based on list. If it null then we just return null
        ArrayList<JSONObject> dynamicEnums = enumUtil.getDynamicEnum(endpoint);
        ArrayList<JSONObject> dynamicEnumsUpdated = new ArrayList<JSONObject>();

        for (JSONObject dynamicEnumJS : dynamicEnums) {

            if (dynamicEnumJS == null) return dynamicEnumsUpdated;

            // add the values return from aspace enum an arraylist to make lookup easier
            JSONArray valuesJA = dynamicEnumJS.getJSONArray("values");
            ArrayList<String> valuesList = new ArrayList<String>();
            for (int i = 0; i < valuesJA.length(); i++) {
                valuesList.add(valuesJA.getString(i));
            }

            // now see if all the archon values are in the ASpace list already
            // if they are not then add them
            String valueKey = dynamicEnumJS.getString("valueKey");
            String idPrefix = dynamicEnumJS.getString("idPrefix");

            boolean toLowerCase = true;
            if (dynamicEnumJS.has("keepValueCase")) {
                toLowerCase = false;
            }

            int count = 0;
            Iterator<String> keys = enumList.keys();
            while (keys.hasNext()) {
                JSONObject enumJS = enumList.getJSONObject(keys.next());
                String value = enumJS.getString(valueKey);

                // most values in ASpace are lower case so normalize
                if (toLowerCase) {
                    value = value.toLowerCase();
                }

                // some values have spaces which space normally uses underscore for
                value = value.replace(" ", "_");

                // map the id to value
                String id = idPrefix + "_" + enumJS.get("ID");
                enumUtil.addIdAndValueToEnumList(id, value);
                if (idPrefix.equals("container_types")) {
                    aspaceCopyUtil.addContainerTypeValueToIDMapping(value, enumJS.getString("ID"));
                }

                // see if to add this to aspace
                if (!valuesList.contains(value)) {
                    valuesJA.put(value);
                    count++;
                    System.out.println("Adding value " + value);
                }
            }

            // need to add other to extent unit type enum list
            if (endpoint.contains("extentunits")) {
                valuesJA.put(ASpaceEnumUtil.UNMAPPED);
            }


            if (count != 0) {
                dynamicEnumsUpdated.add(dynamicEnumJS);
            }
        }
        return dynamicEnumsUpdated;
    }

    /**
     * Method to get the corporate agent object from a repository
     *
     * @param repository
     * @return
     */
    public String getCorporateAgent(JSONObject repository) throws JSONException {
        // Main json object, agent_person.rb schema
        JSONObject agentJS = new JSONObject();
        agentJS.put("agent_type", "agent_corporate_entity");

        // hold name information
        JSONArray namesJA = new JSONArray();
        JSONObject namesJS = new JSONObject();

        //add the contact information
        JSONArray contactsJA = new JSONArray();
        JSONObject contactsJS = new JSONObject();

        contactsJS.put("name", repository.get("Name"));
        contactsJS.put("address_1", repository.get("Address"));
        contactsJS.put("address_2", repository.get("Address2"));
        contactsJS.put("city", repository.get("City"));
        contactsJS.put("region", repository.get("State"));

        // add the country and country code together
        contactsJS.put("country", enumUtil.getASpaceCountryCode(repository.getInt("CountryID")));

        String postCode = repository.getString("ZIPCode") + "";
        if (!repository.getString("ZIPPlusFour").isEmpty()) postCode += "-" + repository.get("ZIPPlusFour");
        contactsJS.put("post_code", postCode);

        addPhoneNumbers(contactsJS, repository.getString("Phone"), repository.getString("PhoneExtension"),
                repository.getString("Fax"));

        contactsJS.put("email", repository.get("Email"));
        contactsJS.put("email_signature", repository.get("EmailSignature"));

        contactsJA.put(contactsJS);
        agentJS.put("agent_contacts", contactsJA);

        // add the names object
        String primaryName = repository.getString("Name");
        namesJS.put("source", "local");
        namesJS.put("primary_name", primaryName);
        namesJS.put("sort_name", primaryName);

        namesJA.put(namesJS);
        agentJS.put("names", namesJA);

        return agentJS.toString();
    }

    /**
     * Method to add the telephone and fax numbers to an agent contact information
     *
     * @param contactsJS
     * @param telephone
     * @param fax
     */
    private void addPhoneNumbers(JSONObject contactsJS, String telephone, String ext, String fax) throws JSONException {
        JSONArray telephonesJA = new JSONArray();

        if (telephone != null && !telephone.isEmpty()) {
            JSONObject phoneJS = new JSONObject();
            phoneJS.put("number", telephone);
            phoneJS.put("ext", ext);
            phoneJS.put("number_type", "business");
            telephonesJA.put(phoneJS);
        }

        if (fax != null && !fax.isEmpty()) {
            JSONObject phoneJS = new JSONObject();
            phoneJS.put("number", fax);
            phoneJS.put("number_type", "fax");
            telephonesJA.put(phoneJS);
        }

        contactsJS.put("telephones", telephonesJA);
    }

    /**
     * Method to convert an Archon repository record
     *
     * @param record
     * @return
     * @throws Exception
     */
    public String convertRepository(JSONObject record, String agentURI) throws Exception {
        // Main json object
        JSONObject json = new JSONObject();

        // add the Archon database Id as an external ID
        addExternalId(record, json, "repository");

        // get the repo code
        json.put("repo_code", record.get("Name"));
        json.put("name", fixEmptyString(record.getString("Name")));
        json.put("org_code", record.get("Code"));
        json.put("url", fixUrl(record.getString("URL")));
        json.put("publish", true);
        json.put("country", enumUtil.getASpaceCountryCode(record.getInt("CountryID")));

        if(agentURI != null) {
            json.put("agent_representation", getReferenceObject(agentURI));
        }

        return json.toString();
    }

    /**
     * Method to convert an AT subject record to
     *
     * @param record
     * @return
     * @throws Exception
     */
    public String convertUser(JSONObject record) throws Exception {
        // Main json object
        JSONObject json = new JSONObject();

        // add the AT database Id as an external ID
        addExternalId(record, json, "user");

        // get the username replacing spaces with underscores
        String username = record.getString("Login");
        json.put("username", username);

        // get the full name
        String name = fixEmptyString(record.getString("DisplayName"), "full name not entered");

        json.put("name", name);
        json.put("first_name", record.get("FirstName"));
        json.put("last_name", record.get("LastName"));
        json.put("email", record.get("Email"));

        return json.toString();
    }

    /**
     * Method to convert a subject record
     *
     * @param record
     * @return
     * @throws Exception
     */
    public JSONObject convertSubject(JSONObject record) throws Exception {
        // Main json object
        JSONObject json = new JSONObject();

        // add the AT database Id as an external ID
        addExternalId(record, json, "subject");

        json.put("vocabulary", vocabularyURI);

        json.put("publish", true);

        json.put("source", enumUtil.getASpaceSubjectSource(record.getInt("SubjectSourceID")));

        // add the terms
        JSONArray termsJA = new JSONArray();
        try {
            addSubjectTerms(record, termsJA);
            json.put("terms", termsJA);
        } catch(Exception e) {
            String message = "Invalid subject terms for " + record + "\n";
            aspaceCopyUtil.addErrorMessage(message);
            return null;
        }

        return json;
    }

    /**
     * Method to add terms to a subject
     *
     * @param record
     * @param termsJA
     * @throws Exception
     */
    private void addSubjectTerms(JSONObject record, JSONArray termsJA) throws Exception {
        if(record.get("Parent") != null && record.getInt("ParentID") != 0) {
            addSubjectTerms(record.getJSONObject("Parent"), termsJA);
        }

        JSONObject termJS = new JSONObject();
        termJS.put("term", record.get("Subject"));
        termJS.put("term_type", enumUtil.getASpaceTermType(record.getInt("SubjectTypeID")));
        termJS.put("vocabulary", vocabularyURI);
        termsJA.put(termJS);
    }

    /**
     * Method to convert a name aka creator record to an ASpace agent record
     *
     * @param record
     * @param creatorTypeId
     * @return
     * @throws Exception
     */
    public JSONObject convertCreator(JSONObject record, int creatorTypeId) throws Exception {
         // Main json object
        JSONObject agentJS = new JSONObject();

        // add the AR database Id as an external ID
        addExternalId(record, agentJS, "creator");

        agentJS.put("vocabulary", vocabularyURI);

        agentJS.put("publish", true);

        // hold name information
        JSONArray namesJA = new JSONArray();
        JSONObject namesJS = new JSONObject();

        // add the biog-history note to the agent object
        if(record.has("BiogHist") && !record.getString("BiogHist").isEmpty()) {
            addBiologicalHistoryNote(agentJS, record, creatorTypeId);
        }

        // add the agent date
        if(record.has("Dates") && !record.getString("Dates").isEmpty()) {
            JSONArray datesJA = new JSONArray();
            JSONObject dateJS = new JSONObject();
            dateJS.put("date_type", "single");
            dateJS.put("label", "existence");
            dateJS.put("expression", record.get("Dates"));
            datesJA.put(dateJS);
            agentJS.put("dates_of_existence", datesJA);
        }

        // add the source for the name
        if (record.has("SubjectSourceID")) {
            namesJS.put("source", enumUtil.getASpaceNameSourceForSubject(record.getInt("SubjectSourceID")));
        } else namesJS.put("source", enumUtil.getASpaceNameSource(record.getInt("CreatorSourceID")));

        // add basic information to the names record
        String sortName = record.getString("Name");
        namesJS.put("sort_name", sortName);
        namesJS.put("name_order", "direct");

        switch (creatorTypeId) {
            case 19:
            case 21:
            case 23:
                agentJS.put("agent_type", "agent_person");
                namesJS.put("primary_name", sortName);

                if(record.has("NameFullerForm")) {
                    namesJS.put("fuller_form", record.get("NameFullerForm"));
                }
                break;
            case 20:
                agentJS.put("agent_type", "agent_family");
                namesJS.put("family_name", sortName);
                break;
            case 22:
                agentJS.put("agent_type", "agent_corporate_entity");
                namesJS.put("primary_name", sortName);
                break;
            default:
                String message = sortName + ":: Unknown name type: " + creatorTypeId + "\n";
                aspaceCopyUtil.addErrorMessage(message);
                return null;
        }

        // add the names array and names json objects to main record, including any alternative name
        namesJA.put(namesJS);

        if(record.has("NameVariants") && !record.getString("NameVariants").isEmpty()) {
            String nameVariant = record.getString("NameVariants");

            JSONObject namesVariantJS = new JSONObject();

            namesVariantJS.put("name_order", "direct");
            namesVariantJS.put("source", namesJS.get("source"));
            namesVariantJS.put("sort_name", nameVariant);

            if(namesJS.has("primary_name")) {
                namesVariantJS.put("primary_name", nameVariant);
            } else {
                namesVariantJS.put("family_name", nameVariant);
            }

            namesJA.put(namesVariantJS);
        }

        agentJS.put("names", namesJA);

        return agentJS;
    }

    /**
     * Method to convert the donor information in an accession record to
     * an agent
     *
     * @param record the accession record
     * @return
     * @throws Exception
     */
    public JSONObject convertAccessionDonor(JSONObject record) throws Exception {
         // Main json object
        JSONObject agentJS = new JSONObject();

        // add the AR database Id as an external ID
        addExternalId(record, agentJS, "donor");

        agentJS.put("vocabulary", vocabularyURI);
        agentJS.put("agent_type", "agent_person");
        agentJS.put("publish", false);

        // hold name information
        JSONArray namesJA = new JSONArray();
        JSONObject namesJS = new JSONObject();

        // add the source for the name
        namesJS.put("source", "local");

        // add basic information to the names record
        String sortName = record.getString("Donor");
        namesJS.put("sort_name", sortName);
        namesJS.put("name_order", "direct");

        agentJS.put("agent_type", "agent_person");
        namesJS.put("primary_name", sortName);

        // add the names array and names json objects to main record
        namesJA.put(namesJS);
        agentJS.put("names", namesJA);

        // add the contact information
        if(record.has("DonorContactInformation")) {
            JSONArray contactsJA = new JSONArray();
            JSONObject contactsJS = new JSONObject();

            contactsJS.put("name", sortName);
            contactsJS.put("address_1", record.get("DonorContactInformation"));
            contactsJS.put("note", record.get("DonorNotes"));

            contactsJA.put(contactsJS);
            agentJS.put("agent_contacts", contactsJA);
        }

        return agentJS;
    }

    /**
     * Method to convert the classification record
     *
     * @param record
     * @return
     * @throws Exception
     */
    public JSONObject convertClassification(JSONObject record) throws Exception {
        // Main json object
        JSONObject json = new JSONObject();

        // set the model type
        if(record.getString("ParentID").equals("0")) {
            json.put("jsonmodel_type", "classification");
        } else {
            json.put("jsonmodel_type", "classification_term");
            /*TODO 10/8/2015 Below code causes bug in ASpace v1.4.0*/
            //json.put("position", record.getInt("Position"));
        }

        json.put("identifier", record.get("ClassificationIdentifier"));
        json.put("title", record.get("Title"));
        json.put("description", record.get("Description"));

        return json;
    }

    /**
     * Method to convert an accession record to json ASpace JSON
     *
     * @param record
     * @return
     * @throws Exception
     */
    public JSONObject convertAccession(JSONObject record) throws Exception {
        // Main json object
        JSONObject json = new JSONObject();

        // add the AR database Id as an external ID
        addExternalId(record, json, "accession");

        json.put("publish", convertToBoolean(record.getInt("Enabled")));

        // check to make sure we have a title
        String title = fixEmptyString(record.getString("Title"), null);

        String id_0 = record.getString("Identifier");
        String id_1 = getUniqueID(ASpaceClient.ACCESSION_ENDPOINT, id_0, null, title);

        if (makeUnique) {
            id_0 = randomStringLong.nextString();
        }

        if(!appendTestIdentifier.isEmpty()){
            id_0 += appendTestIdentifier;
        }

        Date date = getDate(record.getString("AccessionDate"));

        if (date == null) {
            // use the default date of 01/01/9999
            date = getDate("99990101");

            // add an error message about this
            String message = "Invalid Accession Date for" + id_0 + "\n";
            aspaceCopyUtil.addErrorMessage(message);
        }

        json.put("title", title);

        json.put("accession_date", date);

        json.put("id_0", id_0);
        json.put("id_1", id_1); // This is only used to make sure the ids are unique

        json.put("content_description", record.get("ScopeContent"));

        json.put("condition_description", record.get("PhysicalDescription"));

        json.put("general_note", record.get("Comments"));

        if(record.has("MaterialTypeID")) {
            json.put("resource_type", enumUtil.getASpaceResourceType(record.getString("MaterialTypeID")));
        }

        /* add linked records (extents, dates, rights statement)*/

        // add the extent array containing one object or many depending if we using multiple extents
        if(record.has("ReceivedExtent") && record.getDouble("ReceivedExtent") != 0) {
            JSONArray extentJA = new JSONArray();
            JSONObject extentJS = new JSONObject();

            extentJS.put("extent_type", enumUtil.getASpaceExtentType(record.getInt("ReceivedExtentUnitID")));
            extentJS.put("number", record.getString("ReceivedExtent"));
            extentJS.put("portion", "whole");
            extentJS.put("container_summary", "Received Extent");

            extentJA.put(extentJS);
            json.put("extents", extentJA);
        }

        // add the inclusive dates
        addDate(record.getString("InclusiveDates"), json, "inclusive", "other");

        // add the collection management record now
        if(record.has("ExpectedCompletionDate") && !record.getString("ExpectedCompletionDate").isEmpty()) {
            addCollectionManagementRecord(record, json);
        }

        /*

        json.put("suppressed", record.getInternalOnly());

        json.put("acquisition_type", enumUtil.getASpaceAcquisitionType(record.getAcquisitionType()));

        json.put("resource_type", enumUtil.getASpaceAccessionResourceType(record.getResourceType()));

        json.put("restrictions_apply", record.getRestrictionsApply());

        json.put("retention_rule", record.getRetentionRule());

        json.put("general_note", record.getGeneralAccessionNote());

        json.put("access_restrictions", record.getAccessRestrictions());

        json.put("access_restrictions_note", record.getAccessRestrictionsNote());

        json.put("use_restrictions_note", record.getUseRestrictionsNote());

        json.put("use_restrictions", record.getUseRestrictions());
        */

        return json;
    }

    /**
     * Method to return a collection management record object from an accession
     *
     * @param record
     * @param recordJS
     * @return
     * @throws Exception
     */
    public void addCollectionManagementRecord(JSONObject record, JSONObject recordJS) throws Exception {
        // Main json object
        JSONObject json = new JSONObject();

        json.put("processing_plan", "Expected Completion Date: " + record.get("ExpectedCompletionDate"));

        if (record.has("ProcessingPriorityID")) {
            json.put("processing_priority", enumUtil.getASpaceCollectionManagementRecordProcessingPriority(record.getInt("ProcessingPriorityID")));
        }

        recordJS.put("collection_management", json);
    }

    /**
     * Method to add a bioghist note agent object
     *
     *
     * @param agentJS
     * @param record
     * @param creatorTypeId
     * @throws Exception
     */
    public void addBiologicalHistoryNote(JSONObject agentJS, JSONObject record, int creatorTypeId) throws Exception {
        JSONArray notesJA = new JSONArray();
        JSONObject noteJS = new JSONObject();

        noteJS.put("jsonmodel_type", "note_bioghist");
        noteJS.put("label", "biographical statement");

        JSONArray subnotesJA = new JSONArray();

        JSONObject textNoteJS = new JSONObject();
        addTextNote(textNoteJS, record.getString("BiogHist"));
        subnotesJA.put(textNoteJS);

        // add a subnote which holds the citation information
        if(!record.getString("BiogHistAuthor").isEmpty()) {
            JSONObject citationJS = new JSONObject();
            citationJS.put("jsonmodel_type", "note_citation");
            JSONArray contentJA = new JSONArray();
            contentJA.put("Author: " + record.get("BiogHistAuthor"));
            citationJS.put("content", contentJA);
            subnotesJA.put(citationJS);
        }

        // add the subnote which hold the source information
        if(!record.getString("Sources").isEmpty()) {
            String noteType = "note_citation";
            if(creatorTypeId == 22) {
                noteType = "note_abstract";
            }

            JSONObject subnoteJS = new JSONObject();
            subnoteJS.put("jsonmodel_type", noteType);
            JSONArray contentJA = new JSONArray();
            contentJA.put(record.get("Sources"));
            subnoteJS.put("content", contentJA);
            subnotesJA.put(subnoteJS);
        }

        noteJS.put("subnotes", subnotesJA);
        notesJA.put(noteJS);
        agentJS.put("notes", notesJA);
    }

    /**
     * Method to convert a digital object record
     *
     * @param record
     * @return
     */
    public JSONObject convertDigitalObject(JSONObject record) throws Exception {
        // Main json object
        JSONObject json = new JSONObject();

        // add the AT database Id as an external ID
        addExternalId(record, json, "digital_object");

        /* add the fields required for abstract_archival_object.rb */

        String title = record.getString("Title");
        json.put("title", title);

        boolean dateAdded = addDate(record.getString("Date"), json, null, "digitized");

        // need to add title if no date or title
        if(title.isEmpty() && !dateAdded) {
            json.put("title", "Digital Object " + record.get("ID"));
        }

        /* add the fields required digital_object.rb */

        JSONArray fileVersionsJA = new JSONArray();
        addFileVersion(fileVersionsJA, record, "Digital Object");
        json.put("file_versions", fileVersionsJA);

        json.put("digital_object_id", getUniqueID(ASpaceClient.DIGITAL_OBJECT_ENDPOINT, record.getString("Identifier"), null, title));

        // set the digital object type
        json.put("digital_object_type", "mixed_materials");

        // set weather to publish
        publishRecord = convertToBoolean(record.getInt("Browsable"));
        json.put("publish", publishRecord);

        // add the notes
        addDigitalObjectNotes(record, json);

        return json;
    }

    /**
     * Method to convert a digital object record into a aspace digital object component
     *
     * @param record
     * @return
     */
    public JSONObject convertToDigitalObjectComponent(JSONObject record) throws Exception {
        // Main json object
        JSONObject json = new JSONObject();

        json.put("publish", publishRecord);

        /* add the fields required for abstract_archival_object.rb */
        String title = record.getString("Title");
        json.put("title", fixEmptyString(title));

        /* add fields required for digital object component*/
        JSONArray fileVersionsJA = new JSONArray();
        addFileVersion(fileVersionsJA, record, "Digital Object Component");
        json.put("file_versions", fileVersionsJA);

        String label = title;
        json.put("label", label);

        json.put("position", record.getInt("DisplayOrder"));

        if(appendTestIdentifier.isEmpty()){
            json.put("component_id", fixEmptyString(record.getString("ID"), "ID_" + randomString.nextString()));
        } else {
            json.put("component_id", fixEmptyString(record.getString("ID") + appendTestIdentifier, "ID_" + randomString.nextString() + appendTestIdentifier));
        }
        return json;
    }

    /**
     * Method to add a file version object to the digital object
     *
     * @param fileVersionsJA
     */
    public void addFileVersion(JSONArray fileVersionsJA, JSONObject record, String type) throws JSONException {
        if(record.has("ContentURL") && !record.getString("ContentURL").isEmpty()) {
            JSONObject fileVersionJS = new JSONObject();

            fileVersionJS.put("file_uri", record.getString("ContentURL"));
            fileVersionJS.put("use_statement", "image-master");
            fileVersionJS.put("xlink_actuate_attribute", "none");
            fileVersionJS.put("xlink_show_attribute", "none");

            fileVersionsJA.put(fileVersionJS);
        } else if(record.has("Filename") && !record.getString("Filename").isEmpty()) {
            String filename = verifyFilename(record.getString("ID"), record.getString("Filename"));

            JSONObject fileVersionJS = new JSONObject();
            fileVersionJS.put("file_uri", digitalObjectBaseURI + filename);
            fileVersionJS.put("use_statement", "image-master");
            fileVersionJS.put("xlink_actuate_attribute", "none");
            fileVersionJS.put("xlink_show_attribute", "none");
            fileVersionJS.put("file_format_name", record.get("FileTypeID"));
            fileVersionJS.put("file_size_bytes", NumberUtils.toInt((String) record.get("Bytes"), 0));

            fileVersionsJA.put(fileVersionJS);
        } else {
            //System.out.println("No file version found for " + type + ": " + record.get("Title"));
        }
    }

    /**
     * Method to sanitize filenames and make them unique
     * @param id
     * @param filename
     * @return
     */
    private String verifyFilename(String id, String filename) {
        // first remove all none valid characters and replace with "_"
        filename = filename.replaceAll("[^a-zA-Z0-9.-]", "_");

        // now check to see if we have a unique name
        if(!digitalObjectFilenames.contains(filename)) {
            digitalObjectFilenames.add(filename);
        } else {
            // create a unique filename by appending the -0 + id to name
            String uniqueFilename;
            int i = filename.lastIndexOf('.');

            if (i > 0) {
                uniqueFilename = filename.substring(0, i) + "-0" + id + "." + filename.substring(i + 1);
            } else {
                uniqueFilename = filename + "-0" + id;
            }

            String message = "Duplicate Digital Object Filename: "  + filename  + " Changed To: " + uniqueFilename + "\n";
            aspaceCopyUtil.addErrorMessage(message);

            filename = uniqueFilename;
            digitalObjectFilenames.add(filename);
        }

        // add the filename and ID so we can save it later
        fileIDsToFilenamesMap.put(id, filename);

        return filename;
    }

    /**
     * Method to return the hashmap containing the file ids and the files names for saving
     * to the download directory
     * @return
     */
    public HashMap<String, String> getFileIDsToFilenamesMap() {
        return fileIDsToFilenamesMap;
    }

    /**
     * Method to convert an collection record to json ASpace JSON
     *
     * @param record
     * @param classificationIdentifiers
     * @return
     * @throws Exception
     */
    public JSONObject convertCollection(JSONObject record, HashMap<String, String> classificationIdentifiers,
                                        HashMap<String, String> classificationParents) throws Exception {
        // Main json object
        JSONObject json = new JSONObject();

        // add the AR database Id as an external ID
        addExternalId(record, json, "collection");

        /* Add fields needed for abstract_archival_object.rb */

        // check to make sure we have a title
        String title = cleanTitle(fixEmptyString(record.getString("Title")));

        json.put("title", title);

        // add the language code
        json.put("language", getLanguageCode(null, "eng"));

        // add the extent array containing one object or many depending if we using multiple extents
        addResourceExtent(record, json);

        // add the date array containing the dates json objects
        addResourceDates(record, json, "Resource: " + currentCollectionRecordIdentifier);

        // add external documents
        String otherURL = record.getString("OtherURL");
        if(!otherURL.isEmpty()) {
            addExternalDocument(json, "Other URL", otherURL);
        }

        /* Add fields needed for resource.rb */

        // get the ids and make them unique if we in DEBUG mode
        String id = record.getString("CollectionIdentifier");

        // if the collection ID is empty fix it
        if (id == null || id.isEmpty()) {
            id = "##" + randomString.nextString();
            while (resourceIDs.contains(id)) {
                id = "##" + randomString.nextString();
            }
            aspaceCopyUtil.addErrorMessage("Empty collection ID. Changed to " + id + "\n");
        }

        String classificationID = record.getString("ClassificationID");
        String[] idParts = new String[]{"", "", "", ""};

        Stack<String> fullId = new Stack<String>();
        fullId.push(id);
        String cId = classificationID;
        while (cId != null) {
            String identifier = classificationIdentifiers.get(cId);
            if (identifier == null) break;
            fullId.push(identifier);
            cId = classificationParents.get(cId);
        }

        idParts[0] = fullId.pop();
        if (!fullId.isEmpty()) idParts[1] = fullId.pop();
        if (!fullId.isEmpty()) idParts[2] = fullId.pop();
        while (fullId.size() > 1) idParts[2] += "-" + fullId.pop();
        if (!fullId.isEmpty()) idParts[3] = fullId.pop();

        // make sure the id is unique
        getUniqueID(ASpaceClient.RESOURCE_ENDPOINT, "", idParts, title);

        // debug code to generate random ids for copying over the same collection records
        if(makeUnique) {
            idParts[0] = randomString.nextString();
            idParts[1] = randomString.nextString();
            idParts[2] = randomString.nextString();
            idParts[3] = randomString.nextString();
        }

        if(!appendTestIdentifier.isEmpty()){
            idParts[0] += appendTestIdentifier;
        }

        json.put("id_0", idParts[0]);
        json.put("id_1", idParts[1]);
        json.put("id_2", idParts[2]);
        json.put("id_3", idParts[3]);

        // get the level
        json.put("level", "collection");

        if(record.has("MaterialTypeID")) {
            json.put("resource_type", enumUtil.getASpaceResourceType(record.getString("MaterialTypeID")));
        }

        // set the publish, restrictions, processing note, container summary
        publishRecord = convertToBoolean(record.getInt("Enabled"));
        json.put("publish", publishRecord);

        json.put("container_summary", "Archon Container Summary");

        // add fields for EAD
        json.put("ead_id", concatIdParts(idParts));
        json.put("ead_location", "Archon Finding Aid location");
        json.put("finding_aid_title", "Archon Finding Aid Title");
        json.put("finding_aid_date", getHumanReadableDate(record.getString("PublicationDate")));
        json.put("finding_aid_author", record.get("FindingAidAuthor"));

        Integer descriptiveRulesID = record.getInt("DescriptiveRulesID");
        if(descriptiveRulesID != null) {
            json.put("finding_aid_description_rules", enumUtil.getASpaceFindingAidDescriptionRule(descriptiveRulesID));
        }

        json.put("finding_aid_language", enumUtil.getASpaceLanguageCode(record.getString("FindingLanguageID")));
        json.put("finding_aid_note", record.get("PublicationNote"));

        // add any reversion statements
        addRevisionStatement(record, json);

        // add the notes
        addResourceNotes(record, json);

        return json;
    }

    /**
     * Takes a natural language alternative extent statement that may 
     * containt many individual extent statments seperated by an "and"
     * a "," or a "." and splits them into individual statements
     * 
     * @param alternativeExtent string containing list of extent statements
     * @return an array of extent statements
     */
    public String[] splitAlternativeExtent(String alternativeExtent)
    {
        alternativeExtent = alternativeExtent.replaceAll("^and ", "");
        String regex = "\\s?and\\s|\\.\\s|,\\s";
        return alternativeExtent.split(regex);

    }

    /**
     * Takes a written value and returns the corresponding number as a string
     * Examples: 
     *  input: an => output: 1
     *  input: seven => output: 7
     *  input: a single => output: 1
     * @param writtenValue
     * @return
     */
    public String getNumber(String writtenValue) {
        HashMap<String, String> numberTranslator = new HashMap<String, String>();
        numberTranslator.put("a", "1");
        numberTranslator.put("an", "1");
        numberTranslator.put("a single", "1");
        numberTranslator.put("one", "1");
        numberTranslator.put("two", "2");
        numberTranslator.put("three", "3");
        numberTranslator.put("four", "4");
        numberTranslator.put("five", "5");
        numberTranslator.put("six", "6");
        numberTranslator.put("seven", "7");
        numberTranslator.put("eight", "8");
        numberTranslator.put("nine", "9");
        numberTranslator.put("ten", "10");

        if (numberTranslator.containsKey(writtenValue)) {
            return numberTranslator.get(writtenValue);
        } else {
            return "";
        }
        
    }

    /**
     * This takes a natural langauge extent statement that is expected to have
     * a single unit expressed a digits, decimal number, indefinate article, or 
     * written number followed by a single unpunctuated string which is the unit. 
     * If matching fails, error key is set to true and the failing string is
     * added
     * 
     * @param extent a natural language extent statement 
     * @return parsed extent as JSONObject with keys for unit, value, and error
     * @throws JSONException
     */
    public JSONObject parseExtentStatement(String extent) throws JSONException
    {

        JSONObject structuredExtent = new JSONObject();

        String regex = "(^\\d+|\\d+\\.\\d+|a|an|a single|one|two|three|four|five|six|seven|eight|nine|ten)\\s([A-z\s]+)";

        Pattern pattern = Pattern.compile(regex);
        Matcher matcher  = pattern.matcher(extent);
        String unitValue = "";
        String unit;
        Boolean error;
        String confound;
        if (matcher.find()) {
            unitValue = matcher.group(1);
            unit = matcher.group(2);
            error = false;
            confound = "";
        } else {
            unitValue = "";
            unit = "";
            error = true;
            confound = extent;
        }

        //if a non-digit was used for unit value, get a digit version
        if (!getNumber(unitValue).isEmpty()) {
            unitValue = getNumber(unitValue);
        };

        structuredExtent.put("unit", unit);
        structuredExtent.put("unitValue", unitValue);
        structuredExtent.put("error", error);
        structuredExtent.put("confound", confound);

        return structuredExtent;
    }

    /**
     * This takes a natural language string and tries to find a good 
     * match among the existing extent types. It returns a JSONObject
     * with the best match and a score to indicate how good of a match
     *  
     * @param inputExtent a natural langugae expression of extent type
     * @return
     * @throws JSONException 
     */
    public JSONObject mapExtentType(String inputExtent) throws JSONException {
        String cleanInput = inputExtent.toLowerCase().replace(" ", "_");
        //all of the archon extents should have already been added to ASpace. Keeping this 
        //here for now in case we want to test
        // ArrayList<String> allExtents = enumUtil.getAllArchonExtents();

        // for (String aSpaceExtent : aSpaceExtents ) {
        //     allExtents.add(aSpaceExtent);
        // }

        JSONObject match  = new JSONObject();
        String mapping = "";

        for (String extent : aSpaceExtents) {
            match.put("clean_input", cleanInput);
            if (cleanInput.contains(extent)) {
                mapping = extent;
            }

        }
        //TODO: conduct the fuzzy matching and generate matching score
        match.put("score","");
        match.put("mapping", mapping);
        return match;

    }

    public JSONObject processAlternativeExtent(String alternativeExtent) throws JSONException {

        
        JSONObject extentsWrapper = new JSONObject();
        extentsWrapper.put("errors", "false");
        JSONArray processedExtents = new JSONArray();
        
        //get an array of all the extents listed in the alternative extent statement
        String[] extents = splitAlternativeExtent(alternativeExtent);
        
        
        //parse each of the extent statements into a structure extent JSONObject 
        for (String extent : extents) {
            JSONObject parsedExtent = parseExtentStatement(extent);
            processedExtents.put(parsedExtent);
            if (parsedExtent.getString("error").equalsIgnoreCase("true")) {
                extentsWrapper.put("errors", "true");
            }
        }

        //if we have anything that didn't cleanly map to an extent, put the entire alt extent statement into a comment
        //for the collection 
        extentsWrapper.put("processedExtents", processedExtents);
        return extentsWrapper;

    }

    /**
     * Method to add extent information
     *
     * @param record
     * @param json
     * @throws Exception
     */
    private void addResourceExtent(JSONObject record, JSONObject json) throws Exception {
        JSONArray extentJA = new JSONArray();
        JSONObject extentJS = new JSONObject();
        String altExtent = record.getString("AltExtentStatement");

        //TODO: add some additional parsing to tell if the alt extent is a true alt extent
        //in which case the alt extent should be added as container_summary and portion 
        //should be se as whole
        if (!altExtent.isEmpty()){
            extentJS.put("portion", "part");

        } else {
            extentJS.put("portion", "whole");
        }
        
        extentJS.put("extent_type", enumUtil.getASpaceExtentType(record.getInt("ExtentUnitID")));

        if (!record.getString("Extent").isEmpty()) {
            extentJS.put("number", record.getString("Extent"));
        } else {
            extentJS.put("number", "0");
        }

        extentJA.put(extentJS);

        //TODO: this should be refactored into processAlternativeExtent to make it more testable
        // add the alternative extent statement
        if(!altExtent.isEmpty()) {
            JSONObject altExtentJS = new JSONObject();
            altExtentJS.put("portion", "part");
            JSONObject structuredExtentsWrapper = processAlternativeExtent(altExtent);



            //if any of the extents had an error, add the whole alt extent to the statement
            if (structuredExtentsWrapper.getString("errors").equalsIgnoreCase("true")) {
                altExtentJS.put("container_summary", altExtent);    
                //TODO: output an error message
            }

            //proccess the alternative extent statement into structured extents and add to altExtentJA
            JSONArray structuredExtents = structuredExtentsWrapper.getJSONArray("processedExtents");
            for (int i=0; i < structuredExtents.length(); i++) { 
                JSONObject structuredExtent = structuredExtents.getJSONObject(i);
                if ( ! structuredExtent.getString("unit").isEmpty()) {
                    //TODO: implement score checking
                    altExtentJS.put("extent_type", structuredExtent.getString("unit"));
                    altExtentJS.put("number", structuredExtent.getString("unitValue"));
                    extentJA.put(altExtentJS);
                } else {
                    String collectionIdentifier = record.getString("CollectionIdentifier");
                    String archonID = record.getString("ID");
                    String debugMessage = "Collection: " + collectionIdentifier + "has an alternative extent statement that didn't map cleanly\n"
                                        + "Archon ID: " + archonID + "\n"
                                        + "Structured Alt Extent:\n"
                                        + structuredExtentsWrapper.getJSONArray("processedExtents").toString();
                    
                    aspaceCopyUtil.addErrorMessage(debugMessage);
                }
            }
        }

        json.put("extents", extentJA);
    }

    /**
     * Method to add a date json object
     *
     * @param json
     * @param record
     */
    private void addResourceDates(JSONObject record, JSONObject json, String recordIdentifier) throws Exception {
        JSONArray dateJA = new JSONArray();

        JSONObject dateJS = new JSONObject();

        dateJS.put("date_type", "single");
        dateJS.put("label", "created");

        String dateExpression = record.getString("InclusiveDates");
        dateJS.put("expression", dateExpression);

        Integer dateBegin = convertToInteger(record.getString("NormalDateBegin"));
        Integer dateEnd = convertToInteger(record.getString("NormalDateEnd"));

        if (dateBegin != null) {
            dateJS.put("date_type", "inclusive");

            dateJS.put("begin", dateBegin.toString());

            if (dateEnd != null) {
                if(dateEnd >= dateBegin) {
                    dateJS.put("end", dateEnd.toString());
                } else {
                    dateJS.put("end", dateBegin.toString());

                    String message = "End date: " + dateEnd + " before begin date: " + dateBegin + ", ignoring end date\n" + recordIdentifier;
                    aspaceCopyUtil.addErrorMessage(message);
                }
            } else {
                dateJS.put("end", dateBegin.toString());
            }
        }

        // see if to add this date now
        if((dateExpression != null && !dateExpression.isEmpty()) || dateBegin != null) {
            dateJA.put(dateJS);
        }

        // add the bulk dates
        String bulkDates = record.getString("PredominantDates");
        if(!bulkDates.isEmpty()) {
            dateJS = new JSONObject();

            dateJS.put("date_type", "bulk");

            dateJS.put("label", "other");

            dateExpression = bulkDates;
            dateJS.put("expression", dateExpression);

            dateJA.put(dateJS);
        }

        // add the acquisition date
        String acquisitionDate = record.getString("AcquisitionDate");
        if(!acquisitionDate.isEmpty()) {
            dateJS = new JSONObject();

            dateJS.put("date_type", "single");

            dateJS.put("label", "other");

            // convert date to human readable format
            acquisitionDate = getHumanReadableDate(acquisitionDate);

            dateExpression = "Date acquired: " + acquisitionDate;

            dateJS.put("expression", dateExpression);

            dateJA.put(dateJS);
        }

        // it is still possible to get to this point without any dates so just hard a dummy
        // date so that the record can be saved.
        if(dateJA.length() == 0) {
            dateJS = new JSONObject();

            dateJS.put("date_type", "single");

            dateJS.put("label", "other");

            dateExpression = "Dummy Date";
            dateJS.put("expression", dateExpression);

            dateJA.put(dateJS);
        }

        json.put("dates", dateJA);
    }

    /**
     * Add a revision statement to the resource record
     * @param json
     * @param record
     */
    private void addRevisionStatement(JSONObject record, JSONObject json) throws JSONException {
        String revisionHistory = record.getString("RevisionHistory");
        if(revisionHistory.isEmpty()) return;

        JSONObject revisionStatementJS = new JSONObject();
        revisionStatementJS.put("date", "09099999");
        revisionStatementJS.put("description", revisionHistory);

        JSONArray revisionStatementsJA = new JSONArray();
        revisionStatementsJA.put(revisionStatementJS);

        json.put("revision_statements", revisionStatementsJA);
    }

    /**
     * Method to convert a collection content record to json ASpace JSON
     *
     * @param record
     * @return
     * @throws Exception
     */
    public JSONObject convertCollectionContent(JSONObject record) throws Exception {
        // Main json object
        JSONObject json = new JSONObject();

        json.put("publish", publishRecord);

        addExternalId(record, json, "Collection_Content");

        /* Add fields needed for abstract_archival_object.rb */

        // check to make sure we have a title
        String title = cleanTitle(record.getString("Title"));
        json.put("title", title);

        boolean dateAdded = addDate(record.getString("Date"), json, null, "created");

        // need to add title if no date or title
        String uniqueId = record.getString("UniqueID");
        if(title.isEmpty() && !dateAdded) {
            if(!uniqueId.isEmpty()) {
                json.put("title", uniqueId);
            } else {
                json.put("title", "migration_" + randomStringLong.nextString() + "_" + record.get("ID"));
            }
        }

        /* add field required for archival_object.rb */

        // make the ref id unique otherwise ASpace complains
        //String refId = record.getString("ID") + "_SortOrder-" + record.getInt("SortOrder");
        String refId = record.getString("ID");
        json.put("ref_id", refId);

        //String level = "series";
        String level = record.getString("EADLevel");
        json.put("level", level);

        if(level.equals("otherlevel")) {
            json.put("other_level", fixEmptyString(record.getString("OtherLevel")));
        }

        if(!uniqueId.isEmpty()) {
            json.put("component_id", record.getString("UniqueID"));
        }

        // add the notes
        addResourceComponentNotes(record, json);

        return json;
    }

    /**
     * Method to add a date json object
     *
     * @param dateExpression
     * @param json
     * @param label
     */
    private boolean addDate(String dateExpression, JSONObject json, String dateType, String label) throws Exception {
        //String dateExpression = record.getString("Date");
        if(dateExpression.isEmpty()) return false;

        JSONArray dateJA = new JSONArray();
        JSONObject dateJS = new JSONObject();

        if(dateType == null) {
            dateJS.put("date_type", "single");
        } else {
            dateJS.put("date_type", dateType);
        }

        dateJS.put("label", label);
        dateJS.put("expression", dateExpression);

        dateJA.put(dateJS);
        json.put("dates", dateJA);

        return true;
    }

    /**
     * Method to return the language code
     *
     * @param languageCodes
     * @return
     */
    private String getLanguageCode(JSONArray languageCodes, String defaultLanguageCode) throws Exception {
        if(languageCodes != null && languageCodes.length() != 0) {
            return languageCodes.getString(0);
        } else {
            return defaultLanguageCode;
        }
    }

    /**
     * Add an external document to the JSON object
     * @param json
     * @param title
     * @param location
     */
    private void addExternalDocument(JSONObject json, String title, String location) throws Exception {
        JSONArray externalDocumentsJA = new JSONArray();

        JSONObject documentJS = new JSONObject();
        documentJS.put("publish", true);
        documentJS.put("title", title);
        documentJS.put("location", fixUrl(location));
        externalDocumentsJA.put(documentJS);

        json.put("external_documents", externalDocumentsJA);
    }

    /**
     * Method to add notes to the digital object
     *
     * @param json
     * @param record
     */
    private void addDigitalObjectNotes(JSONObject record, JSONObject json) throws Exception {
        JSONArray notesJA = new JSONArray();

        String noteContent = "";

        addDigitalObjectNote(notesJA, "summary", "Summary Note", record.getString("Scope"));

        addDigitalObjectNote(notesJA, "physdesc", "Physical Description Note", record.getString("PhysicalDescription"));

        if(!record.getString("Publisher").isEmpty()) {
            noteContent = "Publisher: " + record.getString("Publisher");
            addDigitalObjectNote(notesJA, "note", "Publisher Note", noteContent);
        }

        if(!record.getString("Contributor").isEmpty()) {
            noteContent = "Contributor: " + record.getString("Contributor");
            addDigitalObjectNote(notesJA, "note", "Contributor Note", noteContent);
        }

        addDigitalObjectNote(notesJA, "userestrict", "Rights Statement Note", record.getString("RightsStatement"));

        json.put("notes", notesJA);
    }

    /**
     * Method to add notes to this finding aid
     * @param json
     * @param record
     */
    private void addResourceNotes(JSONObject record, JSONObject json) throws Exception {
        JSONArray notesJA = new JSONArray();

        String noteContent = "";

        addMultipartNote(notesJA, "scopecontent", "Scope and Contents", record.getString("Scope"));

        addSinglePartNote(notesJA, "abstract", "Abstract", record.getString("Abstract"));

        addMultipartNote(notesJA, "arrangement", "Arrangement Note", record.getString("Arrangement"));

        addMultipartNote(notesJA, "accessrestrict", "Conditions Governing Access", record.getString("AccessRestrictions"));

        addMultipartNote(notesJA, "userestrict", "Conditions Governing Use", record.getString("UseRestrictions"));

        addMultipartNote(notesJA, "phystech", "Physical Access Requirements", record.getString("PhysicalAccess"));

        addMultipartNote(notesJA, "phystech", "Technical Access Requirements", record.getString("TechnicalAccess"));

        addMultipartNote(notesJA, "acqinfo", "Source of Acquisition", record.getString("AcquisitionSource"));

        addMultipartNote(notesJA, "acqinfo", "Method of Acquisition", record.getString("AcquisitionMethod"));

        addMultipartNote(notesJA, "appraisal", "Appraisal Information", record.getString("AppraisalInfo"));

        addMultipartNote(notesJA, "accruals", "Accruals and Additions", record.getString("AccrualInfo"));

        addMultipartNote(notesJA, "custodhist", "Custodial History", record.getString("CustodialHistory"));

        noteContent = record.getString("OrigCopiesNote") + "\n\n" + record.get("OrigCopiesURL");
        addMultipartNote(notesJA, "originalsloc", "Existence and Location of Originals", noteContent);

        noteContent = record.getString("RelatedMaterials") + "\n\n" + record.get("RelatedMaterialsURL");
        addMultipartNote(notesJA, "relatedmaterial", "Related Materials", noteContent);

        addMultipartNote(notesJA, "relatedmaterial", "Related Publications", record.getString("RelatedPublications"));

        addMultipartNote(notesJA, "separatedmaterial", "Separated Materials", record.getString("SeparatedMaterials"));

        addMultipartNote(notesJA, "prefercite", "Preferred Citation", record.getString("PreferredCitation"));

        addMultipartNote(notesJA, "odd", "Other Descriptive Information", record.getString("OtherNote"));

        addMultipartNote(notesJA, "processinfo", "Processing Information", record.getString("ProcessingInfo"));

        noteContent = record.getString("BiogHist") + "\n\nNote written by " + record.get("BiogHistAuthor");
        if(!noteContent.trim().equals("Note written by")) {
            addMultipartNote(notesJA, "bioghist", "Biographical or Historical Information", noteContent);
        }

        json.put("notes", notesJA);
    }

    /**
     * Method to add notes for this resource component
     *
     * @param json
     * @param record
     */
    private void addResourceComponentNotes(JSONObject record, JSONObject json) throws Exception {
        JSONArray notesJA = new JSONArray();

        addSinglePartNote(notesJA, "materialspec", "Private Title", record.getString("PrivateTitle"));

        addMultipartNote(notesJA, "scopecontent", "Scope and Contents", record.getString("Description"));

        Object object = record.get("Notes");
        if(object instanceof JSONObject) {
            JSONObject recordNotes = (JSONObject)object;

            Iterator<String> keys = recordNotes.keys();
            while(keys.hasNext()) {
                JSONObject note = recordNotes.getJSONObject(keys.next());
                if(note.has("NoteType")) {
                    addMultipartNote(notesJA, note.getString("NoteType"), note.getString("Label"), note.getString("Content"));
                }
            }
        }

        json.put("notes", notesJA);
    }

    /**
     * Method to add single part note
     * @param notesJA
     * @param noteType
     * @param noteLabel
     * @param noteContent
     * @throws Exception
     */
    private void addSinglePartNote(JSONArray notesJA, String noteType, String noteLabel, String noteContent) throws Exception {
        if(noteContent.isEmpty() || noteType.isEmpty()) return;

        JSONObject noteJS = new JSONObject();

        noteJS.put("jsonmodel_type", "note_singlepart");
        noteJS.put("type", noteType);
        noteJS.put("label", noteLabel);
        noteJS.put("publish", publishRecord);

        JSONArray contentJA = new JSONArray();
        contentJA.put(noteContent);
        noteJS.put("content", contentJA);

        notesJA.put(noteJS);
    }

    /**
     * Add a multipart note
     *
     * @param notesJA
     * @param noteType
     * @param noteLabel
     * @param noteContent
     * @throws Exception
     */
    private void addMultipartNote(JSONArray notesJA, String noteType, String noteLabel, String noteContent) throws Exception {
        if(noteContent.trim().isEmpty() || noteType.isEmpty()) return;

        // these note types don't exist in ASpace
        if (noteType.equals("unitid") || noteType.equals("origination") || noteType.equals("note")) noteType = "odd";

        // these note types should be single part
        if (noteType.equals("physfacet") || noteType.equals("physdesc") || noteType.equals("langmaterial") ||
                noteType.equals("materialspec")) {
            addSinglePartNote(notesJA, noteType, noteLabel, noteContent);
            return;
        }

        JSONObject noteJS = new JSONObject();

        noteJS.put("jsonmodel_type", "note_multipart");
        noteJS.put("type", noteType);
        noteJS.put("label", noteLabel);
        noteJS.put("publish", publishRecord);

        JSONArray subnotesJA = new JSONArray();

        // add the default text note
        JSONObject textNoteJS = new JSONObject();
        addTextNote(textNoteJS, fixEmptyString(noteContent, "multi-part note content"));
        subnotesJA.put(textNoteJS);

        noteJS.put("subnotes", subnotesJA);

        notesJA.put(noteJS);
    }

    /**
     * Method to add a text note
     *
     * @param noteJS
     * @param content
     * @throws Exception
     */
    private void addTextNote(JSONObject noteJS, String content) throws Exception {
        noteJS.put("jsonmodel_type", "note_text");
        noteJS.put("publish", publishRecord);
        noteJS.put("content", content);
    }

    /**
     * Method to add digital object note
     *
     * @param notesJA
     * @param noteType
     * @param noteLabel
     * @param noteContent
     * @throws Exception
     */
    private void addDigitalObjectNote(JSONArray notesJA, String noteType, String noteLabel, String noteContent) throws Exception {
        if(noteContent.isEmpty() || noteType.isEmpty()) return;

        JSONObject noteJS = new JSONObject();

        noteJS.put("jsonmodel_type", "note_digital_object");
        noteJS.put("type", noteType);
        noteJS.put("label", noteLabel);
        noteJS.put("publish", publishRecord);

        JSONArray contentJA = new JSONArray();
        contentJA.put(noteContent);
        noteJS.put("content", contentJA);

        notesJA.put(noteJS);
    }

    /**
     * Method to add the AT internal database ID as an external ID for the ASpace object
     *
     * @param record
     * @param source
     */
    public void addExternalId(JSONObject record, JSONObject recordJS, String source) throws Exception {
        source = "Archon Instance::" + source.toUpperCase();

        JSONArray externalIdsJA = new JSONArray();
        JSONObject externalIdJS = new JSONObject();

        externalIdJS.put("external_id", record.get("ID"));
        externalIdJS.put("source", source);

        externalIdsJA.put(externalIdJS);

        recordJS.put("external_ids", externalIdsJA);
    }

    /**
     * Method to get a reference object which points to another URI
     *
     * @param recordURI
     * @return
     * @throws Exception
     */
    public JSONObject getReferenceObject(String recordURI) throws Exception {
        JSONObject referenceJS = new JSONObject();
        referenceJS.put("ref", recordURI);
        return referenceJS;
    }

    /**
     * Method to set a string that's empty to "unspecified"
     * @param text
     * @return
     */
    public String fixEmptyString(String text) {
        return fixEmptyString(text, null);
    }

    /**
     * Method to set a string that empty to "not set"
     * @param text
     * @return
     */
    private String fixEmptyString(String text, String useInstead) {
        if(text == null || text.trim().isEmpty()) {
            if(useInstead == null) {
                return "unspecified";
            } else {
                return useInstead;
            }
        } else {
            return text;
        }
    }

    /**
     * Method to prepend http:// to a url to prevent ASpace from complaining
     *
     * @param url
     * @return
     */
    private String fixUrl(String url) {
        if(url.isEmpty()) return "http://url.unspecified";

        String lowercaseUrl = url.toLowerCase();

        // check to see if its a proper uri format
        if(lowercaseUrl.contains("://")) {
            return url;
        } else if(lowercaseUrl.startsWith("/") || lowercaseUrl.contains(":\\")) {
            url = "file://" + url;
            return url;
        } else {
            url = "http://" + url;
            return  url;
        }
    }

    /**
     * Method to map the ASpace group to one or more AT access classes
     *
     * @param groupJS
     * @return
     */
    public void mapAccessClass(HashMap<String, JSONObject> repositoryGroupURIMap,
                               JSONObject groupJS, String repoURI) {
        try {
            String groupCode = groupJS.getString("group_code");
            String key = "";

            if (groupCode.equals("administrators")) { // map to access class 5
                key = repoURI + ACCESS_CLASS_PREFIX + "1";
                repositoryGroupURIMap.put(key, groupJS);
            } else if(groupCode.equals("repository-managers")) { // map to access class 4
                key = repoURI + ACCESS_CLASS_PREFIX + "1";
                repositoryGroupURIMap.put(key, groupJS);
            } else if(groupCode.equals("repository-archivists")) { // map to access class 3
                key = repoURI + ACCESS_CLASS_PREFIX + "1";
                repositoryGroupURIMap.put(key, groupJS);
            } else if(groupCode.equals("repository-advanced-data-entry")) { // map to access class 2
                key = repoURI + ACCESS_CLASS_PREFIX + "2";
                repositoryGroupURIMap.put(key, groupJS);
            } else if(groupCode.equals("repository-basic-data-entry")) { // map to access class 1
                key = repoURI + ACCESS_CLASS_PREFIX + "3";
                repositoryGroupURIMap.put(key, groupJS);
            } else if (groupCode.equals("repository-viewers")) { // map access class to access class 0 for now
                key = repoURI + ACCESS_CLASS_PREFIX + "4";
                repositoryGroupURIMap.put(key, groupJS);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Method to return a unique id, in cases where ASpace needs a unique id but AT doesn't
     *
     * @param endpoint
     * @param id
     * @return
     */
    private String getUniqueID(String endpoint, String id, String[] idParts, String title) {
        id = id.trim();

        if(endpoint.equals(ASpaceClient.DIGITAL_OBJECT_ENDPOINT)) {
            // if id is empty add text
            if(id.isEmpty()) {
                id = "Digital Object ID ##"+ randomStringLong.nextString();
            }
            
            if(!appendTestIdentifier.isEmpty()){
                id += appendTestIdentifier;
            }

            if(!digitalObjectIDs.contains(id)) {
                digitalObjectIDs.add(id);
            } else {
                id += " ##" + randomStringLong.nextString();
                digitalObjectIDs.add(id);
            }

            return id;
        } else if(endpoint.equals(ASpaceClient.ACCESSION_ENDPOINT)) {
            String message;

            if(!appendTestIdentifier.isEmpty()){
                id += appendTestIdentifier;
            }

            if(!accessionIDs.contains(id)) {
                accessionIDs.add(id);
                return "";
            } else {
                String nid;

                do {
                    nid = "##" + randomStringLong.nextString();
                } while (accessionIDs.contains(nid));

                if(!appendTestIdentifier.isEmpty()){
                    nid += appendTestIdentifier;
                }

                accessionIDs.add(nid);

                message = "Duplicate Accession (" + title +  ") Id: "  + id  + " Added: " + nid + "\n";
                System.out.println(message);
                aspaceCopyUtil.addErrorMessage(message);

                return nid;
            }
        } else if(endpoint.equals(ASpaceClient.RESOURCE_ENDPOINT)) {
            String message = null;

            // get the concat id now
            id = concatIdParts(idParts);

            if (!resourceIDs.contains(id)) {
                resourceIDs.add(id);
            } else {
                String fullId = "";

                do {
                    idParts[0] += " ##" + randomString.nextString();
                    fullId = concatIdParts(idParts);
                } while (resourceIDs.contains(fullId));

                resourceIDs.add(fullId);

                message = "Duplicate Resource Id: " + id + " Changed to: " + fullId + "\n";
                aspaceCopyUtil.addErrorMessage(message);
            }

            // we don't need to return the new id here, since the idParts array
            // is being used to to store the new id
            return "not used";
        } else if(endpoint.equals("ead")) {
            if(id.isEmpty()) {
                return "";
            }

            if(!appendTestIdentifier.isEmpty()){
                id += appendTestIdentifier;
            }

            if(!eadIDs.contains(id)) {
                eadIDs.add(id);
            } else {
                String nid = "";

                do {
                    nid = id + " ##" + randomString.nextString();
                } while(eadIDs.contains(nid));

                if(!appendTestIdentifier.isEmpty()){
                    nid += appendTestIdentifier;
                }

                eadIDs.add(nid);

                String message = "Duplicate EAD Id: "  + id  + " Changed to: " + nid + "\n";
                aspaceCopyUtil.addErrorMessage(message);

                // assign id to new id
                id = nid;
            }

            return id;
        } else {
            return id;
        }
    }

    /**
     * Method to concat the id parts in a string array into a full id delimited by "."
     *
     * @param ids
     * @return
     */
    private String concatIdParts(String[] ids) {
        String fullId = "";
        for(int i = 0; i < ids.length; i++) {
            if(!ids[i].isEmpty() && i == 0) {
                fullId += ids[0];
            } else if(!ids[i].isEmpty()) {
                fullId += "."  + ids[i];
            }
        }

        return fullId;
    }


    /**
     * Method to set the current resource record identifier. Usefull for error
     * message generation
     *
     * @param identifier
     */
    public void setCurrentCollectionRecordIdentifier(String identifier) {
        this.currentCollectionRecordIdentifier = identifier;
    }

    /**
     * A Method to remove bbcode from the title of Archival Objects
     *
     * @param title
     */
    private String cleanTitle(String title) {
        if (bbcodeToHTML) {
            title = title.replace("[i]", "<i>").replace("[/i]", "</i>");
            title = title.replace("[b]", "<b>").replace("[/b]", "</b>");
        } else if (bbcodeToBlank) {
            title = title.replace("[i]", "").replace("[/i]", "");
            title = title.replace("[b]", "").replace("[/b]", "");
        }

        return title;
    }

    /**
     * Method to convert any string to integer
     *
     * @param string
     * @return
     */
    private Integer convertToInteger(String string) {
        Integer number = null;

        try {
            if(!string.isEmpty()) {
                number = new Integer(string);
            }
        } catch(NumberFormatException nfe) { }

        return number;
    }

    /**
     * Method to convert a number to an integer
     * @param i
     * @return
     */
    private Boolean convertToBoolean(Integer i) {
        if(i == 1) {
            return Boolean.TRUE;
        } else {
            return Boolean.FALSE;
        }
    }

    /**
     * Method to return a date from a string formatted as
     *
     * @param dateString
     * @return
     */
    private Date getDate(String dateString) {
        try {
            return simpleDateFormat.parse(dateString);
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Method to return the date if the format is not checked
     *
     * @param dateString
     * @return
     */
    private String getHumanReadableDate(String dateString) {
        try {
            String year = dateString.substring(0, 4);
            String month = dateString.substring(4, 6);
            String day = dateString.substring(6);
            return month + "/" + day + "/" + year;
        } catch (Exception e) {
            return dateString;
        }
    }

    /**
     * Method to test the conversion without having to startup the gui
     *
     * @param args
     */
    public static void main(String[] args) throws JSONException {
        ASpaceMapper mapper = new ASpaceMapper();
    }
}
