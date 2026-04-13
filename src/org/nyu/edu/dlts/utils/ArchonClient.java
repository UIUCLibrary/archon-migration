package org.nyu.edu.dlts.utils;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeMap;

/**
 * Created by IntelliJ IDEA.
 * User: nathan
 * Date: 10/22/2013
 * Time: 3:59 PM
 *
 * This class handles all posting and reading from the ASpace project
 */
public class ArchonClient {
    private PrintConsole printConsole = null;

    public static final String ADMIN_LOGIN_ENDPOINT = "?p=core/authenticate";

    public static final String ENUM_USER_GROUPS_ENDPOINT = "?p=core/enums&enum_type=usergroups";
    public static final String ENUM_SUBJECT_SOURCES_ENDPOINT = "?p=core/enums&enum_type=subjectsources";
    public static final String ENUM_CREATOR_SOURCES_ENDPOINT = "?p=core/enums&enum_type=creatorsources";
    public static final String ENUM_EXTENT_UNITS_ENDPOINT = "?p=core/enums&enum_type=extentunits";
    public static final String ENUM_MATERIAL_TYPES_ENDPOINT = "?p=core/enums&enum_type=materialtypes";
    public static final String ENUM_ACCESSION_TYPES_ENDPOINT = "?p=core/enums&enum_type=materialtypes&accessiontype=true";
    public static final String ENUM_CONTAINER_TYPES_ENDPOINT = "?p=core/enums&enum_type=containertypes";
    public static final String ENUM_FILE_TYPES_ENDPOINT = "?p=core/enums&enum_type=filetypes";
    public static final String ENUM_PROCESSING_PRIORITIES_ENDPOINT = "?p=core/enums&enum_type=processingpriorities";
    public static final String ENUM_COUNTRIES_ENDPOINT = "?p=core/enums&enum_type=countries";

    public static final String REPOSITORY_ENDPOINT = "?p=core/repositories";
    public static final String USER_ENDPOINT = "?p=core/users";
    public static final String SUBJECT_ENDPOINT = "?p=core/subjects";
    public static final String CREATOR_ENDPOINT = "?p=core/creators";
    public static final String CLASSIFICATION_ENDPOINT = "?p=core/classifications";
    public static final String ACCESSION_ENDPOINT = "?p=core/accessions";
    public static final String DIGITAL_OBJECT_ENDPOINT = "?p=core/digitalcontent";
    public static final String DIGITAL_FILE_ENDPOINT = "?p=core/digitalfiles";
    public static final String DIGITAL_FILE_BLOB_ENDPOINT = "?p=core/digitalfileblob";
    public static final String COLLECTION_ENDPOINT = "?p=core/collections";
    public static final String COLLECTION_CONTENT_ENDPOINT = "?p=core/content";

    private HttpClient httpclient = new HttpClient();

    private String host = "";

    private String username = "";

    private String password = "";

    // String that stores the session
    private String session;

    // String that holds connection message
    private String connectionMessage;

    // let keep all the errors we encounter so we can have a log
    private StringBuilder errorBuffer = new StringBuilder();

    // boolean to use to execute debug code
    private boolean debug = true;

    // hashmap used to store the raw Archon response in any in pieces key by their their endpoint
    private HashMap<String, String> archonRecordsMap = new HashMap<String, String>();

    /**
     * The main constructor
     *
     * @param host
     * @param username
     * @param password
     */
    public ArchonClient(String host, String username, String password) {
        this.host = host;
        this.username = username;
        this.password = password;
    }

    /**
     * Constructor that takes a host name and valid session
     *
     * @param host
     * @param session
     */
    public ArchonClient(String host, String session) {
        this.host = host;
        this.session = session;
    }

    /**
     * Method to return the host name
     *
     * @return
     */
    public String getHost() {
        return host;
    }

