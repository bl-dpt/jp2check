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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.InvalidPropertiesFormatException;
import java.util.LinkedList;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/**
 * This class implements a manual check of Jpylyzer outputs
 * @author wpalmer
 */
public class JP2Check {

	/**
	 * Path to the Jpylyzer binary
	 * Win32 binary can be downloaded from http://www.openplanetsfoundation.org/software/jpylyzer
	 */
	//private static final String gJpylyzer = "/home/will/local/bin/jpylyzer";
	private static String gJpylyzer = "c:/bin/jpylyzer/jpylyzer.exe";
	private static final String JPYLYZER_EXT = ".jpylyzer.xml";

	/**
	 * Sets the location of the Jpylyzer binary
	 * @param pBinary the location of the binary file
	 */
	public static void setJpylyzerBinary(String pBinary) {
		if(new File(pBinary).exists()) { 
			gJpylyzer = pBinary; 
		}
	}
	
	/**
	 * Class containing the keys for the jpylyzer XML
	 */
	 @SuppressWarnings("javadoc")
	 public static final class Keys {
		 //these are element names used in the jpylyzer outputs
		 public final static String	ISVALID = "isValid";
		 public final static String	ORDER = "order";
		 public final static String	PRECINCTS = "precincts";
		 public final static String	SOP = "sop";
		 public final static String	EPH = "eph";
		 public final static String	LAYERS = "layers";
		 public final static String	LEVELS = "levels";
		 public final static String	CODEBLOCKWIDTH = "codeBlockWidth";
		 public final static String	CODEBLOCKHEIGHT = "codeBlockHeight";
		 public final static String	CODINGBYPASS = "codingBypass";
		 public final static String	TRANSFORMATION = "transformation";
		 public final static String	PRECINCTSIZE = "precinctSize";
		 public final static String	NUMBEROFTILES = "numberOfTiles";
		 public final static String TILEXDIM = "xTsiz";
		 public final static String TILEYDIM = "yTsiz";
		 //these are used for the xml loading/saving of profiles
		 public final static String TILED = "tiled";
		 public final static String TILEDIM = "tileDim";
		 public final static String CODEBLOCKSIZE = "codeBlockSize";
		 public final static String COMPRESSIONRATES = "compressionRates";

		 private Keys() {}
	 }
	
	/**
	 * Load a j2k profile from an XML properties file
	 * @param pProfileFile profile file to load
	 * @return JP2Profile loaded profile
	 * @throws IOException on error
	 * @throws InvalidPropertiesFormatException on error 
	 */
	public static JP2Profile loadProfile(String pProfileFile) throws InvalidPropertiesFormatException, IOException {
		
		JP2Profile jp2Profile = new JP2Profile();
		Properties profile = new Properties();
		profile.loadFromXML(new FileInputStream(pProfileFile));
		
		jp2Profile.progressionOrder = profile.getProperty(Keys.ORDER);
		if(profile.getProperty(Keys.SOP).equals("yes")) {
			jp2Profile.SOP=true; 
		} else {
			jp2Profile.SOP=false;
		}
		if(profile.getProperty(Keys.EPH).equals("yes")) { 
			jp2Profile.EPH=true;
		} else{ 
			jp2Profile.EPH=false;
		}
		jp2Profile.levels = new Integer(profile.getProperty(Keys.LEVELS));
		jp2Profile.codeblockSize = new Integer(profile.getProperty(Keys.CODEBLOCKSIZE));
		if(profile.getProperty(Keys.CODINGBYPASS).equals("yes")) {
			jp2Profile.coderBypass=true;
		} else{ 
			jp2Profile.coderBypass=false;
		}
		if(profile.getProperty(Keys.TRANSFORMATION).equals("irreversible")) {
			jp2Profile.irreversible=true;
		} else {
			jp2Profile.irreversible=false;
		} 
		jp2Profile.tiled = new Boolean(profile.getProperty(Keys.TILED).equals("yes"));
		if(jp2Profile.tiled) {
			jp2Profile.tileDim = new Integer(profile.getProperty(Keys.TILEDIM));
		}

		String[] compRates = profile.getProperty(Keys.COMPRESSIONRATES).split(",");
		jp2Profile.compressionRates = new double[compRates.length];
		for(int i=0;i<compRates.length;i++) {
			jp2Profile.compressionRates[i] = new Double(compRates[i]);
		}

		String[] prec = profile.getProperty(Keys.PRECINCTS).split(",");
		if(prec[0].toLowerCase().equals("no")) {
			//i.e. no precincts
			jp2Profile.precincts = new int[0];
		} else {
			jp2Profile.precincts = new int[prec.length];
			for(int i=0;i<prec.length;i++) {
				jp2Profile.precincts[i] = new Integer(prec[i]);
			}
		}
	
		return jp2Profile;
	}
	
