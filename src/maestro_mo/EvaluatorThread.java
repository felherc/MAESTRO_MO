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

import maestro_mo.solution.SolutionRoot;

/**
 * This class invokes the generation and evaluation of new solutions in a loop. The process is 
 * executed in separate threads to take advantage of multi-core CPUs and thus speeding up run 
 * times.
 * @author Felipe Hernández
 */
public class EvaluatorThread extends Thread
{

	// --------------------------------------------------------------------------------------------
	// Attributes
	// --------------------------------------------------------------------------------------------
	
	/**
	 * Instance of the MAESTRO manager class
	 */
	private Optimizer optimizer;
	
	/**
	 * True if the thread should continue processing new solutions
	 */
	private volatile boolean run;
	
	/**
	 * Time in milliseconds when the last root evaluation started. -1 if no evaluation has started.
	 */
	private long analysisStart;
	
	// --------------------------------------------------------------------------------------------
	// Constructor
	// --------------------------------------------------------------------------------------------
	
	/**
	 * @param manager	{@link #optimizer}
	 * @param solution	{@link #solution}
	 */
	public EvaluatorThread(Optimizer manager)
	{
		this.optimizer	= manager;
		run				= true;
		analysisStart	= -1;
	}
	
	// --------------------------------------------------------------------------------------------
	// Methods
	// --------------------------------------------------------------------------------------------
	
	/**
	 * Tells the solution thread to stop processing new solutions
	 */
	public void end()
	{
		run = false;
	}
	
	/**
	 * Invokes the generation and processing of new solutions within an inner cycle run. The thread
	 * stops processing new solutions when the inner cycle ends.
	 */
	public void run()
	{
		run = true;
		while(run)
		{
			// Obtain solution root
			SolutionRoot root		= null;
			try
			{
				root				= optimizer.getSolutionRoot();
			} catch (Exception e)
			{
				e.printStackTrace();
			}
			
			// Analyze solution root
			try
			{
				if(root != null)
				{
					analysisStart	= System.currentTimeMillis();
					optimizer.offerSolutionRoot(root);
				}
				else
				{
					analysisStart	= -1;
					run				= false;
				}
			} catch (Exception e)
			{
				analysisStart		= -1;
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * @return {@link #analysisStart}
	 */
	public long getAnalysisStart()
	{
		return analysisStart;
	}
	
}
