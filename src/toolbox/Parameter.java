package toolbox;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Iterator;
import java.util.Vector;

/* Parameter Class
 * This is a generic class for parameter file parsing. 
 * The class should read a file, and then return
 * parameters given stringnames upon request.
 * 
 * The Parameter file is composed as follows. Each line is either:
 * <parameter name> = <parameter value>
 * Where parameter name and parameter value are both strings to be read, separated by an "=" sign.
 * 
 * or
 * 
 * # comment
 * Where any line beginning with a "#" sign is ignored (use it for comments in the parameter file).
 * 
 * Parameter name and values ignore case, and ignore leading/ending spaces. # after parameter values are also ignored.
 * 
 * The file will read the parameter file and store all the parameters in that file. When you want 
 * to use those parameters, invoke the "get instance" of Parameter, and then the "getParam(String)" method.
 * String here is the parameter name. If that parameter name does not exist, the function will return "null", 
 * else, it will return a string with the parameter value.
 * 
 * 
 */

/**
 * @author caranha
 *
 */
public class Parameter {
	
	protected static Parameter instance;
	
	Vector<String> value; //contain the values of stored parameters
	Vector<String> name; // contain the names of stored parameters
	static int dlevel = 0;
	
	protected Parameter() 
	{
		value = new Vector<String>();
		name = new Vector<String>();
	}
	
	public static synchronized Parameter getInstance()
	{
		return getInstance(0);
	}
	public static synchronized Parameter getInstance(int debuglevel)
	{
		if(instance == null) {
			instance = new Parameter();
	      }	
		dlevel = debuglevel;
		
		return instance;
	}	
	
	
	
	/* Clears all parameters */
	public void clear()
	{
		name.clear();
		value.clear();
	}
	
	/**
	 *  Loads a new set of parameters. 
	 * The new parameters are loaded in addition to any existing parameters
	 * Parameters with identical names are overwritten
	 * 
	 * Parameters must be in the format:
	 * PARAMETER_NAME = PARAMETER_VALUE
	 * 
	 * Where PARAMETER value is a string. The value ends at a #
	 * Character (which indicates comments). The value is trimmed.
	 * 
	 * Lines starting with # will be ignored
	 * 
	 */
	public void load(String filename) throws Exception
	{
		
		
    	BufferedReader reader;
    	String line = null;
    	 	
    	reader = 
    		new BufferedReader(new FileReader(new File(filename)));

    	while ((line = reader.readLine()) != null) 
    	{
    		
    		if (line.length() > 0 && line.charAt(0) != '#') // ignores lines beginning with #
    		{	
    			String[] input = line.split("="); // removes leading/ending spaces.
    			    			
    			input[0] = input[0].trim();
    			input[0] = input[0].toLowerCase(); // parameters are not case sensitive
    				
    			String paramval;
    			int clone = name.indexOf(input[0]);
    			try {
    				paramval = input[1].split("#")[0].trim(); // removes anything after a # sign
    			} catch (Exception e)
    			{
    				paramval = null;
    				System.err.println("Error reading parameter file " + e.getMessage() + ". Ignoring line:");
    				System.err.println("   \"" + line + "\"");    					
    			}

    			if (paramval != null)
    			{
    				if (clone == -1) // parameter hasn't been specified yet
    				{
    					name.add(input[0]);
    					value.add(paramval);
    				}
    				else
    				{
    					value.set(clone, paramval);
    				}
    			}	
    		
    		}
    	}
    	
    	reader.close();
	}
	
	/* Sets a new parameter, or change the value of an old one */
	public void setParam(String n, String v)
	{
		n = n.trim();
		n = n.toLowerCase();
		
		int index = name.indexOf(n);
		
		if (index == -1)
		{
			name.add(n);
			value.add(v);
		}
		else
		{
			value.set(index, v);
		}
	}
	
	/* Returns a parameter of the given name. 
	 * If no parameter of that name exists, returns null
	 */
	public String getParam(String x)
	{		
		x = x.trim();
		x = x.toLowerCase();
		
		int index = name.indexOf(x);
		if (index == -1) //there is no such parameter
		{
			if (dlevel == 1)
				System.err.println("Parameter "+x+" not found, using default values if possible.");
			return null;
		}
		else
			return value.get(index);
	}
	

	/**
	 * Generates a string with all contents in the parameter object
	 * @return
	 */
	public String dump()
	{
		String ret = "";
		System.err.println("Parameter Values for object " + this );

		Iterator<String> it_name = name.iterator();
		Iterator<String> it_value = value.iterator();
		
		while(it_name.hasNext())
		{
			ret = ret + "Parameter name: " + it_name.next() + " Parameter value: " + it_value.next() +"\n";
		}
		
		return ret;
	}
	
}
