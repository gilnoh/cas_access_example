package eu.excitementproject.eop.lap.lappoc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.XmiCasDeserializer;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLInputSource;
import org.xml.sax.SAXException;

import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Lemma;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;

import eu.excitement.casexample.PrintAnnotations;
import eu.excitement.type.entailment.EntailmentMetadata;
import eu.excitement.type.entailment.Pair;
import eu.excitementproject.eop.lap.LAPException;

/**
 * This class provides a few "probing" methods into CAS, that will check whether or not 
 * the given CAS follows CAS structure (views and annotations) that EXCITEMENT platform 
 * specification defined for EDA (and other component) input. 
 *   
 * It provides two static methods: 
 * - one that check a given CAS (argument is a JCas) 
 * - one that will check a file (argument is a File, that holds serialized CAS file (XMI)) 
 * 
 * 
 * @author tailblues
 *
 */
public class PlatformCASProber {

	public static void probeCas(JCas aJCas, PrintStream aOut) throws LAPException
	{
		// Okay, we got a CAS, check it for needed data. 
		// check two views 
		JCas tView = null; 
		JCas hView = null; 
		try {
			tView = aJCas.getView(TVIEW); 
			hView = aJCas.getView(HVIEW); 
		}
		catch(CASException e)
		{
			throw new LAPException("This CAS does not have two proper Views.", e); 
		}
		
		if (aOut != null)
		{
			aOut.println("The CAS has two needed Views: Okay");
		}
		
		// check entailment metadata 
		FSIterator<TOP> iter = aJCas.getJFSIndexRepository().getAllIndexedFS(EntailmentMetadata.type);
		
		if (iter.hasNext())
		{
			EntailmentMetadata m = (EntailmentMetadata) iter.next(); 
			// print metatdata content 
			if (aOut != null)
			{
				aOut.println("The Cas has EntailmentMetadata: Okay"); 
				// its content 
				aOut.printf("Language:%s\nTask:%s\n", m.getLanguage(), m.getTask()); 
				aOut.printf("Origin:%s\nChannel:%s\n", m.getOrigin(), m.getChannel()); 
				aOut.printf("TextDocumentID:%s\nTextCollectionID:%s\n", m.getTextDocumentID(), m.getTextCollectionID()); 
				aOut.printf("HypothesisDocumentID:%s\nHypothesisCollectionID:%s\n", m.getHypothesisDocumentID(), m.getHypothesisCollectionID()); 
				
				if (iter.hasNext())
				{
					aOut.println("Warn: The CAS has more than single EntailmentMetadata. The prober only checks the first one.");
				}
			}			
		}
		else
		{
			throw new LAPException("This CAS does not have EntailmentMetadata."); 
		}
		
		// check entailment pairs, loop for each pair 
		iter = aJCas.getJFSIndexRepository().getAllIndexedFS(Pair.type); 
		
		if (iter.hasNext())
		{
			if (aOut != null)
				aOut.println("The CAS has one (or more) Entailment.Pair"); 
			
			// check & print pair content 
			int i; 
			for(i=0; iter.hasNext(); i++)
			{
				Pair p = (Pair) iter.next(); 
				if (aOut != null)
					aOut.printf("PairID: %s\n", p.getPairID()); 
				String text = null; 
				String hypothesis = null; 
				try {
					text = p.getText().getCoveredText(); 
				}
				catch (Exception e)	{
					throw new LAPException("The CAS has a Pair without a proper Text", e); 
				}
				try {
					hypothesis = p.getHypothesis().getCoveredText(); 
				}
				catch (Exception e) {
					throw new LAPException("The CAS has a Pair without a proper Hypothesis", e); 
				}
				if (aOut != null)
				{
					aOut.printf("TextSOFA:%s\nHypothesisSOFA:%s\n", text, hypothesis); 
				}
			}
			if (aOut != null)
				aOut.println("The CAS has " + i + " Pair(s)"); 
		}
		else
		{
			throw new LAPException("This CAS does not have Entailment.Pair."); 			
		}
		if (aOut != null)
		{	
			// check annotations of T
			aOut.println("Checking Annotations of TextView"); 
			checkAnnotations(tView, aOut); 

			// check annotations of H 		
			aOut.println("Checking Annotations of HypothesisView"); 
			checkAnnotations(hView, aOut); 
		}
	}
	
	public static void probeCasAndPrintContent(JCas aJCas, PrintStream aOut) throws LAPException
	{
		probeCas(aJCas, aOut); 
		JCas tView = null; JCas hView = null; 
		try {
			tView = aJCas.getView(TVIEW); 
			hView = aJCas.getView(HVIEW); 
		}
		catch(CASException e)
		{
			throw new LAPException("Unable to get view", e); 
		}
		if (aOut == null)
			return; 
		
		aOut.println("==CONTENTS of TextView===");
	    PrintAnnotations.printAnnotations(tView.getCas(), aOut); 
		aOut.println("==CONTENTS of HypothesisView===");
		PrintAnnotations.printAnnotations(hView.getCas(), aOut);
	}
		
