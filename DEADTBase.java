package deadtspv;

//  This program is free software: you can redistribute it and/or modify
//  it under the terms of the GNU Lesser General Public License as published by
//  the Free Software Foundation, either version 3 of the License, or
//  (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//  GNU Lesser General Public License for more details.
// 
//  You should have received a copy of the GNU Lesser General Public License
//  along with this program.  If not, see <http://www.gnu.org/licenses/>.

import de.DEModifiedCrossover;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.uma.jmetal.algorithm.singleobjective.differentialevolution.DifferentialEvolution;
import org.uma.jmetal.operator.impl.crossover.DifferentialEvolutionCrossover;
import org.uma.jmetal.operator.impl.selection.DifferentialEvolutionSelection;
import org.uma.jmetal.problem.DoubleProblem;
import org.uma.jmetal.solution.DoubleSolution;
import org.uma.jmetal.util.comparator.ObjectiveComparator;
import org.uma.jmetal.util.evaluator.SolutionListEvaluator;
import util.Parameters;

/**
 *
 * @author rafael
 */
public class DEADTBase extends DifferentialEvolution {

  private int trials;
  private int total;
  Comparator<DoubleSolution> comparador;
  private ConfigurationAP problema;
  
  public DEADTBase(DoubleProblem problem, int maxEvaluations, int populationSize, DifferentialEvolutionCrossover crossoverOperator, DifferentialEvolutionSelection selectionOperator, SolutionListEvaluator<DoubleSolution> evaluator) {
    super(problem, maxEvaluations, populationSize, crossoverOperator, selectionOperator, evaluator);
    comparador = new ObjectiveComparator(0);
    ((DEModifiedCrossover) crossoverOperator).setF(Parameters.F);
  }

  @Override
  protected List<DoubleSolution> replacement(List<DoubleSolution> population,
    List<DoubleSolution> offspringPopulation) {
    List<DoubleSolution> pop = new ArrayList<>();
    int tam = offspringPopulation.size();
    for (int ind = 0; ind < tam; ind++) {
      if (population.get(ind).getObjective(0) < offspringPopulation.get(ind).getObjective(0)) {
        pop.add(population.get(ind));
      } else {
        pop.add(offspringPopulation.get(ind));
        trials++;
      }
    }
    total += tam;
    return pop;
  }

  public int getTrials() {
    return trials;
  }

  public ConfigurationAP getProblema() {
    return problema;
  }

  public void setProblema(ConfigurationAP problema) {
    this.problema = problema;
  }

  public int getTotal() {
    return total;
  }

}
