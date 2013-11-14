package jp.ac.tsukuba.conclave.cytometry.runnable;

import java.io.IOException;

import jp.ac.tsukuba.conclave.cytometry.data.RealLabelledData;
import jp.ac.tsukuba.conclave.cytometry.data.RealLabelledDataFactory;
import jp.ac.tsukuba.conclave.cytometry.de.DEPopulation;
import jp.ac.tsukuba.conclave.cytometry.de.Genome;
import jp.ac.tsukuba.conclave.cytometry.toolbox.Maths;
import jp.ac.tsukuba.cs.conclave.utils.Parameter;

import gam_writer.RWriter;

/**
 * 
 * This is a version of the Cytometry experiment for the GAM system.
 * 
 * An arbitrary number of labels are taken from a Flow Cytometry data file,
 * and a projection is generated for these labels using Differential Evolution.
 * 
 * Output is sent to the GAM reader and the standard output.
 * 
 * @relevantparameters 
 *   "datafile" - path to the training data
 *   "label list" - comma separated list of cluster labels to use
 * 
 * @author caranha
 *
 */
public class cytometryExperimentGAM {

	static Parameter p;
	
	/**
	 * @param args location of the parameter file
	 */
	public static void main(String[] args) {
		
		p = new Parameter();
		
		if (args.length == 0) // No Parameters
		{
			printParameters();
			System.exit(1);
		}
		
		// Read parameters
		try {
			p.loadTextFile(args[0]);
		} catch (IOException e) {
			System.err.println("Error when loading Parameter File: "+e.getMessage());
			e.printStackTrace();
			System.exit(1);
		}

		int maxrepeats = Integer.parseInt(p.getParameter("Repetition Number", "10"));
		
		// Loading the label list
		String[] labels = p.getParameter("Label List", "1,2").split(",");
		int[] labellist = new int[labels.length];
		for (int i = 0; i < labels.length; i++)
			labellist[i] = Integer.parseInt(labels[i]);

		// Loading Data
		String trainfile = p.getParameter("datafile",null);				
		RealLabelledData data = RealLabelledDataFactory.dataFromTextFile(trainfile, -1, null);
		System.out.println(data);
		
		
		data = RealLabelledDataFactory.selectLabels(data, labellist);
		System.out.println(data);
				
		// Excute DE for X
		System.out.println("Data Initialized");
		
		RWriter gam_output = new RWriter();
		
		for (int i = 0; i < maxrepeats; i++)
		{
			gam_output.set_repetition_no(i);
			System.out.print("Rep "+i+": ");
			DEPopulation dex = new DEPopulation(data,p);
			dex.initPopulation();
			
			int currgen;
			while ((currgen = dex.runGeneration()) != -1)
			{
				gam_output.set_generation_no(currgen);
				gam_output.set_avg_fitness(dex.avg_fitness[currgen]);
				gam_output.set_best_fitness(dex.max_fitness[currgen]);
				
				// TODO: this is horrible, dex should provide a genome iterator or an array 
				// of fitnesses.
				double fit[] = new double[dex.individual.size()];
				int j = 0;
				for (Genome aux: dex.individual)
				{
					fit[j] = aux.fitness;
				}
				
				gam_output.set_fitness_stdev(Maths.deviation(fit));
				gam_output.set_diversity(0);
				gam_output.write_line();
				
				if (currgen%10 == 0) System.out.print(".");
			}
			System.out.println(" complete!");
			System.out.println("Best Projection: ");
			System.out.println(dex.best.dump());
		}
		// Final Output
		
		
	}

	private static void printParameters() {
		// TODO Auto-generated method stub
		System.out.println("Needs a parameter file. A default parameter file for this experiment should look like this:");
		
		p.addParameter("datafile", "path/to/data/file");
		p.addParameter("Label List", "1,3");
		p.addParameter("Repetition Number", "10");
		p.addParameter("Generation Number", "300");
		p.addParameter("Individual Number", "50");
		p.addParameter("DE F","0.8");
		p.addParameter("DE C", "0.9");
		p.addParameter("kernel size","1");
		
		System.out.println(p);
	}
	

}
