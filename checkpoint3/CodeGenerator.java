import java.io.PrintStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import absyn.*;

/**
 * Generates TM assembly for a semantically valid C- program.
 */
public class CodeGenerator {
  private static final int AC = 0;
  private static final int AC1 = 1;
  private static final int GP = 6;
  private static final int FP = 5;
  private static final int PC = 7;

  private enum StorageKind {
    GLOBAL,
    LOCAL,
    PARAM_SCALAR,
    PARAM_ARRAY
  }

  private static class VarInfo {
    final String name;
    final int type;
    final boolean isArray;
    final int arraySize;
    final StorageKind kind;
    final int offset;

    VarInfo(String name, int type, boolean isArray, int arraySize, StorageKind kind, int offset) {
      this.name = name;
      this.type = type;
      this.isArray = isArray;
      this.arraySize = arraySize;
      this.kind = kind;
      this.offset = offset;
    }

    boolean isArrayParam() {
      return kind == StorageKind.PARAM_ARRAY;
    }

    boolean isGlobal() {
      return kind == StorageKind.GLOBAL;
    }

    int baseRegister() {
      return isGlobal() ? GP : FP;
    }
  }

  private static class FunctionInfo {
    final String name;
    final int returnType;
    final ArrayList<VarInfo> params;
    final boolean isBuiltin;
    final ArrayList<Integer> pendingCallPatches;
    FunDeclExp definition;
    int entryLoc;
    int nextLocalOffset;

    FunctionInfo(String name, int returnType, ArrayList<VarInfo> params, boolean isBuiltin) {
      this.name = name;
      this.returnType = returnType;
      this.params = params;
      this.isBuiltin = isBuiltin;
      this.pendingCallPatches = new ArrayList<Integer>();
      this.definition = null;
      this.entryLoc = -1;
      this.nextLocalOffset = -2;
    }
  }

  private static class TMEmitter {
    private abstract static class Entry {
      abstract void write(PrintStream out);
    }

    private static class CommentEntry extends Entry {
      private final String text;

      CommentEntry(String text) {
        this.text = text;
      }

      @Override
      void write(PrintStream out) {
        out.println("* " + text);
      }
    }

    private static class InstructionEntry extends Entry {
      final int loc;
      String op;
      final boolean rr;
      int a;
      int b;
      int c;
      String comment;

      InstructionEntry(int loc, String op, boolean rr, int a, int b, int c, String comment) {
        this.loc = loc;
        this.op = op;
        this.rr = rr;
        this.a = a;
        this.b = b;
        this.c = c;
        this.comment = comment == null ? "" : comment;
      }

      @Override
      void write(PrintStream out) {
        if (rr) {
          out.printf("%3d: %6s%3d,%1d,%1d", loc, op, a, b, c);
        } else {
          out.printf("%3d: %6s%3d,%3d(%1d)", loc, op, a, b, c);
        }
        if (!comment.isEmpty()) {
          out.print("\t" + comment);
        }
        out.println();
      }
    }

    private final ArrayList<Entry> entries;
    private final HashMap<Integer, InstructionEntry> byLoc;
    private int emitLoc;

    TMEmitter() {
      this.entries = new ArrayList<Entry>();
      this.byLoc = new HashMap<Integer, InstructionEntry>();
      this.emitLoc = 0;
    }

    int emitLoc() {
      return emitLoc;
    }

    void emitComment(String text) {
      entries.add(new CommentEntry(text));
    }

    int emitRR(String op, int r, int s, int t, String comment) {
      InstructionEntry entry = new InstructionEntry(emitLoc, op, true, r, s, t, comment);
      entries.add(entry);
      byLoc.put(emitLoc, entry);
      return emitLoc++;
    }

    int emitRM(String op, int r, int d, int s, String comment) {
      InstructionEntry entry = new InstructionEntry(emitLoc, op, false, r, d, s, comment);
      entries.add(entry);
      byLoc.put(emitLoc, entry);
      return emitLoc++;
    }

    int emitSkip(int count) {
      int start = emitLoc;
      for (int i = 0; i < count; i++) {
        emitRM("LDA", PC, 0, PC, "");
      }
      return start;
    }

