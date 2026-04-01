import java.io.PrintStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import absyn.*;

/**
 * Semantic analysis pass:
 * - Builds/checks symbols with scoped lookup.
 * - Resolves expression types into Exp.dtype.
 * - Reports semantic errors without aborting traversal.
 */
public class SemanticAnalyzer implements AbsynVisitor {
  private static class FunctionRecord {
    final SymbolTable.SymbolEntry entry;
    boolean hasDefinition;

    FunctionRecord(SymbolTable.SymbolEntry entry, boolean hasDefinition) {
      this.entry = entry;
      this.hasDefinition = hasDefinition;
    }
  }

  private final SymbolTable symbolTable;
  private final PrintStream errors;
  private final LinkedHashMap<String, FunctionRecord> functions;

  private boolean initialized;
  private boolean hadErrors;
  private int scopeLevel;
  private int currentFunctionReturnType;
  private static final int NO_FUNCTION = -100;
  private boolean insideCallArgs;
  private Exp lastTopLevelDeclaration;

  public SemanticAnalyzer() {
    this(System.out, System.err);
  }

  public SemanticAnalyzer(PrintStream symbolOutput) {
    this(symbolOutput, System.err);
  }

  public SemanticAnalyzer(PrintStream symbolOutput, PrintStream errorOutput) {
    this.symbolTable = new SymbolTable(symbolOutput);
    this.errors = errorOutput;
    this.functions = new LinkedHashMap<String, FunctionRecord>();
    this.initialized = false;
    this.hadErrors = false;
    this.scopeLevel = 0;
    this.currentFunctionReturnType = NO_FUNCTION;
    this.insideCallArgs = false;
    this.lastTopLevelDeclaration = null;
  }

  public boolean hasErrors() {
    return hadErrors;
  }

  public void analyze(Absyn tree) {
    if (tree != null) {
      tree.accept(this, 0);
    }
  }

  private void initializeIfNeeded() {
    if (initialized) {
      return;
    }

    initialized = true;
    symbolTable.enterScope("global");
    scopeLevel = 0;

    // Built-ins:
    // input(void) -> int
    SymbolTable.SymbolEntry inputFn =
        new SymbolTable.SymbolEntry(
            "input",
            SymbolTable.INT,
            false,
            -1,
            true,
            new ArrayList<SymbolTable.SymbolEntry>(),
            0,
            0);
    inputFn.isBuiltin = true;
    symbolTable.insert("input", inputFn);
    functions.put("input", new FunctionRecord(inputFn, true));

    // output(int) -> void
    ArrayList<SymbolTable.SymbolEntry> outputParams = new ArrayList<SymbolTable.SymbolEntry>();
    outputParams.add(
        new SymbolTable.SymbolEntry(
            "x",
            SymbolTable.INT,
            false,
            -1,
            false,
            null,
            0,
            0));
    SymbolTable.SymbolEntry outputFn =
        new SymbolTable.SymbolEntry(
            "output",
            SymbolTable.VOID,
            false,
            -1,
            true,
            outputParams,
            0,
            0);
    outputFn.isBuiltin = true;
    symbolTable.insert("output", outputFn);
    functions.put("output", new FunctionRecord(outputFn, true));
  }

  private void error(int row, int col, String message) {
    hadErrors = true;
    int printableRow = row >= 0 ? row + 1 : row;
    int printableCol = col >= 0 ? col + 1 : col;
    errors.println("Error at " + printableRow + ":" + printableCol + ": " + message);
  }

  private int typeFromTypeSpec(TypeSpec spec) {
    if (spec == null || spec.name == null) {
      return SymbolTable.VOID;
    }
    if ("int".equals(spec.name)) {
      return SymbolTable.INT;
    }
    if ("bool".equals(spec.name)) {
      return SymbolTable.BOOL;
    }
    return SymbolTable.VOID;
  }

  private boolean isConditionType(int type) {
    return type == SymbolTable.INT || type == SymbolTable.BOOL;
  }

  private boolean isKnownType(int type) {
    return type == SymbolTable.INT || type == SymbolTable.BOOL || type == SymbolTable.VOID;
  }

