/**
 * Holds real valued, observations with assigned labels. 
 * The observations have no missing attributes.
 * Labels are numbered from 1.
 * 
 */

package data;

import java.util.ArrayList;
import java.util.Iterator;
import projection.*;

public class RealLabelledData implements Iterable<ObservationReal> {
	
	int label_total; // total number of different labels
	int attr_total; // number of attributes in the data;

	ArrayList<ObservationReal> observations;
	ArrayList<Integer> labels;
	
	/**
	 * Empty constructor
	 */
	public RealLabelledData()
	{
		label_total = 0;
		attr_total = 0;
		
		observations = new ArrayList<ObservationReal>();
		labels = new ArrayList<Integer>();
	}
		
	/* Deep Copy Constructor */
	public RealLabelledData(RealLabelledData copy)
	{
		label_total = copy.label_total;
		attr_total = copy.attr_total;
		
		observations = new ArrayList<ObservationReal>();
		for (ObservationReal aux: copy.observations)	
		{
			observations.add(new ObservationReal(aux));
		}
		
		labels = new ArrayList<Integer>();
		for (Integer aux: copy.labels)
		{
			labels.add(new Integer(aux));
		}		
	}
	
	/**
	 * Returns the total number of elements in data reader
	 * @return
	 */
	public int size()
	{
		return observations.size();
	}
	
	/** 
	 * Returns the total number of elements in class c
	 * @param c class to count the number of elements
	 * @return number of classes
	 */
	public int size(int c)
	{
		if (c == 0)
			return observations.size();
		else
			return labels.get(c); // labels contains the number of elements in the label "i"
	}

	/**
	 * Returns the total number of attributes for this data set
	 * @return
	 */
	public int getTotalAttributes()
	{
		return attr_total;
	}
	
	/**
	 * Add all the observations of another Data Collection into this one.
	 * 
	 * @param dr: The data to be merged
	 */
	public void merge(RealLabelledData dr)
	{
		for (ObservationReal aux: dr)
			this.addObservation(aux);		
	}

	@Override
	public String toString()
	{
		return "RealLabelledData: "+observations.size()+" observations, "+attr_total+" attributes and "+label_total+" labels.";
	}
	
	/**
	 * Creates an ortogonal data projection, based on the linear projection P. The orthogonal 
	 * data projection is created as P(Pt*P)^-1*Pt*I
	 * @param p
	 * @return
	 */
	
	/**
	 * Calculates an orthogonal projection based on the linear projection P. 
	 * The orthogonal projection transforms the data so that it removes the information 
	 * selected by P.
	 * 
	 * The orthogonal data projection is created as P(Pt*P)^-1*Pt*I
	 * 
	 * @param p
	 * @return A new Real Labelled Data created by the projection.
	 */
	public RealLabelledData ortogonalProjection(LinProj p)
	{
		// Step one, calculating the transformation matrix:
		// TODO: this step should not be done here.
		double denom = 0;
		double[][] tmatrix = new double[p.w.length][p.w.length];
		for (int i = 0; i < p.w.length; i++)
		{
			denom += p.w[i] * p.w[i];
			for (int j = 0; j < p.w.length; j++)
			{
				tmatrix[i][j] = p.w[i]*p.w[j];
			}		
		}
				
		for (int i = 0; i < p.w.length; i ++)
			for (int j = 0; j < p.w.length; j ++)
			{
				tmatrix[i][j] = - tmatrix[i][j]/denom;
				if (i==j)
					tmatrix[i][j] = 1 + tmatrix[i][j];
			}
		
		// Step two: recalculating values for each point based on transformation matrix
	
		RealLabelledData ret = new RealLabelledData();
		
		for (ObservationReal aux: this)
		{
			double[] d = new double[aux.getAttributeSize()];

			for (int i = 0; i < d.length; i++)
				for (int j = 0; j < d.length; j++)
				{
					d[i] += aux.getAttribute(j)*tmatrix[i][j];
				}
			ret.addObservation(new ObservationReal(aux.getLabel(),d));
		}
		
		return ret;		
	}	
	
	/**
	 * returns an array with the list of labels in this Data collection
	 * 
	 * @return
	 * 
	 */
	public int[] getLabelList()
	{
		int ret[] = new int[label_total];
		int i = 0;
		
		for (int j = 0; j < labels.size(); j++)
		{
			if (labels.get(i) > 0)
				ret[i] = j;
		}
		
		return ret;
	}

	@Override
	public Iterator<ObservationReal> iterator() {
		return observations.iterator();
	}

	/** 
	 * Adds a new observation
	 * @param aux
	 */
	public void addObservation(ObservationReal aux) {
		observations.add(aux);
		
		// Making sure the label vector has enough space
		if (labels.size() <= aux.getLabel())
			for (int i = labels.size(); i <= aux.getLabel(); i++)
				labels.add(0);

		// updating label count
		Integer lc = labels.get(aux.getLabel())+1;		
		labels.set(aux.getLabel(), lc);
		
		if (lc == 1) // new label
			label_total++;
	}

	/**
	 * Gets an observation based on its index;
	 * @param index
	 * @return
	 */
	public ObservationReal get(Integer index) {		
		return observations.get(index);
	}

	/**
	 * Gets an ArrayList with all the observations.
	 * This arraylist can be modified (but not the individual observations)
	 * 
	 * @return
	 */
	public ArrayList<ObservationReal> getArrayList() {
		ArrayList<ObservationReal> ret = new ArrayList<ObservationReal>();
		for (ObservationReal aux: observations)
			ret.add(aux);
		
		return ret;
	}
	
	/**
	 * Returns an Arraylist with only the labels of each observation
	 * (the Y column of the data)
	 * @return
	 */
	public ArrayList<Integer> getLabelsArray() {
		ArrayList<Integer> ret = new ArrayList<Integer>();
		for (ObservationReal aux: observations)
			ret.add(aux.getLabel());
		
		return ret;
	}
}
