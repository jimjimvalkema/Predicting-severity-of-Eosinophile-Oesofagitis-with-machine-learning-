# Predicting-severity-of-Eosinophile-Oesofagitis-with-machine-learning-

## contents
* EDA folder: contains exploratory data analysis
    * Log of the eda and rest of the project:  
           Log-Eosinofiele-Oesofagitis-dataset-EDA.RMD
    * Final report of the project:  
            report-JimjimValkema-Predicting_Eosinofiele_Oesofagitis_with_machine_learning.RMD
* app folder: contains the source and builds of the cli
    * src:   
        contains the source code   
    * build/libs:   
        contains the compiled jar files and some test data
## how to use the commandline interface of the model
To run the program as a .jar:
download this entire folder: app/build/libs/

Then go to the folder where you saved the libs folder and type the following in the terminal to run on the test csv file.
Or change folder path after the "-f" flag to classify your own csv
`java -jar javaWrapper-1.0-SNAPSHOT-all.jar -f testdata/test.csv`

or to run on a single instance 
And change the values after the "-i" flag to run on your own instance.  
The order of attribute are:   
Neocate, Gender, B.Carbohydrates.gr, B.PUFAS.gr, B.Linoleicacid.gr, B.Calcium

`java -jar javaWrapper-1.0-SNAPSHOT-all.jar -i 1,1,20.76,1.49,19.24,1.45`

This program has the following options
-h show help
-f input csv file 
-i classify a single instance