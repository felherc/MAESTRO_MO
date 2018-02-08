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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import utilities.thread.Executor;
import utilities.thread.ExecutorThread;
import maestro_mo.solution.SolutionWrapper;
import maestro_mo.gen.Ensemble;
import maestro_mo.gen.Generator;
import maestro_mo.gen.GenWrapper;
import maestro_mo.pop.Population;
import maestro_mo.pop.groupMerge.Front;
import maestro_mo.pop.groupMerge.GroupMergePopulation;
import maestro_mo.solution.Solution;
import maestro_mo.solution.SolutionRoot;

/**
 * This is the main class of MAESTRO-MO: Multi-Algorithm Ensemble for Several-Threats Robust
 * Optimization (Multi-Objective). It allows to solve global multi-objective optimization problems 
 * which may contain discrete decision variables, continuous decision variables, or a mixture of 
 * them. Candidate solutions are generated by an ensemble of meta-heuristic algorithms that include 
 * a Genetic Algorithm (GA), a Gradient Descent (GD) algorithm, and a hybrid Metropolis-Ant Colony 
 * Optimization (MetroACO) algorithm. An interface is used so that additional meta-heuristics can
 * be used. MAESTRO can be run in parallel to take advantage of multiple CPUs.
 * @author Felipe Hernández
 */
public class Optimizer implements Executor
{

	// --------------------------------------------------------------------------------------------
	// Constants
	// --------------------------------------------------------------------------------------------
	
	/**
	 * Termination message: time limit reached
	 */
	private static final String TERMINATION_TIME = "Time limit reached";
	
	/**
	 * Termination message: solution evaluation limit reached
	 */
	private static final String TERMINATION_EVALUATION_LIMIT = "Solution evaluation count limit "
																+ "reached";
	
	/**
	 * Termination message: solution-triggered termination
	 */
	private static final String TERMINATION_SOLUTION = "Solution met user-defined criterion";
	
	/**
	 * Error message: no decision variables were defined
	 */
	public final static String ERR_NO_VARIABLES = "No discrete or continuous decision variables "
													+ "were defined";

	/**
	 * Error message: no optimization objectives were defined
	 */
	public static final String ERR_NO_OBJECTIVES = "No optimization objectives were defined";
	
	/**
	 * Error message: method {@link #execute(String) called with an invalid process id
	 */
	public static final String ERR_INVALID_EXECUTION_PROCESS_ID = "The process id is not "
																	+ "recognized";
	
	/**
	 * Error message: the population is empty
	 */
	public static final String ERR_POP_EMPTY = "The population is empty";
	
	/**
	 * Executor process id: populate {@link #genBuffer}
	 */
	public final static String EXEC_POPULATE_BUFFER = "Populate buffer";
	
	/**
	 * Default value for {@link #threadCount}
	 */
	public final static int DEF_THREAD_COUNT = 1;
	
	/**
	 * Default value for {@link #concurrentUpdates}
	 */
	public final static boolean DEF_CONCURRENT_UPDATES = false;
	
	/**
	 * Default value for {@link #evaluationTimeLimit}
	 */
	public final static long DEF_EVALUATION_TIME_LIMIT = -1;
	
	/**
	 * Default value for {@link #randomSolutionRatio}
	 */
	public final static double DEF_RANDOM_SOLUTION_RATIO = 1.0;

	/**
	 * Index of generator for solution root: created randomly 
	 */
	public final static int GEN_INDEX_RANDOM = -1;
	
	/**
	 * Index of generator for solution root: created by user 
	 */
	public final static int GEN_INDEX_USER = -2;
	
	/**
	 * Maximum time in milliseconds to wait for the threads to be interrupted before returning the 
	 * results
	 */
	public final static int MAX_TIMEOUT = 10000;
	
	// --------------------------------------------------------------------------------------------
	// Attributes - Structure
	// --------------------------------------------------------------------------------------------
	
	/**
	 * The list of the discrete variables of the problem
	 */
	private ArrayList<DiscVar> discVars;
	
	/**
	 * The list of the continuous variables of the problem
	 */
	private ArrayList<ContVar> contVars;
	
	/**
	 * The list of objectives of the optimization problem
	 */
	private ArrayList<Objective> objectives;
	
	/**
	 * The solution object that allows to create and evaluate new solutions
	 */
	private Solution solution;
	
	/**
	 * The instance of the object implementing the {@link Monitor} interface to communicate with 
	 * the caller application
	 */
	private Monitor monitor;
	
	/**
	 * A group of solutions to the optimization problem organized using a customized strategy for
	 * balancing the trade-offs between the multiple objectives. The population has a limited
	 * capacity and it is supposed to maintain the most worthwhile solutions found so far.
	 */
	private GroupMergePopulation population;
	
