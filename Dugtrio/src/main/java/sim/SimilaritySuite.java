package sim;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.stream.StreamSupport;

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

public class SimilaritySuite {
	
	/*public String findOddLuceneSim(String[] tripleID, String[] tripleText, int[] triplesDocIDs, IndexSearcher is, String analyzer) throws ParseException, IOException {
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
	
	public void calcParaPairSimScore(String pageParaFile, String indexDirPath, String method, String outputFile) {
		try {
			FileInputStream fis2 = new FileInputStream(pageParaFile);
			ObjectInputStream ois2 = new ObjectInputStream(fis2);
			HashMap<String, ArrayList<String>> pageParas = (HashMap<String, ArrayList<String>>) ois2.readObject();
			ois2.close();
			fis2.close();
			
			HashMap<String, HashMap<String, Double>> pageParaPairScores = new HashMap<String, HashMap<String, Double>>();
			IndexSearcher is = new IndexSearcher(DirectoryReader.open(FSDirectory.open((new File(indexDirPath).toPath()))));
			
			if(method.equalsIgnoreCase("bm25")) {
				is.setSimilarity(new BM25Similarity());
				
				StreamSupport.stream(pageParas.keySet().spliterator(), true).forEach(page -> {
					try {
						System.out.println("PAGE: "+page+" started, "+pageParas.get(page).size()+" paras");
						BooleanQuery.setMaxClauseCount(65536);
						QueryParser qpID = new QueryParser("Id", new StandardAnalyzer());
						QueryParser qp = new QueryParser("Text", new StandardAnalyzer());
						ArrayList<String> paras = pageParas.get(page);
						for(int i=0; i<paras.size()-1; i++) {
							for(int j=i+1; j<paras.size(); j++) {
								double score = 
										Double.max(
												is.explain(qp.parse(QueryParser.escape(is.doc(is.search(qpID.parse(pageParas.get(page).get(i)), 1).scoreDocs[0].doc).get("Text"))), is.search(qpID.parse(pageParas.get(page).get(j)), 1).scoreDocs[0].doc).getValue(), 
												is.explain(qp.parse(QueryParser.escape(is.doc(is.search(qpID.parse(pageParas.get(page).get(j)), 1).scoreDocs[0].doc).get("Text"))), is.search(qpID.parse(pageParas.get(page).get(i)), 1).scoreDocs[0].doc).getValue());
								if(pageParaPairScores.keySet().contains(page))
									pageParaPairScores.get(page).put(i+" "+j, score);
								else {
									HashMap<String, Double> paraPairScores = new HashMap<String, Double>();
									paraPairScores.put(i+" "+j, score);
									pageParaPairScores.put(page, paraPairScores);
								}
							}
						}
						System.out.println("PAGE: "+page+" done");
					} catch (IOException | ParseException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				});
			}
			
			FileOutputStream fos = new FileOutputStream(new File(outputFile));
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			oos.writeObject(pageParaPairScores);
			oos.close();
			fos.close();
			
		} catch (IOException | ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		SimilaritySuite s = new SimilaritySuite();
		String pageParaFile = "/home/sumanta/Documents/Dugtrio-data/AttnetionWindowData/pageParasData";
		String indexDirPath = "/media/sumanta/Seagate Backup Plus Drive/indexes/paragraph-corpus-paragraph-index-with-aspvec/paragraph.lucene";
		String method = "bm25";
		String outputFile = "/home/sumanta/Documents/Dugtrio-data/AttnetionWindowData/bm25.parapair.train";
		s.calcParaPairSimScore(pageParaFile, indexDirPath, method, outputFile);
	}
	
//	public String findOddAsptext(String[] tripleID, String[] tripleText, int[] triplesDocIDs, IndexSearcher is, String analyzer) {
//		
//	}

}