    void patchRM(int loc, String op, int r, int d, int s, String comment) {
      InstructionEntry entry = byLoc.get(Integer.valueOf(loc));
      if (entry == null) {
        throw new IllegalStateException("No instruction at location " + loc);
      }
      entry.op = op;
      entry.a = r;
      entry.b = d;
      entry.c = s;
      entry.comment = comment == null ? "" : comment;
    }

    void patchAbsJump(int loc, String op, int r, int target, String comment) {
      patchRM(loc, op, r, target - (loc + 1), PC, comment);
    }

    void write(PrintStream out) {
      for (Entry entry : entries) {
        entry.write(out);
      }
    }
  }

  private final LinkedHashMap<String, VarInfo> globals;
  private final LinkedHashMap<String, FunctionInfo> functions;
  private final IdentityHashMap<VarDeclExp, VarInfo> declBindings;
  private final IdentityHashMap<ParamExp, VarInfo> paramBindings;
  private final Deque<LinkedHashMap<String, VarInfo>> scopes;

  private TMEmitter emitter;
  private FunctionInfo currentFunction;
  private int nextGlobalOffset;
  private int tempOffset;
  private int programFrameBase;

  public CodeGenerator() {
    this.globals = new LinkedHashMap<String, VarInfo>();
    this.functions = new LinkedHashMap<String, FunctionInfo>();
    this.declBindings = new IdentityHashMap<VarDeclExp, VarInfo>();
    this.paramBindings = new IdentityHashMap<ParamExp, VarInfo>();
    this.scopes = new ArrayDeque<LinkedHashMap<String, VarInfo>>();
    this.currentFunction = null;
    this.nextGlobalOffset = 0;
    this.tempOffset = -2;
    this.programFrameBase = 0;
  }

  public void generate(Absyn tree, PrintStream out) {
    if (!(tree instanceof ExpList)) {
      throw new IllegalArgumentException("Expected ExpList as program root");
    }

    installBuiltinFunctions();
    collectTopLevel((ExpList) tree);
    layoutFunctions();

    emitter = new TMEmitter();
    emitProgramHeader();
    emitPreludeAndBuiltins();
    emitGlobalComments();
    emitFunctions();
    emitStartup();
    emitter.write(out);
  }

  private void installBuiltinFunctions() {
    ArrayList<VarInfo> inputParams = new ArrayList<VarInfo>();
    functions.put("input", new FunctionInfo("input", SymbolTable.INT, inputParams, true));

    ArrayList<VarInfo> outputParams = new ArrayList<VarInfo>();
    outputParams.add(new VarInfo("x", SymbolTable.INT, false, 0, StorageKind.PARAM_SCALAR, -2));
    functions.put("output", new FunctionInfo("output", SymbolTable.VOID, outputParams, true));
  }

  private void collectTopLevel(ExpList list) {
    ExpList cursor = list;
    while (cursor != null) {
      Exp exp = cursor.head;
      if (exp instanceof VarDeclExp) {
        collectGlobal((VarDeclExp) exp);
      } else if (exp instanceof FunDeclExp) {
        collectFunction((FunDeclExp) exp);
      }
      cursor = cursor.tail;
    }
    programFrameBase = nextGlobalOffset;
  }

  private void collectGlobal(VarDeclExp exp) {
    boolean isArray = exp.size != null;
    int size = isArray ? parseArraySize(exp.size) : 1;
    int offset = nextGlobalOffset;
    VarInfo info =
        new VarInfo(
            exp.name,
            typeFromTypeSpec(exp.type),
            isArray,
            isArray ? size : 0,
            StorageKind.GLOBAL,
            offset);
    globals.put(exp.name, info);
    nextGlobalOffset -= size;
    declBindings.put(exp, info);
  }

  private void collectFunction(FunDeclExp exp) {
    ArrayList<VarInfo> params = signatureParams(exp.params);
    FunctionInfo existing = functions.get(exp.name);
    if (existing == null || existing.isBuiltin) {
      FunctionInfo info = new FunctionInfo(exp.name, typeFromTypeSpec(exp.resultType), params, false);
      info.definition = exp.body != null ? exp : null;
      functions.put(exp.name, info);
      return;
    }

    if (existing.definition == null && exp.body != null) {
      existing.definition = exp;
    }
  }

