package eu.excitement.casexample;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
//import java.io.IOException;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngine;
//import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
//import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.util.XMLInputSource;
import org.apache.uima.util.XMLSerializer;

//import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.Type; 
import org.apache.uima.cas.impl.XmiCasDeserializer;
import org.apache.uima.cas.impl.XmiCasSerializer;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.jcas.tcas.Annotation;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.Organization;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.Person;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

import java.util.*; 


// TODO to be merged finally, 
// It needs modifications on 
// EntailmentMetadata (as TOP) 
// Pair (as TOP) 
// Update auto-generated JCAS java files (with above) 

/**
 * This is a very short example that shows how to: 
 * 1) generate a CAS (as JCas) 
 * 2) add annotations to CAS (with JCas java classes) 
 * 3) store it as a file, read it from a file 
 * 4) enumerate over annotations 
 * 5) check other annotations that also covers "text of this annotation" 
 * 
 * For "Views" and "multi-view" annotations (like TE type), see Example-2. 
 * 
 * Warning : This is, a very primitive example with a big ugly main() 
 * - just to help your understanding. :-) 
 * 
 * Note that this example uses UIMA types and their generated 
 * Java codes by including ExcitementTypes project in the build path.) 
 * 
 * Also note that what is shown here in this example can be done more 
 * cleanly/easily (with shorter codes) with UIMAFit. The example 
 * uses only the UIMA APIs. 
 * 
 * @author Gil 
 */


public class CasAccessExample1 {

