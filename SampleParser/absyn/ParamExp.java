package absyn;

public class ParamExp extends Exp {
  public TypeSpec type;
  public String name;
  public boolean isArray;

  public ParamExp(int row, int col, TypeSpec type, String name, boolean isArray) {
    this.row = row;
    this.col = col;
    this.type = type;
    this.name = name;
    this.isArray = isArray;
  }

  public void accept(AbsynVisitor visitor, int level) {
    visitor.visit(this, level);
  }
}
