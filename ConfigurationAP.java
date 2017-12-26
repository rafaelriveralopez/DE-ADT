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


import uci.*;
import util.*;
import static uci.Attribute.*;
import static uci.Node.*;
import static util.Parameters.*;

public class ConfigurationAP extends DecisionTree {

  protected int[] umbrales;        // Umbrales asociados a los atributos
  protected int[] indices;         // Indices asociados a los atributos     
  protected int iTerm;             // Número total de atributos a utilizarse 
  protected int dimension;         // Tamaño de la particula (atributos + umbrales)
  protected int numeroClases;
  protected double minPosition = -10;
  protected double maxPosition = 10;
  protected int numNodo = 1;
  protected int ultimoId;
  public static String[] TIPO_NODO = {"X", "I", "H"};
  private int h;

  public ConfigurationAP() {
    int numAtributos = atributos.length - 1;
    double log2 = Math.log10(2);
    int hI = (int) Math.ceil(Math.log10(numAtributos + 1) / log2);
    int hH = (int) Math.ceil(Math.log10(clases.length) / log2);
    int hD = (int) Math.ceil(Math.log10(instancias.length + 1) / log2);
    h = Math.max(hI, hH);
    if (instancias.length < numAtributos) {
      h = Math.min(h, hD);
    }
    h++;
    iTerm = (int) (Math.pow(2, h) - 1);
    if (iTerm < numAtributos) {
      iTerm = numAtributos;
    }
    indices = new int[iTerm];
    int j = 0;
    int k = 0;
    while (j < iTerm) {
      int i = k++ % atributos.length;
      if (i != indiceClase) {
        indices[j++] = i;
      }
    }
    minPosition = -5;
    maxPosition = 5;
    dimension = iTerm + asignarUmbrales();
  }

  public final int asignarUmbrales() {
    int ctos = 0;
    umbrales = new int[iTerm];
    for (int i = 0; i < umbrales.length; i++) {
      boolean esNumerico = atributos[indices[i]].getTipo() == NUMERIC;
      if (!esNumerico) {
        umbrales[i] = -1;
      } else {
        umbrales[i] = iTerm + (ctos++);
      }
    }
    return ctos;
  }

  public AxisParallelNode crearArbolAnchura(double[] posicion, int ind) {
    int[] orden = obtenerAtributos(posicion);
    AxisParallelNode[] nodos = new AxisParallelNode[orden.length];
    for (int i = 0; i < nodos.length; i++) {
      nodos[i] = crearNodo(orden[i], posicion);
    }
    AxisParallelNode raiz = nodos[0];
    int sigHijo = 1;
    for (AxisParallelNode nodo : nodos) {
      if (nodo.getTipoNodo() != NODO_INUTIL) {
        for (int j = 0; j < nodo.getAridad(); j++) {
          if (sigHijo < nodos.length) {
            Attribute atributo = nodos[sigHijo].getAtributo();
            if (!ocupado(atributo, nodo, nodos[sigHijo], true)
              && !(nodo.getAridad() == 2 && j == 1
              && ((AxisParallelNode) nodo.getHijos()[0]).getAtributo() == atributo
              && atributo.getTipo() == NOMINAL)) {
              nodo.getHijos()[j] = nodos[sigHijo];
              nodos[sigHijo].setPadre(nodo);
              nodo.setTipoNodo(NODO_INTERNO);
            } else {
              nodos[sigHijo].setTipoNodo(NODO_INUTIL);
              j--;
            }
            sigHijo++;
          }
        }
      }
    }
    Clasification ejemplos = getInstanciasRaiz();
//    if (Parameters.CONSTRUCCION == Parameters.MUESTREO) {
//      ejemplos = getMuestreoInstancias()[ind];
//    }
    raiz.setInstancias(ejemplos);
    Queue<AxisParallelNode> cola = new Queue();
    cola.enqueue(raiz);
    int contador = nodos.length + 10;
    while (!cola.isEmpty()) {
      AxisParallelNode nodo = cola.dequeue();
      if (nodo.isNodoHoja()) {
        nodo.setTipoNodo(NODO_HOJA);
        nodo.setHijos(null);
      } else {
        nodo.setTipoNodo(NODO_INTERNO);
        Clasification[] instHijo = asignarInstanciasEnNodo(nodo, posicion);
        nodo.setInstHijo(instHijo);
//        if (nodo.getAridad() == 2) {
//          for (Clasification hijo : instHijo) {
////            if (hijo.getContador() < THRESHOLD_INSTANCES) {
////            if ((h>5 && hijo.getContador()<THRESHOLD_INSTANCES) || 
////              (h<=5 && hijo.getContador() == 0)) {
//            if (hijo.getContador() == 0) {
//              nodo.setTipoNodo(NODO_HOJA);
//              break;
//            }
//          }
//        }
        if (nodo.getTipoNodo() == NODO_INTERNO) {
          for (int j = 0; j < nodo.getAridad(); j++) {
            if (nodo.getHijos()[j] != null) {
              nodo.getHijos()[j].setInstancias(instHijo[j]);
              cola.enqueue((AxisParallelNode) nodo.getHijos()[j]);
            } else {
              AxisParallelNode nuevo = new AxisParallelNode(contador++);
              nodo.getHijos()[j] = nuevo;
              nuevo.setInstancias(nodo.getInstHijo()[j]);
              nuevo.setPadre(nodo);
              nuevo.setTipoNodo(NODO_HOJA);
              nuevo.setHijos(null);
            }
          }
        }
      }
    }
    this.ultimoId = contador;
    return raiz;
  }

