package edu.curtin.comp3003.filecomparer;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import edu.curtin.comp3003.filecomparer.ComparisonCalculator;
import edu.curtin.comp3003.filecomparer.ComparisonUserInterface;
import javafx.application.Platform;

public class ComparisonFileReader
{
    private ComparisonCombinations producer;
    private Thread thread;
    private ArrayBlockingQueue<char[]> q = new ArrayBlockingQueue<>(2);     //array for file contents
    private ArrayBlockingQueue<String> pathQueue = new ArrayBlockingQueue<>(2);     //array for associated file paths to the contents
    private ComparisonUserInterface ui;

    public ComparisonFileReader(ComparisonCombinations inProducer, ComparisonUserInterface inUI)
    {
        this.producer = inProducer;
        this.ui = inUI;
    }

    public void start()
    {
        Runnable readTask = () ->
        {
            try
            {
                while(true)
                {
                    char[] result = new char[0];

                    try 
                    {
                        // Retrieve a path string from ComparisonCombinations (producer) queue
                        String fileStr = producer.getNextFilePath();
                        
                        // Debugging
                        //System.out.println("Reading: " + fileStr.substring(fileStr.lastIndexOf("\\") + 1, fileStr.length()));
                        
                        pathQueue.put(fileStr);

                        // Retrieve file contents using path string provided
                        FileInputStream fStream = new FileInputStream(fileStr);
                        DataInputStream dStream = new DataInputStream(fStream);
                        BufferedReader br = new BufferedReader(new InputStreamReader(dStream));
                        int strChar = br.read();
                        ArrayList<Character> contents = new ArrayList<>();
                        
                        // Read the file char-by-char until eof
                        while (strChar != -1) 
                        {
                            // Add line to array
                            contents.add((char) strChar);

                            strChar = br.read();    //read in next line
                        }
                        
                        // Close file when done reading
                        dStream.close();

                        // Convert ArrayList to char[] for ComparisonCalculator
                        result = new char[contents.size()];
                        for(int i = 0; i < contents.size(); i++) {
                            result[i] = contents.get(i);
                        }

                    } 
                    catch (IOException e) 
                    {
                        // This will need to update the GUI so will be encompassed by Platform.runLater()
                        Platform.runLater(() ->
                        {
                            ui.showError(e.getClass().getName() + ": " + e.getMessage());
                        });
                    }

                    //put contents of file into queue and start similarity calculation thread
                    q.put(result);
                    if (q.size() == 2)
                    {
                        ComparisonCalculator cc = new ComparisonCalculator(this, this.ui, pathQueue.take(), pathQueue.take());
                        cc.start();
                    }
                    
                }

            }
            catch (InterruptedException e)
            {
                System.out.println("Reading thread interrupted.");
                Thread.currentThread().interrupt(); 
                //throw new RuntimeException(e);
            }
        };

        thread = new Thread(readTask, "read-thread");
        thread.start();
    }

    public char[] getNextFileContents() throws InterruptedException
    {
        return q.take();    //take one of the two file's contents from the current combination
    }

}