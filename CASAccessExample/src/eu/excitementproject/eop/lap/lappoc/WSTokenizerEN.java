package eu.excitementproject.eop.lap.lappoc;

import java.io.File;

import org.apache.uima.jcas.JCas;

import eu.excitementproject.eop.lap.LAPAccess;
import eu.excitementproject.eop.lap.LAPException;

public class WSTokenizerEN implements LAPAccess {

	public WSTokenizerEN() {
		// What's to be prepared in the Constructor? hmm. 
		
	}

	@Override
	public JCas generateSingleTHPairCAS(String text, String hypothesis)
			throws LAPException {
		
		return null;
	}

	@Override
	public void processRawInputFormat(File inputFile, File outputDir)
			throws LAPException {
		

	}

	@Override
	public JCas addAnnotationOn(JCas aJCas, String viewName)
			throws LAPException {
		return null;
	}

	@Override
	public JCas addAnnotationOn(JCas aJCas) throws LAPException {
		return addAnnotationOn(aJCas, "_InitialView"); 
	}
}
