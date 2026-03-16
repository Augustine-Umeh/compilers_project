package absyn;

abstract public class Exp extends Absyn {
  // Resolved data type (set during semantic analysis): 0=INT, 1=BOOL, 2=VOID.
  public int dtype = -1;
}
