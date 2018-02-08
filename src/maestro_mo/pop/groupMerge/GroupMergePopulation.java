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
import java.util.Collections;
import java.util.HashSet;

import probDist.Normal;
import utilities.Utilities;
import utilities.stat.ContSeries;
import utilities.thread.Executor;
import utilities.thread.ExecutorThread;
import maestro_mo.Objective;
import maestro_mo.pop.Population;
import maestro_mo.solution.SolutionWrapper;

/**
 * A group-merging solution population for multi-objective optimization problems. The group-merging 
 * distinction comes from the fact that the population is only updated after a new group of 
 * solutions, which has a given relative size compared to the population capacity, is created. The 
 * updated population is created after merging the two groups and then selecting half of the 
 * individuals: those in dominating fronts and that provide the higher variety.
 * @author Felipe Hernández
 */
public class GroupMergePopulation implements Population, Executor
{
	
	// --------------------------------------------------------------------------------------------
	// Constants
	// --------------------------------------------------------------------------------------------
	
	/**
	 * Name of the attribute of the solutions that counts how many times they have been dominated. 
	 * This value is used while merging the current population with the buffer.
	 */
	public final static String DOMINATION_COUNTER = "Times dominated";
	
	/**
	 * Default value for {@link #capacity}
	 */
	public final static int D_CAPACITY = 20;
	
	/**
	 * Default value for {@link #allowEqPerformers}
	 */
	public final static boolean D_ALLOW_EQ_PERFORMERS = true;
	
	/**
	 * Default value for {@link #updateTrigger}
	 */
	public final static double D_UPDATE_TRIGGER = 1.0;
	
	/**
	 * Default value for {@link #concurrentUpdate}
	 */
	public final static boolean D_CONCURRENT_UPDATE = false;
	
	/**
	 * Default value for {@link #minQ}
	 */
	public final static double D_MIN_Q = 0.1;
	
	/**
	 * Default value for {@link #maxQ}
	 */
	public final static double D_MAX_Q = 10.0;
	
	/**
	 * Default value for {@link #greedToQPow}
	 */
	public final static double D_GREED_TO_Q_POW = 5.0;
	
	/**
	 * Population update identifier for the {@link #execute(String)} method
	 */
	public final static String EXECUTE_UPDATE_POP = "Update population";
	
	// --------------------------------------------------------------------------------------------
	// Attributes
	// --------------------------------------------------------------------------------------------
	
	/**
	 * A list with the objectives of the optimization problem
	 */
	private ArrayList<Objective> objectives;
	
	/**
	 * The target size of the current population
	 */
	private int capacity;
	
	/**
	 * Structure that stores the solutions in the current population. The structure is a list of
	 * fronts of type {@link Front}, each of which is a list of solutions. The fronts indicate the 
	 * level of non-dominance of the solutions. The solutions in the first front dominate 
	 * (according to the objectives) the solutions in the second front, the ones in the second 
	 * dominate the ones in the third, and so on recursively and transitively.
	 */
	private ArrayList<Front> currentPopulation;
	
	/**
	 * List with the recently added solutions building up the second group. When the list size 
	 * reaches {@link #updateTrigger}*{@link #capacity}, the current population is updated by 
	 * merging the solutions in the current population and the ones in this list by
	 * {@link #updatePopulation()}.
	 */
	private ArrayList<SolutionWrapper> buffer;
	
	/**
	 * Hash set used to store the defining values of all the solutions (both in the current 
	 * population and in the buffer) to avoid adding identical solutions
	 */
	private HashSet<SolValues> registry;
	
	/**
	 * <code>true</code> if solutions with different defining values but with exactly the same 
	 * performance should be allowed in the population. (Only numerical objectives are taken into 
	 * account for the sake of performance.)
	 */
	private boolean allowEqPerformers;
	
	/**
	 * Hash set used to store the fitness values of all the solutions (both in the current 
	 * population and in the buffer) to avoid adding equivalent-performing solutions if
	 * {@link #allowEqPerformers} is <code>false</code>.
	 */
	private HashSet<SolValues> perfReg;
	
	/**
	 * The percentage of solutions in the {@link #buffer} compared to the {@link #capacity} to 
	 * trigger the updating of the population
	 */
	private double updateTrigger;
	
	/**
	 * True if the updating of the population should be made in a separate execution thread. False
	 * if it is is to be run sequentially on the same thread that adds the triggering solution by 
	 * calling {@link #offerSolution(SolutionWrapper)}.
	 */
	private boolean concurrentUpdate;
	
