package util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.HashMap;

import com.google.gson.Gson;

public class JavaObjToJSON {
	
	public void convertToJSON(Object obj, String outputFile) {
		try {
			Gson gson = new Gson();
			String json = gson.toJson(obj);
			BufferedWriter bw = new BufferedWriter(new FileWriter(new File(outputFile)));
			bw.write(json);
			bw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		try {
			FileInputStream fis = new FileInputStream("/home/sumanta/Documents/Dugtrio-data/AttnetionWindowData/bm25.parapair.train");
			ObjectInputStream ois = new ObjectInputStream(fis);
			HashMap<String, HashMap<String, Double>> bm25Scores = (HashMap<String, HashMap<String, Double>>) ois.readObject();
			ois.close();
			fis.close();
			
			FileInputStream fis2 = new FileInputStream("/home/sumanta/Documents/Dugtrio-data/AttnetionWindowData/bm25-windowed.parapair.train");
			ObjectInputStream ois2 = new ObjectInputStream(fis2);
			HashMap<String, HashMap<String, Double>> windowedScores = (HashMap<String, HashMap<String, Double>>) ois2.readObject();
			ois2.close();
			fis2.close();
			
			JavaObjToJSON j = new JavaObjToJSON();
			j.convertToJSON(bm25Scores, "/home/sumanta/Documents/Dugtrio-data/AttnetionWindowData/bm25.parapair.train.json");
			j.convertToJSON(windowedScores, "/home/sumanta/Documents/Dugtrio-data/AttnetionWindowData/bm25-windowed.parapair.train.json");
		} catch (ClassNotFoundException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
