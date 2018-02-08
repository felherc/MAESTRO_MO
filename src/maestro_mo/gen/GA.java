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
import java.util.Collections;

import maestro_mo.ContVar;
import maestro_mo.DiscVar;
import maestro_mo.Objective;
import maestro_mo.pop.Population;
import maestro_mo.solution.SolutionRoot;
import maestro_mo.solution.SolutionWrapper;
import probDist.Normal;
import probDist.Uniform;
import utilities.Utilities;

/**
 * This generator allows to create new solutions to a problem with discrete and continuous 
 * variables using a sorted base population of solutions and a Genetic Algorithm
 * @author Felipe Hernández
 */
public class GA implements Generator
{
	
	// --------------------------------------------------------------------------------------------
	// Constants
	// --------------------------------------------------------------------------------------------

	/**
	 * The identifier of the generator method
	 */
	public final static String ID = "Genetic Algorithm";
	
	/**
	 * The short version of the identifier of the generator method
	 */
	public final static String SHORT_ID = "GA";
	
	/**
	 * Attribute printing name for {@link #greed}
	 */
	public final static String PARAM_GREED = "Greed = ";
	
	/**
	 * The <code>kPerc</code> attribute printing name
	 */
	public final static String PARAM_K_PERC = "Tournament percentage = ";
	
	/**
	 * The <code>trunc</code> attribute printing name
	 */
	public final static String PARAM_TRUNC = "Truncated percentage = ";
	
	/**
	 * The <code>points</code> attribute printing name
	 */
	public final static String PARAM_POINTS = "Crossover points = ";
	
	/**
	 * The <code>pointUniform</code> attribute printing name
	 */
	public final static String PARAM_POINT_UNIFORM = "Use uniform crossover (UC) prob = ";
	
	/**
	 * The <code>pUniform</code> attribute printing name
	 */
	public final static String PARAM_P_UNIFORM = "UC probability = ";
	
	/**
	 * The <code>unifMethod</code> attribute printing name
	 */
	public final static String PARAM_UNIF_METHOD = "UC method (cont) = ";
	
	/**
	 * The <code>unifDistParam</code> attribute printing name
	 */
	public final static String PARAM_UNIF_DIST_PARAM = "UC distribution param = ";
	
	/**
	 * The <code>mutationProb</code> attribute printing name
	 */
	public final static String PARAM_MUTATION_PROB = "Mutation (mut) probability = ";
	
	/**
	 * The <code>randomMutation</code> attribute printing name
	 */
	public final static String PARAM_RANDOM_MUTATION = "Random mut weight = ";
	
	/**
	 * The <code>adjacentMutation</code> attribute printing name
	 */
	public final static String PARAM_ADJACENT_MUTATION = "Adjacent mut weight = ";
	
	/**
	 * The <code>boundaryMutation</code> attribute printing name
	 */
	public final static String PARAM_BOUNDARY_MUTATION = "Boundary mut weight = ";
	
	/**
	 * The <code>gaussianMutation</code> attribute printing name
	 */
	public final static String PARAM_GAUSSIAN_MUTATION = "Gaussian mut st. dev. = ";
	
	/**
	 * Fitness proportionate selection or Roulette-wheel selection printing name
	 */
	public final static String SEL_ROULETTE_PRINT = "roulette-wheel";
	
	/**
	 * Stochastic universal sampling selection printing name
	 */
	public final static String SEL_SUS_PRINT = "stochastic universal sampling";
	
	/**
	 * Tournament selection printing name
	 */
	public final static String SEL_TOURNAMENT_PRINT = "tournament";
	
	/**
	 * Either-or uniform crossover printing name
	 */
	public final static String UNIF_EITHER_OR_PRINT = "either-or";
	
	/**
	 * Uniform distribution uniform crossover printing name
	 */
	public final static String UNIF_UDIST_PRINT = "uniform ditribution";
	
	/**
	 * Normal distribution uniform crossover printing name
	 */
	public final static String UNIF_NORMALDIST_PRINT = "normal distribution";
	
