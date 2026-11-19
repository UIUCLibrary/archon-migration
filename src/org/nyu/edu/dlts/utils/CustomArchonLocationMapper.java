package org.nyu.edu.dlts.utils;

import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

/**
 * Class to provide custom location information mapping information
 */

public class CustomArchonLocationMapper {

    private JSONObject archonLocationsMap;
    private String archonSectionSeparator = "";
    private String locationMapFileName = "";
    private Boolean alwaysSplitSectionValue = false;

    /**
     * Default constructor 
     */
    public CustomArchonLocationMapper(){
        setLocationMappingVariables("default");
        loadLocationMapping();
    }
    
    /**
     * Constructor using custom option indicator
     * Supplies the section separator and file name for each option
     * @param customOption
     */
    public CustomArchonLocationMapper(String customOption){
        if(customOption != null && customOption != ""){
            String lowercaseCustomOption = customOption.toLowerCase();
            setLocationMappingVariables(lowercaseCustomOption);
        } else {
            setLocationMappingVariables("default");
        }
        loadLocationMapping();
    }

    /**
     * Method to set the file name and section separator variables based on data in a json file
     * @param optionCode
     */
    private void setLocationMappingVariables(String optionCode){
        //load json
        try {
            String text = IOUtils.toString(this.getClass().getResourceAsStream("custom-json/custom-location-info.json"), "UTF-8");
            JSONObject customInfo = new JSONObject(text);
                    //set variables based on json
            if(customInfo.has(optionCode)) {
                JSONObject optionCodeJS = customInfo.getJSONObject(optionCode);
                if(optionCodeJS.has("FileName")) locationMapFileName = optionCodeJS.getString("FileName");
                if(optionCodeJS.has("SectionSeparator")) archonSectionSeparator = optionCodeJS.getString("SectionSeparator");
                if(optionCodeJS.has("AlwaysSplitSectionValue")) alwaysSplitSectionValue = optionCodeJS.getBoolean("AlwaysSplitSectionValue");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Method to load the location mappings
     * To be called after the location map file name has been set
     */
    private void loadLocationMapping(){
        if(locationMapFileName != null && locationMapFileName != ""){
            try {
                String text = IOUtils.toString(this.getClass().getResourceAsStream(locationMapFileName), "UTF-8");
                archonLocationsMap = new JSONObject(text);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Method to get the locations map
     * @return
     */
    public JSONObject getLocationsMap(){
        return archonLocationsMap;
    }

    /**
     * Method to get the components of a given location from the locations map
     * @param locationText
     * @return
     */
    public JSONObject getLocationComponents(String locationText){
        if(archonLocationsMap != null && archonLocationsMap.has(locationText)){
            try{
                return archonLocationsMap.getJSONObject(locationText);
            } catch(Exception e) {
                e.printStackTrace();
                return null;
            }
        } else {
            return null;
        }
        
    }

    /**
     * Method to get the section shelf separator
     * @return
     */
    public String getSectionSeparator(){
        return archonSectionSeparator;
    }

    /**
     * Method to get whether to always split on section separator
     * @return
     */
    public Boolean getAlwaysSplitSectionValue(){
        return alwaysSplitSectionValue;
    }

    /**
     * Method to test the class without running the whole process
     *
     * @param args
     */
    public static void main(String[] args) throws JSONException {
        CustomArchonLocationMapper customMapper = new CustomArchonLocationMapper("UA");
        String separator = customMapper.getSectionSeparator();
        Boolean alwaysSplitSectionValue = customMapper.getAlwaysSplitSectionValue();
        if(separator != null){
            System.out.println("Separator: " + separator);
        }else{
            System.out.println("separator is null");
        }
        if(alwaysSplitSectionValue != null){
            System.out.println("Always split section value: " + alwaysSplitSectionValue);
        }else{
            System.out.println("separator is null");
        }

        try {
            JSONObject locationsMap  = customMapper.getLocationsMap();
            if(locationsMap != null){
                System.out.println("locations map is not null!");
                //System.out.println(locationsMap.toString(2));
                JSONObject locationComponents = customMapper.getLocationComponents("Test location text");
                if(locationComponents != null){
                    System.out.println(locationComponents.toString(2));
                    System.out.println(locationComponents.length());
                    System.out.println(locationComponents.getString("Area"));
                } else {
                    System.out.println("location component is null");
                }
            }else{
                System.out.println("locations map is null");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}