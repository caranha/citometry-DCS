package toolbox;


import java.util.ArrayList;
import java.util.Collections;



public class Maths {

	
	/**
	 * Returns the Gausian Probability density with mean 0, for value x, with standard error sigma.
	 * 
	 * gaussian(x,sigma) = (1/sqrt(2*sigma^2*pi)) * exp(- x^2/2a^2)
	 *
	 * @param x value to be calculated
	 * @param sigma standard deviation
	 * @return
	 */
	static public double gaussian(double x, double sigma)
	{
		if (sigma == 0)
		{
			// To avoid NaNs if all the attribute values are the same
			if (x!=0)
				return 0;
			else
				return Double.MAX_VALUE/64; // Shouldn't this be 1????
		}
		
		double ret = 0;
		ret = Math.exp(-((x*x)/(2*sigma*sigma)));
		ret = ret/Math.sqrt(sigma*sigma*2*Math.PI);
		
		return ret;
	}
	/** 
	 * k-dimensional version of the Gaussian PDF
	 */
	// FIXME: Make sure this works
	static public double gaussian(double x[], double sigma[])
	{
		double multisigma = 1;
		double xsum = 0;
		for (int i = 0; i < sigma.length; i++)
		{	
			//System.out.println(multisigma + " " + sigma[i]);
			multisigma*=sigma[i]*sigma[i];
			xsum += x[i]*x[i];
		}
		
		
		if (multisigma == 0)
		{
			// To avoid NaNs if all the attribute values are the same
			if (xsum!=0)
				return 0;
			else
				return Double.MAX_VALUE/64;
		}
		
		double ret = 0;
		ret = Math.exp(-(xsum/(2*multisigma)));
		ret = ret/Math.sqrt(multisigma*2*Math.pow(Math.PI,sigma.length));
		
		return ret;
	}
	
	static public double median(double[] d)
	{
		if (d.length == 1)
			return d[0];

		ArrayList<Double> t = new ArrayList<Double>();
		for (int i = 0; i < d.length; i++)
			t.add(d[i]);
		Collections.sort(t);
		
		
		return (t.get(t.size()/2)+t.get((t.size()-1)/2))/2;
	}
		
	static public double deviation(double[] d)
	{
		double ret = 0;
		
		double mean = 0;
		
		for (int i = 0; i < d.length; i++)
			mean += d[i];
		mean = mean/d.length;
		
		for (int i = 0; i < d.length; i++)
			ret = ret + (mean - d[i])*(mean - d[i]);
		ret = ret/(d.length-1);		
		
		return Math.sqrt(ret);
	}
}
