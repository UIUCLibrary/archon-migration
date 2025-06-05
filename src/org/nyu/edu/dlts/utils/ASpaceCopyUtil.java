package org.nyu.edu.dlts.utils;

import org.apache.commons.httpclient.NameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.nyu.edu.dlts.utils.uiuc.UIUCPropertiesReader;

import javax.swing.*;
import java.io.File;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: Nathan Stevens
 *
 * Date: 9/5/12
 * Time: 1:48 PM
 * Utility class for copying data from the Archon to Archivesspace
 */
public class ASpaceCopyUtil implements  PrintConsole {

    // String to indicate when no ids where return from aspace backend
    private final String NO_ID = "no id assigned";

    // used to create the Archive Space JSON data
    private ASpaceMapper mapper;

    // The utility class used to map to ASpace Enums
    private ASpaceEnumUtil enumUtil;

    // used to make REST calls to archive space backend service
    private ASpaceClient aspaceClient = null;

    // used make http connection to archon
    private ArchonClient archonClient = null;

    // used to store information about the archives space backend
    private String aspaceInformation = "Simulator";

    private String aspaceVersion = "";

    // hashmap that maps repository from old database with copy in new database
    private HashMap<String, String> repositoryURIMap = new HashMap<String, String>();

    // hasmap to store the repo agents for use in creating event objects
    private HashMap<String, String> repositoryAgentURIMap = new HashMap<String, String>();

    // hashmap that stores the repository groups from the archive space database
    private HashMap<String, JSONObject> repositoryGroupURIMap = new HashMap<String, JSONObject>();

    // store the repository names and Archon ID for debugging purposes
    private ArrayList<String> repositoryNamesList = new ArrayList<String>();

    // hashmap that maps location from the old database with copy in new database
    private HashMap<String, String> locationURIMap = new HashMap<String, String>();

    // hashmap that maps subjects from old database with copy in new database
    private HashMap<String, String> subjectURIMap = new HashMap<String, String>();

    // hashmap that maps names from old database with copy in new database
    private HashMap<String, String> nameURIMap = new HashMap<String, String>();

    // hashmap that maps classification from old database with copy in new database
    private HashMap<String, String> classificationURIMap = new HashMap<String, String>();

    // hashmap that maps classification term from old database with copy in new database
    private HashMap<String, String> classificationTermURIMap = new HashMap<String, String>();

    // hashmap that maps classification and classification term ids to make it easy to construct
    private HashMap<String, String> classificationIdentifiers = new HashMap<String, String>();
    private HashMap<String, String> classificationParents = new HashMap<String, String>();

    // hashset to keep track of classifications ids which have been linked to a record
    // so un-liked classifications can be clean deleted
    private HashSet<String> linkedClassificationSet = new HashSet<String>();

    // hashmap that maps accessions from old database with copy in new database
    private HashMap<String, String> accessionURIMap = new HashMap<String, String>();

    // a hash map to store the Repository for a particular accession
    private HashMap<String, String> accessionRepositoryIdMap = new HashMap<String, String>();

    private HashMap<String, ArrayList<String>> resourcesToAccessionsMap = new HashMap<String, ArrayList<String>>();

    // hashmap that maps digital objects from old database with copy in new database
    private HashMap<String, String> digitalObjectURIMap = new HashMap<String, String>();

    // hashmap that stores the converted digital objects so that they can be save to the correct repo
    // when saving the collection content
    private HashMap<Integer, HashMap<Integer, ArrayList<JSONArray>>> digitalObjectMap =
            new HashMap<Integer, HashMap<Integer, ArrayList<JSONArray>>>();

    // hashmap that maps resource from old database with copy in new database
    private HashMap<String, String> resourceURIMap = new HashMap<String, String>();

    // stores subject that need to be converted to agent records
    private ArrayList<JSONObject> subjectToAgentList = new ArrayList<JSONObject>();

    // stop watch object for keeping tract of time
    private StopWatch stopWatch = null;

    // specify debug the boolean
    private boolean debug = true;

    // random string generator to use when simulating rest calls
    private RandomString randomString = new RandomString(3);

    // specify the current record type and ID in case we have fetal error during migration
    private String currentRecordType = "";
    private String currentRecordIdentifier = "";
    private String currentRecordDBID = "";

    // integer to keep track of # of resource records copied
    private int copyCount = 1;

    // integer to keep track of total number of aspace client threads
    private int totalASpaceClients = 1;

    // integers to keeps track of the number of digital objects  and locations copied
    private int digitalObjectTotal = 0;
    private int digitalObjectSuccess = 0;
    private int locationTotal = 0;
    private int locationSuccess = 0;

    // boolean to specify weather to save digital objects by themselves or with the resource records
    boolean saveDigitalObjectsWithResources = true;

    // These fields are used to track of the number of messages posted to the output console
    // in order to prevent memory usage errors
    private int messageCount = 0;
    private final int MAX_MESSAGES = 100;

    // keep tract of the number of errors when converting and saving records
    private int saveErrorCount = 0;
    private int aspaceErrorCount = 0;

    private String resetPassword = "password";

    // this is used to output text to user when doing the data transfer
    private JTextArea outputConsole;

    // this are used to give user better feedback on progress
    private JProgressBar progressBar;
    private JLabel errorCountLabel;

    // used to specify the stop the copying process. Only get checked when copying resources
    private boolean stopCopy = false;

    // used to specified the the copying process is running
    private boolean copying = false;

    // An HashMap for storing the total number of main records transferred
    LinkedHashMap<String, String> recordTotals = new LinkedHashMap<String, String>();

    // Specifies whether or not to simulate the REST calls
    private boolean simulateRESTCalls = false;

    // Specifies whether to delete the previously saved resource records. Useful for testing purposes
    private boolean deleteSavedResources = false;

    // this list is used to copy a specific resource
    private ArrayList<String> collectionsIDsList;

    // this list is used to copy a specific resource
    private ArrayList<String> collArchonIDsList;

    // A string builder object to track errors
    private StringBuilder errorBuffer = new StringBuilder();

    // A string builder object to track data changes
    private StringBuilder changelogBuffer = new StringBuilder();

    // count the number of changes logged
    private int changelogCount = 0;

    private File recordDumpDirectory = null;

    private JSONObject cachedCollectionsJS;

    // the default repository id
    private String defaultRepositoryId;

    // variable to set the default instance type
    // todo: set using the GUI
    private String defaultInstanceType = "mixed_materials";

    // a hashmap for getting Archon enum IDs from enum values
    private HashMap<String, String> archonValuesToIDs = new HashMap<String, String>();

    /**
     * The main constructor, used when running as a stand alone application
     *
     * @param archonClient
     */
    public ASpaceCopyUtil(ArchonClient archonClient, String host, String admin, String adminPassword) {
        this.archonClient = archonClient;
        this.aspaceClient = new ASpaceClient(host, admin, adminPassword);
        init();
    }

    /**
     * Method to initiate certain variables that are needed to work
     */
    private void init() {
        print("Starting database copy ... ");

        // set the error buffer for the mapper
        mapper = new ASpaceMapper(this);

        // set the enum util to that of the mapper
        enumUtil = mapper.getEnumUtil();

        // first add the admin repo to the repository URI map
        repositoryURIMap.put("adminRepo", ASpaceClient.ADMIN_REPOSITORY_ENDPOINT);

        // set the print console object so we have something to print out while the
        // records are being read in batches of 100
        archonClient.setPrintConsole(this);

        // start the stop watch object so we can see how long this data transfer takes
        startWatch();
    }

    /**
     * Method to set the reset password when copying user records
     *
     * @param resetPassword
     */
    public void setResetPassword(String resetPassword) {
        this.resetPassword = resetPassword;
    }

    /**
     * Method to set the base uri for digital objects
     * @param baseURI
     */
    public void setDigitalObjectBaseURI(String baseURI) {
        if(!baseURI.endsWith("/")) {
            baseURI = baseURI + "/";
        }
        mapper.setDigitalObjectBaseURI(baseURI);
    }

    
    /**
     * Method to set the default instance type
     *
     * @param instanceType
     */
    public void setDefaultInstanceType(String instanceType) {
        //todo: check that the string is a valid instance type for aspace
        defaultInstanceType = instanceType;
    }

    /**
     * Method to set the output console
     *
     * @param outputConsole
     */
    public void setOutputConsole(JTextArea outputConsole) {
        this.outputConsole = outputConsole;
    }

    /**
     * Method to set the progress bar
     *
     * @param progressBar
     * @param errorCountLabel
     */
    public void setProgressIndicators(JProgressBar progressBar, JLabel errorCountLabel) {
        this.progressBar = progressBar;
        this.errorCountLabel = errorCountLabel;
    }

    /**
     * Set the option for either converting some bbcode to html
     *
     * @param option
     */
    public void setBBCodeOption(String option) {
        mapper.setBBCodeOption(option);
    }

    /**
     * Method to set the default repository id
     * @param id
     */
    public void setDefaultRepositoryId(String id) throws Exception {
        if(id.equals("Based On Linked Collection")) {
            defaultRepositoryId = "";
            findAccessionRecordRepositories();
        } else if(id.contains(" - ")) {
            String[] sa = id.split(" - ", 2);
            defaultRepositoryId = sa[0];
        } else {
            defaultRepositoryId = id;
        }
    }

    /**
     * Method to copy the archon enum records
     */
    public void copyEnumRecords() throws Exception {
        if(simulateRESTCalls) return;

        HashMap<String, JSONObject> dynamicEnums = aspaceClient.loadDynamicEnums();

        if(dynamicEnums == null) {
            print("Can't load ASpace dynamic enums ...");
            return;
        }

        updateProgress("Enum List", 0, 0);

        mapper.setASpaceDynamicEnums(dynamicEnums);

        JSONObject enumsJS = archonClient.getAllEnums();
        Iterator<String> keys = enumsJS.keys();

        int total = enumsJS.length();
        int count = 0;

        while(keys.hasNext()) {
            String enumEndpoint = keys.next();
            JSONObject enumList = enumsJS.getJSONObject(enumEndpoint);

            ArrayList<JSONObject> updatedEnums = mapper.mapEnumList(enumList, enumEndpoint);

            for (JSONObject updatedEnumJS : updatedEnums) {

                if (updatedEnumJS != null && !enumEndpoint.contains("enum_type=countries")) {
                    String endpoint = updatedEnumJS.getString("uri");
                    String jsonText = updatedEnumJS.toString();
                    String id = saveRecord(endpoint, jsonText, "EnumList->" + enumEndpoint);

                    if (!id.equalsIgnoreCase(NO_ID)) {
                        print("Copied Enum List Values: " + enumEndpoint + " :: " + id);

                        // since this list needs to be updated twice need to store updates to avoid conflict
                        if (updatedEnumJS.getString("name").equals("name_source")) {
                            dynamicEnums = aspaceClient.loadDynamicEnums();
                            mapper.setASpaceDynamicEnums(dynamicEnums);
                        }
                    }
                }
            }

            count++;
            updateProgress("Enum List", total, count);
        }

        // set this to be 100 percent since not all lookup will need to be migrated
        updateRecordTotals("Enum List", total, total);
    }

    /**
     * Method to copy the repository records
     *
     * @throws Exception
     */
    public void copyRepositoryRecords() throws Exception {
        print("Copying repository records ...");

        // update the progress bar to indicate loading of records
        updateProgress("Repositories", 0, 0);

        JSONObject records = archonClient.getRepositoryRecords();

        // A hashmap to map the short name to the repository URI to prevent duplications
        HashMap<String, String> shortNamesToURIMap = new HashMap<String, String>();

        // these are used to update the progress bar
        int total = records.length();
        int count = 0;
        int success = 0;

        Iterator<String> keys = records.keys();
        while(keys.hasNext()) {
            if(stopCopy) return;

            JSONObject repository = records.getJSONObject(keys.next());

            String shortName = repository.getString("Name");
            String repoID = repository.getString("ID");

            repositoryNamesList.add(shortName + ", ID: " + repoID);

            if (!shortNamesToURIMap.containsKey(shortName)) {
                String jsonText;
                String id;

                // get and save the agent object for the repository
                String agentURI = null;
                jsonText = mapper.getCorporateAgent(repository);
                id = saveRecord(ASpaceClient.AGENT_CORPORATE_ENTITY_ENDPOINT, jsonText, "Repository_Name_Corporate->" + shortName);

                if (!id.equalsIgnoreCase(NO_ID)) {
                    agentURI = ASpaceClient.AGENT_CORPORATE_ENTITY_ENDPOINT + "/" + id;
                }

                jsonText = mapper.convertRepository(repository, agentURI);
                id = saveRecord(ASpaceClient.REPOSITORY_ENDPOINT, jsonText, "Repository->" + shortName);

                if (!id.equalsIgnoreCase(NO_ID)) {
                    String uri = ASpaceClient.REPOSITORY_ENDPOINT + "/" + id;
                    repositoryURIMap.put(repoID, uri);
                    repositoryAgentURIMap.put(uri, agentURI);
                    shortNamesToURIMap.put(shortName, uri);

                    print("Copied Repository: " + shortName + " :: " + id);
                    success++;
                } else {
                    print("Fail -- Repository: " + shortName);
                }
            } else {
                repositoryURIMap.put(repoID, shortNamesToURIMap.get(shortName));

                print("Repository already in database " + shortName);
                success++;
            }

            count++;
            updateProgress("Repositories", total, count);
        }

        updateRecordTotals("Repositories", total, success);
    }

