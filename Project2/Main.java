import java.io.File; // Import the File class
import java.io.FileNotFoundException; // Import this class to handle errors
import java.io.FileWriter;
import java.io.IOException;
import java.lang.management.ThreadInfo;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Scanner; // Import the Scanner class to read text files
import java.util.concurrent.TimeUnit;

import javax.print.DocFlavor.STRING;

public class Main {
    public static void main(String[] args) {

        Boolean indexesBuilt = false; // boolean for whether indexes are built yet or not

        hashBasedIndex hbi = new hashBasedIndex(); // object for hash-based index
        arrayBasedIndex abi = new arrayBasedIndex(); // object for array based index

        System.out.println("Program is ready and waiting for user command");

        Scanner myObj = new Scanner(System.in); // Create a Scanner object

        // Loop to allow the input of commands infintely
        while (true) {
            String comm = myObj.nextLine();

            // creates index if the command includes "CREATE INDEX"
            if (comm.contains("CREATE INDEX ON Project2Dataset")) {
                // reads all records and stores them in an array
                String[] allRecs = readAllRecords("Project2Dataset");
                hbi.buildIndex(allRecs); // builds hash-based index with the records
                abi.buildIndex(allRecs); // builds array based index with the records

                indexesBuilt = true;

                System.out.println(
                        "The hash-based and array-based indexes are built successfully. Program is ready and waiting for user command.");

                // runs queries if command includes "SELECT * FROM"
            } else if (comm.contains("SELECT * FROM Project2Dataset WHERE RandomV")) {
                long startTime = System.nanoTime();
                // runs for inequality query
                if (comm.contains("!=")) {
                    int val = Integer.parseInt(comm.substring(comm.indexOf("=") + 1).trim());
                    int[] values = new int[1];
                    values[0] = val;
                    // runs full table scan for the randomV value given
                    fullTableScan("Project2Dataset", values, true);

                    // prints out time taken
                    long endTime = System.nanoTime();
                    long durationInNano = (endTime - startTime);
                    long durationInMillis = TimeUnit.NANOSECONDS.toMillis(durationInNano);
                    System.out.println(durationInMillis + " ms");

                    // Number of files read
                    System.out.println("1 file read");
                } else if (comm.contains("=")) {
                    int val = Integer.parseInt(comm.substring(comm.indexOf("=") + 1).trim());
                    int[] values = new int[1];
                    values[0] = val;
                    String indexType = "";
                    // Prints records
                    if (indexesBuilt) {
                        hbi.printRecords(values[0]);
                    } else { // runs full table scan if indexes not built yet
                        System.out.println("===========");
                        fullTableScan("Project2Dataset", values, false);
                        indexType = "Full Table Scan";
                    }
                    // Index type or Full Scan
                    System.out.println(indexType);

                    // Time Taken
                    long endTime = System.nanoTime();
                    long durationInNano = (endTime - startTime);
                    long durationInMillis = TimeUnit.NANOSECONDS.toMillis(durationInNano);
                    System.out.println(durationInMillis + " ms");

                    // Number of files read
                    System.out.println("3 Files read");
                } else if ((comm.contains(">")) && (comm.contains("<"))) {
                    String indexType = "";
                    int minval = Integer.parseInt(comm.substring(comm.indexOf(">") + 1, comm.indexOf(">") + 6).trim());
                    int maxval = Integer.parseInt(comm.substring(comm.indexOf("<") + 1).trim());
                    int[] values = new int[maxval - minval];
                    int j = 0;

                    // gets all integers between the minimum and maximum value specified
                    for (int i = minval; i < maxval; i++) {
                        values[j] = i;
                        j = j + 1;
                    }
                    // Prints Records
                    if (indexesBuilt) {
                        // runs index based search on each value in the range
                        for (int i : values) {
                            abi.printRecords(i);
                            indexType = "Index-Based Search";
                        }

                    } else { // full table scan if indexes not built
                        fullTableScan("Project2Dataset", values, false);
                        indexType = "Full Table Scan";
                    }
                    // Index type or Full Scan
                    System.out.println(indexType);
                    // Time Taken
                    long endTime = System.nanoTime();
                    long durationInNano = (endTime - startTime);
                    long durationInMillis = TimeUnit.NANOSECONDS.toMillis(durationInNano);

                    System.out.println(durationInMillis + " ms");
                    // Number of files read
                }
            }
        }
    }

    // function to read all records from a directory
    public static String[] readAllRecords(String dirName) {
        String data = "";
        final File file = new File("./" + dirName);
        for (final File child : file.listFiles()) {
            String datalines = "";
            try {
                File myObj = child;
                Scanner myReader = new Scanner(myObj);
                while (myReader.hasNextLine()) {
                    datalines = myReader.nextLine();
                }
                myReader.close();
            } catch (FileNotFoundException e) {
                System.out.println("An error occurred.");
                e.printStackTrace();
            }

            data = data + datalines;
        }

        return data.split("\\...");
    }