	/**
	 * A list with all solutions evaluated. <code>null</code> if the history is not to be kept as
	 * defined when calling the constructor.
	 */
	private ArrayList<SolutionWrapper> allSolutions;
	
	/**
	 * A table with all the solutions that at any time made part of the first front of the 
	 * {@link #population} indexed by their solution index
	 */
	private Hashtable<Integer, SolutionWrapper> hallOfFame;
	
	/**
	 * The ensemble of low-level optimization algorithms (or "generators") that are used 
	 * alternately to create new solutions
	 */
	private Ensemble ensemble;
	
	/**
	 * A queue that stores generated solution roots that need to be analyzed and offered to the
	 * {@link #population} 
	 */
	private Queue<SolutionRoot> genBuffer;
	
	/**
	 * A list with the threads that evaluate the new solutions after being generated. Evaluations 
	 * are run within these threads to take advantage of of multi-core CPUs and speeding up run 
	 * times.
	 */
	private ArrayList<EvaluatorThread> threads;
	
	// --------------------------------------------------------------------------------------------
	// Attributes - Parameters
	// --------------------------------------------------------------------------------------------
	
	/**
	 * The name of the optimization problem being solved 
	 */
	private String problem;
	
	/**
	 * An index that identifies the current run of the optimization algorithm 
	 */
	private int runIndex;
	
	/**
	 * The maximum optimization time in milliseconds
	 */
	private long timeLimit;
	
	/**
	 * The maximum number of solutions to be evaluated
	 */
	private int solutionLimit;
	
	/**
	 * The number of concurrent threads to process new solutions
	 */
	private int threadCount;
	
	/**
	 * True if the generation of new solutions and the updating of the {@link #population} are 
	 * allowed to happen on a different thread. False if all should run sequentially.
	 */
	private boolean concurrentUpdates;
	
	/**
	 * The file route for the file to save {@link #hallOfFame} to. "" if no file should be 
	 * created.
	 */
	private String hallOfFameFile;
	
	/**
	 * The maximum number of milliseconds an evaluation is allowed to run before it is discarded.
	 * -1 if evaluations should never be stopped.
	 */
	private long evaluationTimeLimit;
	
	/**
	 * The number of solutions to be created randomly at the beginning of the optimization process
	 * as the percentage of the {@link #population}'s capacity. If smaller than 1.0, it can only
	 * be honored if there are enough user predefined solutions to complete the initial population.
	 */
	private double randomSolutionRatio;
	
	// --------------------------------------------------------------------------------------------
	// Attributes - Control
	// --------------------------------------------------------------------------------------------
	
	/**
	 * The optimization starting time in milliseconds
	 */
	private long startTime;
	
	/**
	 * The number of solution roots offered so far; used for assigning consecutive indexes to new
	 * solution roots
	 */
	private int offerCount;
	
	/**
	 * The number of solutions evaluated so far
	 */
	private int evalCount;
	
	/**
	 * Termination message for the optimization process; "" if MAESTRO should continue running
	 */
	private String terminateFlag;
	
	/**
	 * True if the caller has been notified of the termination of the optimization process. False 
	 * otherwise. This flag is used to prevent the notification to be issued more than once.
	 */
	private boolean terminationNotified;
	
	// --------------------------------------------------------------------------------------------
	// Constructors
	// --------------------------------------------------------------------------------------------
	
	/**
	 * @param problem		{@link #problem}
	 * @param runIndex		{@link #runIndex}
	 * @param solution		{@link #solution}
	 * @param monitor		{@link #monitor}
	 * @param keepHistory	True if all solutions generated should be stored. False otherwise.
	 */
	public Optimizer(String problem, int runIndex, Solution solution, Monitor monitor,
						boolean keepHistory)
	{
		this.problem		= problem;
		this.runIndex		= runIndex;
		discVars			= null;
		contVars			= null;
		objectives			= new ArrayList<>();
		this.solution		= solution;
		this.monitor		= monitor;
		genBuffer			= new LinkedBlockingQueue<>();
		population			= new GroupMergePopulation();
		ensemble			= new Ensemble();
		threadCount			= DEF_THREAD_COUNT;
		concurrentUpdates	= DEF_CONCURRENT_UPDATES;
		evaluationTimeLimit	= DEF_EVALUATION_TIME_LIMIT;
		randomSolutionRatio	= DEF_RANDOM_SOLUTION_RATIO;
		population.setConcurrentUpdate(concurrentUpdates);
		allSolutions		= keepHistory ? new ArrayList<>() : null;
		hallOfFame			= new Hashtable<>();
		hallOfFameFile		= "";
	}
	
