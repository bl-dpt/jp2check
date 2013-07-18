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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.URIResolver;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/**
 * A class to generate XSLT from Schematron input and to validate input files against that XSLT
 * @author wpalmer
 *
 */
public class JpylyzerSchematron {

	private final static String SCHEMATRONSCHEMA = "jpylyzer-schematron.sch";
	private final static String SCHEMATRONXSLT = SCHEMATRONSCHEMA+".xsl"; 
	
	private final static class ISOFiles {
		final static String ISO_DSDL="iso-schematron/iso_dsdl_include.xsl";
		final static String ISO_ABSTRACT="iso-schematron/iso_abstract_expand.xsl";
		final static String ISO_SVRL="iso-schematron/iso_svrl_for_xslt1.xsl";
		
		private ISOFiles() {}
	}
	
	//see here: http://stackoverflow.com/a/12453881
	/**
	 * Implement a URIResolver so that XSL files can be found in the jar resources
	 * @author wpalmer
	 */
	public static class ResourceResolver implements URIResolver {
		public StreamSource resolve(String pRef, String pBase) {
			//System.out.println("resolving: "+ref+", "+base);
			return new StreamSource(ResourceResolver.class.getClassLoader().getResourceAsStream("iso-schematron/"+pRef));
		}
		/**
		 * Empty initialiser
		 */
		public ResourceResolver() {}
	}
	