	/**
	 * Index of the either-or uniform crossover method
	 */
	public final static int UNIF_EITHER_OR = 0;
	
	/**
	 * Index of the uniform distribution uniform crossover method
	 */
	public final static int UNIF_UDIST = 1;
	
	/**
	 * Index of the normal distribution uniform crossover method
	 */
	public final static int UNIF_NORMALDIST = 2;
	
	/**
	 * Default value for {@link #greed}
	 */
	public final static double D_GREED = 0.5;
	
	/**
	 * Default value for the <code>kPerc</code> attribute
	 */
	public final static double D_K_PERC = 0.2; 
	// Low significance for low dimensions. 0.2 for high dimensionality with tournament selection.
	
	/**
	 * Default value for the <code>trunc</code> attribute
	 */
	public final static double D_TRUNC = 0.75; 
	// More significant for more dimensions; higher is better
	
	/**
	 * Default value for the <code>points</code> attribute
	 */
	public final static int D_POINTS = 1; 
	// Low significance
	
	/**
	 * Default value for the <code>pointUniform</code> attribute
	 */
	public final static double D_POINT_UNIFORM = 0.5; 
	// Low significance 
	
	/**
	 * Default value for the <code>pUniform</code> attribute
	 */
	public final static double D_PUNIFORM = 0.5; 
	// Negligible effect
	
	/**
	 * Default value for the <code>unifMethod</code> attribute
	 */
	public final static int D_UNIF_METHOD = 0; 
	// 0 is better that 1 or 2
	
	/**
	 * Default value for the <code>unifDistParam</code> attribute
	 */
	public final static double D_UNIF_DIST_PARAM = 0.2; 
	// Low significance 
	
	/**
	 * Default value for the <code>mutationProb</code> attribute
	 */
	public final static double D_MUTATION_PROB = 0.05; 
	// Important - near sweet-spot 0.075(10), 0.05(30); lower is better for more dimensions
	
	/**
	 * Default value for the <code>randomMutation</code> attribute
	 */
	public final static double D_RANDOM_MUTATION = 1.0;
	
	/**
	 * Default value for the <code>adjacentMutation</code> attribute
	 */
	public final static double D_ADJACENT_MUTATION = 1.0;
	
	/**
	 * Default value for the <code>boundaryMutation</code> attribute
	 */
	public final static double D_BOUNDARY_MUTATION = 1.0;
	
	/**
	 * Default value for the <code>gaussianMutation</code> attribute
	 */
	public final static double D_GAUSSIAN_MUTATION = 0.05; 
	// Important - Smaller is better
	
	// --------------------------------------------------------------------------------------------
	// Attributes
	// --------------------------------------------------------------------------------------------
	
	/**
	 * The list of the discrete decision variables of the problem
	 */
	private ArrayList<DiscVar> discVars;
	
	/**
	 * The list of the continuous decision variables of the problem
	 */
	private ArrayList<ContVar> contVars;
	
	/**
	 * A value between -1.0 and 1.0 that represents the expected mean "quality" of the solutions
	 * to be selected from the current population. 1.0 if the selected solutions should only be 
	 * among the best according to the objectives. -1.0 if they should only be among the worst. 
	 * 0.0 if there is no preference and the solutions have uniform chances to be selected.
	 */
	private double greed;
	
	/**
	 * The percentage of solutions to be sampled in the Tournament selection strategy
	 */
	private double kPerc;
	
	/**
	 * The percentage of solutions that are to be truncated from selection. The 
	 * <i>(1 - <code>trunc</code>)*n</i> best solutions are selected.
	 */
	private double trunc;
	
	/**
	 * The number of splitting points in new solutions for the assignment of data from either 
	 * parent 1, parent 2 or uniform crossover. A value of <i>0</i> means that all the values in 
	 * the new solution are computed using the same method.
	 */
	private int points;
	
	/**
	 * The probability of using uniform crossover on a segment of a new solution defined by 
	 * crossover points. <i>0</i> if only point crossover should be used. <i>1</i> if only 
	 * uniform crossover should be used.
	 */
	private double pointUniform;
	