  public boolean ocupado(Attribute atributo, AxisParallelNode nodo, AxisParallelNode nuevo, boolean numerico) {
    boolean usado = false;
    AxisParallelNode rama = nodo;
    while (rama != null) {
      if (rama.getAtributo() == atributo) {
        usado = true;
        break;
      }
      rama = (AxisParallelNode) rama.getPadre();
    }
    if (numerico) {
      if (atributo.getTipo() == NUMERIC && rama != null
        && rama.getUmbral() <= nuevo.getUmbral()) {
        return true;
      }
    }
    return atributo.getTipo() == NOMINAL && usado;
  }

  //GVP method
  public int[] obtenerAtributos(double[] posicion) {
    int[] orden = new int[iTerm];
    int lugar = 0;
    double[] att = new double[iTerm];
    System.arraycopy(posicion, 0, att, 0, att.length);
    for (int i = 0; i < iTerm; i++) {
      double max = Double.POSITIVE_INFINITY;
      int iMax = -1;
      for (int j = 0; j < iTerm; j++) {
        if (att[j] < max) {
          max = att[j];
          iMax = j;
        }
      }
      orden[lugar++] = iMax;
      att[iMax] = Double.POSITIVE_INFINITY;
    }
    return orden;
  }

  public AxisParallelNode crearNodo(int lugar, double[] posicion) {
    int lugarAtributo = indices[lugar];
    Attribute atributo = atributos[lugarAtributo];
    AxisParallelNode nodo = new AxisParallelNode(lugar, atributo);
    nodo.setIndiceAtributo(lugarAtributo);
    nodo.setAridad(atributo.getAridad());
    nodo.setHijos(new AxisParallelNode[atributo.getAridad()]);
    nodo.setTipoNodo(NODO_INTERNO);
    if (atributo.getTipo() == NUMERIC) {
      int posUmbral = umbrales[lugar];
      nodo.setUmbral(obtenerUmbral(lugar, posicion[posUmbral]));
    }
    return nodo;
  }

  public double obtenerUmbral(int lugar, double valorSwarm) {
    int lugarAtributo = indices[lugar];
    NumericalAttribute atr = (NumericalAttribute) atributos[lugarAtributo];
    double rangoDato = atr.getRango();
    double rangoSwarm = maxPosition - minPosition;
    double factor = rangoDato / rangoSwarm;
    return (valorSwarm - minPosition) * factor + atr.getValorMinimo();
  }

