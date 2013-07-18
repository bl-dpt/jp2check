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

/**
 * Represent a JP2 profile
 * The BL jpeg2000 settings loaded as defaults 
 * @author wpalmer
 *
 */
@SuppressWarnings("javadoc")
public class JP2Profile {
	public boolean irreversible = true;
	public String progressionOrder = "RPCL";
	public boolean tiled = false; 
	public int tileDim = 0;
	public int levels = 6;	
	//codestream markers (NOTE: SOP and EPH are not specified in the BL profile)
	public boolean SOP = false;
	public boolean EPH = false;
	//NOTE: we rely on these being in descending order
	//NOTE: openjpeg needs the extra 128 values
	//there should be levels+1 precincts listed here
	public int[] precincts = { 256, 256, 128, 128, 128, 128, 128 };
	public int codeblockSize = 64;
	//NOTE: when enabled with BL profile causes artefacts with openjpeg
	public boolean coderBypass = false;//true; 
	//NOTE: these should be in ascending ratio of compression i.e. 1:1 -> 320:1
	//also, layers = compressionRates.length
	//FIXME: should 1 be 2.16??
	public double[] compressionRates = { 1, 2.4, 2.75, 3.4, 4.6, 7, 11.25,
		20, 40, 80, 160, 320 };
	
	public JP2Profile() {}
}
