package eu.excitement.casexample;

import java.util.HashMap;
import java.util.Map;

import org.apache.uima.UIMAFramework;
import org.apache.uima.UimaContextAdmin;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.Resource;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.util.XMLInputSource;

public class CasAccessExample3 {

	/**
	 * This example shows how you can use an existing AE, to annotate 
	 * 1) Single View CAS
	 * 2) a View of the multi-View CAS 
	 * 
	 */
	
	
	public static void main(String[] args) {
		
		/// 1. Generate a CAS (JCas) with single view, generate another CAS with two views. 
		JCas jcas1=null;
		JCas jcas2=null; 
		AnalysisEngine ae = null; 
		try 
		{
			XMLInputSource in = new XMLInputSource("./desc/WSSeparator.xml"); // a simple AE, that is equal to pseudo-tokenizer of Example 1 
			ResourceSpecifier specifier = UIMAFramework.getXMLParser().parseResourceSpecifier(in);		
			ae = UIMAFramework.produceAnalysisEngine(specifier); 
			jcas1 = ae.newJCas(); 
			jcas2 = ae.newJCas(); 
		}
		catch (Exception e)
		{
			e.printStackTrace(); 
		}
		
		// jcas1 as single view 
		jcas1.setDocumentLanguage("EN");
		jcas1.setDocumentText("This is an example of single view CAS");
		
		JCas viewT = null; 
		JCas viewH = null; 
		try {
			viewT = jcas2.createView("TextView");
			viewH = jcas2.createView("HypothesisView");
		}
		catch (Exception e)
		{
			e.printStackTrace(); 
		}
		
		viewT.setDocumentText("This is a sentence from Text View.");
		viewH.setDocumentText("This is the sentence from Hypothesis View.");
		
		// 2. now Annotate them with the AE. 
		// For Single View AE, it is simple. Just call it. 
		try {
			ae.process(jcas1);
		}
		catch (Exception e) {
			e.printStackTrace(); 
		}
		
		// now it will have token annotations. check its content 
		PrintAnnotations.printAnnotations(jcas1.getCas(), System.out); 
		
		// 3. For multi View AE, you have to "map the views". 
		// simple ae.process(viewT), or ae.process(viewH) never works, 
		// since whatever view it is passed on, AE only sees the top CAS. 
		
		// Mapping TextView 
		// Recreate another AE with different "View mapping" 
		
		// This is a lenghty code -- (See UIMA Tutorial and Developers Guide 6.4.4) 	
		// for full detail. 
		// or use "addAnnotationTo()" method that wraps this. 
		try {
			//create a "root" UIMA context for your whole application
			UimaContextAdmin rootContext =
					UIMAFramework.newUimaContext(UIMAFramework.getLogger(),
							UIMAFramework.newDefaultResourceManager(),
							UIMAFramework.newConfigurationManager());

			XMLInputSource input = new XMLInputSource("./desc/WSSeparator.xml");
			ResourceSpecifier desc = UIMAFramework.getXMLParser().parseAnalysisEngineDescription(input);

			//setup sofa name mappings using the api
			HashMap<String,String> sofamappings = new HashMap<String,String>();
			sofamappings.put("_InitialView", "TextView");
			//sofamappings.put("localName2", "globalName2");
			
			//create a UIMA Context for the new AE we are about to create
			//first argument is unique key among all AEs used in the application
			UimaContextAdmin childContext = rootContext.createChild("WSSeparator", sofamappings);

			//instantiate AE, passing the UIMA Context through the additional
			//parameters map
			Map<String,Object> additionalParams = new HashMap<String,Object>();
			additionalParams.put(Resource.PARAM_UIMA_CONTEXT, childContext);

			AnalysisEngine ae2 = null; 
			ae2 =  UIMAFramework.produceAnalysisEngine(desc,additionalParams);
			ae2.process(jcas2); 
		}
		catch (Exception e)
		{
			e.printStackTrace(); 
		}
		// check annotation 
		PrintAnnotations.printAnnotations(viewT.getCas(), System.out); 

		// do the same thing for HypothesisView, but I've wrapped it as a method
		try {
			addAnnotationTo(jcas2, "HypothesisView");
		}
		catch (Exception e)
		{
			e.printStackTrace(); 
		}
		// check annotations 
		PrintAnnotations.printAnnotations(viewH.getCas(), System.out); 
	}
	
	
	static private void addAnnotationTo(JCas aJCas, String viewName) throws Exception
	{
		// NOte: this is just an example, not really efficient. . 
		// This method actually inits the AE every time it runs. 
		// If the mapping + AE is fixed (normally they are), you should 
		// do the AE generations in the constructor. 
		try {
			//create a "root" UIMA context for your whole application
			UimaContextAdmin rootContext =
					UIMAFramework.newUimaContext(UIMAFramework.getLogger(),
							UIMAFramework.newDefaultResourceManager(),
							UIMAFramework.newConfigurationManager());

			XMLInputSource input = new XMLInputSource("./desc/WSSeparator.xml");
			ResourceSpecifier desc = UIMAFramework.getXMLParser().parseAnalysisEngineDescription(input);

			//setup sofa name mappings using the api
			HashMap<String,String> sofamappings = new HashMap<String,String>();
			sofamappings.put("_InitialView", viewName);
			
			//create a UIMA Context for the new AE we are about to create
			//first argument is unique key among all AEs used in the application
			UimaContextAdmin childContext = rootContext.createChild("WSSeparator", sofamappings);

			//instantiate AE, passing the UIMA Context through the additional
			//parameters map
			Map<String,Object> additionalParams = new HashMap<String,Object>();
			additionalParams.put(Resource.PARAM_UIMA_CONTEXT, childContext);

			AnalysisEngine ae =  UIMAFramework.produceAnalysisEngine(desc,additionalParams);
			ae.process(aJCas); 
		}
		catch (Exception e)
		{
			throw e; 
		}

	}
	
}
