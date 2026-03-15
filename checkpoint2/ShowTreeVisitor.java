import absyn.*;

public class ShowTreeVisitor implements AbsynVisitor {

  final static int SPACES = 4;

  private void indent( int level ) {
    for( int i = 0; i < level * SPACES; i++ ) System.out.print( " " );
  }

  private String opName(int op) {
    switch (op) {
      case OpExp.BOOL:
        return "BOOL";
      case OpExp.ELSE:
        return "ELSE";
      case OpExp.IF:
        return "IF";
      case OpExp.INT:
        return "INT";
      case OpExp.RETURN:
        return "RETURN";
      case OpExp.VOID:
        return "VOID";
      case OpExp.WHILE:
        return "WHILE";
      case OpExp.TIMES:
        return "*";
      case OpExp.OVER:
        return "/";
      case OpExp.LT:
        return "<";
      case OpExp.GT:
        return ">";
      case OpExp.PLUS:
        return "+";
      case OpExp.MINUS:
        return "-";
      case OpExp.LTEQ:
        return "<=";
      case OpExp.GTEQ:
        return ">=";
      case OpExp.EQ:
        return "==";
      case OpExp.NEQ:
        return "!=";
      case OpExp.UMINUS:
        return "UNARY_MINUS";
      case OpExp.OR:
        return "||";
      case OpExp.AND:
        return "&&";
      case OpExp.ASSIGN:
        return "=";
      case OpExp.SEMI:
        return ";";
      case OpExp.COMMA:
        return ",";
      case OpExp.LCB:
        return "{";
      case OpExp.RCB:
        return "}";
      case OpExp.LSB:
        return "[]";
      case OpExp.RSB:
        return "RSB";
      case OpExp.LHB:
        return "CALL_OR_PARAMS";
      case OpExp.RHB:
        return "FUN_WITH_BODY";
      case OpExp.NUM:
        return "NUM";
      case OpExp.ID:
        return "ID";
      case OpExp.TRUTH:
        return "TRUTH";
      case OpExp.NOT:
        return "NOT";
      default:
        return "UNKNOWN(" + op + ")";
    }
  }

  public void visit( ExpList expList, int level ) {
    while( expList != null ) {
      expList.head.accept( this, level );
      expList = expList.tail;
    } 
  }

  public void visit( AssignExp exp, int level ) {
    indent( level );
    System.out.println( "AssignExp:" );
    level++;
    exp.lhs.accept( this, level );
    exp.rhs.accept( this, level );
  }

  public void visit( IfExp exp, int level ) {
    indent( level );
    System.out.println( "IfExp:" );
    level++;
    exp.test.accept( this, level );
    exp.thenpart.accept( this, level );
    if (exp.elsepart != null )
       exp.elsepart.accept( this, level );
  }

  public void visit( IntExp exp, int level ) {
    indent( level );
    System.out.println( "IntExp: " + exp.value ); 
  }

  public void visit( OpExp exp, int level ) {
    indent( level );
    System.out.println( "OpExp: " + opName(exp.op) );
    level++;
    if (exp.left != null)
       exp.left.accept( this, level );
    if (exp.right != null)
       exp.right.accept( this, level );
  }

  public void visit( ReadExp exp, int level ) {
    indent( level );
    System.out.println( "ReadExp:" );
    exp.input.accept( this, ++level );
  }

  public void visit( RepeatExp exp, int level ) {
    indent( level );
    System.out.println( "RepeatExp:" );
    level++;
    if (exp.exps != null) {
      exp.exps.accept( this, level );
    }
    if (exp.test != null) {
      exp.test.accept( this, level );
    }
  }

  public void visit( VarExp exp, int level ) {
    indent( level );
    System.out.println( "VarExp: " + exp.name );
  }

  public void visit( WriteExp exp, int level ) {
    indent( level );
    System.out.println( "WriteExp:" );
    if (exp.output != null)
       exp.output.accept( this, ++level );
  }

  public void visit(TypeSpec exp, int level) {
    indent(level);
    System.out.println("TypeSpec: " + exp.name);
  }

  public void visit(VarDeclExp exp, int level) {
    indent(level);
    System.out.println("VarDeclExp: " + exp.name);
    level++;
    if (exp.type != null) {
      exp.type.accept(this, level);
    }
    if (exp.size != null) {
      indent(level);
      System.out.println("ArraySize:");
      exp.size.accept(this, level + 1);
    }
  }

  public void visit(ParamExp exp, int level) {
    indent(level);
    System.out.println("ParamExp: " + exp.name + (exp.isArray ? "[]" : ""));
    if (exp.type != null) {
      exp.type.accept(this, level + 1);
    }
  }

  public void visit(FunDeclExp exp, int level) {
    indent(level);
    System.out.println("FunDeclExp: " + exp.name);
    level++;
    if (exp.resultType != null) {
      indent(level);
      System.out.println("ResultType:");
      exp.resultType.accept(this, level + 1);
    }
    if (exp.params != null) {
      indent(level);
      System.out.println("Params:");
      exp.params.accept(this, level + 1);
    }
    if (exp.body != null) {
      indent(level);
      System.out.println("Body:");
      exp.body.accept(this, level + 1);
    } else {
      indent(level);
      System.out.println("PrototypeOnly");
    }
  }

  public void visit(CompoundExp exp, int level) {
    indent(level);
    System.out.println("CompoundExp:");
    if (exp.localDecls != null) {
      indent(level + 1);
      System.out.println("LocalDecls:");
      exp.localDecls.accept(this, level + 2);
    }
    if (exp.statements != null) {
      indent(level + 1);
      System.out.println("Statements:");
      exp.statements.accept(this, level + 2);
    }
  }

  public void visit(WhileExp exp, int level) {
    indent(level);
    System.out.println("WhileExp:");
    if (exp.test != null) {
      indent(level + 1);
      System.out.println("Condition:");
      exp.test.accept(this, level + 2);
    }
    if (exp.body != null) {
      indent(level + 1);
      System.out.println("Body:");
      exp.body.accept(this, level + 2);
    }
  }

  public void visit(ReturnExp exp, int level) {
    indent(level);
    System.out.println("ReturnExp:");
    if (exp.value != null) {
      exp.value.accept(this, level + 1);
    }
  }

  public void visit(CallExp exp, int level) {
    indent(level);
    System.out.println("CallExp: " + exp.function);
    if (exp.args != null) {
      exp.args.accept(this, level + 1);
    }
  }

  public void visit(IndexVarExp exp, int level) {
    indent(level);
    System.out.println("IndexVarExp: " + exp.name);
    if (exp.index != null) {
      exp.index.accept(this, level + 1);
    }
  }

  public void visit(BoolExp exp, int level) {
    indent(level);
    System.out.println("BoolExp: " + exp.value);
  }

  public void visit(NilExp exp, int level) {
    indent(level);
    System.out.println("NilExp");
  }

}