	/**
	 * Loads jpylyzer XML output from a file
	 * @return Pairs of relevant key/values as read  
	 */
	private static HashMap<String, String> loadJpylyzerXML(String pFileName) {
		//parse the values returned
		DocumentBuilder docB = null;
		Document doc = null;
		
		try {
			docB = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		} catch(ParserConfigurationException pce) {
			return new HashMap<String, String>();
		}
		try {
			doc = docB.parse(pFileName);
		} catch(IOException ioe) {
			ioe.printStackTrace();
		} catch(SAXException se) {
			se.printStackTrace();
		}
		
		Node root = doc.getFirstChild();
		HashMap<String, String> items = new HashMap<String, String>();
		
		XPath xpath = XPathFactory.newInstance().newXPath();
		String pathCCB = "/jpylyzer/properties/contiguousCodestreamBox/";
		String path = pathCCB+"cod/";

		try {

			items.put(Keys.ISVALID, xpath.evaluate("/jpylyzer/isValidJP2", root));
			items.put(Keys.ORDER, xpath.evaluate(path+Keys.ORDER, root));
			items.put(Keys.PRECINCTS, xpath.evaluate(path+Keys.PRECINCTS, root));
			items.put(Keys.SOP, xpath.evaluate(path+Keys.SOP, root));
			items.put(Keys.EPH, xpath.evaluate(path+Keys.EPH, root));
			items.put(Keys.LAYERS, xpath.evaluate(path+Keys.LAYERS, root));
			items.put(Keys.LEVELS, xpath.evaluate(path+Keys.LEVELS, root));
			items.put(Keys.CODEBLOCKWIDTH, xpath.evaluate(path+Keys.CODEBLOCKWIDTH, root));
			items.put(Keys.CODEBLOCKHEIGHT, xpath.evaluate(path+Keys.CODEBLOCKHEIGHT, root));
			items.put(Keys.CODINGBYPASS, xpath.evaluate(path+Keys.CODINGBYPASS, root));
			items.put(Keys.TRANSFORMATION, xpath.evaluate(path+Keys.TRANSFORMATION, root));

			if(items.get(Keys.PRECINCTS).toLowerCase().equals("yes")) {
				int x = 0;
				int y = 0;
				int count = new Integer(xpath.evaluate("count("+path+Keys.PRECINCTSIZE+"X)", root));
				items.put(Keys.PRECINCTSIZE, new Integer(count).toString());
				for(int i=0;i<count;i++) {
					//note xpath array references are 1-based, not 0-based
					x = new Integer(xpath.evaluate(path+Keys.PRECINCTSIZE+"X["+(i+1)+"]", root));
					y = new Integer(xpath.evaluate(path+Keys.PRECINCTSIZE+"Y["+(i+1)+"]", root));
					//HACK: this is a slightly funny way of saving this data, but it works
					if (x==y) items.put(Keys.PRECINCTSIZE+i,new Integer(x).toString()); 
					else System.out.println("ERROR in precinctSize parsing");
				}
			}

			path = pathCCB+"siz/";
			items.put(Keys.NUMBEROFTILES, xpath.evaluate(path+Keys.NUMBEROFTILES, root));
			if(new Integer(items.get(Keys.NUMBEROFTILES))>1) {
				items.put(Keys.TILEXDIM, xpath.evaluate(path+Keys.TILEXDIM, root));
				items.put(Keys.TILEYDIM, xpath.evaluate(path+Keys.TILEYDIM, root));
			}

		} catch(XPathExpressionException xpee) {
			System.out.println("Only loaded the following:");
			//print all the loaded data
			for(String s:items.keySet())System.out.println(s+": "+items.get(s));
			xpee.printStackTrace();
		}

		return items;

	}
	
	/**
	 * Checks whether jpylyzer says the input file was valid
	 * @param pFileName file containing jpylyzer xml
	 * @return true if it is valid, false if not  
	 */
	public static boolean jpylyzerSaysValid(String pFileName) {
		
		HashMap<String, String> items = loadJpylyzerXML(pFileName);
		
		String key = Keys.ISVALID;
		if(items.get(key).toLowerCase().equals("true")) return true; 
		
		return false;
	}
	