	/**
	 * The probability of selecting values from parent 1 in uniform crossover. The complement is
	 * the probability of selecting values from parent 2.
	 */
	private double pUniform;
	
	/**
	 * The index of the uniform crossover method for continuous variables as defined by the class 
	 * constants: <ul>
	 * <li> <code>UNIF_EITHER_OR</code>: Either-or
	 * <li> <code>UNIF_UDIST</code>: Uniform distribution
	 * <li> <code>UNIF_NORMALDIST</code>: Normal distribution </ul>
	 */
	private int unifMethod;
	
	/**
	 * The parameter for probability distribution uniform crossover methods for continuous 
	 * variables: the probability of the generated value to fall outside the range between the 
	 * values of the parents if the uniform distribution method is used; the standard deviation
	 * of the normal distributions as a percentage of the range between the values of the parents 
	 * if the normal distribution method is used
	 */
	private double unifDistParam;
	
	/**
	 * The probability of mutating each value in new solutions after crossover
	 */
	private double mutationProb;
	
	/**
	 * The weight of the random mutation operator for scalar discrete variables. The probability of
	 * using the random mutation operator is its weight over the sum of the weights of all the 
	 * operators. 
	 */
	private double randomMutation;
	
	/**
	 * The weight of the adjacent mutation operator for scalar discrete variables. The probability 
	 * of using the adjacent mutation operator is its weight over the sum of the weights of all the 
	 * operators.
	 */
	private double adjacentMutation;
	
	/**
	 * The weight of the boundary mutation operator for scalar discrete variables. The probability 
	 * of using the boundary mutation operator is its weight over the sum of the weights of all the 
	 * operators.
	 */
	private double boundaryMutation;
	
	/**
	 * The percentage of a continuous variable range to use as the standard deviation of the normal
	 * distribution mutation method for continuous variables. <code>Double.NaN</code> if the
	 * uniform mutation method should be used instead.
	 */
	private double gaussianMutation;
	
	// --------------------------------------------------------------------------------------------
	// Constructors
	// --------------------------------------------------------------------------------------------
	
	/**
	 * Creates a new instance of the Genetic algorithm generator with the default parameters
	 */
	public GA()
	{
		discVars			= new ArrayList<DiscVar>();
		contVars			= new ArrayList<ContVar>();
		greed				= D_GREED;
		kPerc				= D_K_PERC;
		trunc				= D_TRUNC;
		points				= D_POINTS;
		pointUniform		= D_POINT_UNIFORM;
		pUniform			= D_PUNIFORM;
		unifMethod			= D_UNIF_METHOD;
		unifDistParam		= D_UNIF_DIST_PARAM;
		mutationProb		= D_MUTATION_PROB;
		randomMutation		= D_RANDOM_MUTATION;
		adjacentMutation	= D_ADJACENT_MUTATION;
		boundaryMutation	= D_BOUNDARY_MUTATION;
		gaussianMutation	= D_GAUSSIAN_MUTATION;
	}
	
