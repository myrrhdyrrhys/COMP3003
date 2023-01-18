package edu.curtin.comp3003.filecomparer;

public class ComparisonResult
{
    private final String file1;
    private final String file2;
    private final double similarity;
    
    public ComparisonResult(String file1, String file2, double similarity)
    {
        this.file1 = file1;
        this.file2 = file2;
        this.similarity = similarity;
    }
    
    public String getFile1() { return file1; }
    public String getFile2() { return file2; }
    public double getSimilarity() { return similarity; }
}
