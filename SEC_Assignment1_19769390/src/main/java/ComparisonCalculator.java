package edu.curtin.comp3003.filecomparer;

import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import edu.curtin.comp3003.filecomparer.ComparisonFileReader;
import edu.curtin.comp3003.filecomparer.ComparisonUserInterface;
import javafx.application.Platform;

public class ComparisonCalculator 
{
    private ComparisonFileReader producer;
    private Thread thread;
    private ArrayBlockingQueue<Double> q = new ArrayBlockingQueue<>(1);
    private String filePath1;
    private String filePath2;
    private ComparisonUserInterface ui;

    public ComparisonCalculator(ComparisonFileReader inProducer, ComparisonUserInterface inUI, String path1, String path2)
    {
        this.producer = inProducer;
        this.ui = inUI;
        this.filePath1 = path1;
        this.filePath2 = path2;
    }

    //Method for calculating the similarity score between the contents of two files contents passed as char arrays. Logic taken directly from Assignment Specification.
    public void start()
    {
        Runnable calcTask = () ->
        {
            try
            {
                while(true)
                {
                    char[] file1 = producer.getNextFileContents();
                    char[] file2 = producer.getNextFileContents();

                    // Debugging
                    //System.out.println("FileA Contents: " + Arrays.toString(file1) + ", FileB Contents: " + Arrays.toString(file2));

                    int[][] subsolutions = new int[file1.length + 1][file2.length + 1];
                    boolean[][] directionLeft = new boolean[file1.length + 1][file2.length + 1];

                    //fill first row and column of subsolutions with 0s
                    for (int i = 0; i < file1.length + 1; i++)
                    {
                        subsolutions[i][0] = 0;
                    }
                    for (int i = 0; i < file2.length + 1; i++)
                    {
                        subsolutions[0][i] = 0;
                    }

                    for (int i = 1; i <= file1.length; i++)
                    {
                        for (int j = 1; j <= file2.length; j++)
                        {
                            if (file1[i - 1] == file2[j - 1])
                            {
                                subsolutions[i][j] = subsolutions[i - 1][j - 1] + 1;
                            }
                            else if (subsolutions[i - 1][j] > subsolutions[i][j - 1])
                            {
                                subsolutions[i][j] = subsolutions[i - 1][j];
                                directionLeft[i][j] = true;
                            }
                            else
                            {
                                subsolutions[i][j] = subsolutions[i][j - 1];
                                directionLeft[i][j] = false;
                            }
                            
                        }
                    }    
                    
                    int matches = 0;
                    int i = file1.length;
                    int j = file2.length;

                    while (i > 0 && j > 0)
                    {
                        if (file1[i - 1] == file2[j - 1])
                        {
                            matches += 1;
                            i -= 1;
                            j -= 1;
                        }
                        else if (directionLeft[i][j])
                        {
                            i -= 1;
                        }
                        else        
                        {
                            j -= 1;
                        }
                    }

                    //put similarity score in queue and start a comparison finisher thread
                    q.put((double) (matches * 2) / (file1.length + file2.length));

                    //increment total number of comparisons (for progress-bar)
                    Platform.runLater(() ->
                    {
                        ui.incrementNumCompared();
                    });

                    //begin thread to finish off the comparison
                    ComparisonFinisher cf = new ComparisonFinisher(this, this.ui, this.filePath1, this.filePath2);
                    cf.start();
                }
            }
            catch (InterruptedException e)
            {
                System.out.println("Similarity Calculating thread interrupted.");
                Thread.currentThread().interrupt(); 
                //throw new RuntimeException(e);
            }
        };

        thread = new Thread(calcTask, "calc-thread");
        thread.start();
    }

    public double getNextSimilarityScore() throws InterruptedException
    {
        return q.take();    //take the similarity score of the current combination
    }    
}