	/**
	 * Checks whether the currently loaded profile matches the given profile 
	 * @param pFileName file containing jpylyzer xml
	 * @param pJp2Profile profile to check against
	 * @return true if it is, false if not  
	 */
	public static boolean checkJpylyzerProfile(String pFileName, JP2Profile pJp2Profile) {
		
		HashMap<String, String> items = loadJpylyzerXML(pFileName);
		//somewhere to store what doesn't match
		HashMap<String, String> mismatchItems = new HashMap<String, String>();
		//assume we have a match unless we get a false
		boolean matchesSettings = true;
		
		if(items.get(Keys.ISVALID).toLowerCase().equals("true")) {
		} else {
		}
		items.remove(Keys.ISVALID);

		//progression order
		if(!items.get(Keys.ORDER).toLowerCase().equals(pJp2Profile.progressionOrder.toLowerCase())) {
			matchesSettings = false;
			mismatchItems.put(Keys.ORDER, items.get(Keys.ORDER));
		}
		items.remove(Keys.ORDER);
		
		//number of levels
		/*
		 * A file encoded with OpenJPEG will cause Jpylyzer to report (n-1) levels, given n on the command line
		 * so files should be encoded using "-n (n+1)".
		 * A file encoded with Kakadu will cause Jpylyzer to report n levels, given n on the command line
		 */
		if(!new Integer(items.get(Keys.LEVELS)).equals(pJp2Profile.levels)) {
			matchesSettings = false;
			mismatchItems.put(Keys.LEVELS, items.get(Keys.LEVELS));
		}
		items.remove(Keys.LEVELS);

		//sop
		if(!(pJp2Profile.SOP==items.get(Keys.SOP).toLowerCase().equals("yes"))) {
			matchesSettings = false;
			mismatchItems.put(Keys.SOP, items.get(Keys.SOP));
		}
		items.remove(Keys.SOP);
		
		//eph
		if(!(pJp2Profile.EPH==items.get(Keys.EPH).toLowerCase().equals("yes"))) {
			matchesSettings = false;
			mismatchItems.put(Keys.EPH, items.get(Keys.EPH));
		}
		items.remove(Keys.EPH);
		
		//precinct sizes
		if(items.get(Keys.PRECINCTS).toLowerCase().equals("yes")) {
			LinkedList<Integer> precinctVals = new LinkedList<Integer>();
			//add all the precinct values to a new list
			for(int i=new Integer(items.get(Keys.PRECINCTSIZE));i>0;i--) {
				precinctVals.add(new Integer(items.get(Keys.PRECINCTSIZE+""+(i-1))));			
				items.remove(Keys.PRECINCTSIZE+(i-1));		
			}
			//for each of the specified precinct values check if it is in the 
			//jpylyzer output, if so ok, if not then fail comparison
			for(int i=0;i<pJp2Profile.precincts.length;i++) {
				if(precinctVals.contains(pJp2Profile.precincts[i])) {
					precinctVals.removeFirstOccurrence(pJp2Profile.precincts[i]);
				} else {
					//this precinct value is not in the precincts in the jpylyzer file
					matchesSettings = false;
					mismatchItems.put(Keys.PRECINCTSIZE,new Integer(pJp2Profile.precincts[i]).toString());
				}
			}
			//we end up with additional precinct values here - report it
			for(int i : precinctVals) {
				System.out.println("WARNING: precinctSize("+i+","+i+") in jpylyzer output but not specified in header");
			}
			items.remove(Keys.PRECINCTSIZE);
		}
		
		//precincts
		if(!(items.get(Keys.PRECINCTS).toLowerCase().equals("yes")==(pJp2Profile.precincts.length>0))) {
			matchesSettings = false;
			mismatchItems.put(Keys.PRECINCTS, items.get(Keys.PRECINCTS));
		}
		items.remove(Keys.PRECINCTS);

		//layers
		if(!new Integer(items.get(Keys.LAYERS)).equals(pJp2Profile.compressionRates.length)) {
			matchesSettings = false;
			mismatchItems.put(Keys.LAYERS, items.get(Keys.LAYERS));
		}
		items.remove(Keys.LAYERS);
				
		//codeblockwidth
		if(!new Integer(items.get(Keys.CODEBLOCKWIDTH)).equals(pJp2Profile.codeblockSize)) {
			matchesSettings = false;
			mismatchItems.put(Keys.CODEBLOCKWIDTH, items.get(Keys.CODEBLOCKWIDTH));
		}
		items.remove(Keys.CODEBLOCKWIDTH);
		
		//codeblockheight
		if(!new Integer(items.get(Keys.CODEBLOCKHEIGHT)).equals(pJp2Profile.codeblockSize)) {
			matchesSettings = false;
			mismatchItems.put(Keys.CODEBLOCKHEIGHT, items.get(Keys.CODEBLOCKHEIGHT));
		}
		items.remove(Keys.CODEBLOCKHEIGHT);
		
		//number of tiles
		if(new Integer(items.get(Keys.NUMBEROFTILES))>1) {
			if(!new Integer(items.get(Keys.TILEXDIM)).equals(pJp2Profile.tileDim)) {
				matchesSettings = false;
				mismatchItems.put(Keys.TILEXDIM, items.get(Keys.TILEXDIM));
			}
			items.remove(Keys.TILEXDIM);

			if(!new Integer(items.get(Keys.TILEYDIM)).equals(pJp2Profile.tileDim)) {
				matchesSettings = false;
				mismatchItems.put(Keys.TILEYDIM, items.get(Keys.TILEYDIM));
			}
			items.remove(Keys.TILEYDIM);
		}
		
		if(!(new Integer(items.get(Keys.NUMBEROFTILES))>1==pJp2Profile.tiled)) {
			matchesSettings = false;
			mismatchItems.put(Keys.NUMBEROFTILES, items.get(Keys.NUMBEROFTILES));
		}
		items.remove(Keys.NUMBEROFTILES);

		//codingbypass
		if(!(items.get(Keys.CODINGBYPASS).toLowerCase().equals("yes")==pJp2Profile.coderBypass)) {
			matchesSettings = false;
			mismatchItems.put(Keys.CODINGBYPASS, items.get(Keys.CODINGBYPASS));
		}
		items.remove(Keys.CODINGBYPASS);
		
		//transformation
		if(!(items.get(Keys.TRANSFORMATION).toLowerCase().equals("9-7 irreversible")==pJp2Profile.irreversible)) {
			matchesSettings = false;
			mismatchItems.put(Keys.TRANSFORMATION, items.get(Keys.TRANSFORMATION));
		}
		items.remove(Keys.TRANSFORMATION);
		
		//if there are any unchecked items, print them to the console
		if(items.size()>0) {
			System.out.println("WARNING: unchecked items: ");
			for(String k : items.keySet()) {
				System.out.println(k+": "+items.get(k));
			}
		}

		if(matchesSettings) {
			//System.out.println("matches settings: true");
		} else {
			System.out.println("Settings in jpylyzer xml that don't match loaded j2k profile:");
			for(String k : mismatchItems.keySet()) System.out.println(k+": "+mismatchItems.get(k));
		}
		
		return matchesSettings;
	}
	
