package jp.ac.tsukuba.conclave.cytometry.tga;

import java.util.*;

import jp.ac.tsukuba.conclave.cytometry.data.RealLabelledData;
import jp.ac.tsukuba.cs.conclave.utils.Parameter;



public class TGAPopulation {

	public int size = 250; // size of the population
	public int ngens = 60; // total number of generations
	public int currgen; // current generation
	
	/* Crossover parameters */
	int tournamentK = 25; // size of tournament
	double mutrate = 0.2; // chance that a mutation will occur
	double xoverrate = 0.9; // chance that the xover will occur
	
	/* GP Parameters */
	int maxdepth; // Maximum depth for the tree. should be 2xlog(number of assets)
	double treedensity = 1; //chance of a tree being full
	
	/* Containers */
	public ArrayList<TGAGenome> individual;
	public TGAGenome best; 
		
	/* Progress data */
	public double[] max_fitness;
	public double[] avg_fitness;

	RealLabelledData d;
	
	
	/**
	 * Initialize and load parameters.
	 * Parameter comp is a node from a previous 
	 * scenario, which is used for distance calculations.
	 */
	public TGAPopulation(RealLabelledData dd, Parameter p)
	{
		individual = new ArrayList<TGAGenome>();
		d = dd;
		
		max_fitness = new double[ngens+1];
		avg_fitness = new double[ngens+1];

		String param = p.getParameter("maxdepth",null);
		if (param!=null)
			maxdepth = Integer.parseInt(param);
		else
			maxdepth = (int)Math.floor(Math.log(d.getTotalAttributes())/Math.log(2)) + 2;

	}

	/**
	 * Initialize the new population and the local
	 * variables. Startd is the target date for the 
	 * @param startd
	 */
	public void initPopulation()
	{
		currgen = 0;
		for (int i = 0; i < size; i++)
		{
			TGAGenome n = new TGAGenome(d.getTotalAttributes());
			n.init(d, maxdepth,treedensity);
			n.eval(d);
			individual.add(n);
		}
		Collections.sort(individual);
		best = individual.get(0).clone();
		updateStatus();
	}
	
	/**
	 * Runs one generation loop
	 *
	 */
	public int runGeneration()
	{		
		Random dice = new Random();
		
		if (currgen < ngens)
		{	
			currgen++;
			ArrayList<TGAGenome> nextgen = new ArrayList<TGAGenome>();

			// Step 1 - Crossover
			for (int i = 0; i < size/2; i++)
			{
				// Select first and second parent
				TGAGenome parent1 = tournament();
				TGAGenome parent2 = tournament();

				// See if we perform crossover, or if we just copy the individuals
				if (dice.nextDouble() < xoverrate)
					parent1.crossover(parent2);
				
				nextgen.add(parent1);
				nextgen.add(parent2);
			}
			
			
			// Step 2 - Mutation
			// Step 3 - Evaluation

			Iterator<TGAGenome> it = nextgen.iterator();
			while (it.hasNext())
			{
				TGAGenome n = it.next();
				if (dice.nextDouble() < mutrate)
					n.mutate();
				n.eval(d); // eval should be smart and skip evaluating individuals that didn't change
			}
			
			Collections.sort(nextgen);
			individual = nextgen;
			updateStatus();
			return currgen;
		}
		else
			return -1;
	}
	
	/**
	 * update the values of the maxfitness/avg fitness/etc
	 * public arrays;
	 */
	public void updateStatus()
	{
		Iterator<TGAGenome> it = individual.iterator();
		TGAGenome t = it.next();
		
		if (t.fitness > best.fitness)
			best = t.clone();		
		max_fitness[currgen] = best.fitness;
		
		avg_fitness[currgen] = t.fitness;
		while (it.hasNext())
		{
			avg_fitness[currgen] += it.next().fitness;			
		}
		
		avg_fitness[currgen] /= size;	
	}
	
		
	/**
	 * Select one parent from the population by using
	 * deterministic tournament selection.
	 * The function copy the chosen candidate and send
	 * him back.
	 * 
	 * @return
	 */
	TGAGenome tournament()
	{
		Random dice = new Random();
		
		TGAGenome resp = null;
		TGAGenome n = individual.get(dice.nextInt(size));
		resp = n;
		
		for (int i = 1; i < tournamentK; i++)
		{
			n = individual.get(dice.nextInt(size));
			if (n.fitness > resp.fitness)
				resp = n;
		}
	
		return resp.clone();
	}
	
}
