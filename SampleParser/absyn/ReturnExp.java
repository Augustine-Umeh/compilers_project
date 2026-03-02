package absyn;

public class ReturnExp extends Exp {
  public Exp value;

  public ReturnExp(int row, int col, Exp value) {
    this.row = row;
    this.col = col;
    this.value = value;
  }

  public void accept(AbsynVisitor visitor, int level) {
    visitor.visit(this, level);
  }
}
