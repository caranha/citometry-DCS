package jp.ac.tsukuba.conclave.cytometry.data;


/***
 * 
 * This data represents a observation in a classification problem.
 * All the attributes are existing real values.
 * 
 * The class holds the attribute values, and a class label.
 * 
 * @author claus.aranha
 *
 */

public class ObservationReal {

	int c; // class label
	double[] d; // attributes
	
	/**
	 * 
	 * @param label
	 * @param attributes
	 */
	public ObservationReal(int label, double[] attributes)
	{
		d = attributes;
		c = label;
	}
	
	/**
	 * Deep copy constructor
	 * @param copy
	 */
	public ObservationReal(ObservationReal copy)
	{
		c = copy.c;
		d = new double[copy.d.length];
		for (int i = 0; i < copy.d.length; i++)
			d[i] = copy.d[i];
	}
	
	public int getLabel()
	{
		return c;
	}
	
	public double getAttribute(int i)
	{
		return d[i];
	}
	
	/**
	 * Returns the number of attributes in this sample.
	 */
	public int getAttributeSize() {
		return d.length	;
	}
	
	/**
	 * Returns a vector with all the attribute values
	 * Do not change this vector.
	 * 
	 * @return
	 */
	public double[] getAttributeValues() {
		return d;
	}

	
	@Override
	public String toString()
	{
		String ret = "" + c;
		for (int i = 0; i < d.length; i++)
			ret = ret + "," + d[i];			
		return ret;
	}



}