	/**
	 * Executes Jpylyzer, stores output in jp2File+".jpylyzer.xml"
	 * @param commandLine command line to run
	 * @return exit code from execution of the command line
	 * @throws IOException
	 */
	private static int runJpylyzer(String pJp2File) throws IOException {
		String commandLine = gJpylyzer + " " + pJp2File;
		ProcessBuilder pb = new ProcessBuilder(commandLine.split(" "));
		//don't redirect stderr to stdout as our output XML is in stdout
		pb.redirectErrorStream(false);		
		//start the executable
		Process proc = pb.start();
		//create a log of the console output, this is the XML
		BufferedReader stdout = new BufferedReader(new InputStreamReader(proc.getInputStream()));
		BufferedWriter outputFile = new BufferedWriter(new FileWriter(pJp2File+JPYLYZER_EXT));

		try {
			String line;
			while((line=stdout.readLine())!=null) {
				outputFile.write(line);
			}
			outputFile.close();			
			proc.waitFor();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		stdout.close();

		return proc.exitValue();
	}
	
	/**
	 * Test main method, use jpylyzer and check all files on the command line
	 * @param args list of JPEG2000 files
	 */
	public static void main(String[] args) {

		System.out.println("JP2Check: pass a list of jp2 files on the command line and they will be checked against the built-in profile");
		System.out.println("Warning: jpylyzer xml output will be (over)written to file.jp2.jpylyzer.xml");
		
		JP2Profile jp2Profile = new JP2Profile();
		
		for(String arg:args) {
			if(arg.toLowerCase().endsWith(".jp2")) {
				try {
					System.out.println("Checking: "+arg);
					runJpylyzer(arg);
					System.out.println("Jpylyzer says valid: "+jpylyzerSaysValid(arg+JPYLYZER_EXT));
					System.out.println("Matches profile: "+checkJpylyzerProfile(arg+JPYLYZER_EXT, jp2Profile));
					System.out.println("Schematron comparison: "+JpylyzerSchematron.checkJpylyzerOutput(arg+JPYLYZER_EXT));
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
	}

}
