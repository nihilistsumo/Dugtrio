package com;
public class SparseAspectVector {
	private int[] indexes; // stores aspect docid
	private float[] values; // asp doc ID -> asp rank score; indexes[i] -> values[i]
	private boolean isNormalized;

	public SparseAspectVector(int maxNonzeroLen) {
		this.indexes = new int[maxNonzeroLen];
		this.values = new float[maxNonzeroLen];
		this.isNormalized = false;
		for(int i=0; i<this.indexes.length; i++)
			this.indexes[i] = -1; // this means it's corresponding values entry is not valid
	}

	// gets ith element of vector value
	public float get(int i) {
		return this.values[i];
	}

	// gets ith 
	public int getAspDocID(int i) {
		return this.indexes[i];
	}

	public void set(int i, int aspDocID, float val) {
		this.indexes[i] = aspDocID;
		this.values[i] = val;
		if(this.isNormalized)
			this.isNormalized = false;
	}

	public void normalize() {
		float sumScore = 0;
		for(int i=0; i<this.values.length; i++)
			sumScore+=this.values[i];
		for(int i=0; i<this.values.length; i++)
			this.values[i]/=sumScore;
		this.isNormalized = true;
	}

	public boolean isNormalized() {
		return isNormalized;
	}
}