  public Clasification[] asignarInstanciasEnNodo(AxisParallelNode nodo, double[] val) {
    Clasification instNodo = nodo.getInstancias();
    Clasification[] instHijo = new Clasification[nodo.getHijos().length];
    for (int i = 0; i < instHijo.length; i++) {
      instHijo[i] = new Clasification(instNodo.getContador(), clases.length);
    }
    int lugarAtributo = nodo.getIndiceAtributo();
    switch (nodo.getAtributo().getTipo()) {
      case NUMERIC: {
        double umbral = nodo.getUmbral();
        for (int i = 0; i < instNodo.getContador(); i++) {
          Instance inst = instancias[instNodo.getInstancias()[i]];
          double valor = inst.getValores()[lugarAtributo];
          int cual = valor <= umbral ? 0 : 1;
          instHijo[cual].addInstancia(inst);
        }
        break;
      }
      case NOMINAL: {
        for (int i = 0; i < instNodo.getContador(); i++) {
          Instance inst = instancias[instNodo.getInstancias()[i]];
          int valor = (int) inst.getValores()[lugarAtributo];
          instHijo[valor].addInstancia(inst);
        }
      }
    }
    return instHijo;
  }

  @Override
  public int clasificarConArbol(Node nodo1, Instance instancia) {
    AxisParallelNode nodo = (AxisParallelNode) nodo1;
    if (nodo.getTipoNodo() == NODO_HOJA) {
      return nodo.getInstancias().getIndiceMejorClase();
    }
    Attribute atributo = nodo.getAtributo();
    int lugarAtributo = nodo.getIndiceAtributo();
    double umbral = nodo.getUmbral();
    double valor = instancia.getValores()[lugarAtributo];
    int cual = 0;
    switch (atributo.getTipo()) {
      case NUMERIC: {
        cual = valor <= umbral ? 0 : 1;
        break;
      }
      case NOMINAL: {
        cual = (int) valor;
      }
    }
    return clasificarConArbol(nodo.getHijos()[cual], instancia);
  }

  public String sacarArbol(AxisParallelNode nodo, int nivel, int cual) {
    String salida = "";
    String cad;
    if (nodo == null) {
      return salida;
    }
    AxisParallelNode padre = (AxisParallelNode) nodo.getPadre();
    if (padre != null && nodo.getId() == padre.getHijos()[padre.getHijos().length - 1].getId()) {
      String particula = obtenParticula(padre);
      String umbral = obtenUmbral(padre);
      cad = "";
      for (int i = 0; i < nivel - 1; i++) {
        cad += "|  ";
      }
      if (padre.getAtributo().getTipo() == Attribute.NUMERIC) {
        salida += "\n" + cad + particula + " > " + umbral;
      }
    }
    AxisParallelNode nCat = (AxisParallelNode) nodo.getPadre();
    if (nCat != null && nCat.getAtributo().getTipo() == Attribute.NOMINAL) {
      String particula = obtenParticula(nCat);
      cad = "";
      for (int i = 0; i < nivel - 1; i++) {
        cad += "|  ";
      }
      CategorialAttribute ac = (CategorialAttribute) nCat.getAtributo();
      salida += "\n" + cad + particula + " == " + ac.getValores()[cual];
    }
    if (nodo.getTipoNodo() != NODO_HOJA) {
      String particula = obtenParticula(nodo);
      String umbral = obtenUmbral(nodo);
      cad = "";
      for (int i = 0; i < nivel; i++) {
        cad += "|  ";
      }
      if (nodo.getAtributo().getTipo() == Attribute.NUMERIC) {
        salida += "\n" + cad + particula + " <= " + umbral;
      }
    } else {
      Clasification instNodo = nodo.getInstancias();
      int mejorClase = instNodo.getIndiceMejorClase();
      int acc = instNodo.getValorMejorClase();
      int err = instNodo.getContador() - acc;
      String etiqueta = "{" + nodo.getId() + "} [" + mejorClase + "]";
      salida += ": " + etiqueta + "(" + acc + "/" + err + ")";
    }
    if (nodo.getHijos() != null && nodo.getTipoNodo() == NODO_INTERNO) {
      int k = 0;
      for (AxisParallelNode hijo : (AxisParallelNode[]) nodo.getHijos()) {
        salida += sacarArbol(hijo, nivel + 1, k);
        k++;
      }
    }
    return salida;
  }

