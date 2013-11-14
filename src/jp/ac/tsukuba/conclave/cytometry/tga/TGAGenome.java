package jp.ac.tsukuba.conclave.cytometry.tga;

import java.util.Iterator;
import jp.ac.tsukuba.conclave.cytometry.data.ObservationReal;
import jp.ac.tsukuba.conclave.cytometry.data.RealLabelledData;
import jp.ac.tsukuba.conclave.cytometry.divmeasure.Dcs;
import jp.ac.tsukuba.conclave.cytometry.projection.LinProj;


public class TGAGenome extends LinProj implements Comparable<TGAGenome> {

	public double fitness = 0;
	TGANode root = null;
	int maxdepth = 0; 
	int maxpar; // maximum index
	
	// parametros
	double fillrate = 0.8;
	int additive_tree = 0;
	
	boolean evalflag = true; // if this is true, this genome needs to be evalued again (crossover/mutation);
	
	public TGAGenome(int size) {
		super(size);
	}

	/**
	 * Initializes a random genome.
	 */
	public void init(RealLabelledData d, int md, double fr) 
    {
		//maxdepth = (int)Math.floor(Math.log(d.attr_num)/Math.log(2))+2;
		maxdepth = md;
		fillrate = fr;
		maxpar = d.getTotalAttributes();
		root = new TGANode();
		root.generateTree(md, maxpar, 0, fr);
		evalflag = true;
		w = root.getArray();
    }
		
	/**
	 * Generates a copy of the current genome
	 * @return
	 */
    public TGAGenome clone()
    {
    	
    	TGAGenome ret = new TGAGenome(maxpar);
    	ret.fitness = fitness;
    	ret.maxdepth = maxdepth;
    	ret.maxpar = maxpar;
    	ret.fillrate = fillrate;
    	ret.w = w.clone();
    	ret.root = root.clone();
    	ret.evalflag = evalflag;
    	
    	return ret;
    }
	
    
    /** crossover
     * 
     * @param mate
     * @return 
     */
	public void crossover(TGAGenome mate)
	{
		boolean ret = root.crossover(mate.root);
		evalflag = evalflag || ret;
		mate.evalflag = mate.evalflag || ret;
		//System.out.println(ret);
	}
	
	// Mutation
	public void mutate()
	{
		root.mutate(fillrate);
		evalflag = true;
	}
	
	/**
     * this implements High fitness is better 
     * switch signal to change
     */
    public int compareTo(TGAGenome gt)
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
	public void eval(RealLabelledData d)
	{
		if (evalflag == false)
			return;

		// TODO: Parametrize this in a better way
		int alt_w = additive_tree;
		
		if (alt_w == 0)
			w = root.getArray();
		else
			w = root.getArray2();
		
		double[] x = new double[d.size()];
		int[] y = new int[d.size()];
		
		Iterator<ObservationReal> it = d.iterator();
		int i = 0;
		while (it.hasNext())
		{
			ObservationReal u = it.next();
			x[i] = project(u.getAttributeValues());
			y[i] = u.getLabel();
			i++;
	    }
		
		// TODO: Parametrize kernel width (1)
		fitness = Dcs.calculate(x, y, 1);
		evalflag = false;
	}

	/**
	 * generates a string with info about the genome
	 */
	public String dump()
	{
		String s = super.dump();
		s = s + "\nFitness: " + fitness + " (" + evalflag + ")\n";
		s = s + root.dumptree(false);
		
		return s;
	}

}



