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

import org.junit.Test;

/**
 * @author wpalmer
 *
 */
public class JpylyzerSchematronTest {

	/**
	 * 
	 */
	@Test
	public final void testCheckJpylyzerProfile() {
		
		//set up the environment
		String path = "src/test/resources/images/";
		String[] badProfileFiles = { path+"openjpeg_notblprofile/WO1_ANJO_1847_09_15-0004.tif.0.jp2.jpylyzer.xml",
									path+"openjpeg_notblprofile/WO1_ANJO_1847_09_15-0004.tif.0.opj2.jp2.jpylyzer.xml",
									path+"kakadu_notblprofile/WO1_ANJO_1847_09_15-0004.tif.pgm.0.jp2.jpylyzer.xml" };
		String[] blProfileFiles = { //path+"openjpeg_blprofile/openjpeg_coderbypass.jp2.jpylyzer.xml",
									path+"kakadu_blprofile/WO1_BNER_1882_02_22-0012.tif.pgm.kakadu.cb.jp2.jpylyzer.xml"};
		
		//run the tests

		for(String file:blProfileFiles) {
			if(JpylyzerSchematron.checkJpylyzerOutput(file)!=true) {
				fail(file+" does not match bl profile");
			}
		}

		for(String file:badProfileFiles) {
			if(JpylyzerSchematron.checkJpylyzerOutput(file)!=false) {
				fail(file+" incorrectly matches bl profile");
			}
		}

	}

}
