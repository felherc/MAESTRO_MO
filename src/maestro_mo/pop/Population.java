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

package maestro_mo.pop;

import java.util.ArrayList;
import java.util.HashSet;

import maestro_mo.Objective;
import maestro_mo.solution.SolutionWrapper;

/**
 * Interfaces implementations of solution populations for multi-objective optimization problems 
 * @author Felipe Hernández
 */
public interface Population
{

	// --------------------------------------------------------------------------------------------
	// Methods
	// --------------------------------------------------------------------------------------------
	
	/**
	 * Adds a new objective to compare the solutions in the population
	 * @param objective The objective to add
	 */
	public void addObjective(Objective objective);
	
	/**
	 * @return The list of objectives to compare the solutions in the population
	 */
	public ArrayList<Objective> getObjectives();
	
	/**
	 * Clears the population of solutions
	 */
	public void clear();
	
	/**
	 * @returns The number of solutions currently in the population
	 */
	public int size();
	
	/**
	 * @return The target size of the current population
	 */
	public int getCapacity();
	
	/**
	 * @param capacity The target size of the current population
	 */
	public void setCapacity(int capacity);
	
	/**
	 * @param solution The solution to analyze
	 * @return True if an identical solution already exists in the population. That is, if the 
	 * defining values of the provided solution are the same that those in any solution in the
	 * population.
	 */
	public boolean contains(SolutionWrapper solution);
	
	/**
	 * Offers a new solution to be considered for addition to the population
	 * @param solution the solution offered
	 */
	public void offerSolution(SolutionWrapper solution);
	
	/**
	 * Offer a list of new solutions to be considered for addition to the population. Prevents
	 * multiple intermediate updates for bulk offerings compared to iteratively calling
	 * {@link #offerSolution(SolutionWrapper)}.
	 * @param solutions list of solutions offered
	 */
	public void offerSolutions(ArrayList<SolutionWrapper> solutions);
	
	/**
	 * @return A list with all the solutions in the population
	 */
	public ArrayList<SolutionWrapper> getAllSolutions();
	
	/**
	 * @param count The number of requested solutions
	 * @return A list of solutions in the population. A custom strategy can be implemented.
	 */
	public ArrayList<SolutionWrapper> select(int count);
	
	/**
	 * @param count The number of requested solutions
	 * @param greed A value between -1.0 and 1.0 that represents the expected mean "quality" of the 
	 * returned solutions. 1.0 if the selected solutions should be only among the best according to 
	 * the objectives. -1.0 if they should be only among the worst. 0.0 if there is no preference.
	 * @return A list of solutions in the population. A custom strategy can be implemented.
	 */
	public ArrayList<SolutionWrapper> select(int count, double greed);
	
	/**
	 * @param count The number of requested solutions
	 * @return A set of solutions in the population. The number of solutions returned may be 
	 * smaller than the number requested. A custom strategy can be implemented.
	 */
	public HashSet<SolutionWrapper> selectSet(int count);
	
	/**
	 * @param count The number of requested solutions
	 * @param greed A value between -1.0 and 1.0 that represents the expected mean "quality" of the 
	 * returned solutions. 1.0 if the selected solutions should be only among the best according to 
	 * the objectives. -1.0 if they should be only among the worst. 0.0 if there is no preference.
	 * @return A set of solutions in the population. The number of solutions returned may be 
	 * smaller than the number requested. A custom strategy can be implemented.
	 */
	public HashSet<SolutionWrapper> selectSet(int count, double greed);
	
	// TODO Method for assigning weights to solutions according to arbitrary criteria, use a greed
	// parameter to control the weight contrast between solutions
	
}
