package org.nyu.edu.dlts.utils.uiuc;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * This util class is used to get information (archon an aspace credentials) from the uiuc.properties file.
 * This file contains sensitive information, should be located in the root of the project and should never be 
 * committed to the repository nor included in the .jar standalone file, it's just for local development/testing purposes.
 *
 * Created by: leonel
 * Date: 1/14/25
 */
public class UIUCPropertiesReader {
    private static Properties properties;

    public static Properties getUIUCProperties() {
        if (properties == null) {
            properties = new Properties();
            try {
                properties.load(new FileInputStream(System.getProperty("user.dir")+"/uiuc.properties"));
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        return properties;
    }

    public static void main(String[] args) {
        Properties properties = UIUCPropertiesReader.getUIUCProperties();
        if (properties == null) {
            System.out.println("Properties is null");
        } else {
            System.out.println("\nArchon Source: " + properties.getProperty("archon.source"));
        }
    }
}