	/**
	 * Creates a new instance of the Genetic algorithm generator
	 * @param greed {@link #greed}
	 * @param kPerc The percentage of solutions to be sampled in the Tournament selection strategy
	 * @param trunc The percentage of solutions that are to be truncated from selection. The 
	 * <i>(1 - <code>trunc</code>)*n</i> best solutions are selected.
	 * @param points The number of splitting points in new solutions for the assignment of data 
	 * from either parent 1, parent 2 or uniform crossover. A value of <i>0</i> means that all the 
	 * values in the new solution are computed using the same method.
	 * @param pointUniform The probability of using uniform crossover on a segment of a new 
	 * solution defined by crossover points. <i>0</i> if only point crossover should be used. 
	 * <i>1</i> if only uniform crossover should be used.
	 * @param pUniform The probability of selecting values from parent 1 in uniform crossover. The 
	 * complement is the probability of selecting values from parent 2.
	 * @param unifMethod The index of the uniform crossover method for continuous variables as 
	 * defined by the class constants: <ul>
	 * <li> <code>UNIF_EITHER_OR</code>: Either-or
	 * <li> <code>UNIF_UDIST</code>: Uniform distribution
	 * <li> <code>UNIF_NORMALDIST</code>: Normal distribution </ul>
	 * @param unifDistParam The parameter for probability distribution uniform crossover methods 
	 * for continuous variables: the probability of the generated value to fall outside the range 
	 * between the values of the parents if the uniform distribution method is used; the standard 
	 * deviation of the normal distributions as a percentage of the range between the values of the 
	 * parents if the normal distribution method is used
	 * @param mutationProb The probability of mutating each value in new solutions after crossover
	 * @param randomMutation The weight of the random mutation operator for scalar discrete 
	 * variables. The probability of using the random mutation operator is its weight over the sum 
	 * of the weights of all the operators. 
	 * @param adjacentMutation The weight of the adjacent mutation operator for scalar discrete 
	 * variables. The probability of using the adjacent mutation operator is its weight over the 
	 * sum of the weights of all the operators.
	 * @param boundaryMutation The weight of the boundary mutation operator for scalar discrete 
	 * variables. The probability of using the boundary mutation operator is its weight over the 
	 * sum of the weights of all the operators.
	 * @param gaussianMutation The percentage of a continuous variable range to use as the standard 
	 * deviation of the normal distribution mutation method for continuous variables. 
	 * <code>Double.NaN</code> if the uniform mutation method should be used instead.
	 */
	public GA(double greed, double kPerc, double trunc, int points, 
			double pointUniform, double pUniform, int unifMethod, double unifDistParam, 
			double mutationProb, double randomMutation,	double adjacentMutation, 
			double boundaryMutation, double gaussianMutation)
	{
		this.greed = greed;
		this.kPerc = kPerc < 0 ? 0.0 : kPerc;
		this.kPerc = kPerc > 1 ? 1.0 : this.kPerc;
		this.trunc = trunc < 0 ? 0.0 : trunc;
		this.trunc = trunc > 1 ? 1.0 : this.trunc;
		this.points = points < 0 ? 0 : points;
		this.pointUniform = pointUniform < 0 ? 0.0 : pointUniform;
		this.pointUniform = pointUniform > 1 ? 1.0 : this.pointUniform;
		this.pUniform = pUniform < 0 ? 0.0 : pUniform;
		this.pUniform = pUniform > 1 ? 1.0 : this.pUniform;
		this.unifMethod = unifMethod < UNIF_EITHER_OR || unifMethod > UNIF_NORMALDIST ?
				D_UNIF_METHOD : unifMethod;
		this.unifDistParam = unifDistParam;
		this.mutationProb = mutationProb < 0 ? 0.0 : mutationProb;
		this.mutationProb = mutationProb > 1 ? 1.0 : this.mutationProb;
		this.randomMutation = randomMutation;
		this.adjacentMutation = adjacentMutation;
		this.boundaryMutation = boundaryMutation;
		this.gaussianMutation = gaussianMutation;
	}
	
	// --------------------------------------------------------------------------------------------
	// Methods
	// --------------------------------------------------------------------------------------------

	@Override
	public String getId() 
	{
		return ID;
	}

	@Override
	public String getShortId() 
	{
		return SHORT_ID;
	}

	/**
	 * @return {@link #greed}
	 */
	public double getGreed() 
	{
		return greed;
	}

	/**
	 * @param greed {@link #greed}
	 */
	public void setGreed(double greed) 
	{
		this.greed = greed < -1.0 ? -1.0 : greed;
		this.greed = greed >  1.0 ?  1.0 : this.greed;
	}

	/**
	 * @return The percentage of solutions to be sampled in the Tournament selection strategy
	 */
	public double getkPerc() 
	{
		return kPerc;
	}

