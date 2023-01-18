package edu.curtin.comp3003.filecomparer;

import java.io.*;
import edu.curtin.comp3003.filecomparer.ComparisonCalculator;
import edu.curtin.comp3003.filecomparer.ComparisonUserInterface;
import javafx.application.Platform;

public class ComparisonFinisher 
{
    private Thread thread;
    private ComparisonUserInterface ui;
    private ComparisonCalculator producer;
    private String filePath1;
    private String filePath2;

    public ComparisonFinisher(ComparisonCalculator inProducer, ComparisonUserInterface inUI, String inFile1, String inFile2)
    {
        this.producer = inProducer;
        this.ui = inUI;
        this.filePath1 = inFile1;
        this.filePath2 = inFile2;
    }

    public void start()
    {
        Runnable finishTask = () ->
        {
            try
            {
                while(true)
                {
                    //Retrieve similarity scoring
                    double similarity = producer.getNextSimilarityScore();
                    
                    //Creation of ComparisonResult Objects to write to Results.csv (filename1,filename2,score\n)
                    ComparisonResult cr = new ComparisonResult(filePath1, filePath2, similarity);
                    
                    //Write object to Results.txt file
                    writeResult(cr);

                    //Check if similarity is above 0.5 to decide whether to display on GUI list
                    if (similarity > 0.5)
                    {
                        Platform.runLater(() ->
                        {
                            ui.updateResultsTable(cr);
                        });
                    }
                    
                    //Update Progress Bar after each complete comparison
                    Platform.runLater(() ->
                    {
                        ui.setProgressBar(calcProgress());
                    });
                }
            }
            catch (InterruptedException e)
            {
                System.out.println("Combination Writing thread interrupted.");
                Thread.currentThread().interrupt(); 
                //throw new RuntimeException(e);
            }
        };

        thread = new Thread(finishTask, "finish-thread");
        thread.start();
    }

    //Method for calculating the % progress to display on progress bar
    public double calcProgress()
    {
        int total = ui.getTotalNumFiles();
        int compared = ui.getNumComparisons();

        int totalComparisons = fact(total) / (fact(2) * fact(total - 2));   //find the total number of combinations possible with the number of files we found

        return (double)(compared / totalComparisons);
    }

    //Method for finding factorial of a number
    public static int fact(int number) 
    {  
        int f = 1;  
        int j = 1;  
    
        while (j <= number) 
        {  
           f = f * j;  
           j++;  
        }  
        
        return f;  
    }  

    //Method for writing a single result to the results.csv
    public void writeResult(ComparisonResult cr)
    {
        try
        {
            FileWriter fw = new FileWriter(ui.getStartPath() + "/results.csv", true);
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(cr.getFile1() + "," + cr.getFile2() + "," + String.valueOf(cr.getSimilarity()) + "\n");
            bw.close();
        }
        catch (IOException e)
        {
            // This will need to update the GUI so will be encompassed by Platform.runLater()
            Platform.runLater(() ->
            {
                ui.showError(e.getClass().getName() + ": " + e.getMessage());
            });
        }
    }
}
