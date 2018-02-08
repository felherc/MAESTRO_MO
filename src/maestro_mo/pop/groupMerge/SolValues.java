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

package maestro_mo.pop.groupMerge;

import java.util.ArrayList;

import maestro_mo.Objective;
import maestro_mo.solution.Solution;
import maestro_mo.solution.SolutionWrapper;

/**
 * This class allows indexing the values related to a solution, both continuous and discrete, to
 * be rapidly accessed within the {@link GroupMergePopulation} class
 * @author Felipe Hernández
 */
public class SolValues implements Comparable<SolValues>
{

	// --------------------------------------------------------------------------------------------
	// Attributes
	// --------------------------------------------------------------------------------------------
	
	/**
	 * The list of discrete values of the solution
	 */
	private ArrayList<Integer> discValues;
	
	/**
	 * The list of continuous values of the solution
	 */
	private ArrayList<Double> contValues;
	
	// --------------------------------------------------------------------------------------------
	// Constructors
	// --------------------------------------------------------------------------------------------
	
	/**
	 * @param discValues The list of discrete values of the solution
	 * @param contValues The list of continuous values of the solution
	 */
	public SolValues(ArrayList<Integer> discValues, ArrayList<Double> contValues)
	{
		this.discValues = discValues;
		this.contValues = contValues;
	}
	
	/**
	 * @param solution The solution to get the values from
	 */
	public SolValues(Solution solution)
	{
		this.discValues = solution.getDiscValues();
		this.contValues = solution.getContValues();
	}
	
	/**
	 * Creates a registry entry for a solution's fitness values
	 * @param sol			The solution to get the values from
	 * @param objectives	The list of optimization objectives
	 */
	public SolValues(SolutionWrapper sol, ArrayList<Objective> objectives)
	{
		this.discValues	= null;
		this.contValues	= new ArrayList<>();
		for (Objective objective : objectives)
			if (!objective.isCustom())
				contValues.add(sol.getFitness(objective.getIndex()));
	}

	// --------------------------------------------------------------------------------------------
	// Methods
	// --------------------------------------------------------------------------------------------

	/**
	 * @return The list of discrete values of the solution
	 */
	public ArrayList<Integer> getDiscValues() 
	{
		return discValues;
	}

	/**
	 * @param discValues The list of discrete values of the solution
	 */
	public void setDiscValues(ArrayList<Integer> discValues) 
	{
		this.discValues = discValues;
	}

	/**
	 * @return The list of continuous values of the solution
	 */
	public ArrayList<Double> getContValues() 
	{
		return contValues;
	}

	/**
	 * @param contValues The list of continuous values of the solution
	 */
	public void setContValues(ArrayList<Double> contValues) 
	{
		this.contValues = contValues;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() 
	{
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((contValues == null) ? 0 : contValues.hashCode());
		result = prime * result
				+ ((discValues == null) ? 0 : discValues.hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) 
	{
		SolValues other = (SolValues) obj;
		boolean discEqual = discValues == null && other.getDiscValues() == null;
		discEqual = discEqual ? true : discValues.equals(other.getDiscValues());
		boolean contEqual = contValues == null && other.getContValues() == null;
		contEqual = contEqual ? true : contValues.equals(other.getContValues());
		return discEqual && contEqual;
	}

	@Override
	public int compareTo(SolValues other) 
	{
		if(equals(other))
			return 0;
		else
		{
			ArrayList<Integer> otherD =	other.getDiscValues();
			ArrayList<Double> otherC =	other.getContValues();
			int discComp = discValues == null && otherD == null ? 0 : 
				(discValues == null ? -1 : (otherD == null ? 1 : 
					discValues.hashCode() - otherD.hashCode()));
			int contComp = contValues == null && otherC == null ? 0 : 
				(contValues == null ? -1 : (otherC == null ? 1 : 
					contValues.hashCode() - otherC.hashCode()));
			return Math.abs(discComp) > Math.abs(contComp) ? discComp : contComp;
		}
	}
	
}
