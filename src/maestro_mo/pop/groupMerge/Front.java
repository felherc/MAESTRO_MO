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
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;

import maestro_mo.Objective;
import maestro_mo.solution.SolutionWrapper;

/**
 * A partition of a group-merging solution population where none of the contained solutions 
 * dominate or are dominated by each other. That is, they represent a Pareto front of solutions. 
 * This front may be dominated by another front or may dominate a third one.
 * @author Felipe Hernández
 */
public class Front implements Comparator<SolutionWrapper>
{

	// --------------------------------------------------------------------------------------------
	// Constants
	// --------------------------------------------------------------------------------------------
	
	/**
	 * Name of the attribute of the solutions that measures how crowded is the area around the
	 * solution in the objectives space
	 */
	public final static String CROWDING_DISTANCE = "Overall crowding distance";
	
	/**
	 * Name of the attribute of the solutions that stores a temporary distance value
	 */
	public final static String TEMP_DISTANCE = "Temporary distance";
	
	// --------------------------------------------------------------------------------------------
	// Attributes
	// --------------------------------------------------------------------------------------------

	/**
	 * The list of solutions to the multi-objective optimization problem that make part of the 
	 * front	
	 */
	private ArrayList<SolutionWrapper> solutions;
	
	/**
	 * A weight value used to perform selection computations
	 */
	private double weight;
	
	// --------------------------------------------------------------------------------------------
	// Constructor
	// --------------------------------------------------------------------------------------------
	
	/**
	 * Initializes empty front
	 */
	public Front()
	{
		solutions = new ArrayList<SolutionWrapper>();
		weight = Double.NaN;
	}
	
	/**
	 * Initializes front
	 * @param solutions The list of solutions to the multi-objective optimization problem that make 
	 * part of the front	
	 */
	public Front(ArrayList<SolutionWrapper> solutions)
	{
		this.solutions = solutions;
		weight = Double.NaN;
	}
	
	// --------------------------------------------------------------------------------------------
	// Methods
	// --------------------------------------------------------------------------------------------
	
	/**
	 * Getter for the {@link #weight} attribute
	 * @return {@link #weight}
	 */
	public double getWeight() 
	{
		return weight;
	}

	/**
	 * Setter for the {@link #weight} attribute
	 * @param weight {@link #weight}
	 */
	public void setWeight(double weight)
	{
		this.weight = weight;
	}

	/**
	 * @return The number of solutions in the front
	 */
	public int size()
	{
		return solutions.size();
	}
	
	/**
	 * @param solution The solution to search
	 * @return True if the solution specified is contained in the front. False otherwise.
	 */
	public boolean contains(SolutionWrapper solution)
	{
		return solutions.contains(solution);
	}
	
	/**
	 * @return The list of solutions to the multi-objective optimization problem that make part of 
	 * the front
	 */
	public ArrayList<SolutionWrapper> getSolutions()
	{
		return solutions;
	}
	
	/**
	 * Adds a new solution to the front
	 * @param solution The solution to add
	 */
	public void add(SolutionWrapper solution)
	{
		solutions.add(solution);
	}
	
	/**
	 * Adds all the solutions in the collection to the front
	 * @param solutions The collection of solutions to add
	 */
	public void addAll(Collection<SolutionWrapper> solutions)
	{
		this.solutions.addAll(solutions);
	}
	
	/**
	 * Adds all the solutions in the other front to this one
	 * @param other The front to add solutions from
	 */
	public void addAll(Front other)
	{
		solutions.addAll(other.getSolutions());
	}
	
	/**
	 * Returns a reduced front which contains a specified number of solutions. The criteria for
	 * selecting the solutions to include in the reduced front seeks to return those that make the 
	 * most disperse selection in the objective functions multi-dimensional space. To accomplish 
	 * this, an overall crowding distance is computed for each solution, and those with the largest 
	 * crowding distance (those further away from each other) are selected. The process is 
	 * described in the reference below: <br><br>
	 * Deb, K., Pratap, A., Agarwal, S. and Meyarivan, T., 2002, "A fast and elitist multiobjective 
	 * genetic algorithm: NSGA-II", <i>Evolutionary Computation</i>, IEEE Transactions on, 6 (2), 
	 * pp. 182-197
	 * @param count The number of solutions in the reduced front
	 * @param objectives The list of objectives in the optimization problem
	 * @return The reduced front with the selected solutions
	 */
	public Front getReduced(int count, ArrayList<Objective> objectives)
	{
		if(count >= solutions.size())
			return this;
		
		// Initialize crowding distance
		for(SolutionWrapper sol : solutions)
			sol.setValue(CROWDING_DISTANCE, 0);
		
		// Compute overall crowding distances
		ArrayList<SolutionWrapper> ordered = new ArrayList<SolutionWrapper>();
		for(Objective obj : objectives)
		{
			ordered.clear();
			for(SolutionWrapper sol : solutions)
				ordered.add(sol);
			Collections.sort(ordered, obj);
			SolutionWrapper first =	ordered.get(0);
			SolutionWrapper last =	ordered.get(ordered.size() - 1);
			first.setValue(	TEMP_DISTANCE, Double.POSITIVE_INFINITY);
			last.setValue(	TEMP_DISTANCE, Double.POSITIVE_INFINITY);
			double distance = 0;
			double largest = 0;
			for(int i = 1 ; i < ordered.size() - 1 ; i++)
			{
				SolutionWrapper prev = 		ordered.get(i - 1	);
				SolutionWrapper current =	ordered.get(i		);
				SolutionWrapper next =		ordered.get(i + 1	);
				if(obj.isCustom())
					distance = obj.compare(prev, next) != 0.0 ? 1.0 : 0.0;
				else
				{
					int index = obj.getIndex();
					distance = Math.abs(prev.getFitness(index) - next.getFitness(index));
					largest = distance > largest ? distance : largest;
				}
				current.setValue(TEMP_DISTANCE, distance);
			}
			for(SolutionWrapper sol : ordered)
			{
				distance = sol.getValue(CROWDING_DISTANCE);
				if(obj.isCustom())
					sol.setValue(CROWDING_DISTANCE, distance + sol.getValue(TEMP_DISTANCE));
				else
					sol.setValue(CROWDING_DISTANCE, distance + 
															sol.getValue(TEMP_DISTANCE)/largest);
			}
		}
		
		// Sort in crowding distance order
		ordered.clear();
		ordered.addAll(solutions);
		Collections.sort(ordered, this);
		Front reduced = new Front();
		int lastIndex = ordered.size() - 1;
		for(int i = lastIndex ; i > lastIndex - count ; i--)
			reduced.add(ordered.get(i));
		return reduced;
	}

	@Override
	public int compare(SolutionWrapper sol1, SolutionWrapper sol2) 
	{
		Double dist1 = sol1.getValue(CROWDING_DISTANCE);
		Double dist2 = sol2.getValue(CROWDING_DISTANCE);
		if(dist1 == dist2)
			return Math.random() > 0.5 ? 1 : -1;
		return dist1.compareTo(dist2);
	}
	
}
