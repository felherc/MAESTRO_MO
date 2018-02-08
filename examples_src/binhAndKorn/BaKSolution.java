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

import java.util.ArrayList;

import maestro_mo.solution.Solution;

/**
 * Represents a solution to the Binh and Korn two-objective optimization problem
 * @author Felipe Hernández
 */
public class BaKSolution implements Solution 
{
	
	private String id;
	
	private double x;
	
	private double y;
	
	private double obj1;
	
	private double obj2;

	public BaKSolution(String id, double x, double y)
	{
		this.id	= id;
		this.x	= x;
		this.y	= y;
		obj1	= 4*x*x + 4*y*y;
		obj2	= (x - 5)*(x - 5) + (y - 5)*(y - 5);
	}
	
	@Override
	public Solution createNew(int id, ArrayList<Integer> discValues, ArrayList<Double> contValues,
								Object extra) 
	{
		String newId	= "Solution " + id;
		double newX		= contValues.get(0);
		double newY		= contValues.get(1);
		return new BaKSolution(newId, newX, newY);
	}

	@Override
	public String getId() 
	{
		return id;
	}

	@Override
	public ArrayList<Integer> getDiscValues() 
	{
		return null;
	}

	@Override
	public ArrayList<Double> getContValues() 
	{
		ArrayList<Double> contValues = new ArrayList<>();
		contValues.add(x);
		contValues.add(y);
		return contValues;
	}

	@Override
	public boolean isValid()
	{
		return true;
	}

	@Override
	public String getReportHeader() 
	{
		return "obj1\tobj2";
	}

	@Override
	public String getReport() 
	{
		return obj1 + "\t" + obj2;
	}

	@Override
	public double getFitness(int objective) 
	{
		if (objective == 0)
			return obj1;
		else if (objective == 1)
			return obj2;
		else
			return Double.NaN;
	}

	@Override
	public int compareTo(int objective, Solution other) 
	{
		return 0;
	}

	@Override
	public boolean optimizationConverged() 
	{
		return false;
	}

}