	/**
	 * @param kPerc The percentage of solutions to be sampled in the Tournament selection strategy
	 */
	public void setkPerc(double kPerc) 
	{
		this.kPerc = kPerc < 0 ? 0.0 : kPerc;
		this.kPerc = kPerc > 1 ? 1.0 : this.kPerc;
	}

	/**
	 * @return The percentage of solutions that are to be truncated from selection. The 
	 * <i>(1 - <code>trunc</code>)*n</i> best solutions are selected.
	 */
	public double getTrunc() 
	{
		return trunc;
	}

	/**
	 * @param trunc The percentage of solutions that are to be truncated from selection. The 
	 * <i>(1 - <code>trunc</code>)*n</i> best solutions are selected.
	 */
	public void setTrunc(double trunc) 
	{
		this.trunc = trunc < 0 ? 0.0 : trunc;
		this.trunc = trunc > 1 ? 1.0 : this.trunc;
	}

	/**
	 * @return The number of splitting points in new solutions for the assignment of data from 
	 * either parent 1, parent 2 or uniform crossover. A value of <i>0</i> means that all the 
	 * values in the new solution are computed using the same method.
	 */
	public int getPoints() 
	{
		return points;
	}

	/**
	 * @param points The number of splitting points in new solutions for the assignment of data 
	 * from either parent 1, parent 2 or uniform crossover. A value of <i>0</i> means that all the 
	 * values in the new solution are computed using the same method.
	 */
	public void setPoints(int points)
	{
		this.points = points < 0 ? 0 : points;
	}

	/**
	 * @return The probability of using uniform crossover on a segment of a new solution defined by 
	 * crossover points. <i>0</i> if only point crossover should be used. <i>1</i> if only 
	 * uniform crossover should be used.
	 */
	public double getPointUniform() 
	{
		return pointUniform;
	}

	/**
	 * @param pointUniform The probability of using uniform crossover on a segment of a new 
	 * solution defined by crossover points. <i>0</i> if only point crossover should be used. 
	 * <i>1</i> if only uniform crossover should be used.
	 */
	public void setPointUniform(double pointUniform) 
	{
		this.pointUniform = pointUniform < 0 ? 0.0 : pointUniform;
		this.pointUniform = pointUniform > 1 ? 1.0 : this.pointUniform;
	}

	/**
	 * @return The probability of selecting values from parent 1 in uniform crossover. The 
	 * complement is the probability of selecting values from parent 2.
	 */
	public double getpUniform() 
	{
		return pUniform;
	}

	/**
	 * @param pUniform The probability of selecting values from parent 1 in uniform crossover. The 
	 * complement is the probability of selecting values from parent 2.
	 */
	public void setpUniform(double pUniform) 
	{
		this.pUniform = pUniform < 0 ? 0.0 : pUniform;
		this.pUniform = pUniform > 1 ? 1.0 : this.pUniform;
	}

	/**
	 * @return The index of the uniform crossover method for continuous variables as defined by the 
	 * class constants: <ul>
	 * <li> <code>UNIF_EITHER_OR</code>: Either-or
	 * <li> <code>UNIF_UDIST</code>: Uniform distribution
	 * <li> <code>UNIF_NORMALDIST</code>: Normal distribution </ul>
	 */
	public int getUnifMethod() 
	{
		return unifMethod;
	}

	/**
	 * @param unifMethod The index of the uniform crossover method for continuous variables as 
	 * defined by the class constants: <ul>
	 * <li> <code>UNIF_EITHER_OR</code>: Either-or
	 * <li> <code>UNIF_UDIST</code>: Uniform distribution
	 * <li> <code>UNIF_NORMALDIST</code>: Normal distribution </ul>
	 */
	public void setUnifMethod(int unifMethod) 
	{
		this.unifMethod = unifMethod < UNIF_EITHER_OR || unifMethod > UNIF_NORMALDIST ?
							D_UNIF_METHOD : unifMethod;
	}

	/**
	 * @return The parameter for probability distribution uniform crossover methods for continuous 
	 * variables: the probability of the generated value to fall outside the range between the 
	 * values of the parents if the uniform distribution method is used; the standard deviation
	 * of the normal distributions as a percentage of the range between the values of the parents 
	 * if the normal distribution method is used
	 */
	public double getUnifDistParam() 
	{
		return unifDistParam;
	}