  private void layoutFunctions() {
    for (FunctionInfo info : functions.values()) {
      if (info.isBuiltin || info.definition == null) {
        continue;
      }
      layoutFunction(info);
    }
  }

  private void layoutFunction(FunctionInfo function) {
    currentFunction = function;
    scopes.clear();
    LinkedHashMap<String, VarInfo> functionScope = new LinkedHashMap<String, VarInfo>();
    scopes.push(functionScope);

    int paramOffset = -2;
    ExpList cursor = function.definition.params;
    while (cursor != null) {
      if (cursor.head instanceof ParamExp) {
        ParamExp param = (ParamExp) cursor.head;
        StorageKind kind = param.isArray ? StorageKind.PARAM_ARRAY : StorageKind.PARAM_SCALAR;
        VarInfo info =
            new VarInfo(
                param.name,
                typeFromTypeSpec(param.type),
                param.isArray,
                -1,
                kind,
                paramOffset);
        functionScope.put(param.name, info);
        paramBindings.put(param, info);
        paramOffset--;
      }
      cursor = cursor.tail;
    }

    function.nextLocalOffset = paramOffset;
    layoutCompoundBody(function.definition.body, false);
    scopes.pop();
    currentFunction = null;
  }

  private void layoutCompoundBody(CompoundExp compound, boolean createScope) {
    if (compound == null) {
      return;
    }
    if (createScope) {
      scopes.push(new LinkedHashMap<String, VarInfo>());
    }
    layoutLocalDeclarations(compound.localDecls);
    layoutStatements(compound.statements);
    if (createScope) {
      scopes.pop();
    }
  }

  private void layoutLocalDeclarations(ExpList localDecls) {
    ExpList cursor = localDecls;
    while (cursor != null) {
      if (cursor.head instanceof VarDeclExp) {
        VarDeclExp decl = (VarDeclExp) cursor.head;
        boolean isArray = decl.size != null;
        int size = isArray ? parseArraySize(decl.size) : 1;
        int offset = currentFunction.nextLocalOffset;
        currentFunction.nextLocalOffset -= size;
        VarInfo info =
            new VarInfo(
                decl.name,
                typeFromTypeSpec(decl.type),
                isArray,
                isArray ? size : 0,
                StorageKind.LOCAL,
                offset);
        scopes.peek().put(decl.name, info);
        declBindings.put(decl, info);
      }
      cursor = cursor.tail;
    }
  }

  private void layoutStatements(ExpList statements) {
    ExpList cursor = statements;
    while (cursor != null) {
      layoutStatement(cursor.head);
      cursor = cursor.tail;
    }
  }

  private void layoutStatement(Exp exp) {
    if (exp instanceof CompoundExp) {
      layoutCompoundBody((CompoundExp) exp, true);
    } else if (exp instanceof IfExp) {
      IfExp ifExp = (IfExp) exp;
      layoutStatementList(ifExp.thenpart);
      layoutStatementList(ifExp.elsepart);
    } else if (exp instanceof WhileExp) {
      layoutStatement(((WhileExp) exp).body);
    }
  }

  private void layoutStatementList(ExpList list) {
    if (list == null) {
      return;
    }
    ExpList cursor = list;
    while (cursor != null) {
      layoutStatement(cursor.head);
      cursor = cursor.tail;
    }
  }

  private void emitProgramHeader() {
    emitter.emitComment("C-Minus Compilation to TM Code");
  }