	// --------------------------------------------------------------------------------------------
	// Getters and setters
	// --------------------------------------------------------------------------------------------
	
	/**
	 * @return {@link #problem}
	 */
	public String getProblem() 
	{
		return problem;
	}

	/**
	 * @param problem {@link #problem}
	 */
	public void setProblem(String problem) 
	{
		this.problem = problem;
	}

	/**
	 * @return {@link #runIndex}
	 */
	public int getRunIndex() 
	{
		return runIndex;
	}

	/**
	 * @param runIndex {@link #runIndex}
	 */
	public void setRunIndex(int runIndex) 
	{
		this.runIndex = runIndex;
	}

	/**
	 * @return {@link #discVars}
	 */
	public ArrayList<DiscVar> getDiscVars()
	{
		return discVars;
	}

	/**
	 * @return {@link #contVars}
	 */
	public ArrayList<ContVar> getContVars()
	{
		return contVars;
	}

	/**
	 * @return {@link #objectives}
	 */
	public ArrayList<Objective> getObjectives()
	{
		return objectives;
	}

	/**
	 * @return {@link #solution}
	 */
	public Solution getSolution()
	{
		return solution;
	}
	
	/**
	 * @return {@link #population}
	 */
	public Population getPopulation() 
	{
		return population;
	}
	
	/**
	 * @return The maximum number of solutions that can be stored in the {@link #population} 
	 */
	public int getPopCapacity()
	{
		return population.getCapacity();
	}

	/**
	 * @param popCapacity The maximum number of solutions that can be stored in the 
	 * {@link #population} 
	 */
	public void setPopCapacity(int popCapacity)
	{
		this.population.setCapacity(popCapacity);
	}
	
	/**
	 * @return <code>true</code> if solutions with different defining values but with exactly the 
	 * same performance should be allowed in the population. (Only numerical objectives are taken 
	 * into account for the sake of performance.)
	 */
	public boolean areEqPerformersAllowed()
	{
		return population.areEqPerformersAllowed();
	}
	
	/**
	 * @param allowEqPerformers <code>true</code> if solutions with different defining values but 
	 * with exactly the same performance should be allowed in the population. (Only numerical 
	 * objectives are taken into account for the sake of performance.)
	 */
	public void allowEqPerformers(boolean allowEqPerformers)
	{
		population.allowEqPerformers(allowEqPerformers);
	}

	/**
	 * @return {@link #threadCount}
	 */
	public int getThreadCount() 
	{
		return threadCount;
	}

	/**
	 * @param threadCount {@link #threadCount}
	 */
	public void setThreadCount(int threadCount)
	{
		this.threadCount = threadCount;
	}
	
	/**
	 * @return {@link #concurrentUpdates}
	 */
	public boolean getConcurrentUpdates() 
	{
		return concurrentUpdates;
	}

	/**
	 * @param concurrentUpdates {@link #concurrentUpdates}
	 */
	public void setConcurrentUpdates(boolean concurrentUpdates) 
	{
		this.concurrentUpdates = concurrentUpdates;
		population.setConcurrentUpdate(concurrentUpdates);
	}

	/**
	 * @return {@link Ensemble#genRatio}
	 */
	public double getGenRatio() 
	{
		return ensemble.getGenRatio();
	}
	
	/**
	 * @param genRatio {@link Ensemble#genRatio}
	 */
	public void setGenRatio(double genRatio)
	{
		ensemble.setGenRatio(genRatio);
	}
	
	/**
	 * @return {@link Ensemble#genMin}
	 */
	public double getGenMin() 
	{
		return ensemble.getGenMin();
	}
	
	/**
	 * @param genMin {@link Ensemble#genMin}
	 */
	public void setGenMin(double genMin)
	{
		ensemble.setGenMin(genMin);
	}
	
	/**
	 * @return {@link Ensemble#absGenMin}
	 */
	public int getAbsGenMin() 
	{
		return ensemble.getAbsGenMin();
	}
	
	/**
	 * @param absGenMin {@link Ensemble#absGenMin}
	 */
	public void setAbsGenMin(int absGenMin) 
	{
		ensemble.setAbsGenMin(absGenMin);
	}
	
	/**
	 * @return {@link Ensemble#weightPop}
	 */
	public double getWeightPop() 
	{
		return ensemble.getWeightPop();
	}
	
	/**
	 * @param weightPop {@link Ensemble#weightPop}
	 */
	public void setWeightPop(double weightPop) 
	{
		ensemble.setWeightPop(weightPop);
	}
	
	/**
	 * @return {@link Ensemble#weightFront1}
	 */
	public double getWeightFront1() 
	{
		return ensemble.getWeightFront1();
	}
	
