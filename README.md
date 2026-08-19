# 🧩 Algorithmic Sudoku Solver

A high-performance full-stack Java application that solves standard 9x9 Sudoku puzzles using recursive backtracking optimized with O(1) bitwise constraint validation.

---

## ⚡ Key Technical Highlights

* **Recursive Backtracking:** Explores valid candidate placements using depth-first search (DFS) with state restoration.
* **O(1) Bitmask Constraint Checking:** Replaced standard O(N) linear scans across rows, columns, and 3x3 subgrids with bitwise operations (`&`, `|`, `~`), eliminating iterative overhead during deep recursion.
* **Zero External Dependencies:** Built using Java's native `com.sun.net.httpserver` architecture without third-party frameworks.
* **Execution Performance:** Resolves hard and expert-tier grids in under **1 millisecond** (sub-millisecond execution readouts).

---

## 🛠️ System Architecture

```text
┌───────────────────────┐         POST /api/solve (JSON)        ┌────────────────────────┐
│   Interactive Web UI  │ ────────────────────────────────────► │   SudokuServer (Java)  │
│   (HTML5 / CSS / JS)  │ ◄──────────────────────────────────── │ (Bitmask Backtracking) │
└───────────────────────┘       200 OK (Resolved Grid + Time)   └────────────────────────┘
