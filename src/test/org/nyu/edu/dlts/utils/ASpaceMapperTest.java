package test.org.nyu.edu.dlts.utils;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.nyu.edu.dlts.utils.uiuc.UIUCPropertiesReader;
import org.nyu.edu.dlts.utils.ASpaceEnumUtil;
import org.nyu.edu.dlts.utils.ASpaceMapper;
import org.nyu.edu.dlts.utils.ASpaceCopyUtil;
public class ASpaceMapperTest {
    
    @Test
    public void splitAlternativeExtentTest(){
        ASpaceMapper aSpaceMapper = new ASpaceMapper();
        String[]commaResults  = aSpaceMapper.splitAlternativeExtent("1.84 megabytes, 6 microfilm reels, 12 items, 1 oversize folder");
        String[]commaExpected = {"1.84 megabytes","6 microfilm reels","12 items","1 oversize folder"};
        // assertArrayEquals("does not equal",expected,results );
        assertArrayEquals("they are not equal" + Arrays.toString(commaResults), commaExpected, commaResults);

        String[]periodResults = aSpaceMapper.splitAlternativeExtent("1.84 megabytes. 6 microfilm reels. 12 items. 1 oversize folder");
        String[]periodExpected = {"1.84 megabytes","6 microfilm reels","12 items","1 oversize folder"};
        assertArrayEquals("they are not equal" + Arrays.toString(periodResults), periodResults, periodExpected);

        String[]andResults = aSpaceMapper.splitAlternativeExtent("1.84 megabytes and 6 microfilm reels and 12 items and 1 oversize folder");
        String[]andExpected = {"1.84 megabytes","6 microfilm reels","12 items","1 oversize folder"};
        assertArrayEquals("they are not equal" + Arrays.toString(andResults), andResults, andExpected);

        String[]leadingAndResults = aSpaceMapper.splitAlternativeExtent("and 1.84 megabytes and 6 microfilm reels and 12 items and 1 oversize folder");
        String[]leadingAndExpected = {"1.84 megabytes","6 microfilm reels","12 items","1 oversize folder"};
        assertArrayEquals("they are not equal" + Arrays.toString(leadingAndResults), leadingAndResults, leadingAndExpected);

        String[]singleAndResults = aSpaceMapper.splitAlternativeExtent("and 1 microfilm reel");
        String[]singleAndExpected = {"1 microfilm reel"};
        assertArrayEquals("they are not equal" + Arrays.toString(singleAndResults), singleAndResults, singleAndExpected);

        String[]noMatchResults = aSpaceMapper.splitAlternativeExtent("some large folders");
        String[]noMatchExpected = {"some large folders"};
        assertArrayEquals("they are not equal" + Arrays.toString(singleAndResults), singleAndResults, singleAndExpected);


    }

    @Test
    public void getNumber() {
        ASpaceMapper aSpaceMapper = new ASpaceMapper();
        assertEquals("1", aSpaceMapper.getNumber("a"));
        assertEquals("1", aSpaceMapper.getNumber("a single"));
        assertEquals("1", aSpaceMapper.getNumber("an"));
        assertEquals("3", aSpaceMapper.getNumber("three"));



    }

