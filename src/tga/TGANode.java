package tga;

import java.util.Random;
import toolbox.IDGenerator;

public class TGANode {

	/* node internal variables */
	Boolean isTerminal;	
	public Long uniqueID;
	
	public int maxdepth; // maximum depth
	public int curdepth; // current depth. Root = 1
	public int maxindex; // the maximum value for an index
	
	/* Node Contents */
	public TGANode leftChild = null;
	public TGANode rightChild = null;
	public double weight = 0.5; 
	// weight of the children, if node // TODO: Not used right now, so it is fixed.
	// If terminal, determines whether this is a positive or negative terminal
	public int parameterIndex; // index of the parameter, if terminal
		
	/**
	 * Empty constructor - generates an "empty" node.
	 *
	 */
	public TGANode()
	{		
		IDGenerator i = IDGenerator.getInstance();
		uniqueID = i.newID();
		curdepth = -1; // unitiated node
	}

	/**
	 * Copy tree function - recursively copies a tree and send 
	 * it back
	 */
	public TGANode clone()
	{
		IDGenerator i = IDGenerator.getInstance();
		TGANode newn = new TGANode();
		newn.isTerminal = isTerminal;
		newn.parameterIndex = parameterIndex;
		newn.uniqueID = i.newID();
		newn.weight = weight;
		
		newn.curdepth = curdepth;
		newn.maxdepth = maxdepth;
		newn.maxindex = maxindex;

		// Nodes with only one child don't make sense.
		if (!isTerminal)
		{
			newn.leftChild = leftChild.clone();
			newn.rightChild = rightChild.clone();
		}
		
		return newn;
	}
	
	/**
	 * Generate a random tree starting from this node. 
	 * Recursive Function. A tree with only the root 
	 * has depth = 1.
	 * 
	 * Prob is the probability that each node will 
	 * not be a terminal node. If prob = 1, then the 
	 * tree is a full tree.
	 * 
	 * thisdepth is the value where the function is started, 
	 * and is compared with maxdepth to decide when to stop.
	 * 
	 * 
	 */
	public void generateTree(double prob)
	{
		if (curdepth == -1)
		{
			System.err.println("ERRO: You need to pass initialization parameters to generate this tree");
			System.exit(1);
		}
		generateTree(maxdepth, maxindex, curdepth, prob);
	}
	public void generateTree(int maxd, int maxi, int depth, double prob)
	{
		Random dice = new Random();
		maxdepth = maxd;
		maxindex = maxi;
		curdepth = depth;
		
		// try to generate childs
		if ((curdepth < maxdepth) && (dice.nextDouble() < prob))
		{
			weight = 0.5;
			leftChild = new TGANode();
			leftChild.generateTree(maxd, maxi, depth+1, prob);
			rightChild = new TGANode();
			rightChild.generateTree(maxd, maxi, depth+1, prob);
			isTerminal = false;
		}
		else
		{
			parameterIndex = dice.nextInt(maxindex);
			isTerminal = true;
			// In terminals, the weight determines whether this is a positive or negative terminal
			if (dice.nextBoolean())
				weight = 1;
			else
				weight = -1;
		}		
	}
	

	
	public double[] getArray()
	{
		double[] ret = new double[maxindex];
		
		if (isTerminal)
			{
				ret[parameterIndex]=weight;
			}
		else
		{
			double[] lchild = leftChild.getArray();
			double[] rchild = rightChild.getArray();
			
			for(int i = 0; i < ret.length; i++)
			{
				ret[i] = lchild[i]*weight + rchild[i]*(1-weight);
			}
		}
		
		return ret;
	}
	