	/**
	 * @param weightFront1 {@link Ensemble#weightFront1}
	 */
	public void setWeightFront1(double weightFront1) 
	{
		ensemble.setWeightFront1(weightFront1);
	}
	
	/**
	 * @return {@link GroupMergePopulation#capacity}
	 */
	public int getPopulationCapacity()
	{
		return population.getCapacity();
	}
	
	/**
	 * @param capacity {@link GroupMergePopulation#capacity}
	 */
	public void setPopulationCapacity(int capacity)
	{
		population.setCapacity(capacity);
	}
	
	/**
	 * @return {@link #evaluationTimeLimit}
	 */
	public long getEvaluationTimeLimit()
	{
		return evaluationTimeLimit;
	}

	/**
	 * @param evaluationTimeLimit {@link #evaluationTimeLimit}
	 */
	public void setEvaluationTimeLimit(long evaluationTimeLimit)
	{
		this.evaluationTimeLimit = evaluationTimeLimit;
	}

	/**
	 * @return {@link #randomSolutionRatio}
	 */
	public double getRandomSolutionRatio()
	{
		return randomSolutionRatio;
	}

	/**
	 * @param randomSolutionRatio {@link #randomSolutionRatio}
	 */
	public void setRandomSolutionRatio(double randomSolutionRatio)
	{
		this.randomSolutionRatio = randomSolutionRatio;
	}

	/**
	 * @return {@link #timeLimit}
	 */
	public long getTimeLimit()
	{
		return timeLimit;
	}
	
	/**
	 * @return {@link #solutionLimit}
	 */
	public int getSolutionLimit()
	{
		return solutionLimit;
	}
	
	/**
	 * @return {@link #startTime}
	 */
	public long getStartTime()
	{
		return startTime;
	}
	
	/**
	 * @return {@link #offerCount}
	 */
	public int getOfferCount()
	{
		return offerCount;
	}
	
	/**
	 * @return {@link #evalCount}
	 */
	public int getEvalCount()
	{
		return evalCount;
	}
	
	/**
	 * @return {@link #terminateFlag}
	 */
	public String getTerminationMessage()
	{
		return terminateFlag;
	}
	
	/**
	 * @return The list of solutions in the first front of the {@link #population}
	 */
	public ArrayList<SolutionWrapper> getFirstFront()
	{
		if (population.getFronts().size() == 0)
			population.forceUpdate();
		return population.getFronts().get(0).getSolutions();
	}
	
	/**
	 * @return The list of solutions in the {@link #population}
	 */
	public ArrayList<SolutionWrapper> getSolutionsInPopulation()
	{
		if (population.getFronts().size() == 0)
			population.forceUpdate();
		ArrayList<SolutionWrapper> solutions = new ArrayList<>();
		for (Front front : population.getFronts())
			solutions.addAll(front.getSolutions());
		return solutions;
	}
	
	/**
	 * @return {@link #allSolutions}
	 */
	public ArrayList<SolutionWrapper> getAllSolutions()
	{
		return allSolutions;
	}
	
	/**
	 * @return The list of solutions in the {@link #hallOfFame} in the order they were created
	 */
	public ArrayList<SolutionWrapper> getHallOfFame()
	{
		Enumeration<Integer> indEnum			= hallOfFame.keys();
		ArrayList<Integer> indices				= new ArrayList<>();
		while (indEnum.hasMoreElements())
			indices.add(indEnum.nextElement());
		Collections.sort(indices);
		ArrayList<SolutionWrapper> solutions	= new ArrayList<>();
		for (int i = 0; i < indices.size(); i++)
		{
			Integer index						= indices.get(i);
			solutions.add(hallOfFame.get(index));
		}
		return solutions;
	}

	/**
	 * @return {@link #hallOfFameFile}
	 */
	public String getHallOfFameFile()
	{
		return hallOfFameFile;
	}

	/**
	 * @param hallOfFameFile {@link #hallOfFameFile}
	 * @throws IOException If there is a problem creating the file
	 */
	public void setHallOfFameFile(String hallOfFameFile) throws IOException
	{
		this.hallOfFameFile	= hallOfFameFile;
		PrintWriter out		= new PrintWriter(new BufferedWriter(new FileWriter(
														hallOfFameFile, true)));
		out.close();
	}
	
	// --------------------------------------------------------------------------------------------
	// Methods
	// --------------------------------------------------------------------------------------------

	/**
	 * Adds a new discrete optimization variable with its possible values. The values are the 
	 * specified number of integers starting from the <code>min</code> value and sequentially on.
	 * @param name The identifier of the variable
	 * @param min The minimum integer value the variable can take
	 * @param count The number of values the variable can take
	 * @param scalar True if the values of the variable correspond to a scale
	 */
	public void addDiscVar(String name, int min, int count, boolean scalar)
	{
		DiscVar var = new DiscVar(name, min, count, scalar);
		if(discVars == null)
			discVars = new ArrayList<DiscVar>();
		discVars.add(var);
	}
	
