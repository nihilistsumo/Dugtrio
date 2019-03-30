package com;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.stream.StreamSupport;

import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.store.FSDirectory;
import org.json.simple.JSONObject;


public class OddOneOut {
	
	/*public String findOddOne(String[] tripleID, String[] tripleText, int[] triplesDocIDs, IndexSearcher is, String analyzer) throws ParseException, IOException {
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
		
		 * We take the max between sim(d1,d2) and sim(d2,d1) because: sim(d1,d2) != sim(d2,d1)
		 * This happens because d1 and d2 may have different doc length and there is high chance
		 * of getting higher similarity when using smaller document as query.
		 
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
	}*/
	
	public String findOddOne(HashMap<String, HashMap<String, Double>> pageParaPairScores, HashMap<String, ArrayList<String>> pageParas, String page, String p0, String p1, String p2) {
		String odd = "";
		
		double simScore01 = pageParaPairScores.get(page).get(Math.min(Integer.parseInt(p0), Integer.parseInt(p1))+" "+Math.max(Integer.parseInt(p0), Integer.parseInt(p1)));
		double simScore02 = pageParaPairScores.get(page).get(Math.min(Integer.parseInt(p0), Integer.parseInt(p2))+" "+Math.max(Integer.parseInt(p0), Integer.parseInt(p2)));
		double simScore12 = pageParaPairScores.get(page).get(Math.min(Integer.parseInt(p1), Integer.parseInt(p2))+" "+Math.max(Integer.parseInt(p1), Integer.parseInt(p2)));
		
		if(simScore01<Double.MIN_VALUE && simScore02<Double.MIN_VALUE && simScore12<Double.MIN_VALUE) {
//			System.out.println(page+" "+pageParas.get(page).get(Integer.parseInt(p0))+" "+pageParas.get(page).get(Integer.parseInt(p1))+" "+pageParas.get(page).get(Integer.parseInt(p2)));
			return odd;
		}
		if(simScore01 > simScore02) {
			if(simScore01 > simScore12)
				odd = p2;
			else
				odd = p0;
		} else {
			if(simScore02 > simScore12)
				odd = p1;
			else
				odd = p0;
		}
		return odd;
	}
	
	public void runTriplesLucene(String tripleFilePath, String pageParasFile, String paraPairScoreFile, String indexDirPath, String method, String analyzer, String outDir) {
		try {
			IndexSearcher is = new IndexSearcher(DirectoryReader.open(FSDirectory.open((new File(indexDirPath).toPath()))));
			if(method.equalsIgnoreCase("bm25"))
				is.setSimilarity(new BM25Similarity());
			else
				method = "default";
			
			BufferedReader br = new BufferedReader(new FileReader(new File(tripleFilePath)));
			
			FileInputStream fis = new FileInputStream(paraPairScoreFile);
			ObjectInputStream ois = new ObjectInputStream(fis);
			HashMap<String, HashMap<String, Double>> pageParaPairScores = (HashMap<String, HashMap<String, Double>>) ois.readObject();
			ois.close();
			fis.close();
			
			FileInputStream fis2 = new FileInputStream(pageParasFile);
			ObjectInputStream ois2 = new ObjectInputStream(fis2);
			HashMap<String, ArrayList<String>> pageParas = (HashMap<String, ArrayList<String>>) ois2.readObject();
			ois2.close();
			fis2.close();
			
			ArrayList<String> correctLines = new ArrayList<String>();
			BufferedWriter bwCorrect = new BufferedWriter(new FileWriter(new File(outDir+"/correct-"+method+"-"+analyzer)));
			ArrayList<String> incorrectLines = new ArrayList<String>();
			BufferedWriter bwIncorrect = new BufferedWriter(new FileWriter(new File(outDir+"/incorrect-"+method+"-"+analyzer)));
			
			HashMap<String, ArrayList<String>> lines = new HashMap<String, ArrayList<String>>();
			String l = br.readLine();
			while(l != null) {
				if(lines.keySet().contains(l.split(" ")[0]))
					lines.get(l.split(" ")[0]).add(l);
				else {
					ArrayList<String> pageLines = new ArrayList<String>();
					pageLines.add(l);
					lines.put(l.split(" ")[0], pageLines);
				}
				l = br.readLine();
			}
			int cnt=1;
			System.out.println("PAGE LINES-IN-TRIPLES ACCURACY% NOINFO%");
			for(String page:lines.keySet()) {
				int correct = 0;
				int incorrect = 0;
				int noInfo = 0;
				for(String line:lines.get(page)) {
					String[] elems = line.split(" ");
					String correctOdd = elems[3];
					String retrievedOdd = this.findOddOne(pageParaPairScores, pageParas, page, elems[1], elems[2], elems[3]);
					if(correctOdd.equals(retrievedOdd)) {
						correctLines.add(line);
						correct++;
					}
					else {
						incorrectLines.add(line+" "+retrievedOdd);
						incorrect++;
						if(retrievedOdd.length()==0)
							noInfo++;
					}
//					if((correctLines.size()+incorrectLines.size())%500==0)
//						System.out.print(".");
//					if((correctLines.size()+incorrectLines.size())%25000==0)
//						System.out.println("+");
				}
				System.out.println(cnt+" "+page+" "+lines.get(page).size()+" "+100.0*correct/(correct+incorrect)+" "+100.0*noInfo/lines.get(page).size());
				cnt++;
			}
			for(String c:correctLines)
				bwCorrect.write(c+"\n");
			for(String ic:incorrectLines)
				bwIncorrect.write(ic+"\n");
			br.close();
			bwCorrect.close();
			bwIncorrect.close();
			System.out.println("\nCorrect: "+correctLines.size());
			System.out.println("Incorrect: "+incorrectLines.size());
			System.out.println("Total: "+(correctLines.size()+incorrectLines.size()));
			System.out.println("Accuracy of "+method+": "+(100.0*(double)correctLines.size()/(correctLines.size()+incorrectLines.size()))+"%");
		} catch (IOException | ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		OddOneOut oddOne = new OddOneOut();
		if(args[0].equalsIgnoreCase("run")) {
			if(args.length!=8)
				System.out.println("Usage: java -jar [target/jar-with-dependencies] run triples-file page-paras-file parapair-score-file index-dir method analyzer output-dir");
			else
				oddOne.runTriplesLucene(args[1], args[2], args[3], args[4], args[5], args[6], args[7]);
		}
		else if(args[0].equalsIgnoreCase("vec")) {
			if(args.length!=7) {
				System.out.println("Usage: java -jar [target/jar-with-dependencies] vec triples-file index-dir asp-index-dir analyzer vector-length output-dir");
			}
			else {
				AspectVecGeneratorOfTriples asp = new AspectVecGeneratorOfTriples();
				asp.getAspvecOfParasFromTriples(args[1], args[2], args[3], args[4], args[5], args[6]);
			}
		}

	}

}
