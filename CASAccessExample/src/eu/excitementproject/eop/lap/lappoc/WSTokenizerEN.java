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

import eu.excitement.type.entailment.EntailmentMetadata;
import eu.excitement.type.entailment.Hypothesis;
import eu.excitement.type.entailment.Pair;
import eu.excitement.type.entailment.Text;
import eu.excitementproject.eop.lap.LAPAccess;
import eu.excitementproject.eop.lap.LAPException;

//TODO before merge? 
// 1) make it independent to casexample. (moving PrintAnnotations and WSSeparator into lappoc) 
// 

/**
 * A sample LAP component that follows the interface LAPAccess. 
 * This implementation intentionally uses only the "addAnnotationOn(Jcas, String)" 
 * as the main annotation method. This may be a bit inefficient (especially when 
 * you use AE in addAnnotationOn()), but it makes this sample implementation as a
 * "generic" one. --- if you replace "addAnnotationOn()" for your own annotator, 
 * you automatically get other methods like "generateSingleTHPair()" and 
 * "processRawInputFormat()". 
 * 
 * Note that addAnnotationOn() of this sample LAP only annotates "Token" by 
 * whitespace separation. Replace it with a real linguistic analysys component, 
 * (and add some codes for "Language" and other metadata) you get your LAP. 
 * (Again, this may not be an efficient implementation.) 
 * 
 * Note that generating a new CAS is an expansive operation. try to reuse 
 * existing one by cleaning it up with reset().
 * 
 * @author Gil 
 *
 */
public class WSTokenizerEN implements LAPAccess {

	public WSTokenizerEN() throws LAPException {
		// setting up the AE, which is needed to get a new JCAS
		// note that you need at least one AE to get a JCAS. (valina UIMA) 
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
	 * 
	 * <P>
	 * This method does not Pair ID, or Metatdata.channel, source, collectionID, etc. 
	 * They should be set by the caller. 
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
		
		// annotate Text (on TextView) 
		Text t = new Text(textView);
		t.setBegin(0); t.setEnd(text.length()); 
		t.addToIndexes(); 
		
		// annotate Hypothesis (on HypothesisView) 
		Hypothesis h = new Hypothesis(hypoView);
		h.setBegin(0); h.setEnd(hypothesis.length()); 
		h.addToIndexes(); 
		
		// annotate Pair (on the top CAS) 
		Pair p = new Pair(aJCas); 
		p.setText(t); // points T & H 
		p.setHypothesis(h); 
		// p.setPairID() is not set by this method. If needed, the caller should set it. 
		p.addToIndexes(); // this is indexed to the 
		
		// annotate Metadata (on the top CAS) 
		EntailmentMetadata m = new EntailmentMetadata(aJCas); 
		m.setLanguage(this.languageIdentifier); 
		// again, the method don't set channel, origin, etc on the metadata. If needed, the caller should set it. 
		m.addToIndexes(); 
		
	}
		
	/**
	 * Path to actual "worker AE". If you don't use AE, this isn't needed (unlike typeAE). 
	 */
	private final String descPath = "./desc/WSSeparator.xml"; 

	/**
	 * Analysis engine that holds the type system. Note that even if you 
	 * don't call AE, (or not using any AE), you need this. AE provides .newJCas() 
	 */
	private final AnalysisEngine typeAE; 
	
	/**
	 * We will set language directly with this id. 
	 */
	private final String languageIdentifier = "EN"; 
}