	/**
	 * Adds a new discrete optimization variable with its possible values. The values are integers 
	 * from 0 and on, that map to the set of identifiers.
	 * @param name The identifier of the variable
	 * @param values The set of identifiers of the variable's possible values
	 * @param scalar True if the values of the variable correspond to a scale
	 */
	public void addDiscVar(String name, ArrayList<String> values, boolean scalar)
	{
		DiscVar var = new DiscVar(name, values, scalar);
		if(discVars == null)
			discVars = new ArrayList<DiscVar>();
		discVars.add(var);
	}
	
	/**
	 * @param discVars {@link #discVars}
	 */
	public void setDiscVars(ArrayList<DiscVar> discVars)
	{
		this.discVars = discVars;
	}
	
	/**
	 * Adds a new continuous optimization variable
	 * @param name Identifier of the variable
	 * @param min The minimum value the variable can take
	 * @param max The maximum value the variable can take
	 */
	public void addContVar(String name, double min, double max)
	{
		ContVar var = new ContVar(name, min, max);
		if(contVars == null)
			contVars = new ArrayList<ContVar>();
		contVars.add(var);
	}
	
	/**
	 * @param contVars {@link #contVars}
	 */
	public void setContVars(ArrayList<ContVar> contVars)
	{
		this.contVars = contVars;
	}
	
	/**
	 * @param index The index of the variable
	 * @return The name of the discrete variable
	 */
	public String getDiscVarName(int index)
	{
		return discVars.get(index).getName();
	}
	
	/**
	 * @param varIndex The index of the variable
	 * @param value The index of the value
	 * @return The identifier of the value of a variable
	 */
	public String getDiscValueID(int varIndex, int value)
	{
		DiscVar variable = discVars.get(varIndex);
		if(variable.getValues() == null)
			return value + "";
		else
			return variable.getValues().get(value);
	}
	
	/**
	 * @param index The index of the variable
	 * @return The name of the continuous variable
	 */
	public String getContVarName(int index)
	{
		return contVars.get(index).getName();
	}
	
	/**
	 * Validates that the values in the provided array are within the valid range of the 
	 * discrete optimization variables. Any values outside the range are replaced by the 
	 * corresponding limit.
	 * @param discValues The list of values to validate
	 */
	public void validateDiscValues(ArrayList<Integer> discValues)
	{
		if (discVars == null)
			return;
		else
			if (discValues == null)
				throw new IllegalArgumentException("The value list is null");
		if (discValues.size() < discVars.size())
			throw new IllegalArgumentException("The list has only " + discValues.size() + 
										" values; there are " + discVars.size() + " variables");
		for (int v = 0; v < discVars.size(); v++)
			discValues.set(v, discVars.get(v).validate(discValues.get(v)));
	}
	
	/**
	 * Validates that the values in the provided array are within the valid range of the 
	 * continuous optimization variables. Any values outside the range are replaced by the 
	 * corresponding limit.
	 * @param contValues The list of values to validate
	 */
	public void validateContValues(ArrayList<Double> contValues)
	{
		if (contVars == null)
			return;
		else
			if (contValues == null)
				throw new IllegalArgumentException("The value list is null");
		if (contValues.size() < contVars.size())
			throw new IllegalArgumentException("The list has only " + contValues.size() + 
										" values; there are " + contVars.size() + " variables");
		for (int v = 0; v < contVars.size(); v++)
			contValues.set(v, contVars.get(v).validate(contValues.get(v)));
	}
	
	/**
	 * Adds a new numerical comparing optimization objective. Solutions are compared through 
	 * the values obtained in {@link maestro_mo.solution.Solution#getFitness}.
	 * @param index The unique index of the objective
	 * @param id The identifier of the objective
	 * @param maximization True if the numerical fitness value for this objective is to be 
	 * maximized. False if it is to be minimized.
	 */
	public void addNumericalObjective(int index, String id, boolean maximization)
	{
		objectives.add(new Objective(index, id, maximization));
	}
	
	/**
	 * Adds a new custom optimization objective. Instead of comparing a single numerical value,
	 * the custom objective calls the {@link maestro_mo.solution.Solution#compareTo} custom 
	 * function.
	 * @param index The unique index of the objective
	 * @param id The identifier of the objective
	 */
	public void addCustomObjective(int index, String id)
	{
		objectives.add(new Objective(index, id));
	}
	
