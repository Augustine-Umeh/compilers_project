package absyn;

public class OpExp extends Exp {
  public final static int BOOL   = 0;
  public final static int ELSE  = 1;
  public final static int IF  = 2;
  public final static int INT   = 3;
  public final static int RETURN     = 4;
  public final static int VOID     = 5;
  public final static int WHILE     = 6;
  public final static int TIMES = 7;
  public final static int OVER = 8;
  public final static int LT = 9;
  public final static int GT = 10;
  public final static int PLUS = 11;
  public final static int MINUS = 12;
  public final static int LTEQ = 13;
  public final static int GTEQ = 14;
  public final static int EQ = 15;
  public final static int NEQ = 16;
  public final static int UMINUS = 17;
  public final static int OR = 18;
  public final static int AND = 19;
  public final static int ASSIGN = 20;
  public final static int SEMI = 21;
  public final static int COMMA = 22;
  public final static int LCB = 23;
  public final static int RCB = 24;
  public final static int LSB = 25;
  public final static int RSB = 26;
  public final static int LHB = 27;
  public final static int RHB = 28;
  public final static int NUM = 29;
  public final static int ID = 30;
  public final static int TRUTH = 31;

  public Exp left;
  public int op;
  public Exp right;

  public OpExp( int row, int col, Exp left, int op, Exp right ) {
    this.row = row;
    this.col = col;
    this.left = left;
    this.op = op;
    this.right = right;
  }

  public void accept( AbsynVisitor visitor, int level ) {
    visitor.visit( this, level );
  }
}
