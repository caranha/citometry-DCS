package jp.ac.tsukuba.conclave.cytometry.toolbox;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;

import jp.ac.tsukuba.conclave.cytometry.data.ObservationReal;
import jp.ac.tsukuba.conclave.cytometry.data.RealLabelledData;
import jp.ac.tsukuba.conclave.cytometry.data.RealLabelledDataFactory;
import jp.ac.tsukuba.conclave.cytometry.divmeasure.Dcs;
import jp.ac.tsukuba.conclave.cytometry.projection.LinProj;
import jp.ac.tsukuba.cs.conclave.utils.Parameter;


public class TestResult {

	/**
	 * This method takes a dataset, and calculates the best n attributes of that data set, according to the separability measure
	 * returns an int vector with the best attribute indexes in order
	 * 
	 * @param data Data to be analyzed
	 * @param n number of attributes to be selected
	 * @return
	 */
	static public int[] bestAttributes(RealLabelledData data, int n, double kerneldivisor)
	{
		if (n > data.getTotalAttributes())
		{
			System.err.println("bestAttributes error: Tried to select more attributes than available!");
			System.exit(0);
		}
			
		double attrib_index[] = new double[data.getTotalAttributes()];
		int attrib_select[] = new int[n];
		for (int i = 0; i < n; i++)
			attrib_select[i] = -1;
		
		/* Selecting the n best attributes */
		/* selection sort is used. This is inefficient */
		for (int i=0; i < data.getTotalAttributes(); i++)
		{
			LinProj L = new LinProj(data.getTotalAttributes());
			L.w[i] = 1;
			
			double[] x = new double[data.size()];
			int[] y = new int[data.size()];
    	
    		Iterator<ObservationReal> it = data.iterator();
    		int jj = 0;
    		while (it.hasNext())
    		{
    			ObservationReal u = it.next();
    			x[jj] = L.project(u.getAttributeValues());
    			y[jj] = u.getLabel();
    			jj++;
    		}
    		attrib_index[i] = Dcs.calculate(x, y, kerneldivisor);
    		
    		/* placing the new attribute in the "best" list */
    		int index = i;
    		for (jj = 0; jj < n; jj++)
    		{
    			if (attrib_select[jj] == -1)
    			{
    				attrib_select[jj] = index;
    				break;
    			}
    			if (attrib_index[index] > attrib_index[attrib_select[jj]])
    			{
    				int tmp = attrib_select[jj];
    				attrib_select[jj] = index;
    				index = tmp;
    			}
    		}
    		
		}
				
		return attrib_select;
	}
	
	
	/**
	 * This method generate a set of files, one for each cluster, with the projected points 
	 * for those clusters. Each file is named <param>projdata_c<x><opt>, where <param> is a parameter
	 * from the parameter file ("file_prefix"), <opt> is a function parameter (for 
	 * separating base data and generalization data, for instance) and <x> is the cluster number
	 * 
	 * @param data
	 * @param P1
	 * @param P2
	 * @return 
	 */
	static public void dumpProjection(RealLabelledData data, LinProj P1, LinProj P2, String opt, Parameter P)
	{
		// First Step: Separating the clusters, and transforming for the second dimension
		RealLabelledData d1[] = RealLabelledDataFactory.partitionLabels(data);
		RealLabelledData d2[] = RealLabelledDataFactory.partitionLabels(data);
		
		String prefix = P.getParameter("file_prefix","");
		String posfix = opt;
		if (prefix == null)
			prefix = "";
		if (posfix == null)
			posfix = "";
		
		for (int i = 0; i < d2.length; i++)
			if (d1[i].size() > 0)
			{				
				d2[i].ortogonalProjection(P1);
				Iterator<ObservationReal> it1 = d1[i].iterator();
				Iterator<ObservationReal> it2 = d2[i].iterator();
		    	try
		    	{
		    		BufferedWriter outfile = new BufferedWriter(new FileWriter(prefix+"projdata" + posfix+ ".c"+i));	
		    		while (it1.hasNext())
		    			outfile.write(P1.project(it1.next().getAttributeValues())+" "+P2.project(it2.next().getAttributeValues())+"\n"); //writing projectd data
		    		outfile.close();
				}	
		    	catch(IOException e)
		    	{
		    		System.err.print(e.getMessage());
		    	}

			}
		
	}
	
	static public boolean[] generalizeTest(RealLabelledData data, RealLabelledData test, LinProj P1, LinProj P2)
	{
		boolean[] ret = new boolean[test.size()];
		// Create Projections, separated by cluster
		RealLabelledData d1[] = RealLabelledDataFactory.partitionLabels(data);
		RealLabelledData d2[] = RealLabelledDataFactory.partitionLabels(data);
		Double m1[] = new Double[d1.length];
		Double m2[] = new Double[d2.length];

		// Calculate median of each projection		
		for (int i = 0; i < d2.length; i++)
			if (d1[i].size() > 0)
			{				
				d2[i].ortogonalProjection(P1);
				Iterator<ObservationReal> it1 = d1[i].iterator();
				Iterator<ObservationReal> it2 = d2[i].iterator();
				double[] x1 = new double[d1[i].size()];
				double[] x2 = new double[d2[i].size()];
				int j = 0;
		    	while (it1.hasNext())
		    	{
		    		x1[j] = P1.project(it1.next().getAttributeValues());
		    		x2[j] = P2.project(it2.next().getAttributeValues());
		    		j++;
		    	}
		    	m1[i] = Maths.median(x1);
		    	m2[i] = Maths.median(x2);
			}

		// For each test data, calculate distance from all medians
		Iterator<ObservationReal> itt1 = test.iterator();
		Iterator<ObservationReal> itt2 = test.ortogonalProjection(P1).iterator();
		
		int i = 0;
		while(itt1.hasNext())
		{
			ObservationReal t1 = itt1.next();
			ObservationReal t2 = itt2.next();
			
			double mindist = Double.MAX_VALUE;
			int minindex = -1;

			double x1 = P1.project(t1.getAttributeValues());
			double x2 = P2.project(t2.getAttributeValues());
			
			// For each test data, calculate distance from all medians
			for (int j = 0; j < d1.length; j++)
				if (d1[j].size() > 0) // cluster exists
				{
					double dist = Math.pow(m1[j] - x1,2) + Math.pow(m2[j] - x2, 2);
					if (dist < mindist)
					{
						mindist = dist; // current minimum distance
						minindex = j; // current cluster for minimum distance
					}
				}
			
			// if distance is closest to true cluster, set ret[i] to TRUE, else FALSE
			if (!(data.size(minindex) > 0))
			{				
				System.err.println("Error: closest cluster does not exist: cluster index = " + minindex);
				System.exit(1);
			}
			
			ret[i] = (minindex == t1.getLabel()); // is the closest cluster the right one?			
			i++;
		}
		
		
		return ret;
	}
	
}
