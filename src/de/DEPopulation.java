package de;
/**
 * DEPopulation
 * 
 * Evolves the Solution using the Differential Evolution
 * Framework described in:
 * "An Enhanced Memetic Differential Evolution in Filter 
 * Design for Defect Detection in Paper Production"
 * Tirronem et. al. 2008, Evolutionary Computation.
 * 
 *  
 *  Basic Algorithm: Steady state, with new individual an
 *  created as: a1 + K(a2 - a3), and an replaces a4 if 
 *  f(an) > f(a4) (with a smidge of crossover between an and 
 *  a4.
 *  
 *  This DE is based on array genomes.
 */

import data.*;
import java.util.*;

import toolbox.GAVISparam;

public class DEPopulation {

	public int size = 50; // size of the population
	public int ngens = 300; // total number of generations
	public int currgen; // current generation

	double F = 0.8;
	double C = 0.9;
		
	/* Containers */
	public ArrayList<Genome> individual; 
	public Genome best;
	
	int a1, a2, a3, a4; // parents are replaced by indexes;
		
	/* Progress data */
	public double[] max_fitness;
	public double[] avg_fitness;

	RealLabelledData d; // the data
	
	/**
	 * Initialize and load parameters.
	 */
	public DEPopulation(RealLabelledData dd)
	{
		GAVISparam P = GAVISparam.getInstance();
		ngens = P.DE_gen;
		size = P.DE_pop;
		F = P.DE_F;
		C = P.DE_C;
		
		individual = new ArrayList<Genome>();		
		d = dd;
		max_fitness = new double[ngens+1];
		avg_fitness = new double[ngens+1];
	}

	/**
	 * Initialize the new population and the local
	 * variables. Startd is the target date for the 
	 * @param startd
	 */
	public void initPopulation()
	{
		// This is generation 0;
		currgen = 0;

		for (int i = 0; i < size; i++)
		{
			Genome n = new Genome(d.getTotalAttributes());
			n.init();
			n.eval(d);
			individual.add(n);
		}
		Collections.sort(individual);
		updateStatus();
	}
	
	/**
	 * Runs one generation loop
	 *
	 * In EMDE, there are no generations per se (steady-state). In Noman's DE, there are 
	 * generations, and each individual generates one offspring. I'll go with the second
	 * for now.
	 *
	 * returns the current generation, or -1 if done;
	 */
	public int runGeneration()
	{
		if (currgen < ngens)
		{	
			currgen++;
			Genome tmp;
			ArrayList<Genome> nextgen = new ArrayList<Genome>();
			Iterator<Genome> it = individual.iterator();
		
			// Step 1 - copy the best individual to the next generation;
			tmp = it.next();
			nextgen.add(tmp);
		
			// Step 2 - for each individual, generate a possible offspring
			while(it.hasNext())
			{
				nextgen.add(genNewIndividual(it.next()));
			}
		
			Collections.sort(nextgen);
			individual = nextgen;
		
			updateStatus();
			return currgen;
		}
		else
			return -1;
	}
	
	public Genome genNewIndividual(Genome parent)
	{

		Genome gj, gk, gl;
		Genome child;

		Random dice = new Random();

		int a1 = a2 = a3 = 0;
		while ((a1 == a2) || (a1 == a3) || (a2 == a3))
		{
			a1 = dice.nextInt(size);
			a2 = dice.nextInt(size);
			a3 = dice.nextInt(size);
		}
		
		gj = individual.get(a1);
		gk = individual.get(a2);
		gl = individual.get(a3);
				
		/* child = a1 + F(a2 - a3) */		
		child = new Genome(gj.w.length);		
		
		for (int i = 0; i < child.w.length; i++)
		{
			child.w[i] = gj.w[i] + F*(gk.w[i] - gl.w[i]);
		}
				
		// exchange with parent
		for (int i = 0; i < child.w.length; i++)
			if (dice.nextDouble() > C) 
				// C is the probability of receiving the mutated value, not the parent value!
				// If results get worse, just change the parameter.
			{
				child.w[i] = parent.w[i];
			}	
		
		child.eval(d);
		
		if (child.fitness > parent.fitness)
			return child;
		else
			return parent;		
		
	}
		
	/**
	 * update the values of the maxfitness/avg fitness/etc
	 * public arrays;
	 */
	public void updateStatus()
	{
		
		Iterator<Genome> it = individual.iterator();
		Genome t = it.next();


		max_fitness[currgen] = t.fitness;
		avg_fitness[currgen] = t.fitness;
		best = t.clone();
		
		
		while (it.hasNext())
		{
			avg_fitness[currgen] += it.next().fitness;			
		}
		
		avg_fitness[currgen] /= size;	
	}
	
		
	
	
	
}

