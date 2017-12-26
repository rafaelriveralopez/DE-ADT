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


import org.uma.jmetal.algorithm.singleobjective.differentialevolution.DifferentialEvolutionBuilder;
import org.uma.jmetal.problem.DoubleProblem;

public class DEADTBuilder extends DifferentialEvolutionBuilder {

  private final ConfigurationAP problema;

  public DEADTBuilder(DoubleProblem problem, ConfigurationAP problema) {
    super(problem);
    this.problema = problema;
  }

  @Override
  public DEADTBase build() {
    DEADTBase miDE = new DEADTBase(getProblem(), getMaxEvaluations(), 
      getPopulationSize(), getCrossoverOperator(),
      getSelectionOperator(), getSolutionListEvaluator());
    miDE.setProblema(problema);
    return miDE;
  }
}
