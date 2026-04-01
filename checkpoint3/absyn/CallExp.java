package absyn;

public class CallExp extends Exp {
  public String function;
  public ExpList args;

  public CallExp(int row, int col, String function, ExpList args) {
    this.row = row;
    this.col = col;
    this.function = function;
    this.args = args;
  }

  public void accept(AbsynVisitor visitor, int level) {
    visitor.visit(this, level);
  }
}