  private int nodeType(Exp exp) {
    if (exp == null) {
      return SymbolTable.VOID;
    }
    if (!isKnownType(exp.dtype)) {
      return SymbolTable.VOID;
    }
    return exp.dtype;
  }

  private int parseArraySize(Exp size) {
    if (size instanceof IntExp) {
      try {
        return Integer.parseInt(((IntExp) size).value);
      } catch (NumberFormatException ignored) {
        return -1;
      }
    }
    return -1;
  }

  private int listLength(ExpList list) {
    int len = 0;
    ExpList cursor = list;
    while (cursor != null) {
      len++;
      cursor = cursor.tail;
    }
    return len;
  }

  private ArrayList<SymbolTable.SymbolEntry> buildParamEntries(ExpList params) {
    ArrayList<SymbolTable.SymbolEntry> entries = new ArrayList<SymbolTable.SymbolEntry>();
    ExpList cursor = params;
    while (cursor != null) {
      if (cursor.head instanceof ParamExp) {
        ParamExp param = (ParamExp) cursor.head;
        int paramType = typeFromTypeSpec(param.type);
        if (paramType == SymbolTable.VOID) {
          error(param.row, param.col, "parameter '" + param.name + "' cannot have type void");
          paramType = SymbolTable.INT;
        }
        entries.add(
            new SymbolTable.SymbolEntry(
                param.name,
                paramType,
                param.isArray,
                param.isArray ? -1 : 0,
                false,
                null,
                param.row,
                param.col));
      }
      cursor = cursor.tail;
    }
    return entries;
  }

  private boolean expressionRepresentsArray(Exp exp) {
    if (exp instanceof VarExp) {
      SymbolTable.SymbolEntry entry = symbolTable.lookup(((VarExp) exp).name);
      return entry != null && entry.isArray;
    }
    return false;
  }

  private void checkArgumentTypes(CallExp call, SymbolTable.SymbolEntry functionEntry) {
    int expectedCount = functionEntry.params.size();
    int actualCount = listLength(call.args);
    if (expectedCount != actualCount) {
      error(
          call.row,
          call.col,
          "function '" + call.function + "' expects "
              + expectedCount
              + " argument(s), got "
              + actualCount);
      return;
    }

    ExpList actualArg = call.args;
    for (int i = 0; i < expectedCount; i++) {
      SymbolTable.SymbolEntry expected = functionEntry.params.get(i);
      Exp actual = actualArg.head;
      int actualType = nodeType(actual);
      boolean actualIsArray = expressionRepresentsArray(actual);

      if (actualType != expected.type) {
        error(
            actual.row,
            actual.col,
            "argument "
                + (i + 1)
                + " of function '"
                + call.function
                + "' has wrong type");
      } else if (actualIsArray != expected.isArray) {
        error(
            actual.row,
            actual.col,
            "argument "
                + (i + 1)
                + " of function '"
                + call.function
                + "' has wrong array/scalar form");
      }

      actualArg = actualArg.tail;
    }
  }

  private boolean isComparisonOperator(int op) {
    return op == OpExp.LT
        || op == OpExp.LTEQ
        || op == OpExp.GT
        || op == OpExp.GTEQ
        || op == OpExp.EQ
        || op == OpExp.NEQ;
  }

  private boolean sameSignature(
      SymbolTable.SymbolEntry existing, int returnType, ArrayList<SymbolTable.SymbolEntry> params) {
    if (existing.type != returnType || existing.params.size() != params.size()) {
      return false;
    }
    for (int i = 0; i < params.size(); i++) {
      SymbolTable.SymbolEntry a = existing.params.get(i);
      SymbolTable.SymbolEntry b = params.get(i);
      if (a.type != b.type || a.isArray != b.isArray) {
        return false;
      }
    }
    return true;
  }

