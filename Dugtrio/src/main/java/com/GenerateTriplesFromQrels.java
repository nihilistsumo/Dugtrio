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
import java.util.Collections;
import java.util.HashMap;

public class GenerateTriplesFromQrels {
	
	public void generateTriples(HashMap<String, ArrayList<String>> pageParas, HashMap<String, ArrayList<String>> pageTops, HashMap<String, HashMap<String, Integer>> labels, String outputFile){
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(new File(outputFile)));
			for(String page:pageParas.keySet()) {
				HashMap<String, Integer> paraLabels = labels.get(page);
				for(int i=-1; i<=Collections.max(paraLabels.values()); i++) {
					ArrayList<Integer> similar = new ArrayList<Integer>();
					ArrayList<Integer> dissimilar = new ArrayList<Integer>();
					for(int p=0; p<pageParas.get(page).size(); p++) {
						if(paraLabels.get(pageParas.get(page).get(p)) == i)
							similar.add(p);
						else
							dissimilar.add(p);
					}
					if(similar.size()>1 && dissimilar.size()>1) {
						for(int a=0; a<similar.size()-1; a++) {
							for(int b=a+1; b<similar.size(); b++) {
								for(int c=0; c<dissimilar.size(); c++) {
									bw.write(page+" "+similar.get(a)+" "+similar.get(b)+" "+dissimilar.get(c)+" 0\n");
								}
							}
						}
					}
				}
			}
			bw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		try {
			String pageParaFile = "/home/sumanta/Documents/Dugtrio-data/AttnetionWindowData/pageParasData";
			String pageTopFile = "/home/sumanta/Documents/Dugtrio-data/AttnetionWindowData/pageTopsData";
			String paraLabelFile = "/home/sumanta/Documents/Dugtrio-data/AttnetionWindowData/pageParaLabelData";
			String triplesOutputFile = "/home/sumanta/Documents/Dugtrio-data/AttnetionWindowData/by1-train-nodup.triples";
			FileInputStream fis = new FileInputStream(paraLabelFile);
			ObjectInputStream ois = new ObjectInputStream(fis);
			HashMap<String, HashMap<String, Integer>> labels = (HashMap<String, HashMap<String, Integer>>) ois.readObject();
			ois.close();
			fis.close();
			FileInputStream fis2 = new FileInputStream(pageParaFile);
			ObjectInputStream ois2 = new ObjectInputStream(fis2);
			HashMap<String, ArrayList<String>> pageParas = (HashMap<String, ArrayList<String>>) ois2.readObject();
			ois2.close();
			fis2.close();
			FileInputStream fis3 = new FileInputStream(pageTopFile);
			ObjectInputStream ois3 = new ObjectInputStream(fis3);
			HashMap<String, ArrayList<String>> pageTops = (HashMap<String, ArrayList<String>>) ois3.readObject();
			ois3.close();
			fis3.close();
			GenerateTriplesFromQrels gen = new GenerateTriplesFromQrels();
			gen.generateTriples(pageParas, pageTops, labels, triplesOutputFile);
			
			/*String page = "enwiki:Polyelectrolyte";
			BufferedReader br = new BufferedReader(new FileReader(new File("/home/sumanta/Documents/Dugtrio-data/AttnetionWindowData/temp")));
			String line = br.readLine();
			while(line!=null) {
				System.out.println(pageParas.get(page).get(Integer.parseInt(line.split(" ")[1]))
						+" "+pageParas.get(page).get(Integer.parseInt(line.split(" ")[2]))
						+" "+pageParas.get(page).get(Integer.parseInt(line.split(" ")[3])));
				line = br.readLine();
			}
			br.close();*/
		} catch (ClassNotFoundException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
