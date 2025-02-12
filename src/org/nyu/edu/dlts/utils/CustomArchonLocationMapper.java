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
            setLocationMappingVariables(customOption);
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
            String text = IOUtils.toString(this.getClass().getResourceAsStream("custom-location-info.json"), "UTF-8");
            JSONObject customInfo = new JSONObject(text);
                    //set variables based on json
            if(customInfo.has(optionCode)) {
                JSONObject optionCodeJS = customInfo.getJSONObject(optionCode);
                if(optionCodeJS.has("FileName")) locationMapFileName = optionCodeJS.getString("FileName");
                if(optionCodeJS.has("SectionSeparator")) archonSectionSeparator = optionCodeJS.getString("SectionSeparator");
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
     * Method to get the section shelf separator
     * @return
     */
    public String getSectionSeparator(){
        return archonSectionSeparator;
    }
}