  private void analyzeCompoundBody(CompoundExp exp, int level, boolean createScope, String scopeName) {
    if (createScope) {
      symbolTable.enterScope(scopeName == null ? "block" : scopeName);
      scopeLevel++;
    }

    if (exp.localDecls != null) {
      exp.localDecls.accept(this, level + 1);
    }
    if (exp.statements != null) {
      exp.statements.accept(this, level + 1);
    }

    if (createScope) {
      symbolTable.printScope(scopeLevel);
      symbolTable.leaveScope();
      scopeLevel--;
    }
    exp.dtype = SymbolTable.VOID;
  }

  private void visitWithBlockContext(Exp exp, int level, String blockContext) {
    if (exp == null) {
      return;
    }
    if (exp instanceof CompoundExp) {
      analyzeCompoundBody((CompoundExp) exp, level, true, "block " + blockContext);
    } else {
      exp.accept(this, level);
    }
  }

  private void visitListWithBlockContext(ExpList list, int level, String blockContext) {
    if (list == null) {
      return;
    }
    if (list.head instanceof CompoundExp && list.tail == null) {
      analyzeCompoundBody((CompoundExp) list.head, level, true, "block " + blockContext);
    } else {
      list.accept(this, level);
    }
  }

  @Override
  public void visit(ExpList expList, int level) {
    initializeIfNeeded();
    ExpList cursor = expList;
    while (cursor != null) {
      if (level == 0) {
        lastTopLevelDeclaration = cursor.head;
      }
      if (cursor.head != null) {
        cursor.head.accept(this, level);
      }
      cursor = cursor.tail;
    }

    if (level == 0) {
      SymbolTable.SymbolEntry mainEntry = symbolTable.lookup("main");
      FunctionRecord mainRecord = functions.get("main");
      if (mainEntry == null || !mainEntry.isFunction || mainRecord == null || !mainRecord.hasDefinition) {
        error(-1, -1, "program must define a 'void main(void)' function");
      } else {
        if (mainEntry.type != SymbolTable.VOID || mainEntry.params.size() != 0) {
          error(mainEntry.row, mainEntry.col, "'main' must have signature void main(void)");
        }
        if (!(lastTopLevelDeclaration instanceof FunDeclExp)
            || !"main".equals(((FunDeclExp) lastTopLevelDeclaration).name)
            || ((FunDeclExp) lastTopLevelDeclaration).body == null) {
          error(mainEntry.row, mainEntry.col, "'main' must be the last declaration in the program");
        }
      }

      for (Map.Entry<String, FunctionRecord> item : functions.entrySet()) {
        FunctionRecord function = item.getValue();
        if (!function.entry.isBuiltin && !function.hasDefinition) {
          error(function.entry.row, function.entry.col,
              "function '" + function.entry.name + "' declared but never defined");
        }
      }
      symbolTable.printScope(0);
      symbolTable.leaveScope();
    }
  }

  @Override
  public void visit(AssignExp exp, int level) {
    if (exp.lhs != null) {
      exp.lhs.accept(this, level + 1);
    }
    if (exp.rhs != null) {
      exp.rhs.accept(this, level + 1);
    }

    int lhsType = nodeType(exp.lhs);
    int rhsType = nodeType(exp.rhs);
    if (lhsType == SymbolTable.VOID || rhsType == SymbolTable.VOID || lhsType != rhsType) {
      error(exp.row, exp.col, "type mismatch in assignment");
      exp.dtype = (lhsType != SymbolTable.VOID) ? lhsType : rhsType;
      return;
    }
    exp.dtype = lhsType;
  }

  @Override
  public void visit(IfExp exp, int level) {
    if (exp.test != null) {
      exp.test.accept(this, level + 1);
      int conditionType = nodeType(exp.test);
      if (!isConditionType(conditionType)) {
        error(exp.row, exp.col, "if condition must be int or bool");
      }
    }
    visitListWithBlockContext(exp.thenpart, level + 1, "if");
    visitListWithBlockContext(exp.elsepart, level + 1, "else");
    exp.dtype = SymbolTable.VOID;
  }

  @Override
  public void visit(IntExp exp, int level) {
    exp.dtype = SymbolTable.INT;
  }

