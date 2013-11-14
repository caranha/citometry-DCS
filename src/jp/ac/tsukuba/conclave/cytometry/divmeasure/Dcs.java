package jp.ac.tsukuba.conclave.cytometry.divmeasure;


import jp.ac.tsukuba.conclave.cytometry.toolbox.Maths;

/***
 * Calculates the Cauchy-Schwartz divergence measure for multiple clusters
 * @author claus.aranha
 *
 */

public class Dcs {

	
	
	/**
	 * Calculates the Dcs divergence for a number of values. This is the uni-dimensional Dcs.
	 * You need a number of values, with the label value for each element.
	 * 
	 * Parameters:
	 * One big array of doubles with all the data
	 * An integer array with the start index of each class
	 * 
	 * @return
	 * 
	 */
	//TODO: Optimize the shit out of this
	public static double calculate(double[] v, int[] l, double kerneldivisor)
	{
		return DCS(v,l,kerneldivisor);

		// TODO: Define how to parametrize the lateral penalty DCS
		// return lateralPenaltyDCS(v,l);
	}
		
	public static double DCS(double[] v, int[] l, double kerneldivisor)
	{
		if (v.length != l.length)
		{
			System.err.println("Dcs Error: Data array and label array are of different sizes!");
			return -1;
		}		
		/* We assume that label > 0. If label is zero, print an error message */
		// TODO: write exception handling for this error message

		int maxlabel = 0;
		for (int i = 0; i < l.length; i++)
			if (l[i] > maxlabel)
				maxlabel = l[i];
			
		// variance 0 is the data variance. Variance i is the variance of label i
		// These variables are too long, but I don't want to calculate maxlabel, and I have enough memory
		// I'm trusting Java to initialize these vectors to 0. It seems it does.
		double mean[] = new double[maxlabel+1];
		double variance[] = new double[maxlabel+1]; 
		int size[] = new int[maxlabel+1];
		double Gcalc[] = new double[maxlabel+1];		
		
		/* Two pass calculation of the variance, for each class, and for all the elements */
		/* Calculating the mean */
		for (int i = 0; i < v.length; i++)
		{
			if (l[i] > maxlabel)
				maxlabel = l[i]; // now I know the maximum label
			mean[0] += v[i];
			mean[l[i]] += v[i];
			size[0] ++;
			size[l[i]] ++;
		}
		for (int i = 0; i < maxlabel +1; i++)
			mean[i] = mean[i]/size[i];		
		
		/* Calculating the Variance - I'm not compensating for the sum, this might bring me problems */
		for (int i = 0; i < v.length; i++)
		{
			variance[0] += (v[i] - mean[0])*(v[i] - mean[0]);
			variance[l[i]] += (v[i] - mean[l[i]])*(v[i] - mean[l[i]]);
		}
		for (int i = 0; i < maxlabel+1; i++)
			if (size[i] > 1)
			{
				variance[i] = (variance[i]/(size[i] - 1))/kerneldivisor; // variance (kernel) is divided by a parameter
			}
			else 
				variance[i] = 0;
		
		// DEBUG
		//for(int i = 0; i < maxlabel + 1; i++)
		//	System.out.println(variance[i]);
		
		/* Calculating the sum of Gaussian function values */
		/* They are summed for the entire dataset (i == j) and for each cluster (x!=j) */
		
		for (int i = 0; i < v.length; i++)
			for (int j = 0; j < v.length; j++)
				if (l[i] == l[j]) // mesma classe (G da classe)
				{
					Gcalc[l[i]] += Maths.gaussian(v[i] - v[j], Math.sqrt(2*variance[l[i]]));
				}
				else // classes diferentes (G do grupo)
				{
					Gcalc[0] += Maths.gaussian(v[i] - v[j], Math.sqrt(variance[l[i]]+variance[l[j]]));
				}

		// DEBUG
		//for(int i = 0; i < maxlabel + 1; i++)
		//	System.out.println(Gcalc[i]);
		
		double H = 0;
		double CEF = 2;
		for (int i = 0; i < maxlabel; i++)
		{
			// H is consistent with Rodrigo's code
			if(size[i+1] > 0)
			{
				H += -1.0 * Math.log((1.0/(size[i+1]*size[i+1]))*Gcalc[i+1]); // Summing the entropies
				CEF *= size[i+1];
			}
		}
		
		// CEF is consistent with Rodrigo's program
		CEF = -2.0*Math.log(Gcalc[0]/CEF);

//		System.out.print(CEF+" "+H+" ");
		double ret = CEF - H;		
		
		return ret;
	}

