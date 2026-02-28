JAVA=java
JAVAC=javac
#JFLEX=jflex
JFLEX=/jflex-1.9.1/bin/jflex.bat
#CLASSPATH=-cp /usr/share/java/cup.jar:.
CLASSPATH=-cp /java-cup-bin-11b-20160615/java-cup-11b.jar:.
#CUP=cup
JFLEX=~/Projects/jflex/bin/jflex
CLASSPATH=-cp ~/Projects/java-cup-11b.jar:.
CUP=$(JAVA) $(CLASSPATH) java_cup.Main

all: Main.class

Main.class: absyn/*.java parser.java sym.java Lexer.java ShowTreeVisitor.java Scanner.java Main.java

%.class: %.java
	$(JAVAC) $(CLASSPATH) $^

Lexer.java: cminus.flex
	$(JFLEX) cminus.flex

parser.java: tiny.cup
	#$(CUP) -dump -expect 3 tiny.cup
	$(CUP) -expect 3 tiny.cup

clean:
	rm -f parser.java Lexer.java sym.java *.class absyn/*.class *~