	/**
	 * @param unifDistParam The parameter for probability distribution uniform crossover methods 
	 * for continuous variables: the probability of the generated value to fall outside the range 
	 * between the values of the parents if the uniform distribution method is used; the standard 
	 * deviation of the normal distributions as a percentage of the range between the values of the 
	 * parents if the normal distribution method is used
	 */
	public void setUnifDistParam(double unifDistParam) 
	{
		this.unifDistParam = unifDistParam;
	}

	/**
	 * @return The probability of mutating each value in new solutions after crossover
	 */
	public double getMutationProb() 
	{
		return mutationProb;
	}

	/**
	 * @param mutationProb The probability of mutating each value in new solutions after crossover
	 */
	public void setMutationProb(double mutationProb) 
	{
		this.mutationProb = mutationProb < 0 ? 0.0 : mutationProb;
		this.mutationProb = mutationProb > 1 ? 1.0 : this.mutationProb;
	}

	/**
	 * @return The weight of the random mutation operator for scalar discrete variables. The 
	 * probability of using the random mutation operator is its weight over the sum of the weights 
	 * of all the operators. 
	 */
	public double getRandomMutation() 
	{
		return randomMutation;
	}

	/**
	 * @param randomMutation The weight of the random mutation operator for scalar discrete 
	 * variables. The probability of using the random mutation operator is its weight over the sum 
	 * of the weights of all the operators. 
	 */
	public void setRandomMutation(double randomMutation) 
	{
		this.randomMutation = randomMutation;
	}

	/**
	 * @return The weight of the adjacent mutation operator for scalar discrete variables. The 
	 * probability of using the adjacent mutation operator is its weight over the sum of the 
	 * weights of all the operators.
	 */
	public double getAdjacentMutation() 
	{
		return adjacentMutation;
	}

	/**
	 * @param adjacentMutation The weight of the adjacent mutation operator for scalar discrete 
	 * variables. The probability of using the adjacent mutation operator is its weight over the 
	 * sum of the weights of all the operators.
	 */
	public void setAdjacentMutation(double adjacentMutation) 
	{
		this.adjacentMutation = adjacentMutation;
	}

	/**
	 * @return The weight of the boundary mutation operator for scalar discrete variables. The 
	 * probability of using the boundary mutation operator is its weight over the sum of the 
	 * weights of all the operators.
	 */
	public double getBoundaryMutation() 
	{
		return boundaryMutation;
	}

	/**
	 * @param boundaryMutation The weight of the boundary mutation operator for scalar discrete 
	 * variables. The probability of using the boundary mutation operator is its weight over the 
	 * sum of the weights of all the operators.
	 */
	public void setBoundaryMutation(double boundaryMutation) 
	{
		this.boundaryMutation = boundaryMutation;
	}

	/**
	 * @return The percentage of a continuous variable range to use as the standard deviation of 
	 * the normal distribution mutation method for continuous variables. <code>Double.NaN</code> if 
	 * the uniform mutation method should be used instead.
	 */
	public double getGaussianMutation() 
	{
		return gaussianMutation;
	}

	/**
	 * @param gaussianMutation The percentage of a continuous variable range to use as the standard 
	 * deviation of the normal distribution mutation method for continuous variables. 
	 * <code>Double.NaN</code> if the uniform mutation method should be used instead.
	 */
	public void setGaussianMutation(double gaussianMutation) 
	{
		this.gaussianMutation = gaussianMutation;
	}