	/**
	 * Minimum value for the selection weight assignment variable <i>q</i>, corresponding to a 
	 * maximum weight for the solutions in the first front. Weights are assigned according to the 
	 * probability density of a Normal distribution with mean 1 and standard deviation <i>qk</i>, 
	 * where <i>k</i> is the number of solutions in the population. The argument for the 
	 * distribution is the rank of the solution.
	 */
	private double minQ;
	
	/**
	 * Maximum value for the selection weight assignment variable <i>q</i>, corresponding to a 
	 * uniform probability for all the solutions. Weights are assigned according to the probability 
	 * density of a Normal distribution with mean 1 and standard deviation <i>qk</i>, where 
	 * <i>k</i> is the number of solutions in the population. The argument for the distribution is 
	 * the rank of the solution.
	 */
	private double maxQ;
	
	/**
	 * Power of the function to obtain the value of <i>q</i> for weight assignment from different
	 * greed values. A higher power increases the greed range in which the solutions in the first
	 * front are preferred; a lower power increases the range in which all solutions are equally
	 * likely to be selected.
	 */
	private double greedToQPow;
	
	/**
	 * True if the population is currently updating
	 */
	private boolean updating;
	
	// --------------------------------------------------------------------------------------------
	// Constructors
	// --------------------------------------------------------------------------------------------
	
	public GroupMergePopulation()
	{
		objectives			= new ArrayList<>();
		capacity			= D_CAPACITY;
		currentPopulation	= new ArrayList<>();
		buffer				= new ArrayList<>();
		registry			= new HashSet<>();
		perfReg				= new HashSet<>();
		updateTrigger		= D_UPDATE_TRIGGER;
		concurrentUpdate	= D_CONCURRENT_UPDATE;
		minQ				= D_MIN_Q;
		maxQ				= D_MAX_Q;
		greedToQPow			= D_GREED_TO_Q_POW;
		updating			= false;
	}
	
	// --------------------------------------------------------------------------------------------
	// Methods
	// --------------------------------------------------------------------------------------------

	/**
	 * @return {@link #allowEqPerformers}
	 */
	public boolean areEqPerformersAllowed()
	{
		return allowEqPerformers;
	}

	/**
	 * @param allowEqPerformers {@link #allowEqPerformers}
	 */
	public void allowEqPerformers(boolean allowEqPerformers)
	{
		this.allowEqPerformers = allowEqPerformers;
	}

	/**
	 * @return {@link #updateTrigger}
	 */
	public double getUpdateTrigger() 
	{
		return updateTrigger;
	}

	/**
	 * @param updateTrigger {@link #updateTrigger}
	 */
	public void setUpdateTrigger(double updateTrigger) 
	{
		this.updateTrigger = updateTrigger;
	}

	/**
	 * Getter of the {@link #concurrentUpdate} attribute
	 * @return {@link #concurrentUpdate}
	 */
	public boolean isUpdateConcurrent() 
	{
		return concurrentUpdate;
	}

	/**
	 * Setter of the {@link #concurrentUpdate} attribute
	 * @param concurrentUpdate {@link #concurrentUpdate}
	 */
	public void setConcurrentUpdate(boolean concurrentUpdate) 
	{
		this.concurrentUpdate = concurrentUpdate;
	}
	
	/**
	 * @return {@link #minQ}
	 */
	public double getMinQ() 
	{
		return minQ;
	}

	/**
	 * @param minQ {@link #minQ}
	 */
	public void setMinQ(double minQ) 
	{
		this.minQ = minQ;
	}

	/**
	 * @return {@link #maxQ}
	 */
	public double getMaxQ() 
	{
		return maxQ;
	}

	/**
	 * @param maxQ {@link #maxQ}
	 */
	public void setMaxQ(double maxQ) 
	{
		this.maxQ = maxQ;
	}

	/**
	 * @return {@link #greedToQPow}
	 */
	public double getGreedToQPow() 
	{
		return greedToQPow;
	}

	/**
	 * @param greedToQPow {@link #greedToQPow}
	 */
	public void setGreedToQPow(double greedToQPow) 
	{
		this.greedToQPow = greedToQPow;
	}

	@Override
	public void addObjective(Objective objective) 
	{
		objectives.add(objective);
	}
	
	@Override
	public ArrayList<Objective> getObjectives()
	{
		return objectives;
	}

	@Override
	public void clear()
	{
		currentPopulation.clear();
		buffer.clear();
		registry.clear();
		perfReg.clear();
	}

