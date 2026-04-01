# C- Compiler Project Report

## Overview

This checkpoint 3 submission completes the front end from checkpoints 1 and 2 by adding TM code generation for valid C- programs. The compiler accepts one command-line option per run:

- `-a` for AST generation
- `-s` for semantic analysis and symbol-table output
- `-c` for TM assembly generation

The implementation is designed to match the course reference TM structure closely, especially the prelude, built-in I/O routines, frame layout, and commenting style used in the provided `fac.tm`, `gcd.tm`, and `sort.tm` examples.

## Front-End Summary

### Scanner and parser

The scanner is generated from `cminus.flex` using JFlex. It recognizes the C- keywords, identifiers, numbers, boolean literals, operators, punctuation, whitespace, and C-style block comments.

The parser is generated from `cminus.cup` using CUP. It constructs AST nodes under `absyn/` for declarations, statements, expressions, calls, array indexing, and control flow. Error productions are present so that parsing can report useful syntax errors with line and column positions.

### Semantic analysis

The semantic analyzer performs scoped symbol tracking, expression type assignment, and static checking for:

- undefined identifiers
- redefinitions in the same scope
- illegal uses of `void`
- operator and assignment type compatibility
- `if` and `while` condition checks
- array indexing checks
- function call argument checks
- return-type checks

For checkpoint 3, the semantic pass was tightened to enforce:

- exact `void main(void)` signature
- `main` being the last declaration
- prototype/definition compatibility
- rejection of unresolved prototype-only functions

Built-in `input` and `output` are inserted into the global environment automatically.

## Code Generation Design

### Overall structure

The backend is implemented in `CodeGenerator.java`. It does not replace the AST or visitor architecture; instead, it traverses the existing AST after semantic analysis has succeeded.

The code generator has three major responsibilities:

1. Collect global variables and function signatures
2. Lay out parameters and locals for each function
3. Emit commented TM assembly

### TM prelude and finale

The compiler emits:

- the standard prelude to initialize `gp`, `fp`, and data memory location `0`
- built-in `input` and `output` routines
- all user functions
- startup code that calls `main`
- a final `HALT`

This keeps the emitted structure close to the reference outputs provided in the course package.

### Register and frame conventions

The implementation uses the same TM register roles as the provided examples:

- `r0` as `ac`
- `r1` as `ac1`
- `r5` as `fp`
- `r6` as `gp`
- `r7` as `pc`

Function frames follow this layout:

- `0(fp)` : saved old frame pointer
- `-1(fp)` : return address
- `-2(fp), -3(fp), ...` : parameters
- lower negative offsets : locals, nested-block locals, and temporary spill locations

Nested block locals are given fixed offsets during layout, rather than changing the frame at runtime. This keeps the generated TM code simple and close to the reference style.

### Globals and arrays

Globals are allocated downward from `gp`, starting at offset `0`. Arrays reserve contiguous memory, and array addressing follows the reference model where the base address is treated as the highest-address element. Element access therefore computes:

`base - index`

Array parameters are passed by reference as base addresses only. No hidden length parameter is introduced.

### Function calls

Arguments are evaluated left to right. The caller stores them into the future callee frame, saves the old frame pointer, switches `fp`, loads a return address into `ac`, and jumps to the callee entry location. After return, the caller restores `fp`.

Recursive calls and mutual recursion are supported. Calls to functions emitted later are backpatched after all user function entry locations become known.

### Expressions and control flow

Arithmetic expressions are lowered to TM `ADD`, `SUB`, `MUL`, and `DIV`.

Comparisons are compiled by subtracting operands and then producing normalized `0/1` boolean values through conditional branches.

Logical operators are compiled with short-circuit behavior:

- `&&` stops early on false
- `||` stops early on true
- `~` produces boolean negation

`if` and `while` treat `0` as false and nonzero as true, which is compatible with the semantic phase allowing both `int` and `bool` conditions.

### Runtime errors

Array accesses emit runtime checks:

- all array accesses halt on negative indices
- global and local arrays with known sizes also halt on `index >= size`
- array parameters perform the lower-bound check only, since their lengths are not available in the calling convention

The runtime failure mechanism is a direct TM `HALT`, which is simple and consistent with the simulator environment.

## Testing

### Reference programs

The backend was checked against the course reference programs:

- `providedFiles/fac.cm`
- `providedFiles/gcd.cm`
- `providedFiles/sort.cm`

These were compiled with `-c` and executed on the provided TM simulator.

### Submission test suite

The root directory contains the checkpoint-style submission set:

- `1.cm`, `2.cm`, `3.cm` as valid runnable programs
- `4.cm` to `8.cm` as focused syntax/semantic/runtime tests
- `9.cm` and `0.cm` as stress cases

The valid set covers:

- arithmetic, assignments, while, and built-in I/O
- boolean logic and control flow
- arrays, array parameters, nested blocks, and function calls
- mutual recursion through prototypes

The error set covers:

- syntax recovery
- redefinition and call checking
- invalid returns
- runtime negative indexing
- runtime upper-bound indexing

## Design Changes and Lessons Learned

The largest change from checkpoint 2 was realizing that code generation needs more than just type information. In particular, backend work required explicit layout decisions for:

- where globals live
- where function parameters live
- how nested local variables are assigned offsets
- how temporary expression results are stored
- how calls to later-defined functions are patched

Another important lesson was that checkpoint 3 forced a revisit of semantic details that were easy to postpone earlier, especially:

- the exact `void main(void)` requirement
- prototype handling
- unresolved prototype rejection

These changes improved both correctness and backend simplicity.

## Assumptions and Limitations

- The implementation prioritizes grading-safe behavior over a large refactor.
- The emitted TM code aims to be structurally similar to the course references, but not byte-for-byte identical.
- Array parameters do not carry lengths, so only lower-bound runtime checks are emitted for them.
- Runtime errors use `HALT` directly instead of a richer reporting convention.
- Some legacy AST node classes from earlier scaffolding remain in the tree package even though the current grammar does not build them.

## Possible Improvements

- Add a richer runtime error convention for array bounds failures
- Add more control-flow-aware return analysis for non-void functions
- Reuse temporary stack slots more aggressively
- Extend the report with member-specific contribution details if submitted as a group

## Contributions

This section should be edited as needed for the final submission:

- If submitted individually, state that the work was completed individually.
- If submitted as a group, list each member and summarize their major contributions.
