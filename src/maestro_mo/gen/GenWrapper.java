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

package maestro_mo.gen;

import java.util.ArrayList;

import maestro_mo.ContVar;
import maestro_mo.DiscVar;
import maestro_mo.Objective;
import maestro_mo.pop.Population;
import maestro_mo.solution.SolutionRoot;

/**
 * Wraps objects that implement the {@link Generator} interface by adding a suffix to their
 * identifiers, to differentiate it from other generators of the same type, and counting the total 
 * number of solutions generated.
 * @author Felipe Hernández
 */
public class GenWrapper implements Generator 
{
	
	// --------------------------------------------------------------------------------------------
	// Attributes
	// --------------------------------------------------------------------------------------------
	
	/**
	 * The generator being wrapped
	 */
	private Generator generator;
	
	/**
	 * Suffix for the identifier of the generator. Used when more than one of the generators has 
	 * the same identifier.
	 */
	private String suffix;
	
	/**
	 * The total number of solutions generated
	 */
	private int genTotal;
	
	// --------------------------------------------------------------------------------------------
	// Constructor
	// --------------------------------------------------------------------------------------------
	
	/**
	 * @param generator The generator being contained
	 */
	public GenWrapper(Generator generator)
	{
		this.generator	= generator;
		suffix			= "";
		genTotal		= 0;
	}
	
	// --------------------------------------------------------------------------------------------
	// Methods
	// --------------------------------------------------------------------------------------------

	@Override
	public String getId()
	{
		return generator.getId() + suffix;
	}
	
	@Override
	public String getShortId()
	{
		return generator.getShortId() + suffix;
	}
	
	/**
	 * @return {@link #suffix}
	 */
	public String getSuffix()
	{
		return suffix;
	}
	
	/**
	 * @param suffix {@link #suffix}
	 */
	public void setSuffix(String suffix)
	{
		this.suffix = suffix;
	}
	
	/**
	 * @return {@link #genTotal}
	 */
	public int getGenTotal()
	{
		return genTotal;
	}

	@Override
	public String getParamSummary() 
	{
		return generator.getParamSummary();
	}

	@Override
	public void addDiscVariable(DiscVar variable) 
	{
		generator.addDiscVariable(variable);
	}

	@Override
	public void addContVariable(ContVar variable) 
	{
		generator.addContVariable(variable);
	}

	@Override
	public void clearVariables() 
	{
		generator.clearVariables();
	}

	@Override
	public void setObjectives(ArrayList<Objective> objectives) 
	{
		generator.setObjectives(objectives);
	}

	@Override
	public ArrayList<SolutionRoot> generateSolutions(Population population,
			int number) 
	{
		ArrayList<SolutionRoot> roots = generator.generateSolutions(population, number);
		genTotal += roots == null ? 0 : roots.size();
		return roots;
	}

}