	@Override
	public synchronized int size() 
	{
		int size	= 0;
		for (Front front : currentPopulation)
			size += front.size();
		return size;
	}
	
	/**
	 * @return The size of the current population (the same as {@link #size()}) plus the size of 
	 * the buffer (recently added solutions not yet considered for their addition to the current 
	 * population)
	 */
	public synchronized int getTotalSize()
	{
		return registry.size();
	}
	
	@Override
	public int getCapacity()
	{
		return capacity;
	}

	/**
	 * @param capacity The target size of the current population
	 */
	public void setCapacity(int capacity) 
	{
		this.capacity = capacity > 0 ? capacity : 1;
		update();
	}

	@Override
	public boolean contains(SolutionWrapper solution)
	{
		SolValues values = new SolValues(solution);
		return registry.contains(values);
	}

	@Override
	public void offerSolution(SolutionWrapper solution) 
	{
		SolValues values = new SolValues(solution);
		if (!registry.contains(values) && !samePerformance(solution))
		{
			buffer.add(solution);
			registry.add(values);
			addToPerfReg(solution);
		}
		if (buffer.size() >= capacity)
			update();
	}
	
	@Override
	public void offerSolutions(ArrayList<SolutionWrapper> solutions)
	{
		for (SolutionWrapper solution : solutions)
		{
			SolValues values = new SolValues(solution);
			if (!registry.contains(values) && !samePerformance(solution))
			{
				buffer.add(solution);
				registry.add(values);
				addToPerfReg(solution);
			}
		}
		if (buffer.size() >= capacity)
			update();
	}

	/**
	 * Determines if the provided solution should be accepted into the population in light of the
	 * uniqueness of its performance values. Always <code>false</code> if 
	 * {@link #allowEqPerformers} is <code>true</code>.
	 * @param solution The offered solution
	 * @return <code>true</code> if there is already a solution with the same performance values
	 */
	private boolean samePerformance(SolutionWrapper solution)
	{
		return allowEqPerformers ? false : perfReg.contains(new SolValues(solution, objectives));
	}

	@Override
	public synchronized ArrayList<SolutionWrapper> getAllSolutions() 
	{
		ArrayList<SolutionWrapper> all = new ArrayList<SolutionWrapper>();
		for(Front front : currentPopulation)
			all.addAll(front.getSolutions());
		return all;
	}
	
	/**
	 * @return {@link #currentPopulation}
	 */
	public ArrayList<Front> getFronts()
	{
		return currentPopulation;
	}

	@Override
	public ArrayList<SolutionWrapper> select(int count) 
	{
		ArrayList<SolutionWrapper> all = getAllSolutions();
		ArrayList<SolutionWrapper> selection = new ArrayList<SolutionWrapper>();
		for(int i = 0 ; i < count ; i++)
		{
			int index = Utilities.uniformRandomSelect(all.size());
			selection.add(all.get(index));
		}
		return selection;
	}
	
	@Override
	public HashSet<SolutionWrapper> selectSet(int count) 
	{
		ArrayList<SolutionWrapper> all = getAllSolutions();
		HashSet<SolutionWrapper> selection = new HashSet<SolutionWrapper>();
		if(count >= all.size())
		{
			selection.addAll(all);
			return selection;
		}
		Collections.shuffle(all);
		for(int i = 0 ; i < count ; i++)
			selection.add(all.get(i));
		return selection;
	}

	@Override
	public ArrayList<SolutionWrapper> select(int count, double greed) 
	{
		ArrayList<SolutionWrapper> selection	= new ArrayList<>();
		if (size() != 0)
		{
			ContSeries weights					= computeWeights(greed);
			ArrayList<Double> frontIndices		= weights.sampleMultiple(count, true);
			for (Double frontIndex : frontIndices)
			{
				Front front		= currentPopulation.get((int)(double)frontIndex);
				int solIndex	= Utilities.uniformRandomSelect(front.size());
				selection.add(front.getSolutions().get(solIndex));
			}
		}
		return selection;
	}

	@Override
	public HashSet<SolutionWrapper> selectSet(int count, double greed) 
	{
		HashSet<SolutionWrapper> selection		= new HashSet<>();
		ArrayList<SolutionWrapper> temporal		= select(count, greed);
		selection.addAll(temporal);
		return selection;
	}
	