	/**
	 * Adds a new low-level optimization algorithm or generator to the {@link #ensemble}. If no 
	 * custom generators are added, a default ensemble is used.
	 * @param generator The low-level optimization algorithm to add
	 */
	public void addGenerator(Generator generator)
	{
		ensemble.addGenerator(generator);
	}
	
	/**
	 * @return {@link Ensemble#generators}
	 */
	public ArrayList<GenWrapper> getGenerators()
	{
		return ensemble.getGenerators();
	}
	
	/**
	 * @param genIndex The index of the generator method
	 * @return The identifier of the generator method
	 */
	public String getGeneratorId(int genIndex)
	{
		return ensemble.getGeneratorId(genIndex);
	}
	
	/**
	 * @param genIndex The index of the generator method
	 * @return The short identifier of the generator method
	 */
	public String getGeneratorShortId(int genIndex)
	{
		switch (genIndex)
		{
			case GEN_INDEX_RANDOM:	return "Random";
			case GEN_INDEX_USER:	return "User";
			default:				return ensemble.getGeneratorShortId(genIndex);
		}
	}
	
	/**
	 * @return {@link Ensemble#generationHistory}
	 */
	public ArrayList<String> getGenerationHistory()
	{
		return ensemble.getGenerationHistory();
	}
	
	/**
	 * Adds a predefined solution root to be analyzed and offered to the population
	 * @param root The solution root to add
	 */
	public void addPredefinedSolution(SolutionRoot root)
	{
		this.validateDiscValues(root.getDiscValues());
		this.validateContValues(root.getContValues());
		root.setGenIndex(GEN_INDEX_USER);
		genBuffer.offer(root);
	}
	
	/**
	 * Initializes the optimization process which runs until the time limit is reached, the maximum
	 * number of solutions is generated or the method <code>terminate()</code> is called. When the 
	 * process is finished, the method <code>terminate()</code> in the <code>Monitor</code> is 
	 * called.
	 * @param timeLimit The time limit for the optimization process in milliseconds
	 * @param solutionLimit The maximum number of candidate solutions that should be processed
	 */
	public synchronized void startOptimization(long timeLimit, int solutionLimit)
	{
		// Check if the optimization problem was formulated in a valid way
		if (discVars == null && contVars == null)
			throw new RuntimeException(ERR_NO_VARIABLES);
		if (objectives.size() == 0)
			throw new RuntimeException(ERR_NO_OBJECTIVES);
		
		// Stop threads
		if (threads != null)
		{
			for (EvaluatorThread thread : threads)
				thread.end();
			threads.clear();
		}
		
		// Prepare data structures
		for (Objective objective : objectives)
			population.addObjective(objective);
		ensemble.setPopulation(population);
		ensemble.setVariables(discVars, contVars);
		ensemble.setObjectives(objectives);
		startTime					= System.currentTimeMillis();
		offerCount					= 0;
		evalCount					= 0;
		terminateFlag				= "";
		this.timeLimit				= timeLimit;
		this.solutionLimit			= solutionLimit;
		prepareHallOfFameFile();
		terminationNotified			= false;
		
		// Generate initial random population
		initialRandomPopulation();
		
		// Launch threads
		threads						= new ArrayList<>();
		for (int t = 0; t < threadCount; t++)
		{
			EvaluatorThread thread	= new EvaluatorThread(this);
			threads.add(thread);
		}
		for (EvaluatorThread thread : threads)
			thread.start();
	}
	
	/**
	 * Randomly generates a number of solution roots and adds them to the {@link #genBuffer}. If no
	 * predefined solution root was defined, the number of roots generated is the same as the 
	 * target size of the population.
	 */
	private void initialRandomPopulation() 
	{
		int popCapacity		= population.getCapacity();
		int toCompletePop	= Math.max(0, popCapacity - genBuffer.size());
		int randSol			= Math.max(toCompletePop, (int) (popCapacity*randomSolutionRatio));
		ArrayList<ArrayList<Integer>> discValues = null;
		if (discVars != null)
		{
			discValues		= new ArrayList<ArrayList<Integer>>();
			for (int j = 0 ; j < discVars.size() ; j++)
				discValues.add(discVars.get(j).generateRandomValues(randSol));		
		}
		ArrayList<ArrayList<Double>> contValues = null;
		if (contVars != null)
		{
			contValues		= new ArrayList<ArrayList<Double>>();
			for (int j = 0 ; j < contVars.size() ; j++)
				contValues.add(contVars.get(j).generateRandomValues(randSol, true));
		}
		for(int i = 0 ; i < randSol ; i++)
		{
			ArrayList<Integer> solDiscVals = discValues == null ? null : new ArrayList<Integer>();
			if(solDiscVals != null)
				for(int j = 0 ; j < discVars.size() ; j++)
					solDiscVals.add(discValues.get(j).get(i));
			ArrayList<Double> solContVals = contValues == null ? null : new ArrayList<Double>();
			if(solContVals != null)
				for(int j = 0 ; j < contVars.size() ; j++)
					solContVals.add(contValues.get(j).get(i));
			SolutionRoot solution = new SolutionRoot(solDiscVals, solContVals);
			genBuffer.offer(solution);
		}
	}
	