    /**
     * Method to load the repository groups, map them to access classes
     *
     * then place them in a hashmap
     */
    public void mapRepositoryGroups() {
        if(simulateRESTCalls) return;

        print("Mapping repository user group records ...");

        // update the progress bar to indicate loading of records
        updateProgress("Repository Groups", 0, 0);

        // these are used to update the progress bar
        int total = repositoryURIMap.size();
        int count = 0;

        // now load the groups for all repositories
        for(String repoURI: repositoryURIMap.values()) {
            JSONArray groupsJA = aspaceClient.loadRepositoryGroups(repoURI);

            // process each group and map it to an AT user class
            for(int i = 0; i < groupsJA.length(); i++) {
                try {
                    JSONObject groupJS = groupsJA.getJSONObject(i);

                    // if no members array exist add one
                    if(groupJS.isNull("member_usernames")) {
                        JSONArray membersJA = new JSONArray();
                        groupJS.put("member_usernames", membersJA);
                    }

                    // map this group to an AR access class
                    mapper.mapAccessClass(repositoryGroupURIMap, groupJS, repoURI);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                count++;
                updateProgress("Repository Groups", total, count);
            }
        }

        if(debug) {
            print("Number of groups mapped: " + repositoryGroupURIMap.size());
        }
    }

    /**
     * Copy the user records, remapping the repository
     *
     * @throws Exception
     */
    public void copyUserRecords() throws Exception {
        print("Copying User records ...");

        // update the progress bar here to indicate loading of records
        updateProgress("Users", 0, 0);

        JSONObject records = archonClient.getUserRecords();

        // these are used to update the progress bar
        int total = records.length();
        int count = 0;
        int success = 0;

        Iterator<String> keys = records.keys();
        while(keys.hasNext()) {
            if(stopCopy) return;

            JSONObject user = records.getJSONObject(keys.next());
            String userName = user.getString("DisplayName");

            // check to see if the is admin is set to true, otherwise continue
            if(user.getInt("IsAdminUser") == 0 || user.getString("Login").equals("admin")) {
                print("Non admin or duplicate user: " + userName + ", not migrating ...");
                continue;
            }

            // first get the group the user belongs too
            ArrayList<String> groupURIs = getUserGroupURIs(user);

            if(groupURIs == null) {
                print("Denied user: " + userName + ", not migrating ...");
                continue;
            }

            NameValuePair[] params = new NameValuePair[1 + groupURIs.size()];
            params[0] = new NameValuePair("password", resetPassword);

            for (int i = 0; i < groupURIs.size(); i++) {
                params[i + 1] = new NameValuePair("groups[]", groupURIs.get(i));
            }

            String jsonText = mapper.convertUser(user);
            String id = saveRecord(ASpaceClient.USER_ENDPOINT, jsonText, params, "User->" + userName);

            if (!id.equalsIgnoreCase(NO_ID)) {
                print("Copied User: " + userName + " :: " + id);
                success++;
            } else {
                print("Fail -- User: " + userName);
            }

            count++;
            updateProgress("Users", total, count);
        }

        updateRecordTotals("Users", total, success);

        // refresh the database connection to prevent heap space error
        freeMemory();
    }

    /**
     * Method to return the URI's of ASpace group(s) a user belongs to based on repository and access class
     *
     * It currently assigns users to a single group, but is designed to multiple groups if needed
     */
    public ArrayList<String> getUserGroupURIs(JSONObject user) throws Exception {
        ArrayList<String> groupURIs = new ArrayList<String>();

        // get the repository URIs the user belong to
        int repositoryLimit = user.getInt("RepositoryLimit");
        ArrayList<String> repoURIs = new ArrayList<String>();

        if(repositoryLimit == 1) {
            JSONArray repoIDs = user.getJSONArray("Repositories");
            for (int i = 0; i < repoIDs.length(); i++) {
                String repoID = repoIDs.getString(i);
                repoURIs.add(getRepositoryURI(repoID));
            }
        } else {
            for(String repoURI: repositoryURIMap.values()) {
                if (!repoURI.endsWith("/1")) {
                    repoURIs.add(repoURI);
                }
            }
        }

        // construct the key base on access class
        String key = "";
        JSONObject groupJS;

        // Get the correct access class by getting the lowest value user group number
        int accessClass = 10;

        JSONArray userGroupsJA = user.getJSONArray("Usergroups");
        for(int i = 0; i < userGroupsJA.length(); i++) {
            int userGroup = userGroupsJA.getInt(i);
            if(userGroup < accessClass) {
                accessClass = userGroup;
            }
        }

        // first check that this user access class prevents them from being
        if(accessClass == 5) {
            return null;
        } else if (accessClass == 1) {
            key = ASpaceClient.ADMIN_REPOSITORY_ENDPOINT + ASpaceMapper.ACCESS_CLASS_PREFIX + 1;
            groupJS = repositoryGroupURIMap.get(key);
            if (groupJS != null) {
                groupURIs.add(groupJS.getString("uri"));
            }
        } else {
            for (String repoURI : repoURIs) {
                key = repoURI + ASpaceMapper.ACCESS_CLASS_PREFIX + accessClass;
                groupJS = repositoryGroupURIMap.get(key);

                if (groupJS != null) {
                    groupURIs.add(groupJS.getString("uri"));
                }
            }
        }

        return groupURIs;
    }

    /**
     * Method to copy subject records
     * @throws Exception
     */
    public void copySubjectRecords() throws Exception {
        print("Copying Subject records ...");

        // update the progress so that the title changes
        updateProgress("Subjects", 0, 0);

        JSONObject records = archonClient.getSubjectRecords();

        int total = records.length();
        int success = 0;
        int count = 0;

        Iterator<String> keys = records.keys();
        while (keys.hasNext()) {
            if (stopCopy) return;

            String key = keys.next();

            JSONObject subject = records.getJSONObject(key);

            String arId = subject.getString("ID");
            int subjectTypeID = subject.getInt("SubjectTypeID");

            // check the subject type id since some of these subject need to be converted
            // to an agent record instead
            if((subjectTypeID == 3 || subjectTypeID == 8 || subjectTypeID == 10) && subject.getInt("ParentID") == 0) {
                subjectToAgentList.add(subject);
                subjectURIMap.put(arId, "agent");
                success++;
            } else {
                JSONObject json = mapper.convertSubject(subject);

                if(json != null) {
                    String id = saveRecord(ASpaceClient.SUBJECT_ENDPOINT, json.toString(), "Subject->" + subject.getString("Subject"));

                    if (!id.equalsIgnoreCase(NO_ID)) {
                        String uri = ASpaceClient.SUBJECT_ENDPOINT + "/" + id;
                        subjectURIMap.put(arId, uri);
                        print("Copied Subject: " + subject + " :: " + id);
                        success++;
                    } else {
                        print("Fail -- Subject: " + subject);
                    }
                } else {
                    print("Error Convert Subject Record to JSON: " + subject);
                }
            }

            count++;
            updateProgress("Subjects", total, count);

            String subjectTerm = subject.getString("Subject");
            System.out.println(count + " :: Subject Term: " + subjectTerm);
        }

        updateRecordTotals("Subjects", total, success);

        // try freeing some memory
        freeMemory();
    }

    /**
     * Copy the name records
     *
     * @throws Exception
     */
    public void copyCreatorRecords() throws Exception {
        print("Copying Creator records ...");

        // update the progress so that the title changes
        updateProgress("Creator", 0, 0);

        JSONObject records = archonClient.getCreatorRecords();

        // now add the subject which should be saved as name records
        addSubjectAgentsToCreatorRecords(records);

        // these are used to update the progress bar
        int total = records.length();
        int count = 0;
        int success = 0;

        // get the inverted keys
        String[] invertedKeys = getKeysInInvertedOrder(records);

        for(String key: invertedKeys) {
            if(stopCopy) return;

            JSONObject creator = records.getJSONObject(key);

            String arId = creator.getString("ID");

            int creatorTypeId = creator.getInt("CreatorTypeID");

            JSONObject agentJS = mapper.convertCreator(creator, creatorTypeId);

            if(agentJS == null) {
                print("Unknown Creator Type for: " + creator.getString("Name") + " :: " + arId);
                continue;
            }

            // based on the type of name copy to the correct location
            String id = "";
            String uri = "";

            switch (creatorTypeId) {
                case 19:
                case 21:
                case 23:
                    id = saveRecord(ASpaceClient.AGENT_PEOPLE_ENDPOINT, agentJS.toString(), "Creator_Person->" + creator.getString("Name"));
                    uri = ASpaceClient.AGENT_PEOPLE_ENDPOINT + "/" + id;
                    break;
                case 20:
                    id = saveRecord(ASpaceClient.AGENT_FAMILY_ENDPOINT, agentJS.toString(), "Creator_Family->" + creator.getString("Name"));
                    uri = ASpaceClient.AGENT_FAMILY_ENDPOINT + "/" + id;
                    break;
                case 22:
                    id = saveRecord(ASpaceClient.AGENT_CORPORATE_ENTITY_ENDPOINT, agentJS.toString(), "Creator_Corporate->" + creator.getString("Name"));
                    uri = ASpaceClient.AGENT_CORPORATE_ENTITY_ENDPOINT + "/" + id;
                    break;
                default:
                    break;
            }

            if (!id.equalsIgnoreCase(NO_ID)) {
                nameURIMap.put(arId, uri);
                print("Copied Creator: " + creator.get("Name") + " :: " + id);
                success++;
            } else {
                print("Failed -- Creator: " + creator.get("Name"));
            }

            count++;
            updateProgress("Names", total, count);
        }

        updateRecordTotals("Names", total, success);

        // now add the relationships between creators
        print("Adding related creator relationships ...");
        for (String key : invertedKeys) {
            JSONObject creator = records.getJSONObject(key);
            // add any relationships if needed
            if(creator.has("CreatorRelationships") && !creator.getString("CreatorRelationships").equals("null")) {
                try {
                    String arId = creator.getString("ID");
                    String uri = nameURIMap.get(arId);
                    JSONObject agentJS = new JSONObject(aspaceClient.get(uri, null));
                    addCreatorRelationShips(records, creator, agentJS);
                    saveRecord(uri, agentJS.toString(), "Creator->" + creator.getString("Name"));
                } catch(Exception e) {
                    print("Invalid Relationship Record For: " + creator.getString("Name"));
                }
            }
        }

        // refresh the database connection to prevent heap space error
        freeMemory();
    }

    /**
     * Method to add a creator relationship
     *
     * @param records
     * @param creator
     * @param agentJS
     */
    private void addCreatorRelationShips(JSONObject records, JSONObject creator, JSONObject agentJS) throws Exception {
        JSONArray relationships = creator.getJSONArray("CreatorRelationships");
        int creatorTypeID = creator.getInt("CreatorTypeID");

        JSONArray relatedAgentsJA = new JSONArray();

        for(int i = 0; i < relationships.length(); i++) {
            JSONObject relationship = relationships.getJSONObject(i);

            int creatorRelationshipTypeID = relationship.getInt("CreatorRelationshipTypeID");

            JSONObject relatedCreator = records.getJSONObject(relationship.getString("RelatedCreatorID"));

            String relatedCreatorID = relatedCreator.getString("ID");

            int relatedCreatorTypeID = relatedCreator.getInt("CreatorTypeID");
            String uri = nameURIMap.get(relatedCreatorID);

            if (uri == null) {
                System.out.println("Related creator not saved to ArchivesSpace, Missing related Agent URI ...");
                continue;
            }

            JSONObject agentRelationJS = new JSONObject();
            agentRelationJS.put("ref", uri);
            if (relationship.has("Description")) {
                String description = relationship.getString("Description");
                if (!description.equals("null")) agentRelationJS.put("description", relationship.getString("Description"));
            }

            // find the appropriate ASpace relationship type based on agent types and Archon relationship type
            switch (creatorRelationshipTypeID) {
                case 4:
                    // agent_relationship_earlierlater :: is_earlier_form_of
                    agentRelationJS.put("jsonmodel_type", "agent_relationship_earlierlater");
                    agentRelationJS.put("relator", "is_earlier_form_of");
                    break;
                case 5:
                    // agent_relationship_earlierlater :: is_later_form_of
                    agentRelationJS.put("jsonmodel_type", "agent_relationship_earlierlater");
                    agentRelationJS.put("relator", "is_later_form_of");
                    break;
                case 2:
                    if (creatorTypeID == 19 || creatorTypeID == 21 || creatorTypeID == 23) {
                        if (relatedCreatorTypeID == 19 || relatedCreatorTypeID == 21 || relatedCreatorTypeID == 23) {
                            // agent_relationship_parentchild :: is_parent_of
                            agentRelationJS.put("jsonmodel_type", "agent_relationship_parentchild");
                            agentRelationJS.put("relator", "is_parent_of");
                            break;
                        }
                    }
                    if (creatorTypeID == 22 && relatedCreatorTypeID == 22) {
                        // agent_relationship_subordinatesuperior :: is_superior_of
                        agentRelationJS.put("jsonmodel_type", "agent_relationship_subordinatesuperior");
                        agentRelationJS.put("relator", "is_superior_of");
                        break;
                    }
                case 3:
                    if (creatorTypeID == 19 || creatorTypeID == 21 || creatorTypeID == 23) {
                        if (relatedCreatorTypeID == 19 || relatedCreatorTypeID == 21 || relatedCreatorTypeID == 23) {
                            // agent_relationship_parentchild :: is_child_of
                            agentRelationJS.put("jsonmodel_type", "agent_relationship_parentchild");
                            agentRelationJS.put("relator", "is_child_of");
                            break;
                        }
                    }
                    if (creatorTypeID == 22 && relatedCreatorTypeID == 22) {
                        // agent_relationship_subordinatesuperior :: is_subordinate_to
                        agentRelationJS.put("jsonmodel_type", "agent_relationship_subordinatesuperior");
                        agentRelationJS.put("relator", "is_subordinate_to");
                        break;
                    }
                default:
                    // agent_relationship_associative :: is_associative_with
                    agentRelationJS.put("jsonmodel_type", "agent_relationship_associative");
                    agentRelationJS.put("relator", "is_associative_with");
            }

            relatedAgentsJA.put(agentRelationJS);

            System.out.println("Adding relationship " + relationship.toString());
        }

        if(relatedAgentsJA.length() > 0) {
            agentJS.put("related_agents", relatedAgentsJA);
        }
    }

    /**
     * Method to add subject agent record to the name record list
     * @param records
     */
    private void addSubjectAgentsToCreatorRecords(JSONObject records) throws Exception {
        for(JSONObject subject: subjectToAgentList) {
            int subjectTypeID = subject.getInt("SubjectTypeID");
            String sortName = subject.getString("Subject");
            String key = "subject_"  + subject.get("ID");

            subject.put("ID", key);
            subject.put("Name", sortName);

            if(subjectTypeID == 8) {
                // this is a person agent
                subject.put("CreatorTypeID", 19);
            } else if(subjectTypeID == 10) {
                // this is a family agent
                subject.put("CreatorTypeID", 20);
            } else {
                // must be a corperate agent
                subject.put("CreatorTypeID", 22);
            }

            records.put(key, subject);
        }
    }

    /**
     * Method to copy classification records
     *
     * @throws Exception
     */
    public void copyClassificationRecords() throws Exception {
        print("Copying Classification records ...");

        // update the progress so that the title changes
        updateProgress("Classifications", 0, 0);

        JSONObject records = archonClient.getClassificationRecords();

        int total = records.length();
        int success = 0;
        int count = 0;

        // the total repositories
        int repositoryCount = repositoryURIMap.size() - 1;

        Iterator<String> keys = records.keys();
        while (keys.hasNext()) {
            if (stopCopy) return;

            String key = keys.next();

            JSONObject classification = records.getJSONObject(key);

            String arId = classification.getString("ID");

            JSONObject classificationJS = mapper.convertClassification(classification);

            // add any creator record
            addCreator(classificationJS, classification);

            // copy this classification to each repository. The empty ones will be deleted
            // once all the records are copied over
            int copyCount = 0;
            for(String repoURI: repositoryURIMap.values()) {
                if (!repoURI.endsWith("/1")) {
                    copyCount += copyClassificationToRepository(repoURI, arId, classification, classificationJS);
                }
            }

            // increment the success variable only if we copied the classification records to each of the
            // repositories
            if(copyCount == repositoryCount) {
                success++;
            }

            count++;
            updateProgress("classification", total, count);

            String title = classification.getString("Title");
            System.out.println(count + " :: Classification: " + title + "/ ID: " + arId);
        }

        updateRecordTotals("Classification", total, success);

        // try freeing some memory
        freeMemory();
    }

    /**
     * Method to copy a classification record to a particular repository. This is only needed
     * because classifications from Archon are not tied to a particular repository, but in
     * ASpace they are. In principle they really shouldn't be.
     *
     * @param repoURI
     * @param arId
     * @param classification
     * @param classificationJS
     * @throws Exception
     */
    private int copyClassificationToRepository(String repoURI, String arId, JSONObject classification, JSONObject classificationJS) throws Exception {
        // create the batch import JSON array and dummy URI now
        JSONArray batchJA = new JSONArray();

        String batchEndpoint = repoURI + ASpaceClient.BATCH_IMPORT_ENDPOINT;
        String classificationURI = repoURI + ASpaceClient.CLASSIFICATION_ENDPOINT + "/" + arId;
        String classificationTitle = classification.getString("Title");

        if (classificationJS != null) {
            classificationJS.put("uri", classificationURI);
            batchJA.put(classificationJS);

            // add the creator
            addCreator(classificationJS, classification);

            // add any classification terms here
            ArrayList<String> classificationTermURIs = new ArrayList<String>();
            JSONArray classificationChildren = classification.getJSONArray("children");

            // add the id for this classification
            classificationIdentifiers.put(arId, classification.getString("ClassificationIdentifier"));

            for (int i = 0; i < classificationChildren.length(); i++) {
                if (stopCopy) return 0;

                JSONObject classificationTerm = classificationChildren.getJSONObject(i);

                JSONObject classificationTermJS = mapper.convertClassification(classificationTerm);

                String classificationTermTitle = classificationTerm.getString("Title");

                String cId = classificationTerm.getString("ID");

                String classificationTermURI = repoURI + ASpaceClient.CLASSIFICATION_TERM_ENDPOINT + "/" + cId;
                classificationTermURIs.add(classificationTermURI);

                classificationIdentifiers.put(cId, classificationTerm.getString("ClassificationIdentifier"));
                String pId = classificationTerm.getString("ParentID");
                classificationParents.put(cId, pId);

                if (classificationTermJS != null) {
                    classificationTermJS.put("uri", classificationTermURI);

                    classificationTermJS.put("classification", mapper.getReferenceObject(classificationURI));

                    if (!pId.equals(arId)) {
                        String parentURI = repoURI + ASpaceClient.CLASSIFICATION_TERM_ENDPOINT + "/" + pId;
                        classificationTermJS.put("parent", mapper.getReferenceObject(parentURI));
                    }

                    // add the linked agents aka Names records
                    addCreator(classificationTermJS, classificationTerm);

                    batchJA.put(classificationTermJS);

                    print("Added Classification Term: " + classificationTermTitle + " :: " + cId);
                } else {
                    print("Fail -- Classification Term to JSON: " + classificationTermTitle);
                }
            }

            // now save the classification and its classification terms using the batch endpoint
            String batchJAString = batchJA.toString(2);
            String bids = saveRecord(batchEndpoint, batchJAString, arId);

            if (!bids.equals(NO_ID)) {
                String parentKey = repoURI + "_" + arId;

                if (!simulateRESTCalls) {
                    JSONObject bidsJS = new JSONObject(bids);
                    classificationURI = (new JSONArray(bidsJS.getString(classificationURI))).getString(0);

                    // now map all the classification term uris
                    for(String uri: classificationTermURIs) {
                        String key = repoURI + "_" + getIdFromURI(uri);
                        String classificationTermURI = (new JSONArray(bidsJS.getString(uri))).getString(0);

                        // need to store the parent classification key along with the
                        // term uri so we can keep track of which classifications are being used
                        String value = classificationTermURI + "," + parentKey;
                        classificationTermURIMap.put(key, value);
                    }
                }

                classificationURIMap.put(parentKey, classificationURI);

                print("Batch Copied Classification: " + classificationTitle + " :: " + parentKey);
            } else {
                print("Batch Copy Fail -- : " + classificationTitle);
                return 0;
            }
        }

        return 1;
    }

    /**
     * Method to set the accession records repository based on any linked
     * resources. Because this loads both collection and accession records it takes
     * a while
     *
     * @throws Exception
     */
    public void findAccessionRecordRepositories() throws Exception {
        print("Finding Accession Records Repositories ...");

        // update the progress so that the title changes
        updateProgress("Accession/Collection Repositories", 0, 0);

        JSONObject accessions = archonClient.getAccessionRecords();

        // these are used to update the progress bar
        int total = accessions.length();
        int count = 0;

        // check to see if we have any accessions before spending the time to load
        // any collections
        if(total == 0) {
            print("No accessions found ...");
            return;
        }

        // load the collections
        JSONObject collections = archonClient.getCollectionRecords();

        Iterator<String> keys = accessions.keys();
        while (keys.hasNext()) {
            if (stopCopy) return;

            String key = keys.next();

            JSONObject accession = accessions.getJSONObject(key);

            String arId = accession.getString("ID");
            String accessionTitle = accession.getString("Title");
            String repoID = defaultRepositoryId;

            // get the linked collections and set the repository to the first one
            try {
                JSONArray linkedCollections = accession.getJSONArray("Collections");

                if(linkedCollections.length() != 0 && linkedCollections.getInt(0) != 0) {
                    JSONObject collection = collections.getJSONObject(linkedCollections.getString(0));
                    repoID = collection.getString("RepositoryID");
                }
            } catch(Exception e) {
                print("Error assigning repository ID for: " + accessionTitle + " :: " + arId);
            }

            accessionRepositoryIdMap.put(arId, repoID);

            print("Accession: " + accessionTitle + " :: " + arId + " / Repository ID: " + repoID);

            count++;
            updateProgress("Accession/Collection Repositories", total, count);
        }

        // refresh the database connection to prevent heap space error
        freeMemory();
    }

    /**
     * Method to copy accession records
     *
     * @throws Exception
     */
    public void copyAccessionRecords() throws Exception {
        print("Copying Accession records ...");

        // update the progress so that the title changes
        updateProgress("Accessions", 0, 0);

        JSONObject records = archonClient.getAccessionRecords();

        // these are used to update the progress bar
        int total = records.length();
        int count = 0;
        int success = 0;

        Iterator<String> keys = records.keys();
        while (keys.hasNext()) {
            if (stopCopy) return;

            String key = keys.next();

            JSONObject accession = records.getJSONObject(key);

            System.out.println("\n\nAccession Record:\n" + accession.toString(2));

            String arId = accession.getString("ID");
            String accessionTitle = accession.getString("Title");

            // check to see if we are only copying accessions for specific collections based on collection archon ID
            if(collArchonIDsList != null) {
                Boolean attachedToCollInList = false;
                String notInList = "";
                if(accession.has("Collections")) {
                    JSONArray collectionIds = accession.getJSONArray("Collections");
                    for(int i = 0; i < collectionIds.length(); i++) {
                        String cid = collectionIds.getString(i);
                        if(collArchonIDsList.contains(cid)){
                            attachedToCollInList = true;
                            break;
                        } else {
                            notInList += ", " + cid;
                        }
                    }
                } else {
                    notInList = ", no collection ID noted";
                }
                if(!attachedToCollInList){
                    print("Not Copied: Accession not attached to Archon Collection ID in list: " + accessionTitle + notInList);
                    continue;
                }
            }

            JSONObject accessionJS = mapper.convertAccession(accession);

            if (accessionJS != null) {
                String repoURI = getAccessionRepositoryURI(arId);

                // add the subjects
                ArrayList<String> subjectAsCreatorsList = addSubjects(accessionJS, accession.getJSONArray("Subjects"), accessionTitle);

                // add the linked agents aka Names records
                JSONArray linkedAgentsJA = addCreators(accessionJS, accession.getJSONArray("Creators"), accessionTitle);

                // add the agent subjects
                addSubjectsAsCreators(linkedAgentsJA, subjectAsCreatorsList, accessionTitle);

                // add the donors
                if(accession.has("Donor") && !accession.getString("Donor").isEmpty()) {
                    addDonor(linkedAgentsJA, accession, accessionTitle);
                }

                // add the classification
                if(accession.has("Classifications")) {
                    addClassifications(repoURI, accessionJS, accession.getJSONArray("Classifications"), accessionTitle);
                }

                // add an instance that holds the location information
                if(accession.has("Locations")) {
                    addLocationInstances(accessionJS, accession.getJSONArray("Locations"), "accession",
                            null, repoURI);
                }

                String uri = repoURI + ASpaceClient.ACCESSION_ENDPOINT;
                String id = saveRecord(uri, accessionJS.toString(), "Accession->" + accessionTitle);
                if (!id.equalsIgnoreCase(NO_ID)) {
                    uri = uri + "/" + id;

                    // now add the event objects
                    //addEvents(accession, repoURI, uri);

                    // now map the ids of any collections linked to this accession record
                    // so a linked can be created when copying the resource records
                    if(accession.has("Collections")) {
                        JSONArray collectionIds = accession.getJSONArray("Collections");
                        for(int i = 0; i < collectionIds.length(); i++) {
                            String cid = collectionIds.getString(i);

                            ArrayList<String> collectionsList;
                            if(resourcesToAccessionsMap.containsKey(cid)) {
                                collectionsList = resourcesToAccessionsMap.get(cid);
                            } else {
                                collectionsList = new ArrayList<String>();
                                resourcesToAccessionsMap.put(cid, collectionsList);
                            }

                            collectionsList.add(uri);
                        }
                    }

                    accessionURIMap.put(arId, uri);
                    print("Copied Accession: " + accessionTitle + " :: " + id);
                    success++;
                } else {
                    print("Fail -- Accession: " + accessionTitle);
                }
            } else {
                print("Fail -- Accession to JSON: " + accessionTitle);
            }

            count++;
            updateProgress("Accessions", total, count);
        }

        updateRecordTotals("Accessions", total, success);

        // refresh the database connection to prevent heap space error
        freeMemory();
    }

    /**
     * Method to copy the digital object records
     *
     * @throws Exception
     */
    public void copyDigitalObjectRecords() throws Exception {
        print("Copying Digital Object records ...");

        // update the progress so that the title changes
        updateProgress("Digital Objects", 0, 0);

        JSONObject records = archonClient.getDigitalObjectRecords();

        // these are used to update the progress bar
        digitalObjectTotal = 0;
        digitalObjectSuccess = 0;

        int total = records.length();
        int totalFree = 0;  // total number of free digital objects
        int count = 0;

        Iterator<String> keys = records.keys();
        while (keys.hasNext()) {
            if (stopCopy) return;

            String key = keys.next();

            // check to make sure we don't process the array of children objects
            if (key.equals("components")) continue;

            JSONObject digitalObject = records.getJSONObject(key);

            String arId = digitalObject.getString("ID");
            String digitalObjectTitle = digitalObject.getString("Title");

            // check to see if we are only copying digital objects for specific collections based on collection archon ID
            String strCollectionID = digitalObject.getString("CollectionID");
            if(collArchonIDsList != null && !collArchonIDsList.contains(strCollectionID)) {
                print("Not Copied: Digital Object not attached to Archon Collection ID in list: " + digitalObjectTitle);
                continue;
            }

            // create the batch import JSON array and dummy URI now
            JSONArray batchJA = new JSONArray();

            String repoURI = getRepositoryURI(defaultRepositoryId);
            String batchEndpoint = repoURI + ASpaceClient.BATCH_IMPORT_ENDPOINT;
            String digitalObjectURI = repoURI + ASpaceClient.DIGITAL_OBJECT_ENDPOINT + "/" + arId;

            JSONObject digitalObjectJS = mapper.convertDigitalObject(digitalObject);

            if (digitalObjectJS != null) {
                digitalObjectJS.put("uri", digitalObjectURI);
                digitalObjectJS.put("jsonmodel_type", "digital_object");
                batchJA.put(digitalObjectJS);

                // add the subjects
                ArrayList<String> subjectAsCreatorsList = addSubjects(digitalObjectJS, digitalObject.getJSONArray("Subjects"), digitalObjectTitle);

                // add the linked agents aka Names records
                JSONArray linkedAgentsJA = addCreators(digitalObjectJS, digitalObject.getJSONArray("Creators"), digitalObjectTitle);

                // add the agent subjects
                addSubjectsAsCreators(linkedAgentsJA, subjectAsCreatorsList, digitalObjectTitle);

                // add any archival objects here
                JSONArray digitalObjectChildren = digitalObject.getJSONArray("components");
                for (int i = 0; i < digitalObjectChildren.length(); i++) {
                    if (stopCopy) return;

                    JSONObject digitalObjectChild = digitalObjectChildren.getJSONObject(i);

                    JSONObject digitalObjectChildJS = mapper.convertToDigitalObjectComponent(digitalObjectChild);

                    String digitalObjectChildTitle = digitalObjectChild.getString("Title");

                    String cId = digitalObjectChild.getString("ID");

                    String digitalObjectChildURI = repoURI + ASpaceClient.DIGITAL_OBJECT_COMPONENT_ENDPOINT + "/" + cId;

                    if (digitalObjectChildJS != null) {
                        digitalObjectChildJS.put("uri", digitalObjectChildURI);
                        digitalObjectChildJS.put("jsonmodel_type", "digital_object_component");
                        digitalObjectChildJS.put("digital_object", mapper.getReferenceObject(digitalObjectURI));

                        batchJA.put(digitalObjectChildJS);

                        print("Added Digital Object Component: " + digitalObjectChildTitle + " :: " + cId);
                    } else {
                        print("Fail -- Digital Object Child to JSON: " + digitalObjectChildTitle);
                    }
                }

                // check to see we just not saving the digital objects or copying them now
                int collectionID = 0;
                int contentID = 0;

                if(saveDigitalObjectsWithResources) {
                    collectionID = digitalObject.getInt("CollectionID");
                    contentID = digitalObject.getInt("CollectionContentID");
                }

                // call methods to actually save this digital object to the back end
                // or store it for saving when saving the resource records
                if (collectionID != 0 || contentID != 0) {
                    digitalObjectTotal++;
                    storeDigitalObject(batchJA, collectionID, contentID);
                } else {

                    print("No record to attach digital object " + digitalObjectTitle + "...");
                    print("Saving to default repository ...");

                    totalFree++;
                    saveDigitalObject(batchEndpoint, batchJA);
                }
            } else {
                print("Fail -- Digital Object to JSON: " + digitalObjectTitle);
            }

            count++;
            updateProgress("Digital Objects", total, count);
        }

        updateRecordTotals("Standalone Digital Objects", totalFree, digitalObjectSuccess);

        // refresh the database connection to prevent heap space error
        freeMemory();
    }

    /**
     * Method to save a digital object to the backend using the batch API
     *
     * @param batchEndpoint
     * @param batchJA
     * @return
     * @throws Exception
     */
    private String saveDigitalObject(String batchEndpoint, JSONArray batchJA) throws Exception {
        JSONObject digitalObjectJS = batchJA.getJSONObject(0);
        String digitalObjectURI = digitalObjectJS.getString("uri");
        String digitalObjectTitle = digitalObjectJS.getString("title");
        String arId  = digitalObjectURI.split("/")[4];

        String bids = saveRecord(batchEndpoint, batchJA.toString(2), arId);

        if (!bids.equals(NO_ID)) {
            if (!simulateRESTCalls) {
                JSONObject bidsJS = new JSONObject(bids);
                digitalObjectURI = (new JSONArray(bidsJS.getString(digitalObjectURI))).getString(0);
            }

            digitalObjectURIMap.put(arId, digitalObjectURI);

            digitalObjectSuccess++;

            print("Batch Copied Digital Object: " + digitalObjectTitle + " :: " + arId);
            return digitalObjectURI;
        } else {
            String message = "Batch Copy Fail -- Digital Object: " + digitalObjectTitle;
            print(message);
            return null;
        }
    }

    /**
     * Method to store the digital object in a hashmap so it can be saved later
     *
     * @param batchJA
     *
     * @param contentID
     * @param collectionID
     */
    private void storeDigitalObject(JSONArray batchJA, int collectionID, int contentID) {//String digitalObjectListKey) {

        HashMap<Integer, ArrayList<JSONArray>> collectionMap = digitalObjectMap.get(collectionID);

        if (collectionMap == null) {
            collectionMap = new HashMap<Integer, ArrayList<JSONArray>>();
            digitalObjectMap.put(collectionID, collectionMap);
        }

        ArrayList<JSONArray> digitalObjectList = collectionMap.get(contentID);

        if (digitalObjectList == null) {
            digitalObjectList = new ArrayList<JSONArray>();
            collectionMap.put(contentID, digitalObjectList);
        }

        digitalObjectList.add(batchJA);
    }

    /**
     * Method to download the digital object files
     *
     * @param downloadDirectory
     */
    public void downloadDigitalObjectFiles(File downloadDirectory) throws Exception {
        print("Copying Digital Object Files ...");

        // update the progress so that the title changes
        updateProgress("Digital Object Files", 0, 0);

        HashMap<String, String> fileIDsToFilenamesMap = mapper.getFileIDsToFilenamesMap();

        int total = fileIDsToFilenamesMap.size();
        int success = 0;
        int count = 0;

        for(String id: fileIDsToFilenamesMap.keySet()) {
            if (stopCopy) break;

            String filename = fileIDsToFilenamesMap.get(id);

            boolean downloaded = archonClient.downloadDigitalObjectFile(downloadDirectory, id, filename);
            if(downloaded) {
                success++;
                print("Downloaded Digital Object File: " + filename);
            } else {
                print("Fail -- Download of Digital Object File: " + filename);
            }

            count++;
            updateProgress("Digital Object Files", total, count);
        }

        updateRecordTotals("Digital Object File Download", total, success);
    }

    /**
     * Method to copy resource records from one database to the next
     *
     * @throws Exception
     * @param max Used to specify the maximum amount of records that should be copied
     */
    public void copyCollectionRecords(int max) throws Exception {
        currentRecordType = "Collection Record";

        // first delete previously saved resource records if that option was selected by user
        if(deleteSavedResources) {
            deleteSavedResources();
        }

        // update the progress bar now to indicate the records are being loaded
        updateProgress("Collection Records", 0, 0);

        JSONObject records;
        if(cachedCollectionsJS == null) {
            records = archonClient.getCollectionRecords();
            cachedCollectionsJS = records;
        } else {
            records = cachedCollectionsJS;
        }

        // reset the number of digital object copied successfully
        digitalObjectSuccess = 0;

        print("\nCopying " + records.length() + " Collection records ...\n");

        copyCount = 0; // keep track of the number of resource records copied

        // these are used to update the progress bar
        int total = records.length();
        int count = 0;
        int maxBatchLength = 0;

        //counts for collection content
        int countWithCollContent = 0;
        int successWithCollContent = 0;
        int countCollContent = 0;
        int successCollContent = 0;

        // if we in debug mode, then set total to max
        if(debug && max < total) total = max;

        Iterator<String> keys = records.keys();
        while (keys.hasNext()) {
            count++;

            // check if to stop copy process
            if(stopCopy) {
                updateRecordTotals("Collection Records", total, copyCount);
                return;
            }

            // get the collection record
            JSONObject collection = records.getJSONObject(keys.next());

            // get the resource title
            String collectionTitle = collection.getString("Title");

            // get the record id
            String dbId = collection.getString("ID");
            int intID = Integer.parseInt(dbId);

            // get the parent repository
            String repositoryID = collection.getString("RepositoryID");

            // get the at resource identifier to see if to only copy a specified resource
            // and to use for trouble shooting purposes
            String arId = collection.getString("CollectionIdentifier");

            if(arId == null || arId.isEmpty()) {
                arId = "DBID-" +dbId;
            }

            currentRecordIdentifier = "DB ID: " + dbId + "\nAR ID: " + arId;
            currentRecordDBID = dbId;

            // set the atId in the mapper object
            mapper.setCurrentCollectionRecordIdentifier(arId);

            // check to see if we are not just copying a single resource based on collection identifier
            if(collectionsIDsList != null && !collectionsIDsList.contains(arId)) {
                print("Not Copied: Collection not in Collection identifier list: " + collectionTitle);
                continue;
            }
            // check to see if we are not just copying a single resource based on archon ID
            if(collArchonIDsList != null && !collArchonIDsList.contains(dbId)) {
                print("Not Copied: Collection not in Archon ID list: " + collectionTitle);
                continue;
            }

            if (resourceURIMap.containsKey(dbId)) {
                print("Not Copied: Collection already in database " + collection);
                updateProgress("Collection Records", total, count);
                continue;
            }

            // create the batch import JSON array in case we need it
            JSONArray batchJA = new JSONArray();

            // we need to update the progress bar here
            updateProgress("Collection Records", total, count);

            // indicate we are copying the resource record
            print("Copying Collection: " + collectionTitle + " ,DB_ID: " + dbId);

            // get the main json object
            JSONObject resourceJS = mapper.convertCollection(collection, classificationIdentifiers, classificationParents);

            if (resourceJS != null) {
                String repoURI = getRepositoryURI(repositoryID);
                String endpoint = repoURI + ASpaceClient.RESOURCE_ENDPOINT;
                String batchEndpoint = repoURI + ASpaceClient.BATCH_IMPORT_ENDPOINT;

                // add the subjects
                ArrayList<String> subjectAsCreatorsList = addSubjects(resourceJS, collection.getJSONArray("Subjects"), collectionTitle);

                // add the linked agents aka Names records
                JSONArray linkedAgentsJA = addCreators(resourceJS, collection.getJSONArray("Creators"), collectionTitle);

                // linked subjects which are creators as agents
                addSubjectsAsCreators(linkedAgentsJA, subjectAsCreatorsList, collectionTitle);

                // add any linked classifications
                String classificationId = collection.getString("ClassificationID");
                if(!classificationId.isEmpty() && !classificationId.equals("0")) {
                    JSONArray classificationIds = new JSONArray();
                    classificationIds.put(classificationId);
                    addClassifications(repoURI, resourceJS, classificationIds, collectionTitle);
                }

                // add the instances
                addDigitalInstances(resourceJS, intID, 0, collectionTitle, batchEndpoint);

                // add the linked accessions
                addRelatedAccessions(resourceJS, dbId, arId, repoURI + "/");

                // if we using batch import then we not not going to
                resourceJS.put("uri", endpoint + "/" + dbId);
                resourceJS.put("jsonmodel_type", "resource");
                batchJA.put(resourceJS);

                // set the resource URI and archival object endpoint
                String resourceURI = endpoint + "/" + dbId;
                String aoEndpoint = repoURI + ASpaceClient.ARCHIVAL_OBJECT_ENDPOINT;

                // map of intellectual component IDs to their JSON objects
                HashMap<String, JSONObject> intellectualComponents = new HashMap<String, JSONObject>();
                HashMap<String, String> intellectualComponentParents = new HashMap<String, String>();

                // add any archival objects here
                JSONObject resourceComponents = archonClient.getCollectionContentRecords(dbId);

                if(resourceComponents != null && resourceComponents.length()>0){
                    countWithCollContent++;
                }
                //for tracking whether any content is added
                Boolean addedCollContent = false;

                // stores any component IDs that are referenced as parents but not actually in the database
                HashSet<String> notFoundIDs = new HashSet<String>();

                // stores components with invalid (looping) parent relationships
                HashSet<String> invalidParentIDs = new HashSet<String>();

                // maps saved top container type/indicator combos with its uri in ASpace
                HashMap<String, String> topContainerURIs = new HashMap<String, String>();

                // maps a physical component to any intellectual components it will need an instance for
                // this is for intellectual only components that are immediate children of a physical only component
                HashMap<String, ArrayList<String>> physicalComponentInstanceOfIDs = new HashMap<String, ArrayList<String>>();

                // maps a physical component ID with that component's intellectual parent ID
                HashMap<String, String> physicalComponentParents = new HashMap<String, String>();

                // map a physical component ID with that components hierarchy of physical components (for containers)
                HashMap<String, Stack<JSONObject>> physicalComponentsData = new HashMap<String, Stack<JSONObject>>();

                // stores string sort keys
                HashMap<String, TreeSet<String>> sortKeysByParent = new HashMap<String, TreeSet<String>>();

                // maps an intellectual component to a boolean that tells if it has exactly 1 child
                HashMap<String, Boolean> hasOneChild = new HashMap<String, Boolean>();

                // store physical content IDs we may not need to add an instance for since they have child physical content
                HashSet<String> skipParentInstanceFor = new HashSet<String>();

                Iterator<String> ckeys = resourceComponents.sortedKeys();
                while (ckeys.hasNext()) {
                    countCollContent++;
                    JSONObject component = resourceComponents.getJSONObject(ckeys.next());
                    String title = component.getString("Title");

                    int contentType = component.getInt("ContentType");
                    String cid = component.getString("ID");

                    // add an resource component if component is not physical only
                    if(contentType != 2) {
                        JSONObject componentJS = mapper.convertCollectionContent(component);

                        if (componentJS != null) {
                            componentJS.put("resource", mapper.getReferenceObject(resourceURI));

                            // set the parent component if needed
                            int parentId = 0;

                            String nextComponentID = component.getString("ParentID");
                            int loopCount = 0;
                            String sortKey = Character.toString((char) component.getInt("SortOrder"));

                            // find the first intellectual parent component if there is one
                            while (!nextComponentID.equals("0")) {

                                // avoid getting stuck in an infinite loop if references circle back around
                                if (nextComponentID.equals(cid) || ++loopCount > 12) {
                                    componentJS.put("publish", false);
                                    invalidParentIDs.add(cid);
                                    break;
                                }

                                // find the next parent component
                                JSONObject nextComponent;
                                try {
                                    nextComponent = resourceComponents.getJSONObject(nextComponentID);
                                } catch (JSONException e) {
                                    // if a parent doesn't exist, save component at top level but don't publish
                                    notFoundIDs.add(nextComponentID);
                                    componentJS.put("publish", false);
                                    break;
                                }

                                // if the component is not physical only it can be the parent and we're done
                                if (nextComponent.getInt("ContentType") != 2) {
                                    parentId = nextComponent.getInt("ID");

                                    if (loopCount == 1) {
                                        // update whether the parent has exactly one child
                                        if (!hasOneChild.containsKey(parentId + "")) {
                                            hasOneChild.put(parentId + "", true);
                                        } else {
                                            hasOneChild.put(parentId + "", false);
                                        }
                                    }

                                    break;
                                } else {

                                    // if the immediate parent of a intellectual only component is physical only
                                    // then we will need to create an instance of the child to link them
                                    if (loopCount == 1 && (contentType == 1 || nextComponent.getString("OtherLevel").equals("undefined"))) {
                                        if (!physicalComponentInstanceOfIDs.containsKey(nextComponentID)) {
                                            physicalComponentInstanceOfIDs.put(nextComponentID, new ArrayList<String>());
                                        }
                                        physicalComponentInstanceOfIDs.get(nextComponentID).add(cid);
                                    }

                                    // add the sort order for parent physical only components to the sort key
                                    char nextSortKey = (char) nextComponent.getInt("SortOrder");
                                    sortKey = nextSortKey + sortKey;
                                }

                                // otherwise try again with that component's parent
                                nextComponentID = nextComponent.getString("ParentID");
                            }

                            // if we found a parent add a reference to it here
                            // parent id might still actually be zero, which would cause a save error
                            if(parentId != 0) {
                                String parentURI = aoEndpoint + "/" + parentId;
                                componentJS.put("parent", mapper.getReferenceObject(parentURI));
                            }

                            // add mapping of component to parent
                            intellectualComponentParents.put(cid, parentId + "");

                            // add the sort key so the position can be calculated
                            componentJS.put("sort_key", sortKey);
                            if (!sortKeysByParent.containsKey(parentId + "")) {
                                sortKeysByParent.put(parentId + "", new TreeSet<String>());
                            }
                            sortKeysByParent.get(parentId + "").add(sortKey);

                            // add the subjects now
                            subjectAsCreatorsList = addSubjects(componentJS, component.getJSONArray("Subjects"), title);

                            // add the linked agents aka Names records
                            linkedAgentsJA = addCreators(componentJS, component.getJSONArray("Creators"), title);

                            // linked subjects which are creators as agents
                            addSubjectsAsCreators(linkedAgentsJA, subjectAsCreatorsList, title);

                            // add the digital instances
                            addDigitalInstances(componentJS, intID, Integer.parseInt(cid), collectionTitle, batchEndpoint);

                            // save this json record now to get the URI
                            componentJS.put("uri", aoEndpoint + "/" + cid);
                            componentJS.put("jsonmodel_type", "archival_object");

                            // store this component so we can add any instances later
                            intellectualComponents.put(cid, componentJS);

                            print("Copied Resource Component: " + title + " :: " + cid + "\n");
                            addedCollContent = true;
                            successCollContent++;
                        } else {
                            print("Fail -- Resource Component to JSON: " + title);
                        }
                    }

                    // prepare to add an instance (and container data) if it is a physical component
                    if (contentType != 1 && !component.getString("OtherLevel").equals("undefined")){

                        String intellectualParentID = null;

                        // this holds the chain of parent physical components for the component
                        Stack<JSONObject> componentChain = new Stack<JSONObject>();
                        String nextComponentID = cid;

                        // fail safe to make sure we don't get stuck in an infinite loop
                        int loopCount = 0;

                        // find the intellectual parent and physical hierarchy
                        while (!nextComponentID.equals("0")) {

                            if (++loopCount > 12) break;

                            JSONObject nextComponent;
                            try {
                                nextComponent = resourceComponents.getJSONObject(nextComponentID);
                            } catch (JSONException e) {
                                // if a referenced parent is not actually in Archon
                                notFoundIDs.add(nextComponentID);
                                break;
                            }

                            // if it is a physical component add it to the stack
                            if (nextComponent.getInt("ContentType") != 1 &&
                                    !nextComponent.getString("OtherLevel").equals("undefined")) {

                                // usually earliest way to tell if we are stuck in infinate loop
                                if (!componentChain.contains(nextComponent)) {
                                    componentChain.push(nextComponent);
                                    if (loopCount > 1) skipParentInstanceFor.add(nextComponentID);
                                }
                                else break;
                            }

                            // if it is an intellectual component and we haven't found a parent yet, make it the parent
                            if (intellectualParentID == null && nextComponent.getInt("ContentType") != 2) {
                                intellectualParentID = nextComponent.getString("ID");
                            }
                            nextComponentID = nextComponent.getString("ParentID");
                        }

                        // just in case there is an issue and we end up with an empty stack
                        if (componentChain.size() == 0) componentChain.push(component);

                        // save the hierarchy/parent data we just found so we can add an instance later
                        physicalComponentParents.put(cid, intellectualParentID);
                        physicalComponentsData.put(cid, componentChain);

                        // update whether the parent has exactly one child
                        if (componentChain.size() == 1) {
                            if (!hasOneChild.containsKey(intellectualParentID)) {
                                hasOneChild.put(intellectualParentID, true);
                            } else {
                                hasOneChild.put(intellectualParentID, false);
                            }
                        }
                    }
                }

                // now add instances for the physical components
                for (String id: physicalComponentParents.keySet()) {

                    // get the parent's ASpace JSON record
                    String parentId = physicalComponentParents.get(id);
                    JSONObject parentJSON = intellectualComponents.get(parentId);
                    if (parentJSON == null) parentJSON = resourceJS;

                    // get the component's physical hierarchy
                    Stack<JSONObject> componentChain = physicalComponentsData.get(id);

                    // add an instance for this physical component to each record in list
                    ArrayList<JSONObject> instanceList = new ArrayList<JSONObject>();

                    if (physicalComponentInstanceOfIDs.containsKey(id)) {
                        for (String recordID : physicalComponentInstanceOfIDs.get(id)) {
                            JSONObject recordJSON = intellectualComponents.get(recordID);
                            if (recordJSON != null) instanceList.add(recordJSON);
                        }
                    }

                    // if there are no components in list link the container to an instance of its intellectual parent
                    // also if the container is the only child of the parent and top level
                    if (id.equals(parentId)) {
                        instanceList.add(parentJSON);
                    } else {
                        if (componentChain.size() == 1 && hasOneChild.containsKey(parentId) && hasOneChild.get(parentId)) {
                            instanceList.add(parentJSON);
                        } else if (instanceList.isEmpty() && !skipParentInstanceFor.contains(id)) {
                            instanceList.add(parentJSON);
                        }
                    }

                    // add an instance to every component in list
                    if (!instanceList.isEmpty()) addInstance(instanceList, componentChain, topContainerURIs, repoURI);

                    // add any digital objects linked to the physical component in archon
                    // they linked to the component's parent in ASpace since digital object can't be linked to container
                    addDigitalInstances(parentJSON, intID, Integer.parseInt(id), null, batchEndpoint);
                }

                // add an instance that holds the location information
                if(collection.has("Locations")) {
                    addLocationInstances(resourceJS, collection.getJSONArray("Locations"), defaultInstanceType,
                            topContainerURIs, repoURI);
                }

                // add any digital instances that were attached to a component that was not found
                HashMap<Integer, ArrayList<JSONArray>> collectionDOMap = digitalObjectMap.get(intID);
                if (collectionDOMap != null) {
                    Integer[] notFoundKeys = collectionDOMap.keySet().toArray(new Integer[0]);
                    for (int id : notFoundKeys) {
                        notFoundIDs.add(id + "");
                        addDigitalInstances(resourceJS, intID, id, collectionTitle, batchEndpoint);
                    }
                }

                // remove the entire resource from the digital object map so we'll know it was found
                digitalObjectMap.remove(intID);

                // add warning about any component IDs that were missing from Archon
                if (notFoundIDs.size() != 0) {
                    StringBuilder errorMessage = new StringBuilder();
                    errorMessage.append("Could not find ").append(notFoundIDs.size())
                            .append(" component(s) for resource: ").append(arId);
                    if (notFoundIDs.size() <= 20) {
                        errorMessage.append("\nNot found components:");
                        for (String id : notFoundIDs) errorMessage.append(" ").append(id);
                    }
                    errorMessage.append("\nReferencing child components/digital objects will have root resource record as parent.");
                    errorMessage.append("\nReferencing child components will be unpublished.\n");
                    addErrorMessage(errorMessage.toString());
                }

                // add the component positions and add them to the batch JA
                HashSet<String> added = new HashSet<String>();
                for (String cid : intellectualComponents.keySet()) {
                    JSONObject componentJS = intellectualComponents.get(cid);
                    addComponentPosition(componentJS, cid, intellectualComponentParents, sortKeysByParent);
                    addComponentToBatch(cid, batchJA, intellectualComponentParents, intellectualComponents, added, invalidParentIDs);
                }

                // add a warning for components with invalid parent relationships
                if (invalidParentIDs.size() != 0) {
                    StringBuilder errorMessage = new StringBuilder();
                    errorMessage.append("Invalid parent relationships found for ").append(invalidParentIDs.size())
                            .append(" component(s) for resource: ").append(arId);
                    if (invalidParentIDs.size() <= 20) {
                        errorMessage.append("\nInvalid relationships found for components:");
                        for (String id : invalidParentIDs) errorMessage.append(" ").append(id);
                    }
                    errorMessage.append("\nThese components will have root resource record as parent and be unpublished.");
                    errorMessage.append("\nCleanup will be required for this resource.\n");
                    addErrorMessage(errorMessage.toString());
                }

                // free some memory now
                freeMemory();

                print("Batch Copying Resource # " + count + " || Title: " + collectionTitle);

                String bids = saveRecord(batchEndpoint, batchJA.toString(2), arId);
                if (!bids.equals(NO_ID)) {
                    if (!simulateRESTCalls) {
                        JSONObject bidsJS = new JSONObject(bids);
                        resourceURI = (new JSONArray(bidsJS.getString(resourceURI))).getString(0);
                    }

                    updateResourceURIMap(dbId, resourceURI);
                    incrementCopyCount();

                    // update count for collections with collection content
                    if(addedCollContent){
                        successWithCollContent++;
                    }

                    // update the copy message
                    updateRecordTotals("Instance Digital Objects", digitalObjectTotal, digitalObjectSuccess);
                    updateRecordTotals("Locations", locationTotal, locationSuccess);
                    updateRecordTotals("Collections", total, copyCount);

                    // updates added for collection content
                    updateRecordTotals("Collections with collection content", countWithCollContent, successWithCollContent);
                    updateRecordTotals("Collection content", countCollContent, successCollContent);

                    print("Batch Copied Collection: " + collectionTitle + " :: " + resourceURI);
                } else {
                    print("Batch Copy Fail -- Collection: " + collectionTitle);
                }
            } else {
                print("Fail -- Resource to JSON: " + collectionTitle);
            }

            if (debug && copyCount >= max) break;
        }

        // update the number of digital records that were copied during processing of collection records
        updateRecordTotals("Instance Digital Objects", digitalObjectTotal, digitalObjectSuccess);

        // update the number of location records copied
        updateRecordTotals("Locations", locationTotal, locationSuccess);

        // update the number of resource actually copied
        updateRecordTotals("Collections", total, copyCount);

        // update collections with collection content copied
        updateRecordTotals("Collections with collection content", countWithCollContent, successWithCollContent);

        // update collections with collection content copied
        updateRecordTotals("Collection content", countCollContent, successCollContent);
    }

    /**
     * calculates the position of a given component
     * this has to be redone since the containers are no longer included in the sorting
     * @param componentJS
     * @param cID
     * @param intellectualComponentParents
     * @param sortKeysByParent
     * @throws JSONException
     */
    private void addComponentPosition(JSONObject componentJS, String cID,
                                      HashMap<String, String> intellectualComponentParents,
                                      HashMap<String, TreeSet<String>> sortKeysByParent) throws JSONException {
        // get the parent since sorting is scoped by parent
        String parentID = "0";
        if (intellectualComponentParents.containsKey(cID)) parentID = intellectualComponentParents.get(cID);

        // the sort keys are strings that have the correct sort order
        String sortKey = componentJS.getString("sort_key");
        TreeSet<String> parentKeys = sortKeysByParent.get(parentID);

        // to get an integer sort key get the position of the sort key string in the sorted set
        if (parentKeys != null) {
            int position = parentKeys.headSet(sortKey, true).size();
            componentJS.put("position", position * 1000);
        }
    }

    /**
     * method to add all the resource components to the JSON Array in an acceptable order for batch saving
     *
     * @param cid
     * @param batchJA
     * @param intellectualComponentParents
     * @param intellectualComponents
     * @param added
     */
    private void addComponentToBatch(String cid, JSONArray batchJA, HashMap<String, String> intellectualComponentParents,
                                     HashMap<String, JSONObject> intellectualComponents, HashSet<String> added,
                                     HashSet<String> invalidParents) throws JSONException {

        // make sure we don't add a duplicate
        if (!added.contains(cid)) {

            String nextID = cid;
            int loopCount = 0;

            // final check that the parent relationship is valid and doesn't loop
            while (!nextID.equals("0")) {
                nextID = intellectualComponentParents.get(nextID);
                if (added.contains(nextID)) break;
                if (++loopCount > 12 || nextID == null) {
                    JSONObject component = intellectualComponents.get(cid);
                    component.remove("parent");
                    component.put("publish", false);
                    invalidParents.add(cid);
                    intellectualComponentParents.put(cid, "0");
                    break;
                }
            }

            // unless it's a top level component make sure it's parent has been added to the batch array first
            if (!intellectualComponentParents.get(cid).equals("0")) {
                addComponentToBatch(intellectualComponentParents.get(cid), batchJA, intellectualComponentParents,
                        intellectualComponents, added, invalidParents);
            }

            // add the component to the batch array and the set of IDs that have already been added
            batchJA.put(intellectualComponents.get(cid));
            added.add(cid);
        }
    }

    /**
     * method to figure out what the container type and indicator should be for a linked location based on location content
     * sometimes the content field will be making an obvious reference to an existing container - this method identifies those
     * this could be made to work for more situations but its not really worth expending too much migration time on
     *
     * @param content
     * @return
     */
    private String[] getTypeAndIndicator(String content) {
        String containerType;
        String containerIndicator;
        String[] splitId = content.split(" ");
        String containerTypeID = null;

        // account for plural forms of Archon's default physical content types
        if (splitId[0].equalsIgnoreCase("boxes")) splitId[0] = "box";
        else if (splitId[0].equalsIgnoreCase("folders")) splitId[0] = "folder";

        // try to find an Archon physical content type that matches
        // start with just the first word add add on a word at a time, checking for a match whenever a word is added
        StringBuilder typeSb = new StringBuilder();
        int indicatorStart = 1;
        while (indicatorStart < splitId.length) {
            typeSb.append(splitId[indicatorStart - 1]);
            containerTypeID = getContainerTypeArchonID(typeSb.toString());
            if (containerTypeID != null) break;
            typeSb.append(" ");
            indicatorStart++;
        }

        // whatever is leftover after the type is the indicator
        if (indicatorStart == splitId.length - 1) containerIndicator = splitId[indicatorStart];
        else {
            StringBuilder indicatorSb = new StringBuilder();
            for (int i = indicatorStart; i < splitId.length; i++) indicatorSb.append(splitId[i]);
            containerIndicator = indicatorSb.toString();

        }

        // if no type was found, type will be empty and the indicator will be location content
        if (containerTypeID == null) containerIndicator = content;

        // find the ASpace container type the Archon physical content type maps to
        containerType = enumUtil.getASpaceInstanceContainerType(containerTypeID);

        return new String[]{containerType, containerIndicator};
    }

    /**
     * method to add an instance to a resource/component
     * @param instanceList
     * @param componentChain
     * @param topContainerURIs
     * @param repoURI
     * @throws Exception
     */
    private void addInstance(ArrayList<JSONObject> instanceList, Stack<JSONObject> componentChain, HashMap<String, String> topContainerURIs,
                             String repoURI) throws Exception {

        // the first item in the component chain becomes the top container
        JSONObject topLevel = componentChain.pop();

        String containerType;
        String containerIndicator;
        String cid;

        containerType = enumUtil.getASpaceInstanceContainerType(topLevel.getString("ContainerTypeID"));
        containerIndicator = topLevel.getString("ContainerIndicator");
        cid = topLevel.getString("ID");

        // we're not going to scope it by content ID if it is physical only
        // these indicators are typically not repeated unless they are actually the same container
        if (topLevel.getInt("ContentType") == 2) cid = null;

        // add top container or get a reference to it if it already exists
        String containerURI = addTopContainer(containerType, containerIndicator, topContainerURIs, repoURI, null, cid);
        JSONObject topRef = mapper.getReferenceObject(containerURI);

        // create a json object for the instance
        JSONObject json = new JSONObject();

        json.put("instance_type", defaultInstanceType);

        // json object for the sub container
        JSONObject containerJS = new JSONObject();

        // second item in component chain is level 2 container
        if (!componentChain.isEmpty()) {
            JSONObject secondLevel = componentChain.pop();
            containerJS.put("type_2", enumUtil.getASpaceInstanceContainerType(secondLevel.getString("ContainerTypeID")));
            containerJS.put("indicator_2", secondLevel.getString("ContainerIndicator"));
        }

        // third item in component chain is level 3 container
        if (!componentChain.isEmpty()) {
            JSONObject thirdLevel = componentChain.pop();
            containerJS.put("type_3", enumUtil.getASpaceInstanceContainerType(thirdLevel.getString("ContainerTypeID")));
            containerJS.put("indicator_3", thirdLevel.getString("ContainerIndicator"));
        }

        // add reference to top container
        containerJS.put("top_container", topRef);

        // add sub container to instance
        json.put("sub_container", containerJS);

        // add the instance to each component in our set
        for (JSONObject recordJS : instanceList) {
            // get the instance list from the record or if there is none create an empty one
            JSONArray instanceJA;
            try {
                instanceJA = recordJS.getJSONArray("instances");
            } catch (JSONException e) {
                instanceJA = new JSONArray();
            }

            instanceJA.put(json);

            recordJS.put("instances", instanceJA);
        }
    }

    /**
     * method to add a top container to ASpace or return the URI of a previously added equivalent one
     * @param containerType
     * @param containerIndicator
     * @param topContainerURIs
     * @param repoURI
     * @param location
     * @return
     * @throws Exception
     */
    private String addTopContainer(String containerType, String containerIndicator, HashMap<String, String> topContainerURIs,
                                   String repoURI, JSONObject location, String cid) throws Exception {
        String uri;

        // the key that is used to map to uri if there is no component ID
        String containerKey = (containerType + " " + containerIndicator).toLowerCase();

        if (cid == null) cid = containerKey;

        JSONObject json;

        if (topContainerURIs.containsKey(cid)) {
            // the container already exists so we don't need to do anything else
            uri =  topContainerURIs.get(cid);
        } else {
            // create json top container record
            json = new JSONObject();
            json.put("indicator", containerIndicator);
            json.put("type", containerType);

            // if a linked location is specified add its data
            if (location != null) addLocationInfo(json, location);

            // save the top container record and get its uri
            String id = saveRecord(repoURI + ASpaceClient.TOP_CONTAINER_ENDPOINT, json.toString(), "Collection content->" + cid);
            if (!id.equalsIgnoreCase(NO_ID)) {
                uri = repoURI + ASpaceClient.TOP_CONTAINER_ENDPOINT + "/" + id;
                print("Copied Top Container: " + containerKey);
                topContainerURIs.put(containerKey, uri);
                topContainerURIs.put(cid, uri);
            } else {
                print("Fail -- Top Container: " + containerKey);
                return null;
            }
        }

        return uri;
    }

    /**
     * adds a location reference to a top container record
     *
     * @param containerJS
     * @param location
     * @throws Exception
     */
    private void addLocationInfo(JSONObject containerJS, JSONObject location) throws Exception {
        // add the extent information
        String extentNote = "";

        if(location.has("Extent")) {
            extentNote += location.getString("Extent") + " ";
            extentNote += enumUtil.getASpaceExtentType(location.getInt("ExtentUnitID"));
        }

        // add a location record record now
        String building = location.getString("Location");
        String coordinate1 = location.getString("RangeValue");
        String coordinate2 = location.getString("Section");
        String coordinate3 = location.getString("Shelf");

        String locationURI = getLocationURI(building, coordinate1, coordinate2, coordinate3);

        if(locationURI != null) {
            Date date = new Date(); // this is need to have valid container_location json record

            JSONArray locationsJA = new JSONArray();

            JSONObject locationJS = new JSONObject();
            locationJS.put("status", "current");
            locationJS.put("start_date", date);
            locationJS.put("note", extentNote);
            locationJS.put("ref", locationURI);

            locationsJA.put(locationJS);

            // put all the records together now
            containerJS.put("container_locations", locationsJA);
        }
    }

    /**
     * Method to add a digital instance to resource, or resource component record
     *
     * @param json
     * @param recordTitle the title of the record
     * @throws Exception
     */
    private void addDigitalInstances(JSONObject json, int collectionID, int contentID, String recordTitle, String batchEndpoint) throws Exception {

        JSONArray instancesJA = new JSONArray();

        HashMap<Integer, ArrayList<JSONArray>> collectionMap = digitalObjectMap.get(collectionID);
        ArrayList<JSONArray> digitalObjectList;

        HashMap<Integer, ArrayList<JSONArray>> noCollectionMap = digitalObjectMap.get(0);

        if (collectionMap != null) {
            digitalObjectList = collectionMap.get(contentID);
            if (digitalObjectList == null) {
                digitalObjectList = new ArrayList<JSONArray>();
            }
        } else {
            collectionMap = new HashMap<Integer, ArrayList<JSONArray>>();
            digitalObjectList = new ArrayList<JSONArray>();
        }

        if (noCollectionMap != null && noCollectionMap.containsKey(contentID)) {
            digitalObjectList.addAll(noCollectionMap.get(contentID));
            noCollectionMap.remove(contentID);
        }

        if (recordTitle == null) {
            try {
                recordTitle = json.getString("title");
            } catch (JSONException  e) {
                recordTitle = "untitled";
            }
        }


        // to make sure digital objects aren't added twice if component is both intellectual and physical
        collectionMap.remove(contentID);

        for(JSONArray batchJA: digitalObjectList) {
            String digitalObjectURI = saveDigitalObject(batchEndpoint, batchJA);

            if(digitalObjectURI != null) {
                JSONObject instanceJS = new JSONObject();
                instanceJS.put("instance_type", "digital_object");
                instanceJS.put("digital_object", mapper.getReferenceObject(digitalObjectURI));
                instancesJA.put(instanceJS);

                if (debug) print("Added Digital Object Instance to " + recordTitle);
            }
        }

        if (instancesJA.length() != 0) {
            json.put("instances", instancesJA);
        }
    }

    /**
     * Method to add a related accessions to a resource record
     *
     * @param json The ASpace resource record
     */
    private void addRelatedAccessions(JSONObject json, String dbId, String arId, String recordRepoURI) throws Exception {
        String message;
        JSONArray accessionsJA = new JSONArray();

        if (resourcesToAccessionsMap.containsKey(dbId)) {
            for (String accessionURI : resourcesToAccessionsMap.get(dbId)) {
                if (accessionURI.contains(recordRepoURI)) {
                    accessionsJA.put(mapper.getReferenceObject(accessionURI));
                    if (debug) print("Added Accession to Resource: " + arId);
                } else {
                    message = "Repository Mismatch Between Resource -- Accession (for collection with Archon ID: " + dbId + "); Resource Repository URI [ "+ recordRepoURI + " ] / Accession URI [ " + accessionURI + " ]\n";
                    addErrorMessage(message);
                }
            }
        }

        if (accessionsJA.length() != 0) {
            json.put("related_accessions", accessionsJA);
        }
    }

    /**
     * Add the subjects to the json resource, or resource component record
     *
     * @param json   The json representation of the AR record
     * @param subjectIds The subjects ids
     * @param recordTitle The record parent record title
     * @throws Exception
     */
    private ArrayList<String> addSubjects(JSONObject json, JSONArray subjectIds, String recordTitle) throws Exception {
        ArrayList<String> subjectAsCreatorsList = new ArrayList<String>();
        JSONArray subjectsJA = new JSONArray();

        for (int i = 0; i < subjectIds.length(); i++) {
            String id = subjectIds.getString(i);
            String subjectURI = subjectURIMap.get(id);

            if (subjectURI != null) {
                if(!subjectURI.equals("agent")) {
                    subjectsJA.put(mapper.getReferenceObject(subjectURI));
                    if (debug) print("Added subject to " + recordTitle);
                } else {
                    subjectAsCreatorsList.add("subject_" + id);
                }
            } else {
                print("No mapped subject found ...");
            }
        }

        // if we had any subjects add them parent json record
        if (subjectsJA.length() != 0) {
            json.put("subjects", subjectsJA);
        }

        return subjectAsCreatorsList;
    }

    /**
     * Add the name creator to the resource or resource component record
     *
     * @param json   object record
     * @param record
     * @throws Exception
     */
    private void addCreator(JSONObject json, JSONObject record) throws Exception {
        String creatorID = record.getString("CreatorID");
        String nameURI = nameURIMap.get(creatorID);

        if (nameURI != null) {
            json.put("creator", mapper.getReferenceObject(nameURI));
        } else if (!creatorID.isEmpty() && !creatorID.equals("0")) {
            print("No mapped creator found ...");
        }
    }

    /**
     * Add the names to the resource or resource component record
     *
     * @param json       object record
     * @param creatorIds The array of subject ids
     * @throws Exception
     */
    private JSONArray addCreators(JSONObject json, JSONArray creatorIds, String recordTitle) throws Exception {
        JSONArray linkedAgentsJA = new JSONArray();

        for (int i = 0; i < creatorIds.length(); i++) {
            String id = creatorIds.getString(i);
            String nameURI = nameURIMap.get(id);

            if (nameURI != null) {
                JSONObject linkedAgentJS = new JSONObject();

                linkedAgentJS.put("role", "creator");
                linkedAgentJS.put("ref", nameURI);
                linkedAgentsJA.put(linkedAgentJS);

                if (debug) print("Added creator to " + recordTitle);
            } else {
                print("No mapped name found ...");
            }
        }

        // if we had any subjects add them parent json record
        json.put("linked_agents", linkedAgentsJA);

        return linkedAgentsJA;
    }

    /**
     * Method to add subjects which are really creators
     * @param linkedAgentsJA
     * @param subjectAsCreatorsList
     */
    private void addSubjectsAsCreators(JSONArray linkedAgentsJA, ArrayList<String> subjectAsCreatorsList, String recordTitle) throws JSONException {
        for (String id: subjectAsCreatorsList) {
            String nameURI = nameURIMap.get(id);

            if (nameURI != null) {
                JSONObject linkedAgentJS = new JSONObject();

                linkedAgentJS.put("role", "subject");
                linkedAgentJS.put("ref", nameURI);
                linkedAgentsJA.put(linkedAgentJS);

                if (debug) print("Added creator to " + recordTitle);
            } else {
                print("No mapped name found ...");
            }
        }
    }

    /**
     * Method to create an agent record and add it to the Accession record as a donor
     *
     * @param linkedAgentsJA
     * @param accession
     * @param accessionTitle
     */
    private void addDonor(JSONArray linkedAgentsJA, JSONObject accession, String accessionTitle) throws Exception {
        String donorName = accession.getString("Donor");

        String nameURI = getDonorURI(donorName, accession, accessionTitle);

        if (nameURI != null) {
            JSONObject linkedAgentJS = new JSONObject();

            linkedAgentJS.put("role", "source");
            linkedAgentJS.put("relator", "dnr");
            linkedAgentJS.put("ref", nameURI);
            linkedAgentsJA.put(linkedAgentJS);

            if (debug) print("Added donor to " + accessionTitle);
        } else {
            print("No mapped name found ...");
        }
    }

    /**
     * Method to see if we need to save an agent record for the donor or just use an existing one
     *
     * @param donorName
     * @param accession
     * @param accessionTitle
     * @return
     */
    private String getDonorURI(String donorName, JSONObject accession, String accessionTitle) throws Exception {
        String uri = null;

        if(nameURIMap.containsKey(donorName)) {
            uri = nameURIMap.get(donorName);
        } else {
            JSONObject agentJS = mapper.convertAccessionDonor(accession);

            if (agentJS != null) {
                String id = saveRecord(ASpaceClient.AGENT_PEOPLE_ENDPOINT, agentJS.toString(), "Donor_Person->" + donorName);

                if (!id.equalsIgnoreCase(NO_ID)) {
                    uri = ASpaceClient.AGENT_PEOPLE_ENDPOINT + "/" + id;
                    nameURIMap.put(donorName, uri);
                    print("Copied Donor: " + donorName + " :: " + id);
                } else {
                    print("Failed -- Donor: " + donorName);
                }
            }
        }

        return uri;
    }

    /**
     * Add the classifications to the json resource, or accession record
     *
     * @param repoURI This is needed since classification are linked to specific repository
     * @param json The json representation of the AR record
     * @param classificationIds  The classification ids
     * @param recordTitle The record parent record title
     * @throws Exception
     */
    private void addClassifications(String repoURI, JSONObject json, JSONArray classificationIds, String recordTitle) throws Exception {
        JSONArray classificationsJA = new JSONArray();

        for (int i = 0; i < classificationIds.length(); i++) {
            String key = repoURI + "_" + classificationIds.getString(i);
            String classificationURI = getClassificationURI(key);

            if (classificationURI != null) {
                classificationsJA.put(mapper.getReferenceObject(classificationURI));

                if (debug) print("Added classification to " + recordTitle);
            } else {
                print("No mapped classification found ...");
            }
        }

        // if we had any subjects add them parent json record
        if (classificationsJA.length() != 0) {
            json.put("classifications", classificationsJA);
        }
    }

    /**
     * Method to add an instance to an Accession or Resource record which hold location information
     *  @param recordJS
     * @param locations
     */
    private void addLocationInstances(JSONObject recordJS, JSONArray locations, String instanceType,
                                      HashMap<String, String> topContainerURIs, String repoURI) throws Exception {
        if (topContainerURIs == null) topContainerURIs = new HashMap<String, String>();

        JSONArray instancesJA = new JSONArray();

        for (int i = 0; i < locations.length(); i++) {

            JSONObject location = locations.getJSONObject(i);

            String content = location.getString("Content");

            // if it says 'all' in the content field, add the location to all existing containers
            if (content.trim().equalsIgnoreCase("all") && !topContainerURIs.isEmpty()) {
                for (String topContainerURI : topContainerURIs.values()) {
                    try {
                        JSONObject containerJS = new JSONObject(aspaceClient.get(topContainerURI, null));
                        addLocationInfo(containerJS, location);
                        String id = saveRecord(topContainerURI, containerJS.toString(), null);
                        if (!id.equalsIgnoreCase(NO_ID)) {
                            print("Added Location to Top Container: all");
                        } else {
                            print("Fail -- Add location to Top Container: all");
                        }
                    } catch (NullPointerException e) {
                        print("Fail -- Add location to Top Container: all\nCould not load " + topContainerURI);
                    }
                }
                continue;
            }

            String[] info = getTypeAndIndicator(content);

            String containerType = info[0];

            ArrayList<String> containerIndicators = getContainerIndicators(info[1]);

            for (String containerIndicator : containerIndicators) {

                String topContainerURI = null;
                String containerKey = containerType + " " + containerIndicator;

                topContainerURI = topContainerURIs.get(containerKey);

                if (topContainerURI == null) {
                    createInstanceForLocation(location, instanceType, containerType, containerIndicator, instancesJA,
                            topContainerURIs, repoURI);
                } else {
                    try {
                        JSONObject containerJS = new JSONObject(aspaceClient.get(topContainerURI, null));
                        addLocationInfo(containerJS, location);
                        String id = saveRecord(topContainerURI, containerJS.toString(), null);
                        if (!id.equalsIgnoreCase(NO_ID)) {
                            print("Added Location to Top Container: " + containerKey);
                        } else {
                            print("Fail -- Add location to Top Container: " + containerKey);
                        }
                    } catch (NullPointerException e) {
                        print("Fail -- Add location to Top Container: " + containerKey + "\nCould not load " + topContainerURI);
                    }
                }
            }
        }

        // if we had any instances add them parent json record
        if (instancesJA.length() != 0) {
            recordJS.put("instances", instancesJA);
        }
    }

    /**
     * this attempts to tell if the location content is referring to multiple containers
     * again, could be better but not worth increasing migration time too much
     *
     * @param indicatorsString
     * @return
     */
    private ArrayList<String> getContainerIndicators(String indicatorsString) {
        ArrayList<String> containerIndicators = new ArrayList<String>();

        // split at commas since they probably indicate separate items
        String[] commasSplit = indicatorsString.split(",");
        for (String str: commasSplit) {

            // a dash probably indicates a range
            // if the items on either side are numbers use these numbers and if so add these and all in between
            if (str.contains("-")) {
                String[] bounds = str.split("-");
                if (bounds.length == 2) {
                    try {
                        int start = Integer.parseInt(bounds[0].trim());
                        int end = Integer.parseInt(bounds[1].trim());
                        if (start >= end) throw new NumberFormatException();
                        for (int i = start; i <= end; i++) {
                            containerIndicators.add(i + "");
                        }
                    } catch (NumberFormatException e) {
                        containerIndicators.add(str.trim());
                    }
                }
            } else {
                containerIndicators.add(str.trim());
            }
        }
        return containerIndicators;
    }

    /**
     * creates an instance for a location that is linked to a resource/accession
     *
     * @param location
     * @param instanceType
     * @param containerType
     * @param containerIndicator
     * @param instancesJA
     * @param topURIs
     * @param repoURI
     * @throws Exception
     */
    public void createInstanceForLocation(JSONObject location, String instanceType, String containerType,
                                          String containerIndicator, JSONArray instancesJA,
                                          HashMap<String, String> topURIs, String repoURI) throws Exception {
        JSONObject instanceJS = new JSONObject();

        // set the type
        instanceJS.put("instance_type", instanceType);

        // add the container now
        JSONObject containerJS = new JSONObject();

        String containerURI = addTopContainer(containerType, containerIndicator, topURIs, repoURI,
                location, null);
        JSONObject containerRef = mapper.getReferenceObject(containerURI);

        containerJS.put("top_container", containerRef);

        instanceJS.put("sub_container", containerJS);

        instancesJA.put(instanceJS);

    }

    /**
     * Method to either create or return a location record based on the coordinate and so on
     * @param building
     * @param coordinate1
     * @param coordinate2
     * @param coordinate3
     * @return
     */
    private String getLocationURI(String building, String coordinate1, String coordinate2, String coordinate3) throws Exception {

        String key = building;

        // lets create a JSON object for the location in case we need to save it
        JSONObject locationJS = new JSONObject();
        locationJS.put("building", building);

        if (!coordinate1.equals("null") && !coordinate1.isEmpty()) {
            locationJS.put("coordinate_1_label", "Range");
            locationJS.put("coordinate_1_indicator", coordinate1);
            key += "-" + coordinate1;
        } else {
            // put in dummy range so record saves
            locationJS.put("coordinate_1_label", "Range");
            locationJS.put("coordinate_1_indicator", "0");
            key += "-0";
        }

        if (!coordinate2.equals("null") && !coordinate2.isEmpty()) {
            locationJS.put("coordinate_2_label", "Section");
            locationJS.put("coordinate_2_indicator", coordinate2);
            key += "-" + coordinate2;
        }

        if (!coordinate3.equals("null") && !coordinate3.isEmpty()) {
            locationJS.put("coordinate_3_label", "Shelf");
            locationJS.put("coordinate_3_indicator", coordinate3);
            key += "-" + coordinate3;
        }

        if(locationURIMap.containsKey(key)) {
            return locationURIMap.get(key);
        } else {
            locationTotal++;

            String id = saveRecord(ASpaceClient.LOCATION_ENDPOINT, locationJS.toString(), "Location->" + key);

            if (!id.equalsIgnoreCase(NO_ID)) {
                String uri = ASpaceClient.LOCATION_ENDPOINT + "/" + id;
                locationURIMap.put(key, uri);
                locationSuccess++;
                print("Copied Location: " + key + " :: " + id);
                return uri;
            } else {
                print("Fail -- Location: " + key);
                return null;
            }
        }
    }

    /**
     * if after going through all the resources a digital object liked resource/component is not found add it
     *
     * @throws Exception
     */
    public void copyDigitalObjectStragglers() throws Exception {

        for (int rid : digitalObjectMap.keySet()) {

            HashMap<Integer, ArrayList<JSONArray>> leftoverDOs = digitalObjectMap.get(rid);

            if (leftoverDOs != null) {

                String repoURI = getRepositoryURI(defaultRepositoryId);
                String batchEndpoint = repoURI + ASpaceClient.BATCH_IMPORT_ENDPOINT;

                for (int cid : leftoverDOs.keySet()) {
                    ArrayList<JSONArray> digitalObjects = leftoverDOs.get(cid);
                    String message = "Digital object linked resource/component not found: Resource " +
                            rid + ", Component " + cid + "\nLinked to " + digitalObjects.size() +
                            " digital object(s). Digital object(s) will be unlinked in stead.\n";
                    addErrorMessage(message);

                    for (JSONArray digitalObjectJA : digitalObjects) {
                        saveDigitalObject(batchEndpoint, digitalObjectJA);
                    }
                }

                // update the number of digital records that were copied
                updateRecordTotals("Instance Digital Objects", digitalObjectTotal, digitalObjectSuccess);
            }
        }
    }

    /**
     * Method to start the start the time watch
     */
    private void startWatch() {
        stopWatch = new StopWatch();
        stopWatch.start();
    }

    private String stopWatch() {
        stopWatch.stop();
        return stopWatch.getPrettyTime();
    }

    /**
     * Method to return the status of getting the session needed to create certain records
     *
     * @return
     */
    public boolean getSession() {
        if (simulateRESTCalls) return true;

        boolean connected = aspaceClient.getSession();

        if (connected) {
            aspaceClient.setASpaceCopyUtil(this);
            aspaceInformation = aspaceClient.getArchivesSpaceInformation();
            setASpaceVersion();
        }

        return connected;
    }

    /**
     * Method to extract the aspace version from the information return from the backend
     */
    private void setASpaceVersion() {
        try {
            JSONObject infoJS = new JSONObject(aspaceInformation);
            String version = infoJS.getString("archivesSpaceVersion");
            int end = version.lastIndexOf(".");
            aspaceVersion = version.substring(0, end);
        } catch (Exception e) {
        }
    }

    /**
     * Method to return the aspace version
     */
    public String getASpaceVersion() {
        return aspaceVersion;
    }

    /** Method to add to resource map in a thread safe manner
     *
     * @param oldIdentifier
     * @param uri
     */
    private void updateResourceURIMap(String oldIdentifier, String uri) {
        resourceURIMap.put(oldIdentifier, uri);
    }

    /**
     * Method to increment the number of resource records copied
     */
    private void incrementCopyCount() {
        copyCount++;
    }

    /**
     * Method to delete any previously saved resource records on the ASpace backend.
     * Useful for testing purposes, but not much else
     */
    private void deleteSavedResources() {
        print("\nNumber of Resources to Delete: " + resourceURIMap.size());
        ArrayList<String> deletedKeys = new ArrayList<String>();

        // initialize the progress bar
        updateProgress("Resource", resourceURIMap.size(), -1);

        int count = 1;
        for(String key: resourceURIMap.keySet()) {
            try {
                String uri = resourceURIMap.get(key);
                print("Deleting Resource: " + uri);

                String message = aspaceClient.deleteRecord(uri);
                print(message + "\n");
                deletedKeys.add(key);

                // update the progress bar
                updateProgress("Resource", 0, count);
                count++;
            } catch (Exception e) {
                e.printStackTrace();
                print("Error deleting ... \n" + e.getMessage());
            }
        }

        // remove the deleted keys now, since we can't modify a
        // hashmap while we still iterating over it
        for(String key: deletedKeys) {
            resourceURIMap.remove(key);
        }
    }

    /**
     * Method to return the new repository for a given domain object.
     *
     * @param repoID
     * @return The URI of the new repository
     */
    private String getRepositoryURI(String repoID) {
        // check to see if old repo is not null. If it is then we need to just return anyone
        // since this should never occur in a properly formatted AT database
        if(repoID != null && !repoID.isEmpty() && repositoryURIMap.containsKey(repoID)) {
            return repositoryURIMap.get(repoID);
        } else {
            return "/repositories/2";
        }
    }

    /**
     * Method to return a repository URI for a particular accession record
     *
     * @param accessionID
     * @return
     */
    private String getAccessionRepositoryURI(String accessionID) {
        if(accessionRepositoryIdMap.containsKey(accessionID)) {
            return getRepositoryURI(accessionRepositoryIdMap.get(accessionID));
        } else {
            return getRepositoryURI(defaultRepositoryId);
        }
    }

    /**
     * Method to return the new repository for a given domain object.
     *
     * @param key The hash map key
     * @return The URI of the new repository
     */
    private String getClassificationURI(String key) {
        if (classificationURIMap.containsKey(key)) {
            linkedClassificationSet.add(key);
            return classificationURIMap.get(key);
        } else if (classificationTermURIMap.containsKey(key)) {
            String sa[] = classificationTermURIMap.get(key).split(",");
            linkedClassificationSet.add(sa[1]);
            return sa[0];
        }else {
            return null;
        }
    }

    /**
     * Method to save the record that takes into account running in stand alone
     * or within the AT
     *
     * @param endpoint to make post to
     * @param jsonText record
     */
    public String saveRecord(String endpoint, String jsonText, String atId) {
        return saveRecord(endpoint, jsonText, null, atId);
    }

    /**
     * Method to save the record that takes into account running in stand alone
     * or within the AT
     *
     * @param endpoint to make post to
     * @param jsonText record
     * @param params   parameters to pass to service
     */
    public String saveRecord(String endpoint, String jsonText, NameValuePair[] params, String atId) {
        String id = NO_ID;

        try {
            // Make sure we don't try to print out a batch import record since they can
            // be thousands of lines long
            if(endpoint.contains(ASpaceClient.BATCH_IMPORT_ENDPOINT)) {
                print("Route: " + endpoint + "\nBatch Record Length: " + jsonText.length() + " bytes");
            } else {
                print("Route: " + endpoint + "\n" + jsonText);
            }

            // we may want to save this record to the file system for debugging purposes
            if(recordDumpDirectory != null) {
                saveRecordToFile(jsonText);
            }

            if(simulateRESTCalls) {
                id = "1001" + randomString.nextString();
                Thread.sleep(2);
            } else {
                id = aspaceClient.post(endpoint, jsonText, params, atId);
            }
        } catch (Exception e) {
            if(endpoint.contains(ASpaceClient.BATCH_IMPORT_ENDPOINT)) {
                print("Error saving batch import record ...");
            } else {
                print("Error saving record " + jsonText);
            }

            incrementSaveErrorCount();
            incrementASpaceErrorCount();
        }

        return id;
    }

    /**
     * Method to save the json record to the file system. Mainly used for debug purposes
     *
     * @param jsonText
     */
    private void saveRecordToFile(String jsonText) {
        String filename = currentRecordType.replace(" ", "_") + currentRecordDBID + ".json";
        File file = new File(recordDumpDirectory, filename);
        FileManager.saveTextData(file, jsonText);
    }

    /**
     * Method to increment the error count
     */
    public void incrementSaveErrorCount() {
        saveErrorCount++;
    }

    /**
     * Method to increment the aspace error count that occur when saving to the
     * backend
     */
    public void incrementASpaceErrorCount() {
        aspaceErrorCount++;
        updateErrorCountLabel();
    }

    private void updateErrorCountLabel() {
        if(errorCountLabel != null) {
            errorCountLabel.setText(aspaceErrorCount + " and counting ...");
        }
    }

    /**
     * Convenient print method for printing string in the text console in the future
     *
     * @param string
     */
    public void print(String string) {
        if(outputConsole != null) {
            messageCount++;

            if(messageCount < MAX_MESSAGES) {
                outputConsole.append(string + "\n");
            } else {
                messageCount = 0;
                outputConsole.setText(string + "\n");
            }
        } else {
            System.out.println(string);
        }
    }

    /**
     * Method to update the progress bar if not running in command line mode
     *
     * @param recordType
     * @param total
     * @param count
     */
    private void updateProgress(String recordType, int total, int count) {
        if(progressBar == null) return;

        if(count == -1) {
            progressBar.setMinimum(0);
            progressBar.setMaximum(total);
            progressBar.setString("Deleting " + total + " " + recordType);
        } else if(count == 0) {
            progressBar.setMinimum(0);
            progressBar.setMaximum(1);
            progressBar.setString("Loading " + recordType);
        } else if(count == 1) {
            progressBar.setMinimum(0);
            progressBar.setMaximum(total);
            progressBar.setString("Copying " + total + " " + recordType);
        }

        progressBar.setValue(count);
    }

    /**
     * Method to update the record totals
     *
     * @param recordType
     * @param total
     * @param success
     */
    private void updateRecordTotals(String recordType, int total, int success) {
        float percent = (new Float(success)/new Float(total))*100.0f;
        recordTotals.put(recordType, recordType + " : " + success + " / " + total + " (" + String.format("%.2f", percent) + "%)");
    }

    /**
     * Method to return the number of errors when saving records
     *
     * @return
     */
    public int getSaveErrorCount() {
        return saveErrorCount;
    }

    /**
     * Get the total error count
     * @return
     */
    public int getASpaceErrorCount() {
        return aspaceErrorCount;
    }

    /**
     * Method to add an error message to the buffer
     *
     * @param message
     */
    public void addErrorMessage(String message) {
        errorBuffer.append(message).append("\n");
        incrementASpaceErrorCount();
    }

    /**
     * Method to add an change log message to the buffer
     *
     * @param message
     */
    public void addChangeMessage(String message) {
        changelogBuffer.append(message).append("\n");
        changelogCount++;
    }

    /**
     * Method to return the error messages that occurred during the transfer process
     * @return
     */
    public String getSaveErrorMessages() {
        int errorsAndWarnings = Math.abs(aspaceErrorCount - saveErrorCount);

        String errorMessage = "RECORD CONVERSION ERRORS/WARNINGS ( " + errorsAndWarnings + " ) ::\n\n" + errorBuffer.toString() +
                "\n\n\nRECORD SAVE ERRORS ( " + saveErrorCount + " ) ::\n\n" + aspaceClient.getErrorMessages() +
                "\n\nTOTAL COPY TIME: " + stopWatch.getPrettyTime() +
                "\n\nNUMBER OF RECORDS COPIED: \n" + getTotalRecordsCopiedMessage();

        return errorMessage;
    }

    /**
     * Method to return the change log messages that occurred during the transfer process
     * @return
     */
    public String getChangeLogMessages() {
        String changelogMessage = "RECORD CONVERSION CHANGES NOTED ( " + changelogCount + " ) ::\n\n" + changelogBuffer.toString();
        return changelogMessage;
    }

    /**
     * Method to do certain task after the copy has completed
     */
    public void cleanUp() {
        copying = false;

        // remove any un-linked classifications
        deleteUnlinkedClassifications();

        String totalRecordsCopied = getTotalRecordsCopiedMessage();

        print("\n\nFinished copying data ... Total time: " + stopWatch.getPrettyTime());
        print("\nNumber of Records copied: \n" + totalRecordsCopied);

        print("\nNumber of errors/warnings: " + aspaceErrorCount);
    }

    /**
     * Method to delete all the unlinked classifications
     */
    public void deleteUnlinkedClassifications() {
        for(String key: classificationURIMap.keySet()) {
            if(!linkedClassificationSet.contains(key)) {
                print("Deleting unlinked classification: " + key);
                String uri = classificationURIMap.get(key);

                if(!simulateRESTCalls) {
                    try {
                        aspaceClient.deleteRecord(uri);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } else {
                print("Linked classification: " + key);
            }
        }
    }

    /**
     * Method to return the current status of the migration
     *
     * @return
     */
    public String getCurrentProgressMessage() {
        int errorsAndWarnings = Math.abs(aspaceErrorCount - saveErrorCount);

        String totalRecordsCopied = getTotalRecordsCopiedMessage();

        String errorMessages = "RECORD CONVERSION ERRORS/WARNINGS ( " + errorsAndWarnings + " ) ::\n\n" + errorBuffer.toString() +
                "\n\n\nRECORD SAVE ERRORS ( " + saveErrorCount + " ) ::\n\n" + aspaceClient.getErrorMessages();

        String message = errorMessages +
                "\n\nRunning for: " + stopWatch.getPrettyTime() +
                "\n\nCurrent # of Records Copied: \n" + totalRecordsCopied;

        return message;
    }

    /**
     * Method to return string with total records copied
     * @return
     */
    private String getTotalRecordsCopiedMessage() {
        String totalRecordsCopied = "";

        for(String entry: recordTotals.values()) {
            totalRecordsCopied += entry + "\n";
        }

        return totalRecordsCopied;
    }

    /**
     * Method to set the boolean which specifies whether to stop copying the resources
     */
    public void stopCopy() {
        stopCopy = true;
    }

    /**
     * Method to check if the copying process is running
     *
     * @return
     */
    public boolean isCopying() {
        return copying;
    }

    /**
     * Method to set the whether the copying process is running
     *
     * @param copying
     */
    public void setCopying(boolean copying) {
        this.copying = copying;
    }

    /**
     * Method to try and free some memory by refreshing the hibernate session and running GC
     */
    private void freeMemory() {
        if(outputConsole != null) {
            outputConsole.setText("");
        }

        Runtime runtime = Runtime.getRuntime();

        long freeMem = runtime.freeMemory();
        System.out.println("\nFree memory before GC: " + freeMem/1048576L + "MB");

        runtime.gc();

        freeMem = runtime.freeMemory();
        System.out.println("Free memory after GC:  " + freeMem/1048576L + "MB");

        System.out.println("Number of client threads: "  + getTotalASpaceClients() + "\n");
    }

    /**
     * Method to return the keys in inverted order. Used to correctly assign creator
     * relationships, otherwise a relationship is created to an agent that's not yet created
     *
     * @param records
     * @return
     */
    private String[] getKeysInInvertedOrder(JSONObject records) {
        String[] invertedKeys = new String[records.length()];
        int start = records.length() - 1;

        Iterator<String> keys = records.sortedKeys();
        while (keys.hasNext()) {
            invertedKeys[start] = keys.next();
            start--;
        }

        return invertedKeys;
    }

    /**
     * Method to get the current aspace client
     *
     * @return current number of aspace client
     */
    private synchronized int getTotalASpaceClients() {
        return totalASpaceClients;
    }

    /**
     * Method used to simulate the REST calls. Useful for testing memory usage and for setting baseline
     * data transfer time
     *
     * @param simulateRESTCalls
     */
    public void setSimulateRESTCalls(boolean simulateRESTCalls) {
        this.simulateRESTCalls = simulateRESTCalls;
    }

    /**
     * Method to specify whether to delete the save resources.
     *
     * @param deleteSavedResources
     */
    public void setDeleteSavedResources(boolean deleteSavedResources) {
        this.deleteSavedResources = deleteSavedResources;
    }

    /**
     * Method to set the resources to copy
     *
     * @param collectionsIDsList
     */
    public void setCollectionsToCopyList(ArrayList<String> collectionsIDsList) {
        if(collectionsIDsList.size() != 0) {
            this.collectionsIDsList = collectionsIDsList;
        } else {
            this.collectionsIDsList = null;
        }
    }

    /**
     * Method to set the resources to copy by archon collection ID
     *
     * @param collArchonIDsList
     */
    public void setCollArchonIDToCopyList(ArrayList<String> collArchonIDsList) {
        if(collArchonIDsList.size() != 0) {
            this.collArchonIDsList = collArchonIDsList;
        } else {
            this.collArchonIDsList = null;
        }
    }

    /**
     * Method to get the current
     * @return
     */
    public String getCurrentRecordInfo()  {
        String info = "Current Record Type: " + currentRecordType +
                "\nRecord Identifier : " + currentRecordIdentifier;

        return info;
    }

    /**
     * Method to extract the id from a given URI
     * @param uri
     * @return
     */
    private String getIdFromURI(String uri) {
        int beginIndex = uri.lastIndexOf("/") + 1;
        return uri.substring(beginIndex).replace("\"}", "");
    }

    /**
     * Print the EnumIDsToValues list entries for a given starting string
     * for testing/debugging 
     * @param type
     */
    public void printEnumIDList(String type){
        HashMap<String, String> enumIDList = enumUtil.getEnumListIDsToValues();
        System.out.println("\n**Contents of Enum ID List for " + type + "**");
        for (Map.Entry<String, String> e : enumIDList.entrySet()){
            if(e.getKey().startsWith(type)){    
                System.out.println("Key: " + e.getKey() + " Value: " + e.getValue());
            }
        }
        System.out.println("**End contents of Enum ID List for " + type + "**");
    }

    /**
     * Method to test the conversion without having to startup the gui
     *
     * @param args
     */
    public static void main(String[] args) throws JSONException {
        //String host = "http://archives-dev.library.illinois.edu/archondev/tracer";
        String host = "http://localhost/~nathan/archon";
        String username = "admin";
        String password = "admin";
        if (UIUCPropertiesReader.getUIUCProperties() != null) {
            host = UIUCPropertiesReader.getUIUCProperties().getProperty("archon.source");
            username = UIUCPropertiesReader.getUIUCProperties().getProperty("archon.user");
            password = UIUCPropertiesReader.getUIUCProperties().getProperty("archon.password");
        }
        
        ArchonClient archonClient = new ArchonClient(host, username, password);

        archonClient.getSession();

        String aspaceHost = "http://54.227.35.51:8089";
        String aspaceAdminUser = "admin";
        String aspacePassword = "admin";

        if (UIUCPropertiesReader.getUIUCProperties() != null) {
            aspaceHost = UIUCPropertiesReader.getUIUCProperties().getProperty("aspace.host");
            aspaceAdminUser = UIUCPropertiesReader.getUIUCProperties().getProperty("aspace.admin");
            aspacePassword = UIUCPropertiesReader.getUIUCProperties().getProperty("aspace.password");
        }

        ASpaceCopyUtil aspaceCopyUtil  = new ASpaceCopyUtil(archonClient, aspaceHost, aspaceAdminUser, aspacePassword);
        aspaceCopyUtil.setSimulateRESTCalls(false);
        aspaceCopyUtil.getSession();
        aspaceCopyUtil.setBBCodeOption("-bbcode_html");

        //limit collections for testing
        ArrayList<String> collectionsIDsList = new ArrayList<String>();
        collectionsIDsList.add("54");
        //aspaceCopyUtil.setCollectionsToCopyList(collectionsIDsList);

        //limit collections for testing by archon collection ID
        ArrayList<String> collArchonIDsList = new ArrayList<String>();
        collArchonIDsList.add("8434");
        aspaceCopyUtil.setCollArchonIDToCopyList(collArchonIDsList);
        
        archonClient.setDebugMode(false);
        aspaceCopyUtil.mapper.setAppendTestIdentifier("test");

        try {
            /*
            File recordDirectory = new File("/Users/nathan/temp/JSON_Records");
            aspaceCopyUtil.setRecordDumpDirectory(recordDirectory);*/
            aspaceCopyUtil.setDefaultRepositoryId("1");

            aspaceCopyUtil.copyEnumRecords();
            aspaceCopyUtil.copyRepositoryRecords();
            aspaceCopyUtil.mapRepositoryGroups();
            aspaceCopyUtil.copyUserRecords();
            aspaceCopyUtil.copySubjectRecords();
            aspaceCopyUtil.copyCreatorRecords();
            aspaceCopyUtil.copyClassificationRecords();
            aspaceCopyUtil.findAccessionRecordRepositories();
            aspaceCopyUtil.copyAccessionRecords();
            aspaceCopyUtil.copyDigitalObjectRecords();
            aspaceCopyUtil.copyCollectionRecords(100000);

            //aspaceCopyUtil.downloadDigitalObjectFiles(new File("/Users/nathan/temp/archon_files"));

            // removed all unused classifications
            aspaceCopyUtil.deleteUnlinkedClassifications();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // print out the error messages
        System.out.println("\n\nSave Errors:\n" + aspaceCopyUtil.getSaveErrorMessages());
        // print out record changes
        System.out.println("\n\nData changes:\n" + aspaceCopyUtil.getChangeLogMessages());
        System.exit(0);
    }

    public void addContainerTypeValueToIDMapping(String value, String id) {
        archonValuesToIDs.put(value.toLowerCase().trim(), id);
    }

    private String getContainerTypeArchonID(String value) {
        return archonValuesToIDs.get(value.toLowerCase().trim());
    }
}
