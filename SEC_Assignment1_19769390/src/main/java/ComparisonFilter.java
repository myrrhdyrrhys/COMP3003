package edu.curtin.comp3003.filecomparer;

import edu.curtin.comp3003.filecomparer.ComparisonUserInterface;
import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javafx.application.Platform;

public class ComparisonFilter
{
    private ArrayList<String> files;    //full list of all non-empty text files
    private ComparisonUserInterface ui; //for interacting with the UI
    private String searchPath;  //search directory given by the user through the ui, set by constructor
    private ExecutorService es; //for thread pool submission
    private Thread thread;  //reference to finder thread
    /*  Flag used to ensure this class's operations were completed before "getFiles" was called in the GUI thread. 
        I had attempted to wrap the contents of the "getFiles()" and "findFiles()" method in the synchronized statement, 
        assuming that would allow the findFiles() method to run before getFiles() method (as is written in the event handler 
        for the compare button in ComparisonUserInterface), however the getFiles() call appeared to execute before the 
        findFiles() method on every test. Even now, the program is not guaranteed to retrieve the correct list size for unknown reasons.    */
    private int numFiles;   //A counter incremented when files are found, and decremented when they are filtered. ComparisonUserInterface may only access the "files" field when this is 0 (i.e. filtering is done)
    
    public ComparisonFilter(String inSearch, ComparisonUserInterface inUI)
    {
        files = new ArrayList<>();
        this.searchPath = inSearch;
        this.ui = inUI;
        es = Executors.newFixedThreadPool(48);     // Arbitrary hardcoded value here for thread number, IO Bound processes
        numFiles = 0;
    }

    public int getNumFiles(){
        return numFiles;
    }

    public ArrayList<String> getFiles()
    {
        return files;
    }

    //Gets total number of files so UserInterface has a condition to wait on
    public void findNumFiles()
    {
        try
        {       
            // Recursing through the directory tree
            Files.walkFileTree(Paths.get(searchPath), new SimpleFileVisitor<Path>()
            {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                {
                    // Retrieve a filename/path
                    String fileStr = file.toString();
                    String extension = fileStr.substring(fileStr.lastIndexOf(".") + 1, fileStr.length());

                    // Check if file extension is valid (.txt, .md, .java, .cs)
                    if (extension.equals("txt") || extension.equals("md") || extension.equals("java") || extension.equals("cs"))
                    {
                        numFiles++;
                    }

                    return FileVisitResult.CONTINUE;
                }
            });
        }
        catch(IOException e)
        {
            Platform.runLater(() ->
            {
                ui.showError(e.getClass().getName() + ": " + e.getMessage());
            });
        }
    }

    //Method to run threads to find all non empty files in directory given
    public void findFiles()
    {
        Runnable findTask = () -> 
        {            
            try
            {       
                // Recursing through the directory tree
                Files.walkFileTree(Paths.get(searchPath), new SimpleFileVisitor<Path>()
                {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                    {
                        // Retrieve a filename/path
                        String fileStr = file.toString();
                        String extension = fileStr.substring(fileStr.lastIndexOf(".") + 1, fileStr.length());

                        // Check if file extension is valid (.txt, .md, .java, .cs)
                        if (extension.equals("txt") || extension.equals("md") || extension.equals("java") || extension.equals("cs"))
                        {
                            try
                            {
                                // Debugging
                                //System.out.println("File found: " + fileStr);
                                
                                // Start filter thread for that file
                                filterFiles(fileStr);
                            }
                            catch (InterruptedException e)
                            {
                                System.out.println("Filter thread interrupted.");
                                Thread.currentThread().interrupt(); 
                                //throw new RuntimeException(e);
                            }
                        }

                        return FileVisitResult.CONTINUE;
                    }
                });
            }
            catch(IOException e)
            {
                // This will need to update the GUI so will be encompassed by Platform.runLater()
                Platform.runLater(() ->
                {
                    ui.showError(e.getClass().getName() + ": " + e.getMessage());
                });
            }
        };

        thread = new Thread(findTask, "finder-thread");
        thread.start();
    }

    //Method to read contents of file to determine if empty
    public void filterFiles(String fileStr) throws InterruptedException
    {
        // Runnable method for opening and filtering individual file contents
        Runnable filter = () -> 
        {           
            try 
            {
                // Retrieve file contents using path string provided
                FileInputStream fStream = new FileInputStream(fileStr);
                DataInputStream dStream = new DataInputStream(fStream);
                BufferedReader br = new BufferedReader(new InputStreamReader(dStream));
                String strLine = br.readLine();
                
                // Read the first line of file to check if empty, otherwise add to list
                if (strLine != null)
                {
                    // Debugging
                    //System.out.println("Filtered: " + fileStr.substring(fileStr.lastIndexOf("\\") + 1, fileStr.length()));
                    
                    files.add(fileStr);
                }
                
                // Close file when done reading
                dStream.close();
            } 
            catch (IOException e) 
            {
                System.out.println("IO Error: " + e.getMessage());
            }
        };
 
        // Submit filter task to executor service to read through individual file contents
        es.submit(filter);
    }

    public void stop()
    {
        es.shutdownNow();   //interrupt threads submitted to executor service
        thread.interrupt(); //interrupt finder thread
        thread = null;
    }
}