    /**
     * Method to get the session using the admin login
     */
    public String getSession() {
        // set the connection messaged to fail for now
        connectionMessage = "Connection to Archon Instance @ " + host + " failed ...";

        session = null;

        httpclient.getState().setCredentials(
 	        AuthScope.ANY,
 	        new UsernamePasswordCredentials(username, password));

        // get a session id using the admin login
        String fullUrl = host + ADMIN_LOGIN_ENDPOINT;

        GetMethod get = new GetMethod(fullUrl);
        get.setDoAuthentication(true);

        try {
            int statusCode = httpclient.executeMethod(get);

            String statusMessage = "Status code: " + statusCode +
                    "\nStatus text: " + get.getStatusText();

            if (debug) {
                System.out.println("username: " + username  + ", password: " + password);
                System.out.println("post: " + fullUrl);
                System.out.println(statusMessage);
            }

			if (get.getStatusCode() == HttpStatus.SC_OK) {
                try {
					String responseBody = get.getResponseBodyAsString();
                    JSONObject jsonObject = new JSONObject(responseBody);
                    session = jsonObject.getString("session");

                    connectionMessage = "Connection to Archon Instance @ " + host + " established ...";
                    if (debug) System.out.println("response: " + responseBody);
				} catch (Exception e) {
                    session = null;
				}
			}
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } finally {
            get.releaseConnection();
        }

        // session was generated so return true
        return session;
    }

    /**
     * get the connection message
     */
    public String getConnectionMessage() {
        return connectionMessage + "\n\n";
    }

    /**
     * Method to return a JSON object from the call a get method
     *
     * @param endpoint Location of resource
     * @return
     * @throws Exception
     */
    public String get(String endpoint, NameValuePair[] params) throws Exception {
        String fullUrl = host + endpoint;
        GetMethod get = new GetMethod(fullUrl);

        // set any parameters
        if(params != null) {
            get.setQueryString(params);
        }

        // add session to the header if it's not null
        if(session != null) {
            get.setRequestHeader("session", session);
        }

		// set the token in the header
		//get.setRequestHeader("Authorization", "OAuth " + accessToken);
        String responseBody = null;

		try {
            if (debug) System.out.println("get: " + fullUrl);

            int statusCode = httpclient.executeMethod(get);

            String statusMessage = "Status code: " + statusCode +
                    "\nStatus text: " + get.getStatusText();

            if (debug) System.out.println(statusMessage);

			if (get.getStatusCode() == HttpStatus.SC_OK) {
                try {
					responseBody = get.getResponseBodyAsString();

                    if (debug) System.out.println("response: " + responseBody);
				} catch (Exception e) {
                    errorBuffer.append(statusMessage).append("\n\n").append(responseBody).append("\n");
					e.printStackTrace();
					throw e;
				}
			} else {
                errorBuffer.append(statusMessage).append("\n");
            }
		} finally {
			get.releaseConnection();
		}

        return responseBody;
    }

