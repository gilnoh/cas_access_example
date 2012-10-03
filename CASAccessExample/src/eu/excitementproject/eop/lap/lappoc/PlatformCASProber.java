package eu.excitementproject.eop.lap.lappoc;

import java.io.File;
import java.io.PrintStream;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.Type;
import org.apache.uima.jcas.JCas;

import eu.excitement.casexample.PrintAnnotations;
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
		// TODO write this, and use this instead of PrintAnnotation 
		// check two views 
		
		// check entailment metadata 
		
		// check entailment pairs, loop for each pair 
		
		// check annotations of T
		
		// check annotations of H 		
		
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
		JCas aJCas = null; 
		// TODO write this
		// 1. deserialize the XMI file 
		
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
	
	@SuppressWarnings("unused")
	private static int checkAnnotation(CAS aCAS, Type aAnnotType)
	{
		// Iterate over this type and count the numbers, 
		// TODO see PrintAnnotations and do similar things 
		return 0; 
	}
	
	private static final String TVIEW = "TextView";
	private static final String HVIEW = "HypothesisView"; 
}
