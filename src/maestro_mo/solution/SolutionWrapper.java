/**
 * Copyright 2018 Felipe Hernández
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the 
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package maestro_mo.solution;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.concurrent.LinkedBlockingQueue;

import maestro_mo.Optimizer;
import maestro_mo.Objective;

/**
 * Wraps a {@link Solution} implementation to include additional information such as the index of 
 * the {@link maestro_mo.gen.Generator} that created the solution, the rank of the solution in a 
 * non-dominated population, and other values that generators might need to associate with the
 * solution.
 * 
 * @author Felipe Hernández
 */
public class SolutionWrapper implements Solution
{
	
	// --------------------------------------------------------------------------------------------
	// Constants
	// --------------------------------------------------------------------------------------------
	
	/**
	 * Name of the attribute that stores the weight of the solution
	 */
	public final static String WEIGHT = "Weight";

	// --------------------------------------------------------------------------------------------
	// Attributes
	// --------------------------------------------------------------------------------------------
	
	/**
	 * Unique index of the solution. -1 if not index has been assigned yet.
	 */
	private int index;
	
	/**
	 * Instance of the MAESTRO-MO controller class
	 */
	private Optimizer optimizer;
	
	/**
	 * The solution being wrapped
	 */
	private Solution solution;
	
	/**
	 * 	A hash table that stores additional values as may be required by different users
	 */
	private Hashtable<String, Double> otherValues;
	
	/**
	 * Collection that references other solutions with a lower hierarchical level
	 */
	private Collection<SolutionWrapper> children;
	
	/**
	 * The index of the generator algorithm that created the solution
	 */
	private int genIndex;
	
	/**
	 * The list of ranks assigned in a non-dominated population throughout the optimization 
	 * process. Ranks are equal or larger than 1, or -1 if the solution is not in the population.
	 */
	private ArrayList<Integer> rankHistory;
	
	/**
	 * An empty string ("") in most cases. Not empty if the generating root was defined by the user 
	 * and a special label was assigned to identify the resulting solution.
	 */
	private String userLabel;

	// --------------------------------------------------------------------------------------------
	// Constructors
	// --------------------------------------------------------------------------------------------
	
	/**
	 * @param solution	{@link #solution}
	 */
	public SolutionWrapper(Solution solution)
	{
		optimizer		= null;
		this.solution	= solution;
		otherValues		= new Hashtable<String, Double>();
		children		= new ArrayList<SolutionWrapper>();
		this.genIndex	= -999;
		rankHistory		= new ArrayList<>();
		userLabel		= "";
	}
	
	/**
	 * @param optimizer	{@link #optimizer}
	 * @param solution	{@link #solution}
	 */
	public SolutionWrapper(Optimizer optimizer, Solution solution)
	{
		this.optimizer	= optimizer;
		this.solution	= solution;
		otherValues		= new Hashtable<String, Double>();
		children		= new ArrayList<SolutionWrapper>();
		this.genIndex	= -999;
		rankHistory		= new ArrayList<>();
		userLabel		= "";
	}
	
	/**
	 * @param optimizer	{@link #optimizer}
	 * @param solution	{@link #solution}
	 * @param genIndex	{@link #genIndex}
	 */
	public SolutionWrapper(Optimizer optimizer, Solution solution, int genIndex)
	{
		this.optimizer	= optimizer;
		this.solution	= solution;
		otherValues		= new Hashtable<String, Double>();
		children		= new ArrayList<SolutionWrapper>();
		this.genIndex	= genIndex;
		rankHistory		= new ArrayList<>();
		userLabel		= "";
	}
	
	// --------------------------------------------------------------------------------------------
	// Methods
	// --------------------------------------------------------------------------------------------
	
	/**
	 * @return {@link #index}
	 */
	public int getIndex()
	{
		return index;
	}

	/**
	 * @param index {@link #index}
	 */
	public void setIndex(int index)
	{
		this.index = index;
	}

	/**
	 * @return The solution being wrapped
	 */
	public Solution getSolution() 
	{
		return solution;
	}
	
	@Override
	public String getId()
	{
		return solution.getId();
	}

	@Override
	public Solution createNew(int id, ArrayList<Integer> discValues,
			ArrayList<Double> contValues, Object extra) 
	{
		return solution.createNew(id, discValues, contValues, extra);
	}

	@Override
	public ArrayList<Integer> getDiscValues() 
	{
		return solution.getDiscValues();
	}

	@Override
	public ArrayList<Double> getContValues() 
	{
		return solution.getContValues();
	}

	@Override
	public boolean isValid()
	{
		return solution.isValid();
	}

	@Override
	public String getReportHeader() 
	{
		return solution.getReportHeader();
	}

	@Override
	public String getReport() 
	{
		return solution.getReport();
	}

	@Override
	public double getFitness(int objective) 
	{
		return solution.getFitness(objective);
	}

	@Override
	public int compareTo(int objective, Solution other) 
	{
		return solution.compareTo(objective, other);
	}
	
	@Override
	public boolean optimizationConverged() 
	{
		return solution.optimizationConverged();
	}
	
	/**
	 * Determines if either solution dominates the other or no. A solution dominates another if 
	 * it is better than the other according to at least one objective and is not worse in all the
	 * others.
	 * @param other The other solution to compare with
	 * @param objectives The list of objective to perform the comparison
	 * @return 0 if there is no dominant solution. A positive number indicating the number of 
	 * objectives in which this solution beats the other if this is the dominant. A negative number 
	 * indicating the negative number of objectives in which the other solution beats this one if
	 * the other is the dominant.
	 */
	public int dominates(Solution other, ArrayList<Objective> objectives)
	{
		if (objectives.size() == 0)
			return 0;
		
		boolean ok	= false;
		int obj		= 0;
		int better	= 0;
		int worse	= 0;
		while (!ok)
		{
			Objective objective	= objectives.get(obj);
			int compare			= objective.compare(this, other);
			better				+= compare > 0 ? 1 : 0;
			worse				+= compare < 0 ? 1 : 0;
			ok					= obj == objectives.size() - 1 || (better > 0 && worse > 0);
			obj					++;
		}
		if (better > 0 && worse > 0)
			return 0;
		if (better > 0)
			return better;
		return -worse;
	}
	
