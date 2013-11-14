package jp.ac.tsukuba.conclave.cytometry.projection;

/***
 * This class stores a linear projection
 * A linear projection is defined as a set of real valued weights.
 * 
 * 
 * 
 * @author claus.aranha
 *
 */
public class LinProj {

	public double[] w;
	
	/**
	 * Constructor for empty projection based on size
	 * Should be composed all of zeroes.
	 * 
	 * @param size
	 */
	public LinProj(int size)
	{
		w = new double[size];
	}
	
	/**
	 * Constructor for copying a projection based on an array of weights
	 */
	public LinProj(double[] ww)
	{
		w = ww.clone();
	}
	
	public LinProj clone()
	{
		return(new LinProj(w.clone()));
	}
	
	
	///////////////////////////////////////////////////////////////////////////
	
	/**
	 * calculates a projection point based on an original data point
	 */
	public double project(double[] data)
	{
		if (data.length != w.length)
		{
			System.err.println("Error: Projection length and data lenght are different!");
			System.exit(-1);
		}
		
		double ret = 0;
		for (int i = 0; i < data.length; i++)
			ret += data[i]*w[i];
		
		return ret;
	}
	
	// TODO: Dump function
	public String dump()
	{
		String s = "";
		for (int i = 0; i < w.length; i++)
		{
			s = s + w[i] + " ";
		}
		return s;
	}
}
