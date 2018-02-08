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

package maestro_mo;

import java.util.Comparator;

import maestro_mo.solution.Solution;

/**
 * This class represents an objective of an optimization problem
 * @author Felipe Hernández
 */
public class Objective implements Comparator<Solution>
{
	
	// --------------------------------------------------------------------------------------------
	// Attributes
	// --------------------------------------------------------------------------------------------
	
	/**
	 * The unique index of the objective
	 */
	private int index;
	
	/**
	 * The identifier of the objective
	 */
	private String id;
	
	/**
	 * True if the numerical fitness value for this objective is to be maximized. False if it is to 
	 * be minimized.
	 */
	private boolean maximization;
	
	/**
	 * True if the evaluation of this objective is not performed by comparing a single numerical
	 * value but it is done in a customized way that is implemented in the solutions themselves. 
	 * False if a numerical fitness value is used instead.
	 */
	private boolean custom;
	
	// --------------------------------------------------------------------------------------------
	// Constructors
	// --------------------------------------------------------------------------------------------
	
	/**
	 * Creates a new numerical comparing optimization objective. Solutions are compared through 
	 * the values obtained in {@link maestro_mo.solution.Solution#getFitness}.
	 * @param index The unique index of the objective
	 * @param id The identifier of the objective
	 * @param maximization True if the numerical fitness value for this objective is to be 
	 * maximized. False if it is to be minimized.
	 */
	public Objective(int index, String id, boolean maximization)
	{
		this.index = index;
		this.id = id;
		this.maximization = maximization;
		custom = false;
	}
	
	/**
	 * Creates a new custom optimization objective. Instead of comparing a single numerical value,
	 * the custom objective calls the {@link maestro_mo.solution.Solution#compareTo} custom 
	 * function.
	 * @param index The unique index of the objective
	 * @param id The identifier of the objective
	 */
	public Objective(int index, String id)
	{
		this.index = index;
		this.id = id;
		maximization = false;
		custom = true;
	}
	
	// --------------------------------------------------------------------------------------------
	// Methods
	// --------------------------------------------------------------------------------------------

	/**
	 * @return The unique index of the objective
	 */
	public int getIndex() 
	{
		return index;
	}

	/**
	 * @param index The unique index of the objective
	 */
	public void setIndex(int index) 
	{
		this.index = index;
	}

	/**
	 * @return the The identifier of the objective
	 */
	public String getId() 
	{
		return id;
	}

	/**
	 * @param id The identifier of the objective
	 */
	public void setId(String id) 
	{
		this.id = id;
	}

	/**
	 * @return the maximization
	 */
	public boolean isMaximization() 
	{
		return maximization;
	}

	/**
	 * @param maximization the maximization to set
	 */
	public void setMaximization(boolean maximization) 
	{
		this.maximization = maximization;
	}

	/**
	 * @return True if the evaluation of this objective is not performed by comparing a single 
	 * numerical value but it is done in a customized way that is implemented in the solutions 
	 * themselves. False if a numerical fitness value is used instead.
	 */
	public boolean isCustom() 
	{
		return custom;
	}

	/**
	 * @param custom True if the evaluation of this objective is not performed by comparing a 
	 * single numerical value but it is done in a customized way that is implemented in the 
	 * solutions themselves. False if a numerical fitness value is used instead.
	 */
	public void setCustom(boolean custom) 
	{
		this.custom = custom;
	}

	@Override
	public int compare(Solution sol1, Solution sol2) 
	{
		if(custom)
			return sol1.compareTo(index, sol2);
		double fitness1 = sol1.getFitness(index);
		double fitness2 = sol2.getFitness(index);
		
		if (Double.isNaN(fitness1))
			return Double.isNaN(fitness2) ? 0 : -1;
		if (Double.isNaN(fitness2))
			return 1;
		
		if(fitness1 == fitness2)
			return 0;
		boolean larger = fitness1 > fitness2;
		return maximization ? (larger ? 1 : -1) : (larger ? -1 : 1);
	}

}