	/**
	 * Stores a value that can be retrieved using the {@link #getValue(String)} method
	 * @param name The identifier of the value
	 * @param value The value to be stored
	 */
	public void setValue(String name, double value)
	{
		otherValues.put(name, value);
	}
	
	/**
	 * @param name The identifier of the value
	 * @return A value corresponding to the provided identifier that was stored using the 
	 * {@link #setValue(String, double)} method. <code>null</code> if the value is not found.
	 */
	public double getValue(String name)
	{
		return otherValues.get(name);
	}
	
	/**
	 * Removes the value corresponding to the provided identifier that was stored using the 
	 * {@link #setValue(String, double)} method
	 * @param name The identifier of the value
	 */
	public void deleteValue(String name)
	{
		otherValues.remove(name);
	}
	
	/**
	 * @return The list of identifiers of the additional values
	 */
	public Enumeration<String> getValuesIds()
	{
		return otherValues.keys();
	}

	/**
	 * @return Collection that references other solutions with a lower hierarchical level
	 */
	public Collection<SolutionWrapper> getChildren() 
	{
		return children;
	}

	/**
	 * @param children Collection that references other solutions with a lower hierarchical level
	 */
	public void setChildren(Collection<SolutionWrapper> children) 
	{
		if(children == null)
			this.children = new ArrayList<SolutionWrapper>();
		else
			this.children = children;
	}
	
	/**
	 * Adds a child to the solution
	 * @param child The solution to add
	 */
	public void addChild(SolutionWrapper child)
	{
		children.add(child);
	}
	
	/**
	 * Clears the children of the solution
	 */
	public void clearChildren()
	{
		children.clear();
	}
	
	/**
	 * @return The height of the <code>SolutionWrapper</code> tree with this 
	 * <code>SolutionWrapper</code> as the root
	 */
	public int getHeight()
	{
		if(children.size() == 0)
			return 1;
		else
		{
			Iterator<SolutionWrapper> iter = children.iterator();
			int height = 0;
			while(iter.hasNext())
				height = Math.max(height, iter.next().getHeight());
			return height + 1;
		}
	}
	
	/**
	 * @return A list with all the descendants with depth-first order
	 */
	public ArrayList<SolutionWrapper> getDescendantsDF()
	{
		ArrayList<SolutionWrapper> list = new ArrayList<SolutionWrapper>();
		list.add(this);
		for(SolutionWrapper child : children)
			list.addAll(child.getDescendantsDF());
		return list;
	}
	
	/**
	 * @return A list with all the descendants with breadth-first order
	 */
	public ArrayList<SolutionWrapper> getDescendantsBF()
	{
		ArrayList<SolutionWrapper> list = new ArrayList<SolutionWrapper>();
		list.add(this);
		LinkedBlockingQueue<SolutionWrapper> queue = new LinkedBlockingQueue<SolutionWrapper>();
		for(SolutionWrapper child : children)
			queue.offer(child);
		while(!queue.isEmpty())
		{
			SolutionWrapper solution = queue.poll();
			Collection<SolutionWrapper> children = solution.getChildren();
			if(children != null)
				for(SolutionWrapper child : children)
					queue.offer(child);
			list.add(solution);
		}
		return list;
	}

	/**
	 * @return {@link #genIndex}
	 */
	public int getGenIndex()
	{
		return genIndex;
	}

	/**
	 * @param genIndex {@link #genIndex}
	 */
	public void setGenIndex(int genIndex)
	{
		this.genIndex = genIndex;
	}
	
	/**
	 * @return The identifier of the generator of this solution. Returns the {@link #userLabel} if
	 * it is not an empty string.
	 */
	public String getGeneratorId()
	{
		if (userLabel.equals(""))
			return optimizer.getGeneratorId(genIndex);
		else
			return userLabel;
	}
	
	/**
	 * @return The short identifier of the generator of this solution. Returns the 
	 * {@link #userLabel} if it is not an empty string.
	 */
	public String getGeneratorShortId()
	{
		if (userLabel.equals(""))
			return optimizer.getGeneratorShortId(genIndex);
		else
			return userLabel;
	}

	/**
	 * @return The rank in the non-dominated population. The rank is equal or greater than 1 if the 
	 * solution exists in the population or -1 if it does not.
	 */
	public int getRank()
	{
		if (rankHistory.size() == 0)
			return -1;
		else
			return rankHistory.get(rankHistory.size() - 1);
	}

	/**
	 * @param rank The rank in the non-dominated population. The rank is equal or greater than 1 if 
	 * the solution exists in the population or -1 if it does not.
	 */
	public void setRank(int rank)
	{
		rankHistory.add(rank);
		if (optimizer != null)
			optimizer.rankChange(this);
	}
	
	/**
	 * @return {@link #rankHistory}
	 */
	public ArrayList<Integer> getRankHistory()
	{
		return rankHistory;
	}
	
	/**
	 * @return {@link #userLabel}
	 */
	public String getUserLabel()
	{
		return userLabel;
	}

	/**
	 * @param userLabel {@link #userLabel}
	 */
	public void setUserLabel(String userLabel)
	{
		this.userLabel = userLabel;
	}
	
}