  private void emitPreludeAndBuiltins() {
    emitter.emitComment("Standard prelude:");
    emitter.emitRM("LD", GP, 0, 0, "load gp with maxaddress");
    emitter.emitRM("LDA", FP, 0, GP, "copy gp to fp");
    emitter.emitRM("ST", AC, 0, 0, "clear location 0");

    emitter.emitComment("Jump around i/o routines here");
    int jumpAroundIo = emitter.emitSkip(1);

    FunctionInfo inputFn = functions.get("input");
    inputFn.entryLoc = emitter.emitLoc();
    emitter.emitComment("code for input routine");
    emitter.emitRM("ST", AC, -1, FP, "store return");
    emitter.emitRR("IN", AC, 0, 0, "input");
    emitter.emitRM("LD", PC, -1, FP, "return to caller");

    FunctionInfo outputFn = functions.get("output");
    outputFn.entryLoc = emitter.emitLoc();
    emitter.emitComment("code for output routine");
    emitter.emitRM("ST", AC, -1, FP, "store return");
    emitter.emitRM("LD", AC, -2, FP, "load output value");
    emitter.emitRR("OUT", AC, 0, 0, "output");
    emitter.emitRM("LD", PC, -1, FP, "return to caller");

    emitter.patchAbsJump(jumpAroundIo, "LDA", PC, emitter.emitLoc(), "jump around i/o code");
    emitter.emitComment("End of standard prelude.");
  }

  private void emitGlobalComments() {
    for (VarInfo info : globals.values()) {
      emitter.emitComment("allocating global var: " + info.name);
      emitter.emitComment("<- vardecl");
    }
  }

  private void emitFunctions() {
    for (FunctionInfo function : functions.values()) {
      if (function.isBuiltin || function.definition == null) {
        continue;
      }
      emitFunction(function);
    }
    patchPendingCalls();
  }

  private void emitFunction(FunctionInfo function) {
    currentFunction = function;
    tempOffset = function.nextLocalOffset - 1;
    scopes.clear();
    scopes.push(new LinkedHashMap<String, VarInfo>());
    for (VarInfo param : function.params) {
      if (!param.name.equals("")) {
        scopes.peek().put(param.name, param);
      }
    }

    emitter.emitComment("processing function: " + function.name);
    emitter.emitComment("jump around function body here");
    int jumpAround = emitter.emitSkip(1);
    function.entryLoc = emitter.emitLoc();

    emitter.emitRM("ST", AC, -1, FP, "store return");
    emitCompoundBody(function.definition.body, false);
    emitter.emitRM("LD", PC, -1, FP, "return to caller");

    emitter.patchAbsJump(jumpAround, "LDA", PC, emitter.emitLoc(), "jump around fn body");
    emitter.emitComment("<- fundecl");
    scopes.pop();
    currentFunction = null;
  }

  private void emitStartup() {
    FunctionInfo mainFn = functions.get("main");
    emitter.emitRM("ST", FP, programFrameBase, FP, "push ofp");
    emitter.emitRM("LDA", FP, programFrameBase, FP, "push frame");
    emitter.emitRM("LDA", AC, 1, PC, "load ac with ret ptr");
    emitter.emitRM("LDA", PC, mainFn.entryLoc - (emitter.emitLoc() + 1), PC, "jump to main loc");
    emitter.emitRM("LD", FP, 0, FP, "pop frame");
    emitter.emitComment("End of execution.");
    emitter.emitRR("HALT", 0, 0, 0, "");
  }

  private void patchPendingCalls() {
    for (FunctionInfo function : functions.values()) {
      if (function.entryLoc < 0) {
        continue;
      }
      for (Integer loc : function.pendingCallPatches) {
        emitter.patchAbsJump(loc.intValue(), "LDA", PC, function.entryLoc, "jump to fun loc");
      }
      function.pendingCallPatches.clear();
    }
  }

  private void emitCompoundBody(CompoundExp compound, boolean createScope) {
    if (compound == null) {
      return;
    }
    emitter.emitComment("-> compound statement");
    if (createScope) {
      scopes.push(new LinkedHashMap<String, VarInfo>());
    }

    ExpList localDecls = compound.localDecls;
    while (localDecls != null) {
      if (localDecls.head instanceof VarDeclExp) {
        VarDeclExp decl = (VarDeclExp) localDecls.head;
        VarInfo info = declBindings.get(decl);
        scopes.peek().put(decl.name, info);
        emitter.emitComment("processing local var: " + decl.name);
      }
      localDecls = localDecls.tail;
    }

    ExpList statements = compound.statements;
    while (statements != null) {
      emitStatement(statements.head);
      statements = statements.tail;
    }

    if (createScope) {
      scopes.pop();
    }
    emitter.emitComment("<- compound statement");
  }