	/**
	 * Computes a selection weight value for each front depending on the level of greed and the
	 * number of solutions in each front. The higher the weight, the most likely solutions from
	 * that front are to be selected.
	 * @param greed A value between -1.0 and 1.0 that represents how much to favor solutions in 
	 * 				each of the fronts. 1.0 if only the solutions in the first front are to be 
	 * 				assigned non-negligible weights. 0.0 if all solutions should be weighted 
	 * 				uniformly. -1.0 if only the solutions in the last front should be assigned 
	 * 				non-negligible weights.
	 * @return The series of the indexes of the fronts with the corresponding weight
	 */
	private ContSeries computeWeights(double greed)
	{
		// Obtain q
		greed				= greed >  1.0 ?  1.0 : greed;
		greed				= greed < -1.0 ? -1.0 : greed;
		double temp			= Math.pow(1 - Math.abs(greed), greedToQPow);;
		double q			= minQ + (maxQ - minQ)*temp;
		
		// Compute weights
		ContSeries weights	= new ContSeries(true);
		int frontCount		= currentPopulation.size();
		int frontIndex		= greed >= 0 ? 0 : frontCount - 1;
		int solRank			= 1;
		boolean ok			= frontCount == 0;
		while (!ok)
		{
			Front front		= currentPopulation.get(frontIndex);
			int solCount	= front.size();
			double weight	= 0;
			for (int i = 0; i < solCount; i++)
			{
				weight		+= Normal.computepdf(1.0, q*size(), solRank);
				solRank++;
			}
			weights.addValue(frontIndex, weight);
			
			if (greed >= 0)	frontIndex++;
			else			frontIndex--;
			ok = frontIndex < 0 || frontIndex > frontCount - 1;
		}
		return weights;
	}
	
	/**
	 * Forces the updating of the population even if the number of solutions in the {@link #buffer} 
	 * is smaller than {@link #updateTrigger}*{@link #capacity} (this is the normal update 
	 * criteria). The updating is performed by calling {@link #updatePopulation()} and is executed 
	 * on a separate thread if {@link #concurrentUpdate} is true.
	 */
	public void forceUpdate()
	{		
		if (!updating)
		{
			if(concurrentUpdate)
			{
				ExecutorThread exec = new ExecutorThread(this);
				exec.start(EXECUTE_UPDATE_POP);
			}
			else
				updatePopulation();
		}
	}
	
	/**
	 * Updates the population if the number of solutions in the {@link #buffer} is equal or larger 
	 * than {@link #updateTrigger}*{@link #capacity}. The updating is performed by calling 
	 * {@link #updatePopulation()} and is executed on a separate thread if 
	 * {@link #concurrentUpdate} is true.
	 */
	public void update() 
	{		
		boolean bufferFull			= buffer.size() >= updateTrigger*capacity;
		boolean overflowed			= size() > capacity;
		if ((bufferFull || overflowed) && !updating)
		{
			if(concurrentUpdate)
			{
				ExecutorThread exec = new ExecutorThread(this);
				exec.start(EXECUTE_UPDATE_POP);
			}
			else
				updatePopulation();
		}
	}
	
	/**
	 * Merges the solutions in the current population with those in the buffer, sorts them in 
	 * fronts using fast non-dominated sorting 
	 * ({@link #fastNonDominatedSort(ArrayList, ArrayList, int)}), and then removes
	 * any solutions beyond the {@link #capacity} by removing excessive fronts and reducing the 
	 * last qualifying front ({@link Front#getReduced(int, ArrayList)}).
	 */
	private synchronized void updatePopulation()
	{		
		if(buffer.size() == 0 && size() <= capacity)
			return;
		
		updating		= true;
		
		// Merge current population with buffer
		ArrayList<SolutionWrapper> combined = new ArrayList<SolutionWrapper>();
		combined.addAll(buffer);
		buffer.clear();
		for (Front front : currentPopulation)
			combined.addAll(front.getSolutions());
		
		// Sort the combined population into domination fronts
		ArrayList<Front> tempFronts = fastNonDominatedSort(combined, objectives, capacity);
		
		// Reduce fronts to match the target size of the population
		boolean ok		= false;
		int added		= 0;
		int frontIndex	= 0;
		while (!ok)
		{
			Front front	= tempFronts.get(frontIndex);
			int inFront	= front.size();
			int needed	= capacity - added;
			if (needed < inFront)
			{
				front	= front.getReduced(needed, objectives);
				tempFronts.set(frontIndex, front);
				ok		= true;
			}
			added		+= inFront;
			frontIndex++;
			if (frontIndex == tempFronts.size())
				ok		= true;
		}
		
		// Update current population
		currentPopulation.clear();
		for (int i = 0; i < frontIndex; i++)
			currentPopulation.add(tempFronts.get(i));
		
		// Update registries
		registry.clear();
		perfReg.clear();
		for (Front front : currentPopulation)
			for (SolutionWrapper sol : front.getSolutions())
			{
				registry.add(new SolValues(sol));
				addToPerfReg(sol);
				if (buffer.contains(sol))
					buffer.remove(sol);
			}
		for (SolutionWrapper sol : buffer)
		{
			registry.add(new SolValues(sol));
			addToPerfReg(sol);
		}
		
		updating		= false;
	}
	
