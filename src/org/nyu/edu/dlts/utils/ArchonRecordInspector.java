package org.nyu.edu.dlts.utils;

import org.json.JSONException;
import org.json.JSONObject;
import org.nyu.edu.dlts.utils.uiuc.UIUCPropertiesReader;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;

/**
 * A simple class which allow inspecting of Archon records for doing analysis
 *
 * Created by nathan on 3/4/2015.
 */

public class ArchonRecordInspector {
    // used to connect to the archon client
    private static ArchonClient archonClient;

    // variables which store a particular archon record
    private static JSONObject contentRecordsJS;
    private static  HashMap<String, String> archonRecordsMap;

    // file used to save the records locally
    private static File jsonFile;
    private static File mapFile;

    private static File parentDirectory;

    /**
     * Method to load the collection content for a particular collection record
     * @param cidKey
     */
    public static void loadCollectionContent(String cidKey) {
        System.out.println("Reading Collection Content for Collection: " + cidKey);

        // get the aggregated json record
        contentRecordsJS = archonClient.getCollectionContentRecords(cidKey);

        // get the raw records for each call in a hasp map
        archonRecordsMap = archonClient.getArchonRecordsMap();

        // now save these records to files
        jsonFile = new File(parentDirectory, "content_" + cidKey + ".json");
        mapFile = new File(parentDirectory, "content_" + cidKey + ".bin");

        try {
            FileManager.saveTextData(jsonFile, contentRecordsJS.toString(2));
            FileManager.saveObjectToFile(mapFile, archonRecordsMap);
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Method to load collection content record from files instead of the archon backend
     *
     * @param cidKey
     */
    public static void loadCollectionContentFromFile(String cidKey) {
        System.out.println("Loading content record from file ...\n");

        jsonFile = new File(parentDirectory, "content_" + cidKey + ".json");
        mapFile = new File(parentDirectory, "content_" + cidKey + ".bin");

        try {
            contentRecordsJS = FileManager.getJSONObject(jsonFile);
            archonRecordsMap = (HashMap<String, String>) FileManager.getObjectFromFile(mapFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Method to run test on the collection content
     */
    private static void processCollectionContent() {
        String fullText = "";

        Iterator<String> keys = contentRecordsJS.keys();
        while(keys.hasNext()) {
            String key = keys.next();
            System.out.println("Processing content " + key);

            try {
                JSONObject recordJS = contentRecordsJS.getJSONObject(key);
                String parentId = recordJS.getString("ParentID");

                if(parentId.equals(key)) {
                    System.out.println("Circular Relation Ship Between Parent And Child");
                    break;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        /* now get the number of records in the hash map
        int count = 0;
        for(String key: archonRecordsMap.keySet()) {
            System.out.println("content endpoint: " + key);

            try {
                JSONObject recordPart = new JSONObject(archonRecordsMap.get(key));
                System.out.println("Record length: " + recordPart.length());
                count += recordPart.length();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }*/

        System.out.println("\nNumber of Content Records From Merge JSON: " + contentRecordsJS.length());
    }

    /**
     * Method to
     */
    public static void loadAccessions() {
        JSONObject accessionRecordsJS = archonClient.getAccessionRecords();
        System.out.println("Number of Accessions: " + accessionRecordsJS.length());
    }

    /**
     * Method to load a particular collection record
     * @param searchFor
     */
    public static void loadCollection(String searchFor) {
        JSONObject collectionRecordsJS = archonClient.getCollectionRecords();

        Iterator<String> keys = collectionRecordsJS.keys();
        while(keys.hasNext()) {
            String key = keys.next();
            System.out.println("Processing collection " + key);

            try {
                JSONObject recordJS = collectionRecordsJS.getJSONObject(key);
                String identifier = recordJS.getString("CollectionIdentifier");

                if (identifier.equalsIgnoreCase(searchFor)) {
                    System.out.println("Found Record " + recordJS.get("Title"));

                    // get the collection content
                    JSONObject collectionContentsJS = archonClient.getCollectionContentRecords(key);

                    break;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Main method
     *
     * @param args
     */
    public static void main(String[] args) throws JSONException {
        // set the parent directory
        parentDirectory = new File("/Users/nathan/temp");

        //String host = "http://archives-dev.library.illinois.edu/archondev/tracer";
        String host = "http://quanta2.bobst.nyu.edu/~nathan/archon";
        //archonClient = new ArchonClient(host, "admin", "admin");
        //String host = "http://archivestest.unco.edu/archon";
        //String host = "http://localhost/~nathan/archon";
        //archonClient = new ArchonClient(host, "MigAdmin", "111zwSHOO");
        //String host = "http://128.122.90.55:9000/~nathan/archon";

        if (UIUCPropertiesReader.getUIUCProperties() != null) {
            host = UIUCPropertiesReader.getUIUCProperties().getProperty("archon.source");
            archonClient = new ArchonClient(host, UIUCPropertiesReader.getUIUCProperties().getProperty("archon.user"), UIUCPropertiesReader.getUIUCProperties().getProperty("archon.password"));
        } else {
            archonClient = new ArchonClient(host, "admin", "admin");
        }
//        archonClient = new ArchonClient(host, "admin", "admin");
        archonClient.getSession();

        System.out.println("Connected to " + host + "\n\n");

        // load the accessions records
        //loadAccessions();

        // load the collection record
//        loadCollection("27-3");

        // load the content record
        //loadCollectionContent("259");
        //loadCollectionContentFromFile("811");

        // process the content records
        //processCollectionContent();
    }
}