    @Test
    public void parseExtentStatementTest() throws JSONException{
        ASpaceMapper aSpaceMapper = new ASpaceMapper();
        String[] extentsPass = {"1.84 megabyte","6 microfilm reels","12 items","1 oversize folder","a big folder", "three cassettes"};
        String[] extentsFail = { "", "including photostats of 1934 book", "transcript and microfilm copy"};

        //1.84 megabyte
        JSONObject parsedExtentTest0 = aSpaceMapper.parseExtentStatement(extentsPass[0]);
        assertEquals("the units are not equal", "", parsedExtentTest0.get("unit"));
        assertEquals("the values are not equal", "1.84", parsedExtentTest0.get("unitValue"));
        assertEquals("the errors are not equal", false, parsedExtentTest0.get("exactMatch"));

        //6 microfilm reels
        JSONObject parsedExtentTest1 = aSpaceMapper.parseExtentStatement(extentsPass[1]);
        assertEquals("the units are not equal", "microfilm_reel", parsedExtentTest1.get("unit"));
        assertEquals("the values are not equal", "6", parsedExtentTest1.get("unitValue"));
        assertEquals("the errors are not equal", false, parsedExtentTest1.get("exactMatch"));

        //12 items
        JSONObject parsedExtentTest2 = aSpaceMapper.parseExtentStatement(extentsPass[2]);
        assertEquals("the units are not equal", "item", parsedExtentTest2.get("unit"));
        assertEquals("the values are not equal", "12", parsedExtentTest2.get("unitValue"));
        assertEquals("the errors are not equal", false, parsedExtentTest2.get("exactMatch"));

        //1 oversize folder"
        JSONObject parsedExtentTest3 = aSpaceMapper.parseExtentStatement(extentsPass[3]);
        assertEquals("the units are not equal", "folder", parsedExtentTest3.get("unit"));
        assertEquals("the values are not equal", "1", parsedExtentTest3.get("unitValue"));
        assertEquals("the errors are not equal", false, parsedExtentTest3.get("exactMatch"));

        //a big folder
        JSONObject parsedExtentTest4 = aSpaceMapper.parseExtentStatement(extentsPass[4]);
        assertEquals("the units are not equal", "folder", parsedExtentTest4.get("unit"));
        assertEquals("the values are not equal", "1", parsedExtentTest4.get("unitValue"));
        assertEquals("the errors are not equal", false, parsedExtentTest4.get("exactMatch"));

        //three cassettes
        JSONObject parsedExtentTest5 = aSpaceMapper.parseExtentStatement(extentsPass[5]);
        assertEquals("the units are not equal", "cassettes", parsedExtentTest5.get("unit"));
        assertEquals("the values are not equal", "3", parsedExtentTest5.get("unitValue"));
        assertEquals("the errors are not equal", true, parsedExtentTest5.get("exactMatch"));
        
        //"" 
        JSONObject parsedExtentTest6 = aSpaceMapper.parseExtentStatement(extentsFail[0]);
        assertEquals("the units are not equal", "", parsedExtentTest6.get("unit"));
        assertEquals("the values are not equal", "", parsedExtentTest6.get("unitValue"));
        assertEquals("the errors are not equal", false, parsedExtentTest6.get("exactMatch"));
        assertEquals("the confound values are not equal", "", parsedExtentTest6.get("altExtentStatement"));

        // including photostats of 1934 book
        JSONObject parsedExtentTest7 = aSpaceMapper.parseExtentStatement(extentsFail[1]);
        assertEquals("the units are not equal", "", parsedExtentTest7.get("unit"));
        assertEquals("the values are not equal", "", parsedExtentTest7.get("unitValue"));
        assertEquals("the errors are not equal", false, parsedExtentTest7.get("exactMatch"));
        assertEquals("the confound values are not equal", "including photostats of 1934 book", parsedExtentTest7.get("altExtentStatement"));

        //transcript and microfilm copy
        JSONObject parsedExtentTest8 = aSpaceMapper.parseExtentStatement(extentsFail[2]);
        assertEquals("the units are not equal", "", parsedExtentTest8.get("unit"));
        assertEquals("the values are not equal", "", parsedExtentTest8.get("unitValue"));
        assertEquals("the errors are not equal", false, parsedExtentTest8.get("exactMatch"));
        assertEquals("the confound values are not equal", "transcript and microfilm copy", parsedExtentTest8.get("altExtentStatement"));

    }

    @Test
    public void mapExtentTypeTest() throws Exception {
        ASpaceMapper aSpaceMapper = new ASpaceMapper();
        String[] inputExtents = {"cassettes","cubic feet","files","gigabytes","leaves","linear feet", "items","transcript", "microfilm copy", "including photostats of 1934 book", "box artifacts"};

        JSONObject mappedExtent0 = aSpaceMapper.mapExtentType(inputExtents[0]);
        assertEquals("the cleaned extent: " + mappedExtent0.get("clean_input"),"cassettes", mappedExtent0.get("mapping"));

        JSONObject mappedExtent1 = aSpaceMapper.mapExtentType(inputExtents[1]);
        assertEquals("the cleaned extent:" + mappedExtent1.get("clean_input"),"cubic_feet", mappedExtent1.get("mapping"));

        JSONObject mappedExtent2 = aSpaceMapper.mapExtentType(inputExtents[2]);
        assertEquals("the cleaned extent:" + mappedExtent2.get("clean_input"),"files", mappedExtent2.get("mapping"));

        JSONObject mappedExtent3 = aSpaceMapper.mapExtentType(inputExtents[3]);
        assertEquals("the cleaned extent:" + mappedExtent3.get("clean_input"),"gigabytes", mappedExtent3.get("mapping"));

        JSONObject mappedExtent4 = aSpaceMapper.mapExtentType(inputExtents[4]);
        assertEquals("the cleaned extent: " + mappedExtent4.get("clean_input"),"leaves", mappedExtent4.get("mapping"));

        JSONObject mappedExtent5 = aSpaceMapper.mapExtentType(inputExtents[5]);
        assertEquals("the cleaned extent: " + mappedExtent4.get("clean_input"),"linear_feet", mappedExtent5.get("mapping"));


    }