	/**
	 * Adds the fitness values corresponding to the numerical objectives of the provided solution
	 * to the {@link #perfReg}
	 * @param sol The solution whose fitness values are to be extracted
	 */
	private void addToPerfReg(SolutionWrapper sol)
	{
		if (!allowEqPerformers)
			perfReg.add(new SolValues(sol, objectives));
	}

	/**
	 * Sorts the provided population of solutions to a multi-objective optimization problem in a
	 * series of domination fronts. The solutions in the first front dominate the solutions in the
	 * second front and so on. The process is conducted using the fast non-dominated sorting
	 * algorithm described in the reference below: <br><br>
	 * Deb, K., Pratap, A., Agarwal, S. and Meyarivan, T., 2002, "A fast and elitist multiobjective 
	 * genetic algorithm: NSGA-II," <i>Evolutionary Computation</i>, IEEE Transactions on, 6 (2), 
	 * pp. 182-197
	 * @param population The original population to be sorted
	 * @param objectives The objectives of the optimization problem
	 * @param max The maximum number of solutions that are going to be accepted. The returned front
	 * list will only contain as many fronts as needed to contain this number of solutions. Use a
	 * negative number if this constraint is not to be used and all solutions are to be sorted. 
	 * @return The sorted population arranged in a list of domination fronts
	 */
	public static ArrayList<Front> fastNonDominatedSort(ArrayList<SolutionWrapper> population, 
														ArrayList<Objective> objectives, int max)
	{
		max = max < 0 ? Integer.MAX_VALUE : max;
		
		// Initialize domination list and domination counter
		for(SolutionWrapper sol : population)
		{
			sol.clearChildren();
			sol.setValue(DOMINATION_COUNTER, 0.0);
		}
		
		// Determine domination
		Front tempFront1 = new Front();
		for(int i = 0 ; i < population.size() ; i++)
		{
			SolutionWrapper sol1 = population.get(i);
			for(int j = i + 1 ; j < population.size() ; j++)
			{
				SolutionWrapper sol2 = population.get(j);
				int dominates = sol1.dominates(sol2, objectives);
				if(dominates != 0)
				{
					SolutionWrapper dominant = dominates > 0 ? sol1 : sol2;
					SolutionWrapper dominated = dominates > 0 ? sol2 : sol1;
					dominant.addChild(dominated);
					dominated.setValue(DOMINATION_COUNTER, 
													dominated.getValue(DOMINATION_COUNTER) + 1);
				}
			}
			if(sol1.getValue(DOMINATION_COUNTER) == 0)
			{
				sol1.setRank(1);
				tempFront1.add(sol1);
			}
		}
		
		// Create other fronts
		ArrayList<Front> fronts = new ArrayList<Front>();
		fronts.add(tempFront1);
		Front tempFront2 = new Front();
		int included = tempFront1.size();
		while(tempFront1.size() > 0 && included < max)
		{
			for(SolutionWrapper sol1 : tempFront1.getSolutions())
			{
				ArrayList<SolutionWrapper> dominated = (ArrayList<SolutionWrapper>)
															sol1.getChildren();
				for(SolutionWrapper sol2 : dominated)
				{
					int domCount = (int)sol2.getValue(DOMINATION_COUNTER) - 1;
					sol2.setValue(DOMINATION_COUNTER, domCount);
					if(domCount == 0)
					{
						sol2.setRank(fronts.size() + 1);
						tempFront2.add(sol2);
					}
				}
			}
			if(tempFront2.size() > 0)
				fronts.add(tempFront2);
			tempFront1 = tempFront2;
			tempFront2 = new Front();
		}
		return fronts;
	}

	@Override
	public void execute(String processId) 
	{
		if (processId.equals(EXECUTE_UPDATE_POP))
			updatePopulation();
	}

}