  private void emitStatement(Exp exp) {
    if (exp == null || exp instanceof NilExp) {
      return;
    }
    if (exp instanceof CompoundExp) {
      emitCompoundBody((CompoundExp) exp, true);
      return;
    }
    if (exp instanceof IfExp) {
      emitIf((IfExp) exp);
      return;
    }
    if (exp instanceof WhileExp) {
      emitWhile((WhileExp) exp);
      return;
    }
    if (exp instanceof ReturnExp) {
      emitReturn((ReturnExp) exp);
      return;
    }
    emitExpression(exp);
  }

  private void emitIf(IfExp exp) {
    emitter.emitComment("-> if");
    emitExpression(exp.test);
    int jumpToElse = emitter.emitSkip(1);
    emitSingletonStatement(exp.thenpart);
    int jumpToEnd = emitter.emitSkip(1);
    int elseLoc = emitter.emitLoc();
    emitter.patchAbsJump(jumpToElse, "JEQ", AC, elseLoc, "if: jmp to else");
    emitSingletonStatement(exp.elsepart);
    emitter.patchAbsJump(jumpToEnd, "LDA", PC, emitter.emitLoc(), "jmp to end");
    emitter.emitComment("<- if");
  }

  private void emitWhile(WhileExp exp) {
    emitter.emitComment("-> while");
    int testLoc = emitter.emitLoc();
    emitExpression(exp.test);
    int jumpToEnd = emitter.emitSkip(1);
    emitStatement(exp.body);
    emitter.emitRM("LDA", PC, testLoc - (emitter.emitLoc() + 1), PC, "while: absolute jmp to test");
    emitter.patchAbsJump(jumpToEnd, "JEQ", AC, emitter.emitLoc(), "while: jmp to end");
    emitter.emitComment("<- while");
  }

  private void emitReturn(ReturnExp exp) {
    emitter.emitComment("-> return");
    if (exp.value != null && !(exp.value instanceof NilExp)) {
      emitExpression(exp.value);
    }
    emitter.emitRM("LD", PC, -1, FP, "return to caller");
    emitter.emitComment("<- return");
  }

  private void emitSingletonStatement(ExpList list) {
    if (list == null || list.head == null || list.head instanceof NilExp) {
      return;
    }
    emitStatement(list.head);
  }

  private void emitExpression(Exp exp) {
    if (exp == null || exp instanceof NilExp) {
      emitter.emitRM("LDC", AC, 0, 0, "load nil");
      return;
    }
    if (exp instanceof IntExp) {
      emitter.emitComment("-> constant");
      emitter.emitRM("LDC", AC, Integer.parseInt(((IntExp) exp).value), 0, "load const");
      emitter.emitComment("<- constant");
      return;
    }
    if (exp instanceof BoolExp) {
      emitter.emitComment("-> bool");
      emitter.emitRM("LDC", AC, "true".equals(((BoolExp) exp).value) ? 1 : 0, 0, "load bool");
      emitter.emitComment("<- bool");
      return;
    }
    if (exp instanceof VarExp) {
      emitVarValue((VarExp) exp);
      return;
    }
    if (exp instanceof IndexVarExp) {
      emitIndexedValue((IndexVarExp) exp);
      return;
    }
    if (exp instanceof CallExp) {
      emitCall((CallExp) exp);
      return;
    }
    if (exp instanceof OpExp) {
      emitOperation((OpExp) exp);
      return;
    }
  }

  private void emitVarValue(VarExp exp) {
    VarInfo info = lookupVar(exp.name);
    emitter.emitComment("-> id");
    emitter.emitComment("looking up id: " + exp.name);
    if (info.isArray) {
      emitArrayBase(info);
      emitter.emitComment("<- id");
      return;
    }
    emitter.emitRM("LD", AC, info.offset, info.baseRegister(), "load id value");
    emitter.emitComment("<- id");
  }