	/**
	 * Writes the header of the hall of fame file
	 */
	private void prepareHallOfFameFile() 
	{
		PrintWriter out;
		try 
		{
			if (!hallOfFameFile.equals(""))
			{
				out = new PrintWriter(new BufferedWriter(new FileWriter(
																hallOfFameFile, true)));
				String line		= Reporter.SOLUTIONS_ID + "\t" + Reporter.GENERATORS_ID + "\t";
				line			+= solution.getReportHeader() + "\t";
				if (discVars != null)
					for (int i = 0; i < discVars.size(); i++)
						line		+= getDiscVarName(i) + "\t";
				if (contVars != null)
					for (int i = 0; i < contVars.size(); i++)
						line		+= getContVarName(i) + "\t";
				out.println(line);
				out.close();
			}
		} catch (IOException e) 
		{
			e.printStackTrace();
		}
	}

	/**
	 * @return The next solution root to be evaluated
	 */
	public synchronized SolutionRoot getSolutionRoot()
	{		
		if(!terminateFlag.equals(""))
			return null;
		
		if(genBuffer.size() == 0)	// Execute on the same thread
			synchronized (genBuffer)
			{
				generateRoots();
			}
		else if(genBuffer.size() <= threads.size() && concurrentUpdates)	// Execute on a
		{																	// separate thread
			ExecutorThread exec = new ExecutorThread(this);
			exec.start(EXEC_POPULATE_BUFFER);
		}		
		return genBuffer.poll();
	}
	
	/**
	 * Adds new solution roots to the {@link #genBuffer} so that they can be evaluated
	 */
	public void generateRoots()
	{
		if(terminateFlag.equals(""))
		{
			if (population.getTotalSize() == 0)
				throw new RuntimeException(ERR_POP_EMPTY);
			if (population.size() == 0)
				population.forceUpdate();
			
			ArrayList<SolutionRoot> roots = ensemble.generateSolutions();
			if(roots != null)
				for(SolutionRoot root : roots)
				{
					validateDiscValues(root.getDiscValues());
					validateContValues(root.getContValues());
					genBuffer.offer(root);
				}
		}
	}
	
	/**
	 * Offers a generated solution root to be evaluated and offered to the population
	 * @param root The solution root to be evaluated and offered to the population
	 */
	public void offerSolutionRoot(SolutionRoot root)
	{
		if (terminateFlag.equals(""))
		{
			// Evaluate solution
			int solIndex					= ++offerCount;
			int genIndex					= root.getGenIndex();
			ArrayList<Integer> discValues	= root.getDiscValues();
			ArrayList<Double> contValues	= root.getContValues();
			Object extra					= root.getExtra();
			Solution sol			= solution.createNew(solIndex, discValues, contValues, extra);
			if (!sol.isValid())
				return;
			SolutionWrapper wrapper			= new SolutionWrapper(this, sol, genIndex);
			wrapper.setIndex(solIndex);
			wrapper.setUserLabel(root.getLabel());
			
			// Update population
			synchronized (population)
			{
				population.offerSolution(wrapper);
			}
			
			// Add to solution list
			if (allSolutions != null)
				allSolutions.add(wrapper);
			
			// Check for met termination criteria
			evalCount++;
			checkTerminate();
			
			// Verify if any thread is stuck
			verifyThreadsRunning();
		}
		
		// Verify for convergence
		if (terminateFlag.equals(""))
			if (solution.optimizationConverged())
			{
				terminateFlag				= TERMINATION_SOLUTION + ": " + solution.getId();
				terminate(terminateFlag);
			}
	}

	/**
	 * Verifies if the provided solution should be added to the {@link #hallOfFame}
	 * @param solution The solution to consider adding
	 */
	public void rankChange(SolutionWrapper solution)
	{
		if (solution.getRank() == 1)
		{
			Integer index = solution.getIndex();
			if (!hallOfFame.containsKey(index))
			{
				hallOfFame.put(index, solution);
				storeHallOfFamer(solution);
			}
		}
	}
	
