package com;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
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
	
	public String findOddOne(String[] tripleID, String[] tripleText, int[] triplesDocIDs, IndexSearcher is, String analyzer) throws ParseException, IOException {
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
	
	public void runTriples(String tripleFilePath, String indexDirPath, String method, String analyzer, String outDir) {
		try {
			IndexSearcher is = new IndexSearcher(DirectoryReader.open(FSDirectory.open((new File(indexDirPath).toPath()))));
			if(method.equalsIgnoreCase("bm25"))
				is.setSimilarity(new BM25Similarity());
			else
				method = "default";
			QueryParser qpID = new QueryParser("Id", new StandardAnalyzer());
			BufferedReader br = new BufferedReader(new FileReader(new File(tripleFilePath)));
			BufferedWriter bwCorrect = new BufferedWriter(new FileWriter(new File(outDir+"/correct-"+method+"-"+analyzer)));
			BufferedWriter bwIncorrect = new BufferedWriter(new FileWriter(new File(outDir+"/incorrect-"+method+"-"+analyzer)));
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
				String correctOdd = paraIDs[2];
				String retrievedOdd = this.findOddOne(paraIDs, paraTexts, docIDs, is, analyzer);
				if(correctOdd.equals(retrievedOdd)) {
					hit++;
					bwCorrect.write(line+"\n");
				}
				else
					bwIncorrect.write(line+" "+retrievedOdd+"\n");
				count++;
				if(count%500==0) {
					System.out.print(".");
					countDot++;
					if(countDot%50==0)
						System.out.println("+");
				}
			}
			br.close();
			bwCorrect.close();
			bwIncorrect.close();
			System.out.println("\nCorrect: "+hit);
			System.out.println("Incorrect: "+(count-hit));
			System.out.println("Total: "+count);
			System.out.println("Accuracy of "+method+": "+(100.0*(double)hit/count)+"%");
		} catch (IOException | ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		OddOneOut oddOne = new OddOneOut();
		if(args.length!=5)
			System.out.println("Usage: java -jar [target/jar-with-dependencies] triples-file index-dir method analyzer output-dir");
		else
			oddOne.runTriples(args[0], args[1], args[2], args[3], args[4]);

	}

}