	@Override
	public String getParamSummary()
	{
		String unMethod = unifMethod == UNIF_EITHER_OR ? UNIF_EITHER_OR_PRINT :
			(unifMethod == UNIF_UDIST ? UNIF_UDIST_PRINT :
			(unifMethod == UNIF_NORMALDIST ? UNIF_NORMALDIST_PRINT : "N/A"));
		
		String line = "";
		line +=		   PARAM_GREED				+ greed;
		line += "; " + PARAM_K_PERC				+ kPerc;
		line += "; " + PARAM_TRUNC				+ trunc;
		line += "; " + PARAM_POINTS				+ points;
		line += "; " + PARAM_POINT_UNIFORM		+ pointUniform;
		line += "; " + PARAM_P_UNIFORM			+ pUniform;
		line += "; " + PARAM_UNIF_METHOD		+ unMethod;
		line += "; " + PARAM_UNIF_DIST_PARAM	+ unifDistParam;
		line += "; " + PARAM_MUTATION_PROB		+ mutationProb;
		line += "; " + PARAM_RANDOM_MUTATION	+ randomMutation;
		line += "; " + PARAM_ADJACENT_MUTATION	+ adjacentMutation;
		line += "; " + PARAM_BOUNDARY_MUTATION	+ boundaryMutation;
		line += "; " + PARAM_GAUSSIAN_MUTATION	+ gaussianMutation;
		return line;
	}

	@Override
	public void addDiscVariable(DiscVar variable) 
	{
		discVars.add(variable);
	}

	@Override
	public void addContVariable(ContVar variable) 
	{
		contVars.add(variable);
	}

	@Override
	public void clearVariables() 
	{
		discVars = new ArrayList<DiscVar>();
		contVars = new ArrayList<ContVar>();
	}

	@Override
	public void setObjectives(ArrayList<Objective> objectives) 
	{
		// Do nothing
	}

	@Override
	public synchronized ArrayList<SolutionRoot> generateSolutions(
														Population population, int number)
	{
		if (population.size() == 0 || discVars.size() + contVars.size() == 0)
			return null;
		
		// Select parents
		ArrayList<SolutionWrapper> parentPool = population.select(2*number, greed);
		
		// Generate offspring
		ArrayList<SolutionRoot> roots = new ArrayList<SolutionRoot>();
		for(int i = 0 ; i < number ; i++)
		{
			SolutionWrapper parent1 = parentPool.get(2*i);
			SolutionWrapper parent2 = parentPool.get(2*i + 1);
			ArrayList<Integer> discValues = childDiscVals(parent1, parent2);
			ArrayList<Double> contValues = childContVals(parent1, parent2);
			roots.add(new SolutionRoot(discValues, contValues));
		}
		return roots;
	}
	
	/**
	 * Generates the discrete values of a new child solution
	 * @param parent1 The first parent of the child solution
	 * @param parent2 The second parent of the child solution
	 * @return The discrete values of a new child solution
	 */
	private ArrayList<Integer> childDiscVals(SolutionWrapper parent1, SolutionWrapper parent2)
	{
		// Generate discrete values
		ArrayList<Integer> discValues = null;
		int discCount = discVars.size();
		if(discCount > 0)
		{
			// Crossover
			discValues = new ArrayList<Integer>();
			ArrayList<Double> partPerc = new ArrayList<Double>();
			for(int partition = 0 ; partition < points ; partition++)
				partPerc.add(Math.random());
			Collections.sort(partPerc);
			boolean ok = false;
			int partition = 0;
			int var = 0;
			while(!ok)
			{
				int next = partition == points ? discCount : 
												(int)(partPerc.get(partition)*(double)discCount);
				for(int i = var ; i < next ; i++)
				{
					boolean usePoint = Math.random() > pointUniform;
					if(usePoint)
					{
						int value = partition % 2 == 0 ? parent1.getDiscValues().get(i)
													   : parent2.getDiscValues().get(i);
						discValues.add(value);
					}
					else  // Use uniform crossover
					{
						int value = Math.random() < pUniform ? parent1.getDiscValues().get(i)
								  							 : parent2.getDiscValues().get(i);
						discValues.add(value);
					}
					var++;
				}
				partition++;
				if(partition == points + 1)
					ok = true;
			}
			
			// Mutation
			for(var = 0 ; var < discCount ; var++)
			{
				if(Math.random() < mutationProb)
				{
					DiscVar variable = discVars.get(var);
					double sum = randomMutation + adjacentMutation + boundaryMutation;
					double random = variable.isScalar() ? Math.random()*sum : 0.0;
					if(random <= randomMutation)
						discValues.set(var, variable.getMin() + 
											Utilities.uniformRandomSelect(variable.getCount()));
					else if(random < (randomMutation + adjacentMutation))
					{
						int modif = Math.random() < 0.5 ? -1 : 1;
						boolean change = modif == 1 ?
							(discValues.get(var) < variable.getMin() + variable.getCount() - 1 ?
									true : false) : (discValues.get(var) > variable.getMin() ? 
									true : false);
						if(change)
							discValues.set(var, discValues.get(var) + modif);
					}
					else
					{
						int value = Math.random() < 0.5 ? variable.getMin() : 
													variable.getMin() + variable.getCount() - 1;
						discValues.set(var, value);
					}
				}
			}
		}
		return discValues;
	}
	