  public void imprimirArbol(AxisParallelNode nodo, int nivel) {
    System.out.println(sacarArbol(nodo, nivel, 0));
  }

  public String obtenParticula(AxisParallelNode nodo) {
    for (Attribute atributo : atributos) {
      if (atributo == nodo.getAtributo()) {
        return atributo.getNombre();
      }
    }
    return nodo.getAtributo().getNombre();
  }

  public String obtenUmbral(AxisParallelNode nodo) {
    double umbral = nodo.getUmbral();
    return (umbral < 0 ? "-" : "") + String.format("%5.2f", Math.abs(umbral)).trim();
  }

  @Override
  public void crearRama(Node nodo1) {
    AxisParallelNode nodo = (AxisParallelNode) nodo1;
    switch (TIPO_SPLITTING) {
      case Parameters.SPLITTING_ERROR:
        crearRamaUsandoError(nodo);
        break;
      case Parameters.SPLITTING_INFO:
        crearRamaUsandoCriterio(nodo);
    }
  }

  public void crearRamaUsandoError(AxisParallelNode nodo) {
    Clasification instNodo = nodo.getInstancias();
    int errorActual = instNodo.getContador() - instNodo.getValorMejorClase();
    double menorError = Double.MAX_VALUE;
    int mejorAtributo = -1;
    double mejorUmbral = -1;
    int errores = 0;
    Clasification[] mejoresHijos = null;
    AxisParallelNode prueba = new AxisParallelNode(-1);
    AxisParallelNode padre = (AxisParallelNode) nodo.getPadre();
    for (int atr = 0; atr < atributos.length; atr++) {
      if (atr != indiceClase
        && !ocupado(atributos[atr], nodo, prueba, false)
        && !(atributos[atr].getTipo() == NOMINAL
        && padre.getAridad() == 2
        && nodo == padre.getHijos()[1]
        && padre.getHijos()[0].getTipoNodo() == NODO_INTERNO
        && ((AxisParallelNode) padre.getHijos()[0]).getAtributo() == atributos[atr])) {
        prueba.setAtributo(atributos[atr]);
        prueba.setIndiceAtributo(atr);
        prueba.setInstancias(instNodo);
        int aridad = atributos[atr].getAridad();
        prueba.setHijos(new AxisParallelNode[aridad]);
        if (atributos[atr].getTipo() == NUMERIC) {
//          Double[][] lista = crearUmbrales(instNodo);
          double[] lista = crearListaValores(atr, instNodo);
          for (int k = 1; k < lista.length; k++) {
            double umbral = lista[k - 1] + (lista[k] - lista[k - 1]) / 2;
            prueba.setUmbral(umbral);
            if (!ocupado(atributos[atr], nodo, prueba, true)) {
              Clasification[] instHijo = asignarInstanciasEnNodo(prueba, null);
              errores = 0;
              for (Clasification hijo : instHijo) {
                errores += (hijo.getContador() - hijo.getValorMejorClase());
              }
              if (errores < menorError) {
                boolean crecer = true;
                for (Clasification hijo : instHijo) {
                  if (hijo.getContador() <= THRESHOLD_INSTANCES) {
                    crecer = false;
                    break;
                  }
                }
                if (crecer) {
                  menorError = errores;
                  mejorAtributo = atr;
                  mejorUmbral = umbral;
                  mejoresHijos = new Clasification[instHijo.length];
                  System.arraycopy(instHijo, 0, mejoresHijos, 0, instHijo.length);
                }
              }
            }
          }
        } else if (atributos[atr].getTipo() == NOMINAL) {
          Clasification[] instHijo = asignarInstanciasEnNodo(prueba, null);
          errores = 0;
          for (Clasification hijo : instHijo) {
            errores += (hijo.getContador() - hijo.getValorMejorClase());
          }
          if (errores < menorError) {
            boolean crecer = true;
            if (instHijo != null && instHijo.length == 2) {
              for (Clasification hijo : instHijo) {
                if (hijo.getContador() <= THRESHOLD_INSTANCES) {
                  crecer = false;
                  break;
                }
              }
            }
            if (crecer) {
              menorError = errores;
              mejorAtributo = atr;
              mejoresHijos = new Clasification[instHijo.length];
              System.arraycopy(instHijo, 0, mejoresHijos, 0, instHijo.length);
            }
          }
        }
      }
    }
    boolean crecer = true;
    if (mejoresHijos != null && mejoresHijos.length == 2) {
      for (Clasification mejorHijo : mejoresHijos) {
        if (mejorHijo.getContador() <= THRESHOLD_INSTANCES) {
          crecer = false;
          break;
        }
      }
    }
    if (crecer && menorError < Double.MAX_VALUE && menorError < errorActual) {
      nodo.setTipoNodo(NODO_INTERNO);
      nodo.setAtributo(atributos[mejorAtributo]);
      nodo.setIndiceAtributo(mejorAtributo);
      nodo.setUmbral(mejorUmbral);
      AxisParallelNode[] hijos = new AxisParallelNode[nodo.getAtributo().getAridad()];
      nodo.setHijos(hijos);
      for (int j = 0; j < nodo.getHijos().length; j++) {
        hijos[j] = new AxisParallelNode(++ultimoId);
        hijos[j].setPadre(nodo);
        hijos[j].setInstancias(mejoresHijos[j]);
        hijos[j].setTipoNodo(NODO_HOJA);
        hijos[j].setHijos(null);
        if (!hijos[j].isNodoHoja()) {
          crearRama(hijos[j]);
        }
      }
    }
  }