    // function to run a full table scan
    public static void fullTableScan(String dirName, int[] val, Boolean inequal) {
        String[] str = readAllRecords(dirName);

        for (String string : str) {
            for (int i : val) {
                if (inequal) {
                    if (!(string.split(", ")[3].equals(Integer.toString(i)))) {
                        System.out.println(string);
                    }
                } else {
                    if (string.split(", ")[3].equals(Integer.toString(i))) {
                        System.out.println(string);
                    }
                }

            }

        }

        System.out.println("99 files read");

    }
}

// data structure to store the location of a record
class recLocation {
    int fileNum;
    int offset;

    public recLocation(int fileNum, int offset) {
        this.fileNum = fileNum;
        this.offset = offset;
    }

    public int getFileNum() {
        return this.fileNum;
    }

    public void setFileNum(int fileNum) {
        this.fileNum = fileNum;
    }

    public int getOffset() {
        return this.offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

}

// class for each hash table entry
class hashRow {
    int k; // randomV k
    recLocation v; // record location v

    public hashRow(int k, recLocation v) {
        this.k = k;
        this.v = v;
    }

    public int getK() {
        return this.k;
    }

    public void setK(int k) {
        this.k = k;
    }

    public recLocation getV() {
        return this.v;
    }

    public void setV(recLocation v) {
        this.v = v;
    }

}

// class for hash table
class hashBasedIndex {
    hashRow[] index; // random v k, record location v

    public hashBasedIndex() {
    }

    public hashRow[] getIndex() {
        return this.index;
    }

    public void setIndex(hashRow[] index) {
        this.index = index;
    }

    public int hashFunction(int i) {
        return i / 2;
    }

    public void printRecords(int v) {
        if (this.getIndex()[hashFunction(v)].getV().getFileNum() != 0) {
            System.out.println(readRecord(this.getIndex()[hashFunction(v)].getV().getFileNum(),
                    this.getIndex()[hashFunction(v)].getV().getOffset()));
        }

    }

    public String readRecord(int fNum, int offset) {
        File file = new File("./Project2Dataset/F" + fNum + ".txt");
        String datalines = "";
        try {
            File myObj = file;
            Scanner myReader = new Scanner(myObj);
            while (myReader.hasNextLine()) {
                datalines = myReader.nextLine();
            }
            myReader.close();
        } catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }

        return datalines.split("\\...")[offset - 1];
    }

    public int[] getFileAndOffset(String s) {
        int[] result = new int[2];

        result[0] = Integer.parseInt(s.substring(1, 3));
        result[1] = Integer.parseInt(s.substring(7, 10));

        return result;
    }

    public void buildIndex(String[] arr) {
        int k = 0;
        recLocation v = new recLocation(0, 0);
        int hashIndex = 0;
        int[] result = new int[2];

        this.index = new hashRow[arr.length];

        for (String string : arr) {
            String[] recArr = string.split(", ");
            k = Integer.parseInt(recArr[3]);
            hashIndex = hashFunction(k);
            result = getFileAndOffset(recArr[0]);
            v = new recLocation(result[0], result[1]);
            this.index[hashIndex] = new hashRow(k, v);

        }

    }

}

// class for array index
class arrayBasedIndex {
    int[][] arr; // array of file number and offset

    public int[][] getArr() {
        return this.arr;
    }

    public void setArr(int[][] arr) {
        this.arr = arr;
    }

    public arrayBasedIndex() {

    }

    public void printRecords(int v) {
        if (this.getArr()[v][0] != 0) {
            System.out.println(readRecord(this.getArr()[v][0], this.getArr()[v][1]));
        }

    }

    public String readRecord(int fNum, int offset) {
        File file = new File("./Project2Dataset/F" + fNum + ".txt");
        String datalines = "";
        try {
            File myObj = file;
            Scanner myReader = new Scanner(myObj);
            while (myReader.hasNextLine()) {
                datalines = myReader.nextLine();
            }
            myReader.close();
        } catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }

        return datalines.split("\\...")[offset - 1];
    }

    public int[] getFileAndOffset(String s) {
        int[] result = new int[2];

        result[0] = Integer.parseInt(s.substring(1, 3));
        result[1] = Integer.parseInt(s.substring(7, 10));

        return result;
    }

    public void buildIndex(String[] data) {
        int[][] arr = new int[5001][2];
        this.arr = arr;

        int k = 0;

        for (String string : data) {
            String[] recArr = string.split(", ");

            int[] val = getFileAndOffset(recArr[0]);
            k = Integer.parseInt(recArr[3]);
            this.arr[k][0] = val[0];
            this.arr[k][1] = val[1];

        }
    }

}