	public static JCas probeXmi(File xmiFile, PrintStream aOut) throws LAPException
	{
		//
		// 1. deserialize the XMI file 
		JCas aJCas = null; 
		try {
			// prepare AE that has the type system, and get JCas 	
			XMLInputSource in = new XMLInputSource(typeAeDescPath); // This AE does nothing, but holding all types. 
			ResourceSpecifier specifier = UIMAFramework.getXMLParser().parseResourceSpecifier(in);		
			AnalysisEngine ae = UIMAFramework.produceAnalysisEngine(specifier); 

			aJCas = ae.newJCas(); 
		}
		catch (IOException e) {
			throw new LAPException("Unable to open the AE descriptor file", e); 
		} catch (InvalidXMLException e) {
			throw new LAPException("Invalid XML descriptor for AE", e); 
		} catch (ResourceInitializationException e) {
			throw new LAPException("Failed to produce the AE for typesystem", e); 
		}
		
		try {	
			//Load the XMI to the JCas 
			FileInputStream inputStream = new FileInputStream(xmiFile);
			XmiCasDeserializer.deserialize(inputStream, aJCas.getCas()); 
			inputStream.close();
		} catch (FileNotFoundException e) {
			throw new LAPException("No such XMI file", e); 
		} catch (SAXException e) {
			throw new LAPException("XMI file failed to parse as XML. Corrupted file?", e);
		} catch (IOException e) {
			throw new LAPException("Unable to access the XMI file",e); 
		}
		
		// 2. run probeCas with it 
		probeCas(aJCas, aOut); 
		return aJCas; 
	}
	
	public static JCas probeXmiAndPrintContent(File xmilFile, PrintStream aOut) throws LAPException
	{
		// 1. run probeXmi 
		JCas aJCas = probeXmi(xmilFile, aOut);
		
		// 2. print content by using printAnnotations 
		try {
			aOut.println("==CONTENTS of TextView===");
			PrintAnnotations.printAnnotations(aJCas.getView(TVIEW).getCas(), aOut);
			aOut.println("==CONTENTS of HypothesisView===");
			PrintAnnotations.printAnnotations(aJCas.getView(HVIEW).getCas(), aOut);
		}
		catch(CASException e)
		{
			throw new LAPException("Unable to access the views", e); 
		}				
		return aJCas; 
	}

	//
	//
	
	private static void checkAnnotations(JCas aJCas, PrintStream aOut) throws LAPException 
	{
		// Here, we will check existence of 
		// Sentence, 
		// Token, 
		// Lemma, 
		// NER, 
		// Dependency parse
		// TODO Add other types like alignment, temporal, semrole, constituent? 
	
		try 
		{
			Sentence sent = new Sentence(aJCas); 
			int sentCount = countAnnotation(aJCas, sent.getType());
			
			Token token = new Token(aJCas); 
			int tokenCount = countAnnotation(aJCas, token.getType()); 
			
			Lemma lemma = new Lemma(aJCas);
			int lemmaCount = countAnnotation(aJCas, lemma.getType());
			
			NamedEntity ner = new NamedEntity(aJCas); 
			int nerCount = countAnnotation(aJCas, ner.getType());
			
			Dependency dep = new Dependency(aJCas);
			int depCount = countAnnotation(aJCas, dep.getType());
		
			if (aOut != null) 
			{	
				aOut.println("It has:"); 
				aOut.println(sentCount + " sentence Annotation(s)");
				aOut.println(tokenCount +" token Annotation(s)");
				aOut.println(lemmaCount +" lemma Annotation(s)");
				aOut.println(nerCount +" NER Annotation(s)");
				aOut.println(depCount +" Dependency Annotation(s)");
			}
		}
		catch (Exception e)
		{
			throw new LAPException("Integrity failure -- CAS has a problem. This exception cannot occur by user-side data. Code itself has some problem. (A type that is unknown to the CAS is queired, etc)"); 
		}
		
	}
	
	private static int countAnnotation(JCas aJCas, Type aAnnotType)
	{
		int count = 0; 
		
	    // get iterator over annotations
		FSIterator<Annotation> iter = aJCas.getAnnotationIndex(aAnnotType).iterator(); 

	    // iterate
		while(iter.hasNext())
		{
			count++; 
			iter.next(); 
		}
		
		return count; 
	}
	
	private static final String typeAeDescPath = "./desc/DummyAE.xml"; 
	private static final String TVIEW = "TextView";
	private static final String HVIEW = "HypothesisView"; 
}