  private void emitVarAddress(VarExp exp) {
    VarInfo info = lookupVar(exp.name);
    emitter.emitComment("-> id");
    emitter.emitComment("looking up id: " + exp.name);
    if (info.isArrayParam()) {
      emitter.emitRM("LD", AC, info.offset, FP, "load array parameter base");
    } else {
      emitter.emitRM("LDA", AC, info.offset, info.baseRegister(), "load id address");
    }
    emitter.emitComment("<- id");
  }

  private void emitIndexedValue(IndexVarExp exp) {
    emitIndexedAddress(exp);
    emitter.emitRM("LD", AC, 0, AC, "load value at array index");
    emitter.emitComment("<- subs");
  }

  private void emitIndexedAddress(IndexVarExp exp) {
    VarInfo info = lookupVar(exp.name);
    emitter.emitComment("-> subs");
    emitExpression(exp.index);
    int savedTemp = reserveTemp();
    emitter.emitRM("ST", AC, savedTemp, FP, "store array index");
    emitLowerBoundCheck();
    if (!info.isArrayParam() && info.arraySize >= 0) {
      emitter.emitRM("LD", AC, savedTemp, FP, "reload index");
      emitter.emitRM("LDC", AC1, info.arraySize, 0, "load array size");
      emitter.emitRR("SUB", AC, AC, AC1, "index - size");
      int haltUpper = emitter.emitSkip(1);
      emitter.emitRM("LDA", PC, 1, PC, "skip halt if in range");
      int haltLoc = emitter.emitRR("HALT", 0, 0, 0, "halt if subscript >= size");
      emitter.patchAbsJump(haltUpper, "JGE", AC, haltLoc, "halt if subscript >= size");
    }
    emitter.emitRM("LD", AC, savedTemp, FP, "reload index");
    if (info.isArrayParam()) {
      emitter.emitRM("LD", AC1, info.offset, FP, "load array base addr");
    } else {
      emitter.emitRM("LDA", AC1, info.offset, info.baseRegister(), "load array base addr");
    }
    emitter.emitRR("SUB", AC, AC1, AC, "base is at top of array");
    releaseTemp(savedTemp);
  }

  private void emitLowerBoundCheck() {
    int haltNeg = emitter.emitSkip(1);
    emitter.emitRM("LDA", PC, 1, PC, "absolute jump if not");
    int haltLoc = emitter.emitRR("HALT", 0, 0, 0, "halt if subscript < 0");
    emitter.patchAbsJump(haltNeg, "JLT", AC, haltLoc, "halt if subscript < 0");
  }

  private void emitCall(CallExp exp) {
    FunctionInfo function = functions.get(exp.function);
    if (function == null) {
      throw new IllegalStateException("Unknown function " + exp.function);
    }
    emitter.emitComment("-> call of function: " + exp.function);

    int argCount = function.params.size();
    int savedTemp = tempOffset;
    int frameBase = tempOffset;
    tempOffset -= (argCount + 2);

    ArrayList<Exp> args = flattenArgs(exp.args);
    for (int i = 0; i < args.size(); i++) {
      Exp arg = args.get(i);
      VarInfo expected = function.params.get(i);
      if (expected.isArray && arg instanceof VarExp) {
        VarInfo actual = lookupVar(((VarExp) arg).name);
        if (actual != null && actual.isArray) {
          emitArrayBase(actual);
        } else {
          emitExpression(arg);
        }
      } else {
        emitExpression(arg);
      }
      emitter.emitRM("ST", AC, frameBase - (i + 2), FP, "store arg val");
    }

    emitter.emitRM("ST", FP, frameBase, FP, "push ofp");
    emitter.emitRM("LDA", FP, frameBase, FP, "push frame");
    emitter.emitRM("LDA", AC, 1, PC, "load ac with ret ptr");
    if (function.entryLoc >= 0) {
      emitter.emitRM("LDA", PC, function.entryLoc - (emitter.emitLoc() + 1), PC, "jump to fun loc");
    } else {
      int callPatch = emitter.emitSkip(1);
      function.pendingCallPatches.add(Integer.valueOf(callPatch));
    }
    emitter.emitRM("LD", FP, 0, FP, "pop frame");
    tempOffset = savedTemp;
    emitter.emitComment("<- call");
  }

