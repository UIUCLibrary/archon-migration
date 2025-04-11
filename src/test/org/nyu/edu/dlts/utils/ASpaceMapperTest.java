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
    public void parseExtentStatementTest() throws JSONException{
        ASpaceMapper aSpaceMapper = new ASpaceMapper();
        String[] extents = {"1.84 megabytes","6 microfilm reels","12 items","1 oversize folder","a big folder"};

        JSONObject parsedExtentTest0 = aSpaceMapper.parseExtentStatement(extents[0]);
        assertEquals("the units are not equal", "megabytes", parsedExtentTest0.get("unit"));
        assertEquals("the values are not equal", "1.84", parsedExtentTest0.get("value"));
        assertEquals("the errors are not equal", false, parsedExtentTest0.get("error"));

        JSONObject parsedExtentTest1 = aSpaceMapper.parseExtentStatement(extents[1]);
        assertEquals("the units are not equal", "microfilm reels", parsedExtentTest1.get("unit"));
        assertEquals("the values are not equal", "6", parsedExtentTest1.get("value"));
        assertEquals("the errors are not equal", false, parsedExtentTest1.get("error"));


        JSONObject parsedExtentTest2 = aSpaceMapper.parseExtentStatement(extents[2]);
        assertEquals("the units are not equal", "items", parsedExtentTest2.get("unit"));
        assertEquals("the values are not equal", "12", parsedExtentTest2.get("value"));
        assertEquals("the errors are not equal", false, parsedExtentTest2.get("error"));


        JSONObject parsedExtentTest3 = aSpaceMapper.parseExtentStatement(extents[3]);
        assertEquals("the units are not equal", "oversize folder", parsedExtentTest3.get("unit"));
        assertEquals("the values are not equal", "1", parsedExtentTest3.get("value"));
        assertEquals("the errors are not equal", false, parsedExtentTest3.get("error"));

        JSONObject parsedExtentTest4 = aSpaceMapper.parseExtentStatement(extents[4]);
        assertEquals("the units are not equal", "", parsedExtentTest4.get("unit"));
        assertEquals("the values are not equal", "", parsedExtentTest4.get("value"));
        assertEquals("the errors are not equal", true, parsedExtentTest4.get("error"));

    }

    @Test
    public void enumValueTest() {
        ASpaceEnumUtil enumUtil = new ASpaceEnumUtil();
        String[] extentTypes = enumUtil.getAllASpaceExtentTypes();
        // String extentType  = enumUtil.getASpaceExtentType(12);
        String extentType = enumUtil.getASpaceExtentType("gigabyte");
        assertTrue("the extent type is actually: " + extentType, "." == "");
    }
}
