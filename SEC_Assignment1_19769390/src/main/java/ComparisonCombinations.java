package edu.curtin.comp3003.filecomparer;

import java.io.*;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import edu.curtin.comp3003.filecomparer.ComparisonCalculator;
import edu.curtin.comp3003.filecomparer.ComparisonResult;
import javafx.application.Platform;

public class ComparisonCombinations
{
    private ComparisonUserInterface ui;
    private Thread thread;
    private String[] data;  // A temporary array to store all combinations individually as they are calculated
    private String writePath;   //path to write results.csv to
    private ArrayBlockingQueue<String> q = new ArrayBlockingQueue<>(2);    //producer task is likely going to be faster here than the consumer, so synchronous queue

    public ComparisonCombinations(ComparisonUserInterface inUI, String inPath)
    {
        this.ui = inUI;
        writePath = inPath;
        data = new String[2];   
    }

    // Function that handles all combinations 
    public void start(ArrayList<String> arr)
    {
        Runnable combinationsTask = () ->
        {
            try
            {
                // Begin task to find combinations
                findCombinations(arr, data, 0, arr.size() - 1, 0);
            }
            catch (InterruptedException e)
            {
                System.out.println("Combinations thread interrupted.");
                Thread.currentThread().interrupt(); 
                //throw new RuntimeException(e);
            }
        };

        thread = new Thread(combinationsTask, "combinations-thread");
        thread.start();
    }
 
    /* Parameters: input array, temporary array for storing combination, start end indexes of input array, current index in array, combination size (2 in this case) */
    public void findCombinations(ArrayList<String> arr, String data[], int start, int end, int index) throws InterruptedException
    {
        // Current combination is ready
        if (index == data.length)
        {
            // Insert the two file paths of the current combination into the blocking queue for the similarity calculator to remove
            q.put(data[0]);
            q.put(data[1]);

            // Debugging
            //System.out.print("Combination: " + data[0].substring(data[0].lastIndexOf("\\") + 1, data[0].length()) + ", " + data[1].substring(data[1].lastIndexOf("\\") + 1, data[1].length()) + "\n");

            // Start a comparison thread for the current combination
            ComparisonFileReader cfr = new ComparisonFileReader(this, this.ui);
            cfr.start();
            
            return;
        }
        else if (start <= end)
        {
            data[index] = arr.get(start);
            findCombinations(arr, data, start + 1, end, index + 1);
            findCombinations(arr, data, start + 1, end, index);
        }
    }
 
    public String getNextFilePath() throws InterruptedException
    {
        return q.take();    //take one of the two file paths of a complete combination from the blocking queue
    }

    public void stop()
    {
        thread.interrupt();
        thread = null;
    }
}