  private void emitArrayBase(VarInfo info) {
    if (info.isArrayParam()) {
      emitter.emitRM("LD", AC, info.offset, FP, "load id value");
    } else {
      emitter.emitRM("LDA", AC, info.offset, info.baseRegister(), "load id address");
    }
  }

  private void emitOperation(OpExp exp) {
    emitter.emitComment("-> op");
    switch (exp.op) {
      case OpExp.ASSIGN:
        emitAssignment(exp);
        break;
      case OpExp.PLUS:
        emitBinaryArithmetic(exp, "ADD", "op +");
        break;
      case OpExp.MINUS:
        emitBinaryArithmetic(exp, "SUB", "op -");
        break;
      case OpExp.TIMES:
        emitBinaryArithmetic(exp, "MUL", "op *");
        break;
      case OpExp.OVER:
        emitBinaryArithmetic(exp, "DIV", "op /");
        break;
      case OpExp.LT:
        emitComparison(exp, "JLT", "op <");
        break;
      case OpExp.LTEQ:
        emitComparison(exp, "JLE", "op <=");
        break;
      case OpExp.GT:
        emitComparison(exp, "JGT", "op >");
        break;
      case OpExp.GTEQ:
        emitComparison(exp, "JGE", "op >=");
        break;
      case OpExp.EQ:
        emitComparison(exp, "JEQ", "op ==");
        break;
      case OpExp.NEQ:
        emitComparison(exp, "JNE", "op !=");
        break;
      case OpExp.UMINUS:
        emitUnaryMinus(exp);
        break;
      case OpExp.NOT:
        emitLogicalNot(exp);
        break;
      case OpExp.AND:
        emitLogicalAnd(exp);
        break;
      case OpExp.OR:
        emitLogicalOr(exp);
        break;
      case OpExp.COMMA:
        emitExpression(exp.left);
        emitExpression(exp.right);
        break;
      default:
        emitExpression(exp.right);
        break;
    }
    emitter.emitComment("<- op");
  }

  private void emitAssignment(OpExp exp) {
    if (exp.left instanceof VarExp) {
      emitVarAddress((VarExp) exp.left);
    } else if (exp.left instanceof IndexVarExp) {
      emitIndexedAddress((IndexVarExp) exp.left);
      emitter.emitComment("<- subs");
    }
    int addrTemp = reserveTemp();
    emitter.emitRM("ST", AC, addrTemp, FP, "op: push left");
    emitExpression(exp.right);
    emitter.emitRM("LD", AC1, addrTemp, FP, "op: load left");
    emitter.emitRM("ST", AC, 0, AC1, "assign: store value");
    releaseTemp(addrTemp);
  }

  private void emitBinaryArithmetic(OpExp exp, String op, String comment) {
    emitExpression(exp.left);
    int leftTemp = reserveTemp();
    emitter.emitRM("ST", AC, leftTemp, FP, "op: push left");
    emitExpression(exp.right);
    emitter.emitRM("LD", AC1, leftTemp, FP, "op: load left");
    emitter.emitRR(op, AC, AC1, AC, comment);
    releaseTemp(leftTemp);
  }

  private void emitComparison(OpExp exp, String jumpOp, String opComment) {
    emitExpression(exp.left);
    int leftTemp = reserveTemp();
    emitter.emitRM("ST", AC, leftTemp, FP, "op: push left");
    emitExpression(exp.right);
    emitter.emitRM("LD", AC1, leftTemp, FP, "op: load left");
    emitter.emitRR("SUB", AC, AC1, AC, opComment);
    int trueJump = emitter.emitSkip(1);
    emitter.emitRM("LDC", AC, 0, 0, "false case");
    int endJump = emitter.emitSkip(1);
    int trueLoc = emitter.emitLoc();
    emitter.emitRM("LDC", AC, 1, 0, "true case");
    emitter.patchAbsJump(trueJump, jumpOp, AC, trueLoc, "br if true");
    emitter.patchAbsJump(endJump, "LDA", PC, emitter.emitLoc(), "unconditional jmp");
    releaseTemp(leftTemp);
  }

