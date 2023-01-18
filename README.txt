------- File Comparer -------

Purpose: To compare all text files in a given directory to one another so that those comparisons with a similarity score of above 0.5 may be displayed to the user, with all results written to an output "results.csv" file.

Compilation/Execution: This project makes use of Gradle for compilation. To build and run the project, navigate to the "SEC_Assignment1_19769390/" folder in terminal and enter the following command:
	(on Linux) ./gradlew run
	(on Windows) .\gradlew run

Usage: To add a directory you wish to run the comparer on, simply click on the Compare.. button in the pop-up window and choose an appropriate folder.
	Full comparison results are written to results.csv in the given directory.
	The \test directory contains a few simple sample files for testing.
	The code contains some print lines for debugging that have been commented out, but can be used if interested.
	