/*
  Created by: Fei Song, Silas Wright
  File Name: Main.java
  To Build: 
  After the Scanner.java, tiny.flex, and tiny.cup have been processed, do:
    javac Main.java
  
  To Run: 
    java -classpath /usr/share/java/cup.jar:. Main gcd.tiny

  where gcd.tiny is an test input file for the tiny language.
*/
   
import java.io.*;
import absyn.*;
   
class Main {
  public final static boolean SHOW_TREE = true;

  public static String toAbsFileName(String inputFileName) {
      int dotIndex = inputFileName.lastIndexOf('.');
      
      String baseName;
      if (dotIndex > 0) {
          baseName = inputFileName.substring(0, dotIndex);
      } else {
          baseName = inputFileName;  // no extension found
      }

      System.out.println(baseName + ".abs");

      return baseName + ".abs";
  }
  static public void main(String argv[]) {   

    if (argv.length < 2) {
      System.err.println("Usage: java Main <-a|-s|-c> <inputfile>");
      return;
    }

    String flag = argv[0];  // take flag
    String inputfile = argv[1]; // store filename

    //dynamically set output file based on inputfile
    try {
      PrintStream fileOut = new PrintStream(new File(toAbsFileName(inputfile)));
      System.setOut(fileOut); // set stdout to output file
  } catch (FileNotFoundException e) {
      System.err.println("Error: Cannot create or write to file '" + inputfile + "'");
  }
    if (flag.equals("-a")) {
        /* Start the parser */
        try {
          parser p = new parser(new Lexer(new FileReader(inputfile)));
          Absyn result = (Absyn)(p.parse().value);      
          if (SHOW_TREE && result != null) {
            System.out.println("The abstract syntax tree is:");
            AbsynVisitor visitor = new ShowTreeVisitor();
            result.accept(visitor, 0); 
          }
        } catch (Exception e) {
          /* do cleanup here -- possibly rethrow e */
          e.printStackTrace();
        }
    }
    else if (flag.equals("-s")) {
        //checkpoint 2
    }
    else if (flag.equals("-c")) {
        //checkpoint 3
    }
    else {
        System.err.println("Unknown flag: " + flag);
        System.err.println("Use -a, -s, or -c");
    }
  }
}