	/**
	 * Appends the summary of the provided solution to the elite history file
	 * @param solution
	 */
	private void storeHallOfFamer(SolutionWrapper solution)
	{
		try 
		{
			if (!hallOfFameFile.equals(""))
			{
				PrintWriter out	= new PrintWriter(new BufferedWriter(new FileWriter(
															hallOfFameFile, true)));
				String line		= solution.getId() + "\t";
				line			+= getGeneratorShortId(solution.getGenIndex()) + "\t";				
				line			+= solution.getSolution().getReport() + "\t";
				ArrayList<Integer> discValues	= solution.getSolution().getDiscValues();
				ArrayList<Double> contValues	= solution.getSolution().getContValues();
				if (discValues != null)
					for (int i = 0 ; i < discValues.size() ; i++)
					{
						int value = discValues.get(i);
						line		+= String.valueOf(getDiscValueID(i, value)) + "\t";
					}
				if (contValues != null)
					for (Double value : contValues)
						line		+= value + "\t";
				out.println(line);
				out.close();
			}
		} catch (IOException e) 
		{
			e.printStackTrace();
		}
	}

	/**
	 * Verifies if the optimization process should end
	 */
	private void checkTerminate() 
	{
		long time			= System.currentTimeMillis();
		if(time - startTime >= timeLimit)
			terminateFlag	= TERMINATION_TIME;
		if(evalCount >= solutionLimit)
			terminateFlag	= TERMINATION_EVALUATION_LIMIT;
		if(!terminateFlag.equals(""))
			terminate(terminateFlag);
	}

	/**
	 * Verifies if any evaluation is taking longer than {@link #evaluationTimeLimit}. If so, the
	 * thread is interrupted and a new one is launched. The verification happens every time a
	 * number of solutions equal to the {@link #population}'s capacity has been evaluated.
	 */
	private void verifyThreadsRunning()
	{
		if (evaluationTimeLimit > 0)
			if (evalCount%population.getCapacity() == 0)
			{				
				long current			= System.currentTimeMillis();
				for (EvaluatorThread thread : threads)
					if (thread.isInterrupted() ||
							current - thread.getAnalysisStart() > evaluationTimeLimit)
					{
						thread.end();
						thread.interrupt();
						thread			= new EvaluatorThread(this);
						thread.start();
					}
			}
	}

	/**
	 * Terminates the optimization process if the message is different form an empty string (""). 
	 * In that case, the method {@link Monitor#terminate(String)} is called.
	 * @param message The reason for terminating the algorithm
	 */
	public synchronized void terminate(String message)
	{
		terminateFlag = message.equals("") ? terminateFlag : message;
		if (!terminateFlag.equals(""))
		{
			// Stop threads
			if (threads != null)
			{
				for (EvaluatorThread thread : threads)
				{
					thread.end();
					thread.interrupt();
				}
				
				// Wait until the threads are interrupted
				long start				= System.currentTimeMillis();
				for (EvaluatorThread thread : threads)
				{
					boolean timeOut		= false;
					while (!(thread.isInterrupted() || timeOut))
					{
						thread.interrupt();
						long current	= System.currentTimeMillis();
						timeOut			= current - start > MAX_TIMEOUT;
					}
					if (timeOut)
						break;
				}
				threads = null;
			}
			
			// Notify caller
			if (!terminationNotified)
			{
				terminationNotified = true;
				
				// Update the population
				population.forceUpdate();
				
				monitor.terminate(terminateFlag);
			}
		}
	}

	@Override
	public void execute(String processId) 
	{
		if (processId.equals(EXEC_POPULATE_BUFFER))
			generateRoots();
		else
			throw new RuntimeException(ERR_INVALID_EXECUTION_PROCESS_ID + ": " + processId);
	}
	
	/**
	 * Writes a report file containing the results from the execution of MAESTRO
	 * @param customFileName	True if a custom file name will be provided; false if the file
	 * 							name should be generated using the {@link #problem} and the 
	 * 							{@link #runIndex}
	 * @param folder			The system path of the folder to save the report; complete path
	 * 							if <code>customFileName</code> is true
	 * @param writeConfig 		True if the parameters and generators of MAESTRO should be included
	 * @param writeGenHist 		True if the history of generators used should be included
	 * @param entirePopulation	True if the entire population should be included; false if only the
	 * 							Pareto (first) front should be included
	 * @param writeHallOfFame	True if the solutions in the hall of fame should be included
	 * @param writeAllSolutions	True if all solutions should be included
	 * @throws IOException		If the file cannot be created
	 */
	public void writeReport(boolean customFileName, String folder, 
			boolean writeConfig, boolean entirePopulation, boolean writeGenHist, 
			boolean writeHallOfFame, boolean writeAllSolutions) throws IOException
	{
		String fileRoute = folder + (customFileName ? "" : "/" 
										+ problem + " " + runIndex + ".txt");
		Reporter.writeReport(this, fileRoute, writeConfig, entirePopulation, writeGenHist, 
									writeHallOfFame, writeAllSolutions);
	}
	
}