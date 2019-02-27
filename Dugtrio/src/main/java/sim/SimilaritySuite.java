package sim;

import java.io.IOException;

import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;

public class SimilaritySuite {
	
	public String findOddLuceneSim(String[] tripleID, String[] tripleText, int[] triplesDocIDs, IndexSearcher is, String analyzer) throws ParseException, IOException {
		String odd = "";
		QueryParser qp;
		//QueryParser qpID = new QueryParser("Id", new EnglishAnalyzer());
		if(analyzer.equalsIgnoreCase("eng"))
			qp = new QueryParser("Text", new EnglishAnalyzer());
		else
			qp = new QueryParser("Text", new StandardAnalyzer());
		
		BooleanQuery.setMaxClauseCount(65536);
		Query q0 = qp.parse(QueryParser.escape(tripleText[0]));
		Query q1 = qp.parse(QueryParser.escape(tripleText[1]));
		Query q2 = qp.parse(QueryParser.escape(tripleText[2]));
		double simScore01 = is.explain(q0, triplesDocIDs[1]).getValue();
		double simScore10 = is.explain(q1, triplesDocIDs[0]).getValue();
		double simScore02 = is.explain(q0, triplesDocIDs[2]).getValue();
		double simScore20 = is.explain(q2, triplesDocIDs[0]).getValue();
		double simScore12 = is.explain(q1, triplesDocIDs[2]).getValue();
		double simScore21 = is.explain(q2, triplesDocIDs[1]).getValue();
		/*
		 * We take the max between sim(d1,d2) and sim(d2,d1) because: sim(d1,d2) != sim(d2,d1)
		 * This happens because d1 and d2 may have different doc length and there is high chance
		 * of getting higher similarity when using smaller document as query.
		 */
		simScore01 = Math.max(simScore01, simScore10);
		simScore02 = Math.max(simScore02, simScore20);
		simScore12 = Math.max(simScore12, simScore21);
		
		if(simScore01 > simScore02) {
			if(simScore01 > simScore12)
				odd = tripleID[2];
			else
				odd = tripleID[0];
		} else {
			if(simScore02 > simScore12)
				odd = tripleID[1];
			else
				odd = tripleID[0];
		}
		return odd;
	}
	
//	public String findOddAsptext(String[] tripleID, String[] tripleText, int[] triplesDocIDs, IndexSearcher is, String analyzer) {
//		
//	}

}
