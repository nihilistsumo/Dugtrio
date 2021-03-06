package com;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Constants;

public class DatasetExplorer {
	
	public HashMap<String, ArrayList<String>> getPageParas(String artQrelsFile) {
		HashMap<String, ArrayList<String>> pageParas = new HashMap<String, ArrayList<String>>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(new File(artQrelsFile)));
			String line = br.readLine();
			while(line != null) {
				String page = line.split(" ")[0];
				String para = line.split(" ")[2];
				if(pageParas.keySet().contains(page)) {
					pageParas.get(page).add(para);
				}
				else {
					ArrayList<String> paraList = new ArrayList<String>();
					paraList.add(para);
					pageParas.put(page, paraList);
				}
				line = br.readLine();
			}
			br.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return pageParas;
	}
	
	public HashMap<String, ArrayList<String>> getPageTopSections(String topicsFile) {
		HashMap<String, ArrayList<String>> pageTopSections = new HashMap<String, ArrayList<String>>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(new File(topicsFile)));
			String line = br.readLine();
			while(line != null) {
				if(line.length() > 0) {
					String page = line.split("/")[0];
					String top = line.split("/")[1];
					if(pageTopSections.keySet().contains(page)) {
						if(!pageTopSections.get(page).contains(top))
							pageTopSections.get(page).add(top);
					}
					else {
						ArrayList<String> topList = new ArrayList<String>();
						topList.add(top);
						pageTopSections.put(page, topList);
					}
				}
				line = br.readLine();
			}
			br.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return pageTopSections;
	}
	
	public HashMap<String, HashMap<String, Integer>> getPageParaTopsecLabelMap(HashMap<String, ArrayList<String>> pageParas, HashMap<String, ArrayList<String>> pageTops, String topQrelsFile) {
		HashMap<String, HashMap<String, Integer>> pageParaLabels = new HashMap<String, HashMap<String, Integer>>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(new File(topQrelsFile)));
			String line = br.readLine();
			while(line != null) {
				String page = line.split(" ")[0].split("/")[0];
				String para = line.split(" ")[2];
				String top = line.split(" ")[0].contains("/") ? line.split(" ")[0].split("/")[1] : "";
				int label = 0;
				if(top.length() == 0)
					label = -1;
				else
					label = pageTops.get(page).indexOf(top);
				if(!pageParaLabels.keySet().contains(page)) {
					HashMap<String, Integer> paraLables = new HashMap<String, Integer>();
					paraLables.put(para, label);
					pageParaLabels.put(page, paraLables);
				}
				else {
					pageParaLabels.get(page).put(para, label);
				}
				line = br.readLine();
			}
			br.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return pageParaLabels;
	}
	
	public ArrayList<String> getTokensFromTextUsingLucene(String input, Analyzer analyzer) throws IOException{
	    ArrayList<String> tokens = new ArrayList<String>();
	    TokenStream tokenStream = analyzer.tokenStream("text", input);
	    CharTermAttribute attr = tokenStream.addAttribute(CharTermAttribute.class);
	    tokenStream.reset();
	    while(tokenStream.incrementToken())
	       tokens.add(attr.toString());
	    tokenStream.close();
	    return tokens;
	}
	
	public ArrayList<Integer> mergeWindows(ArrayList<Integer> start, ArrayList<Integer> end) {
		Collections.sort(start);
		Collections.sort(end);
		
		ArrayList<Integer> result = new ArrayList<Integer>();
		int i = 0;
		int j = 1;
		while(j-1 < start.size()) {
			while(j < start.size() && start.get(j) <= end.get(j-1))
				j++;
			result.add(start.get(i));
			result.add(end.get(j-1));
			i = j;
			j++;
		}
		return result;
	}
	
	// Try to get one string representations for each paragraph.
	// Use each top section heading to open up windows into the
	// para text. Get overlapping windows to merge to a single window.
	
	public ArrayList<String> getParaRepresentations(ArrayList<String> paras, ArrayList<String> secLabels, int windowArmSize, IndexSearcher is, Analyzer analyzer) {
		ArrayList<String> paraReps = new ArrayList<String>();
		try {
			QueryParser qpID = new QueryParser("Id", new StandardAnalyzer());
			int c = 1;
			for(String para:paras) {
//				System.out.println("PARA: "+para);
				ArrayList<String> paraTextTokens = new ArrayList<String>(Arrays.asList(is.doc(is.search(qpID.parse(para), 1).scoreDocs[0].doc).get("Text").split(" ")));
				String paraRep = "";
				ArrayList<Integer> windowStartIndices = new ArrayList<Integer>();
				ArrayList<Integer> windowEndIndices = new ArrayList<Integer>();
				for(String sec:secLabels) {
//					System.out.println("SEC: "+sec);
					String paraRepForSec = "";
					ArrayList<String> secLabelTokens = this.getTokensFromTextUsingLucene(sec.replaceAll("%20", " "), analyzer);
					
					for(String secToken:secLabelTokens) {
						String paraToken = "";
						for(int i=0; i<paraTextTokens.size(); i++) {
							paraToken = paraTextTokens.get(i);
							if(secToken.equalsIgnoreCase(paraToken)) {
								windowStartIndices.add(Math.max(0, i-windowArmSize));
								windowEndIndices.add(Math.min(paraTextTokens.size()-1, i+windowArmSize));
							}
						}
					}
					c++;
				}
				ArrayList<Integer> windows = this.mergeWindows(windowStartIndices, windowEndIndices);
				for(int w=0; w<windows.size()/2; w++) {
					for(String windowToken:paraTextTokens.subList(windows.get(2*w), windows.get(2*w+1)+1))
						paraRep += windowToken.toLowerCase()+" ";
				}
				paraReps.add(paraRep.trim());
			}
		} catch (IOException | ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return paraReps;
	}

	/*public static void main(String[] args) {
		// TODO Auto-generated method stub
		try {
			DatasetExplorer exp = new DatasetExplorer();
			
			String artQrels = "/home/sumanta/Documents/trec_dataset/benchmarkY1/benchmarkY1-train-nodup/train.pages.cbor-article.qrels";
			String topQrels = "/home/sumanta/Documents/trec_dataset/benchmarkY1/benchmarkY1-train-nodup/train.pages.cbor-toplevel.qrels";
			String topicsFile = "/home/sumanta/Documents/trec_dataset/benchmarkY1/benchmarkY1-train-nodup/topics";
			String indexDirPath = "/media/sumanta/Seagate Backup Plus Drive/indexes/paragraph-corpus-paragraph-index/paragraph.lucene";
			String outputDirPath = "/home/sumanta/Documents/Dugtrio-data/AttnetionWindowData";
			String paraRepIndexDirPath = "/home/sumanta/Documents/Dugtrio-data/AttnetionWindowData/by1-train-nodup-window-paraRep.index";
			
			IndexSearcher is = new IndexSearcher(DirectoryReader.open(FSDirectory.open((new File(indexDirPath).toPath()))));
			HashMap<String, ArrayList<String>> pageParas = exp.getPageParas(artQrels);
			HashMap<String, ArrayList<String>> pageTops = exp.getPageTopSections(topicsFile);
			HashMap<String, HashMap<String, Integer>> pageParaLabels = exp.getPageParaTopsecLabelMap(pageParas, pageTops, topQrels);
			
			Directory indexdir = FSDirectory.open((new File(paraRepIndexDirPath)).toPath());
			IndexWriterConfig conf = new IndexWriterConfig(new StandardAnalyzer());
			conf.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
			IndexWriter iw = new IndexWriter(indexdir, conf);
			
			HashMap<String, ArrayList<String>> pageParaReps = new HashMap<String, ArrayList<String>>();
			for(String page:pageParas.keySet()) {
				System.out.println("PAGE: "+page);
				ArrayList<String> parasInPage = pageParas.get(page);
				ArrayList<String> paraRepInPage = exp.getParaRepresentations(parasInPage, pageTops.get(page), 3, is, new StandardAnalyzer());
				pageParaReps.put(page, paraRepInPage);
				for(int p=0; p<parasInPage.size(); p++) {
					Document paraDoc = new Document();
					paraDoc.add(new StringField("Id", parasInPage.get(p), Field.Store.YES));
					paraDoc.add(new TextField("Text", paraRepInPage.get(p), Field.Store.YES));
					iw.addDocument(paraDoc);
				}
//				for(int i=0; i<parasInPage.size(); i++) {
//					System.out.println(parasInPage.get(i));
//					for(int j=0; j<paraRepInPage.get(i).size(); j++)
//						if(paraRepInPage.get(i).get(j).length() > 0)
//							System.out.println(j+". "+paraRepInPage.get(i).get(j));
//						else
//							System.out.println(j+". BLANK");
//					System.out.println();
//				}
			}
			iw.commit();
			FileOutputStream fos = new FileOutputStream(new File(outputDirPath+"/pageParaRepsData"));
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			oos.writeObject(pageParaReps);
			oos.close();
			fos.close();
			
			FileOutputStream fos2 = new FileOutputStream(new File(outputDirPath+"/pageParasData"));
			ObjectOutputStream oos2 = new ObjectOutputStream(fos2);
			oos2.writeObject(pageParas);
			oos2.close();
			fos2.close();
			
			FileOutputStream fos3 = new FileOutputStream(new File(outputDirPath+"/pageTopsData"));
			ObjectOutputStream oos3 = new ObjectOutputStream(fos3);
			oos3.writeObject(pageTops);
			oos3.close();
			fos3.close();
			
			FileOutputStream fos4 = new FileOutputStream(new File(outputDirPath+"/pageParaLabelData"));
			ObjectOutputStream oos4 = new ObjectOutputStream(fos4);
			oos4.writeObject(pageParaLabels);
			oos4.close();
			fos4.close();
			
//			String page = "enwiki:Photosynthesis";
//			ArrayList<String> paraReps = exp.getParaRepresentations(pageParas.get(page), pageTops.get(page), 3, is, new StandardAnalyzer());
//			for(String paraRep:paraReps)
//				System.out.println(paraRep+"\n");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}*/
	
	public static void main(String[] args) {
		try {
			FileInputStream fis2 = new FileInputStream("/home/sumanta/Documents/Dugtrio-data/AttnetionWindowData/pageParaRepsData");
			ObjectInputStream ois2 = new ObjectInputStream(fis2);
			HashMap<String, ArrayList<String>> pageParaReps = (HashMap<String, ArrayList<String>>) ois2.readObject();
			ois2.close();
			fis2.close();
			int total = 0;
			int blank = 0;
			for(String page:pageParaReps.keySet()) {
				for(String para:pageParaReps.get(page)) {
					if(para.length()<1)
						blank++;
					total++;
				}
			}
			System.out.println("Out of "+total+" paras in by1train, "+blank+" are blank");
		} catch (ClassNotFoundException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
