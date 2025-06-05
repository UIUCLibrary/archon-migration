package org.nyu.edu.dlts.utils;

import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Class to provide ability to use subsets of records in Archon for testing
 */

public class ArchonIDListReader {

    private ArrayList<String> archonIDList;
    private String recordSubsetFileName = "";
    private String recordSubsetCode = "";
    private String suffixForTesting = "";

    /**
     * Default constructor 
     */
    public ArchonIDListReader(){
        setSubsetVariables("default");
        loadRecordSubset();
    }
    
    /**
     * Constructor using custom option indicator
     * Supplies the file name for each option
     * @param customOption
     */
    public ArchonIDListReader(String customOption){
        if(customOption != null && !customOption.isEmpty()){
            String lowercaseCustomOption = customOption.toLowerCase();
            setSubsetVariables(lowercaseCustomOption);
        } else {
            setSubsetVariables("default");
        }
        loadRecordSubset();
    }

    /**
     * Method to set the file name and subset code based on data in a json file
     * @param optionCode
     */
    private void setSubsetVariables(String optionCode){
        //load json
        try {
            String text = IOUtils.toString(this.getClass().getResourceAsStream("custom-json/custom-subset-info.json"), "UTF-8");
            JSONObject customInfo = new JSONObject(text);
                    //set variables based on json
            if(customInfo.has(optionCode)) {
                JSONObject optionCodeJS = customInfo.getJSONObject(optionCode);
                if(optionCodeJS.has("FileName")) recordSubsetFileName = optionCodeJS.getString("FileName");
                if(optionCodeJS.has("SubsetCode")) recordSubsetCode = optionCodeJS.getString("SubsetCode");
                if(optionCodeJS.has("IdentifierSuffixForTesting")) suffixForTesting = optionCodeJS.getString("IdentifierSuffixForTesting");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        archonIDList = new ArrayList<>();
    }
    
    /**
     * Method to load the record subset
     */
    private void loadRecordSubset(){
        if(recordSubsetFileName != null && !recordSubsetFileName.isEmpty()){
            try {
                String text = IOUtils.toString(this.getClass().getResourceAsStream(recordSubsetFileName), "UTF-8");
                JSONObject recordSubsets = new JSONObject(text);
                if(recordSubsets.has(recordSubsetCode)){
                    JSONArray subsetJArray = recordSubsets.getJSONArray(recordSubsetCode);
                    for(int i = 0; i < subsetJArray.length(); i++){
                        archonIDList.add(subsetJArray.getString(i));
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Method to get the record subset list
     * @return
     */
    public ArrayList<String> getRecordSubset(){
        return archonIDList;
    }

    /**
     * Method to get the subset code
     * @return
     */
    public String getSubsetCode(){
        return recordSubsetCode;
    }

    /**
     * Method to return a string to use for testing
     * @return
     */
    public String getSuffixForTesting(){
        return suffixForTesting;
    }

    /**
     * Method to test the class without running the whole process
     *
     * @param args
     */
    public static void main(String[] args) {
        ArchonIDListReader subsetReader = new ArchonIDListReader("ALA");
        String code = subsetReader.getSubsetCode();
        if(code != null){
            System.out.println("subset code: " + code);
        }else{
            System.out.println("code is null");
        }

        String testString = subsetReader.getSuffixForTesting();
        if(testString != null){
            System.out.println("suffix for testing: " + testString);
        }else{
            System.out.println("suffix is null");
        }

        ArrayList<String> subsetList  = subsetReader.getRecordSubset();
        if(subsetList != null && !subsetList.isEmpty()){
            System.out.println("subset list is set");
            System.out.println(subsetList);
        }else{
            System.out.println("subset list is null or empty");
        }
    }

}