  @Override
  public void visit(OpExp exp, int level) {
    if (exp.left != null) {
      exp.left.accept(this, level + 1);
    }
    if (exp.right != null) {
      exp.right.accept(this, level + 1);
    }

    int leftType = nodeType(exp.left);
    int rightType = nodeType(exp.right);

    switch (exp.op) {
      case OpExp.ASSIGN:
        if (!(exp.left instanceof VarExp || exp.left instanceof IndexVarExp)) {
          error(exp.row, exp.col, "left side of assignment must be a variable");
          exp.dtype = (leftType != SymbolTable.VOID) ? leftType : rightType;
          return;
        }
        if (leftType == SymbolTable.VOID || rightType == SymbolTable.VOID || leftType != rightType) {
          error(exp.row, exp.col, "type mismatch in assignment");
          exp.dtype = (leftType != SymbolTable.VOID) ? leftType : rightType;
        } else {
          exp.dtype = leftType;
        }
        return;

      case OpExp.PLUS:
      case OpExp.MINUS:
      case OpExp.TIMES:
      case OpExp.OVER:
        if (leftType != SymbolTable.INT || rightType != SymbolTable.INT) {
          error(exp.row, exp.col, "arithmetic operators require int operands");
        }
        exp.dtype = SymbolTable.INT;
        return;

      case OpExp.UMINUS:
        if (rightType != SymbolTable.INT) {
          error(exp.row, exp.col, "unary minus requires int operand");
        }
        exp.dtype = SymbolTable.INT;
        return;

      case OpExp.AND:
      case OpExp.OR:
        if (leftType != SymbolTable.BOOL || rightType != SymbolTable.BOOL) {
          error(exp.row, exp.col, "logical operators require bool operands");
        }
        exp.dtype = SymbolTable.BOOL;
        return;

      case OpExp.NOT:
        if (rightType != SymbolTable.BOOL) {
          error(exp.row, exp.col, "logical not requires bool operand");
        }
        exp.dtype = SymbolTable.BOOL;
        return;

      default:
        if (isComparisonOperator(exp.op)) {
          if (leftType == SymbolTable.VOID || rightType == SymbolTable.VOID || leftType != rightType) {
            error(exp.row, exp.col, "comparison requires two operands of the same non-void type");
          }
          exp.dtype = SymbolTable.BOOL;
        } else if (exp.op == OpExp.COMMA) {
          exp.dtype = rightType;
        } else {
          exp.dtype = SymbolTable.VOID;
        }
    }
  }

  @Override
  public void visit(ReadExp exp, int level) {
    if (exp.input != null) {
      exp.input.accept(this, level + 1);
      if (nodeType(exp.input) == SymbolTable.VOID) {
        error(exp.row, exp.col, "read target must have non-void type");
      }
    }
    exp.dtype = SymbolTable.VOID;
  }

  @Override
  public void visit(RepeatExp exp, int level) {
    if (exp.exps != null && exp.exps.head instanceof CompoundExp && exp.exps.tail == null) {
      analyzeCompoundBody((CompoundExp) exp.exps.head, level + 1, true, "block repeat");
    } else if (exp.exps != null) {
      exp.exps.accept(this, level + 1);
    }
    if (exp.test != null) {
      exp.test.accept(this, level + 1);
      if (!isConditionType(nodeType(exp.test))) {
        error(exp.row, exp.col, "repeat condition must be int or bool");
      }
    }
    exp.dtype = SymbolTable.VOID;
  }

  @Override
  public void visit(VarExp exp, int level) {
    SymbolTable.SymbolEntry entry = symbolTable.lookup(exp.name);
    if (entry == null) {
      error(exp.row, exp.col, "undefined identifier '" + exp.name + "'");
      exp.dtype = SymbolTable.VOID;
      return;
    }
    if (entry.isFunction) {
      error(exp.row, exp.col, "'" + exp.name + "' is a function, not a variable");
      exp.dtype = SymbolTable.VOID;
      return;
    }
    if (entry.isArray) {
      if (insideCallArgs) {
        exp.dtype = entry.type;
      } else {
        error(exp.row, exp.col, "array '" + exp.name + "' used without index");
        exp.dtype = entry.type;
      }
      return;
    }
    exp.dtype = entry.type;
  }