    @Test
    public void getParsedAltExtentsTest() throws JSONException {
        ASpaceMapper aSpaceMapper = new ASpaceMapper();
        String[] inputExtents = {
            "1 megabyte",
            "6 microfilm reels",
            "12 items",
            "1 oversize folder",
            "2 folders and 24 items",
            "1 folder. 14 items",
            "6 volumes, 54 items",
            "1 item, transcript and microfilm copy",
            "1 folder, including photostats of 1934 book",
            "1 volume and 3 items in 1 folder",
            "1 box artifacts",
            "4 typescripts",
            "113 letters",
            "1 megabyte, 1 megabyte, 3 items, 1 megabyte"
        };

        JSONArray extenJsonObject0 = aSpaceMapper.getParsedAltExtents(inputExtents[0]);
        assertEquals("The count is not the same",1, extenJsonObject0.length());
        assertTrue(extenJsonObject0.getJSONObject(0).getString("unit").equalsIgnoreCase(""));
        assertTrue(extenJsonObject0.getJSONObject(0).getString("unitValue").equals("1"));


        JSONArray extenJsonObject1 = aSpaceMapper.getParsedAltExtents(inputExtents[1]);
        assertEquals("The count is not the same",1, extenJsonObject1.length());
        
        JSONArray extenJsonObject2 = aSpaceMapper.getParsedAltExtents(inputExtents[2]);
        assertEquals("The count is not the same",1, extenJsonObject2.length());

        JSONArray extenJsonObject3 = aSpaceMapper.getParsedAltExtents(inputExtents[3]);
        assertEquals("The count is not the same",1, extenJsonObject3.length());

        JSONArray extenJsonObject4 = aSpaceMapper.getParsedAltExtents(inputExtents[4]);
        assertEquals("The count is not the same",2, extenJsonObject4.length());

        JSONArray extenJsonObject5 = aSpaceMapper.getParsedAltExtents(inputExtents[5]);
        assertEquals("The count is not the same",2, extenJsonObject5.length());

        JSONArray extenJsonObject6 = aSpaceMapper.getParsedAltExtents(inputExtents[6]);
        assertEquals("The count is not the same",2, extenJsonObject6.length());

        JSONArray extenJsonObject7 = aSpaceMapper.getParsedAltExtents(inputExtents[7]);
        assertEquals("The count is not the same",3, extenJsonObject7.length());

        JSONArray extenJsonObject8 = aSpaceMapper.getParsedAltExtents(inputExtents[8]);
        assertEquals("The count is not the same",2, extenJsonObject8.length());

        JSONArray extenJsonObject9 = aSpaceMapper.getParsedAltExtents(inputExtents[9]);
        assertEquals("The count is not the same",2, extenJsonObject9.length());

        JSONArray extenJsonObject10 = aSpaceMapper.getParsedAltExtents(inputExtents[10]);
        assertEquals("The count is not the same",1, extenJsonObject10.length());

        JSONArray extenJsonObject11 = aSpaceMapper.getParsedAltExtents(inputExtents[11]);
        assertEquals("The count is not the same",1, extenJsonObject11.length());

        JSONArray extenJsonObject12 = aSpaceMapper.getParsedAltExtents(inputExtents[12]);
        assertEquals("The count is not the same",1, extenJsonObject12.length());

        JSONArray extenJsonObject13 = aSpaceMapper.getParsedAltExtents(inputExtents[13]);
        assertEquals("The count is not the same",4, extenJsonObject13.length());
        assertTrue("no the true extent unit is: " + extenJsonObject13.getJSONObject(0).getString("unit"), extenJsonObject13.getJSONObject(0).getString("unit").equalsIgnoreCase(""));
        assertTrue("no the true unit value is: " + extenJsonObject13.getJSONObject(0).getString("unitValue"), extenJsonObject13.getJSONObject(0).getString("unitValue").equalsIgnoreCase("1"));
        assertTrue(!extenJsonObject13.getJSONObject(0).getBoolean("exactMatch"));
        assertTrue(extenJsonObject13.getJSONObject(0).getString("altExtentStatement").equals("1 megabyte"));




        //TODO: refactor with JSONAssert

    }

