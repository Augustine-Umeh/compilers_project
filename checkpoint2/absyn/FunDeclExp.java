package absyn;

public class FunDeclExp extends Exp {
  public TypeSpec resultType;
  public String name;
  public ExpList params;
  public CompoundExp body;

  public FunDeclExp(int row, int col, TypeSpec resultType, String name, ExpList params, CompoundExp body) {
    this.row = row;
    this.col = col;
    this.resultType = resultType;
    this.name = name;
    this.params = params;
    this.body = body;
  }

  public void accept(AbsynVisitor visitor, int level) {
    visitor.visit(this, level);
  }
}
