/*
 * Copyright 2013 The British Library / The SCAPE Project Consortium
 * Author: William Palmer (William.Palmer@bl.uk)
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package uk.bl.dpt.qa;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.InvalidPropertiesFormatException;

import org.junit.Test;

/**
 * @author wpalmer
 *
 */
public class JP2CheckTest {

	/**
	 * Test method for {@link uk.bl.dpt.qa.JP2Check#jpylyzerSaysValid(java.lang.String)}.
	 */
	@Test
	public final void testJpylyzerSaysValid() {
		if(JP2Check.jpylyzerSaysValid("src/test/resources/jpylyzer/isvalid_true.xml")!=true) {
			fail("isvalid_true.xml does not report true");
		}
		if(JP2Check.jpylyzerSaysValid("src/test/resources/jpylyzer/isvalid_false.xml")!=false) {
			fail("isvalid_false.xml does not report false");
		}
		if(JP2Check.jpylyzerSaysValid("src/test/resources/jpylyzer/isvalid_null.xml")!=false) {
			fail("isvalid_null.xml does not report false");
		}
	}

	/**
	 * Test method for {@link uk.bl.dpt.qa.JP2Check#checkJpylyzerProfile(java.lang.String, uk.bl.dpt.qa.JP2Profile)}.
	 */
	@Test
	public final void testCheckJpylyzerProfile() {
		
		//set up the environment
		
		JP2Profile blProfile = new JP2Profile();
		JP2Profile badProfile = new JP2Profile();
		JP2Profile[] badBLProfiles = null;
														
		try {
			blProfile = JP2Check.loadProfile("src/test/resources/profiles/bl_profile.xml");
			badProfile = JP2Check.loadProfile("src/test/resources/profiles/bad_profile_1.xml");
			badBLProfiles = new JP2Profile[] { 
					JP2Check.loadProfile("src/test/resources/profiles/bl_profile_change_codeblocksize.xml"),
					JP2Check.loadProfile("src/test/resources/profiles/bl_profile_change_coderbypass.xml"),
					JP2Check.loadProfile("src/test/resources/profiles/bl_profile_change_compressionrates.xml"),
					JP2Check.loadProfile("src/test/resources/profiles/bl_profile_change_eph.xml"),
					JP2Check.loadProfile("src/test/resources/profiles/bl_profile_change_levels.xml"),
					JP2Check.loadProfile("src/test/resources/profiles/bl_profile_change_order.xml"),
					JP2Check.loadProfile("src/test/resources/profiles/bl_profile_change_precincts.xml"),
					JP2Check.loadProfile("src/test/resources/profiles/bl_profile_change_sop.xml"),
					JP2Check.loadProfile("src/test/resources/profiles/bl_profile_change_transformation.xml") };

		} catch (InvalidPropertiesFormatException e) {
			fail("xml error");
		} catch (IOException e) {
			fail("IO error");
		}
		
		String path = "src/test/resources/images/";
		String[] badProfileFiles = { path+"openjpeg_notblprofile/WO1_ANJO_1847_09_15-0004.tif.0.jp2.jpylyzer.xml",
									path+"openjpeg_notblprofile/WO1_ANJO_1847_09_15-0004.tif.0.opj2.jp2.jpylyzer.xml",
									path+"kakadu_notblprofile/WO1_ANJO_1847_09_15-0004.tif.pgm.0.jp2.jpylyzer.xml" };
		String[] blProfileFiles = { //path+"openjpeg_blprofile/openjpeg_coderbypass.jp2.jpylyzer.xml",
									path+"kakadu_blprofile/WO1_BNER_1882_02_22-0012.tif.pgm.kakadu.cb.jp2.jpylyzer.xml"};
		
		//run the tests
		
		for(String file:blProfileFiles) {
			if(JP2Check.checkJpylyzerProfile(file, blProfile)!=true) {
				fail(file+" does not match bl profile");
			}
			if(JP2Check.checkJpylyzerProfile(file, badProfile)!=false) {
				fail(file+" incorrectly matches bad profile");
			}
		}
		
		for(String file:badProfileFiles) {
			if(JP2Check.checkJpylyzerProfile(file, blProfile)!=false) {
				fail(file+" incorrectly matches bl profile");
			}
		}
		
		for(JP2Profile prof:badBLProfiles) {
			for(String file:blProfileFiles) {
				if(JP2Check.checkJpylyzerProfile(file, prof)!=false) {
					fail(file+" incorrectly matches bl profile");
				}
			}
		}
		
	}

}
