JAVA=java
JAVAC=javac
#JFLEX=/jflex-1.9.1/bin/jflex.bat
#CLASSPATH=-cp /java-cup-bin-11b-20160615/java-cup-11b.jar:.
JFLEX=java -jar C:\Users\siwri\Desktop\Uni\compilers\C1\C1-Package\compilers_project\nucleartest\jflex-1.9.1\jflex-1.9.1\lib\jflex-full-1.9.1.jar
CLASSPATH=-cp .;C:\Users\siwri\Desktop\Uni\compilers\C1\C1-Package\compilers_project\nucleartest\java-cup-bin-11b-20160615\java-cup-11b.jar

CUP=$(JAVA) $(CLASSPATH) java_cup.Main

#all: Main.class
#all: old_scanner_for_testing.java
all: Token.class Lexer.class ScannerII.class

Main.class: absyn/*.java parser.java sym.java Lexer.java ShowTreeVisitor.java Scanner.java Main.java

%.class: %.java
	$(JAVAC) $(CLASSPATH) $^

Lexer.java: cminus.flex
	$(JFLEX) cminus.flex

parser.java: tiny.cup
	#$(CUP) -dump -expect 3 tiny.cup
	$(CUP) -expect 3 tiny.cup

clean:
	rm -f Lexer.java *.class