  public void crearRamaUsandoCriterio(AxisParallelNode nodo) {
    double mejorCriterio = 0;
    int mejorAtributo = -1;
    double mejorUmbral = -1;
    Clasification[] mejoresHijos = null;
    Clasification instNodo = nodo.getInstancias();
    AxisParallelNode prueba = new AxisParallelNode(-1);
    AxisParallelNode padre = (AxisParallelNode) nodo.getPadre();
    double criterio = -1;
    for (int atr = 0; atr < atributos.length; atr++) {
      if (atr != indiceClase
        && !ocupado(atributos[atr], nodo, prueba, false)
        && !(padre != null && atributos[atr].getTipo() == NOMINAL 
             && padre.getAridad() == 2
             && nodo == padre.getHijos()[1]
             && padre.getHijos()[0].getTipoNodo() == NODO_INTERNO
             && ((AxisParallelNode) padre.getHijos()[0]).getAtributo() == atributos[atr])
        ) {
        prueba.setAtributo(atributos[atr]);
        prueba.setIndiceAtributo(atr);
        prueba.setInstancias(instNodo);
        int aridad = atributos[atr].getAridad();
        prueba.setHijos(new AxisParallelNode[aridad]);
        if (atributos[atr].getTipo() == NUMERIC) {
          double[] lista = crearListaValores(atr, instNodo);
          for (int k = 1; k < lista.length; k++) {
            double umbral = lista[k - 1] + (lista[k] - lista[k - 1]) / 2;
            prueba.setUmbral(umbral);
            if (!ocupado(atributos[atr], nodo, prueba, true)) {
              Clasification[] instHijo = asignarInstanciasEnNodo(prueba, null);
              criterio = gainRatio(instNodo, instHijo);
//              criterio = twoing(instNodo, instHijo);
              if (criterio > mejorCriterio) {
                boolean crecer = true;
//                for (Clasification hijo : instHijo) {
////                  if (hijo.getContador() <= THRESHOLD_INSTANCES) {
//                  if (hijo.getContador() < 2) {
//                    crecer = false;
//                    break;
//                  }
//                }
                if (crecer) {
                  mejorCriterio = criterio;
                  mejorAtributo = atr;
                  mejorUmbral = umbral;
                  mejoresHijos = new Clasification[instHijo.length];
                  System.arraycopy(instHijo, 0, mejoresHijos, 0, instHijo.length);
                }
              }
            }
          }
        } else if (atributos[atr].getTipo() == NOMINAL) {
          Clasification[] instHijo = asignarInstanciasEnNodo(prueba, null);
          criterio = gainRatio(instNodo, instHijo);
          if (criterio > mejorCriterio) {
            boolean crecer = true;
//            if (instHijo != null && instHijo.length == 2) {
//              for (Clasification hijo : instHijo) {
////                if (hijo.getContador() <= THRESHOLD_INSTANCES) {
//                if (hijo.getContador() < 2) {
//                  crecer = false;
//                  break;
//                }
//              }
//            }
            if (crecer) {
              mejorCriterio = criterio;
              mejorAtributo = atr;
              mejoresHijos = new Clasification[instHijo.length];
              System.arraycopy(instHijo, 0, mejoresHijos, 0, instHijo.length);
            }
          }
        }
      }
    }
    boolean crecer = true;
//    if (mejoresHijos != null && mejoresHijos.length == 2) {
//      for (Clasification mejorHijo : mejoresHijos) {
////        if (mejorHijo.getContador() <= THRESHOLD_INSTANCES) {
//        if (mejorHijo.getContador() < 2) {
//          crecer = false;
//          break;
//        }
//      }
//    }
    if (crecer && mejorCriterio > 0) {
      nodo.setTipoNodo(NODO_INTERNO);
      nodo.setAtributo(atributos[mejorAtributo]);
      nodo.setIndiceAtributo(mejorAtributo);
      nodo.setUmbral(mejorUmbral);
      AxisParallelNode[] hijos = new AxisParallelNode[nodo.getAtributo().getAridad()];
      nodo.setHijos(hijos);
      for (int j = 0; j < nodo.getHijos().length; j++) {
        hijos[j] = new AxisParallelNode(++ultimoId);
        hijos[j].setPadre(nodo);
        hijos[j].setInstancias(mejoresHijos[j]);
        hijos[j].setTipoNodo(NODO_HOJA);
        hijos[j].setHijos(null);
        if (!hijos[j].isNodoHoja()) {
          crearRama(hijos[j]);
        }
      }
    }
  }

