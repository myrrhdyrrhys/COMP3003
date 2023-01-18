package edu.curtin.comp3003.filecomparer;

import javafx.application.Application;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.Scene;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import edu.curtin.comp3003.filecomparer.ComparisonResult;
import edu.curtin.comp3003.filecomparer.ComparisonCombinations;
import edu.curtin.comp3003.filecomparer.ComparisonFilter;
import java.io.File;
import java.util.*;

public class ComparisonUserInterface extends Application
{
    public static void main(String[] args)
    {
        Application.launch(args);
    }
    
    private TableView<ComparisonResult> resultTable = new TableView<>();  
    private ProgressBar progressBar = new ProgressBar();
    private ObservableList<ComparisonResult> toShow = FXCollections.observableArrayList();  //list to allow for dynamic updating of resultTable
    private String startPath;   //the chosen directory by the user
    private int numFiles;       //total number of files in chosen directory, this and the below figure are used in ComparisonFinisher to update the progress bar when a comparison is finished
    private int numCompared;    //the running total of complete comparisons
    private ComparisonFilter cf;    //reference to the class that handles finding files in the directory
    private ComparisonCombinations cc;  //reference to the class that begins the chain of threads that handle the comparisons between files found with ComparisonFilter
    
    @Override
    public void start(Stage stage)
    {
        stage.setTitle("File Comparer");
        stage.setMinWidth(600);

        // Create toolbar
        Button compareBtn = new Button("Compare...");
        Button stopBtn = new Button("Stop");
        ToolBar toolBar = new ToolBar(compareBtn, stopBtn);
        
        // Set up button event handlers.
        compareBtn.setOnAction(event -> crossCompare(stage));
        stopBtn.setOnAction(event -> stopComparison());
        
        // Initialise progressbar
        progressBar.setProgress(0.0);
        
        TableColumn<ComparisonResult,String> file1Col = new TableColumn<>("File 1");
        TableColumn<ComparisonResult,String> file2Col = new TableColumn<>("File 2");
        TableColumn<ComparisonResult,String> similarityCol = new TableColumn<>("Similarity");
        
        // The following tell JavaFX how to extract information from a ComparisonResult 
        // object and put it into the three table columns.
        file1Col.setCellValueFactory(   
            (cell) -> new SimpleStringProperty(cell.getValue().getFile1()) );
            
        file2Col.setCellValueFactory(   
            (cell) -> new SimpleStringProperty(cell.getValue().getFile2()) );
            
        similarityCol.setCellValueFactory(  
            (cell) -> new SimpleStringProperty(
                String.format("%.1f%%", cell.getValue().getSimilarity() * 100.0)) );
          
        // Set and adjust table column widths.
        file1Col.prefWidthProperty().bind(resultTable.widthProperty().multiply(0.40));
        file2Col.prefWidthProperty().bind(resultTable.widthProperty().multiply(0.40));
        similarityCol.prefWidthProperty().bind(resultTable.widthProperty().multiply(0.20));            
        
        // Add the columns to the table.
        resultTable.getColumns().add(file1Col);
        resultTable.getColumns().add(file2Col);
        resultTable.getColumns().add(similarityCol);

        // Add the main parts of the UI to the window.
        BorderPane mainBox = new BorderPane();
        mainBox.setTop(toolBar);
        mainBox.setCenter(resultTable);
        mainBox.setBottom(progressBar);
        Scene scene = new Scene(mainBox);
        stage.setScene(scene);
        stage.sizeToScene();
        stage.show();

        //Set resultTable to display contents of dynamically updated list
        resultTable.setItems(toShow);

        //Set initial value for numCompared to 0
        numCompared = 0;
    }
    
    private void crossCompare(Stage stage)
    {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setInitialDirectory(new File("."));
        dc.setTitle("Choose directory");
        File directory = dc.showDialog(stage);
        
        System.out.println("Comparing files within " + directory + "...");
        
        // Update directory field for finisher to check
        startPath = directory.getPath();

        // Chosen directory is passed to ComparisonFilter class to actually do the comparison process
            // First a list of available text files is compiled
        cf = new ComparisonFilter(startPath, this);
        cf.findNumFiles();
        cf.findFiles();
  
        // Set numFiles once file list has been compiled
        while (cf.getFiles().size() != cf.getNumFiles()) // Wait for filtering to be done
        {
            // Debugging
            //System.out.println("Total files: " + cf.getNumFiles() + ", Files filtered: " + cf.getFiles().size());
        }
        // The following line should only run after the executor service in ComparisonFilter has finished filtering all the files submitted to it
        numFiles = cf.getFiles().size();    
        
        // Debugging
        /*System.out.println("Files list contents: ");
        for (String s : cf.getFiles()){
            System.out.println(s);
        }*/
        
        // Then each combination of text files is identified, and run through the similarity test
        cc = new ComparisonCombinations(this, startPath);
        cc.start(cf.getFiles());
    }
    
    private void stopComparison()
    {
        System.out.println("Stopping comparison...");
        toShow.clear(); //clear on-screen results list

        //interrupt threads
        if (cf != null)
        {
            cf.stop();
        }
        if (cc != null)
        {
            cc.stop();
        }
    }

    //for displaying IO errors in UI
    public void showError(String message) 
    {
        Alert a = new Alert(Alert.AlertType.ERROR, message, ButtonType.CLOSE);
        a.setResizable(true);
        a.showAndWait();
    }

    //will need to be run with Platform.runLater();
    public void setProgressBar(double inProgress)
    {
        progressBar.setProgress(inProgress);
    }

    //will need to be run with Platform.runLater();
    public void updateResultsTable(ComparisonResult inResult)
    {
        toShow.add(inResult);
    }

    public void incrementNumCompared()
    {
        numCompared++;
    }

    public int getNumComparisons()
    {
        return numCompared;
    }

    public int getTotalNumFiles()
    {
        return numFiles;
    }

    public String getStartPath()
    {
        return startPath;
    }
}
