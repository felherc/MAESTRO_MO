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

/**
 * This interface represents a solution to a multi-objective optimization problem
 * @author Felipe Hernández
 */
public interface Solution 
{

	// --------------------------------------------------------------------------------------------
	// Methods
	// --------------------------------------------------------------------------------------------
	
	/**
	 * Creates a new Solution object with the provided values and computes the fitness variables.
	 * The values must be stored to be able to return them using the methods 
	 * {@link #getContValues()} and {@link #getDiscValues()}. These values may be modified 
	 * in this method if necessary (when special constraints must be met, for instance).
	 * @param index			A consecutive integer value to be optionally used in the 
	 * 						identification of the new solution. The identification is returned by 
	 * 						the {@link #getId()} method.
	 * @param contValues	The values for the continuous defining variables; null if there are no 
	 * 						continuous variables
	 * @param discValues	The values for the discrete defining variables; null if there are no 
	 * 						discrete variables
	 * @param extra			<code>null</code> in most cases. Contains an object if any was attached
	 * 						to the user-defined root from which this solution was created. Used in
	 * 						case the user wants to attach additional information to such 
	 * 						predefined solutions.
	 * @return The new Solution object
	 */
	public Solution createNew(int index, ArrayList<Integer> discValues, 
								ArrayList<Double> contValues, Object extra);
	
	/**
	 * Returns the identifier of the solution. Must not be an empty string (""). Each solution must
	 * have a unique identifier.
	 * @return The identifier of the solution.
	 */
	public String getId();
	
	/**
	 * Returns the values for the discrete defining variables; null if there are no discrete 
	 * variables. The order of the values must be consistent with the creation order of the 
	 * variables.
	 * @return The values for the discrete variables
	 */
	public ArrayList<Integer> getDiscValues();
	
	/**
	 * Returns the values for the continuous defining variables; null if there are no continuous 
	 * variables. The order of the values must be consistent with the creation order of the 
	 * variables.
	 * @return The values for the continuous variables
	 */
	public ArrayList<Double> getContValues();
	
	/**
	 * @return <code>true</code> if the solution was successfully created and if it should be
	 * offered to the population. <code>false</code> if for some reason the solution could not be 
	 * successfully created and should not be offered to the population. In the latter case, the
	 * solution will not count towards the solution limit.
	 */
	public boolean isValid();
	
	/**
	 * Returns a string with the header of the columns in the report table. The report should
	 * include the fitness values for the multiple optimization objectives and can additionally 
	 * contain any other values of interest. The individual header strings for each field should 
	 * be separated by a tab (\t) character.
	 * @return A string with the header of the columns in the report table
	 */
	public String getReportHeader();
	
	/**
	 * Returns the values of the solution to be printed in the report table. The report should
	 * include the fitness values for the multiple optimization objectives and can additionally 
	 * contain any other values of interest. The individual strings for each field should be 
	 * separated by a tab (\t) character. The fields should correspond to those in the header 
	 * string returned by the {@link #getReportHeader()} method.
	 * @return The values of the solution to be printed in the report table
	 */
	public String getReport();
	
	/**
	 * @param objective Index of the optimization objective
	 * @return The fitness value for the requested optimization objective
	 */
	public double getFitness(int objective);
	
	/**
	 * Compares this solution with another one according to a specific optimization objective. 
	 * Returns a negative integer, zero, or a positive integer as this solution is less fitted, 
	 * equally fitted, or more fitted than the other according to the provided objective. Will only 
	 * be called for comparisons with objectives that do not use a single fitness value (as 
	 * provided by the {@link #getFitness} method).
	 * @param objective Index of the optimization objective
	 * @param other The other solution to compare
	 * @return A negative integer, zero, or a positive integer as this solution is less fitted, 
	 * equally fitted, or more fitted than the other
	 */
	public int compareTo(int objective, Solution other);

	/**
	 * @return True if the optimization should stop because of some characteristics of this solution
	 * (e.g. a desired goal was achieved). False if the optimization should continue.
	 */
	public boolean optimizationConverged();
	
}