  private void emitUnaryMinus(OpExp exp) {
    emitExpression(exp.right);
    emitter.emitRM("LDC", AC1, 0, 0, "load zero");
    emitter.emitRR("SUB", AC, AC1, AC, "op unary -");
  }

  private void emitLogicalNot(OpExp exp) {
    emitExpression(exp.right);
    int trueJump = emitter.emitSkip(1);
    emitter.emitRM("LDC", AC, 0, 0, "false case");
    int endJump = emitter.emitSkip(1);
    int trueLoc = emitter.emitLoc();
    emitter.emitRM("LDC", AC, 1, 0, "true case");
    emitter.patchAbsJump(trueJump, "JEQ", AC, trueLoc, "br if zero");
    emitter.patchAbsJump(endJump, "LDA", PC, emitter.emitLoc(), "unconditional jmp");
  }

  private void emitLogicalAnd(OpExp exp) {
    emitExpression(exp.left);
    int jumpFalseLeft = emitter.emitSkip(1);
    emitExpression(exp.right);
    int jumpFalseRight = emitter.emitSkip(1);
    emitter.emitRM("LDC", AC, 1, 0, "and true");
    int jumpEnd = emitter.emitSkip(1);
    int falseLoc = emitter.emitLoc();
    emitter.emitRM("LDC", AC, 0, 0, "and false");
    emitter.patchAbsJump(jumpFalseLeft, "JEQ", AC, falseLoc, "and: lhs false");
    emitter.patchAbsJump(jumpFalseRight, "JEQ", AC, falseLoc, "and: rhs false");
    emitter.patchAbsJump(jumpEnd, "LDA", PC, emitter.emitLoc(), "and: end");
  }

  private void emitLogicalOr(OpExp exp) {
    emitExpression(exp.left);
    int jumpTrueLeft = emitter.emitSkip(1);
    emitExpression(exp.right);
    int jumpTrueRight = emitter.emitSkip(1);
    emitter.emitRM("LDC", AC, 0, 0, "or false");
    int jumpEnd = emitter.emitSkip(1);
    int trueLoc = emitter.emitLoc();
    emitter.emitRM("LDC", AC, 1, 0, "or true");
    emitter.patchAbsJump(jumpTrueLeft, "JNE", AC, trueLoc, "or: lhs true");
    emitter.patchAbsJump(jumpTrueRight, "JNE", AC, trueLoc, "or: rhs true");
    emitter.patchAbsJump(jumpEnd, "LDA", PC, emitter.emitLoc(), "or: end");
  }

  private VarInfo lookupVar(String name) {
    for (Map<String, VarInfo> scope : scopes) {
      VarInfo info = scope.get(name);
      if (info != null) {
        return info;
      }
    }
    return globals.get(name);
  }

  private int reserveTemp() {
    int slot = tempOffset;
    tempOffset--;
    return slot;
  }

  private void releaseTemp(int slot) {
    if (slot == tempOffset + 1) {
      tempOffset++;
    }
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

  private int parseArraySize(Exp sizeExp) {
    if (sizeExp instanceof IntExp) {
      return Integer.parseInt(((IntExp) sizeExp).value);
    }
    return 1;
  }

  private ArrayList<VarInfo> signatureParams(ExpList params) {
    ArrayList<VarInfo> result = new ArrayList<VarInfo>();
    int offset = -2;
    ExpList cursor = params;
    while (cursor != null) {
      if (cursor.head instanceof ParamExp) {
        ParamExp param = (ParamExp) cursor.head;
        StorageKind kind = param.isArray ? StorageKind.PARAM_ARRAY : StorageKind.PARAM_SCALAR;
        result.add(
            new VarInfo(
                param.name,
                typeFromTypeSpec(param.type),
                param.isArray,
                -1,
                kind,
                offset));
        offset--;
      }
      cursor = cursor.tail;
    }
    return result;
  }

  private ArrayList<Exp> flattenArgs(ExpList args) {
    ArrayList<Exp> result = new ArrayList<Exp>();
    ExpList cursor = args;
    while (cursor != null) {
      result.add(cursor.head);
      cursor = cursor.tail;
    }
    return result;
  }
}
