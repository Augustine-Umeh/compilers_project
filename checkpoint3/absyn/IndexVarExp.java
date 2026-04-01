package absyn;

public class IndexVarExp extends Exp {
  public String name;
  public Exp index;

  public IndexVarExp(int row, int col, String name, Exp index) {
    this.row = row;
    this.col = col;
    this.name = name;
    this.index = index;
  }

  public void accept(AbsynVisitor visitor, int level) {
    visitor.visit(this, level);
  }
}
