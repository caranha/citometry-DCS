package toolbox;

/**
 * Subclass of parameter, includes global Variables for parameters that are often used in this system -- to avoid string comparison too many times
 * 
 * @author claus.aranha
 *
 */

public class GAVISparam extends Parameter {

	protected static GAVISparam instance;
	
	// 0 for standard DCS, 1 for side-penalty DCS
	public int divmeasure = 1;
	public double kerneldivisor = 50;
	
	// Number of repeats for statistic testing
	public int repeats = 10;
	
	// DE Parameters
	public int DE_pop = 50;
	public int DE_gen = 300;
	public double DE_F = 0.8;
	public double DE_C = 0.5;
	
	protected GAVISparam() 
	{
		super();
	}
	
	public static synchronized GAVISparam getInstance()
	{
		if(instance == null) {
			instance = new GAVISparam();
	      }	
		
		return instance;
	}
	
	
	
	/** 
	 * When loading a parameter file, standard parameters are also set using getParam
	 */
	public void load(String filename) throws Exception
	{
		super.load(filename);		
		
		// after loading the parameters, init the global parameters
		
		String param;

		// DCS parameters
		param = getParam("diversity measure");
		if (param != null)
			divmeasure = Integer.parseInt(param);

		param = getParam("kernel size divisor");
		if (param != null)
			kerneldivisor = Double.parseDouble(param);
		
		// Experimental parameters
		param = getParam("repeat runs");
		if (param != null)
			repeats = Integer.parseInt(param);
		
		// DE Parameters
		param = getParam("DE_population");
		if (param != null)
			DE_pop = Integer.parseInt(param);
		param = getParam("DE_generation");
		if (param != null)
			DE_gen = Integer.parseInt(param);
		param = getParam("DE_F");
		if (param != null)
			DE_F = Double.parseDouble(param);
		param = getParam("DE_C");
		if (param != null)
			DE_C = Double.parseDouble(param);

		
	}
	
	
	
	
}
