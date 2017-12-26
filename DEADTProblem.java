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

package deadtspv;

import java.util.ArrayList;
import java.util.List;
import org.uma.jmetal.problem.impl.AbstractDoubleProblem;
import org.uma.jmetal.solution.DoubleSolution;
import uci.Clasification;
import uci.AxisParallelNode;
import util.Parameters;

/**
 *
 * @author rafael
 */
public class DEADTProblem extends AbstractDoubleProblem {

  private ConfigurationAP problema;
  private int ind;

  public void init(ConfigurationAP problema, int fold, int popSize) {
    this.problema = problema;
    setNumberOfVariables(problema.getDimension());
    setNumberOfObjectives(1);
    setName(problema.getNombreDataset());

    List<Double> lowerLimit = new ArrayList<>(getNumberOfVariables());
    List<Double> upperLimit = new ArrayList<>(getNumberOfVariables());

    for (int i = 0; i < getNumberOfVariables(); i++) {
      lowerLimit.add(problema.getMinPosition());
      upperLimit.add(problema.getMaxPosition());
    }

    setLowerLimit(lowerLimit);
    setUpperLimit(upperLimit);
    
    //CONFIGURA FOLD
    
    problema.setFold(fold);
    problema.setNumNodo(1);
    Clasification instNodo = new Clasification(
      problema.getInstancias().length, problema.getClases().length);
    int[][] dataFolds = problema.getFoldsCV().getDataFolds();
    for (int fila = 0; fila < dataFolds.length; fila++) {
      if (dataFolds[fila][1] != fold) {
        instNodo.addInstancia(problema.getInstancias()[dataFolds[fila][0]]);
      }
    }
    problema.setInstanciasRaiz(instNodo);
  }

  @Override
  public void evaluate(DoubleSolution s) {
    double[] f = new double[getNumberOfObjectives()];
    double posicion[] = new double[this.getNumberOfVariables()];
    for (int i = 0; i < posicion.length; i++) {
      posicion[i] = s.getVariableValue(i);
    }
    AxisParallelNode inicio = problema.crearArbolAnchura(posicion, ind);
    switch (Parameters.SELECCION) {
      case Parameters.TRAINING:
        f[0] = problema.getErroresTraining(inicio);
        break;
      case Parameters.VALIDACION:
        f[0] = problema.getErroresValidacion(inicio);
        break;
      case Parameters.TEST:
        f[0] = problema.getErroresTest(inicio);
        break;
    }
    s.setObjective(0, f[0]);
  }

  public int getInd() {
    return ind;
  }

  public void setInd(int ind) {
    this.ind = ind;
  }
}
