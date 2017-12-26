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
import uci.*;
import java.util.*;
import util.Parameters;
import static util.Parameters.*;
import org.uma.jmetal.algorithm.Algorithm;
import org.uma.jmetal.operator.impl.selection.DifferentialEvolutionSelection;
import org.uma.jmetal.solution.DoubleSolution;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.util.AlgorithmRunner;
import org.uma.jmetal.util.ProblemUtils;
import org.uma.jmetal.util.evaluator.SolutionListEvaluator;
import static uci.UCIFile.*;

public class DEADT {

  private static boolean refinedDT;
  private final static String[] SPLIT = {"Inf", "Err"};

  public static void main(String[] args) {
    Parameters.getParameters(args);
    String dir = System.getProperty("user.dir");
    dir = dir + (dir.contains("dist") ? "/.." : "") + "/results";
    crearCarpetaResultados(dir);
    configuraOpcion(OPCION);
    Parameters.setDataset(DIR + FILE + ".arff");
    ConfigurationAP conf = new ConfigurationAP();
    int popSize = FACTOR_POBLACION * conf.getDimension(); //<==  
    boolean expCompleto = TIPO_EXP == EXP_COMPLETO;
    if (expCompleto) {
      Parameters.setCV(1);
    }
    popSize = Math.min(Math.max(popSize, POBLACION_MIN), POBLACION_MAX);
    Algorithm<DoubleSolution> algorithm;
    DEModifiedCrossover crossover = new DEModifiedCrossover(CR, F, TIPO_DE);
    crossover.setTerm(conf.getiTerm());
    crossover.setGen(GENERACIONES);
    crossover.setPopSize(popSize);
    DifferentialEvolutionSelection selection
      = new DifferentialEvolutionSelection();
    SolutionListEvaluator<DoubleSolution> evaluator = new deadtspv.DEADTEvaluator();;
    DEADTProblem problema
      = (DEADTProblem) ProblemUtils.<DoubleSolution>loadProblem("deadtspv.DEADTProblem");
    double mejorPromedioIntentos = 0;
    //Numero de intentos para mejorar precision
    if (ECHO) {
      System.out.printf("DE-ADT-SPV [%s], File: %s (%s)\n", TIPO_DE, FILE, 
        refinedDT ? "DT with refinement" : "ST without refinement");
    }
    if (expCompleto) {
      Parameters.setThresholdInstances(2);
      crossover.setGen(1000);
      //System.out.println(conf);
    }
    for (int intentos = 0; intentos < INTENTOS; intentos++) {
      double sumaAcu = 0;
      double sumaNod = 0;
      ArrayList<String> lineas = new ArrayList<>();
      //Ciclo de repeticiones
      if (LOG) {
        lineas.add("REP,ACC,NODES_F,TRIALS,NODES_B, PROF_F,REF,TIME,PROF_B");
      }
      for (int ciclos = 0; ciclos < REP; ciclos++) {
        conf.crearFolds(new Random());
        double sumaAciertos = 0;
        double sumaNodos = 0;
        double sumaProf = 0;
        double sumaRef = 0;
        double sumaTrials = 0;
        double sumaTotal = 0;
        double sumaTiempo = 0;
        double sumaNodosB = 0;
        //Ciclos para Folds;
        for (int fold = 0; fold < CV; fold++) {
          long tInicial = System.currentTimeMillis();
          problema.init(conf, fold, popSize);
          algorithm = new DEADTBuilder(problema, conf)
            .setCrossover(crossover).setSelection(selection)
            .setSolutionListEvaluator(evaluator)
            .setMaxEvaluations(GENERACIONES * popSize)
            .setPopulationSize(popSize)
            .build();
          new AlgorithmRunner.Executor(algorithm).execute();
          if (expCompleto) {
            System.out.println(conf);
            arbolConDatasetCompleto(conf, (DEADTBase) algorithm);
            break;
          } else {
            // Analisis con Validación cruzada
            int cual = 0;
            List<DoubleSolution> pop = ((DEADTBase) algorithm).getPopulation();
            double minErrores = Double.MAX_VALUE;
            for (int j = 0; j < pop.size(); j++) {
              double posicion[] = obtenerVector(pop.get(j));
              AxisParallelNode nodoRaiz = conf.crearArbolAnchura(posicion, j);
              conf.podarArbol(nodoRaiz);
              double errores = obtenerErrores(conf, nodoRaiz);
              if (refinedDT) {
                conf.completaArbol(nodoRaiz);
                conf.podarArbol(nodoRaiz);
                double erroresCon = obtenerErrores(conf, nodoRaiz);
                if (erroresCon < errores) {
                  errores = erroresCon;
                }
              }
              if (errores < minErrores) {
                minErrores = errores;
                cual = j;
              }
            }
            double posicion[] = obtenerVector(pop.get(cual));
            AxisParallelNode nodoRaiz = conf.crearArbolAnchura(posicion, cual);
            conf.podarArbol(nodoRaiz);
            double aciertos = conf.getAciertosTest(nodoRaiz);
            double nodos = conf.getHojas(nodoRaiz);
            double prof = conf.maxDepth(nodoRaiz);
            sumaNodosB += nodos;
            if (refinedDT) {
              AxisParallelNode nodoCompleto = nodoRaiz;
              int numRef = conf.completaArbol(nodoCompleto);
              conf.podarArbol(nodoCompleto);
              double aciertosCon = conf.getAciertosTest(nodoCompleto);
              if (aciertosCon > aciertos) {
                sumaRef += numRef;
                aciertos = aciertosCon;
                prof = conf.maxDepth(nodoCompleto);
                nodos = conf.getHojas(nodoCompleto);
              }
            }
            sumaTiempo += (System.currentTimeMillis() - tInicial);
            sumaProf += prof;
            sumaAciertos += aciertos;
            sumaNodos += nodos;
            sumaTrials += ((DEADTBase) algorithm).getTrials();
            sumaTotal += ((DEADTBase) algorithm).getTotal();
          } // Tipo VC
        } //Fin Fold
        if (!expCompleto) {
          double totInstancias = conf.getInstancias().length
            - (conf.getInstValidacion() != null ? conf.getInstValidacion().length : 0);
          double accuracy = sumaAciertos / totInstancias * 100;
          double nodos = sumaNodos / CV;
          double prof = sumaProf / CV;
          double ref = sumaRef / CV;
          double tiempo = sumaTiempo / CV;
          double nodosB = sumaNodosB / CV;
          double trials = sumaTrials / sumaTotal * 100;
          sumaAcu += accuracy;
          sumaNod += nodos;

          if (LOG) {
            String l = String.format("%d,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%d",
              ciclos, accuracy, nodos, trials, nodosB, prof,
              ref, tiempo, conf.getH());
            lineas.add(l);
          }
          if (ECHO) {
            String l = "%s REP=%d H=%2d TIME= %6.2f ACC=%6.2f NODES=%6.2f "
              + "NODES_REF=%6.2f PROF=%6.2f TRIALS=%6.2f REF=%6.2f\n";
            System.out.printf(l, FILE, ciclos, conf.getH(), tiempo,
              accuracy, nodos, nodosB, prof, trials, ref);
          }
        } else {
          break;
        }
      } //Fin de ciclo
      if (!expCompleto) {
        double promedio = sumaAcu / REP;
        double promNodos = sumaNod / REP;
        if (promedio > mejorPromedioIntentos) {
          mejorPromedioIntentos = promedio;
          if (ECHO) {
            System.out.printf("%s INT=%d AVG ACC = %.2f BEST AVG ACC = %.2f {%.2f}\n",
              FILE, intentos, promedio, mejorPromedioIntentos, promNodos);
          }
          if (LOG) {
            String comb = obtenerCombinacion();
            UCIFile.grabarArchivo(dir + "/" + (refinedDT ? "bl" : "sin") + "-"
              + TIPO_DE.replaceAll("/", "-") + "-" + SPLIT[TIPO_SPLITTING] + "-"
              + REP + "-" + CV + "-CV-" + FILE + "-" + comb + ".res", lineas);
          }
        } else if (ECHO) {
          System.out.printf("%s IT=%d AVG = %.2f\n", FILE, intentos, promedio);
        }
      } else {
        break;
      }
    } // intentos
  } // Main