    /**
     * Method return all the enums from the ASpace backend
     *
     * @return
     */
    public JSONObject getAllEnums() {
        JSONObject enumsJS = new JSONObject();

        String[] archonEnums = {ENUM_USER_GROUPS_ENDPOINT, ENUM_SUBJECT_SOURCES_ENDPOINT,
        ENUM_CONTAINER_TYPES_ENDPOINT, ENUM_CREATOR_SOURCES_ENDPOINT, ENUM_EXTENT_UNITS_ENDPOINT,
        ENUM_FILE_TYPES_ENDPOINT, ENUM_ACCESSION_TYPES_ENDPOINT, ENUM_MATERIAL_TYPES_ENDPOINT, ENUM_PROCESSING_PRIORITIES_ENDPOINT,
                ENUM_COUNTRIES_ENDPOINT};

        for(String endpoint: archonEnums) {
            System.out.println("Enum List:"  + endpoint);
            JSONObject records = getEnumList(endpoint);

            try {
                if(!endpoint.equals(ENUM_ACCESSION_TYPES_ENDPOINT)) {
                    enumsJS.put(endpoint, records);
                } else {
                    enumsJS.put("accessiontypes", records);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            System.out.println("\n");
        }

        return enumsJS;
    }

    /**
     * Method to get a particular enum
     *
     * @return JSONObject representing the enum list
     */
    public JSONObject getEnumList(String endpoint) {
        try {
            String completeEndpoint = endpoint + "&batch_start=1";
            String jsonText = get(completeEndpoint, null);

            // need to check to see if an array is returned or a json object
            JSONObject jsonObject = null;

            if(jsonText.startsWith("{")) {
                jsonObject = new JSONObject(jsonText);
            } else { // must be array
                JSONArray jsonArray = new JSONArray(jsonText);
                jsonObject = convertJSONArrayToJSONObject(jsonArray);
            }

            // if countries we need to make some more passes
            if(endpoint.equals(ENUM_COUNTRIES_ENDPOINT)) {
                getAllRecords(jsonObject, endpoint, 101);
            }

            return jsonObject;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Method to return an array list containing the repository records ID and short name
     *
     * @return
     */
    public ArrayList<String> getRepositoryRecordsList() {
        ArrayList<String> repositoryList = new ArrayList<String>();

        JSONObject recordsJS = getRepositoryRecords();
        Iterator<String> keys = recordsJS.keys();

        while (keys.hasNext()) {
            String key = keys.next();
            try {
                JSONObject repository = recordsJS.getJSONObject(key);
                String repoID = repository.getString("ID");
                String shortName = repository.getString("Name");
                repositoryList.add(repoID + " - " + shortName);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return repositoryList;
    }

    /**
     * Method to return the repository records
     *
     * @return
     */
    public JSONObject getRepositoryRecords() {
        return getRecords(REPOSITORY_ENDPOINT);
    }

    /**
     * Method to return the user records
     *
     * @return
     */
    public JSONObject getUserRecords() {
        JSONObject recordsJS = new JSONObject();
        try {
            getAllRecords(recordsJS, USER_ENDPOINT, 1);
            return recordsJS;
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            return null;
        }
    }

    /**
     * Method to return the subject records
     *
     * @return
     */
    public JSONObject getSubjectRecords() {
        JSONObject recordsJS = new JSONObject();
        try {
            getAllRecords(recordsJS, SUBJECT_ENDPOINT, 1);
            return recordsJS;
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            return null;
        }
    }

    /**
     * Method to get creator records
     *
     * @return
     */
    public JSONObject getCreatorRecords() {
        JSONObject recordsJS = new JSONObject();
        try {
            getAllRecords(recordsJS, CREATOR_ENDPOINT, 1);
            return recordsJS;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Method to get the classification records
     *
     * @return
     */
    public JSONObject getClassificationRecords() {
        JSONObject recordsJS = new JSONObject();
        try {
            getAllRecords(recordsJS, CLASSIFICATION_ENDPOINT, 1);

            // now we have to iterate through the return records and place the child records
            // with their parent classifications, by doing several loops which is fine since there
            // are not going to be large amount of records

            // first place all the records in a hashmap to make lookup efficient
            HashMap<String, JSONObject> recordsMap = new HashMap<String, JSONObject>();
            ArrayList<JSONObject> classificationList = new ArrayList<JSONObject>();
            Iterator<String> keys = recordsJS.keys();

            while (keys.hasNext()) {
                String key = keys.next();

                JSONObject recordJS = recordsJS.getJSONObject(key);
                recordJS.put("id_parts", "");

                String id = recordJS.getString("ID");
                recordsMap.put(id, recordJS);

                // if this is a parent classification add it to the parent list
                if(recordJS.getString("ParentID").equals("0")) {
                    classificationList.add(recordJS);
                }
            }

            // now go through the records and gather all children into an array
            HashMap<String, JSONArray> childrenMap = new HashMap<String, JSONArray>();

            for(String key: recordsMap.keySet()) {
                JSONObject recordJS = recordsMap.get(key);

                // first check to see if this is not a parent, if it is then continue
                if(recordJS.getString("ParentID").equals("0")) {
                    // store the identifier to make lookup easy when we need it for collection ID
                    recordJS.put("id_parts", recordJS.get("ClassificationIdentifier"));
                    continue;
                }

                String parentID = getRootParent(recordsMap, recordJS, recordJS);

                // add the component json object to the hash map now
                if(childrenMap.containsKey(parentID)) {
                    JSONArray childrenJA = childrenMap.get(parentID);
                    childrenJA.put(recordJS);
                } else {
                    JSONArray childrenJA = new JSONArray();
                    childrenJA.put(recordJS);
                    childrenMap.put(parentID, childrenJA);
                }
            }

            // now for each classification record, add the children records
            recordsJS = new JSONObject();

            for(JSONObject recordJS: classificationList) {
                String id = recordJS.getString("ID");
                if(childrenMap.containsKey(id)) {
                    JSONArray sortedChildrenJA = sortCollectionItems(childrenMap.get(id));
                    recordJS.put("children", sortedChildrenJA);
                } else {
                    recordJS.put("children", new JSONArray());
                }

                recordsJS.put(id, recordJS);
            }

            return recordsJS;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Method used to assign the position value for collection children so they sort correctly
     *
     * @param childrenJA
     */
    private JSONArray sortCollectionItems(JSONArray childrenJA) throws JSONException {
        TreeMap<String, JSONObject> treeMap = new TreeMap<String, JSONObject>();
        JSONArray sortedChildrenJA = new JSONArray();

        // first put all the children in a tree map so they get sorted
        for(int i = 0; i < childrenJA.length(); i++) {
            JSONObject childJS = childrenJA.getJSONObject(i);
            String key = childJS.get("ParentID") + "-" + childJS.get("ClassificationIdentifier");
            treeMap.put(key, childJS);
        }

        // now process the now sorted children and add a position value
        int position = 1;
        for(JSONObject childJS: treeMap.values()) {
            childJS.put("Position", position++);
            sortedChildrenJA.put(childJS);
        }

        return sortedChildrenJA;
    }

    /**
     * Method to
     * @param recordsMap
     * @param recordJS
     * @return
     */
    private String getRootParent(HashMap<String, JSONObject> recordsMap, JSONObject recordJS, JSONObject classificationTermJS) throws Exception {
        if(recordJS.getString("ParentID").equals("0")) {
            String idParts = "";
            if(!classificationTermJS.getString("id_parts").isEmpty()) {
                idParts = classificationTermJS.get("id_parts") + "/" + recordJS.get("id_parts");
            } else {
                idParts = recordJS.getString("id_parts");
            }

            classificationTermJS.put("id_parts", idParts);

            return recordJS.getString("ID");
        } else {
            if(classificationTermJS.getString("id_parts").isEmpty()) {
                classificationTermJS.put("id_parts", recordJS.get("ClassificationIdentifier"));
            } else {
                String idParts = classificationTermJS.get("ClassificationIdentifier") + "/" + recordJS.get("ClassificationIdentifier");
                classificationTermJS.put("id_parts", idParts);
            }

            JSONObject parentJS = recordsMap.get(recordJS.getString("ParentID"));

            return getRootParent(recordsMap, parentJS, classificationTermJS);
        }
    }

    /**
     * Method to get the classification records
     *
     * @return
     */
    public JSONObject getAccessionRecords() {
        JSONObject recordsJS = new JSONObject();
        try {
            getAllRecords(recordsJS, ACCESSION_ENDPOINT, 1);
            return recordsJS;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Method to get the digital object records
     *
     * @return
     */
    public JSONObject getDigitalObjectRecords() {
        JSONObject recordsJS = new JSONObject();
        JSONObject componentRecordsJS = new JSONObject();

        try {
            // first get the digital content records
            getAllRecords(recordsJS, DIGITAL_OBJECT_ENDPOINT, 1);

            // now get all the child records
            getAllRecords(componentRecordsJS, DIGITAL_FILE_ENDPOINT, 1);

            // now we need to go through the child records and organize by parent records
            HashMap<String, JSONArray> digitalComponentMap = new HashMap<String, JSONArray>();
            Iterator<String> keys = componentRecordsJS.keys();

            while(keys.hasNext()) {
                String key = keys.next();

                //JSONArray componentJA = componentRecordsJS.getJSONArray(key);
                JSONObject componentJS = componentRecordsJS.getJSONObject(key);

                String parentID = componentJS.getString("DigitalContentID");

                if(debug) System.out.println("Digital Files: " + componentJS.getString("Title")
                        + " :: Digital Content ID: " + parentID);

                // add the component json object to the hash map now
                if(digitalComponentMap.containsKey(parentID)) {
                    JSONArray componentsJA = digitalComponentMap.get(parentID);
                    componentsJA.put(componentJS);
                } else {
                    JSONArray componentsJA = new JSONArray();
                    componentsJA.put(componentJS);
                    digitalComponentMap.put(parentID, componentsJA);
                }
            }

            // now for each of the return digital objects add there component records
            keys = recordsJS.keys();

            while(keys.hasNext()) {
                String key = keys.next();
                JSONObject recordJS = recordsJS.getJSONObject(key);

                if(digitalComponentMap.containsKey(key)) {
                    recordJS.put("components", digitalComponentMap.get(key));

                    if(debug) System.out.println("Added Digital Object Components to " + recordJS.getString("Title"));
                } else {
                    recordJS.put("components", new JSONArray());
                }
            }

            return recordsJS;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Method to download a digital object file to the download directory
     *
     * @param id
     * @return
     */
    public boolean downloadDigitalObjectFile(File downloadDirectory, String id, String filename) throws Exception {
        String fullUrl = host + DIGITAL_FILE_BLOB_ENDPOINT + "&fileid=" + id;
        GetMethod get = new GetMethod(fullUrl);

        // add session to the header if it's not null
        if (session != null) {
            get.setRequestHeader("session", session);
        }

        byte[] responseBytes;
        FileOutputStream fos = null;
        boolean downloaded = false;

        try {

            if (debug) System.out.println("get: " + fullUrl);

            int statusCode = httpclient.executeMethod(get);

            String statusMessage = "Status code: " + statusCode +
                    "\nStatus text: " + get.getStatusText();

            if (debug) System.out.println(statusMessage);

            if (get.getStatusCode() == HttpStatus.SC_OK) {
                try {
                    responseBytes = get.getResponseBody();

                    // now save the bytes to a file
                    File file = new File(downloadDirectory, filename);
                    fos = new FileOutputStream(file);
                    fos.write(responseBytes);

                    downloaded = true;
                    if (debug) System.out.println("response bytes: " + responseBytes.length);
                } catch (Exception e) {
                    errorBuffer.append(statusMessage).append("\n\n").append(filename).append("\n");
                    e.printStackTrace();
                    throw e;
                }
            } else {
                errorBuffer.append(statusMessage).append("\n");
            }
        } finally {
            get.releaseConnection();

            if(fos != null) {
                fos.close();
            }
        }

        return downloaded;
    }

    /**
     * Method to get the collection records
     *
     * @return
     */
    public JSONObject getCollectionRecords() {
        JSONObject recordsJS = new JSONObject();
        try {
            getAllRecords(recordsJS, COLLECTION_ENDPOINT, 1);
            return recordsJS;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Method to get the collection contents record for a given collection
     *
     * @param collectionID
     * @return
     */
    public JSONObject getCollectionContentRecords(String collectionID) {
        JSONObject recordsJS = new JSONObject();
        try {
            String endpoint = COLLECTION_CONTENT_ENDPOINT + "&cid=" + collectionID;
            getAllRecords(recordsJS, endpoint, 1);

            return recordsJS;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Method to return the list of archon records
     */
    private JSONObject getRecords(String endpoint) {
        try {
            String completeEndpoint = endpoint + "&batch_start=1";
            String jsonText = get(completeEndpoint, null);

            // need to check to see if an array is returned or a json object
            JSONObject jsonObject = null;

            if(jsonText.startsWith("{")) {
                jsonObject = new JSONObject(jsonText);
            } else { // must be array
                JSONArray jsonArray = new JSONArray(jsonText);
                jsonObject = convertJSONArrayToJSONObject(jsonArray);
            }

            return jsonObject;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }


    /**
     * Method to get all the JSON records from the Archon backend
     *
     * @param parentRecordJS
     * @param endpoint
     * @param batchStart
     */
    private void getAllRecords(JSONObject parentRecordJS, String endpoint, int batchStart) throws Exception {
        do {
            String completeEndpoint = endpoint + "&batch_start=" + batchStart;

            String message = "Getting records for: " + completeEndpoint;
            if (debug) System.out.println(message);

            if(printConsole != null) {
                printConsole.print(message);
            }

            String jsonText = get(completeEndpoint, null);

            if (jsonText.startsWith("{")) {
                JSONObject jso = new JSONObject(jsonText);
                appendToJSONObject(parentRecordJS, jso);

                if(debug) {
                    archonRecordsMap.put(completeEndpoint, jso.toString(2));
                }

                batchStart += 100;
            } else if(jsonText.startsWith("[")) {
                JSONArray jsonArray = new JSONArray(jsonText);
                JSONObject jso = convertJSONArrayToJSONObject(jsonArray);
                appendToJSONObject(parentRecordJS, jso);
                batchStart += 100;
            } else {
                break;
            }
        } while (true);
    }

    /**
     * Method to append records from one JSON object to the next
     *
     * @param jsonObject
     * @param jso
     */
    private void appendToJSONObject(JSONObject jsonObject, JSONObject jso) throws JSONException {
        Iterator<String> keys = jso.keys();

        while (keys.hasNext()) {
            String key = keys.next();
            JSONObject jsoToAdd = jso.getJSONObject(key);
            String id = jsoToAdd.getString("ID");
            jsonObject.put(id, jsoToAdd);
        }
    }

    /**
     * Method to convert a json array from archon to a json object
     *
     * @param jsonArray
     * @return
     * @throws JSONException
     */
    private JSONObject convertJSONArrayToJSONObject(JSONArray jsonArray) throws JSONException {
        JSONObject jsonObject = new JSONObject();

        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject jso = jsonArray.getJSONObject(i);
            String id = jso.getString("ID");
            jsonObject.put(id, jso);
        }

        return jsonObject;
    }

    /**
     * Method to get any error messages that occurred while talking to the AT backend
     *
     * @return String containing error messages
     */
    public String getErrorMessages() {
        return errorBuffer.toString();
    }

    /**
     * Method to get the params from a comma separated string
     *
     * @param paramString
     * @return
     */
    private  NameValuePair[] getParams(String paramString) {
        String[] parts = paramString.split("\\s*,\\s*");

        // make sure we have parameters, otherwise exit
        if(paramString.isEmpty() || parts.length < 1) {
            return null;
        } else {
            NameValuePair[] params = new NameValuePair[parts.length];

            for(int i = 0; i < parts.length; i++) {
                try {
                    String[] sa = parts[i].split("\\s*=\\s*");
                    params[i] = new NameValuePair(sa[0], sa[1]);
                } catch(Exception e) {
                    return null;
                }
            }

            return params;
        }
    }

    /**
     * Return the hash map containing the raw archon response keyed by the endpoint
     *
     * @return
     */
    public HashMap<String, String> getArchonRecordsMap() {
        return archonRecordsMap;
    }

    /**
     * Set the print console so messages can be sent to the user
     *
     * @param printConsole
     */
    public void setPrintConsole(PrintConsole printConsole) {
        this.printConsole = printConsole;
    }

    /**
     * Set the debug mode
     *
     * @param debugOption
     */
    public void setDebugMode(Boolean debugOption) {
        this.debug = debugOption;
    }

    /**
     * Method to test the client
     *
     * @param args
     */
    public static void main(String[] args) throws JSONException {
        //String host = "http://archives-dev.library.illinois.edu/archondev/tracer";
        String host = System.getenv("ARCHON_CLIENT_HOST");
        String user = System.getenv("ARCHON_CLIENT_USER");
        String pass = System.getenv("ARCHON_CLIENT_PASS");
        ArchonClient archonClient = new ArchonClient(host, user, pass);
        archonClient.getSession();

        // the json object containing list of records
        JSONObject records;

        /* test access of the enums
        String[] archonEnums = {ENUM_USER_GROUPS_ENDPOINT, ENUM_SUBJECT_SOURCES_ENDPOINT,
        ENUM_CONTAINER_TYPES_ENDPOINT, ENUM_CREATOR_SOURCES_ENDPOINT, ENUM_EXTENT_UNITS_ENDPOINT,
        ENUM_FILE_TYPES_ENDPOINT, ENUM_MATERIAL_TYPES_ENDPOINT, ENUM_PROCESSING_PRIORITIES_ENDPOINT,
                ENUM_COUNTRIES_ENDPOINT};

        for(String endpoint: archonEnums) {
            System.out.println("Enum List:"  + endpoint);
            records = archonClient.getEnumList(endpoint);
            System.out.println("\n");
        }*/


        // get the repositories
        //records = archonClient.getRepositoryRecords();

        // get the users
        //records = archonClient.getUserRecords();

        // get the subjects
        //records = archonClient.getSubjectRecords();
        //System.out.println("Total Subjects: " + records.getString("total_records"));

        // get the names/creators
        //records = archonClient.getCreatorRecords();
        //System.out.println("Total Creators: " + records.getString("total_records"));

        // get the classifications
        //records = archonClient.getClassificationRecords();
        //System.out.println("Total Classifications: " + records.getString("total_records"));

        // get the accession
        //records = archonClient.getAccessionRecords();
        //System.out.println("Total Accessions: " + records.length());

        // get the digital object records
        records = archonClient.getDigitalObjectRecords();
        System.out.println("Total Digital Objects: " + records.length());

        Iterator<String> keys = records.keys();
        while(keys.hasNext()) {
            String key = keys.next();
            JSONObject recordJS = records.getJSONObject(key);
            // System.out.println(recordJS); // SHOW THE GENERATED JSON
            ASpaceMapper mapper = new ASpaceMapper();
            try {
                JSONObject dobj  = mapper.convertDigitalObject(recordJS);
                 // System.out.println(dobj); // SHOW THE DOBJ JSON PAYLOAD
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (recordJS.has("components")) {
                JSONArray components = recordJS.getJSONArray("components");
                for (int i=0 ; i < components.length(); i++) {
                    JSONObject component = components.getJSONObject(i);
                    // System.out.println(component); // SHOW THE COMPONENT
                    try {
                        JSONObject dobjc  = mapper.convertToDigitalObjectComponent(component);
                        // System.out.println(dobjc); // SHOW THE DOBJC PAYLOAD
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        /*records = archonClient.getCollectionRecords();
        System.out.println("Total Collection Records: " + records.length());

        Iterator<String> keys = records.keys();
        while(keys.hasNext()) {
            String key  = keys.next();

            if(key.equals("811")) {
                JSONObject contentRecordsJS = archonClient.getCollectionContentRecords(key);
                System.out.println("Total Collection Content Records: " + contentRecordsJS.length());
            }
        }
        */
    }
}
