package eu.excitementproject.eop.lap.lappoc;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.uima.UIMAFramework;
import org.apache.uima.UimaContextAdmin;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.Resource;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLInputSource;

import eu.excitementproject.eop.lap.LAPAccess;
import eu.excitementproject.eop.lap.LAPException;

public class WSTokenizerEN implements LAPAccess {

	public WSTokenizerEN() throws LAPException {
		// setting up the AE, which is needed to get a new JCAS 
		try {
			// Type System AE 
			XMLInputSource in = new XMLInputSource("./desc/DummyAE.xml"); // This AE does nothing, but holding all types. 
			ResourceSpecifier specifier = UIMAFramework.getXMLParser().parseResourceSpecifier(in);		
			AnalysisEngine ae = UIMAFramework.produceAnalysisEngine(specifier); 
			this.typeAE = ae; 
		} 
		catch (IOException e)
		{
			throw new LAPException("Unable to open AE descriptor file", e); 
		}
		catch (InvalidXMLException e)
		{
			throw new LAPException("AE descriptor is not a valid XML", e);			
		}
		catch (ResourceInitializationException e)
		{
			throw new LAPException("Unable to initialize the AE", e); 
		}
	}

	@Override
	public void processRawInputFormat(File inputFile, File outputDir)
			throws LAPException {
		// TODO: keep in mind about the orthogonality, changing addAnnotationTo should work for any annotation methods 
		

	}

	@Override
	public JCas generateSingleTHPairCAS(String text, String hypothesis)
			throws LAPException {
		// TODO: keep in mind about the orthogonality changing addAnnotationTo should work for any annotation methods 
		
		// get a new JCAS
		JCas aJCas = null; 
		try {
			aJCas = typeAE.newJCas(); 
		}
		catch (ResourceInitializationException e)
		{
			throw new LAPException("Unable to create new JCas", e); 
		}
		
		// prepare it with Views and Entailment.* annotations. 
		addTEViewAndAnnotations(aJCas, text, hypothesis); 
		
		// now aJCas has TextView, HypothesisView and Entailment.* types. (Pair, T and H) 
		// it is time to add linguistic annotations 
		
		addAnnotationOn(aJCas, "TextView");
		addAnnotationOn(aJCas, "HypothesisView"); 
		
		return aJCas; 
	}


	@Override
	public JCas addAnnotationOn(JCas aJCas, String viewName)
			throws LAPException {

		// prepare UIMA context (For "View" mapping), for the AE.  
		UimaContextAdmin rootContext = UIMAFramework.newUimaContext(UIMAFramework.getLogger(), UIMAFramework.newDefaultResourceManager(), UIMAFramework.newConfigurationManager());
		ResourceSpecifier desc = null; 
		try {
			XMLInputSource input = new XMLInputSource(this.descPath);
			desc = UIMAFramework.getXMLParser().parseAnalysisEngineDescription(input);
		}
		catch (IOException e) {
			throw new LAPException("Unable to open AE descriptor file", e); 
		} catch (InvalidXMLException e) {
			throw new LAPException("AE descriptor is not a valid XML", e);			
		}
		
		//setup sofa name mappings using the api
		HashMap<String,String> sofamappings = new HashMap<String,String>();
		sofamappings.put("_InitialView", viewName);
		UimaContextAdmin childContext = rootContext.createChild("WSSeparator", sofamappings);
		Map<String,Object> additionalParams = new HashMap<String,Object>();
		additionalParams.put(Resource.PARAM_UIMA_CONTEXT, childContext);

		// time to run 
		try {
			//instantiate AE, passing the UIMA Context through the additional parameters map
			AnalysisEngine ae =  UIMAFramework.produceAnalysisEngine(desc,additionalParams);
			// and run the AE 
			ae.process(aJCas); 
		}
		catch (ResourceInitializationException e) {
			throw new LAPException("Unable to initialize the AE", e); 
		} 
		catch (AnalysisEngineProcessException e) {
			throw new LAPException("AE reported back an Exception", e); 
		}
		return aJCas;
	}

	@Override
	public JCas addAnnotationOn(JCas aJCas) throws LAPException {
		return addAnnotationOn(aJCas, "_InitialView"); 
	}
	
	/**
	 * 
	 * This method adds two views (TextView, HypothesisView) and set their 
	 * SOFA string (text on TextView, h on HView). 
	 * And it also annotates them with Entailment.Metadata, Entailment.Pair, 
	 * Entailment.Text and Entailment.Hypothesis. (But it adds no linguistic annotations. )  
	 * @param aJCas 
	 * @param text
	 * @param hypothesis
	 * @return
	 */
	private void addTEViewAndAnnotations(JCas aJCas, String text, String hypothesis) throws LAPException {
		
		// generate views and set SOFA 
		JCas textView = null; JCas hypoView = null; 
		try {
			textView = aJCas.createView("TextView");
			hypoView = aJCas.createView("HypothesisView"); 
		}
		catch (CASException e) 
		{
			throw new LAPException("Unble to create new views", e); 
		}
		textView.setDocumentLanguage(this.languageIdentifier); 
		hypoView.setDocumentLanguage(this.languageIdentifier);
		textView.setDocumentText(text);
		hypoView.setDocumentText(hypothesis);
		
		// annotate Text 
		
		// annotate Hypothesis 

		// annotate Pair 

		// annotate Metadata 
		
	}
		
	private final String descPath = "./desc/WSSeparator.xml"; 

	/**
	 * Analysis engine that holds the type system. Note that even if you 
	 * don't call AE, (or not using any AE), you need this. AE provides .newJCas() 
	 */
	private final AnalysisEngine typeAE; 
	private final String languageIdentifier = "EN"; 
}
