package jp.ac.tsukuba.conclave.cytometry.runnable;


import java.io.BufferedWriter;
import java.io.FileWriter;

import jp.ac.tsukuba.conclave.cytometry.data.*;
import jp.ac.tsukuba.conclave.cytometry.de.DEPopulation;
import jp.ac.tsukuba.conclave.cytometry.projection.*;
import jp.ac.tsukuba.conclave.cytometry.toolbox.*;
import jp.ac.tsukuba.cs.conclave.utils.Parameter;

/***
 * Sandbox class. This class is just a hodgepodge of testing methods.
 * 
 * @author claus.aranha
 *
 */

public class OldTesting {

	public static void main(String[] args) {

		Parameter P = new Parameter();
		try {
			P.loadTextFile("/home/claus.aranha/Desktop/Citometria/misc_files/sample.par");
		} catch (Exception e) {
			e.printStackTrace();
		}

		TestBestXY(P);
		//TestTriples();
		//TestNormal();
		//ListBestAttributes();
		//AttributeSelectionTesting();
		
	}
	
	static public void TestBestXY(Parameter P) // Selects best attributes separately for X and Y
	{
		
		/* For each pair of diseases:
		 * Select best 5 attributes for X
		 * Calculate X
		 * Transform the projection to full attribute
		 * Transform the data using the transformed projection
		 * Select best 5 attributes again, use them for Y
		 * Calculate Y
		 */
		
		String trainfile = P.getParameter("train_datafile",null);
		String testfile = P.getParameter("test_datafile",null);
		String prefix = P.getParameter("file_prefix",null);
		
		RealLabelledData train = RealLabelledDataFactory.dataFromTextFile(trainfile, -1, null);
		RealLabelledData train_c[] = RealLabelledDataFactory.partitionLabels(train);
				
		RealLabelledData test = RealLabelledDataFactory.dataFromTextFile(testfile, -1, null);
		RealLabelledData test_c[] = RealLabelledDataFactory.partitionLabels(test);

		try
		{
			// General fitness/hits file:
			BufferedWriter fiterror1 = new BufferedWriter(new FileWriter(prefix + "_fiterror1"));	
			BufferedWriter fiterror2 = new BufferedWriter(new FileWriter(prefix + "_fiterror2"));
			BufferedWriter output = new BufferedWriter(new FileWriter(prefix + "_output"));


			for(int c1 = 1; c1 < 11; c1++)
				for(int c2 = c1+1; c2 < 11; c2++)
				{
					System.out.print("Running Clusters " + c1 + " and "+ c2 +":\n");
					output.write("Results for Clusters "+c1+" and "+c2+"\n");

					// selecting the data
					RealLabelledData train_data = new RealLabelledData();
					train_data.merge(train_c[c1]);
					train_data.merge(train_c[c2]);

					// Cluster 10 (normal) has no test data. So test_c[10] does not exist
					RealLabelledData test_data = new RealLabelledData();
					test_data.merge(test_c[c1]);
					if (c2 < 10)
					{
						test_data.merge(test_c[c2]);	
					}
					
					// TODO: parametrize kernel divisor (1 here)
					int[] attrib_select_x = TestResult.bestAttributes(train_data, 5, 1);
					RealLabelledData train_data_x = RealLabelledDataFactory.selectAttributes(train_data, attrib_select_x);

					output.write("Best Attributes for X: ");					
					for (int ki = 0; ki < attrib_select_x.length; ki++)
						output.write(attrib_select_x[ki] + " ");					
					output.write("\nBest Attributes for Y: ");
					
					
					int repeats = Integer.parseInt(P.getParameter("Repetition Number","1"));
					
					int[] totalhits = new int[test_data.size()];
					double[] fitness1 = new double[repeats];
					double meanfitness1 = 0;
					double[] fitness2 = new double[repeats];
					double meanfitness2 = 0;
					double[] hits = new double[repeats];
					double meanhits = 0;

					String[] proj1 = new String[repeats];
					String[] proj2 = new String[repeats];

					// -- for each pair of classes, run repeats times
					for (int k = 0; k < repeats; k++)
					{
						System.out.print(".");
						// first projection
						DEPopulation depop = new DEPopulation(train_data_x,P);
						depop.initPopulation();						
						while (depop.runGeneration() >= 0);

						// second projection:

						// first, expand the projection:						
						LinProj P1 = new LinProj(train_data.getTotalAttributes());
						for (int attrib = 0; attrib < attrib_select_x.length; attrib++)
							P1.w[attrib_select_x[attrib]] = depop.best.w[attrib];
						
						// create the projected data based on the expanded projection		
						RealLabelledData dproj = train_data.ortogonalProjection(P1);
						
						// calculate the best 5 attributes of the new projection
						// TODO: Parametrize kernel divisor (here 1)
						int[] attrib_select_y = TestResult.bestAttributes(dproj, 5, 1);
						RealLabelledData train_data_y = RealLabelledDataFactory.selectAttributes(dproj, attrib_select_y);

						// writing the best attributes for Y for this run.
						output.write("\n   ");
						for (int ki = 0; ki < attrib_select_y.length; ki++)
							output.write(attrib_select_y[ki] + " ");
						
						DEPopulation depop2 = new DEPopulation(train_data_y,P);
						depop2.initPopulation();
						while (depop2.runGeneration() >= 0);

						// expand best Y projection for the generalization testing
						LinProj P2 = new LinProj(train_data.getTotalAttributes());
						for (int attrib = 0; attrib < attrib_select_y.length; attrib++)
							P2.w[attrib_select_y[attrib]] = depop2.best.w[attrib];
						

						boolean r[] = TestResult.generalizeTest(train_data, test_data, P1, P2);
						int nhits = 0;

						for (int ki = 0; ki < r.length; ki++)
						{
							if (r[ki] == true) // hit
							{
								nhits++;
								totalhits[ki]++;
							}
						}

						// adding up errors for the run
						fitness1[k] = depop.best.fitness;
						meanfitness1 += depop.best.fitness;
						fitness2[k] = depop2.best.fitness;
						meanfitness2 += depop2.best.fitness;
						hits[k] = nhits;
						meanhits += nhits;

						// -- print info for global fiterror files
						fiterror1.write(depop.best.fitness + " " + nhits + "\n");
						fiterror2.write(depop2.best.fitness + " " + nhits + "\n");

						// -- for each run, print projection files
						TestResult.dumpProjection(train_data, P1, P2, "_"+c1+"_"+c2+"_train"+k, P);
						TestResult.dumpProjection(test_data, P1, P2, "_"+c1+"_"+c2+"_test"+k, P);

						proj1[k] = ((LinProj)depop.best).dump();
						proj2[k] = ((LinProj)depop2.best).dump();

					} // loop for running repeat times

					output.write("\nFitness1 --  mean: "+meanfitness1/repeats+"  deviation: "+Maths.deviation(fitness1)+"\n");
					output.write("Fitness2 --  mean: "+meanfitness2/repeats+"  deviation: "+Maths.deviation(fitness2)+"\n");
					output.write("Hits     --  mean: "+meanhits/repeats+"  deviation: "+Maths.deviation(hits)+"\n");
					output.write("Hits per case: ");
					for (int ki = 0; ki < totalhits.length; ki++)
						output.write(totalhits[ki] + " ");

					output.write("\n\n\n");
					output.flush();
					System.out.println();
				}// loop for running the cluster pairs (one loop for i and j)

		fiterror1.close();
		fiterror2.close();
		output.close();
	}
	catch(Exception e)
	{
		System.err.println("ERROR in the main loop: " + e.getMessage());
		e.printStackTrace();
	}
		
		
		
	}
	
//	static public void TestTriples() // Test all the diseases 3 at a time
//	{
//		GAVISparam P = GAVISparam.getInstance();
//
//		String trainfile = P.getParam("train_datafile");
//		String testfile = P.getParam("test_datafile");
//		String prefix = P.getParam("file_prefix");
//
//		
//		RealLabelledData train = RealLabelledDataFactory.dataFromTextFile(trainfile, -1, null);
//		RealLabelledData train_c[] = RealLabelledDataFactory.partitionLabels(train);
//				
//		RealLabelledData test = RealLabelledDataFactory.dataFromTextFile(testfile, -1, null);
//		RealLabelledData test_c[] = RealLabelledDataFactory.partitionLabels(train);
//		
//		try
//		{
//			// General fitness/hits file:
//			BufferedWriter fiterror1 = new BufferedWriter(new FileWriter(prefix + "_fiterror1"));	
//			BufferedWriter fiterror2 = new BufferedWriter(new FileWriter(prefix + "_fiterror2"));
//			BufferedWriter output = new BufferedWriter(new FileWriter(prefix + "_output"));
//
//
//			for(int c1 = 1; c1 < 11; c1++)
//				for(int c2 = c1+1; c2 < 11; c2++)
//					for(int c3 = c2+1; c3 < 11; c3++)			
//					{
//						System.out.print("Running Clusters " + c1 + ", " + c2 + " and "+ c3 +":\n");
//						// selecting the data
//						RealLabelledData train_data = new RealLabelledData();
//						train_data.merge(train_c[c1]);
//						train_data.merge(train_c[c2]);
//						train_data.merge(train_c[c3]);
//						
//						
//						RealLabelledData test_data = new RealLabelledData();
//						test_data.merge(test_c[c1]);
//						test_data.merge(test_c[c2]);
//
//						if (c3 < 10)
//						{
//							test_data.merge(test_c[c3]);
//						}
//						
//						int[] attrib_select = TestResult.bestAttributes(train_data, 10);
//
//						RealLabelledData train_data_redux = train_data.selectAttrib(attrib_select);
//						RealLabelledData test_data_redux = test_data.selectAttrib(attrib_select);
//
//						int[] totalhits = new int[test_data.size()];
//						double[] fitness1 = new double[P.repeats];
//						double meanfitness1 = 0;
//						double[] fitness2 = new double[P.repeats];
//						double meanfitness2 = 0;
//						double[] hits = new double[P.repeats];
//						double meanhits = 0;
//						String[] proj1 = new String[P.repeats];
//						String[] proj2 = new String[P.repeats];
//
//						// -- for each pair of classes, run repeats times
//						for (int k = 0; k < P.repeats; k++)
//						{
//							System.out.print(".");
//							// first projection
//							DEPopulation depop = new DEPopulation(train_data_redux);
//							depop.initPopulation();						
//							while (depop.runGeneration() >= 0);
//
//							// second projection
//							RealLabelledData dproj = train_data_redux.transform(depop.best);
//							DEPopulation depop2 = new DEPopulation(dproj);
//							depop2.initPopulation();
//							while (depop2.runGeneration() >= 0);
//
//							LinProj p1 = depop.best;
//							LinProj p2 = depop2.best;
//
//							boolean r[] = TestResult.generalizeTest(train_data_redux, test_data_redux, p1, p2);
//							int nhits = 0;
//
//							for (int ki = 0; ki < r.length; ki++)
//							{
//								if (r[ki] == true) // hit
//								{
//									nhits++;
//									totalhits[ki]++;
//								}
//							}
//
//							// adding up errors for the run
//							fitness1[k] = depop.best.fitness;
//							meanfitness1 += depop.best.fitness;
//							fitness2[k] = depop2.best.fitness;
//							meanfitness2 += depop2.best.fitness;
//							hits[k] = nhits;
//							meanhits += nhits;
//
//							// -- print info for global fiterror files
//							fiterror1.write(depop.best.fitness + " " + nhits + "\n");
//							fiterror2.write(depop2.best.fitness + " " + nhits + "\n");
//
//							// -- for each run, print projection files
//							TestResult.dumpProjection(train_data_redux, p1, p2, "_"+c1+"_"+c2+"_"+c3+"_train"+k);
//							TestResult.dumpProjection(test_data_redux, p1, p2, "_"+c1+"_"+c2+"_"+c3+"_test"+k);
//
//							proj1[k] = ((LinProj)depop.best).dump();
//							proj2[k] = ((LinProj)depop2.best).dump();
//
//						} // loop for running repeat times
//
//						output.write("Results for Clusters "+c1+", "+c2+" and "+c3+"\n");
//						output.write("Fitness1 --  mean: "+meanfitness1/10+"  deviation: "+Maths.deviation(fitness1)+"\n");
//						output.write("Fitness1 --  mean: "+meanfitness2/10+"  deviation: "+Maths.deviation(fitness2)+"\n");
//						output.write("Hits     --  mean: "+meanhits/10+"  deviation: "+Maths.deviation(hits)+"\n");
//						output.write("Hits per case: ");
//						for (int ki = 0; ki < totalhits.length; ki++)
//							output.write(totalhits[ki] + " ");
//						output.write("\nBest Attributes: ");
//						for (int ki = 0; ki < attrib_select.length; ki++)
//							output.write(attrib_select[ki] + " ");
//
//						output.write("\n\n\n");
//						output.flush();
//						System.out.println();
//					}// loop for running the cluster pairs (one loop for i and j)
//
//			fiterror1.close();
//			fiterror2.close();
//			output.close();
//		}
//		catch(Exception e)
//		{
//			System.err.println("ERROR in the main loop: " + e.getMessage());
//			e.printStackTrace();
//		}
//	}
//	
//	static public void TestNormal()// many tests involving the normal individuals (6 of them)
//	{
//
//		GAVISparam P = GAVISparam.getInstance();
//
//		String trainfile = P.getParam("train_datafile");
//		String testfile = P.getParam("test_datafile");
//		String prefix = P.getParam("file_prefix");
//
//		RealLabelledData train = new RealLabelledData();
//		train.readFile(trainfile);
//		RealLabelledData train_c[] = train.breakClass();
//
//		RealLabelledData test = new RealLabelledData();
//		test.readFile(testfile);
//		RealLabelledData test_c[] = test.breakClass();
//
//		try
//		{
//			// General fitness/hits file:
//			BufferedWriter fiterror1 = new BufferedWriter(new FileWriter(prefix + "_fiterror1"));	
//			BufferedWriter fiterror2 = new BufferedWriter(new FileWriter(prefix + "_fiterror2"));
//			BufferedWriter output = new BufferedWriter(new FileWriter(prefix + "_output"));
//
//
//
//			for(int i = 1; i < 10; i++) //each cluster
//			{
//				System.out.print("Running Clusters " + 10 + " and "+ i +":\n");
//				// selecting the data
//				RealLabelledData train_data = train_c[10].clone();
//				RealLabelledData t2 = train_c[i].clone();
//				train_data.merge(t2);
//
//				RealLabelledData test_data = test_c[10].clone();
//				t2 = test_c[i].clone();
//				test_data.merge(t2);
//
//				int[] attrib_select = TestResult.bestAttributes(train_data, 5);
//
//				RealLabelledData train_data_redux = train_data.selectAttrib(attrib_select);
//				RealLabelledData test_data_redux = test_data.selectAttrib(attrib_select);
//
//				int[] totalhits = new int[test_data.size()];
//				double[] fitness1 = new double[P.repeats];
//				double meanfitness1 = 0;
//				double[] fitness2 = new double[P.repeats];
//				double meanfitness2 = 0;
//				double[] hits = new double[P.repeats];
//				double meanhits = 0;
//				String[] proj1 = new String[P.repeats];
//				String[] proj2 = new String[P.repeats];
//
//				// -- for each pair of classes, run repeats times
//				for (int k = 0; k < P.repeats; k++)
//				{
//					System.out.print(".");
//					// first projection
//					DEPopulation depop = new DEPopulation(train_data_redux);
//					depop.initPopulation();						
//					while (depop.runGeneration() >= 0);
//
//					// second projection
//					RealLabelledData dproj = train_data_redux.transform(depop.best);
//					DEPopulation depop2 = new DEPopulation(dproj);
//					depop2.initPopulation();
//					while (depop2.runGeneration() >= 0);
//
//					LinProj p1 = depop.best;
//					LinProj p2 = depop2.best;
//
//					boolean r[] = TestResult.generalizeTest(train_data_redux, test_data_redux, p1, p2);
//					int nhits = 0;
//
//					for (int ki = 0; ki < r.length; ki++)
//					{
//						if (r[ki] == true) // hit
//						{
//							nhits++;
//							totalhits[ki]++;
//						}
//					}
//
//					// adding up errors for the run
//					fitness1[k] = depop.best.fitness;
//					meanfitness1 += depop.best.fitness;
//					fitness2[k] = depop2.best.fitness;
//					meanfitness2 += depop2.best.fitness;
//					hits[k] = nhits;
//					meanhits += nhits;
//
//					// -- print info for global fiterror files
//					fiterror1.write(depop.best.fitness + " " + nhits + "\n");
//					fiterror2.write(depop2.best.fitness + " " + nhits + "\n");
//
//					// -- for each run, print projection files
//					TestResult.dumpProjection(train_data_redux, p1, p2, "_"+10+"_"+i+"_train"+k);
//					TestResult.dumpProjection(test_data_redux, p1, p2, "_"+10+"_"+i+"_test"+k);
//
//					proj1[k] = ((LinProj)depop.best).dump();
//					proj2[k] = ((LinProj)depop2.best).dump();
//
//				} // loop for running repeat times
//
//				output.write("Results for Clusters "+10+" and "+i+"\n");
//				output.write("Fitness1 --  mean: "+meanfitness1/10+"  deviation: "+Maths.deviation(fitness1)+"\n");
//				output.write("Fitness1 --  mean: "+meanfitness2/10+"  deviation: "+Maths.deviation(fitness2)+"\n");
//				output.write("Hits     --  mean: "+meanhits/10+"  deviation: "+Maths.deviation(hits)+"\n");
//				output.write("Hits per case: ");
//				for (int ki = 0; ki < totalhits.length; ki++)
//					output.write(totalhits[ki] + " ");
//				output.write("\nBest Attributes: ");
//				for (int ki = 0; ki < attrib_select.length; ki++)
//					output.write(attrib_select[ki] + " ");
//
//				output.write("\n\n\n");
//				output.flush();
//				System.out.println();
//			}// loop for running the cluster pairs (one loop for i and j)
//
//			fiterror1.close();
//			fiterror2.close();
//			output.close();
//		}
//		catch(Exception e)
//		{
//			System.err.println("ERROR in the main loop: " + e.getMessage());
//			e.printStackTrace();
//		}
//		
//		
//		
//	}
//	
//	static public void ListBestAttributes() // print best attributes for all pairs
//	{
//		GAVISparam P = GAVISparam.getInstance();
//		RealLabelledData train = new RealLabelledData();
//		train.readFile("/home/claus.aranha/Desktop/Citometria/dados/data_newattrib.txt");
//
//		
//		
//		
//		RealLabelledData[] k = train.breakClass();
//
//		for (int i = 1; i < 10; i++)
//			for (int j = i+1; j < 10; j++)
//			{
//				System.out.println("\"Attribs: "+i+" and "+j+"\"");
//				RealLabelledData d = k[i].clone();
//				d.merge(k[j]);
//				for (int ii=0; ii < d.attr_num; ii++)
//					System.out.print(ii+" ");
//				System.out.println();
//				for (int ii=0; ii < d.attr_num; ii++)
//				{
//					LinProj L = new LinProj(d.attr_num);
//					L.w[ii] = 1;
//					
//					
//
//					double[] x = new double[d.size()];
//					int[] y = new int[d.size()];
//		    	
//		    		Iterator<ObservationReal> it = d.data.iterator();
//		    		int jj = 0;
//		    		while (it.hasNext())
//		    		{
//		    			ObservationReal u = it.next();
//		    			x[jj] = L.project(u.d);
//		    			y[jj] = u.c;
//		    			jj++;
//		    		}
//		    		System.out.print(Dcs.calculate(x, y)+ " ");
//				}
//				System.out.println("\n");
//				
//			}
//	}
//	
//	static public void AttributeSelectionTesting()
//	{
//		/* Goals: Check the performance of the program as we increase the number of attributes used */
//		/* Test on clusters: 3x8, 5x9, 8x9 */
//		
//		GAVISparam P = GAVISparam.getInstance();
//		
//		String trainfile = P.getParam("train_datafile");
//		String testfile = P.getParam("test_datafile");
//		String prefix = P.getParam("file_prefix");
//		
//		/* O pedreira pediu para eu testar com estes pares */
//		int c1[] = {3, 5, 8, 1};
//		int c2[] = {8, 9, 9, 3};
//
//		RealLabelledData train = new RealLabelledData();
//		train.readFile(trainfile);
//		RealLabelledData train_c[] = train.breakClass();
//		
//		RealLabelledData test = new RealLabelledData();
//		test.readFile(testfile);
//		RealLabelledData test_c[] = test.breakClass();
//		
//		try
//		{
//			// General fitness/hits file:
//    		BufferedWriter fiterror1 = new BufferedWriter(new FileWriter(prefix + "_fiterror1"));	
//    		BufferedWriter fiterror2 = new BufferedWriter(new FileWriter(prefix + "_fiterror2"));
//			BufferedWriter output = new BufferedWriter(new FileWriter(prefix + "_output"));
//
//
//			
//			for(int i = 0; i < c1.length; i++) //each pair
//			{
//				System.out.print("Running Clusters " + c1[i] + " and "+c2[i]+":\n");
//				// selecting the data
//				RealLabelledData train_data = train_c[c1[i]].clone();
//				RealLabelledData t2 = train_c[c2[i]].clone();
//				train_data.merge(t2);
//
//				RealLabelledData test_data = test_c[c1[i]].clone();
//				t2 = test_c[c2[i]].clone();
//				test_data.merge(t2);
//
//				int[] attrib_select = TestResult.bestAttributes(train_data, train_data.attr_num);
//
//				for(int attrib_i = 5; attrib_i < attrib_select.length; attrib_i++)
//				{	
//					System.out.println("With "+attrib_i+" attributes: ");
//					int a_s[] = new int[attrib_i];
//					for (int jj = 0; jj < attrib_i; jj++)
//						a_s[jj] = attrib_select[jj];
//										
//					RealLabelledData train_data_redux = train_data.selectAttrib(a_s);
//					RealLabelledData test_data_redux = test_data.selectAttrib(a_s);
//
//					int[] totalhits = new int[test_data.size()];
//					double[] fitness1 = new double[P.repeats];
//					double meanfitness1 = 0;
//					double[] fitness2 = new double[P.repeats];
//					double meanfitness2 = 0;
//					double[] hits = new double[P.repeats];
//					double meanhits = 0;
//					String[] proj1 = new String[P.repeats];
//					String[] proj2 = new String[P.repeats];
//
//					// -- for each pair of classes, run 10 times
//					for (int k = 0; k < P.repeats; k++)
//					{
//						System.out.print(".");
//						// first projection
//						DEPopulation depop = new DEPopulation(train_data_redux);
//						depop.initPopulation();						
//						while (depop.runGeneration() >= 0);
//
//						// second projection
//						RealLabelledData dproj = train_data_redux.transform(depop.best);
//						DEPopulation depop2 = new DEPopulation(dproj);
//						depop2.initPopulation();
//						while (depop2.runGeneration() >= 0);
//
//						LinProj p1 = depop.best;
//						LinProj p2 = depop2.best;
//
//						boolean r[] = TestResult.generalizeTest(train_data_redux, test_data_redux, p1, p2);
//						int nhits = 0;
//
//						for (int ki = 0; ki < r.length; ki++)
//						{
//							if (r[ki] == true) // hit
//							{
//								nhits++;
//								totalhits[ki]++;
//							}
//						}
//
//						// adding up errors for the run
//						fitness1[k] = depop.best.fitness;
//						meanfitness1 += depop.best.fitness;
//						fitness2[k] = depop2.best.fitness;
//						meanfitness2 += depop2.best.fitness;
//						hits[k] = nhits;
//						meanhits += nhits;
//
//						// -- print info for global fiterror files
//						fiterror1.write(depop.best.fitness + " " + nhits + "\n");
//						fiterror2.write(depop2.best.fitness + " " + nhits + "\n");
//
//						// -- for each run, print projection files
//						TestResult.dumpProjection(train_data_redux, p1, p2, c1[i]+"_"+c2[i]+"_a"+attrib_i+"_train"+k);
//						TestResult.dumpProjection(test_data_redux, p1, p2, c1[i]+"_"+c2[i]+"_a"+attrib_i+"_test"+k);
//
//						proj1[k] = ((LinProj)depop.best).dump();
//						proj2[k] = ((LinProj)depop2.best).dump();
//
//					} // loop for running repeat times
//
//
//
//					output.write("Results for Clusters "+c1[i]+" and "+c2[i]+" and "+attrib_i+" attributes\n");
//					output.write("Fitness1 --  mean: "+meanfitness1/10+"  deviation: "+Maths.deviation(fitness1)+"\n");
//					output.write("Fitness1 --  mean: "+meanfitness2/10+"  deviation: "+Maths.deviation(fitness2)+"\n");
//					output.write("Hits     --  mean: "+meanhits/10+"  deviation: "+Maths.deviation(hits)+"\n");
//					output.write("Hits per case: ");
//					for (int ki = 0; ki < totalhits.length; ki++)
//						output.write(totalhits[ki] + " ");
//					output.write("\nBest Attributes: ");
//					for (int ki = 0; ki < attrib_select.length; ki++)
//						output.write(attrib_select[ki] + " ");
//
////					output.write("\nProjections found: ");
////					for (int ki = 0; ki < proj2.length; ki++)
////						output.write(proj1[ki] + "\n" + proj2[ki] + "\n\n");
//
//					output.write("\n\n\n");
//					output.flush();
//					System.out.println();
//				}
//			}// loop for running the cluster pairs (one loop for i and j)
//			
//			fiterror1.close();
//    		fiterror2.close();
//    		output.close();
//			
//			
//		}
//		catch(Exception e)
//		{
//			System.err.println("ERROR in the main loop: " + e.getMessage());
//			e.printStackTrace();
//		}
//
//		
//		
//		
//	}
//
//	static public void AttributeTest()
//	{
//		GAVISparam P = GAVISparam.getInstance();		
//		
//		P.kerneldivisor = 50.0;
//		
//		RealLabelledData train = new RealLabelledData();
//		train.readFile("/home/claus.aranha/Desktop/Citometria/dados/data_original.txt");
//		RealLabelledData[] k = train.breakClass();
//		RealLabelledData d = k[1].clone();
//		d.merge(k[4]);
//
//		double attrib_index[] = new double[d.attr_num];
//		int attrib_select[] = new int[5];
//		for (int ii = 0; ii < 5; ii++)
//			attrib_select[ii] = -1;
//		
//		/* Selecting the 5 best attributes */
//		/* Dumb way */
//		for (int ii=0; ii < d.attr_num; ii++)
//		{
//			LinProj L = new LinProj(d.attr_num);
//			L.w[ii] = 1;
//
//			double[] x = new double[d.size()];
//			int[] y = new int[d.size()];
//    	
//    		Iterator<ObservationReal> it = d.data.iterator();
//    		int jj = 0;
//    		while (it.hasNext())
//    		{
//    			ObservationReal u = it.next();
//    			x[jj] = L.project(u.d);
//    			y[jj] = u.c;
//    			jj++;
//    		}
//    		attrib_index[ii] = Dcs.calculate(x, y);
//    		System.out.println(Dcs.calculate(x, y)+ " " + ii);
//    		
//    		/* placing the new attribute in the "best" list */
//    		int index = ii;
//    		for (jj = 0; jj < 5; jj++)
//    		{
//    			if (attrib_select[jj] == -1)
//    			{
//    				attrib_select[jj] = index;
//    				break;
//    			}
//    			if (attrib_index[index] > attrib_index[attrib_select[jj]])
//    			{
//    				int tmp = attrib_select[jj];
//    				attrib_select[jj] = index;
//    				index = tmp;
//    			}
//    		}
//		}
//		for(int i = 0; i < 5; i++)
//		{
//			System.out.print(attrib_select[i]+ " ");
//		}
//		
//		
//	}
//	
//	static public void GenTest3DE()
//	{
//		// Testes 1 e 2 de generalidade
//		GAVISparam P = GAVISparam.getInstance();
//		
//		String trainfile = P.getParam("train_datafile");
//		String testfile = P.getParam("test_datafile");
//		String prefix = P.getParam("file_prefix");
//		
//		RealLabelledData train = new RealLabelledData();
//		train.readFile(trainfile);
//		RealLabelledData train_c[] = train.breakClass();
//		
//		RealLabelledData test = new RealLabelledData();
//		test.readFile(testfile);
//		RealLabelledData test_c[] = test.breakClass();
//		
//		try
//		{
//			// General fitness/hits file:
//    		BufferedWriter fiterror1 = new BufferedWriter(new FileWriter(prefix + "_fiterror1"));	
//    		BufferedWriter fiterror2 = new BufferedWriter(new FileWriter(prefix + "_fiterror2"));
//			BufferedWriter output = new BufferedWriter(new FileWriter(prefix + "_output"));
//			
//			// There is some error with Class 10... check it later
//			for(int i = 1; i < 9; i++) //1 to 9 (10)
//				for(int j = i+1; j < 10; j++) // i+1 to 10 (11)
//				{
//					System.out.print("Running Clusters " + i + " and "+j+":");
//					// selecting the data
//					RealLabelledData train_data = train_c[i].clone();
//					RealLabelledData t2 = train_c[j].clone();
//					train_data.merge(t2);
//					
//					RealLabelledData test_data = test_c[i].clone();
//					t2 = test_c[j].clone();
//					test_data.merge(t2);
//
//					
//					double attrib_index[] = new double[train_data.attr_num];
//					int attrib_select[] = new int[5];
//					for (int ii = 0; ii < 5; ii++)
//						attrib_select[ii] = -1;
//					
//					/* Selecting the 5 best attributes */
//					/* Dumb way */
//					for (int ii=0; ii < train_data.attr_num; ii++)
//					{
//						LinProj L = new LinProj(train_data.attr_num);
//						L.w[ii] = 1;
//
//						double[] x = new double[train_data.size()];
//						int[] y = new int[train_data.size()];
//			    	
//			    		Iterator<ObservationReal> it = train_data.data.iterator();
//			    		int jj = 0;
//			    		while (it.hasNext())
//			    		{
//			    			ObservationReal u = it.next();
//			    			x[jj] = L.project(u.d);
//			    			y[jj] = u.c;
//			    			jj++;
//			    		}
//			    		attrib_index[ii] = Dcs.calculate(x, y);
//			    		
//			    		/* placing the new attribute in the "best" list */
//			    		int index = ii;
//			    		for (jj = 0; jj < 5; jj++)
//			    		{
//			    			if (attrib_select[jj] == -1)
//			    			{
//			    				attrib_select[jj] = index;
//			    				break;
//			    			}
//			    			if (attrib_index[index] > attrib_index[attrib_select[jj]])
//			    			{
//			    				int tmp = attrib_select[jj];
//			    				attrib_select[jj] = index;
//			    				index = tmp;
//			    			}
//			    		}
//			    		
//					}
//
//					RealLabelledData train_data_redux = train_data.selectAttrib(attrib_select);
//					RealLabelledData test_data_redux = test_data.selectAttrib(attrib_select);
//					
//					int[] totalhits = new int[test_data.size()];
//					double[] fitness1 = new double[P.repeats];
//					double meanfitness1 = 0;
//					double[] fitness2 = new double[P.repeats];
//					double meanfitness2 = 0;
//					double[] hits = new double[P.repeats];
//					double meanhits = 0;
//					String[] proj1 = new String[P.repeats];
//					String[] proj2 = new String[P.repeats];
//					
//					// -- for each pair of classes, run 10 times
//
//					for (int k = 0; k < P.repeats; k++)
//					{
//						System.out.print(".");
//						// first projection
//						DEPopulation depop = new DEPopulation(train_data_redux);
//						depop.initPopulation();						
//				    	while (depop.runGeneration() >= 0);
//				    	
//				    	// second projection
//						RealLabelledData dproj = train_data_redux.transform(depop.best);
//						DEPopulation depop2 = new DEPopulation(dproj);
//						depop2.initPopulation();
//						while (depop2.runGeneration() >= 0);
//														
//						LinProj p1 = depop.best;
//						LinProj p2 = depop2.best;
//
//						boolean r[] = TestResult.generalizeTest(train_data_redux, test_data_redux, p1, p2);
//						int nhits = 0;
//						
//						for (int ki = 0; ki < r.length; ki++)
//						{
//							if (r[ki] == true) // hit
//							{
//								nhits++;
//								totalhits[ki]++;
//							}
//						}
//						
//						// adding up errors for the run
//						fitness1[k] = depop.best.fitness;
//						meanfitness1 += depop.best.fitness;
//						fitness2[k] = depop2.best.fitness;
//						meanfitness2 += depop2.best.fitness;
//						hits[k] = nhits;
//						meanhits += nhits;
//						
//						// -- print info for global fiterror files
//						fiterror1.write(depop.best.fitness + " " + nhits + "\n");
//						fiterror2.write(depop2.best.fitness + " " + nhits + "\n");
//						
//						// -- for each run, print projection files
//						TestResult.dumpProjection(train_data_redux, p1, p2, "_"+i+"_"+j+"_train"+k);
//						TestResult.dumpProjection(test_data_redux, p1, p2, "_"+i+"_"+j+"_test"+k);
//						
//						proj1[k] = ((LinProj)depop.best).dump();
//						proj2[k] = ((LinProj)depop2.best).dump();
//						
//					} // loop for running 10 times
//					
//					
//					
//					output.write("Results for Clusters "+i+" and "+j+"\n");
//					output.write("Fitness1 --  mean: "+meanfitness1/10+"  deviation: "+Maths.deviation(fitness1)+"\n");
//					output.write("Fitness1 --  mean: "+meanfitness2/10+"  deviation: "+Maths.deviation(fitness2)+"\n");
//					output.write("Hits     --  mean: "+meanhits/10+"  deviation: "+Maths.deviation(hits)+"\n");
//					output.write("Hits per case: ");
//					for (int ki = 0; ki < totalhits.length; ki++)
//						output.write(totalhits[ki] + " ");
//					output.write("\nBest Attributes: ");
//					for (int ki = 0; ki < attrib_select.length; ki++)
//						output.write(attrib_select[ki] + " ");
//					
//					output.write("\nProjections found: ");
//					for (int ki = 0; ki < proj2.length; ki++)
//						output.write(proj1[ki] + "\n" + proj2[ki] + "\n\n");
//					
//					output.write("\n\n\n");					
//					System.out.println();
//				}// loop for running the cluster pairs (one loop for i and j)
//			
//			fiterror1.close();
//    		fiterror2.close();
//    		output.close();
//			
//			
//		}
//		catch(Exception e)
//		{
//			System.err.println("ERROR in the main loop: " + e.getMessage());
//			e.printStackTrace();
//		}
//		
//	}
//
//	static public void GenTest3TGA() // using TGA
//	{
//		// Testes 1 e 2 de generalidade
//		Parameter P = Parameter.getInstance();
//		
//		String trainfile = P.getParam("train_datafile");
//		String testfile = P.getParam("test_datafile");
//		String prefix = P.getParam("file_prefix");
//		
//		RealLabelledData train = new RealLabelledData();
//		train.readFile(trainfile);
//		RealLabelledData train_c[] = train.breakClass();
//		
//		RealLabelledData test = new RealLabelledData();
//		test.readFile(testfile);
//		RealLabelledData test_c[] = test.breakClass();
//		
//		
//				
//		try
//		{
//			// General fitness/hits file:
//    		BufferedWriter fiterror1 = new BufferedWriter(new FileWriter(prefix + "_fiterror1"));	
//    		BufferedWriter fiterror2 = new BufferedWriter(new FileWriter(prefix + "_fiterror2"));
//			BufferedWriter output = new BufferedWriter(new FileWriter(prefix + "_output"));
//			
//			// There is some error with Class 10... check it later
//			for(int i = 1; i < 9; i++) //1 to 9 (10)
//				for(int j = i+1; j < 10; j++) // i+1 to 10 (11)
//				{
//					System.out.print("Running Clusters " + i + " and "+j+":");
//					// selecting the data
//					RealLabelledData train_data = train_c[i].clone();
//					RealLabelledData t2 = train_c[j].clone();
//					train_data.merge(t2);
//					
//					RealLabelledData test_data = test_c[i].clone();
//					t2 = test_c[j].clone();
//					test_data.merge(t2);
//
//					int[] totalhits = new int[test_data.size()];
//					double[] fitness1 = new double[10];
//					double meanfitness1 = 0;
//					double[] fitness2 = new double[10];
//					double meanfitness2 = 0;
//					double[] hits = new double[10];
//					double meanhits = 0;
//					String[] proj1 = new String[10];
//					String[] proj2 = new String[10];
//					
//					// -- for each pair of classes, run 10 times
//
//					for (int k = 0; k < 10; k++)
//					{
//						System.out.print(".");
//						// first projection
//						TGAPopulation depop = new TGAPopulation(train_data);
//						depop.initPopulation();						
//				    	while (depop.runGeneration() >= 0);
//				    	
//				    	// second projection
//						RealLabelledData dproj = train_data.transform(depop.best);
//						TGAPopulation depop2 = new TGAPopulation(dproj);
//						depop2.initPopulation();
//						while (depop2.runGeneration() >= 0);
//														
//						LinProj p1 = depop.best;
//						LinProj p2 = depop2.best;
//
//						boolean r[] = TestResult.generalizeTest(train_data, test_data, p1, p2);
//						int nhits = 0;
//						
//						for (int ki = 0; ki < r.length; ki++)
//						{
//							if (r[ki] == true) // hit
//							{
//								nhits++;
//								totalhits[ki]++;
//							}
//						}
//						
//						// adding up errors for the run
//						fitness1[k] = depop.best.fitness;
//						meanfitness1 += depop.best.fitness;
//						fitness2[k] = depop2.best.fitness;
//						meanfitness2 += depop2.best.fitness;
//						hits[k] = nhits;
//						meanhits += nhits;
//						
//						// -- print info for global fiterror files
//						fiterror1.write(depop.best.fitness + " " + nhits + "\n");
//						fiterror2.write(depop2.best.fitness + " " + nhits + "\n");
//						
//						// -- for each run, print projection files
//						TestResult.dumpProjection(train_data, p1, p2, "_"+i+"_"+j+"_train"+k);
//						TestResult.dumpProjection(test_data, p1, p2, "_"+i+"_"+j+"_test"+k);
//						
//						proj1[k] = ((LinProj)depop.best).dump();
//						proj2[k] = ((LinProj)depop2.best).dump();
//						
//					} // loop for running 10 times
//					
//					
//					
//					output.write("Results for Clusters "+i+" and "+j+"\n");
//					output.write("Fitness1 --  mean: "+meanfitness1/10+"  deviation: "+Maths.deviation(fitness1)+"\n");
//					output.write("Fitness1 --  mean: "+meanfitness2/10+"  deviation: "+Maths.deviation(fitness2)+"\n");
//					output.write("Hits     --  mean: "+meanhits/10+"  deviation: "+Maths.deviation(hits)+"\n");
//					output.write("Hits per case: ");
//					for (int ki = 0; ki < totalhits.length; ki++)
//						output.write(totalhits[ki] + " ");
//					
//					output.write("\nProjections found: ");
//					for (int ki = 0; ki < proj2.length; ki++)
//						output.write(proj1[ki] + "\n" + proj2[ki] + "\n\n");
//					
//					output.write("\n\n\n");
//					
//					
//					System.out.println();
//				}// loop for running the cluster pairs (one loop for i and j)
//			
//			fiterror1.close();
//    		fiterror2.close();
//    		output.close();
//			
//			
//		}
//		catch(Exception e)
//		{
//			System.err.println("ERROR in the main loop: " + e.getMessage());
//			e.printStackTrace();
//		}
//		
//	}
//	
//	static public void GenTest2() // using TGA
//	{
//		// Testes 1 e 2 de generalidade
//		Parameter P = Parameter.getInstance();
//
//		
//		String trainfile = P.getParam("train_datafile");
//		String testfile = P.getParam("test_datafile");
//		String prefix = P.getParam("file_prefix");
//		
//		RealLabelledData train = new RealLabelledData();
//		train.readFile(trainfile);
//		RealLabelledData train_c[] = train.breakClass();
//		
//		RealLabelledData test = new RealLabelledData();
//		test.readFile(testfile);
//		RealLabelledData test_c[] = test.breakClass();
//		
//		
//				
//		try
//		{
//			// General fitness/hits file:
//    		BufferedWriter fiterror1 = new BufferedWriter(new FileWriter(prefix + "_fiterror1"));	
//    		BufferedWriter fiterror2 = new BufferedWriter(new FileWriter(prefix + "_fiterror2"));
//			BufferedWriter output = new BufferedWriter(new FileWriter(prefix + "_output"));
//			
//			// There is some error with Class 10... check it later
//			for(int i = 1; i < 9; i++) //1 to 9 (10)
//				for(int j = i+1; j < 10; j++) // i+1 to 10 (11)
//				{
//					System.out.print("Running Clusters " + i + " and "+j+":");
//					// selecting the data
//					RealLabelledData train_data = train_c[i].clone();
//					RealLabelledData t2 = train_c[j].clone();
//					train_data.merge(t2);
//					
//					RealLabelledData test_data = test_c[i].clone();
//					t2 = test_c[j].clone();
//					test_data.merge(t2);
//
//					int[] totalhits = new int[test_data.size()];
//					double[] fitness1 = new double[10];
//					double meanfitness1 = 0;
//					double[] fitness2 = new double[10];
//					double meanfitness2 = 0;
//					double[] hits = new double[10];
//					double meanhits = 0;
//					
//					// -- for each pair of classes, run 10 times
//
//					for (int k = 0; k < 10; k++)
//					{
//						System.out.print(".");
//						// first projection
//						TGAPopulation depop = new TGAPopulation(train_data);
//						depop.initPopulation();						
//				    	while (depop.runGeneration() >= 0);
//				    	
//				    	// second projection
//						RealLabelledData dproj = train_data.transform(depop.best);
//						TGAPopulation depop2 = new TGAPopulation(dproj);
//						depop2.initPopulation();
//						while (depop2.runGeneration() >= 0);
//														
//						LinProj p1 = depop.best;
//						LinProj p2 = depop2.best;
//
//						boolean r[] = TestResult.generalizeTest(train_data, test_data, p1, p2);
//						int nhits = 0;
//						
//						for (int ki = 0; ki < r.length; ki++)
//						{
//							if (r[ki] == true) // hit
//							{
//								nhits++;
//								totalhits[ki]++;
//							}
//						}
//						
//						// adding up errors for the run
//						fitness1[k] = depop.best.fitness;
//						meanfitness1 += depop.best.fitness;
//						fitness2[k] = depop2.best.fitness;
//						meanfitness2 += depop2.best.fitness;
//						hits[k] = nhits;
//						meanhits += nhits;
//						
//						// -- print info for global fiterror files
//						fiterror1.write(depop.best.fitness + " " + nhits + "\n");
//						fiterror2.write(depop2.best.fitness + " " + nhits + "\n");
//						
//						// -- for each run, print projection files
//						TestResult.dumpProjection(train_data, p1, p2, "_"+i+"_"+j+"_train"+k);
//						TestResult.dumpProjection(test_data, p1, p2, "_"+i+"_"+j+"_test"+k);
//						
//						
//					} // loop for running 10 times
//					
//					
//					
//					output.write("Results for Clusters "+i+" and "+j+"\n");
//					output.write("Fitness1 --  mean: "+meanfitness1/10+"  deviation: "+Maths.deviation(fitness1)+"\n");
//					output.write("Fitness1 --  mean: "+meanfitness2/10+"  deviation: "+Maths.deviation(fitness2)+"\n");
//					output.write("Hits     --  mean: "+meanhits/10+"  deviation: "+Maths.deviation(hits)+"\n");
//					output.write("Hits per case: ");
//					for (int ki = 0; ki < totalhits.length; ki++)
//						output.write(totalhits[ki] + " ");
//					output.write("\n\n\n");
//					System.out.println();
//				}// loop for running the cluster pairs (one loop for i and j)
//			
//			fiterror1.close();
//    		fiterror2.close();
//    		output.close();
//			
//			
//		}
//		catch(Exception e)
//		{
//			System.err.println("ERROR in the main loop: " + e.getMessage());
//			e.printStackTrace();
//		}
//		
//		// O que eu quero de dados:
//		// -- para cada par de classes, media dos acertos
//		// -- para cada par de classes, media/variancia da fitness
//		// -- para cada par de classes, numero de erros de cada teste (6 testes para cada)
//		
//	}
//	
//	static public void GenTest()
//	{
//		// Testes 1 e 2 de generalidade
//		Parameter P = Parameter.getInstance();
//
//		
//		String trainfile = P.getParam("train_datafile");
//		String testfile = P.getParam("test_datafile");
//		String prefix = P.getParam("file_prefix");
//		
//		RealLabelledData train = new RealLabelledData();
//		train.readFile(trainfile);
//		RealLabelledData train_c[] = train.breakClass();
//		
//		RealLabelledData test = new RealLabelledData();
//		test.readFile(testfile);
//		RealLabelledData test_c[] = test.breakClass();
//		
//		
//				
//		try
//		{
//			// General fitness/hits file:
//    		BufferedWriter fiterror1 = new BufferedWriter(new FileWriter(prefix + "_fiterror1"));	
//    		BufferedWriter fiterror2 = new BufferedWriter(new FileWriter(prefix + "_fiterror2"));
//			BufferedWriter output = new BufferedWriter(new FileWriter(prefix + "_output"));
//			
//			// There is some error with Class 10... check it later
//			for(int i = 1; i < 9; i++) //1 to 9 (10)
//				for(int j = i+1; j < 10; j++) // i+1 to 10 (11)
//				{
//					System.out.print("Running Clusters " + i + " and "+j+":");
//					// selecting the data
//					RealLabelledData train_data = train_c[i].clone();
//					RealLabelledData t2 = train_c[j].clone();
//					train_data.merge(t2);
//					
//					RealLabelledData test_data = test_c[i].clone();
//					t2 = test_c[j].clone();
//					test_data.merge(t2);
//
//					int[] totalhits = new int[test_data.size()];
//					double[] fitness1 = new double[10];
//					double meanfitness1 = 0;
//					double[] fitness2 = new double[10];
//					double meanfitness2 = 0;
//					double[] hits = new double[10];
//					double meanhits = 0;
//					
//					// -- for each pair of classes, run 10 times
//
//					for (int k = 0; k < 10; k++)
//					{
//						System.out.print(".");
//						// first projection
//						DEPopulation depop = new DEPopulation(train_data);
//						depop.initPopulation();						
//				    	while (depop.runGeneration() >= 0);
//				    	
//				    	// second projection
//						RealLabelledData dproj = train_data.transform(depop.best);
//						DEPopulation depop2 = new DEPopulation(dproj);
//						depop2.initPopulation();
//						while (depop2.runGeneration() >= 0);
//														
//						LinProj p1 = depop.best;
//						LinProj p2 = depop2.best;
//
//						boolean r[] = TestResult.generalizeTest(train_data, test_data, p1, p2);
//						int nhits = 0;
//						
//						for (int ki = 0; ki < r.length; ki++)
//						{
//							if (r[ki] == true) // hit
//							{
//								nhits++;
//								totalhits[ki]++;
//							}
//						}
//						
//						// adding up errors for the run
//						fitness1[k] = depop.best.fitness;
//						meanfitness1 += depop.best.fitness;
//						fitness2[k] = depop2.best.fitness;
//						meanfitness2 += depop2.best.fitness;
//						hits[k] = nhits;
//						meanhits += nhits;
//						
//						// -- print info for global fiterror files
//						fiterror1.write(depop.best.fitness + " " + nhits + "\n");
//						fiterror2.write(depop2.best.fitness + " " + nhits + "\n");
//						
//						// -- for each run, print projection files
//						TestResult.dumpProjection(train_data, p1, p2, "_"+i+"_"+j+"_train"+k);
//						TestResult.dumpProjection(test_data, p1, p2, "_"+i+"_"+j+"_test"+k);
//						
//						
//					} // loop for running 10 times
//					
//					
//					
//					output.write("Results for Clusters "+i+" and "+j+"\n");
//					output.write("Fitness1 --  mean: "+meanfitness1/10+"  deviation: "+Maths.deviation(fitness1)+"\n");
//					output.write("Fitness1 --  mean: "+meanfitness2/10+"  deviation: "+Maths.deviation(fitness2)+"\n");
//					output.write("Hits     --  mean: "+meanhits/10+"  deviation: "+Maths.deviation(hits)+"\n");
//					output.write("Hits per case: ");
//					for (int ki = 0; ki < totalhits.length; ki++)
//						output.write(totalhits[ki] + " ");
//					output.write("\n\n\n");
//					System.out.println();
//				}// loop for running the cluster pairs (one loop for i and j)
//			
//			fiterror1.close();
//    		fiterror2.close();
//    		output.close();
//			
//			
//		}
//		catch(Exception e)
//		{
//			System.err.println("ERROR in the main loop: " + e.getMessage());
//			e.printStackTrace();
//		}
//		
//		// O que eu quero de dados:
//		// -- para cada par de classes, media dos acertos
//		// -- para cada par de classes, media/variancia da fitness
//		// -- para cada par de classes, numero de erros de cada teste (6 testes para cada)
//		
//	}
//	
//	// Testing if "dump projection" and "testing generality" are working
//	static public void DEtest1(RealLabelledData d, RealLabelledData t)
//	{
//		DEPopulation depop = new DEPopulation(d);
//		depop.initPopulation();
//		
//		try
//    	{
//    		BufferedWriter outfile = new BufferedWriter(new FileWriter("evo_p1"));	
//    		while (depop.runGeneration() >= 0)
//    			outfile.write(depop.currgen + " " + depop.avg_fitness[depop.currgen] + " " + depop.max_fitness[depop.currgen]+"\n");
//    		outfile.close();
//		}	
//    	catch(IOException e)
//    	{
//    		System.err.print(e.getMessage());
//    	}
//
//		RealLabelledData dproj = d.transform(depop.individual.get(0));
//		DEPopulation depop2 = new DEPopulation(dproj);
//		depop2.initPopulation();
//    	
//		try
//    	{
//    		BufferedWriter outfile = new BufferedWriter(new FileWriter("evo_p2"));	
//    		while (depop2.runGeneration() >= 0)
//    			outfile.write(depop2.currgen + " " + depop2.avg_fitness[depop2.currgen] + " " + depop2.max_fitness[depop2.currgen]+"\n");
//    		outfile.close();
//		}	
//    	catch(IOException e)
//    	{
//    		System.err.print(e.getMessage());
//    		e.printStackTrace();
//    	}
//		
//				
//		LinProj p1 = depop.best;
//		LinProj p2 = depop2.best;
//		
//
//		if (t != null)
//		{
//			boolean r[] = TestResult.generalizeTest(d, t, p1, p2);
//			for (int i = 0; i < r.length; i++)
//				System.out.print(r[i] + " ");
//			System.out.println();
//			TestResult.dumpProjection(t, p1, p2, "teste");
//		}
//		
//		TestResult.dumpProjection(d, p1, p2, "");
//		
//		
//	}
//	
//	static public void TGAtest1(RealLabelledData d)
//	{
//		TGAPopulation tgapop = new TGAPopulation(d);
//		tgapop.initPopulation();
//		while (tgapop.runGeneration() >= 0)
//			System.out.println(tgapop.currgen + " " + tgapop.avg_fitness[tgapop.currgen] + " " + tgapop.max_fitness[tgapop.currgen]);
//	
//		RealLabelledData dproj = d.transform(tgapop.best);
//
//		TGAPopulation tgapop2 = new TGAPopulation(dproj);
//		tgapop2.initPopulation();
//
//		while (tgapop2.runGeneration() >= 0)
//			System.out.println(tgapop2.currgen + " " + tgapop2.avg_fitness[tgapop2.currgen] + " " + tgapop2.max_fitness[tgapop2.currgen]);		
//		
//		Iterator<ObservationReal> it1 = d.data.iterator();
//		Iterator<ObservationReal> it2 = dproj.data.iterator();
//
//		LinProj p1 = tgapop.best;
//		LinProj p2 = tgapop2.best;
//		
//		//while (it1.hasNext())
//		//{
//		//	System.out.println(p1.project(it1.next().d)+" "+p2.project(it2.next().d));
//		//}
//		
//	}
//
//	static public void TGAtest0(RealLabelledData d)
//	{
//		TGAGenome t1 = new TGAGenome(d.attr_num);
//		TGAGenome t2 = new TGAGenome(d.attr_num);
//		
//		t1.init(d, 4, 0.7);
//		t1.eval(d);		
//		System.out.println(t1.dump());
//
//		t1.mutate();
//		t1.eval(d);		
//		System.out.println(t1.dump());
//		
//		t1.mutate();
//		t1.eval(d);		
//		System.out.println(t1.dump());
//		
//		
//	}
//	
//	static public void DEtest(RealLabelledData d)
//	{
//		DEPopulation depop = new DEPopulation(d);
//		depop.initPopulation();
//		while (depop.runGeneration() >= 0)
//			System.out.println(depop.currgen + " " + depop.avg_fitness[depop.currgen] + " " + depop.max_fitness[depop.currgen]);
//		
//		
//		RealLabelledData dproj = d.transform(depop.individual.get(0));
//
//		DEPopulation depop2 = new DEPopulation(dproj);
//		depop2.initPopulation();
//
//		while (depop2.runGeneration() >= 0)
//			System.out.println(depop2.currgen + " " + depop2.avg_fitness[depop2.currgen] + " " + depop2.max_fitness[depop2.currgen]);		
//		
//		Iterator<ObservationReal> it1 = d.data.iterator();
//		Iterator<ObservationReal> it2 = dproj.data.iterator();
//		int i = 0;
//		LinProj p1 = depop.individual.get(0);
//		LinProj p2 = depop2.individual.get(0);
//		
//		//while (it1.hasNext())
//		//{
//		//	System.out.println(p1.project(it1.next().d)+" "+p2.project(it2.next().d));
//		//}
//		
//	}
//	
//	// Try 100 random projections for each dimension, keeping the best
//	static public void randomSearch(RealLabelledData d)
//	{
//		int attrsize = d.get_attr();
//		Random dice = new Random();
//		LinProj bestproj = null;
//		double bestDCS = 0;
//		int[] y = d.getClasses();
//		
//		
//		for (int i = 0; i < 1000; i++)
//		{
//			double[] w = new double[attrsize];
//			for (int j = 0; j < attrsize; j++)
//				w[j] = dice.nextGaussian();
//			LinProj test = new LinProj(w);
//
//			double[] x = new double[y.length];
//			int j = 0;
//			Iterator<ObservationReal> it = d.data.iterator();
//			while (it.hasNext())
//			{
//				ObservationReal u = it.next();
//				x[j] = test.project(u.d);
//				j++;
//			}
//			double result = Dcs.calculate(x, y);
//			if (result > bestDCS)
//			{
//				bestDCS = result;
//				bestproj = test.clone();
//				System.out.print("\n" + result + " ");
//			}
//			else
//				System.out.print(".");
//			
//		}
//	
//		System.out.print("\n\n\n");
//		
//		RealLabelledData d2 = d.transform(bestproj);
//		bestDCS = 0;
//		
//		for (int i = 0; i < 1000; i++)
//		{
//			double[] w = new double[attrsize];
//			for (int j = 0; j < attrsize; j++)
//				w[j] = dice.nextGaussian();
//			LinProj test = new LinProj(w);
//
//			double[] x = new double[y.length];
//			int j = 0;
//			Iterator<ObservationReal> it = d.data.iterator();
//			while (it.hasNext())
//			{
//				ObservationReal u = it.next();
//				x[j] = test.project(u.d);
//				j++;
//			}
//			double result = Dcs.calculate(x, y);
//			if (result > bestDCS)
//			{
//				bestDCS = result;
//				bestproj = test.clone();
//				System.out.print("\n" + result + " ");
//			}
//			else
//				System.out.print(".");
//			
//		}
//	
//	}
	
	
}
