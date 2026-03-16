import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Stack;

/**
 * Scoped symbol table for semantic analysis.
 */
public class SymbolTable {
  public static final int INT = 0;
  public static final int BOOL = 1;
  public static final int VOID = 2;

  public static class SymbolEntry {
    public final String name;
    public final int type;
    public final boolean isArray;
    public final int arraySize;
    public final boolean isFunction;
    public final ArrayList<SymbolEntry> params;
    public final int row;
    public final int col;
    public boolean isBuiltin;

    public SymbolEntry(
        String name,
        int type,
        boolean isArray,
        int arraySize,
        boolean isFunction,
        ArrayList<SymbolEntry> params,
        int row,
        int col) {
      this.name = name;
      this.type = type;
      this.isArray = isArray;
      this.arraySize = arraySize;
      this.isFunction = isFunction;
      this.params = (params == null) ? new ArrayList<SymbolEntry>() : params;
      this.row = row;
      this.col = col;
      this.isBuiltin = false;
    }

    private static String typeName(int type) {
      switch (type) {
        case INT:
          return "int";
        case BOOL:
          return "bool";
        case VOID:
          return "void";
        default:
          return "unknown(" + type + ")";
      }
    }

    private String formatType() {
      String base = typeName(type);
      if (!isArray) {
        return base;
      }
      if (arraySize >= 0) {
        return base + "[" + arraySize + "]";
      }
      return base + "[]";
    }

    private String formatFunctionSignature() {
      StringBuilder builder = new StringBuilder();
      builder.append("(");
      for (int i = 0; i < params.size(); i++) {
        SymbolEntry param = params.get(i);
        builder.append(param.formatType());
        if (i < params.size() - 1) {
          builder.append(", ");
        }
      }
      builder.append(") -> ").append(typeName(type));
      return builder.toString();
    }

    @Override
    public String toString() {
      if (isFunction) {
        return name + ": " + formatFunctionSignature();
      }
      return name + ": " + formatType();
    }
  }

  private final Stack<LinkedHashMap<String, ArrayList<SymbolEntry>>> scopes;
  private final Stack<String> scopeNames;
  private final PrintStream out;

  public SymbolTable() {
    this(System.out);
  }

  public SymbolTable(PrintStream out) {
    this.scopes = new Stack<LinkedHashMap<String, ArrayList<SymbolEntry>>>();
    this.scopeNames = new Stack<String>();
    this.out = out;
  }

  private String indentForDepth(int depth) {
    StringBuilder indent = new StringBuilder();
    for (int i = 0; i < depth; i++) {
      indent.append("  ");
    }
    return indent.toString();
  }

  public void enterScope(String name) {
    scopes.push(new LinkedHashMap<String, ArrayList<SymbolEntry>>());
    scopeNames.push(name == null ? "<anonymous>" : name);

    String scope = scopeNames.peek();
    String indent = indentForDepth(scopes.size() - 1);
    if ("global".equals(scope)) {
      out.println(indent + "Entering the global scope:");
    } else if (scope.startsWith("function ")) {
      out.println(indent + "Entering the scope for " + scope + ":");
    } else if (scope.startsWith("block ")) {
      out.println(indent + "Entering a new block (" + scope.substring("block ".length()) + "):");
    } else if ("block".equals(scope)) {
      out.println(indent + "Entering a new block:");
    } else {
      out.println(indent + "Entering scope: " + scope);
    }
  }

  public void leaveScope() {
    if (scopes.isEmpty()) {
      return;
    }

    String scope = scopeNames.peek();
    String indent = indentForDepth(scopes.size() - 1);
    if ("global".equals(scope)) {
      out.println(indent + "Leaving the global scope");
    } else if (scope.startsWith("function ")) {
      out.println(indent + "Leaving the function scope");
    } else if (scope.startsWith("block ")) {
      out.println(indent + "Leaving block (" + scope.substring("block ".length()) + ")");
    } else if ("block".equals(scope)) {
      out.println(indent + "Leaving the block");
    } else {
      out.println(indent + "Leaving scope: " + scope);
    }
    scopes.pop();
    scopeNames.pop();
  }

  public boolean insert(String name, SymbolEntry entry) {
    if (scopes.isEmpty()) {
      return false;
    }

    LinkedHashMap<String, ArrayList<SymbolEntry>> current = scopes.peek();
    if (current.containsKey(name) && !current.get(name).isEmpty()) {
      return false;
    }

    ArrayList<SymbolEntry> entries = current.get(name);
    if (entries == null) {
      entries = new ArrayList<SymbolEntry>();
      current.put(name, entries);
    }
    entries.add(entry);
    return true;
  }

  public SymbolEntry lookup(String name) {
    for (int i = scopes.size() - 1; i >= 0; i--) {
      LinkedHashMap<String, ArrayList<SymbolEntry>> scope = scopes.get(i);
      ArrayList<SymbolEntry> entries = scope.get(name);
      if (entries != null && !entries.isEmpty()) {
        return entries.get(entries.size() - 1);
      }
    }
    return null;
  }

  public SymbolEntry lookupLocal(String name) {
    if (scopes.isEmpty()) {
      return null;
    }

    ArrayList<SymbolEntry> entries = scopes.peek().get(name);
    if (entries == null || entries.isEmpty()) {
      return null;
    }
    return entries.get(entries.size() - 1);
  }

  public void printScope(int level) {
    if (scopes.isEmpty()) {
      return;
    }

    String indent = indentForDepth(scopes.size());

    for (Map.Entry<String, ArrayList<SymbolEntry>> item : scopes.peek().entrySet()) {
      for (SymbolEntry entry : item.getValue()) {
        if (entry.isBuiltin) continue;
        out.println(indent + entry.toString());
      }
    }
  }
}
