package jp.ac.tsukuba.conclave.cytometry.data;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

/**
 * Series of static methods that create variations of RealLabelledData
 * 
 * TODO: Send error messages to a Logger object
 * 
 * @author Claus Aranha (caranha@cs.tsukuba.ac.jp)
 */


public class RealLabelledDataFactory {

	/* Creates a new data reader containing a subset of the attributes of this one. 
	 * The indexes of the attributes to be added are listed in the parameter "index" */
	/**
	 * Selects a subset of the data set, based on a list of indexes.
	 * 
	 * @param index: the indexes that needed to be selected
	 * @param base: the data from which we will perform the copy
	 * 
	 * @return a RealLabelledData with only the selected indexes
	 */
	static public RealLabelledData selectLabels(RealLabelledData base, int label[])
	{
		
		RealLabelledData ret = new RealLabelledData();
		for (ObservationReal aux: base)
		{
			for (int i = 0; i < label.length; i++)
				if (aux.getLabel() == label[i])
				{
					ret.addObservation(aux);
					break;
				}
		}		
		return ret;
	}
	
	/**
	 * Selects a subset of the data where only the attributes indicated in the 
	 * parameter are included. Null is returned if the attributes are invalid.
	 * 
	 * @param base The original data set
	 * @param attributes The attributes to select from the original data set.
	 * @return
	 */
	static public RealLabelledData selectAttributes(RealLabelledData base, int attributes[])
	{
		RealLabelledData ret = new RealLabelledData();
		for (ObservationReal aux: base)
		{
			double[] values = new double[attributes.length];
			for (int i = 0; i < attributes.length; i++)
				values[i] = aux.getAttribute(attributes[i]);
			ret.addObservation(new ObservationReal(aux.getLabel(),values));
		}
		return ret;
	}
	
	
	
	/**
	 * Creates partitions of this data collection based on the labels contained
	 * in it.
	 * 
	 * @param base Data collection to be partitioned
	 * @return Array of data collections, each contain all observations of a single label.
	 */
	static public RealLabelledData[] partitionLabels(RealLabelledData base)
	{
		RealLabelledData[] ret;
		int[] labels = base.getLabelList();
		ret = new RealLabelledData[labels.length];
		
		for (int i = 0; i < ret.length; i++)
		{
			ret[i] = new RealLabelledData();
		}
		
		for(ObservationReal aux: base)
		{
			for (int i = 0; i < ret.length; i++)
			{
				if (aux.getLabel() == labels[i])
					ret[i].addObservation(aux);
			}
		}
		
		return ret;
	}
	
	/**
	 * Creates two partitions from a Data List, sampling without repetition
	 * 
	 * @param base The base dataset which will be partitioned
	 * @param n The number of elements in the first partition (must be < base.size)
	 * @param seed A random number generation (null for new random)
	 * @return Two data lists: the first one with n elements, the second with base.size-n elements.
	 */
	static public RealLabelledData[] twoPartition(RealLabelledData base, int n, Random seed)
	{
		if (n > base.size())
		{
			System.err.println("[RealLabelledDataFactory] Warning: n greater than number of observations when partitioning data.");
			return null; 
		}

		RealLabelledData[] ret = new RealLabelledData[2];
		ret[0] = new RealLabelledData();
		ret[1] = new RealLabelledData();
		
		ArrayList<ObservationReal> bag = base.getArrayList();
		if (seed == null)
			seed = new Random();
		
		Collections.shuffle(bag,seed);

		int i = 0;
		for (ObservationReal aux: bag)
		{
			if (i < n)
				ret[0].addObservation(aux);
			else
				ret[1].addObservation(aux);
			i++;
		}		
		return ret;
	}
	
	
	/**
	 * 
	 * Creates "num" random partitions from "base" of equal-ish size.
	 * 
	 * @param base The base data set from which the random partitions will be created.
	 * @param num The number of random partitions (must be smaller than the size)
	 * @param seed The RNG (use null for a brand new RNG)
	 * @return an array with all the data partitions
	 */
	public static RealLabelledData[] randomPartition(RealLabelledData base, int num, Random seed)
	{
		if (num > base.size())
		{
			System.err.println("[RealLabelledDataFactory] Warning: number of partitions greater than number of observations when partitioning data.");
			return null; 
		}
				
		RealLabelledData[] ret = new RealLabelledData[num];
		for (int i = 0; i < num; i++)
			ret[i] = new RealLabelledData();
		
		ArrayList<ObservationReal> bag = base.getArrayList();
		if (seed == null)
			seed = new Random();		
		Collections.shuffle(bag,seed);
		
		int i = 0;
		for (ObservationReal aux: bag)
		{
			ret[i].addObservation(aux);
			i = (i+1)%ret.length;
		}		
		
		return ret;
	}
	
	
	

	/**
	 * Reads the data from a text file, and store it on a RealLabelledData collection.
	 * 
	 * @param filename Full-path filename.
	 * @param forcelabel If this is -1, labels are assumed to be in the first column. Else, all observations receive this label.
	 * @param split expression that separates values in a row. If now, we use " +" (variable number of spaces)
	 * @return a RealLabelledData object with this data. Or null on failure.
	 */
	static public RealLabelledData dataFromTextFile(String filename, int forcelabel, String split)
	{
		String line = "";
		RealLabelledData ret = new RealLabelledData();		
		BufferedReader reader = null;

		
		if (split == null)
			split = " +"; // if we don't get split information, we assume separated by spaces
		
		try
		{	
			reader = new BufferedReader(new FileReader(new File(filename)));
		}
		catch (Exception e)
		{
			System.err.print("[RealLabelledDataFactory] Error loading file: " + e);
			return null;
		}

		try 
		{
			while ((line = reader.readLine()) != null)
			{
				ObservationReal dt; // Zeratul
				int c; 
				double[] dd;
				
				String[] input = line.split(split);
				
				if (forcelabel == -1) // first element is the class
				{
					dd = new double[input.length - 1];
					for (int i = 0; i < dd.length; i++)
						dd[i] = Double.parseDouble(input[i+1]);
					c = (int)Math.round(Double.parseDouble(input[0]));
				}
				else
				{
					dd = new double[input.length];
					for (int i = 0; i < dd.length; i++)
						dd[i] = Double.parseDouble(input[i]);
					c = forcelabel;
				}
				
				dt = new ObservationReal(c,dd);
				ret.addObservation(dt);
			}
		}
		catch (Exception e)
		{
			System.err.println("[RealLabelledDataFactory] Error reading file (" + filename + "):" + e.getMessage());
			System.err.println("I could not read this line: "+line);
			System.exit(1);
		}
		
		return ret;
	}
	
}