	/**
	 * Convert a Schematron .sch to an XSLT
	 * @param pSchematronFile Schematron file
	 * @param pSchematronXSL Name of output XSLT file
	 * @return true or false based on succes or otherwise of transformation
	 */
	private static boolean schematronToXSLT(InputStream pSchematronFile, String pSchematronXSL) {
		try {
			//generate an xsl from the schematron sch
			TransformerFactory factory = TransformerFactory.newInstance();
			factory.setURIResolver(new ResourceResolver());
			Transformer transformer1 = factory.newTransformer(new StreamSource(JpylyzerSchematron.class.getClassLoader().getResourceAsStream(ISOFiles.ISO_DSDL)));
			Transformer transformer2 = factory.newTransformer(new StreamSource(JpylyzerSchematron.class.getClassLoader().getResourceAsStream(ISOFiles.ISO_ABSTRACT)));
			Transformer transformer3 = factory.newTransformer(new StreamSource(JpylyzerSchematron.class.getClassLoader().getResourceAsStream(ISOFiles.ISO_SVRL)));
			transformer3.setParameter("terminate", "false");//don't halt on errors

			ByteArrayOutputStream tempOutput1 = new ByteArrayOutputStream();
			ByteArrayOutputStream tempOutput2 = new ByteArrayOutputStream();

			transformer1.transform(new StreamSource(pSchematronFile), new StreamResult(tempOutput1));
			transformer2.transform(new StreamSource(new ByteArrayInputStream(tempOutput1.toByteArray())), 
					new StreamResult(tempOutput2));
			transformer3.transform(new StreamSource(new ByteArrayInputStream(tempOutput2.toByteArray())), 
					new StreamResult(new FileOutputStream(pSchematronXSL)));
			
			return true;
			
		} catch (TransformerException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		return false;
	}

	/**
	 * Validate a file against a Schematron XSLT
	 * @param pInputFile input file to check
	 * @param pSchematronXSL XSLT file to use
	 * @param pResultsFile file to write results to
	 * @return true or false based on success or otherwise of validation
	 */
	private static boolean validateSchematron(String pInputFile, String pSchematronXSL, String pResultsFile) {
		try {
			//use the generated xsl to validate the outputs
			TransformerFactory factory = TransformerFactory.newInstance();
			Transformer transformer4 = factory.newTransformer(new StreamSource(pSchematronXSL));
			transformer4.transform(new StreamSource(pInputFile), new StreamResult(new FileOutputStream(pResultsFile)));

			try {
				DocumentBuilder docB = DocumentBuilderFactory.newInstance().newDocumentBuilder();
				Document doc = docB.parse(pResultsFile);
				Node root = doc.getFirstChild();		
				XPath xpath = XPathFactory.newInstance().newXPath();
				String path = "count(/schematron-output/failed-assert)";
				String result = xpath.evaluate(path, root);
				int count = new Integer(result);
				//System.out.println(result+" errors");
				if(count==0) return true;

			} catch(ParserConfigurationException e) {
				e.printStackTrace();
			} catch (XPathExpressionException e) {
				e.printStackTrace();
			} catch (SAXException e) {
				e.printStackTrace();
			}

		} catch(IOException e) {
			e.printStackTrace();			
		} catch (TransformerConfigurationException e1) {
			e1.printStackTrace();
		} catch (TransformerException e1) {
			e1.printStackTrace();
		}

		return false;
	}
	
	/**
	 * Validate a file against a Schematron XSLT
	 * @param pInputFile input file to check
	 * @param pSchematronXSL XSLT file to use
	 * @return true or false based on success or otherwise of validation
	 */
	private static boolean validateSchematron(InputStream pInputFile, String pSchematronXSL) {
		try {
			//use the generated xsl to validate the outputs
			TransformerFactory factory = TransformerFactory.newInstance();
			ByteArrayOutputStream tempOutput1 = new ByteArrayOutputStream();

			Transformer transformer4 = factory.newTransformer(new StreamSource(pSchematronXSL));
			transformer4.transform(new StreamSource(pInputFile), new StreamResult(tempOutput1));

			try {
				DocumentBuilder docB = DocumentBuilderFactory.newInstance().newDocumentBuilder();
				Document doc = docB.parse(new ByteArrayInputStream(tempOutput1.toByteArray()));
				Node root = doc.getFirstChild();		
				XPath xpath = XPathFactory.newInstance().newXPath();
				String path = "count(/schematron-output/failed-assert)";
				String result = xpath.evaluate(path, root);
				int count = new Integer(result);
				//System.out.println(result+" errors");
				if(count==0) return true;

			} catch(ParserConfigurationException e) {
				e.printStackTrace();
			} catch (XPathExpressionException e) {
				e.printStackTrace();
			} catch (SAXException e) {
				e.printStackTrace();
			}

		} catch(IOException e) {
			e.printStackTrace();			
		} catch (TransformerConfigurationException e1) {
			e1.printStackTrace();
		} catch (TransformerException e1) {
			e1.printStackTrace();
		}

		return false;
	}
	
	/**
	 * Validate a file against a Schematron XSLT
	 * @param pInputFile input file to check
	 * @param pSchematronXSL XSLT file to use
	 * @return true or false based on success or otherwise of validation
	 */
	private static boolean validateSchematron(String pInputFile, String pSchematronXSL) {
		try {
			return validateSchematron(new FileInputStream(pInputFile), pSchematronXSL);
		} catch(FileNotFoundException e) {
		}
		return false;
	}
	
	/**
	 * Checks jpylyzer outputs against the default schema
	 * @param pJpylyzerOutput file to check
	 * @return true or false based on success or otherwise of validation
	 */
	public static boolean checkJpylyzerOutput(String pJpylyzerOutput) {
		InputStream schema = JpylyzerSchematron.class.getClassLoader().getResourceAsStream(SCHEMATRONSCHEMA); 
		//File schema = new File(SCHEMATRONSCHEMA);
		File xslt = new File(SCHEMATRONXSLT); 
		//check schematron file exists
		//if xslt is generated then don't regenerate
		if(!xslt.exists()) schematronToXSLT(schema, SCHEMATRONXSLT);
		return validateSchematron(pJpylyzerOutput, SCHEMATRONXSLT);
	}
	
	/**
	 * Checks jpylyzer outputs against the default schema
	 * @param pSchemaFile schema file to use
	 * @param pJpylyzerOutput file to check
	 * @return true or false based on success or otherwise of validation
	 */
	public static boolean checkJpylyzerOutput(String pSchemaFile, String pJpylyzerOutput) {
		try {
			return checkJpylyzerOutput(pSchemaFile, new FileInputStream(pJpylyzerOutput));
		} catch(FileNotFoundException e) {
		}
		return false;
	}
	
	/**
	 * Checks jpylyzer outputs against the default schema
	 * @param pSchemaFile schema file to use
	 * @param pJpylyzerOutput file to check
	 * @return true or false based on success or otherwise of validation
	 */
	public static boolean checkJpylyzerOutput(String pSchemaFile, InputStream pJpylyzerOutput) {
		File schema = new File(pSchemaFile);
		File xslt = new File(SCHEMATRONXSLT); 
		//check schematron file exists
		if(!schema.exists()) return false;
		//check to see if we need to generate an xslt
		//->if it doesn't exist
		//->if it's older than the schema
		if(!xslt.exists()||(schema.lastModified()>xslt.lastModified())) {
			try {
				if(!schematronToXSLT(new FileInputStream(schema), SCHEMATRONXSLT)) return false;
			} catch(FileNotFoundException e) {
				return false;
			}
		}
		return validateSchematron(pJpylyzerOutput, SCHEMATRONXSLT);
	}
	
	/**
	 * Checks jpylyzer outputs against the default schema
	 * @param pSchemaFile schema file to use
	 * @param pJpylyzerOutput file to check
	 * @return true or false based on success or otherwise of validation
	 */
	public static boolean checkJpylyzerOutput(InputStream pSchemaFile, String pJpylyzerOutput) {
		try {
			return checkJpylyzerOutput(pSchemaFile, new FileInputStream(pJpylyzerOutput));
		} catch(FileNotFoundException e) {
		}
		return false;
	}
	
	/**
	 * Checks jpylyzer outputs against the default schema
	 * @param pSchemaFile schema file to use
	 * @param pJpylyzerOutput file to check
	 * @return true or false based on success or otherwise of validation
	 */
	public static boolean checkJpylyzerOutput(InputStream pSchemaFile, InputStream pJpylyzerOutput) {
		if(!schematronToXSLT(pSchemaFile, SCHEMATRONXSLT)) return false;
		return validateSchematron(pJpylyzerOutput, SCHEMATRONXSLT);
	}
	
	/**
	 * Checks jpylyzer outputs against the default schema
	 * @param pSchemaFile schema file to use
	 * @param pJpylyzerOutput file to check
	 * @param pOutputFile file to save outputs to
	 * @return true or false based on success or otherwise of validation
	 */
	public static boolean checkJpylyzerOutput(String pSchemaFile, String pJpylyzerOutput, String pOutputFile) {
		File schema = new File(pSchemaFile);
		File xslt = new File(SCHEMATRONXSLT); 
		//check schematron file exists
		if(!schema.exists()) return false;
		//check to see if we need to generate an xslt
		//->if it doesn't exist
		//->if it's older than the schema
		if(!xslt.exists()||(schema.lastModified()>xslt.lastModified())) {
			try {
				if(!schematronToXSLT(new FileInputStream(schema), SCHEMATRONXSLT)) return false;
			} catch(FileNotFoundException e) {
				return false;
			}
		}
		return validateSchematron(pJpylyzerOutput, SCHEMATRONXSLT, pOutputFile);
	}
	
	/**
	 * Test main method
	 * @param args command line arguments
	 */
	public static void main(String[] args) {
		System.out.println(checkJpylyzerOutput("openjpeg-schematron.sch", "test2.xml", "out2.xml"));
	}

}
