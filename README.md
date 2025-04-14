# MAVL Compiler

This repository contains coursework completed as part of the *Einführung in den Compilerbau* (Introduction to Compiler Construction) module at *Technische Universität Darmstadt* (TU Darmstadt).

## Overview

This is an EDUCATIONAL project, and an archive of the coursework, implementing a compiler for MAVL (Minimal Abstract Virtual Language). The work was completed in **three stages**, each addressing a critical aspect of compiler construction:

1. **Lexical Analysis**: Building a scanner to tokenize MAVL source code.
2. **Syntax Analysis**: Parsing tokens into a syntax tree and verifying grammar.
3. **Semantic Analysis and Code Generation**: Checking semantics and generating intermediate or target code.

## Features
- Fully functional **MAVL Compiler Driver** and **Interactive Interpreter**.
- Modular design for easy extension and understanding.

## Setup and Build Instructions

### Prerequisites
- An active **internet connection** is required for the initial setup.
- This project uses **Gradle 7.5** as the build tool. If Gradle is not installed on your system, you can use the provided Gradle Wrapper (`gradlew` for Linux/macOS or `gradlew.bat` for Windows).

### Initial Setup
To set up the project, execute the following command:
(please note that more detailed instructions are available in the respective `HOWTO` documents in each subsection.)

```bash
$ gradle mavlc mtam
```

If you are using the Gradle Wrapper, run:

```bash
$ ./gradlew mavlc mtam
```

This will create the startup scripts `mavlc` for the MAVL Compiler Driver and `mtam` for the interactive interpreter in the `build/` directory.

## Development and Testing

### Compiling Source Code
During development, you can compile the source code with:

```bash
$ gradle classes
```

This will only compile modified classes. To perform a clean build, first run:

```bash
$ gradle clean
```

### Running Tests
The project includes public test cases for the coursework exercises. You can execute these tests with:

```bash
$ gradle test
```

## Acknowledgments

Special thanks to:
- TU Darmstadt for providing an excellent learning environment.
- The Orga Team and all the contributors mentioned in the subsections.
