package org.aksw.hawk.experiment;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import org.aksw.hawk.datastructures.HAWKQuestion;
import org.aksw.hawk.datastructures.HAWKQuestionFactory;
import org.aksw.hawk.index.DBOIndex;
import org.aksw.hawk.index.Patty_relations;
import org.aksw.qa.commons.load.QALD_Loader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.sparql.core.TriplePath;
import com.hp.hpl.jena.sparql.syntax.ElementPathBlock;
import com.hp.hpl.jena.sparql.syntax.ElementVisitorBase;
import com.hp.hpl.jena.sparql.syntax.ElementWalker;
/*
 * EXPERIMENTAL! 
 */
public class IndexComparer {

	static Logger log = LoggerFactory.getLogger(IndexComparer.class);
	static Patty_relations pattyindex = new Patty_relations();
	static DBOIndex dboindex = new DBOIndex();
	
	public static void main(String[] args) {
		List<HAWKQuestion> questions = getQALDQuestions();
		List<List<HashSet<String>>> questionsWithSetsToCompare = new ArrayList<List<HashSet<String>>>();
		for(HAWKQuestion x : questions){
			 List<HashSet<String>> setsToCompare = getSets(x);
			questionsWithSetsToCompare.add(setsToCompare);
		}
		/*
		 * questionsWithSetsToCompare (...) is of the form [[q1, sparql, patty, dbo], [q2, sparql, patty, dbo], ...]
		 */
		for(List<HashSet<String>> x : questionsWithSetsToCompare){
			String question = x.get(0).toString();
			HashSet<String> relevantDocs = x.get(1);
			//Patty Data
			HashSet<String> pattyRetrievedDocs = x.get(2);
			HashSet<String> pattyIntersection = new HashSet<String>(x.get(1));
			pattyIntersection.retainAll(x.get(2));
			double pattyPrecision = pattyIntersection.size() / (double) pattyRetrievedDocs.size();
			double pattyRecall = pattyIntersection.size()/ ( double )relevantDocs.size();
			//DBO Data
			HashSet<String> dboRetrievedDocs = x.get(3);
			HashSet<String> dboIntersection = new HashSet<String>(x.get(1));
			pattyIntersection.retainAll(x.get(3));
			double dboPrecision = dboIntersection.size() / (double) dboRetrievedDocs.size();
			double dboRecall = dboIntersection.size()/ ( double )relevantDocs.size();
			
			System.out.println(question + " relevant URIs: " + relevantDocs.toString());
			System.out.println("\nPattyRelations" + "\nprecision: " + pattyPrecision + "\nrecall: " + pattyRecall);
			System.out.println("\nDBOIndex" + "\nprecision: " + dboPrecision + "\nrecall: " + dboRecall   + "\n");

		}
		
	}
/*
 * Get Questions from QALD6-Data
 */
	private static List<HAWKQuestion> getQALDQuestions(){
		log.info("Loading dataset");
		URL url = ClassLoader.getSystemClassLoader().getResource("QALD-6/qald-6-train-multilingual.json");
		System.out.println(url);
		List<HAWKQuestion> questions = null;
		try {
			questions = HAWKQuestionFactory.createInstances(QALD_Loader.loadJSON(url.openStream()));
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(0);
		}
		return questions;
	}
/*
 * Method to turn a natural-language question to list of uri's.
 */
	private static HashSet<String> getURISet(String question, String index){
		HashSet<String> result = new HashSet<String>();
		for(String term : question.split(" ")){
			if(index.equals("pattyindex")){
			result.addAll(pattyindex.search(term));}
			else if(index.equals("dboindex")){
			result.addAll(dboindex.search(term));	
			}
			//... else if.. usw
		}
		return result;
	}
/*
 * Method to get necessary SparqlEntities from GoldQueries provided in QALD Data.
 */
	private static HashSet<String> getSparql(HAWKQuestion q){
		String sparqlQuerystring = q.getSparqlQuery();
		if(sparqlQuerystring == null){
			return Sets.newHashSet();
		}
		else{
		Query sparqlQuery = QueryFactory.create(sparqlQuerystring); 
		HashSet<String> predicates = new HashSet<String>();
		ElementVisitorBase ELB = new ElementVisitorBase(){
				public void visit(ElementPathBlock el){
					Iterator<TriplePath> triples = el.patternElts();
					while(triples.hasNext()) {
						String candidate = triples.next().getPredicate().toString();
						if(candidate.contains("http://dbpedia.org/ontology/")){
							predicates.add(candidate);
						}
					}
				}};
		ElementWalker.walk(sparqlQuery.getQueryPattern(), ELB);
		return predicates;
		}
	}
/*
 * Method to obtain a tuple of [question, necessary Entities, some Sets of obtained Entities]
 */
	private static List<HashSet<String>> getSets(HAWKQuestion q){
		String question = (q.getLanguageToQuestion().get("en").replaceAll("\\p{P}", ""));
		HashSet<String> uriBucketPatty = getURISet(question, "pattyindex");
		HashSet<String> uriBucketDBO = getURISet(question, "dboindex");
		HashSet<String> uriSparql = getSparql(q);
		List<HashSet<String>> comparison = new ArrayList<HashSet<String>>();  
		comparison.add(Sets.newHashSet(question));
		comparison.add(uriSparql);
		comparison.add(uriBucketPatty);
		comparison.add(uriBucketDBO);
		return comparison;
	}
}