	/**
	 * Generates the continuous values of a new child solution
	 * @param parent1 The first parent of the child solution
	 * @param parent2 The second parent of the child solution
	 * @return The continuous values of a new child solution
	 */
	private ArrayList<Double> childContVals(SolutionWrapper parent1, SolutionWrapper parent2)
	{
		// Generate discrete values
		ArrayList<Double> contValues = null;
		int contCount = contVars.size();
		if(contCount > 0)
		{		
			// Crossover
			contValues = new ArrayList<Double>();
			ArrayList<Double> partPerc = new ArrayList<Double>();
			for(int partition = 0 ; partition < points ; partition++)
				partPerc.add(Math.random());
			Collections.sort(partPerc);
			boolean ok = false;
			int partition = 0;
			int var = 0;
			while(!ok)
			{
				int next = partition == points ? contCount 
														: (int)(partPerc.get(partition)*contCount);
				for(int i = var ; i < next ; i++)
				{
					boolean usePoint = Math.random() > pointUniform;
					if(usePoint)
					{
						double value = partition % 2 == 0 ? parent1.getContValues().get(i)
														  : parent2.getContValues().get(i);
						contValues.add(value);
					}
					else // Use uniform crossover
					{
						ContVar variable = contVars.get(i);
						double value = Double.NaN;
						double v1 = parent1.getContValues().get(i);
						double v2 = parent2.getContValues().get(i);
						double min = Double.NaN;
						double max = Double.NaN;
						switch(unifMethod)
						{
							case UNIF_UDIST:
								double extra = unifDistParam*Math.abs(v2 - v1)/2;
								min = Math.max(variable.getMin(), Math.min(v1, v2) - extra);
								max = Math.min(variable.getMax(), Math.max(v1, v2) + extra);
								value = Uniform.sample(min, max);
							case UNIF_NORMALDIST:
								double mean = Math.random() < pUniform ? v1 : v2;
								double stDev = unifDistParam*Math.abs(v2 - v1);
								value = Normal.sample(mean, stDev);
								min = variable.getMin();
								max = variable.getMax();
								value = value < min ? min : value;
								value = value > max ? max : value;								
								break;
							default:	value = Math.random() < pUniform ? v1 : v2;
										break;
						}
						contValues.add(value);
					}
					var++;
				}
				partition++;
				if(partition == points + 1)
					ok = true;
			}
			
			// Mutation
			for(var = 0 ; var < contCount ; var++)
			{
				if(Math.random() < mutationProb)
				{
					ContVar variable = contVars.get(var);
					double min = variable.getMin();
					double max = variable.getMax();
					if(Double.isNaN(gaussianMutation))
					{
						double value = Uniform.sample(min, max);
						contValues.set(var, value);
					}
					else
					{
						double range = variable.getMax() - variable.getMin();
						double stDev = gaussianMutation*range;
						double value = Normal.sample(contValues.get(var), stDev);
						value = value < min ? min : value;
						value = value > max ? max : value;
						contValues.set(var, value);
					}
				}
			}
		}
		return contValues;
	}

}