	public static double lateralPenaltyDCS(double[] v, int[] l, double kerneldivisor)
	{
		
		if (v.length != l.length)
		{
			System.err.println("Dcs Error: Data array and label array are of different sizes!");
			return -1;
		}

		int maxlabel = 0;
		for (int i = 0; i < l.length; i++)
			if (l[i] > maxlabel)
				maxlabel = l[i];
			
		// variance 0 is the data variance. Variance i is the variance of label i
		// These variables are too long, but I don't want to calculate maxlabel, and I have enough memory
		// I'm trusting Java to initialize these vectors to 0. It seems it does.
		double mean[] = new double[maxlabel+1];
		double median[] = new double[maxlabel+1];
		double variance[] = new double[maxlabel+1]; 
		int size[] = new int[maxlabel+1];
		double Gcalc[] = new double[maxlabel+1];		
		

		
		/* Two pass calculation of the variance, for each class, and for all the elements */
		/* Calculating the mean */
		for (int i = 0; i < v.length; i++)
		{
			if (l[i] > maxlabel)
				maxlabel = l[i]; // now I know the maximum label
			if (l[i] == 0)
			{
				System.err.println("DCS.java: Error: Label value is 0!");
				System.exit(1);
			}
			
			mean[0] += v[i];
			mean[l[i]] += v[i];
			size[0] ++;
			size[l[i]] ++;
		}

		for (int i = 0; i < maxlabel +1; i++)
		{
			mean[i] = mean[i]/size[i];
		}

		/* Calculating the medians */
		// TODO: This is terribly inneficient ( O(labelsize*v*O(log labelsize)))
		// Repeat the mantra: premature optimization is the root of all evil
		for (int i = 1; i < maxlabel+1; i++)
			if (size[i] > 0)
			{
				double tmp[] = new double[size[i]];
				int ti = 0;
				for (int j = 0; j < v.length; j++) // here I'm creating a temporary array to hold all values of a particular cluster
					if (l[j] == i)
					{
						tmp[ti] = v[j];
						ti++;
					}
				median[i] = Maths.median(tmp);
			}

		
		
		/* Calculating the Variance - I'm not compensating for the sum, this might bring me problems */
		for (int i = 0; i < v.length; i++)
		{
			variance[0] += (v[i] - mean[0])*(v[i] - mean[0]);
			variance[l[i]] += (v[i] - mean[l[i]])*(v[i] - mean[l[i]]);
		}
		
		
		
		for (int i = 0; i < maxlabel+1; i++)
			if (size[i] > 1)
			{
				variance[i] = (variance[i]/(size[i] - 1))/kerneldivisor; // variance (kernel) is divided by a parameter
			}
			else 
				variance[i] = 0;
		
		// DEBUG
		//for(int i = 0; i < maxlabel + 1; i++)
		//	System.out.println(variance[i]);
		
		/* Calculating the sum of Gaussian function values */
		/* They are summed for the entire dataset (i == j) and for each cluster (x!=j) */
		
		for (int i = 0; i < v.length; i++)
			for (int j = 0; j < v.length; j++)
				if (l[i] == l[j]) // mesma classe (G da classe)
				{
					Gcalc[l[i]] += Maths.gaussian(v[i] - v[j], Math.sqrt(2*variance[l[i]]));
				}
				else // classes diferentes (G do grupo)
				{
					// this is modified to take into account the positions of the two points.
					// if the points are switched (median of the smaller point is smaller than the median of the bigger point), set the mean to zero
					
					if (((v[i] < v[j]) && (median[l[i]] > median[l[j]]))||
						((v[i] > v[j]) && (median[l[i]] < median[l[j]])))
						Gcalc[0] += Maths.gaussian(0, Math.sqrt(variance[l[i]]+variance[l[j]]));
					else
						Gcalc[0] += Maths.gaussian(v[i] - v[j], Math.sqrt(variance[l[i]]+variance[l[j]]));
				}

		// DEBUG
		//for(int i = 0; i < maxlabel + 1; i++)
		//	System.out.println(Gcalc[i]);
		
		double H = 0;
		double CEF = 2;
		for (int i = 0; i < maxlabel; i++)
		{
			// H is consistent with Rodrigo's code
			if(size[i+1] > 0)
			{
				H += -1.0 * Math.log((1.0/(size[i+1]*size[i+1]))*Gcalc[i+1]); // Summing the entropies
				CEF *= size[i+1];
			}
		}
		
		// CEF is consistent with Rodrigo's program
		CEF = -2*Math.log(Gcalc[0]/CEF);

//		System.out.print(CEF+" "+H+" ");
		
		double ret = CEF - H;		
		
		return ret;
	}

	
	
}