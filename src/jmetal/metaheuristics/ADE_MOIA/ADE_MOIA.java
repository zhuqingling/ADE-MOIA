package jmetal.metaheuristics.ADE_MOIA;

import java.awt.List;
import java.util.ArrayList;
import java.util.Random;

import jmetal.core.*;
import jmetal.qualityIndicator.QualityIndicator;
import jmetal.util.Distance;
import jmetal.util.JMException;
import jmetal.util.PseudoRandom;
import jmetal.util.Ranking;
import jmetal.util.comparators.CrowdingComparator;

import jmetal.util.StdRandom;
import jmetal.util.comparators.DominanceComparator;
//import jmetal.util.comparators.*;
/**
 * Implementation of NSGA-II. This implementation of NSGA-II makes use of a
 * QualityIndicator object to obtained the convergence speed of the algorithm.
 * This version is used in the paper: A.J. Nebro, J.J. Durillo, C.A. Coello
 * Coello, F. Luna, E. Alba
 * "A Study of Convergence Speed in Multi-Objective Metaheuristics." To be
 * presented in: PPSN'08. Dortmund. September 2008.
 */

public class ADE_MOIA extends Algorithm {
	/**
	 * Constructor
	 * 
	 * @param problem
	 *            Problem to solve
	 */
	public ADE_MOIA(Problem problem) {
		super(problem);
	} // NSGAII
	/**
	 * Runs the NSGA-II algorithm.
	 * 
	 * @return a <code>SolutionSet</code> that is a set of non dominated
	 *         solutions as a result of the algorithm execution
	 * @throws JMException
	 */
	@SuppressWarnings("null")
	public SolutionSet execute() throws JMException, ClassNotFoundException {
		int populationSize;
		int maxEvaluations;
		int evaluations;
		int clonesize = 20;
		double Fm = 0.5;
		double wF = 0.8;
		double Crm= 0.5;
		double wCr = 0.9;
		
		QualityIndicator indicators; // QualityIndicator object
		int requiredEvaluations; // Use in the example of use of the
		// indicators object (see below)

		SolutionSet population;
		SolutionSet offspringPopulation;
		SolutionSet union;
		SolutionSet clonepopulation = new SolutionSet(clonesize);
		SolutionSet Archive = null;
		SolutionSet front = null;
		SolutionSet lastfront = null;

		Operator mutationOperator;
		Operator crossoverOperator;
		Operator cloneoperator;
		Operator DEcrossoverOperator;
		Operator DEselectionOperator;

		DominanceComparator dominaceCompare = new DominanceComparator();
		Distance distance = new Distance();

		// Read the parameters
		populationSize = ((Integer) getInputParameter("populationSize"))
				.intValue();
		maxEvaluations = ((Integer) getInputParameter("maxEvaluations"))
				.intValue();
		indicators = (QualityIndicator) getInputParameter("indicators");

		// Initialize the variables
		population = new SolutionSet(populationSize);
		evaluations = 0;

		requiredEvaluations = 0;

		// Read the operators
		cloneoperator = operators_.get("clone");
		mutationOperator = operators_.get("mutation");
		crossoverOperator = operators_.get("crossover");
		DEcrossoverOperator = operators_.get("DEcrossover");
		//selection operator is null
		DEselectionOperator = operators_.get("DEselection");

		//1. INIT POPULATION
		//Create the initial solutionSet
		Solution newSolution;
		for (int i = 0; i < populationSize; i++) {
			newSolution = new Solution(problem_);
			problem_.evaluate(newSolution);
			problem_.evaluateConstraints(newSolution);
			evaluations++;
			population.add(newSolution);
		} // for
		
		//2.GET THE FIRST FRONT
		//fast non dominated sort
		Ranking ranking = new Ranking(population);
		//get the first front
		front = ranking.getSubfront(0);
		//lastfront = ranking.getSubfront(1);
//		if(lastfront.size()>0)
//			lastfront.clear();
		if (ranking.getNumberOfSubfronts()>1)
		{
			lastfront = ranking.getSubfront(1);
			for (int i =2; i < ranking.getNumberOfSubfronts();++i)
			{
				lastfront = lastfront.union(ranking.getSubfront(i));
				//lastfront=lastfront.union(ranking.getSubfront(i));
			}
		}
		else
		{
			lastfront = ranking.getSubfront(0);
		}
		//sort the front according to the crowding distance
		distance.crowdingDistanceAssignment(front,
				problem_.getNumberOfObjectives());
		front.sort(new CrowdingComparator());
		population.clear();
		
		//get the clone population from the first front
		for (int k = 0; k < front.size() && k < clonesize; k++) {
			clonepopulation.add(front.get(k));
		} // for
		Archive = front;
		// Generations
		ArrayList<Double> Fsuccess=new ArrayList();
		Double FsuccessSum = 0.0;
		int it = 0;
		while (evaluations < maxEvaluations) {
			//1.CLONE POPULATION
			population = (SolutionSet) cloneoperator.execute(clonepopulation);
			// Create the offSpring solutionSet
			offspringPopulation = new SolutionSet(populationSize);
			//Solution[] parents = new Solution[2];
			Solution parents[];
			//set CR value
			DEcrossoverOperator.setParameter("CR", 0.55+(1.0/Math.PI)*Math.atan((1.0-(double)evaluations/(double)maxEvaluations-0.8)/0.1));
			//System.out.println(DEcrossoverOperator.getParameter("CR"));
			it=it+1;
			for (int i = 0; i < population.size(); i++) {
				if (evaluations < maxEvaluations) {
					// obtain parents
					parents = (Solution [])DEselectionOperator.execute(new Object[]{population, i});
					//parent[0],parent[1] select from the first paretof and parent[1]select from the last paretof
			        parents[1]=lastfront.get(PseudoRandom.randInt(0, lastfront.size()-1));
					Solution offSpring;
			        // Crossover. Two parameters are required: the current individual and the 
			        //            array of parents
			        double Fmtemp;
			        do
			        {
			        	Fmtemp = StdRandom.cauchy(Fm, 0.1);
			        }while(Fmtemp<=0.1 || Fmtemp>=0.9);
			        DEcrossoverOperator.setParameter("F", Fmtemp);
			        offSpring = (Solution)DEcrossoverOperator.execute(new Object[]{population.get(i), parents}) ;
					mutationOperator.execute(offSpring);
					problem_.evaluate(offSpring);
					problem_.evaluateConstraints(offSpring);
					offspringPopulation.add(offSpring);
					// offspringPopulation.add(offSpring[1]);
					int dominance;
					dominance = dominaceCompare.compare(offSpring,population.get(i));
					if(dominance == -1)
					{
						//记录该F值
						Fsuccess.add((Double) DEcrossoverOperator.getParameter("F"));
						//Crsuccess.add((Double) DEcrossoverOperator.getParameter("CR"));
					}
					evaluations += 1;
				} // if
			} // for
			
			if(Fsuccess.size()>0.1*populationSize)
			{
				//计算F下一次迭代的中心
				for(int i=0;i<Fsuccess.size();++i)
				{
					FsuccessSum += Fsuccess.get(i);
				}
				Fm =FsuccessSum/(double)Fsuccess.size();
				//System.out.println("FF:"+Fm);
				Fsuccess.clear();FsuccessSum=0.0;
			}
			
			// Create the solutionSet union of solutionSet and offSpring
			union = ((SolutionSet) Archive).union(offspringPopulation);
			union.Suppress();
			
			// Ranking the union
			ranking = new Ranking(union);

			Archive.clear();
			clonepopulation.clear();

			front = ranking.getSubfront(0);
			//GET DA 
			if (ranking.getNumberOfSubfronts()>1)
			{
				lastfront.clear();
				lastfront = ranking.getSubfront(1);
				for (int i =2; i < ranking.getNumberOfSubfronts();++i)
				{
					lastfront=lastfront.union(ranking.getSubfront(i));
				}
			}
			else
			{
				lastfront.clear();
				lastfront = ranking.getSubfront(0);
			}
			distance.crowdingDistanceAssignment(front,
					problem_.getNumberOfObjectives());
			
			front.Suppress();
			// Remain is less than front(index).size, insert only the best one
			front.sort(new CrowdingComparator());

			while (front.size() > populationSize) {
				front.remove(front.size() - 1);
				distance.crowdingDistanceAssignment(front,
						problem_.getNumberOfObjectives());
				front.sort(new CrowdingComparator());
			}
			Archive = front;// mutationOperator.setParameter("distributionIndex",18.0+4*front.size()/Archivesize);
			for (int k = 0; k < clonesize && k < front.size(); k++) {
				clonepopulation.add(front.get(k));
			}
		} // while

		return Archive;
	} // execute
} // NSGA-II