package com;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.store.FSDirectory;

public class OddOneOut {
	
	public String findOddOne(String[] tripleID, String[] tripleText, int[] triplesDocIDs, IndexSearcher is) throws ParseException, IOException {
		String odd = "";
		//QueryParser qpID = new QueryParser("Id", new EnglishAnalyzer());
		QueryParser qp = new QueryParser("Text", new EnglishAnalyzer());
		BooleanQuery.setMaxClauseCount(65536);
		Query q0 = qp.parse(QueryParser.escape(tripleText[0]));
		Query q1 = qp.parse(QueryParser.escape(tripleText[1]));
		Query q2 = qp.parse(QueryParser.escape(tripleText[2]));
		double simScore01 = is.explain(q0, triplesDocIDs[1]).getValue();
		double simScore02 = is.explain(q0, triplesDocIDs[2]).getValue();
		double simScore12 = is.explain(q1, triplesDocIDs[2]).getValue();
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
	
	public void runTriples(String tripleFilePath, String indexDirPath, String method) {
		try {
			IndexSearcher is = new IndexSearcher(DirectoryReader.open(FSDirectory.open((new File(indexDirPath).toPath()))));
			if(method.equalsIgnoreCase("bm25"))
				is.setSimilarity(new BM25Similarity());
			else
				method = "default";
			QueryParser qpID = new QueryParser("Id", new StandardAnalyzer());
			BufferedReader br = new BufferedReader(new FileReader(new File(tripleFilePath)));
			int countDot = 0;
			int hit = 0;
			int count = 0;
			ArrayList<String> lines = new ArrayList<String>();
			String l = br.readLine();
			while(l != null) {
				lines.add(l);
				l = br.readLine();
			}
			for(String line:lines) {
				String[] elems = line.split(" ");
				String[] paraIDs = new String[3];
				String[] paraTexts = new String[3];
				int[] docIDs = new int[3];
				paraIDs[0] = elems[1];
				paraIDs[1] = elems[2];
				paraIDs[2] = elems[3];
				for(int i=0; i<3; i++) {
					docIDs[i] = is.search(qpID.parse(paraIDs[i]), 1).scoreDocs[0].doc;
					paraTexts[i] = is.doc(docIDs[i]).get("Text");
				}
				String correctPara = paraIDs[2];
				if(correctPara.equals(this.findOddOne(paraIDs, paraTexts, docIDs, is)))
					hit++;
				count++;
				if(count%500==0) {
					System.out.print(".");
					countDot++;
					if(countDot%50==0)
						System.out.println("+");
				}
			}
			br.close();
			System.out.println("Accuracy of "+method+": "+(100.0*(double)hit/count)+"%");
		} catch (IOException | ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		OddOneOut oddOne = new OddOneOut();
		oddOne.runTriples(args[0], args[1], args[2]);

	}

}
