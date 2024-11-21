# Testing the MAVLC Compiler with an Example Program

This document outlines the steps we took to test the MAVLC compiler by running it on an example MAVLC program and outputting the Abstract Syntax Tree (AST).

## Prerequisites

Ensure that you have the following installed on your system:
- Java Virtual Machine (JVM) 17
- Gradle 7.5 or the Gradle Wrapper
- Graphviz tools or [online tool](https://www.devtoolsdaily.com/graphviz/#v2=N4IgJg9gLiBc4EsDmAnAhgBwBYAJgB0A7QiMAUxwG0BnLTMgXgFkUyBjCFMAGh2qgCeAG0YAzBEJE8c4yRyGcGAdywIoZXqIiEohNAFtG+EAGE0QhACMUCYwF0A3EVSZclTuRuEkDCAFcoXhJyajIMBgAGADoAZl50QgBrUPCARkciIgBiHABlBH0MEQA5UgoANQgEMAAVAQwyImCyCKohNEsyIQZjSuq6huNeKAgIISgEcN6q2vrGkDtMwhyAEXZ29AntABk1MnQhHH00BEImstSqWnoGSwgAD152zu7jY9OhmQkheUVjLLYgKBnxGYwmUxA7zOCyWOQASmRRPsyIQ2GRduoDjgMF4oLkoF4kOdyAAmK50Bq3B5PDpdHogHGnPEE05EkCab6-FD0gFAwEg0bjSb0xk6fGE+ywnAY-bmHDGURoNgAChiAEpYPKQMSyDFyTc7o8cM86cZ8OaQIqVerNeazeyvnIxn8QLy+QKwcKzRaraqNfKLZKzss8gUimRSuQ8izvDKDjqACywag4AC0AD4cM0YrBg80E21aa8QOLWXHzB6hRDS7G9vGYcGcgAFThQSMUMySHUAVlgGAiybTmeaJNzPb7qQA+gOUxms2Uk3myt3Cy96cBO0IAD7AAA8-fTADE-KitoQdwBBFBIPyGHTUHf7qcRdOpAC+H7flfB9M3QeyOAtigbZlDgm74mg6h3lAOoAGx9jOQ7zuQvZLuQsGrqaIAbuYQgQVBKJQI+B6bgAovcOIft+XqmLh+FkNB-4hgiSKsKi6J1nKopQAAkjoOoAOz6pSho0muxjcXxMEOrIPzOty-x8vyDqglWIq4lJTHwoiyLseWhxWjqAAcwmMKJxpFvShkyZy8k8kpbDURC1mLI20qcYcMQ6gAnKZVJGiaxZeTZToKAproOU59LBa5AH5IUJSgVJ+k6qkiFzs03ljsQFytJQgX0slHlRcYRWYhWDYAUBIFRn+OXkKkqQIYOGVlEZ2XNI1E7Ti1w65R1FyXPllnGDhkjES+x6ngg2iXtet6EQ+e4YM+r6fl+KmCj+xh-pVIbVe2YG4alo79r1yFkAJA0Nadz7nZ1TVoWQqRksN4nYZuE1HiebBnnNN7QUtT7Tmtn4lbRXZ7c2raHeBUCQQxhGpTmZ2zn1N3Xc9epvVhY14fDBE6F9ZEUSgVGbZ6EJwwjjFQzgLG6Wi+nYriNZEvVz0FjQFJmdSFnvdxbOfLJXL2Up4OCzGbKxSGzPeoQvppf6Zoc6kK7cwafMFd68uKxEyuBiFclhWL7oU2pOvakQesG-aMs5PF4aHWzKWq-BaMXWrmOpBhOPFi7xXm9tJZSyldMHaBdWdVdqNIZ1i6pTHd0e518FPakQl++un3LZNP1-cAV4A4txGre+YNBzRu324BMOR3RBOI-xqvtbHrUNVd6cmVno3U4TRG5+mJOURtwxbVXDc00jdMM2xTMeSzTKaarvkayJWsjQyGn8UbouKeLlcQpJO817PKLz+VBlKjqJJ5dc68BZv1kcqFLpusCh9WdfdPM2lN9DffXmj93p-13nZfeZsx6U3pKAmujtEpRjKrKIQN9RwpzKCSR6qDMLFiQfWKBFsQB4IqjXCOtVjocxJCjdK6MyC30xlQ7qNCLoklHE9KhODs64S+lNX6M1zyF3moDUuINy7kwIcHauUoyEdgoSOJMbdaGpCyuwhRyc44YJzKozhvduGD14QXIuC17wiJfGI0eOBVKSOOuHOu5DJD0UYpQ3siiWEJ2cToj6k9+7E1wuREe4M+5N2kqQuxFADH8JvvBVxzQBzsOiU1dBDUGHRJiD1JJZA05RL7GkxJGiGpaOyRgNJaD8nPXavEnJk4cwZJJKhKJnjgARNmoPBEUA-AoEIAMMgW4nzpmKAYHpwAmxoHQIYTES0Pw7kcSXZaaSzG9OKZOVI6YSSLJKemGI6zqnpgTOtcGzToRwLDAgigUluk30ztrIhOgLmf1KrcuYWkcBrDYBsSC-DmaGUod3QB-kxJYWfo6Y2b9Ir3MtN-Y5CUIxJUeYMShq9rnnKeeC5Fgw6avPeWeZmT0Yh3x5v8-mWFoQvxBeFd+ykJE0SOdIsJgFRkGB1DEJqMSMEqKZSyvJ7ddRxI5Y0kZYyvrdMWSsgZhhxGWPHhCAVjKZ46TnhxS+WYmWvT+eZa5JLgV7wigfKlEIaVuXgTCxB5AdAIHEPsJlNSykxDYVazxPFTUTAtSgcGjrCLmoQJan+C90xMq5mqje70-VgJNhAj+er6Qhprr-Jl6tA3AKwqkYWtkw06sgZK6Bxhk10yNYdYhKCOYxHdja+pRbfZIp0GHSNDyoBh1CcBWGEBCijMaEWmOzDsyFPbROe6ZQYjuOzDHUp3Li2YxiFczeG5m0YFbV9bYiIiJ9IAPIYEWSSdMcJkBYCgBKqxVcZ2tueWfPSC9cW-IJeqzemqRbgPTRGzNhCDVxROcas5TrPWWqLVlDJMQKlMsRZvd1ZqXVuo-aBuVrFz6KuQTIa+HMEz4s1om4sQLb1popY5cFLkpQnovrBp6CYAGXqDcSlNr9yVgprSAZ9IY81JXA1611CGR20KI5jBMr0kWMe9dR4DzqmPPNjQh7GCaAXFhzaS7VmHwY5qhU7WFdbioIaTBkhM3b8xc0rUpy+bqq2BwbTVCguQ-CWAJEqM8iYXGdoXHahDLiuVscHQudW1zgAmbM+gPhLT9wvgXaIJdK1N3bt3RY-d1ZTPme87R6Gjb65dgQ9Emz5BEMcYSekspCYy35grVOnOvnvrTRaUY4Ry0y77PBVItyMicBMD8FWCwbAPnaETB2vt5AYjsoQ0ndrZAExZO640urDWEBNYLgV-zgWVlbqQDuvdUr6TDfBI15rMX3JKuTQhi9yHxMwPI2S02D7wt7dza+-N+ndMIZ-Zl-9V2HUXeQXpnTj3bFxajAAVT0CgAQTBTh+GoD2AcrKUtdeaN2PKbnPujJ+39oGB5V2ykIGAebWaQBQ++79wg-3nk1dyF0dgMEObdhZcl3Unceyct6wmcnROWWsYuuDzGxPGl45EL9Ym2gwBqH4SK9MNQsAonXemUiQhQgo8IazgnOO6VtI6U9bsp1SfE6Z1xqdsvOlfXKOYPwZBxfB3V2tmrhyezUN6ySGnYOUaOZYbdy3fYSQZdHVgonKN5m9YV0z7GbnDlfQN8KvpYqhkyvGfsOHDuzFTPc43Ur+55mgz1zRY3r2jO1dIPVttYO1GO9oSSAbmfurW7t-LrTU6mBp5EDuYACIOBcEmW+Hchy4dlxFcs1ZFXqNl7AOnpiX5CAgDfEAA)

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
