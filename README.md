# DE-ADT
## Differential evolution for inducing axis-parallel decision trees

For run the method in the console:

    java -jar DEADT_SPV.jar [options ...]

where the options are: 

    file=<arff_filename> (default is "iris")
    directory=<directory name>

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
    
