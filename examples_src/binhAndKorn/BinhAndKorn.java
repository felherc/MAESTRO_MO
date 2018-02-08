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

package binhAndKorn;

import java.io.IOException;

import maestro_mo.MAESTRO;
import maestro_mo.Monitor;

public class BinhAndKorn implements Monitor 
{

	public static void main(String[] args) 
	{
		int runIndex		=  1;
		double minX			=  0;
		double maxX			=  5;
		double minY			=  0;
		double maxY			=  3;
		int popCapacity		= 50;
		
		long timeLimit		= 5*1000;
		int solutionLimit	= 1000;
		
		BinhAndKorn bak		= new BinhAndKorn(runIndex, minX, maxX, minY, maxY, popCapacity);
		bak.optimize(timeLimit, solutionLimit);
	}
	
	private MAESTRO maestro;
	
	public BinhAndKorn(int runIndex, double minX, double maxX, double minY, double maxY, 
							int popCapacity)
	{
		BaKSolution sol		= new BaKSolution("Root", Double.NaN, Double.NaN); 
		maestro				= new MAESTRO("Binh and Korn function", runIndex, sol, this, true);
		maestro.setPopulationCapacity(popCapacity);
		maestro.addContVar("x", minX, maxX);
		maestro.addContVar("y", minY, maxY);
		maestro.addNumericalObjective(0, "Objective 1", false);
		maestro.addNumericalObjective(1, "Objective 2", false);
	}
	
	public void optimize(long timeLimit, int solutionLimit)
	{
		System.out.println("Optimization started");
		maestro.startOptimization(timeLimit, solutionLimit);
	}

	@Override
	public void terminate(String message) 
	{
		try 
		{
			maestro.writeReport(false, "data/tests/Binh and Korn", true, true, true, true, true);
			System.out.println("Finished!");
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	@Override
	public void reset() 
	{
		// Do nothing
	}

}
