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

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;

import maestro_mo.Optimizer;
import maestro_mo.gen.GenWrapper;
import maestro_mo.solution.SolutionWrapper;

/**
 * This class writes report files for the execution of MAESTRO
 * @author Felipe Hernández
 */
public class Reporter 
{
	
	// --------------------------------------------------------------------------------------------  
	// Constants
	// --------------------------------------------------------------------------------------------
	
	/**
	 * Report file header 1
	 */
	private final static String REPORT_HEADER_1 = "MAESTRO execution report";
	
	/**
	 * Label for optimization problem name
	 */
	private final static String PROBLEM_NAME_LABEL = "Optimization problem: ";
	
	/**
	 * Label for the index of the optimization run
	 */
	private final static String RUN_INDEX_LABEL = "Run index: ";
	
	/**
	 * Label for the time the execution of MAESTRO started 
	 */
	private final static String STARTING_TIME_LABEL = "Execution started: ";
	
	/**
	 * Label for the time of the report
	 */
	private final static String TERMINATION_TIME_LABEL = "Execution completed: ";
	
	/**
	 * Label for the number of solutions processed
	 */
	private final static String SOLUTIONS_LABEL = "Solutions processed: ";
	
	/**
	 * Label for the reason for termination
	 */
	private final static String TERMINATION_LABEL = "Terminated: ";

	/**
	 * Identifier of the MAESTRO parameters table
	 */
	private final static String PARAMETERS_TABLE_HEADER = "[MAESTRO parameters]";
	
	/**
	 * Identifier of the <code>populationCapacity</code> parameter
	 */
	private static final String PARAM_POP_CAPACITY = "Population capacity =";
	
	/**
	 * Identifier of the <code>allowEqPerformers</code> parameter
	 */
	private static final String PARAM_ALLOW_EQ_PERF = "Equivalent performers allowed = ";
	
	/**
	 * Identifier of the <code>randomSolutionRatio</code> parameter
	 */
	private static final String PARAM_RAND_SOL_RATIO = "Random solution ratio = ";

	/**
	 * Identifier of the <code>timeLimit</code> parameter
	 */
	private final static String PARAM_TIME_LIMIT = "Time limit = ";
	
	/**
	 * Identifier of the <code>solutionLimit</code> parameter
	 */
	private final static String PARAM_SOLUTION_LIMIT = "Solution limit = ";
	
	/**
	 * Identifier of the <code>threadCount</code> parameter
	 */
	private final static String PARAM_THREAD_COUNT = "Number of threads = ";
	
	/**
	 * Identifier of the <code>concurrentUpdates</code> parameter
	 */
	private static final String PARAM_CONCURRENT_UPDATES = "Concurrent updates allowed: ";
	
	/**
	 * Identifier of the <code>genRatio</code> parameter
	 */
	private final static String PARAM_GEN_RATIO = "Generation ratio = ";
	
	/**
	 * Identifier of the <code>genMin</code> parameter
	 */
	private final static String PARAM_GEN_MIN = "Generation minimum = ";
	
	/**
	 * Identifier of the <code>absGenMin</code> parameter
	 */
	private final static String PARAM_ABS_GEN_MIN = "Absolute generation minimum = ";
	
	/**
	 * Identifier of the <code>weightPop</code> parameter
	 */
	private final static String PARAM_GEN_WEIGHT_POP = "Population weight (for generation) = ";
	
	/**
	 * Identifier of the <code>weightFront1</code> parameter
	 */
	private final static String PARAM_GEN_WEIGHT_FRONT1 = "Front 1 weight (for generation) = ";

	/**
	 * Identifier of the generators table
	 */
	private final static String GENERATORS_TABLE_HEADER = "[Generator methods]";
	
	/**
	 * Header for the generator method column
	 */
	public final static String GENERATORS_ID = "Generator";
	
	/**
	 * Header for the rank history column
	 */
	public final static String RANK_HISTORY_ID = "Rank history";
	
	/**
	 * Header for the number of total solutions generated column
	 */
	private final static String GEN_TOTAL_ID = "Total solutions";
	
	/**
	 * Header for the generator parameters column
	 */
	private final static String PARAMETERS_ID = "Parameters";
	
	/**
	 * Identifier of the solution generation history table
	 */
	private final static String GEN_HIST_TABLE_HEADER = "[Generator method use]";
	