	/**
	 * alternative getArray function for additive genomes
	 * @return
	 */
	public double[] getArray2()
	{
		double[] ret = new double[maxindex];
		
		if (isTerminal)
			{
				ret[parameterIndex]=weight;
			}
		else
		{
			double[] lchild = leftChild.getArray2();
			double[] rchild = rightChild.getArray2();
			
			for(int i = 0; i < ret.length; i++)
			{
				ret[i] = lchild[i] + rchild[i];
			}
		}
		
		return ret;
	}

	
	/**
	 * walks down the tree from here, recursively. If this node is at maximum 
	 * depth and has children, produce the weight array for this node, and replaces it 
	 * with a terminal with the highest index.
	 * 
	 * also corrects depth. To be used mostly with crossover
	 * 
	 * true is returned if the tree was modified
	 */
	public boolean collapse(int depth)
	{
		curdepth = depth;
		if (isTerminal)
			return false;
		if (curdepth < maxdepth)
		{
			boolean ret;
			ret = leftChild.collapse(depth+1);
			ret = rightChild.collapse(depth+1) && ret;
			return ret;
		}
		
		// not a terminal, and already at maxdepth
		// turn into terminal
		double[] t = getArray2(); //FIXME: must stop this get array/get array 2 nonsense -- probably will stick with 2
		double maxweight = 0;
		weight = 0;
		parameterIndex = 0;
		for (int i = 0; i < maxindex; i++)
			if (Math.abs(t[i]) > maxweight)
			{
				maxweight = Math.abs(t[i]);
				parameterIndex = i;
				weight = t[i];
			}
		isTerminal = true;
		leftChild = null; //TODO: "delete" function
		rightChild = null; //TODO: "delete" function		
		return true;
	}
	
	
	/**
	 * Applies the tree crossover operator to this and another tree, modifying both.
	 * A parent, non terminal node is randomly chosen for each tree (can't crossover root nodes).
	 * A random child of one parent is swapped with the random child of the other parent.
	 * Collapse is applied to both children.
	 * 
	 * @param root root node of the tree to apply crossover to.
	 */
	public boolean crossover(TGANode root)
	{
		Random dice = new Random();
		
		int dep_1 = dice.nextInt(maxdepth);
		int dep_2 = dice.nextInt(maxdepth);
		
		TGANode fnode1 = this;
		TGANode fnode2 = root;
		
		// if any of the roots are terminals, ignore the crossover
		if (fnode1.isTerminal || fnode2.isTerminal)
			return false;
		
		
		// Choosing the break node. Going beyond max depth loop back to the root.
		for (int i = 0; i < dep_1; i++)
		{
			if (dice.nextDouble() > 0.5)
				fnode1 = fnode1.leftChild;
			else
				fnode1 = fnode1.rightChild;
			
			if (fnode1.isTerminal)
				fnode1 = this;
		}		
		for (int i = 0; i < dep_2; i++)
		{
			if (dice.nextDouble() > 0.5)
				fnode2 = fnode2.leftChild;
			else
				fnode2 = fnode2.rightChild;
			
			if (fnode2.isTerminal)
				fnode2 = root;
		}
		
		// i should have two non terminal nodes in hand. showtime:
		int choice = dice.nextInt(4);
		TGANode t;
		switch(choice)
		{
		case 0: // right right
			t = fnode2.rightChild;
			fnode2.rightChild = fnode1.rightChild;
			fnode1.rightChild = t;
			break;
		case 1: // right left
			t = fnode2.leftChild;
			fnode2.leftChild = fnode1.rightChild;
			fnode1.rightChild = t;
			break;
		case 2: // left right
			t = fnode2.rightChild;
			fnode2.rightChild = fnode1.leftChild;
			fnode1.leftChild = t;
			break;
		case 3: // left left
			t = fnode2.leftChild;
			fnode2.leftChild = fnode1.leftChild;
			fnode1.leftChild = t;
			break;
		}
		
		// checking that the nodes don't break depth limits.
		fnode1.collapse(fnode1.curdepth);
		fnode2.collapse(fnode2.curdepth);
		
		return true;
	}
	
	/**
	 * Chooses a random node and recreates the tree from that node.
	 */
	public void mutate(double fullrate)
	{
		Random dice = new Random();
		
		int dep_1 = dice.nextInt(maxdepth);
		TGANode fnode1 = this;
		
		// Choosing the break node. Going beyond max depth loop back to the root.
		for (int i = 0; i < dep_1; i++)
		{
			if (fnode1.isTerminal)
				fnode1 = this;
			else
				if (dice.nextDouble() > 0.5)
					fnode1 = fnode1.leftChild;
				else
					fnode1 = fnode1.rightChild;
		}		

		fnode1.generateTree(fullrate);
		
	}
	
    /**
     * Returns a text string that contains an ASCII 
     * representation of this tree. verbose, if set 
     * to true, prints each node's contents.
     */
    public String dumptree(boolean verbose)
    {
    	return dumptree(0,verbose);
    }
    public String dumptree(int tab, boolean verbose)
    {
    	String ret = "";
    	String tabulation = "";
    	for (int i = 0; i < tab; i++)
    		tabulation = tabulation + "  ";
    	
    	ret = ret + tabulation + "(Node " + uniqueID + ")";
    	if (isTerminal)
    	{
    		ret = ret + " Terminal: " + parameterIndex;
    	}
    	else
    	{
    		ret = ret + " Non-Term: " + weight;
    	}
    	ret = ret + "\n";
    	
    	if (!isTerminal)
    	{
    		ret = ret + tabulation + leftChild.dumptree(tab + 1, verbose);
    		ret = ret + tabulation + rightChild.dumptree(tab + 1, verbose);
    	}
    		
    	return ret;
    }
	
    /**
     * Count the number of nodes and introns in this 
     * tree. Recursive. Introns are defined as the number 
     * of nodes that don't contribute to the final answer, 
     * because they are under zero weight.
     *  
     * @return
     * ret[0] is the total number of nodes, and 
     * ret[1] is the total number of introns;
     */
    public int[] countNodes()
    {
    	int ret[] = new int[2];
    	
    	if (isTerminal)
    	{
    		ret[0] = 1;
    		ret[1] = 0;
    	}
    	else
    	{
    		int lret[] = leftChild.countNodes();
    		int rret[] = rightChild.countNodes();
    		
    		ret[0] = lret[0] + rret[0] + 1;
    		
    		if (weight == 1.0)
    		{
    			ret[1] = lret[1] + rret[0];
    		}
    		else if (weight == 0.0)
    		{
    			ret[1] = lret[0] + rret[1];
    		}
    		else 
    		{
    			ret[1] = lret[1] + rret[1];
    		}
    		
    	}
    	
    	return ret;
    }
        
}