  @Override
  public void visit(WriteExp exp, int level) {
    if (exp.output != null) {
      exp.output.accept(this, level + 1);
      if (nodeType(exp.output) == SymbolTable.VOID) {
        error(exp.row, exp.col, "write expression cannot be void");
      }
    }
    exp.dtype = SymbolTable.VOID;
  }

  @Override
  public void visit(TypeSpec exp, int level) {
    exp.dtype = typeFromTypeSpec(exp);
  }

  @Override
  public void visit(VarDeclExp exp, int level) {
    int declaredType = typeFromTypeSpec(exp.type);
    if (declaredType == SymbolTable.VOID) {
      error(exp.row, exp.col, "variable '" + exp.name + "' cannot have type void");
      declaredType = SymbolTable.INT;
    }

    boolean isArray = exp.size != null;
    int arraySize = isArray ? -1 : 0;
    if (isArray) {
      exp.size.accept(this, level + 1);
      if (nodeType(exp.size) != SymbolTable.INT) {
        error(exp.row, exp.col, "array size for '" + exp.name + "' must be int");
      }
      int parsedSize = parseArraySize(exp.size);
      if (parsedSize >= 0) {
        arraySize = parsedSize;
      }
    }

    SymbolTable.SymbolEntry entry =
        new SymbolTable.SymbolEntry(
            exp.name,
            declaredType,
            isArray,
            arraySize,
            false,
            null,
            exp.row,
            exp.col);

    if (!symbolTable.insert(exp.name, entry)) {
      error(exp.row, exp.col, "redefinition of '" + exp.name + "' in the same scope");
    }

    exp.dtype = SymbolTable.VOID;
  }

  @Override
  public void visit(ParamExp exp, int level) {
    int declaredType = typeFromTypeSpec(exp.type);
    if (declaredType == SymbolTable.VOID) {
      error(exp.row, exp.col, "parameter '" + exp.name + "' cannot have type void");
      declaredType = SymbolTable.INT;
    }

    SymbolTable.SymbolEntry entry =
        new SymbolTable.SymbolEntry(
            exp.name,
            declaredType,
            exp.isArray,
            exp.isArray ? -1 : 0,
            false,
            null,
            exp.row,
            exp.col);

    if (!symbolTable.insert(exp.name, entry)) {
      error(exp.row, exp.col, "redefinition of parameter '" + exp.name + "'");
    }
    exp.dtype = SymbolTable.VOID;
  }

  @Override
  public void visit(FunDeclExp exp, int level) {
    int returnType = typeFromTypeSpec(exp.resultType);
    ArrayList<SymbolTable.SymbolEntry> params = buildParamEntries(exp.params);
    FunctionRecord existing = functions.get(exp.name);
    SymbolTable.SymbolEntry fnEntry;
    boolean analyzeBody = exp.body != null;

    if (existing == null) {
      fnEntry =
          new SymbolTable.SymbolEntry(
              exp.name,
              returnType,
              false,
              -1,
              true,
              params,
              exp.row,
              exp.col);
      if (!symbolTable.insert(exp.name, fnEntry)) {
        error(exp.row, exp.col, "redefinition of function '" + exp.name + "'");
        analyzeBody = false;
      }
      functions.put(exp.name, new FunctionRecord(fnEntry, exp.body != null));
    } else {
      fnEntry = existing.entry;
      if (!sameSignature(fnEntry, returnType, params)) {
        error(exp.row, exp.col, "conflicting declaration of function '" + exp.name + "'");
        analyzeBody = false;
      } else if (exp.body == null) {
        error(exp.row, exp.col, "duplicate declaration of function '" + exp.name + "'");
      } else if (existing.hasDefinition) {
        error(exp.row, exp.col, "redefinition of function '" + exp.name + "'");
        analyzeBody = false;
      } else {
        existing.hasDefinition = true;
      }
    }

    if (analyzeBody) {
      symbolTable.enterScope("function " + exp.name);
      scopeLevel++;

      for (SymbolTable.SymbolEntry param : fnEntry.params) {
        if (!symbolTable.insert(param.name, param)) {
          error(exp.row, exp.col, "duplicate parameter '" + param.name + "' in function '" + exp.name + "'");
        }
      }

      int previousFunctionReturnType = currentFunctionReturnType;
      currentFunctionReturnType = returnType;
      if (exp.body instanceof CompoundExp) {
        // The function body's top-level compound shares the function scope.
        // Nested compounds (if/while/else blocks) still create "block" scopes.
        analyzeCompoundBody((CompoundExp) exp.body, level + 1, false, null);
      } else {
        exp.body.accept(this, level + 1);
      }
      currentFunctionReturnType = previousFunctionReturnType;

      symbolTable.printScope(scopeLevel);
      symbolTable.leaveScope();
      scopeLevel--;
    }

    exp.dtype = SymbolTable.VOID;
  }