	public static void main(String[] args) {

				
		try  // for the moment, let's don't pay attention to Exceptions.   
		{ 				
			///
			/// 1) Generate a CAS (JCas) 
			/// 
			
			// We need a CAS (JCas, we will use). Each JCAS holds its "known" type system, by 
			// attaching itself to an AE. Here, we will not really use an AE (UIMA Analysis Engine). 
			// But we need at least a "dummyAE", which holds all EXCITEMENT adopted/defined types. 
			XMLInputSource in = new XMLInputSource("./desc/DummyAE.xml"); // This AE does nothing, but holding all types. 
			ResourceSpecifier specifier = UIMAFramework.getXMLParser().parseResourceSpecifier(in);		
			AnalysisEngine ae = UIMAFramework.produceAnalysisEngine(specifier); 
			
			// Now we have the AE. we can use it to generate CAS  			
			JCas jcas1 = ae.newJCas(); // this is the command to get a JCas. 
			JCas jcas2 = ae.newJCas(); 
			jcas1.setDocumentText("Did Goethe visited Heidelberg University in 1788?"); 
			jcas1.setDocumentLanguage("EN"); 
			jcas2.setDocumentText("Freiheit und Leben kann man uns nehmen, die Ehre nicht. - Otto Wels."); 
			jcas2.setDocumentLanguage("DE"); 
			
			// here we have two CASes with short texts. If we want an AE to work on this 
			// newly generated CAS, we can pass the CAS to AE, like the followings.  
			ae.process(jcas1);  // ... but since this is a dummy and nothing is added on CAS

			// Before actually adding some annotation, let's see what kind of "types" 
			// this JCas is aware of. 
			System.out.print("All Type names know to the JCas\n=====\n"); 
			TypeSystem ts = jcas1.getTypeSystem(); 
			Iterator<Type> ti = ts.getTypeIterator(); 
			while(ti.hasNext())
			{
				Type t = ti.next(); 
				System.out.println(t.getName()); 
			}// prints out all type names within the typesystem. 
			System.out.println("====="); 
			
			///
			/// 2) Add annotations to CAS 
			///
			
			// Okay. let's try to add a few tokens. 		
			// first, let's get some (not so good) tokens by WS separation. 
			// and generate "Token" annotation for each of them and add to CAS. 
			String enText = jcas1.getDocumentText(); 
		    StringTokenizer st = new StringTokenizer(enText); 
		    int pos=0; 
		    while(st.hasMoreTokens())
		    {
		    	String thisTok = st.nextToken(); 
		    	int begin = enText.indexOf(thisTok, pos);
		    	int end = begin + thisTok.length(); 
		    	pos = end; 
		    	
		    	// This is the generic pattern, how you add a CAS annotation to a JCas
		    	{
		    		Token tokenAnnot = new Token(jcas1);  // Token defined in type system with segmentation.type.Token
		    		tokenAnnot.setBegin(begin); // Annotation's features are accessed with set/get methods  
		    		tokenAnnot.setEnd(end); 
		    		// You can set additional features by calling 
		    		// setLemma() (points a Lemma annotation), setPOS() (points a POS annot)
		    		// , etc. But for now, lets only set begin and end.  
		    		// In this case, POS and Lemma features will remain "null", 
		    		// which means "no data here" in CAS.
		    		
		    		// Finally, add the new annotation to the "annotation index".
		    		tokenAnnot.addToIndexes(); //   
		    		// this is a mandatory step. 
		    		// Without this, the annotation is not accessible from outside. 
		    	}
		    }

		    // Let's add two NER annotations also. 
		    {
		    	// we have 
		    	// "Goethe (4-10)" 
		    	// , "Heidelberg" (19-29), and "University" (30-40) . 		    			    	
		    	Organization orgAnnot = new Organization(jcas1); // ner.type.Organization
		    	orgAnnot.setBegin(19); orgAnnot.setEnd(40); 
		    	orgAnnot.setValue("ORG"); // (an artificial) raw output from NER tool,  
		        orgAnnot.addToIndexes();
		        
		        Person perAnnot = new Person(jcas1); //ner.type.Person
		        perAnnot.setBegin(4); perAnnot.setEnd(10); 
		        perAnnot.setValue("PER"); // raw output from NER tool, 
		        perAnnot.addToIndexes(); 
		        
		    }

		    // All right, let's see what we have inside of this jcas1
		    // Calling PrintAnnotations (which is borrowed from the official UIMA example) 
		    PrintAnnotations.printAnnotations(jcas1.getCas(), System.out); 

		    //
		    // 3) Serialize and De-serialize 
		    //
		    
		    // CASes can be stored and accessed from/to file. 
		    // Standard serialization format is the XMI (XML Metadata Interchange?) file.  
		    // For more info, see UIMA guide 8.3 Using XMI CAS serialization 
		    
		    // Serializing formula. 
		    File xmiFile = new File("./output/example1.xmi"); 
			FileOutputStream out = new FileOutputStream(xmiFile);
			XmiCasSerializer ser = new XmiCasSerializer(jcas1.getTypeSystem());
			XMLSerializer xmlSer = new XMLSerializer(out, false);
			ser.serialize(jcas1.getCas(), xmlSer.getContentHandler());
			out.close();
			
		    // the XMI file is now generated. You can load it up and see how it looks like. 
			// It is a bunch of not-so-human-friendly data. UIMA Java release (uimaj) also 
			// includes an XMI viewer. 
			
			XMLInputSource in2 = new XMLInputSource("./desc/WSSeparator.xml"); // This AE does nothing, but holding all types. 
			ResourceSpecifier specifier2 = UIMAFramework.getXMLParser().parseResourceSpecifier(in2);		
			AnalysisEngine ae2 = UIMAFramework.produceAnalysisEngine(specifier2); 

			//Let's load it and check it in a new CAS 
		    //JCas jcas3 = ae.newJCas(); // a new CAS 
			JCas jcas3 = ae2.newJCas(); 
		    FileInputStream inputStream = new FileInputStream(xmiFile);
		    XmiCasDeserializer.deserialize(inputStream, jcas3.getCas()); 
		    inputStream.close();
		    	
		    // remove comments of the followings to see the loaded jcas3 is 
		    // actually the same with jcas1 
		    System.out.println("=====loaded=====");
		    PrintAnnotations.printAnnotations(jcas3.getCas(), System.out); 
		    		    
		    //
		    // 4) enumerate over annotations 
		    // Let's assume we want to check all tokens in a JCas. how can we do that? 
		    // CAS keeps index, and we can request the index, with specific types. For example, 
		    // Let's only walk the token annotations. 
			AnnotationIndex<Annotation> tokenIndex = jcas3.getAnnotationIndex(Token.type); 
			Iterator<Annotation> tokenIter = tokenIndex.iterator(); 
			int i=1; 
			System.out.print("Iterating over tokens:"); 
		    while(tokenIter.hasNext())
		    {
		    	// The Iterator will walk over tokens, starting from low "begin" to high "begin" positions. 
		    	Token curr = (Token) tokenIter.next(); 
		    	String tokenText = curr.getCoveredText();  
		    	int tokenBegin = curr.getBegin(); 
		    	int tokenEnd = curr.getEnd(); 		    	
		    	// getLemma(), getPOS(), will return null, since we didn't fill them. 
		    	// Note that if they are filled in, they will return "POS" type, or "Lemma" type. (not strings)  
		    	System.out.printf("Token %d: %s (%d,%d)\n", i, tokenText, tokenBegin, tokenEnd);     	
		    	i++; 
		    } // this result will not show you any NER types, since it iterates over only Tokens. 
		    System.out.println("==="); 
		    
		    // Let's see another example. Here, let's see "supertypes" and "subtypes". 
		    // We have two NER types in the jcas. One is "ner.type.Person", the other is "ner.type.Organization". 
		    // Both have the same "supertype", "ner.type.NamedEntity". 
		    
		    // If we iterate the annotation index with "Organization" (or Person) 
		    // we get only one result. 
		    AnnotationIndex<Annotation> orgIndex = jcas3.getAnnotationIndex(Organization.type); 
		    Iterator<Annotation> orgIter = orgIndex.iterator(); 
		    Organization anOrg = null; 
		    while(orgIter.hasNext())
		    {
		    	Organization curr = (Organization) orgIter.next(); 
		    	System.out.printf("Organization: %s (%d,%d)\n", curr.getCoveredText(), curr.getBegin(), curr.getEnd());
		    	anOrg = curr; 
		    }
		   
		    // If we iterate the annotation index with "NamedEntity".  		    
		    // we get both of them. 
		    AnnotationIndex<Annotation> nerIndex = jcas3.getAnnotationIndex(NamedEntity.type);
		    Iterator<Annotation> nerIter = nerIndex.iterator(); 
		    while(nerIter.hasNext())
		    {
		    	NamedEntity curr = (NamedEntity) nerIter.next(); 
		    	System.out.printf("NamedEntity (%s): %s (%d,%d)\n", curr.getType().getName(), curr.getCoveredText(), curr.getBegin(), curr.getEnd());		    	
		    	// Note that annot.getType().getName() will return the full type, not the supertype. 
		    	// Here, it will get you full-type name of Organization, or Person. 
		    }
		    System.out.println("==="); 
		    
		    //
		    // 5) check "other" annotations that covers "my annotation" 
		    //  
		    
		    // Okay. you have a NamedEntity annotation, say "Heidelberg University".  
		    System.out.printf("Organization: %s (%d,%d) has;\n", anOrg.getCoveredText(), anOrg.getBegin(), anOrg.getEnd());
		    
		    // You want to know, "what tokens do this NER has?". 
		    // Note that an NER annotation (like ner.type.Organization) does not have 
		    // a direct link to Token type. (So, we don't have anOrg.getToken() )  
		    // How can we get the Tokens that is covered by this NER? 
		    
		    // UIMAFit provides this kind of tools, (like selectCovered() or selectBetween() 
		    // of uimafit.util.JCasUtil), but let's just write our own small code. No problem.  
		    // the function tokensCoveredByThisAnnotation() does this. 
		    
		    Vector<Token> t = tokensCoveredByThisAnnotation(jcas3, anOrg); // static method that iterates and finds 
		    Iterator<Token> v = t.iterator(); 
		    while(v.hasNext())
		    {
		    	Token tok = v.next();
		    	System.out.printf("Token: %s (%d,%d)\n", tok.getCoveredText(), tok.getBegin(), tok.getEnd()); 
		    }
		    // This only prints the "tokens" annotated on the text of this "NER" ... 
		    
		    //
		    // END of this EXAMPLE
		    // 
		}
		catch (Exception e)
		{
			e.printStackTrace(); 
			
		}
		
	}
	
	private static Vector<Token> tokensCoveredByThisAnnotation(JCas aJcas, Annotation annot)
	{
		Vector<Token> t = new Vector<Token>();  
		
		AnnotationIndex<Annotation> tokenIndex = aJcas.getAnnotationIndex(Token.type); 
		Iterator<Annotation> tokenIter = tokenIndex.iterator(); 
	    while(tokenIter.hasNext())
	    {
	    	Token curr = (Token) tokenIter.next(); 
	    	int tokenBegin = curr.getBegin(); 
	    	int tokenEnd = curr.getEnd(); 		    	
	    	
	    	if (tokenEnd < annot.getBegin())
	    		continue; 
	    	
	    	if (tokenBegin > annot.getEnd())
	    		break; 
	    	
	    	if ( (tokenBegin >= annot.getBegin()) && (tokenEnd <= annot.getEnd()) )
	    	{
	    		t.add(curr); 
	    	}
	    } 
		return t; 
	}
	
}
