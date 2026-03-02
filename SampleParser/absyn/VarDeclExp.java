package absyn;

public class VarDeclExp extends Exp {
  public TypeSpec type;
  public String name;
  public Exp size;

  public VarDeclExp(int row, int col, TypeSpec type, String name, Exp size) {
    this.row = row;
    this.col = col;
    this.type = type;
    this.name = name;
    this.size = size;
  }

  public void accept(AbsynVisitor visitor, int level) {
    visitor.visit(this, level);
  }
}
