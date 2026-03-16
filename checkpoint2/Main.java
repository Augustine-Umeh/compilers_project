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

      return baseName + ".abs";
  }
  public static String toSymFileName(String inputFileName) {
      int dotIndex = inputFileName.lastIndexOf('.');

      String baseName;
      if (dotIndex > 0) {
          baseName = inputFileName.substring(0, dotIndex);
      } else {
          baseName = inputFileName;  // no extension found
      }

      return baseName + ".sym";
  }
  static public void main(String argv[]) {   
    if (argv.length < 2) {
      System.err.println("Usage: java Main [-a] [-s] [-c] <inputfile>");
      return;
    }

    boolean doAst = false;
    boolean doSemantic = false;
    boolean doCodegen = false;

    for (int i = 0; i < argv.length - 1; i++) {
      if ("-a".equals(argv[i])) {
        doAst = true;
      } else if ("-s".equals(argv[i])) {
        doSemantic = true;
      } else if ("-c".equals(argv[i])) {
        doCodegen = true;
      } else {
        System.err.println("Unknown flag: " + argv[i]);
        System.err.println("Use -a, -s, or -c");
        return;
      }
    }

    String inputfile = argv[argv.length - 1];

    parser.valid = true;
    Absyn result = null;
    try {
      parser p = new parser(new Lexer(new FileReader(inputfile)));
      result = (Absyn) (p.parse().value);
    } catch (Exception e) {
      e.printStackTrace();
      return;
    }

    if (doAst && SHOW_TREE && result != null) {
      PrintStream originalOut = System.out;
      try (PrintStream absOut = new PrintStream(new File(toAbsFileName(inputfile)))) {
        System.setOut(absOut);
        System.out.println("The abstract syntax tree is:");
        AbsynVisitor visitor = new ShowTreeVisitor();
        result.accept(visitor, 0);
      } catch (FileNotFoundException e) {
        System.err.println("Error: Cannot create or write to file '" + toAbsFileName(inputfile) + "'");
      } finally {
        System.setOut(originalOut);
      }
    }

    if (doSemantic && parser.valid && result != null) {
      try (PrintStream symOut = new PrintStream(new File(toSymFileName(inputfile)))) {
        SemanticAnalyzer analyzer = new SemanticAnalyzer(symOut);
        analyzer.analyze(result);
      } catch (FileNotFoundException e) {
        System.err.println("Error: Cannot create or write to file '" + toSymFileName(inputfile) + "'");
      }
    }

    if (doCodegen) {
      // checkpoint 3
    }
  }
}