/*
  Created By: Fei Song, Silas Wright
  File Name: cminus.flex
  To Build: jflex cminus.flex

  and then after the parser is created
    javac Lexer.java
*/
   
/* --------------------------Usercode Section------------------------ */
   
import java_cup.runtime.*;
      
%%
   
/* -----------------Options and Declarations Section----------------- */
   
/* 
   The name of the class JFlex will create will be Lexer.
   Will write the code to the file Lexer.java. 
*/
%class Lexer

%eofval{
  return null;
%eofval};

/*
  The current line number can be accessed with the variable yyline
  and the current column number with the variable yycolumn.
*/
%line
%column
    
/* 
   Will switch to a CUP compatibility mode to interface with a CUP
   generated parser.
*/
%cup
   
/*
  Declarations
   
  Code between %{ and %}, both of which must be at the beginning of a
  line, will be copied letter to letter into the lexer class source.
  Here you declare member variables and functions that are used inside
  scanner actions.  
*/
%{   
    /* To create a new java_cup.runtime.Symbol with information about
       the current token, the token will have no value in this
       case. */
    private Symbol symbol(int type) {
        return new Symbol(type, yyline, yycolumn);
    }
    
    /* Also creates a new java_cup.runtime.Symbol with information
       about the current token, but this object has a value. */
    private Symbol symbol(int type, Object value) {
        return new Symbol(type, yyline, yycolumn, value);
    }
%}
   

/*
  Macro Declarations
  
  These declarations are regular expressions that will be used latter
  in the Lexical Rules Section.  
*/
   
/* A line terminator is a \r (carriage return), \n (line feed), or
   \r\n. */
LineTerminator = \r|\n|\r\n
   
/* White space is a line terminator, space, tab, or form feed. */
WhiteSpace     = {LineTerminator} | [ \t\f]
   
/*  */
NUM = [0-9]+
   
/*  */
ID = [_a-zA-Z][_a-zA-Z0-9]*

/*  */
TRUTH = true|false
   
%%
/* ------------------------Lexical Rules Section---------------------- */
   
/*
   This section contains regular expressions and actions, i.e. Java
   code, that will be executed when the scanner matches the associated
   regular expression. */
   
"bool"                           { System.out.print("bool"); return symbol(sym.BOOL); }
"else"                           { System.out.print("else"); return symbol(sym.ELSE); }
"if"                             { System.out.print("if"); return symbol(sym.IF); }
"int"                            { System.out.print("int"); return symbol(sym.INT); }
"return"                         { System.out.print("return"); return symbol(sym.RETURN); }
"void"                           { System.out.print("void"); return symbol(sym.VOID); }
"while"                          { System.out.print("while"); return symbol(sym.WHILE); }
"*"                              { System.out.print("times"); return symbol(sym.TIMES); }
"/"                              { System.out.print("over"); return symbol(sym.OVER); }
"<"                              { System.out.print("lt"); return symbol(sym.LT); }
">"                              { System.out.print("gt"); return symbol(sym.GT); }
"+"                              { System.out.print("plus"); return symbol(sym.PLUS); }
"-"                              { System.out.print("minus"); return symbol(sym.MINUS); }
">="                             { System.out.print(">="); return symbol(sym.GTEQ); }
"<="                             { System.out.print("<="); return symbol(sym.LTEQ); }
"=="                             { System.out.print("=="); return symbol(sym.EQ); }
"!="                             { System.out.print("!="); return symbol(sym.NEQ); }
"||"                             { System.out.print("||"); return symbol(sym.OR); }
"&&"                             { System.out.print("&&"); return symbol(sym.AND); }
"="                              { System.out.print("="); return symbol(sym.ASSIGN); }
";"                              { System.out.print(";"); return symbol(sym.SEMI); }
","                              { System.out.print(","); return symbol(sym.COMMA); }
"{"                              { System.out.print("{"); return symbol(sym.LBRACE); }
"}"                              { System.out.print("}"); return symbol(sym.RBRACE); }
"["                              { System.out.print("["); return symbol(sym.LBRACK); }
"]"                              { System.out.print("]"); return symbol(sym.RBRACK); }
"("                              { System.out.print("("); return symbol(sym.LPAREN); }
")"                              { System.out.print(")"); return symbol(sym.RPAREN); }
{NUM}                            { System.out.print("NUM"); return symbol(sym.NUM, yytext()); }
{TRUTH}                          { System.out.print("TRUTH"); return symbol(sym.TRUTH, yytext()); }
{ID}                             { System.out.print("ID"); return symbol(sym.ID, yytext()); }
{WhiteSpace}+                    { /* skip whitespace */ }   
"/*"([^*]|\*+[^*/])*\*+"/"       { /* skip comments */ }
.                                { return symbol(sym.ERROR); }
