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

  public static String newFileName(String inputFileName, String ext) {
    int dotIndex = inputFileName.lastIndexOf('.');
      
    String baseName;
    if (dotIndex > 0) {
        baseName = inputFileName.substring(0, dotIndex);
    } else {
        baseName = inputFileName;  // no extension found
    }

    return baseName + ext;
  }

  public static int setNewOutputFile(String inputFileName, String ext){
    //dynamically set output file based on inputfile
    try {
      PrintStream fileOut = new PrintStream(new File(newFileName(inputFileName, ext)));
      System.setOut(fileOut); // set stdout to output file

      return 1;//return success
    } catch (FileNotFoundException e) {
      System.err.println("Error: Cannot create or write to file '" + inputFileName + ext + "'");

      return 0;//stop parser return error
    }
  }

  static public void main(String argv[]) {   
    if (argv.length != 2) {
      System.err.println("Usage: java Main [-a] [-s] [-c] <inputfile>");
      return;
    }

    boolean doAst = "-a".equals(argv[0]);
    boolean doSemantic = "-s".equals(argv[0]);
    boolean doCodegen = "-c".equals(argv[0]);
    if (!doAst && !doSemantic && !doCodegen) {
      System.err.println("Unknown flag: " + argv[0]);
      System.err.println("Use -a, -s, or -c");
      return;
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
      if(setNewOutputFile(inputfile, ".abs") == 1){
        AbsynVisitor visitor = new ShowTreeVisitor();
        result.accept(visitor, 0);
      }
    }

    if (doSemantic && parser.valid && result != null) {
      if(setNewOutputFile(inputfile, ".sym") == 1){
        SemanticAnalyzer analyzer = new SemanticAnalyzer(System.out);
        analyzer.analyze(result);
      }
    }

    if (doCodegen && parser.valid && result != null) {
      PrintStream sink =
          new PrintStream(
              new OutputStream() {
                @Override
                public void write(int b) {
                }
              });
      SemanticAnalyzer analyzer = new SemanticAnalyzer(sink);
      analyzer.analyze(result);
      sink.close();
      if (!analyzer.hasErrors() && setNewOutputFile(inputfile, ".tm") == 1) {
        CodeGenerator generator = new CodeGenerator();
        generator.generate(result, System.out);
      }
    }
  }
}