  @Override
  public void visit(CompoundExp exp, int level) {
    analyzeCompoundBody(exp, level, true, "block");
  }

  @Override
  public void visit(WhileExp exp, int level) {
    if (exp.test != null) {
      exp.test.accept(this, level + 1);
      if (!isConditionType(nodeType(exp.test))) {
        error(exp.row, exp.col, "while condition must be int or bool");
      }
    }
    if (exp.body != null) {
      visitWithBlockContext(exp.body, level + 1, "while");
    }
    exp.dtype = SymbolTable.VOID;
  }

  @Override
  public void visit(ReturnExp exp, int level) {
    if (exp.value != null) {
      exp.value.accept(this, level + 1);
    }
    int actualType = nodeType(exp.value);

    if (currentFunctionReturnType == NO_FUNCTION) {
      error(exp.row, exp.col, "return statement outside of function");
    } else if (currentFunctionReturnType == SymbolTable.VOID) {
      if (actualType != SymbolTable.VOID) {
        error(exp.row, exp.col, "void function cannot return a value");
      }
    } else if (actualType != currentFunctionReturnType) {
      error(exp.row, exp.col, "return type does not match function type");
    }

    exp.dtype = SymbolTable.VOID;
  }

  @Override
  public void visit(CallExp exp, int level) {
    boolean prevInsideCallArgs = insideCallArgs;
    insideCallArgs = true;
    if (exp.args != null) {
      exp.args.accept(this, level + 1);
    }
    insideCallArgs = prevInsideCallArgs;

    SymbolTable.SymbolEntry entry = symbolTable.lookup(exp.function);
    if (entry == null) {
      error(exp.row, exp.col, "undefined function '" + exp.function + "'");
      exp.dtype = SymbolTable.VOID;
      return;
    }
    if (!entry.isFunction) {
      error(exp.row, exp.col, "'" + exp.function + "' is not a function");
      exp.dtype = SymbolTable.VOID;
      return;
    }

    checkArgumentTypes(exp, entry);
    exp.dtype = entry.type;
  }

  @Override
  public void visit(IndexVarExp exp, int level) {
    SymbolTable.SymbolEntry entry = symbolTable.lookup(exp.name);
    if (entry == null) {
      error(exp.row, exp.col, "undefined identifier '" + exp.name + "'");
      if (exp.index != null) {
        exp.index.accept(this, level + 1);
      }
      exp.dtype = SymbolTable.VOID;
      return;
    }
    if (entry.isFunction) {
      error(exp.row, exp.col, "'" + exp.name + "' is a function, not an array");
      exp.dtype = SymbolTable.VOID;
      return;
    }
    if (!entry.isArray) {
      error(exp.row, exp.col, "'" + exp.name + "' is not an array");
      exp.dtype = SymbolTable.VOID;
    }

    if (exp.index != null) {
      exp.index.accept(this, level + 1);
      if (nodeType(exp.index) != SymbolTable.INT) {
        error(exp.row, exp.col, "array index for '" + exp.name + "' must be int");
      }
    }

    if (entry.isArray) {
      exp.dtype = entry.type;
    }
  }

  @Override
  public void visit(BoolExp exp, int level) {
    exp.dtype = SymbolTable.BOOL;
  }

  @Override
  public void visit(NilExp exp, int level) {
    exp.dtype = SymbolTable.VOID;
  }
}