  public static void arbolConDatasetCompleto(ConfigurationAP conf, DEADTBase algorithm) {
    int cual = 0;
    List<DoubleSolution> pop = algorithm.getPopulation();
    double minErrores = Double.MAX_VALUE;
    for (int j = 0; j < pop.size(); j++) {
      double posicion[] = obtenerVector(pop.get(j));
      AxisParallelNode nodoCompleto = conf.crearArbolAnchura(posicion, cual);
      conf.completaArbol(nodoCompleto);
      conf.podarArbol(nodoCompleto);
      double errores = conf.getErroresTraining(nodoCompleto);
      if (errores < minErrores) {
        minErrores = errores;
        cual = j;
      }
    }
    double posicion[] = obtenerVector(pop.get(cual));
    AxisParallelNode nodoCompleto = conf.crearArbolAnchura(posicion, cual);
    conf.completaArbol(nodoCompleto);
    conf.podarArbol(nodoCompleto);
    conf.imprimirArbol(nodoCompleto, 0);
    double hojas = conf.getHojas(nodoCompleto);
    double aciertos = conf.getAciertosTraining(nodoCompleto);
    double total = nodoCompleto.getInstancias().getContador();
    double acc = 100 * aciertos / total;
    System.out.printf("\n %s ACCURACY = %.2f, NODES = %.0f\n", conf.getNombreDataset(), acc, hojas);
  }

  public static void configuraOpcion(int opcion) {
    refinedDT = false;
    switch (opcion) {
      case 1: // DT with the best training accuracy in the population
        break;
      case 2: //Refined version of the DT with the best training accuracy in the population.
        refinedDT = true;
    }
  }

  private static double[] obtenerVector(Solution<Double> s) {
    double[] posicion = new double[s.getNumberOfVariables()];
    for (int i = 0; i < posicion.length; i++) {
      posicion[i] = s.getVariableValue(i);
    }
    return posicion;
  }
} // Class
