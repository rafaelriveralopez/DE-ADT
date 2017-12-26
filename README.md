# DE-ADT-SPV method
## A Differential evolution based method for inducing axis-parallel decision trees

The DE-ADT-SPV method is a differential-evolution-based approach implementing a global search strategy to find a near-optimal axis-parallel decision tree is introduced. In this approach, the internal nodes of a decision tree are encoded in a real-valued chromosome, and a population of them evolves using the training accuracy of each one as its fitness value.  The height of a complete binary decision tree whose number of internal nodes is not less than the number of attributes in the training set is used to compute the chromosome size, and a procedure to map a feasible axis-parallel decision tree from one chromosome is applied, which uses both the smallest-position-value rule and the training instances.  
The best decision tree in the final population is refined replacing some leaf nodes with sub-trees to improve its accuracy.

- - -

The DE-ADT-SPV method is implemented in Java languaje, and it uses the Differential Evolution code provided by the JMetal library (http://jmetal.github.io/jMetal/). 

For referring to this method, please use:

R. Rivera-Lopez and J. Canul-Reich: *Construction of near-optimal axis-parallel decision trees using a  differential-evolution-based approach*. To appear in IEEE Access (2017)

- - -

The DE-ADT-SPV.zip contains the DEADT_SPV.jar file and the libraries used to run the method.

The deadtspv.zip contains the main source files implementing the method:

1. ConfigurationAP.java: The definition of the main elements of the DE-ADT-SPV method.
2. DEADT.java: The main class to run the DE-ADT-SPV method.

- - -

For run the method in the console:

    java -jar DEADT_SPV.jar [options ...]

where the options are: 

    file=<arff_filename> (default is "iris")
    directory=<directory name> (default is the directory in which the jar file is located)

    trials=<number of trials of the method> (default is 1)
    test=<1|2> (1=to apply the method without decision tree refinement, 2=to apply the method with decision tree. Default is 1)  
    cv=<number of folds for cross-validations> (default is 10)
    cvRepetitions=<number of repetitions of cross-validation procedure> (default is 10)

    F=<DE parameter for controlling the mutation operator> (default is 0.5)
    CR=<DE parameter for controlling the crossover operator> (default is 0.9)
    gen=<number of generations of DE evolutionary process> (default is 200)
    pobFactor=<the factor to determine the population size> (default is 5)
    popMin=<the minimum population size> (default is 200)
    popMax=<the maximum population size> (default is 500)

    bestTree=<training|validation> The type of selection of the best tree in the population: training=the tree with the best training accuracy, validation=the tree with the best selection accuracy (Default is validation)  

    cts  (if the method must be use the entire dataset)

    className=<name of the class attribute> (if this option is not used, the method select the last attribute in the arff file as the class attribute)

    minNumInstances=<the number of instances to define a leaf node> default is 2
  
    echo=<true|false> for activate or desactivate the output in the console (default is true)
    log=<true|false> for activate or desactivate the creation of a file with the results

Examples:

To apply the method without decision tree refinement with the iris dataset, with 10 repetitions of the 10-fold CV:

    java -jar DEADT_SPV.jar 

To apply the method with decision tree refinement with the glass dataset, with 10 repetitions of the 5-fold CV:
 
    java -jar DEADT_SPV.jar file=glass test=2 cv=5
    
