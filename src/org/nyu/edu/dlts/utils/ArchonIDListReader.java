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
     * Method to get the section shelf separator
     * @return
     */
    public String getSubsetCode(){
        return recordSubsetCode;
    }

    /**
     * Method to test the class without running the whole process
     *
     * @param args
     */
    public static void main(String[] args) throws JSONException {
        ArchonIDListReader subsetReader = new ArchonIDListReader("ALA");
        String code = subsetReader.getSubsetCode();
        if(code != null){
            System.out.println("subset code: " + code);
        }else{
            System.out.println("code is null");
        }

        try {
            ArrayList<String> subsetList  = subsetReader.getRecordSubset();
            if(subsetList != null && !subsetList.isEmpty()){
                System.out.println("subset list is not null or empty!");
                System.out.println(subsetList);
            }else{
                System.out.println("subset list is null or empty");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}