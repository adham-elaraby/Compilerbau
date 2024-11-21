# Testing the MAVLC Compiler with an Example Program

This document outlines the steps we took to test the MAVLC compiler by running it on an example MAVLC program and outputting the Abstract Syntax Tree (AST).

## Prerequisites

Ensure that you have the following installed on your system:
- Java Virtual Machine (JVM) 17
- Gradle 7.5 or the Gradle Wrapper
- Graphviz tools or online tool

## Example MAVLC Program

Create a file named `factorial_example.mavlc` with the following content:

```mavlc
function void main(){
    printString("fac(3): ");
    printInt(fac(3));
    printString("\nfac(10): ");
    printInt(fac(10));
}

function int fac(int n){
    return (n > 1) ? n * fac(n-1) : -1;
}
```

## Compile the MAVLC Program

1. Open a terminal and navigate to the root directory of your MAVLC project.
2. Run the MAVLC compiler using the following command:

    ```sh
    $ build/mavlc factorial_example.mavlc --dump-dot-ast
    ```

This command will generate a file named `factorial_example.syn.dot` in the current directory, containing the DOT representation of the AST.

## Generate the AST Image

1. Ensure that Graphviz is installed on your system.
2. Use the following command to generate a PNG image of the AST:

    ```sh
    $ dot -Tpng -o factorial_example.png factorial_example.syn.dot
    ```

This command will create a file named `factorial_example.png` containing the visual representation of the AST.

## Output

The resulting AST image is shown below:
![AST of Factorial Example](https://github.com/user-attachments/assets/129fdea9-a5a6-4e81-baf0-924e2510fc74)
