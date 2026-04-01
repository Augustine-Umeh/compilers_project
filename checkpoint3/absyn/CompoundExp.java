package absyn;

public class CompoundExp extends Exp {
  public ExpList localDecls;
  public ExpList statements;

  public CompoundExp(int row, int col, ExpList localDecls, ExpList statements) {
    this.row = row;
    this.col = col;
    this.localDecls = localDecls;
    this.statements = statements;
  }

  public void accept(AbsynVisitor visitor, int level) {
    visitor.visit(this, level);
  }
}
