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

import java.util.LinkedList;
import java.util.List;

/**
 * Generates command lines for various JP2 encoders from a single set of parameters
 * @author wpalmer
 */
public class JP2CommandLine {
	
	/**
	 * Generates a Kakadu command line from the currently loaded profile.
	 * NOTE: it has not been tested using Kakadu.
	 * @param profile profile to use to generate command
	 * @return A List of Strings containing the command line
	 */
	//this is untested - for debugging purposes only, so far
	@SuppressWarnings("unused")
	public static List<String> getKakaduCommand(JP2Profile profile) {
		
		LinkedList<String> command = new LinkedList<String>();
		
		//transform
		if(profile.irreversible) command.add("Creversible=no");
			else command.add("Creversible=yes");
		
		//progression order
		command.add("Corder="+profile.progressionOrder);
		
		//TODO components?
		
		//tile size
		if(profile.tiled) {
			//TODO: add code here
		} else {
			//do nothing - openjpeg uses a tile for the whole image by default
		}
	
		//levels
		command.add("Clevels="+profile.levels);
		
		//this will not execute so codestream markers are not added
		if(false) {
		//codestream markers
		if(profile.EPH) {
			command.add("Cuse_eph=yes");
		}
		if(profile.SOP) {
			command.add("Cuse_sop=yes");
		}
		}
		
		//FIXME: unsure what the equivalent for this is with OpenJPEG etc
		command.add("ORGgen_plt=yes");
			
		//precincts
		String prec = "Cprecincts=";
		for(int precinct : profile.precincts) {
			prec+="{"+precinct+","+precinct+"},";
		}
		//get rid of the last comma
		command.add(prec.substring(0, prec.length()-1));
		
		//codeblock size
		command.add("Cblk={"+profile.codeblockSize+","+profile.codeblockSize+"}");
		
		//coder bypass
		if(profile.coderBypass) {
			command.add("Cmodes=BYPASS");
		}
		
		//rates
		command.add("-rate");
		String rates = "";
		for(double rate : profile.compressionRates) {
			if(1==rate) {
				rates += "-,";
			} else {
				//note in the BL format document rates are passed to Kakadu in relation
				//to 24bpp
				//check correctness of this for other bit depths
				rates += String.format("%.3f", 24/rate) + ",";
			}
		}
		//get rid of the last comma
		command.add(rates.substring(0, rates.length()-1));
				
		return command;
	}
	
	/**
	 * Generates an OpenJPEG command line from the currently loaded profile.
 	 * @param profile profile to use to generate command
	 * @return A List of Strings containing the command line
	 */
	public static List<String> getOpenJpegCommand(JP2Profile profile) {
		
		LinkedList<String> command = new LinkedList<String>();
		
		//transform
		if(profile.irreversible) command.add("-I");
		
		//progression order
		command.add("-p");
		command.add(profile.progressionOrder);
		
		//TODO components
		
		//tile size
		if(profile.tiled) {
			//TODO: add code here
		} else {
			//do nothing - openjpeg uses a tile for the whole image by default
		}
	
		//levels
		//OpenJPEG says it should be passed #levels+1 for #levels to equal required levels 
		command.add("-n");
		command.add(new Integer(profile.levels+1).toString());
		
		//codestream markers
		if(profile.EPH) command.add("-EPH");
		if(profile.SOP) command.add("-SOP");
		
		//precincts
		command.add("-c");
		String prec = "";
		for(int precinct : profile.precincts) {
			prec+="["+precinct+","+precinct+"],";
		}
		//get rid of the last comma
		command.add(prec.substring(0, prec.length()-1));
		
		//codeblock size
		command.add("-b");
		command.add(profile.codeblockSize+","+profile.codeblockSize);

		//FIXME! this setting causes problems with the encode and produces files with
		//bad compression artefacts
		//if(false) {
		//coder bypass
		if(profile.coderBypass) {
			command.add("-M");
			command.add("1");
			
			//NOTE: jj2k help suggests that coder bypass needs a termination algorithm (term_type)
			//From using OPJViewer it seems that Kakadu just sets coder bypass and nothing else so
			//no term_type should be required.  Therefore that is an OpenJPEG bug, still present in 
			//version 2.0.0
		}
		//}
		
		//rates
		command.add("-r");
		String rates = "";
		for(double rate : profile.compressionRates) {
			//note this list is backwards for openjpeg
			rates = String.format("%.3f", rate) + "," + rates;
		}
		//get rid of the last comma
		command.add(rates.substring(0, rates.length()-1));
		
		return command;
	}
	
