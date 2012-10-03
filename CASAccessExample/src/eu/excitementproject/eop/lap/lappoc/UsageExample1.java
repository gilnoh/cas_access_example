package eu.excitementproject.eop.lap.lappoc;

import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.TOP;

import eu.excitement.casexample.PrintAnnotations;
import eu.excitement.type.entailment.Pair;
import eu.excitementproject.eop.lap.LAPException;


public class UsageExample1 {

	/**
	 * Simple usage example of this LAP, and also the PlatformCASProber.  
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		
		WSTokenizerEN lap = null; 
		JCas aJCas = null; 
		try {
			lap = new WSTokenizerEN(); 
		}
		catch(LAPException e)
		{
			System.err.println(e.getMessage()); 
			e.printStackTrace();
			System.exit(1); 
		}
		
		try {
			aJCas = lap.generateSingleTHPairCAS("This is Something.", "This is something else."); 
		}
		catch (LAPException e)
		{
			System.err.println(e.getMessage()); 
			e.printStackTrace();
			System.exit(2); 
		}
		
		printTHInfo(aJCas); 
		
	}
	
	public static void printTHInfo(JCas aCas)
	{
		try {
			JCas tView = aCas.getView("TextView"); 
			JCas hView = aCas.getView("HypothesisView");  // again, if the view names are not there, it will raise exceptions

			// Now we need to get Entailment.Pair, to find out about 
			// the entailment problem. 
			
			// If the goal type is an annotation that has begin / end (most of them. Tokens, POSes, NERs ..) 
			// 1) use getAnnotationsIndex and iterate over them. It returns an ordered iterator. 
						
			// If the target type is *not* annotations (i.e. EntailmentMetadata, Pair)  
			// 2) getJFSIndexRepository().getAllIndexedFS() 
			// This can be used to fetch any type instances. (including annotation and non annotation)
			// Unlike getAnnotationsIndex, the returned data has no orders.  
			// (Only a few ECXITEMENT types are non-annotation: including Entailment.EntailmentMetadata, and Entailment.Pair.)   

			// Since we need to get Entailment.Pair,  use getAllIndexedFS
			FSIterator<TOP> pairIter = aCas.getJFSIndexRepository().getAllIndexedFS(Pair.type);
			// note that we get it from outside "wrapping" CAS, not from the view CAS. 
			
			Pair p=null; 
			int i=0; 
			System.out.println("===="); 
			while(pairIter.hasNext())
			{
				p = (Pair) pairIter.next(); 
				i++; 
				System.out.printf("PairID: %s\n", p.getPairID()); 
				System.out.printf("Text of the pair: %s\n", p.getText().getCoveredText()); 
				// note that Text annotation is actually on TextView. 
				System.out.printf("Hypothesis of the pair: %s\n", p.getHypothesis().getCoveredText());
				// note that Hypothesis annotation is actually on HypothesisView. You can access it from pair. 
			}
			System.out.printf("----\nThe CAS had %d pairs.\n====\n", i);
			
			System.out.println("=== Contents of TextView===");
		    PrintAnnotations.printAnnotations(tView.getCas(), System.out); 
			System.out.println("=== Contents of HypothesisView===");
		    PrintAnnotations.printAnnotations(hView.getCas(), System.out); 
		}
		catch (CASException e)
		{
			e.printStackTrace(); 
		}
		
	}

}
