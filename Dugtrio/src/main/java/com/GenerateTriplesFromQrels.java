package com;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.HashMap;

public class GenerateTriplesFromQrels {
	
	public void generateTriples(String pageParaFile, String pageTopFile, String paraLabelFile) {
		try {
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
		} catch (ClassNotFoundException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