  public void imprimirNodos(AxisParallelNode nodo) {
    Queue<AxisParallelNode> s = new Queue<>();
    s.enqueue(nodo);
    while (!s.isEmpty()) {
      AxisParallelNode actual = s.dequeue();
      if (actual.getTipoNodo() == NODO_HOJA) {
        String cad = "";
        while (actual != null) {
          if (actual.getTipoNodo() == NODO_INTERNO) {
            if (actual.getId() < iTerm) {
              cad = "(" + indices[actual.getId()] + "){" + actual.getId() + "}->" + cad;
            } else {
              cad = "<*" + actual.getIndiceAtributo() + "*>{" + actual.getId() + "}->" + cad;
            }
          } else {
            cad = "[" + actual.getInstancias().getValorMejorClase() + "/"
              + (actual.getInstancias().getContador()
              - actual.getInstancias().getValorMejorClase()) + "]" + cad;
          }
          actual = (AxisParallelNode) actual.getPadre();
        }
        System.out.println(cad);
      } else {
        AxisParallelNode[] hijos = (AxisParallelNode[]) actual.getHijos();
        for (AxisParallelNode hijo : hijos) {
          s.enqueue(hijo);
        }
      }
    }
  }

  public int getiTerm() {
    return iTerm;
  }

  public int getH() {
    return h;
  }

  public int getDimension() {
    return dimension;
  }

  public int getNumNodo() {
    return numNodo;
  }

  public void setNumNodo(int numNodo) {
    this.numNodo = numNodo;
  }

  public double getMinPosition() {
    return minPosition;
  }

  public void setMinPosition(double minPosition) {
    this.minPosition = minPosition;
  }

  public double getMaxPosition() {
    return maxPosition;
  }

  public void setMaxPosition(double maxPosition) {
    this.maxPosition = maxPosition;
  }

}
