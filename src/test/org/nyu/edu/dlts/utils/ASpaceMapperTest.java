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
import org.nyu.edu.dlts.utils.ASpaceMapper;;
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
        String[] extentsPass = {"1.84 megabytes","6 microfilm reels","12 items","1 oversize folder","a big folder", "three cassettes"};
        String[] extentsFail = { "", "including photostats of 1934 book", "transcript and microfilm copy"};

        JSONObject parsedExtentTest0 = aSpaceMapper.parseExtentStatement(extentsPass[0]);
        assertEquals("the units are not equal", "megabytes", parsedExtentTest0.get("unit"));
        assertEquals("the values are not equal", "1.84", parsedExtentTest0.get("value"));
        assertEquals("the errors are not equal", false, parsedExtentTest0.get("error"));

        JSONObject parsedExtentTest1 = aSpaceMapper.parseExtentStatement(extentsPass[1]);
        assertEquals("the units are not equal", "microfilm reels", parsedExtentTest1.get("unit"));
        assertEquals("the values are not equal", "6", parsedExtentTest1.get("value"));
        assertEquals("the errors are not equal", false, parsedExtentTest1.get("error"));


        JSONObject parsedExtentTest2 = aSpaceMapper.parseExtentStatement(extentsPass[2]);
        assertEquals("the units are not equal", "items", parsedExtentTest2.get("unit"));
        assertEquals("the values are not equal", "12", parsedExtentTest2.get("value"));
        assertEquals("the errors are not equal", false, parsedExtentTest2.get("error"));


        JSONObject parsedExtentTest3 = aSpaceMapper.parseExtentStatement(extentsPass[3]);
        assertEquals("the units are not equal", "oversize folder", parsedExtentTest3.get("unit"));
        assertEquals("the values are not equal", "1", parsedExtentTest3.get("value"));
        assertEquals("the errors are not equal", false, parsedExtentTest3.get("error"));

        JSONObject parsedExtentTest4 = aSpaceMapper.parseExtentStatement(extentsPass[4]);
        assertEquals("the units are not equal", "big folder", parsedExtentTest4.get("unit"));
        assertEquals("the values are not equal", "1", parsedExtentTest4.get("value"));
        assertEquals("the errors are not equal", false, parsedExtentTest4.get("error"));

        JSONObject parsedExtentTest5 = aSpaceMapper.parseExtentStatement(extentsPass[5]);
        assertEquals("the units are not equal", "cassettes", parsedExtentTest5.get("unit"));
        assertEquals("the values are not equal", "3", parsedExtentTest5.get("value"));
        assertEquals("the errors are not equal", false, parsedExtentTest5.get("error"));

        JSONObject parsedExtentTest6 = aSpaceMapper.parseExtentStatement(extentsFail[0]);
        assertEquals("the units are not equal", "", parsedExtentTest6.get("unit"));
        assertEquals("the values are not equal", "", parsedExtentTest6.get("value"));
        assertEquals("the errors are not equal", true, parsedExtentTest6.get("error"));
        assertEquals("the confound values are not equal", "", parsedExtentTest6.get("confound"));

        JSONObject parsedExtentTest7 = aSpaceMapper.parseExtentStatement(extentsFail[1]);
        assertEquals("the units are not equal", "", parsedExtentTest7.get("unit"));
        assertEquals("the values are not equal", "", parsedExtentTest7.get("value"));
        assertEquals("the errors are not equal", true, parsedExtentTest7.get("error"));
        assertEquals("the confound values are not equal", "including photostats of 1934 book", parsedExtentTest7.get("confound"));

        JSONObject parsedExtentTest8 = aSpaceMapper.parseExtentStatement(extentsFail[2]);
        assertEquals("the units are not equal", "", parsedExtentTest8.get("unit"));
        assertEquals("the values are not equal", "", parsedExtentTest8.get("value"));
        assertEquals("the errors are not equal", true, parsedExtentTest8.get("error"));
        assertEquals("the confound values are not equal", "transcript and microfilm copy", parsedExtentTest8.get("confound"));

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
    public void processAlternativeExtent() {
        ASpaceMapper aSpaceMapper = new ASpaceMapper();
        String[] inputExtents = {
            "1.84 megabytes",
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
            "113 letters"
        };

        JSONObject extenJsonObject0 = aSpaceMapper.processAlternativeExtent(inputExtents[0]);
        assertEquals("The count is not the same","1", extenJsonObject0.get("count"));

        JSONObject extenJsonObject1 = aSpaceMapper.processAlternativeExtent(inputExtents[1]);
        assertEquals("The count is not the same","1", extenJsonObject1.get("count"));
        
        JSONObject extenJsonObject2 = aSpaceMapper.processAlternativeExtent(inputExtents[2]);
        assertEquals("The count is not the same","1", extenJsonObject2.get("count"));

        JSONObject extenJsonObject3 = aSpaceMapper.processAlternativeExtent(inputExtents[3]);
        assertEquals("The count is not the same","1", extenJsonObject3.get("count"));

        JSONObject extenJsonObject4 = aSpaceMapper.processAlternativeExtent(inputExtents[4]);
        assertEquals("The count is not the same","2", extenJsonObject4.get("count"));

        JSONObject extenJsonObject5 = aSpaceMapper.processAlternativeExtent(inputExtents[5]);
        assertEquals("The count is not the same","2", extenJsonObject5.get("count"));

        JSONObject extenJsonObject6 = aSpaceMapper.processAlternativeExtent(inputExtents[6]);
        assertEquals("The count is not the same","2", extenJsonObject6.get("count"));

        JSONObject extenJsonObject7 = aSpaceMapper.processAlternativeExtent(inputExtents[7]);
        assertEquals("The count is not the same","3", extenJsonObject7.get("count"));

        JSONObject extenJsonObject8 = aSpaceMapper.processAlternativeExtent(inputExtents[8]);
        assertEquals("The count is not the same","1", extenJsonObject8.get("count"));

        JSONObject extenJsonObject9 = aSpaceMapper.processAlternativeExtent(inputExtents[9]);
        assertEquals("The count is not the same","2", extenJsonObject9.get("count"));

        JSONObject extenJsonObject10 = aSpaceMapper.processAlternativeExtent(inputExtents[10]);
        assertEquals("The count is not the same","1", extenJsonObject10.get("count"));

        JSONObject extenJsonObject11 = aSpaceMapper.processAlternativeExtent(inputExtents[11]);
        assertEquals("The count is not the same","1", extenJsonObject11.get("count"));

        JSONObject extenJsonObject12 = aSpaceMapper.processAlternativeExtent(inputExtents[12]);
        assertEquals("The count is not the same","1", extenJsonObject12.get("count"));

        JSONObject extenJsonObject13 = aSpaceMapper.processAlternativeExtent(inputExtents[13]);
        assertEquals("The count is not the same","1", extenJsonObject13.get("count"));
        //TODO: refactor with JSONAssert

        assertEquals()




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
