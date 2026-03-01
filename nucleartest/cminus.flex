/*
  Created By: Fei Song, Silas Wright
  File Name: cminus.flex
  To Build: jflex cminus.flex

  and then after the parser is created
    javac Lexer.java
*/

%%

%class Lexer
%type Token

%{
  /* Additional code can go here */
%}

%line
%column

%eofval{
  return null;
%eofval}

LineTerminator = \r|\n|\r\n
WhiteSpace     = {LineTerminator} | [ \t\f]
NUM = [0-9]+
ID = [_a-zA-Z][_a-zA-Z0-9]*
TRUTH = true|false

%%

"bool"      { System.out.println("bool"); }
"else"      { System.out.println("else"); }
"if"        { System.out.println("if"); }
"int"       { System.out.println("int"); }
"return"    { System.out.println("return"); }
"void"      { System.out.println("void"); }
"while"     { System.out.println("while"); }

"*"         { System.out.println("times"); }
"/"         { System.out.println("over"); }
"<"         { System.out.println("lt"); }
">"         { System.out.println("gt"); }
"+"         { System.out.println("plus"); }
"-"         { System.out.println("minus"); }
">="        { System.out.println(">="); }
"<="        { System.out.println("<="); }
"=="        { System.out.println("=="); }
"!="        { System.out.println("!="); }
"~"         { System.out.println("~"); }
"||"        { System.out.println("||"); }
"&&"        { System.out.println("&&"); }
"="         { System.out.println("="); }
";"         { System.out.println(";"); }
","         { System.out.println(","); }
"{"         { System.out.println("LCB"); }
"}"         { System.out.println("RCB"); }
"["         { System.out.println("LSB"); }
"]"         { System.out.println("RSB"); }
"("         { System.out.println("LHB"); }
")"         { System.out.println("RHB"); }

{NUM}       { System.out.println("NUM(" + yytext() + ")"); }
{TRUTH}     { System.out.println("TRUTH(" + yytext() + ")"); }
{ID}        { System.out.println("ID(" + yytext() + ")"); }

{WhiteSpace}+ { /* skip whitespace */ }

"/*"([^*]|\*+[^*/])*\*+"/" { /* skip comments */ }

.           { System.out.println("ERROR: Illegal character '" + yytext() + "'"); }