	/**
	 * Header for the generation column
	 */
	private final static String GENERATION_ID = "Generation";
	
	/**
	 * Header for the number of solutions generated column
	 */
	private final static String GEN_SOLUTIONS_ID = "Solutions generated";
	
	/**
	 * Header for the time of generation column
	 */
	private final static String GEN_TIME = "Total time (ms)";
	
	/**
	 * Header for the time of generation per solution column
	 */
	private final static String GEN_TIME_PER_SOLUTION = "Time for each (ms)";
	
	/**
	 * Identifier of the population table
	 */
	private static final String POPULATION_TABLE_HEADER = "[Final population]";
	
	/**
	 * Identifier of the best solutions table
	 */
	private final static String PARETO_FRONT_TABLE_HEADER = "[Pareto front]";
	
	/**
	 * Identifier of the elite solution history table
	 */
	private final static String HALL_OF_FAME_TABLE_HEADER = "[Hall of fame]";
	
	/**
	 * Identifier of the table with all the solutions
	 */
	private final static String ALL_SOLUTIONS_TABLE_HEADER = "[All solutions]";
	
	/**
	 * Header for the solutions' names column
	 */
	public final static String SOLUTIONS_ID = "Solution";
	
	/**
	 * Identifier for the discrete variables
	 */
	private final static String DISC_VARIABLES_ID = "Discrete variables";
	
	/**
	 * Identifier for the continuous variables
	 */
	private final static String CONT_VARIABLES_ID = "Continuous variables";
	
	// --------------------------------------------------------------------------------------------
	// Methods
	// --------------------------------------------------------------------------------------------
	