    @Test
    public void addResourceExtentTest() throws Exception {
        JSONObject simplifiedASpaceRecord = new JSONObject();
        JSONObject simplifiedArchonRecord = new JSONObject();
        
        simplifiedArchonRecord.put("ExtentUnitID", "1");
        simplifiedArchonRecord.put("Extent", "1");
        simplifiedArchonRecord.put("AltExtentStatement", "1 megabyte, 15 doors, 3 items, 1 gigabyte, 9 microfilm_reels, 9 microfilm reels");
        simplifiedArchonRecord.put("ExtentUnitID", 1);
        simplifiedArchonRecord.put("ID", "99");
        simplifiedArchonRecord.put("CollectionIdentifier", "sample collection identifier");
        
        ASpaceMapper aSpaceMapper = new ASpaceMapper();
        aSpaceMapper.addResourceExtent(simplifiedArchonRecord, simplifiedASpaceRecord);

        String mainExtentType = simplifiedASpaceRecord.getJSONArray("extents").getJSONObject(0).getString("extent_type");
        String mainExtentNumber = simplifiedASpaceRecord.getJSONArray("extents").getJSONObject(0).getString("number");
        String altExtentType1 = simplifiedASpaceRecord.getJSONArray("extents").getJSONObject(1).getString("extent_type");
        String altExtentNumber1 = simplifiedASpaceRecord.getJSONArray("extents").getJSONObject(1).getString("number");
        String altExtentType2 = simplifiedASpaceRecord.getJSONArray("extents").getJSONObject(2).getString("extent_type");
        String altExtentNumber2 = simplifiedASpaceRecord.getJSONArray("extents").getJSONObject(2).getString("number");
        String altExtentType3 = simplifiedASpaceRecord.getJSONArray("extents").getJSONObject(3).getString("extent_type");
        String altExtentNumber3 = simplifiedASpaceRecord.getJSONArray("extents").getJSONObject(3).getString("number");
        String altExtentType4 = simplifiedASpaceRecord.getJSONArray("extents").getJSONObject(4).getString("extent_type");
        String altExtentNumber4 = simplifiedASpaceRecord.getJSONArray("extents").getJSONObject(4).getString("number");
        String altExtentType5 = simplifiedASpaceRecord.getJSONArray("extents").getJSONObject(5).getString("extent_type");
        String altExtentNumber5 = simplifiedASpaceRecord.getJSONArray("extents").getJSONObject(5).getString("number");
        String altExtentType6 = simplifiedASpaceRecord.getJSONArray("extents").getJSONObject(6).getString("extent_type");
        String altExtentNumber6 = simplifiedASpaceRecord.getJSONArray("extents").getJSONObject(6).getString("number");
        // String altExtentType7 = simplifiedASpaceRecord.getJSONArray("extents").getJSONObject(7).getString("extent_type");
        // String altExtentNumber7 = simplifiedASpaceRecord.getJSONArray("extents").getJSONObject(7).getString("number");


        assertTrue("not equal. the len is: " + simplifiedASpaceRecord.getJSONArray("extents").length(), simplifiedASpaceRecord.getJSONArray("extents").length() == 7);
        
        //1 linear_feet
        assertTrue("test failed, the value of to string is: " + mainExtentType,mainExtentType == "linear_feet" );
        assertTrue("test failed, the value of to string is: " + mainExtentNumber,mainExtentNumber == "1" );

        //1 megabyte
        assertTrue("test failed, the value of to string is: " + altExtentType1,altExtentType1.equalsIgnoreCase("megabytes"));
        assertTrue("test failed, the value of to string is: " + altExtentNumber1,altExtentNumber1.equalsIgnoreCase("1") );

        //15 doors
        assertTrue("test failed, the value of to string is: " + altExtentType2,altExtentType2.equalsIgnoreCase("other_unmapped") );
        assertTrue("test failed, the value of to string is: " + altExtentNumber2,altExtentNumber2.equalsIgnoreCase("15") );
        
        //3 items
        assertTrue("test failed, the value of to string is: " + altExtentType3,altExtentType3.equalsIgnoreCase("item") );
        assertTrue("test failed, the value of to string is: " + altExtentNumber3,altExtentNumber3.equalsIgnoreCase("3") );

        //1 gigabyte
        assertTrue("test failed, the value of to string is: " + altExtentType4,altExtentType4.equalsIgnoreCase("gigabytes") );
        assertTrue("test failed, the value of to string is: " + altExtentNumber4,altExtentNumber4.equalsIgnoreCase("1") );

        //9 microfilm_reels
        assertTrue("test failed, the value of to string is: " + altExtentType5,altExtentType5.equalsIgnoreCase("microfilm_reel") );
        assertTrue("test failed, the value of to string is: " + altExtentNumber5,altExtentNumber5.equalsIgnoreCase("9") );

        //9 microfilm reels
        assertTrue("test failed, the value of to string is: " + altExtentType6,altExtentType6.equalsIgnoreCase("microfilm_reel") );
        assertTrue("test failed, the value of to string is: " + altExtentNumber6,altExtentNumber6.equalsIgnoreCase("9") );
        
        assertTrue("here is the extents: " + simplifiedASpaceRecord.getJSONArray("extents").toString(),false);

    }



    //using this a convenience to test the behavior of the enumUtil
    // @Test
    // public void enumValueTest() {
    //     ASpaceEnumUtil enumUtil = new ASpaceEnumUtil();
    //     String[] extentTypes = enumUtil.getAllASpaceExtentTypes();
    //     // String extentType  = enumUtil.getASpaceExtentType(12);
    //     String extentType = enumUtil.getASpaceExtentType("gigabyte");
    //     assertTrue("the extent type is actually: " + extentType, false);
    // }
}
