package com;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
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
import org.apache.lucene.store.FSDirectory;
import org.json.simple.JSONObject;

public class AspectVecGeneratorOfTriples {
	
	public SparseAspectVector getAspectVec(String paraText, IndexSearcher aspectIs, String analyzer, int vecLen) throws IOException, ParseException {
		QueryParser qp;
		if(analyzer.equalsIgnoreCase("eng"))
			qp = new QueryParser("Text", new EnglishAnalyzer());
		else
			qp = new QueryParser("Text", new StandardAnalyzer());
		BooleanQuery.setMaxClauseCount(65536);
		Query q = qp.parse(QueryParser.escape(paraText));
		TopDocs retAspectsPara = aspectIs.search(q, vecLen);
		SparseAspectVector aspVec = new SparseAspectVector(vecLen);
		int i=0;
		// aspVec arrays will have the aspect rank scores sorted; aspVec.values[0] will have the highest rank score
		for(ScoreDoc asp:retAspectsPara.scoreDocs) {
			aspVec.set(i, asp.doc, asp.score);
			i++;
		}
		aspVec.normalize();
		return aspVec;
	}
	
	public void getAspvecOfParasFromTriples(String tripleFilePath, String indexDirPath, String aspIndexDirPath, String analyzer, String vectorLen, String outDir) {
		try {
			int vecLen = Integer.parseInt(vectorLen);
			IndexSearcher is = new IndexSearcher(DirectoryReader.open(FSDirectory.open((new File(indexDirPath).toPath()))));
			IndexSearcher isAsp = new IndexSearcher(DirectoryReader.open(FSDirectory.open((new File(aspIndexDirPath).toPath()))));
			BufferedReader br = new BufferedReader(new FileReader(new File(tripleFilePath)));
			HashSet<String> paras = new HashSet<String>();
			String line = br.readLine();
			while(line != null) {
				paras.add(line.split(" ")[1]);
				paras.add(line.split(" ")[2]);
				paras.add(line.split(" ")[3]);
				line = br.readLine();
			}
			br.close();
			HashMap<String, SparseAspectVector> paraAspVecMap = new HashMap<String, SparseAspectVector>();
			StreamSupport.stream(paras.spliterator(), false).forEach(paraID -> {
				try {
					QueryParser qpID = new QueryParser("Id", new StandardAnalyzer());
					String paraText = is.doc(is.search(qpID.parse(paraID), 1).scoreDocs[0].doc).get("Text");
					SparseAspectVector aspVec = this.getAspectVec(paraText, isAsp, analyzer, vecLen);
					paraAspVecMap.put(paraID, aspVec);
					System.out.print(".");
				} catch (IOException | ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			});
			JSONObject paraVecMap = new JSONObject();
			for(String paraID:paraAspVecMap.keySet()) {
				SparseAspectVector aspVec = paraAspVecMap.get(paraID);
				JSONObject vec = new JSONObject();
				for(int i=0; i<vecLen; i++)
					vec.put("asp:"+aspVec.getAspDocID(i), aspVec.get(i));
				paraVecMap.put(paraID, vec);
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		AspectVecGeneratorOfTriples gen = new AspectVecGeneratorOfTriples();
		gen.getAspvecOfParasFromTriples(args[0], args[1], args[2], args[3], args[4], args[5]);

	}

}