	/**
	 * Writes a report file containing the results from the execution of MAESTRO
	 * @param manager 			The MAESTRO manager object that ran the optimization process
	 * @param fileRoute 		The system route and file name for the report to be written
	 * @param writeConfig 		True if the parameters and generators of MAESTRO should be included
	 * @param entirePopulation	True if the entire population should be included; false if only the
	 * 							Pareto (first) front should be included
	 * @param writeGenHist 		True if the history of generators used should be included
	 * @param writeHallOfFame	True if the solutions in the hall of fame should be included
	 * @param writeAllSolutions True if all solutions should be included
	 * @throws IOException		If the file can not be created
	 */
	public static void writeReport(Optimizer manager, String fileRoute, 
							boolean writeConfig, boolean entirePopulation, boolean writeGenHist, 
							boolean writeHallOfFame, boolean writeAllSolutions) throws IOException
	{
		// Open file
		PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(fileRoute, false)));
		
		// Write header
		out.println(		REPORT_HEADER_1														);
		DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy K:mm:ss a");
		Date date = new Date(manager.getStartTime());
		out.println(		PROBLEM_NAME_LABEL + manager.getProblem()							);
		out.println(		RUN_INDEX_LABEL + manager.getRunIndex()								);
		out.println(		STARTING_TIME_LABEL + dateFormat.format(date)						);
        date = new Date();
        out.println(		TERMINATION_TIME_LABEL + dateFormat.format(date)					);
        out.println(		SOLUTIONS_LABEL + manager.getEvalCount()							);
        out.println(		TERMINATION_LABEL + manager.getTerminationMessage()					);
        out.println(		""																	);
        
        if(writeConfig)
        {        	
        	// Add general parameters
        	out.println(	PARAMETERS_TABLE_HEADER);
        	out.println(	PARAM_POP_CAPACITY + "\t" + manager.getPopulationCapacity()			);
        	out.println(	PARAM_ALLOW_EQ_PERF + "\t" + manager.areEqPerformersAllowed()		);
        	out.println(	PARAM_RAND_SOL_RATIO + "\t" + manager.getRandomSolutionRatio()		);
        	out.println(	PARAM_TIME_LIMIT + "\t" + manager.getTimeLimit() + " ms"			);
        	out.println(	PARAM_SOLUTION_LIMIT + "\t" + manager.getSolutionLimit()			);
        	out.println(	PARAM_THREAD_COUNT + "\t" + manager.getThreadCount()				);
        	out.println(	PARAM_CONCURRENT_UPDATES + "\t" + manager.getConcurrentUpdates()	);
        	out.println(	PARAM_GEN_RATIO + "\t" + manager.getGenRatio()						);
        	out.println(	PARAM_GEN_MIN + "\t" + manager.getGenMin()							);
        	out.println(	PARAM_ABS_GEN_MIN + "\t" + manager.getAbsGenMin()					);
        	out.println(	PARAM_GEN_WEIGHT_POP + "\t" + manager.getWeightPop()				);
        	out.println(	PARAM_GEN_WEIGHT_FRONT1 + "\t" + manager.getWeightFront1()			);
        	out.println(	""																	);
	        
	        // Add generator configuration
        	out.println(	GENERATORS_TABLE_HEADER												);
        	out.println(	GENERATORS_ID + "\t" + GEN_TOTAL_ID + "\t" + PARAMETERS_ID			);
	        ArrayList<GenWrapper> generators = manager.getGenerators();
	        for(GenWrapper generator : generators)
	        {
	        	String line = "";
	        	line += generator.getId() + " (" + generator.getShortId() + ")";
	        	line += "\t" + generator.getGenTotal();
	        	line += "\t" + generator.getParamSummary();
	        	out.println(line																);
	        }
	        out.println(	""																	);
        }
        
        // Add generator use history
        if(writeGenHist)
        {
        	out.println(	GEN_HIST_TABLE_HEADER												);
        	out.println(	GENERATION_ID + "\t" + GENERATORS_ID + "\t" + GEN_SOLUTIONS_ID 
        						+ "\t" + GEN_TIME + "\t" + GEN_TIME_PER_SOLUTION				);
        	ArrayList<String> gens = manager.getGenerationHistory();
        	for(int i = 0 ; i < gens.size() ; i++)
        		out.println((i + 1) + "\t" + gens.get(i)										);
        	out.println(	""																	);
        }
        
        // Add best solutions (in front 1)
        ArrayList<SolutionWrapper> bestSolutions = null;
        String tableHeader = "";
        if (entirePopulation)
        {
        	bestSolutions = manager.getSolutionsInPopulation();
        	tableHeader = POPULATION_TABLE_HEADER;
        }
        else
        {
        	bestSolutions = manager.getFirstFront();
        	tableHeader = PARETO_FRONT_TABLE_HEADER;
        }
        if(bestSolutions != null)
	        if(bestSolutions.size() > 0)
	        {
	        	out.println(tableHeader															);
	        	addSolutionHeader(manager, bestSolutions.get(0), out);
	        	Iterator<SolutionWrapper> solutions = bestSolutions.iterator();
	        	addSolutionList(manager, solutions, out);
	        	out.println(""																	);
	        }
        
        // Add hall of fame
        if(writeHallOfFame)
        {
        	ArrayList<SolutionWrapper> eliteHistory = manager.getHallOfFame();
        	if(eliteHistory.size() > 0)
        	{
        		out.println(HALL_OF_FAME_TABLE_HEADER											);
        		addSolutionHeader(manager, eliteHistory.get(0), out);
        		Iterator<SolutionWrapper> solutions = eliteHistory.iterator();
	        	addSolutionList(manager, solutions, out);
	        	out.println(""																	);
        	}
        }
        
        // Add all solutions
        if(writeAllSolutions)
        {
        	ArrayList<SolutionWrapper> allSolutions = manager.getAllSolutions();
        	if (allSolutions != null)
	        	if(allSolutions.size() > 0)
	        	{
	        		out.println(ALL_SOLUTIONS_TABLE_HEADER										);
	        		addSolutionHeader(manager, allSolutions.get(0), out);
	        		Iterator<SolutionWrapper> solutions = allSolutions.iterator();
		        	addSolutionList(manager, solutions, out);
		        	out.println(""																);
	        	}
        }
        
        // Close file
        out.close();
	}
	
	/**
	 * Writes the header of the solution table to the report file
	 * @param manager	The MAESTRO-MO manager that ran the optimization process
	 * @param solution	Any solution of the process
	 * @param out		PrintWriter of the file
	 */
	private static void addSolutionHeader(Optimizer manager, SolutionWrapper solution, 
											PrintWriter out)
	{
		// Retrieve variable count
		ArrayList<Integer> discValues = solution.getSolution().getDiscValues();
		ArrayList<Double> contValues = solution.getSolution().getContValues();
		
		int total = 2;
		int discCount = discValues == null ? 0 : discValues.size();
		int contCount = contValues == null ? 0 : contValues.size();
		
		total += discCount;
		total += contCount;
		
		String[] line1 = new String[total];
		String[] line2 = new String[total];
		String fitnessHeader = solution.getSolution().getReportHeader();
		String space = "";
		for(int i = 0 ; i < fitnessHeader.split("\t").length - 1 ; i++)
			space += "\t";
		
		line1[0] = "\t\t";
		line1[1] = space;
		line2[0] = SOLUTIONS_ID + "\t" + GENERATORS_ID + "\t" + RANK_HISTORY_ID;
		line2[1] = fitnessHeader;
		
		int index = 2;
		int variableIndex = 0;
		
		// Add header for discrete variables
		if(discCount > 0)
		{
			variableIndex = 0;
			line1[index] = DISC_VARIABLES_ID;
			line2[index] = manager.getDiscVarName(variableIndex);
			index++;
			variableIndex++;
			int max = index + discCount - 1;
			for(int i = index ; i < max ; i++)
			{
				line1[i] = "";
				line2[i] = manager.getDiscVarName(variableIndex);
				index++;
				variableIndex++;
			}
		}
		
		// Add header for continuous variables
		if(contCount > 0)
		{
			variableIndex = 0;
			line1[index] = CONT_VARIABLES_ID;
			line2[index] = manager.getContVarName(variableIndex);
			index++;
			variableIndex++;
			int max = index + contCount - 1;
			for(int i = index ; i < max ; i++)
			{
				line1[i] = "";
				line2[i] = manager.getContVarName(variableIndex);
				index++;
				variableIndex++;
			}
		}
		
		// Add header lines to report
		for (int i = 0; i < line1.length; i++)
			out.print(line1[i] + "\t");
		out.print("\n");
		for (int i = 0; i < line2.length; i++)
			out.print(line2[i] + "\t");
		out.print("\n");
	}
	
	/**
	 * Writes the list of solutions to the report file 
	 * @param manager	The MAESTRO-MO manager that ran the optimization process
	 * @param solutions	The list of the solutions to write
	 * @param out		PrintWriter of the file
	 */
	private static void addSolutionList(Optimizer manager, Iterator<SolutionWrapper> solutions, 
			PrintWriter out)
	{
		while(solutions.hasNext())
		{
			SolutionWrapper solution = solutions.next();
			
			// Retrieve variable count
			ArrayList<Integer> discValues = solution.getSolution().getDiscValues();
			ArrayList<Double> contValues = solution.getSolution().getContValues();
			
			int total = 4;
			int discCount = discValues == null ? 0 : discValues.size();
			int contCount = contValues == null ? 0 : contValues.size();
			
			total += discCount;
			total += contCount;
			
			// Add rank history
			String rankHistLine		= "";
			ArrayList<Integer> rankHistory = solution.getRankHistory();
			int ranks				= rankHistory.size();
			int rank				= -1;
			int mult				= 0;
			for (int r = 0; r <= ranks; r++)
			{
				int newRank			= r == ranks ? -1 : rankHistory.get(r);
				if (newRank != rank && rank > 0)
				{
					rankHistLine	+= rank;
					if (mult > 1)
						rankHistLine += "x" + mult;
					if (r < ranks)
						rankHistLine	+= ", ";
					mult			= 1;
				}
				else
					mult++;
				rank				= newRank;
			}
			
			String[] line = new String[total];
			line[0] = solution.getId();
			line[1] = manager.getGeneratorShortId(solution.getGenIndex());
			line[2] = rankHistLine;
			line[3] = solution.getReport();
			int index = 4;
			
			// Add discrete values
			if(discCount > 0)
				for(int i = 0 ; i < discValues.size() ; i++)
				{
					int value = discValues.get(i);
					line[index] = String.valueOf(manager.getDiscValueID(i, value));
					index++;
				}
			
			// Add continuous values
			if(contCount > 0)
				for(int i = 0 ; i < contValues.size() ; i++)
				{
					line[index] = String.valueOf(contValues.get(i));
					index++;
				}
			
			// Add solution line to table
			for (int i = 0; i < line.length; i++)
				out.print(line[i] + "\t");
			out.print("\n");
		}
	}

}
