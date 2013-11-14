package jp.ac.tsukuba.conclave.cytometry.runnable;

import java.io.IOException;

import jp.ac.tsukuba.conclave.cytometry.data.RealLabelledData;
import jp.ac.tsukuba.conclave.cytometry.data.RealLabelledDataFactory;
import jp.ac.tsukuba.cs.conclave.utils.Parameter;

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
 *   "GAM Fitness" - 'X' or 'Y', which axis fitness data we want to send to GAM project
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

		// Loading the label list
		String[] labels = p.getParameter("Label List", "1,2").split(",");
		int[] labellist = new int[labels.length];
		for (int i = 0; i < labels.length; i++)
			labellist[i] = Integer.parseInt(labels[i]);

		// Loading Data
		String trainfile = p.getParameter("datafile",null);				
		RealLabelledData data = RealLabelledDataFactory.dataFromTextFile(trainfile, -1, null);
		data = RealLabelledDataFactory.selectLabels(data, labellist);
				
		// Excute DE for X and Y
		
		// Output
		
		
	}

	private static void printParameters() {
		// TODO Auto-generated method stub
		System.out.println("Needs a parameter file. A default parameter file for this experiment should look like this:");
		
		p.addParameter("datafile", "path/to/data/file");
		p.addParameter("GAM fitness", "X|Y");
		p.addParameter("Label List", "1,3");
		
	}
	

}
