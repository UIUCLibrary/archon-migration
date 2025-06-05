package org.nyu.edu.dlts.utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.nyu.edu.dlts.utils.uiuc.UIUCPropertiesReader;

import java.io.File;
import java.util.ArrayList;
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

    //for use without saving to ASpace
    private static HashMap<String, String> testClassificationIdentifiers = new HashMap<String, String>();
    private static HashMap<String, String> testClassificationParents = new HashMap<String, String>();

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
     * Method to load a particular collection record by archon database id
     * @param archonID
     */
    public static void loadCollectionByArchonID(String archonID) {
        JSONObject collectionRecordsJS = archonClient.getCollectionRecords();
        if(collectionRecordsJS.has(archonID)){
            try {
                JSONObject recordJS = collectionRecordsJS.getJSONObject(archonID);

                System.out.println("Found Record " + recordJS.get("Title"));

                //print collection json
                System.out.println("printing collection record json...");
                System.out.println(recordJS.toString(2));

                // get the collection content
                JSONObject collectionContentsJS = archonClient.getCollectionContentRecords(archonID);
                System.out.println("printing collection content json...");
                System.out.println(collectionContentsJS.toString(2));
                System.out.println("count of collection content in json:");
                System.out.println(collectionContentsJS.length());

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Method to load data into the classification identifiers and parents 
     * as needed for testing
     * @param testArchonClient
     * @param mapper
     * @param testArchonClient
     * @param mapper
     */
    private static void loadTestClassificationData(ArchonClient testArchonClient, ASpaceMapper mapper) throws Exception{
        
        JSONObject records = testArchonClient.getClassificationRecords();

        Iterator<String> keys = records.keys();
        while (keys.hasNext()) {
            String key = keys.next();

            JSONObject classification = records.getJSONObject(key);

            String arId = classification.getString("ID");

            JSONObject classificationJS = mapper.convertClassification(classification);

            JSONArray batchJA = new JSONArray();

            if (classificationJS != null) {
                batchJA.put(classificationJS);

                JSONArray classificationChildren = classification.getJSONArray("children");

                // add the id for this classification
                testClassificationIdentifiers.put(arId, classification.getString("ClassificationIdentifier"));

                for (int i = 0; i < classificationChildren.length(); i++) {

                    JSONObject classificationTerm = classificationChildren.getJSONObject(i);

                    String cId = classificationTerm.getString("ID");

                    testClassificationIdentifiers.put(cId, classificationTerm.getString("ClassificationIdentifier"));
                    String pId = classificationTerm.getString("ParentID");
                    testClassificationParents.put(cId, pId);
                }
            } else {
                continue;
            }
         }
        }

        /**
     * Method to test converting particular collection record by archon database id
     * Does NOT properly convert the identifier if classifications are used
     * Need to load test classification data first if using classifications in identifier
     * @param archonID
     * @param mapper
     * @param mapper
     */
    public static void testConvertCollection(String archonID, ASpaceMapper mapper) {
        JSONObject collectionRecordsJS = archonClient.getCollectionRecords();
        if(collectionRecordsJS.has(archonID)){
            try {
                JSONObject recordJS = collectionRecordsJS.getJSONObject(archonID);

                System.out.println("Found Record " + recordJS.get("Title"));
                try {
                    JSONObject convertedCollection  = mapper.convertCollection(recordJS,testClassificationIdentifiers,testClassificationParents);
                    System.out.println(convertedCollection.toString(2));
                } catch (Exception e) {
                    e.printStackTrace();
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

   /**
     * Method to test converting particular accession record by archon database id
     * @param archonID
     * @param mapper
     */
    public static void testConvertAccession(String archonID, ASpaceMapper mapper) {
        JSONObject accessionRecordsJS = archonClient.getAccessionRecords();
        if(accessionRecordsJS.has(archonID)){
            try {
                JSONObject recordJS = accessionRecordsJS.getJSONObject(archonID);

                System.out.println("Found Record " + recordJS.get("Title"));
                System.out.println(recordJS.toString(2));
                try {
                    JSONObject convertedAccession  = mapper.convertAccession(recordJS);
                    System.out.println(convertedAccession.toString(2));
                } catch (Exception e) {
                    e.printStackTrace();
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

   /**
     * Method to test converting particular classification record by archon database id
     * @param archonID
     * @param mapper
     */
    public static void testConvertClassification(String archonID, ASpaceMapper mapper) {
        JSONObject archonRecordsJS = archonClient.getClassificationRecords();
        if(archonRecordsJS.has(archonID)){
            try {
                JSONObject recordJS = archonRecordsJS.getJSONObject(archonID);

                System.out.println("Found Record " + recordJS.get("Title"));
                System.out.println(recordJS.toString(2));
                try {
                    JSONObject convertedRecord  = mapper.convertClassification(recordJS);
                    System.out.println("Converted Record " + recordJS.get("Title"));
                    System.out.println(convertedRecord.toString(2));
                } catch (Exception e) {
                    e.printStackTrace();
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Method to load a particular creator record by archon database id
     * @param archonID
     */
    public static void loadCreatorByArchonID(String archonID) {
        JSONObject creatorRecordsJS = archonClient.getCreatorRecords();
        if(creatorRecordsJS.has(archonID)){
            try {
                JSONObject recordJS = creatorRecordsJS.getJSONObject(archonID);

                System.out.println("Found Record " + recordJS.get("Name"));

                //print collection json
                System.out.println(recordJS.toString(2));

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Method to test converting particular creator record by archon database id
     * @param archonID
     * @param mapper
     */
    public static void testConvertCreator(String archonID, ASpaceMapper mapper) {
        JSONObject creatorRecordsJS = archonClient.getCreatorRecords();
        if(creatorRecordsJS.has(archonID)){
            try {
                JSONObject recordJS = creatorRecordsJS.getJSONObject(archonID);
                int creatorTypeId = recordJS.getInt("CreatorTypeID");

                System.out.println("Found Record " + recordJS.get("Name"));
                try {
                    JSONObject convertedcreator  = mapper.convertCreator(recordJS, creatorTypeId);
                    System.out.println(convertedcreator.toString(2));
                } catch (Exception e) {
                    e.printStackTrace();
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

        /**
     * Method to test converting particular creator record by archon database id
     * @param archonID
     * @param mapper
     */
    public static void testConvertDigitalObject(String archonID, ASpaceMapper mapper) {
        JSONObject digitalObjectRecordsJS = archonClient.getDigitalObjectRecords();
        if(digitalObjectRecordsJS.has(archonID)){
            try {
                JSONObject recordJS = digitalObjectRecordsJS.getJSONObject(archonID);

                System.out.println("Found Record with ArchonID " + recordJS.get("ID"));
                System.out.println(recordJS.toString(2));

                try {
                    JSONArray batchJA = new JSONArray();
                    JSONObject digitalObjectJS  = mapper.convertDigitalObject(recordJS);
                    if (digitalObjectJS != null) {
                        digitalObjectJS.put("jsonmodel_type", "digital_object");
                        batchJA.put(digitalObjectJS);
                        JSONArray digitalObjectChildren = recordJS.getJSONArray("components");
                        for (int i = 0; i < digitalObjectChildren.length(); i++) {
                            JSONObject digitalObjectChild = digitalObjectChildren.getJSONObject(i);

                            JSONObject digitalObjectChildJS = mapper.convertToDigitalObjectComponent(digitalObjectChild);

                            if (digitalObjectChildJS != null) {
                                batchJA.put(digitalObjectChildJS);
                            } 
                        }
                    }
                    System.out.println("Converted Record with ArchonID " + recordJS.get("ID"));
                    System.out.println(batchJA.toString(2));
                } catch (Exception e) {
                    e.printStackTrace();
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

        //test loading specific collections
        archonClient.setDebugMode(false);

        ASpaceMapper mapper = new ASpaceMapper();
        String identiferPrefix = null;
        if (UIUCPropertiesReader.getUIUCProperties() != null) {
            identiferPrefix = UIUCPropertiesReader.getUIUCProperties().getProperty("archon.prefix");
        }
        mapper.setIdentifierPrefix(identiferPrefix);

        try {
            loadTestClassificationData(archonClient,mapper);
        } catch(Exception e){
            System.out.println("Error loading classification data" + "\n\n");
        }
        System.out.println("Classification hashmap size: " + testClassificationIdentifiers.size() + "\n\n");
        System.out.println("Classification parents hashmap size: " + testClassificationParents.size() + "\n\n");
        
        String archonIDtoTest = "7394";//"8753";
        loadCollectionByArchonID(archonIDtoTest);
        testConvertCollection(archonIDtoTest, mapper);
         
        String archonCreatorIDtoTest = "3473";
        loadCreatorByArchonID(archonCreatorIDtoTest);
        testConvertCreator(archonCreatorIDtoTest, mapper);
        
        String archonDigitalIDtoTest = "3540";//"188";
        testConvertDigitalObject(archonDigitalIDtoTest, mapper);

        String archonAccesionIDtoTest = "142";
        testConvertAccession(archonAccesionIDtoTest, mapper);

        String archonClassificationIDtoTest = "3340";
        testConvertClassification(archonClassificationIDtoTest, mapper);

    }
}
