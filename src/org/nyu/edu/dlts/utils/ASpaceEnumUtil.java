package org.nyu.edu.dlts.utils;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * This is util class used to mapped ASpace enum list to AT lookup list items
 *
 * Created by IntelliJ IDEA.
 * User: nathan
 * Date: 12/7/12
 * Time: 9:52 AM
 */
public class ASpaceEnumUtil {
    private JSONObject languagesJS;

    private ArrayList<String> aspaceSalutations = new ArrayList<String>();

    private HashMap<String, String> nameLinkCreatorCodes;

    private HashMap<String, JSONObject> dynamicEnums;

    // Hash map that maps AT values to AT codes
    private HashMap<String, String> lookupListValuesToCodes = new HashMap<String, String>();

    private HashMap<String, String> enumListIDsToValues = new HashMap<String, String>();

    // Array list that hold values that are currently in the ASpace backend
    // They is needed, because for some reason, there are values in the AT records
    // which are not in the appropriate LookupListItem table
    private ArrayList<String> validContainerTypes = new ArrayList<String>();
    private ArrayList<String> validResourceTypes = new ArrayList<String>();

    private String[] ASpaceTermTypes = null;
    private String[] ASpaceSubjectSources = null;
    private String[] ASpaceNameSources = null;
    private String[] ASpaceNameRules = null;
    private String[] ASpaceNameDescriptionTypes = null;
    private String[] ASpaceLinkedAgentRoles = null;
    private String[] ASpaceExtentTypes = null;
    private String[] ASpaceDateEnums = null;
    private String[] ASpaceCollectionManagementRecordEnums = null;
    private String[] ASpaceDigitalObjectTypes = null;
    private String[] ASpaceNoteTypes = null;
    private String[] ASpaceResourceLevels = null;
    private String[] ASpaceFindingAidDescriptionRules = null;
    private String[] ASpaceFindingAidStatus = null;
    private String[] ASpaceInstanceTypes = null;
    private String[] ASpaceInstanceContainerTypes = null;
    private String[] ASpaceAcquisitionTypes = null;
    private String[] ASpaceFileVersionUseStatements = null;
    private String[] ASpaceAccessionResourceTypes = null;

    // A trying that is used to bypass
    public final static String UNMAPPED = "other_unmapped";

    public boolean returnATValue = true; // set this to return the AT value instead of UNMAPPED

    //Use to look up long version of language from three letter code
    private JSONObject textLanguagesJS;

    /**
     * Main constructor
     */
    public ASpaceEnumUtil() {
        initASpaceTermTypes();
        initASpaceNameRules();
        initASpaceNameSource();
        initASpaceNameDescriptionType();
        initASpaceExtentTypes();
        initASpaceDateEnums();
        initASpaceCollectionManagementRecordEnums();
        initASpaceLinkedAgentRole();
        initASpaceDigitalObjectType();
        initASpaceFileVersionUseStatements();
        initASpaceNoteTypes();
        initASpaceResourceLevels();
        initASpaceFindingAidDescriptionRules();
        initASpaceFindingAidStatus();
        initASpaceInstanceTypes();
        initASpaceInstanceContainerTypes();
        initASpaceAcquisitionTypes();
        initASpaceAccessionResourceTypes();

        loadLanguageCodes();
        loadLanguageText();
    }