	/**
	 * Generates a Jasper command line from the currently loaded profile.
	 * @param profile profile to use to generate command
	 * @return A List of Strings containing the command line
	 */
	public static List<String> getJasperCommand(JP2Profile profile) {
		
		LinkedList<String> command = new LinkedList<String>();
		
		command.add("--output-format");
		command.add("jp2");
		
		//transform
		if(profile.irreversible) {
			command.add("--output-option");
			command.add("mode=real");
		}
		
		//progression order
		command.add("--output-option");
		command.add("prg="+profile.progressionOrder.toLowerCase());
		
		//TODO components
		
		//tile size
		if(profile.tiled) {
			//TODO: add code here
		} else {
			//do nothing - openjpeg uses a tile for the whole image by default
		}
	
		//levels
		command.add("--output-option");
		command.add("numrlvls="+Integer.toString(profile.levels));
		
		//codestream markers
		if(profile.EPH) {
			command.add("--output-option");
			command.add("eph");
		}
		if(profile.SOP) {
			command.add("--output-option");
			command.add("sop");
		}
		
		//precincts
		//NOTE: can only set one precinct size for JasPer
		command.add("--output-option");
		command.add("prcwidth="+profile.precincts[0]);
		command.add("--output-option");
		command.add("prcheight="+profile.precincts[0]);
	
		//codeblock size
		command.add("--output-option");
		command.add("cblkwidth="+profile.codeblockSize);
		command.add("--output-option");
		command.add("cblkheight="+profile.codeblockSize);
		
		//coder bypass
		if(profile.coderBypass) {
			command.add("--output-option");
			command.add("lazy");
		}
		
		//must set an overall rate <1.0 to lossy encode =1/x (x:1)
		//this rate must be greater than the biggest rate in layer rates
		command.add("--output-option");
		command.add("rate="+(1/profile.compressionRates[0]));
		
		//rates
		command.add("--output-option");
		String rates = "";
		for(int i=1;i<profile.compressionRates.length;i++) {
			//note this list is backwards for openjpeg
			rates = String.format("%.3f", 1/profile.compressionRates[i]) + "," + rates;
		}
		//get rid of the last comma
		command.add("ilyrrates="+rates.substring(0, rates.length()-1));
		
		return command;
	}
	
	/**
	 * Generates a JJ2000 command line from the currently loaded profile.
	 * @param profile profile to use to generate command
	 * @return A List of Strings containing the command line
	 */
	public static List<String> getJJ2000Command(JP2Profile profile) {
		
		LinkedList<String> command = new LinkedList<String>();
		
		command.add("-file_format");
		command.add("on");
		
		command.add("-lossless");
		//transform
		if(profile.irreversible) {
			command.add("off");
		} else {
			command.add("on");
		}
		
		//progression order - should add the rest here (found with trial & error & jpylyzer)
		command.add("-Aptype");
		if (profile.progressionOrder.toLowerCase().equals("rlcp"))
			command.add("res");
		if (profile.progressionOrder.toLowerCase().equals("rpcl"))
			command.add("res-pos");
		if (profile.progressionOrder.toLowerCase().equals("lrcp"))
			command.add("layer");
		
		//TODO components
		
		//tile size
		if(profile.tiled) {
			//TODO: add code here
		} else {
			//do nothing - openjpeg uses a tile for the whole image by default
		}
	
		//levels
		command.add("-Wlev");
		command.add(Integer.toString(profile.levels));
		
		//codestream markers
		command.add("-Peph");
		if(profile.EPH) {
			command.add("on");
		} else {
			command.add("off");
		}
		command.add("-Psop");
		if(profile.SOP) {
			command.add("on");
		} else {
			command.add("off");
		}
		
		//precincts
		command.add("-Cpp");
		String prec = "";
		for(int precinct : profile.precincts) {
			prec+=precinct + " " + precinct + " ";
		}
		//get rid of the last space
		command.add(prec.substring(0, prec.length()-1));
		
		//codeblock size
		command.add("-Cblksiz");
		command.add(Integer.toString(profile.codeblockSize));
		command.add(Integer.toString(profile.codeblockSize));

		//coder bypass
		command.add("-Cbypass");
		if(profile.coderBypass) {
			command.add("on");
		} else {
			command.add("off");
		}
		
		//rates/layers
		command.add("-Alayers");
		String rates = "";
		for(double rate : profile.compressionRates) {
			//note this list is backwards for openjpeg
			rates = String.format("%.3f", 24/rate) + " " + rates;
		}
		//get rid of the last comma
		command.add(rates);
		
		return command;
	}
	

}
