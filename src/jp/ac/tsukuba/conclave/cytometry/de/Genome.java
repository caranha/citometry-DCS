package jp.ac.tsukuba.conclave.cytometry.de;
/**
 * June/2008
 * Modified the Genome object to read parameters and calculate
 * fitness in the same way as the MemeNode object. Also, 
 * it now has support for Binary/Real/Mixed genomes.
 * 
 * October/2011 Heavily modified for Citometry projection use.
 */

import java.util.Random;

import jp.ac.tsukuba.conclave.cytometry.data.ObservationReal;
import jp.ac.tsukuba.conclave.cytometry.data.RealLabelledData;
import jp.ac.tsukuba.conclave.cytometry.divmeasure.Dcs;
import jp.ac.tsukuba.conclave.cytometry.projection.LinProj;

public class Genome extends LinProj implements Comparable<Genome>
{

	public double fitness = 0;
	int param_penalty = 0; // no parameter penalty

	public Genome(int size)
	{
		super(size);
		
	}
	
	public Genome(double[] d)
	{
		super(d);
	}	

	/**
	 * Count the number of non-zero weights in this genome;
	 * @return
	 */
	public int count()
	{
		int ret = 0;
		for (int i = 0; i < w.length; i++)
		{
			if (Math.abs(w[i] - 0) < 0.00001) // Comparing a double to 0. Precision is 0.00001
				ret++;
		}
		return ret;
	}
	
	/**
	 * Initializes a random genome.
	 */
	public void init() 
    {
		Random dice = new Random();
		
		// TODO: Implement the below (probably not here?
		//		param = P.getParam("dimension_penalty");
		//		if (param != null)
		//			param_penalty = Integer.parseInt(param);
		
		for (int i = 0; i < w.length; i++)
		{
			w[i] = (dice.nextDouble()*2) - 1; // uniform distribution between -1 an 1
			// TODO: Make this changeable through a parameter
		}
    }
	
	/**
	 * Generates a copy of the current genome
	 * @return
	 */
    public Genome clone()
    {
    	Genome resp = new Genome(w);
       	resp.fitness = fitness;    	
    	return resp;
    }
    
    
    /**
     * this implements High fitness is better 
     * switch signal to change
     */
    public int compareTo(Genome gt)
    {
    	if (fitness > gt.fitness)
    		return -1;
    	if (fitness < gt.fitness)
    		return 1;
    	return 0;    
    }
    
/**
 * Calculate the fitness of this genome for training data T.
 * @param T
 */
    public void eval(RealLabelledData data, int kernelsize)
    {
    	double[] x = new double[data.size()];
    	int[] y = new int[data.size()];
    	
    	int i = 0;
    	
    	for (ObservationReal aux: data)
    	{
    		x[i] = project(aux.getAttributeValues());
    		y[i] = aux.getLabel();
    		i++;
    	}
    	    	
    	fitness = Dcs.calculate(x, y, kernelsize);
    	if (param_penalty == 1)
    	{
    		fitness = fitness*((w.length - count())/(w.length - 1.0));
    	}
    }
    
}