    /**
     * Method to load the language codes
     */
    private void loadLanguageCodes() {
        try {
            String text = IOUtils.toString(this.getClass().getResourceAsStream("languages.json"), "UTF-8");
            languagesJS = new JSONObject(text);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Method to load the language text json object
     */
    private void loadLanguageText() {
        try {
            String text = IOUtils.toString(this.getClass().getResourceAsStream("languageTextCodes.json"), "UTF-8");
            textLanguagesJS = new JSONObject(text);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Method to set the name link creator hash map
     * @param nameLinkCreatorCodes
     */
    public void setNameLinkCreatorCodes(HashMap<String, String> nameLinkCreatorCodes) {
        this.nameLinkCreatorCodes = nameLinkCreatorCodes;
    }

    /**
     * Initialize the array that hold term types
     */
    private void initASpaceTermTypes() {
        ASpaceTermTypes = new String[]{
                "cultural_context",     // 0
                "function",             // 1
                "geographic",           // 2
                "genre_form",           // 3
                "occupation",           // 4
                "style_period",         // 5
                "technique",            // 6
                "temporal",             // 7
                "topical",              // 8
                "uniform_title"         // 9
        };
    }

    /**
     * Method to map the AR subject term type to the ones in ASPace
     * @param arID
     * @return
     */
    public String getASpaceTermType(int arID) {
        if(arID == 4) {
            return ASpaceTermTypes[1];
        } else if(arID == 5) {
            return ASpaceTermTypes[3];
        } else if(arID == 6) {
            return ASpaceTermTypes[2];
        } else if(arID == 7) {
            return ASpaceTermTypes[4];
        } else if(arID == 2) {
            return ASpaceTermTypes[7];
        } else if(arID == 1) {
            return ASpaceTermTypes[8];
        } else if(arID == 9) {
            return ASpaceTermTypes[9];
        } else { // return topical as the default
            return ASpaceTermTypes[8];
        }
    }

    /**
     * Method to map the Archon country code to a name
     *
     * @param arID
     * @return
     */
    public String getASpaceCountryCode(int arID) {
        String key = "country_" + arID;
        return getEnumValueForID(key, "US");
    }

    /**
     * Method to map the subject source
     *
     * @param arID
     * @return
     */
    public String getASpaceSubjectSource(int arID) {
        String key = "subject_source_" + arID;
        return getEnumValueForID(key, "local");
    }

    /**
     * initialize the name source array
     */
    private void initASpaceNameSource() {
        ASpaceNameSources = new String[]{
                "local",    // 0
                "naf",      // 1
                "nad",      // 2
                "ulan"      // 3
        };

    }

    /**
     * Map the name source
     *
     * @param arID
     * @return
     */
    public String getASpaceNameSource(int arID) {
        String key = "name_source_" + arID;
        return getEnumValueForID(key, "local");
    }

    public String getASpaceNameSourceForSubject(int arID) {
        String key = "name_subject_source_" + arID;
        return getEnumValueForID(key, "local");
    }

    /**
     * Method to initASpaceialize the name rules array
     */
    private void initASpaceNameRules() {
        ASpaceNameRules = new String[] {
                "local",    // 0
                "aacr",     // 1
                "dacs"      // 2
        };
    }

    /**
     * Map the name source
     *
     * @param arID
     * @return
     */
    public String getASpaceExtentType(int arID) {
        String key = "extent_type_" + arID;
        return getEnumValueForID(key, "linear_feet");
    }

    /**
     * Map the aspace resource type
     *
     * @param arID
     * @return
     */
    public String getASpaceResourceType(String arID) {
        String key = "resource_type_" + arID;
        return getEnumValueForID(key, "records");
    }

    /**
     * Method to map the name rule
     *
     * @param atValue
     * @return
     */
    public String getASpaceNameRule(String atValue) {
        if(atValue == null || atValue.isEmpty()) return "";

        atValue = atValue.toLowerCase();

        if(atValue.contains("anglo")) {
            return ASpaceNameRules[1];
        } else if(atValue.contains("describing")) {
            return ASpaceNameRules[2];
        } else if(atValue.contains("local")) {
            return ASpaceNameRules[0];
        } else if(returnATValue) {
            return atValue;
        } else {
            return UNMAPPED;
        }
    }

    /**
     * Get the Name description type
     */
    private void initASpaceNameDescriptionType() {
        ASpaceNameDescriptionTypes = new String[]{
                "administrative history",   // 0
                "biographical statement",   // 1
        };
    }

    /**
     * Method to map the name description type
     *
     * @param atValue
     * @return
     */
    public String getASpaceNameDescriptionType(String atValue) {
        if(atValue == null || atValue.isEmpty()) return "";

        atValue = atValue.toLowerCase();

        if(atValue.contains("administrative")) {
            return ASpaceNameDescriptionTypes[0];
        } else if(atValue.contains("biography")) {
            return ASpaceNameDescriptionTypes[1];
        } else if(returnATValue) {
            return atValue;
        } else {
            return UNMAPPED;
        }
    }

    /**
     * Method to return the ASpace salutation. There is currently no way to map this
     *
     * @param atValue
     * @return
     */
    public String getASpaceSalutation(String atValue) {
        if(atValue == null || atValue.isEmpty()) return "";

        atValue = atValue.toLowerCase();
        atValue = atValue.replace(".", "");

        if(aspaceSalutations.contains(atValue)) {
            return atValue;
        } else {
            return UNMAPPED;
        }
    }

    /**
     * Method to init ASpace extent type array
     */
    private void initASpaceExtentTypes() {
        ASpaceExtentTypes = new String[] {
                "cassettes",            // 0
                "cubic_feet",           // 1
                "files",                // 2
                "gigabytes",            // 3
                "leaves",               // 4
                "linear_feet",          // 5
                "megabytes",            // 6
                "photographic_prints",  // 7
                "photographic_slides",  // 8
                "reels",                // 9
                "sheets",               // 10
                "terabytes",            // 11
                "volumes"               // 12
        };
    }

    /**
     * Method to map the extent type
     *
     * @param atValue
     * @return
     */
    public String getASpaceExtentType(String atValue) {
        if(atValue == null || atValue.isEmpty()) return ASpaceExtentTypes[1];

        atValue = atValue.toLowerCase();

        if(atValue.contains("cubic")) {
            return ASpaceExtentTypes[1];
        } else if(atValue.contains("linear")) {
            return ASpaceExtentTypes[5];
        } else if(returnATValue) {
            return atValue;
        } else {
            return UNMAPPED;
        }
    }

    /**
     * Method init ASpace the array the hold information on dates
     */
    private void initASpaceDateEnums() {
        ASpaceDateEnums = new String[] {
                "single",       // 0
                "bulk",         // 1
                "inclusive",    // 2
                "broadcast",    // 3
                "copyright",    // 4
                "creation",     // 5
                "deaccession",  // 6
                "digitized",    // 7
                "issued",       // 8
                "modified",     // 9
                "publication",  // 10
                "other",        // 11
                "approximate",  // 12
                "inferred",     // 13
                "questionable", // 14
                "ce",           // 15
                "gregorian"     // 16
        };
    }

    /**
     * Method to return the date info. Most of these are direct one-to-one mapping
     * but implementing this way for greater flexibility
     *
     * @param atValue
     */
    public String getASpaceDateEnum(String atValue) {
        if(atValue == null || atValue.isEmpty()) return "other";

        atValue = atValue.toLowerCase();

        if(atValue.contains("creation")) {
            return ASpaceDateEnums[5];
        } else if(atValue.contains("recordkeeping")) {
            return ASpaceDateEnums[11];
        } else if(atValue.contains("publication")) {
            return ASpaceDateEnums[10];
        } else if(atValue.contains("broadcast")) {
            return ASpaceDateEnums[3];
        } else if (returnATValue) {
            return atValue;
        } else {
            return UNMAPPED;
        }
    }

    /**
     * Method to return the ASpace date era
     *
     * @param atValue
     * @return
     */
    public String getASpaceDateEra(String atValue) {
        if(atValue == null || atValue.isEmpty()) return "";

        atValue = atValue.toLowerCase();

        if(atValue.contains("ce")) {
            return ASpaceDateEnums[15];
        } else if (returnATValue) {
            return atValue;
        } else {
            return UNMAPPED;
        }
    }

    /**
     * Map the aspace calender
     *
     * @param atValue
     * @return
     */
    public String getASpaceDateCalender(String atValue) {
        if(atValue == null || atValue.isEmpty()) return "";

        atValue = atValue.toLowerCase();

        if(atValue.contains("gregorian")) {
            return ASpaceDateEnums[16];
        } else if (returnATValue) {
            return atValue;
        } else {
            return UNMAPPED;
        }
    }

    /**
     * Method to initASpaceialize array that holds enums of collection management records
     */
    private void initASpaceCollectionManagementRecordEnums() {
        ASpaceCollectionManagementRecordEnums = new String[] {
                "high",         // 0
                "medium",       // 1
                "low",          // 2
                "new",          // 3
                "in_progress",  // 4
                "completed"     // 5
        };
    }

    /**
     * Map an AR value to a collection management record enum
     *
     * @param arID
     * @return
     */
    public String getASpaceCollectionManagementRecordProcessingPriority(int arID) {
        String key = "processing_priority_" + arID;
        return getEnumValueForID(key, "low");
    }

    /**
     * Map an AT value to a collection management record enum
     * @param atValue
     * @return
     */
    public String getASpaceCollectionManagementRecordProcessingStatus(String atValue) {
        atValue = atValue.toLowerCase();

        if (atValue.contains("new")) {
            return ASpaceCollectionManagementRecordEnums[3];
        } else if(atValue.contains("in progress")) {
            return ASpaceCollectionManagementRecordEnums[4];
        } else if(atValue.contains("processed")) {
            return ASpaceCollectionManagementRecordEnums[5];
        } else if(returnATValue) {
            return atValue;
        } else {
            return UNMAPPED;
        }
    }

    /**
     * Method to initialize array that holds the name linked function roles
     */
    private void initASpaceLinkedAgentRole() {
        ASpaceLinkedAgentRoles = new String[] {
                "creator",  // 0
                "source",   // 1
                "subject"   // 2
        };
    }

    /**
     * Method to map the AT name link function to ASpace linked agent role
     *
     * @param atValue
     * @return
     */
    public String getASpaceLinkedAgentRole(String atValue) {
        if(atValue == null || atValue.isEmpty()) return "";

        atValue = atValue.toLowerCase();

        if (atValue.contains("creator")) {
            return ASpaceLinkedAgentRoles[0];
        } else if(atValue.contains("source")) {
            return ASpaceLinkedAgentRoles[1];
        } else if(atValue.contains("subject")) {
            return ASpaceLinkedAgentRoles[2];
        } else if(returnATValue) {
            return atValue;
        } else {
            return UNMAPPED;
        }
    }

    /**
     * Method to return the link agent relator
     * @param atValue
     * @return
     */
    public String getASpaceLinkedAgentRelator(String atValue) {
        if(atValue == null || atValue.isEmpty()) return "";

        if(nameLinkCreatorCodes.containsKey(atValue)) {
            return nameLinkCreatorCodes.get(atValue);
        } else {
            return UNMAPPED;
        }
    }

    /**
     * Method to return the aspace language code
     *
     * @param arId
     * @return
     */
    public String getASpaceLanguageCodeForArchonID(String arId) {
        if(arId.isEmpty()) return "und";

        if(languagesJS.has(arId)) {
            try {
                JSONObject languageJS = languagesJS.getJSONObject(arId);
                return languageJS.getString("ASpaceCode");
            } catch (JSONException e) {
                return "und";
            }
        } else {
            return "und";
        }
    }

    /**
     * Method to return the ASpace language code when given the Archon 3 letter code.
     * For most language codes this is the same code for each, but not for all.
     *
     * @param languageShort
     * @return
     */
    public String getASpaceLanguageCodeForArchonCode(String languageShort) {
        if(languageShort.isEmpty()) return "und";

        if(textLanguagesJS.has(languageShort)) {
            try {
                JSONObject languageJS = textLanguagesJS.getJSONObject(languageShort);
                return languageJS.getString("ASpaceCode");
            } catch (JSONException e) {
                return "und";
            }
        } else {
            return "und";
        }
    }

    /**
     * Method to return the long version of the language name
     *
     * @param langaugeShort
     * @return
     */
    public String getLanguageLong(String languageShort) {
        if(languageShort.isEmpty())  return "undefined";

        if(textLanguagesJS.has(languageShort)) {
            try {
                JSONObject languageJS = textLanguagesJS.getJSONObject(languageShort);
                return languageJS.getString("LanguageLong");
            } catch (JSONException e) {
                return "undedfined";
            }
        } else {
            return "undefined";
        }
    }

    /**
     * Method to initialize the array that holds the digital object types
     */
    private void initASpaceDigitalObjectType() {
        ASpaceDigitalObjectTypes = new String[]{
                "cartographic",                 // 0
                "mixed_materials",              // 1
                "moving_image",                 // 2
                "notated_music",                // 3
                "software_multimedia",          // 4
                "sound_recording",              // 5
                "sound_recording_musical",      // 6
                "sound_recording_nonmusical",   // 7
                "still_image",                  // 8
                "text"                          // 9
        };
    }

    /**
     * Method to return the type of the digital object
     *
     * @param atValue
     * @return
     */
    public String getASpaceDigitalObjectType(String atValue) {
        if(atValue == null || atValue.isEmpty()) return "";

        if (atValue.contains("cartographic")) {
            return ASpaceDigitalObjectTypes[0];
        } else if(atValue.contains("mixed material")) {
            return ASpaceDigitalObjectTypes[1];
        } else if(atValue.contains("moving image")) {
            return ASpaceDigitalObjectTypes[2];
        } else if(atValue.contains("notated music")) {
            return ASpaceDigitalObjectTypes[3];
        } else if(atValue.contains("software, multimedia")) {
            return ASpaceDigitalObjectTypes[4];
        } else if(atValue.equals("sound recording")) {
            return ASpaceDigitalObjectTypes[5];
        } else if(atValue.contains("sound recording-musical")) {
            return ASpaceDigitalObjectTypes[6];
        } else if(atValue.contains("sound recording-nonmusical")) {
            return ASpaceDigitalObjectTypes[7];
        } else if(atValue.contains("still image")) {
            return ASpaceDigitalObjectTypes[8];
        } else if(atValue.contains("text")) {
            return ASpaceDigitalObjectTypes[9];
        } else if(returnATValue) {
            return atValue;
        } else {
            return UNMAPPED;
        }
    }

    /**
     * Since AT doesn't have this just return collection
     *
     * @param atValue
     * @return
     */
    public String getASpaceDigitalObjectLevel(String atValue) {
        if(atValue == null) {
            return UNMAPPED;
        } else {
            return "collection";
        }
    }

    /**
     * Method to initialize the use statements
     */
    private void initASpaceFileVersionUseStatements() {
        ASpaceFileVersionUseStatements = new String[] {
                "audio-clip",           // 0
                "audio-master",         // 1
                "audio-master-edited",  // 2
                "audio-service",        // 3
                "audio-streaming",      // 4
                "image-master",         // 5
                "image-master-edited",  // 6
                "image-service",        // 7
                "image-service-edited", // 8
                "image-thumbnail",      // 9
                "text-codebook",        // 10
                "text-data",            // 11
                "text-data_definition", // 12
                "text-georeference",    // 13
                "text-ocr-edited",      // 14
                "text-ocr-unedited",    // 15
                "text-tei-transcripted",    // 16
                "text-tei-translated",      // 17
                "video-clip",               // 18
                "video-master",             // 19
                "video-master-edited",      // 20
                "video-service",            // 21
                "video-streaming"           // 22
        };
    }

    /**
     * Map the AT use statements to ASpace equivalents
     *
     * @param atValue
     * @return
     */
    public String getASpaceFileVersionUseStatement(String atValue) {
        if(atValue == null || atValue.isEmpty()) return "";

        atValue = atValue.toLowerCase();

        if(atValue.equals("audio-clip")) {
            return ASpaceFileVersionUseStatements[0];
        } else if(atValue.equals("audio-master")) {
            return ASpaceFileVersionUseStatements[1];
        } else if(atValue.equals("audio-master-edited")) {
            return ASpaceFileVersionUseStatements[2];
        } else if(atValue.equals("audio-service")) {
            return ASpaceFileVersionUseStatements[3];
        } else if(atValue.equals("audio-streaming")) {
            return ASpaceFileVersionUseStatements[4];
        } else if(atValue.equals("image-master")) {
            return ASpaceFileVersionUseStatements[5];
        } else if(atValue.equals("image-master-edited")) {
            return ASpaceFileVersionUseStatements[6];
        } else if(atValue.equals("image-service")) {
            return ASpaceFileVersionUseStatements[7];
        } else if(atValue.equals("image-service-edited")) {
            return ASpaceFileVersionUseStatements[8];
        } else if(atValue.equals("image-thumbnail")) {
            return ASpaceFileVersionUseStatements[9];
        } else if(atValue.equals("text-codebook")) {
            return ASpaceFileVersionUseStatements[10];
        } else if(atValue.equals("text-data")) {
            return ASpaceFileVersionUseStatements[11];
        } else if(atValue.equals("text-data_definition")) {
            return ASpaceFileVersionUseStatements[12];
        } else if(atValue.equals("text-georeference")) {
            return ASpaceFileVersionUseStatements[13];
        } else if(atValue.equals("text-ocr-edited")) {
            return ASpaceFileVersionUseStatements[14];
        } else if(atValue.equals("text-ocr-unedited")) {
            return ASpaceFileVersionUseStatements[15];
        } else if(atValue.equals("text-tei-transcripted")) {
            return ASpaceFileVersionUseStatements[16];
        } else if(atValue.equals("text-tei-translated")) {
            return ASpaceFileVersionUseStatements[17];
        } else if(atValue.equals("video-clip")) {
            return ASpaceFileVersionUseStatements[18];
        } else if(atValue.equals("video-master")) {
            return ASpaceFileVersionUseStatements[19];
        } else if(atValue.equals("video-master-edited")) {
            return ASpaceFileVersionUseStatements[20];
        } else if(atValue.equals("video-service")) {
            return ASpaceFileVersionUseStatements[21];
        } else if(atValue.equals("video-streaming")) {
            return ASpaceFileVersionUseStatements[22];
        } else if(returnATValue) {
            return atValue;
        } else {
            return UNMAPPED;
        }
    }

    /**
     * Method to initASpaceialize array containing ASpace note types
     */
    private void initASpaceNoteTypes() {
        ASpaceNoteTypes = new String[] {
                "abstract",                             // 0
                "accruals",                             // 1
                "appraisal",                            // 2
                "arrangement",                          // 3
                "bioghist",                             // 4
                "accessrestrict",                       // 5
                "userestrict",                          // 6
                "custodhist",                           // 7
                "dimensions",                           // 8
                "edition",                              // 9
                "extent",                               // 10
                "altformavail",                         // 11
                "originalsloc",                         // 12
                "fileplan",                             // 13
                "note",                                 // 14
                "physdesc",                             // 15
                "acqinfo",                              // 16
                "inscription",                          // 17
                "langmaterial",                         // 18
                "legalstatus",                          // 19
                "physloc",                              // 20
                "materialspec",                         // 21
                "otherfindaid",                         // 22
                "phystech",                             // 23
                "physdesc",                             // 24
                "physfacet",                            // 25
                "prefercite",                           // 26
                "processinfo",                          // 27
                "relatedmaterial",                      // 28
                "relatedmaterial",                      // 29
                "scopecontent",                         // 30
                "separatedmaterial",                    // 31
                "summary",                              // 32
                "odd",                                  // 33
        };
    }

    /**
     * Method to return an ASpace digital object note type
     *
     * @param atValue
     * @return
     */
    public String getASpaceDigitalObjectNoteType(String atValue) {
        if(atValue == null || atValue.isEmpty()) return ASpaceNoteTypes[14];

        atValue = atValue.toLowerCase();

        if(atValue.contains("biographical/historical")) {
            return ASpaceNoteTypes[4];
        } else if(atValue.contains("conditions governing access")) {
            return ASpaceNoteTypes[5];
        } else if(atValue.contains("conditions governing use")) {
            return ASpaceNoteTypes[6];
        } else if(atValue.contains("custodial history")) {
            return ASpaceNoteTypes[7];
        } else if(atValue.contains("dimensions")) {
            return ASpaceNoteTypes[8];
        } else if(atValue.contains("existence and location of copies")) {
            return ASpaceNoteTypes[11];
        } else if(atValue.contains("existence and location of originals")) {
            return ASpaceNoteTypes[12];
        } else if(atValue.contains("general note")) {
            return ASpaceNoteTypes[14];
        } else if(atValue.contains("general physical description")) {
            return ASpaceNoteTypes[24];
        } else if(atValue.contains("immediate source of acquisition")) {
            return ASpaceNoteTypes[16];
        } else if(atValue.contains("language of materials")) {
            return ASpaceNoteTypes[18];
        } else if(atValue.contains("legal status")) {
            return ASpaceNoteTypes[19];
        } else if(atValue.contains("preferred citation")) {
            return ASpaceNoteTypes[26];
        } else if(atValue.contains("processing information")) {
            return ASpaceNoteTypes[27];
        } else if(atValue.contains("related archival materials")) {
            return ASpaceNoteTypes[29];
        } else {
            return ASpaceNoteTypes[14];  // just tag this note as a note type
        }
    }

    /**
     * Method to map the AT multipart note type to the ASpace equivalence
     *
     * @param atValue
     * @return
     */
    public String getASpaceMultiPartNoteType(String atValue) {
        if(atValue == null || atValue.isEmpty()) return "";

        atValue = atValue.toLowerCase();

        if(atValue.contains("accruals")) {
            return ASpaceNoteTypes[1];
        } else if(atValue.contains("appraisal")) {
            return ASpaceNoteTypes[2];
        } else if(atValue.contains("arrangement")) {
            return ASpaceNoteTypes[3];
        } else if(atValue.contains("biographical/historical")) {
            return ASpaceNoteTypes[4];
        } else if(atValue.contains("conditions governing access")) {
            return ASpaceNoteTypes[5];
        } else if(atValue.contains("conditions governing use")) {
            return ASpaceNoteTypes[6];
        } else if(atValue.contains("custodial history")) {
            return ASpaceNoteTypes[7];
        } else if(atValue.contains("dimensions")) {
            return ASpaceNoteTypes[8];
        } else if(atValue.contains("existence and location of copies")) {
            return ASpaceNoteTypes[11];
        } else if(atValue.contains("existence and location of originals")) {
            return ASpaceNoteTypes[12];
        } else if(atValue.contains("file plan")) {
            return ASpaceNoteTypes[13];
        } else if(atValue.contains("general note")) {
            return ASpaceNoteTypes[33];
        } else if(atValue.contains("immediate source of acquisition")) {
            return ASpaceNoteTypes[16];
        } else if(atValue.contains("legal status")) {
            return ASpaceNoteTypes[19];
        } else if(atValue.contains("other finding aids")) {
            return ASpaceNoteTypes[22];
        } else if(atValue.contains("physical characteristics and technical requirements")) {
            return ASpaceNoteTypes[23];
        } else if(atValue.contains("preferred citation")) {
            return ASpaceNoteTypes[26];
        } else if(atValue.contains("processing information")) {
            return ASpaceNoteTypes[27];
        } else if(atValue.contains("related archival materials")) {
            return ASpaceNoteTypes[28];
        } else if(atValue.contains("scope and contents")) {
            return ASpaceNoteTypes[30];
        } else if(atValue.contains("separated materials")) {
            return ASpaceNoteTypes[31];
        } else {
            return UNMAPPED;
        }
    }

    /**
     * Method to map the AT single part note type to the ASpace equivalence
     *
     * @param atValue
     * @return
     */
    public String getASpaceSinglePartNoteType(String atValue) {
        if(atValue == null || atValue.isEmpty()) return "";

        atValue = atValue.toLowerCase();

        if (atValue.contains("abstract")) {
            return ASpaceNoteTypes[0];
        } else if(atValue.contains("general physical description")) {
            return ASpaceNoteTypes[15];
        } else if(atValue.contains("language of materials")) {
            return ASpaceNoteTypes[18];
        } else if(atValue.contains("location note")) {
            return ASpaceNoteTypes[20];
        } else if(atValue.contains("material specific details")) {
            return ASpaceNoteTypes[21];
        } else if(atValue.contains("physical facet")) {
            return ASpaceNoteTypes[25];
        } else {
            return UNMAPPED;
        }
    }

    /**
     * Method to return the enumeration value for the order list note
     *
     * @param atValue
     * @return
     */
    public String getASpaceOrderedListNoteEnumeration(String atValue) {
        if(atValue == null || atValue.isEmpty()) {
            return "null";
        } else {
            return atValue;
        }
    }

    /**
     * Initialize the array that stores resource levels
     */
    private void initASpaceResourceLevels() {
        ASpaceResourceLevels = new String[] {
                "class",            // 0
                "collection",       // 1
                "file",             // 2
                "fonds",            // 3
                "item",             // 4
                "otherlevel",       // 5
                "recordgrp",        // 6
                "series",           // 7
                "subfonds",         // 8
                "subgrp",           // 9
                "subseries"         // 10
        };
    }

    /**
     * Method to map the AT resource level to ASpace equivalent
     *
     * @param atValue
     * @return
     */
    public String getASpaceResourceLevel(String atValue) {
        if(atValue == null || atValue.isEmpty()) return "collection";

        atValue = atValue.toLowerCase();

        if (atValue.contains("class")) {
            return ASpaceResourceLevels[0];
        } else if(atValue.contains("collection")) {
            return ASpaceResourceLevels[1];
        } else if(atValue.contains("file")) {
            return ASpaceResourceLevels[2];
        } else if(atValue.contains("fonds")) {
            return ASpaceResourceLevels[3];
        } else if(atValue.contains("item")) {
            return ASpaceResourceLevels[4];
        } else if(atValue.contains("otherlevel")) {
            return ASpaceResourceLevels[5];
        } else if(atValue.contains("recordgrp")) {
            return ASpaceResourceLevels[6];
        } else if(atValue.equals("series")) {
            return ASpaceResourceLevels[7];
        } else if(atValue.contains("subfonds")) {
            return ASpaceResourceLevels[8];
        } else if(atValue.contains("subgrp")) {
            return ASpaceResourceLevels[9];
        } else if(atValue.contains("subseries")) {
            return ASpaceResourceLevels[10];
        } else {
            return UNMAPPED;
        }
    }

    /**
     * Method to map the AT resource component level to ASpace equivalent
     *
     * @param atValue
     * @return
     */
    public String getASpaceArchivalObjectLevel(String atValue) {
        return getASpaceResourceLevel(atValue);
    }

    /**
     * Method to initASpace array that holds the description rules
     */
    private void initASpaceFindingAidDescriptionRules() {
        ASpaceFindingAidDescriptionRules = new String[] {
                "aacr", // 0
                "cco",  // 1
                "dacs", // 2
                "rad",  // 3
                "isadg" // 4
        };
    }

    /**
     * Method to return the map find description
     *
     * @param value
     * @return
     */
    public String getASpaceFindingAidDescriptionRule(Integer value) {
        if (value == 1) {
            return ASpaceFindingAidDescriptionRules[2];
        } else if(value == 2) {
            return ASpaceFindingAidDescriptionRules[0];
        } else if(value == 3) {
            return ASpaceFindingAidDescriptionRules[3];
        } else if(value == 4) {
            return ASpaceFindingAidDescriptionRules[4];
        } else {
            return UNMAPPED;
        }
    }

    /**
     * Method to initASpace the finding aid status
     */
    private void initASpaceFindingAidStatus() {
        ASpaceFindingAidStatus = new String[] {
                "completed",        // 0
                "in_progress",      // 1
                "under_revision",   // 2
                "unprocessed"       // 3
        };
    }

    /**
     * Method to return the finding aid status
     *
     * @param atValue
     * @return
     */
    public String getASpaceFindingAidStatus(String atValue) {
        atValue = atValue.toLowerCase();

        if (atValue.contains("completed")) {
            return ASpaceFindingAidStatus[0];
        } else if(atValue.contains("in_process")) {
            return ASpaceFindingAidStatus[1];
        } else if(atValue.contains("under_revision")) {
            return ASpaceFindingAidStatus[2];
        } else if(atValue.contains("unprocessed")) {
            return ASpaceFindingAidStatus[3];
        } else if (returnATValue) {
            return atValue;
        } else {
            return UNMAPPED;
        }
    }

    /**
     * Method to initialize the ASpace instance types
     */
    private void initASpaceInstanceTypes() {
        ASpaceInstanceTypes = new String[] {
                "audio",                // 0
                "books",                // 1
                "computer_disks",       // 2
                "graphic_materials",    // 3
                "maps",                 // 4
                "microform",            // 5
                "mixed_materials",      // 6
                "moving_images",        // 7
                "realia",               // 8
                "text",                 // 9
                "digital_object"        // 10
        };
    }

    /**
     * Method to return the equivalent ASpace instance type
     *
     * @param atValue
     * @return
     */
    public String getASpaceInstanceType(String atValue) {
        if(atValue == null || atValue.isEmpty()) return "";

        atValue = atValue.toLowerCase();

        if(atValue.contains("audio")) {
            return ASpaceInstanceTypes[0];
        } else if(atValue.contains("books")) {
            return ASpaceInstanceTypes[1];
        } else if(atValue.contains("computer disks")) {
            return ASpaceInstanceTypes[2];
        } else if(atValue.contains("digital object")) {
            return ASpaceInstanceTypes[10];
        } else if(atValue.contains("graphic materials")) {
            return ASpaceInstanceTypes[3];
        } else if(atValue.contains("maps")) {
            return ASpaceInstanceTypes[4];
        } else if(atValue.contains("microform")) {
            return ASpaceInstanceTypes[5];
        } else if(atValue.contains("mixed materials")) {
            return ASpaceInstanceTypes[6];
        } else if(atValue.contains("moving Images")) {
            return ASpaceInstanceTypes[7];
        } else if(atValue.contains("realia")) {
            return ASpaceInstanceTypes[8];
        } else if(atValue.contains("text")) {
            return ASpaceInstanceTypes[9];
        } else if(returnATValue) {
            return atValue;
        } else {
            return UNMAPPED;
        }
    }

    /**
     * Method to initASpace array holding the ASpace container types
     */
    private void initASpaceInstanceContainerTypes() {
        ASpaceInstanceContainerTypes = new String[] {
                "box",      // 0
                "carton",   // 1
                "case",     // 2
                "folder",   // 3
                "frame",    // 4
                "object",   // 5
                "page",     // 6
                "reel",     // 7
                "volume"    // 8
        };
    }

    /**
     * Method to return the equivalent ASpace instance container type
     *
     * if statements with "&& returnATValue" should really be removed, but depending on if the
     * enum is ASpace is expanded then this will save some work
     *
     * @param arID
     * @return
     */
    public String getASpaceInstanceContainerType(String arID) {
        if(arID == null || arID.isEmpty()) return "";

        String key = "container_types_" + arID;
        return getEnumValueForID(key, "");
    }

    /**
     * Method to init the aspace term types
     */
    private void initASpaceAcquisitionTypes() {
        ASpaceAcquisitionTypes = new String[] {
                "deposit",  // 0
                "gift",     // 1
                "purchase", // 2
                "transfer"  // 3
        };
    }

    /**
     * Method to get the ASpace type
     *
     * @param atValue
     * @return
     */
    public String getASpaceAcquisitionType(String atValue) {
        if(atValue == null || atValue.isEmpty()) return "";

        atValue = atValue.toLowerCase();

        if (atValue.contains("deposit")) {
            return ASpaceAcquisitionTypes[0];
        } else if(atValue.contains("gift")) {
            return ASpaceAcquisitionTypes[1];
        } else if(atValue.contains("purchase")) {
            return ASpaceAcquisitionTypes[2];
        } else if(atValue.contains("transfer")) {
            return ASpaceAcquisitionTypes[3];
        } else if(returnATValue) {
            return atValue;
        } else {
            return UNMAPPED;
        }
    }

    /**
     * Method to init the ASpace accession resource type
     */
    private void initASpaceAccessionResourceTypes() {
        ASpaceAccessionResourceTypes = new String[] {
            "collection",
            "papers",
            "publications",
            "records"
        };
    }

    /**
     * Method to return the AccessionResourceType
     *
     * @param atValue
     * @return
     */
    public String getASpaceAccessionResourceType(String atValue) {
        if(atValue == null || atValue.isEmpty()) return "collection";

        atValue = atValue.toLowerCase();

        if (atValue.equals("collection")) {
            return ASpaceAccessionResourceTypes[0];
        } else if(atValue.equals("papers")) {
            return ASpaceAccessionResourceTypes[1];
        } else if(atValue.equals("records")) {
            return ASpaceAccessionResourceTypes[3];
        } else if(returnATValue) {
            if(validResourceTypes.contains(atValue)) {
                return atValue;
            } else {
                return "collection";
            }
        } else {
            return UNMAPPED;
        }
    }

    /**
     * Method to set the hash map that holds the dynamic enums
     *
     * @param dynamicEnums
     */
    public void setASpaceDynamicEnums(HashMap<String, JSONObject> dynamicEnums) {
        this.dynamicEnums = dynamicEnums;

        // now extract the language codes, and salutations so we can compare with the AT values
        JSONObject languageCodeEnumJS = dynamicEnums.get("language_iso639_2");
        JSONObject salutationEnumJS = dynamicEnums.get("agent_contact_salutation");
    }

    /**
     * Method to return the correct enum list based on the AT list name. Only editable list in the
     * Archon are returned
     *
     * @param listName
     * @return
     */
    public ArrayList<JSONObject> getDynamicEnum(String listName) throws Exception {
        JSONObject dynamicEnum = null;
        JSONObject dynamicEnum2 = null;

        if (listName.contains("countries")) {
            dynamicEnum = dynamicEnums.get("country_iso_3166");
            dynamicEnum.put("valueKey", "ISOAlpha2");
            dynamicEnum.put("idPrefix", "country");
            dynamicEnum.put("keepValueCase", true);
        } else if (listName.contains("creatorsources")) {
            dynamicEnum = dynamicEnums.get("name_source");
            dynamicEnum.put("valueKey", "SourceAbbreviation");
            dynamicEnum.put("idPrefix", "name_source");
        } else if (listName.contains("extentunits")) {
            dynamicEnum = dynamicEnums.get("extent_extent_type");
            dynamicEnum.put("valueKey", "ExtentUnit");
            dynamicEnum.put("idPrefix", "extent_type");
        } else if (listName.contains("containertypes")) {
            dynamicEnum = dynamicEnums.get("container_type");
            dynamicEnum.put("valueKey", "ContainerType");
            dynamicEnum.put("idPrefix", "container_types");
        } else if (listName.contains("accessiontypes")) {
            dynamicEnum = dynamicEnums.get("accession_resource_type");
            dynamicEnum.put("valueKey", "MaterialType");
            dynamicEnum.put("idPrefix", "accession_type");
        } else if (listName.contains("materialtypes")) {
            dynamicEnum = dynamicEnums.get("resource_resource_type");
            dynamicEnum.put("valueKey", "MaterialType");
            dynamicEnum.put("idPrefix", "resource_type");
        } else if (listName.contains("filetypes")) {
            dynamicEnum = dynamicEnums.get("file_version_file_format_name");
            dynamicEnum.put("valueKey", "FileType");
            dynamicEnum.put("idPrefix", "file_type");
        } else if (listName.contains("processingpriorities")) {
            dynamicEnum = dynamicEnums.get("collection_management_processing_priority");
            dynamicEnum.put("valueKey", "ProcessingPriority");
            dynamicEnum.put("idPrefix", "processing_priority");
        } else if (listName.contains("subjectsources")) {
            dynamicEnum = dynamicEnums.get("subject_source");
            dynamicEnum.put("valueKey", "EADSource");
            dynamicEnum.put("idPrefix", "subject_source");
            // since some subjects get mapped to agents we need the Archon's subject sources for agents as well as subjects
            dynamicEnum2 = dynamicEnums.get("name_source");
            dynamicEnum2.put("valueKey", "EADSource");
            dynamicEnum2.put("idPrefix", "name_subject_source");
        }

        ArrayList<JSONObject> dynamicEnums = new ArrayList<JSONObject>();
        dynamicEnums.add(dynamicEnum);
        dynamicEnums.add(dynamicEnum2);

        return dynamicEnums;
    }

    /**
     * Method to add a code and a value the code to value data map
     *
     * @param id This is the unique id of the enum value
     * @param value
     */
    public void addIdAndValueToEnumList(String id, String value) {
        enumListIDsToValues.put(id, value);
    }

    /**
     * Method to return an archon enum value given the ID
     *
     * @param key
     * @param defaultValue
     * @return
     */
    public String getEnumValueForID(String key, String defaultValue) {
        if(enumListIDsToValues.containsKey(key)) {
            return enumListIDsToValues.get(key);
        } else {
            return defaultValue;
        }
    }

    // used for testing
    public static void main(String[] args) {
        ASpaceEnumUtil enumUtil = new ASpaceEnumUtil();
    }
}
