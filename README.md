# Nexus Solairy — Entorno de Compilación y Ejecución C3D

[![Java Version](https://img.shields.io/badge/Java-21-orange.svg?style=flat-square&logo=openjdk)](https://openjdk.org/)
[![JavaFX](https://img.shields.io/badge/JavaFX-21.0.6-blue.svg?style=flat-square)](https://openjfx.io/)
[![ANTLR4](https://img.shields.io/badge/ANTLR-4.13.2-red.svg?style=flat-square)](https://www.antlr.org/)
[![Maven](https://img.shields.io/badge/Maven-3.9+-C71A36.svg?style=flat-square&logo=apache-maven)](https://maven.apache.org/)
[![GCC](https://img.shields.io/badge/GCC-C99-00599C.svg?style=flat-square&logo=gnu)](https://gcc.gnu.org/)
[![License](https://img.shields.io/badge/License-MIT-green.svg?style=flat-square)](#)

**Nexus Solairy** es un Entorno de Desarrollo Integrado (IDE), compilador, analizador semántico, máquina virtual y generador de **Código de Tres Direcciones (C3D)** y **C nativo (GCC)** para lenguajes de alto nivel: **Y?** (`.y`), **Zetariano** (`.z`) y **PigLatin** (`.pig`). El sistema implementa un pipeline de compilación completo: análisis léxico, sintáctico con control estricto de identación (`INDENT`/`DEDENT`), análisis semántico con verificación de tipos y tablas de símbolos con ámbitos anidados, descomposición en cuartetas C3D (`Quadruple`), una Máquina Virtual de C3D con simulación de **Stack** y **Heap** en memoria lineal, un transpilador a código C estándar con compilación y ejecución directa vía **GCC**, y un sistema riguroso de control de errores.

---

## Tabla de Contenidos

1. [Características Principales](#-características-principales)
2. [Arquitectura y Pipeline de Compilación](#-arquitectura-y-pipeline-de-compilación)
3. [Especificación de los Lenguajes Soportados](#-especificación-de-los-lenguajes-soportados)
   - [Lenguaje Y? (`.y`)](#lenguaje-y-y)
   - [Lenguaje Zetariano (`.z`)](#lenguaje-zetariano-z)
   - [Lenguaje PigLatin (`.pig`)](#lenguaje-piglatin-pig)
   - [Tipos de Datos y Jerarquía Semántica](#tipos-de-datos-y-jerarquía-semántica)
4. [Código de Tres Direcciones (C3D) y Modelo de Ejecución](#-código-de-tres-direcciones-c3d-y-modelo-de-ejecución)
   - [Estructura de Cuartetas](#estructura-de-cuartetas)
   - [Máquina Virtual C3D y Layout de Memoria (Stack y Heap)](#máquina-virtual-c3d-y-layout-de-memoria-stack-y-heap)
   - [Generador de Código C y Compilación Nativa (GCC)](#generador-de-código-c-y-compilación-nativa-gcc)
5. [Módulos y Componentes del Sistema](#-módulos-y-componentes-del-sistema)
   - [Editor Avanzado con Resaltado de Sintaxis](#editor-avanzado-con-resaltado-de-sintaxis)
   - [Inspector de Memoria en Vivo (Stack y Heap)](#inspector-de-memoria-en-vivo-stack-y-heap)
   - [Depurador y Visor de Cuartetas C3D](#depurador-y-visor-de-cuartetas-c3d)
   - [Control Integral de Errores y Diagnóstico](#control-integral-de-errores-y-diagnóstico)
   - [Tabla de Símbolos y Árbol AST](#tabla-de-símbolos-y-árbol-ast)
   - [Terminal Integrada y Salida de Ejecución](#terminal-integrada-y-salida-de-ejecución)
6. [Estructura del Proyecto](#-estructura-del-proyecto)
7. [Requisitos del Sistema](#-requisitos-del-sistema)
8. [Compilación y Ejecución](#-compilación-y-ejecución)
   - [Ejecución en Desarrollo (Maven)](#ejecución-en-desarrollo-maven)
   - [Construcción del Fat-JAR](#construcción-del-fat-jar)
   - [Ejecución del JAR](#ejecución-del-jar)
9. [Empaquetado Nativo con `jpackage`](#-empaquetado-nativo-con-jpackage)
10. [Ejemplos de Código](#-ejemplos-de-código)
11. [Licencia](#-licencia)

---

## Características Principales

- **Analizadores Multi-Lenguaje con ANTLR 4.13.2:** Gramáticas independientes y optimizadas para **Y?**, **Zetariano** y **PigLatin**, reportando posición precisa de errores (línea y columna).
- **Control Estricto de Identación:** Procesador sintáctico en `YIdentationLexer` para el manejo de bloques basados en espacios (`INDENT` y `DEDENT`), detectando desalineaciones de identación al nivel de desidentación exterior.
- **Verificación Semántica y Tabla de Símbolos:** Resolución de ámbitos globales y locales (`GlobalScope`, `LocalScope`), verificación estricta de tipos (`TypeChecker`), validación de firmas, recursividad y accesos a miembros.
- **Generación de Código de Tres Direcciones (C3D):** Descomposición completa de expresiones, estructuras condicionales, ciclos y llamadas a funciones en cuartetas atómicas (`Quadruple`).
- **Máquina Virtual C3D en Memoria Lineal:** Simulación fiel de bajo nivel con arreglos de `stack` y `heap`, punteros de pila `P` y montículo `H`, y registros temporales.
- **Inspector de Memoria en Tiempo Real:** Visualización en tablas dinámicas de las posiciones de memoria Stack y Heap con dirección física, tipo, valor y descripción del símbolo o referencia.
- **Transpilador y Compilador C (GCC):** Generación automática de código C99 equivalente y compilación nativa en caliente mediante GCC con captura de salida en la consola del IDE.
- **Control Total de Errores:** Si se detecta cualquier error léxico, sintáctico, de identación o semántico, el compilador bloquea de inmediato la generación de C3D, la generación de C y la ejecución del programa.
- **Interfaz Moderna en JavaFX:** Diseñada con `RichTextFX`, `BootstrapFX`, `ControlsFX` e `Ikonli`.

---

## Arquitectura y Pipeline de Compilación

El procesamiento de código fuente se orquesta a través de `AnalysisPipeline`:

```
                  ┌──────────────────────────────────────────────┐
                  │      Código Fuente (.y / .z / .pig)          │
                  └──────────────────────┬───────────────────────┘
                                         │
                                         ▼
                  ┌──────────────────────────────────────────────┐
                  │       Lexer Factory (ANTLR4 Lexers)          │
                  │   (YLexer / ZetarianoLexer / PigLatinLexer)  │
                  └──────────────────────┬───────────────────────┘
                                         │ Tokens / Errores Léxicos
                                         ▼
                  ┌──────────────────────────────────────────────┐
                  │      Parser Factory & YIdentationLexer       │
                  │  (YParser / ZetarianoParser / PigLatinParser)│
                  └──────────────────────┬───────────────────────┘
                                         │ Parse Tree / Errores Sintácticos & Identación
                                         ▼
                  ┌──────────────────────────────────────────────┐
                  │   Semantic Analysis (Visitors & TypeChecker) │
                  │  (Tabla de Símbolos / Ámbitos / Validaciones)│
                  └──────────────────────┬───────────────────────┘
                                         │
                   ¿Existen Errores? ────┴────┐
                                 │ Sí         │ No
                                 ▼            ▼
                  ┌─────────────────────┐  ┌────────────────────────────────────┐
                  │  Bloqueo Total de   │  │    Generación C3D (C3DVisitors)    │
                  │ Ejecución y Código  │  │   (Cuartetas, Temporales, Labels)  │
                  │  (Tablas de Error & │  └─────────────────┬──────────────────┘
                  │   Terminal Info)    │                    │
                  └─────────────────────┘                    ▼
                                           ┌─────────────────┴──────────────────┐
                                           │                                    │
                                           ▼                                    ▼
                                ┌─────────────────────┐              ┌─────────────────────┐
                                │ C3D Virtual Machine │              │ CSourceCodeGenerator│
                                │   (Stack & Heap)    │              │  (Transpilador C99) │
                                └──────────┬──────────┘              └──────────┬──────────┘
                                           │                                    │
                                           ▼                                    ▼
                                ┌─────────────────────┐              ┌─────────────────────┐
                                │ Memoria & Registros │              │ GCC Native Compiler │
                                │ (UI Stack / Heap)   │              │   (Ejecución GCC)   │
                                └─────────────────────┘              └─────────────────────┘
```

---

## Especificación de los Lenguajes Soportados

### Lenguaje Y? (`.y`)

Inspirado en la sintaxis de Python, estructurado por bloques con identación obligatoria y sensibilidad a mayúsculas/minúsculas (*case-sensitive*). No admite variables globales libres fuera de funciones.

- **Estructura Base:**
  - Sección opcional de estructuras globales: `%estructuras`
  - Sección obligatoria de funciones: `%funciones`

- **Paso de Parámetros:**
  - Primitivos: Paso por **valor**.
  - Arreglos: Paso exclusivamente por **referencia** con la notación `[] tipo nombre`. Aplanados a bajo nivel.
  - Estructuras: Paso exclusivamente por **referencia** con la notación `{}` o `{ tipo }`.

```python
%estructuras
estructura Persona:
    cadena nombre
    entero edad
    flotante promedio
    entero calificaciones[5]

%funciones
definir procesarDatos([] entero miArray, {} Persona persona):
    persona.edad = 25
    miArray[0] = 100

definir main():
    entero notas[5] = {90, 80, 70, 85, 95}
    Persona alumno
    alumno.nombre = "Zetariano"
    procesarDatos(notas, alumno)
    imprimir("Proceso completado")
```

- **Estructuras de Control:**
  - Condicionales: `si (condicion) entonces` ... `sino (condicion) entonces` ... `contrario`
  - Selección: `elegir (opcion) { caso 1: ... romper caso 2: ... romper siempre: ... romper }`
  - Ciclos:
    - `para (entero i = 0; i < 10; i++):`
    - `mientras (contador < 5) hacer:`
    - `hacer:` ... `mientras (condicion)`

---

### Lenguaje Zetariano (`.z`)

Lenguaje orientado a objetos inspirado en Java. Cada archivo debe llamarse exactamente como la clase pública definida en su interior (ej. `Persona.z`).

- **Objetos en el Heap:** Las instancias de clases y estructuras se reservan dinámicamente en el Heap con puntero `H`.
- **Características:**
  - Declaración de atributos y métodos.
  - Constructores y sobrecarga de constructores/métodos.
  - Soporte para recursividad completa.
  - Expresiones aritméticas, operadores lógicos y operador ternario (`cond ? a : b`).
  - Arreglos con inicialización estática o dinámica.

```java
public class Calculadora {
    int factor;

    public Calculadora(int factorInicial) {
        factor = factorInicial;
    }

    public int factorial(int n) {
        if (n <= 1) {
            return 1;
        }
        return n * factorial(n - 1);
    }

    public static void main() {
        Calculadora calc = new Calculadora(5);
        int resultado = calc.factorial(calc.factor);
        println("Resultado del factorial: " + resultado);
    }
}
```

---

### Lenguaje PigLatin (`.pig`)

Lenguaje estructurado de la Resistencia que permite modularidad mediante directivas de importación (`import archivo.pig;`). Proporciona definiciones estructuradas de variables y algoritmos para transpilación directa hacia C3D.

---

### Tipos de Datos y Jerarquía Semántica

| Tipo | Y? | Zetariano | PigLatin | Representación en C3D |
| :--- | :--- | :--- | :--- | :--- |
| **Entero** | `entero` | `int` | `int` | Valor numérico entero de 64 bits en Stack / Heap |
| **Flotante / Decimal** | `flotante` | `float`, `double` | `float` | Valor de punto flotante de 64 bits |
| **Booleano** | `bool` (`verdadero`/`falso`) | `boolean` (`true`/`false`) | `bool` | `1.0` (verdadero) / `0.0` (falso) |
| **Carácter** | `caracter` (`'A'`) | `char` (`'A'`) | `char` | Valor numérico ASCII |
| **Cadena** | `cadena` (`"..."`) | `String` (`"..."`) | `string` | Puntero a dirección base en el Heap (secuencia de caracteres) |
| **Arreglo** | `tipo id[size]` | `tipo[] id` | `tipo id[size]` | Dirección base en Heap con elementos contiguos |
| **Objeto / Estructura**| `estructura ID` | `class ID` | `struct ID` | Bloque continuo en Heap indexado por desplazamiento (*offset*) |

---

## Código de Tres Direcciones (C3D) y Modelo de Ejecución

### Estructura de Cuartetas

Cada instrucción C3D se modela mediante la clase `Quadruple`:
$$\text{Quadruple} = \langle \text{Operador}, \text{Argumento 1}, \text{Argumento 2}, \text{Resultado} \rangle$$

Operadores soportados:
- **Aritméticos y Lógicos:** `PLUS`, `MINUS`, `MULT`, `DIV`, `MOD`, `AND`, `OR`, `NOT`
- **Asignación:** `ASSIGN`
- **Control de Flujo:** `GOTO`, `IF_EQ`, `IF_NEQ`, `IF_LT`, `IF_GT`, `IF_LE`, `IF_GE`, `LABEL`
- **Llamadas a Funciones:** `CALL`, `PARAM`, `RETURN`, `FUNCTION`, `END_FUNCTION`
- **Memoria:** `STACK_SET`, `STACK_GET`, `HEAP_SET`, `HEAP_GET`
- **I/O y Control:** `PRINT`, `READ`, `HALT`

### Máquina Virtual C3D y Layout de Memoria (Stack y Heap)

La `C3DVirtualMachine` ejecuta el conjunto de cuartetas sobre dos regiones de memoria lineales:
- **Stack (`stack[100000]`):** Registro de activación para funciones, parámetros, valores de retorno y variables locales. Administrado con el puntero de pila `P`.
- **Heap (`heap[100000]`):** Almacenamiento persistente para cadenas, arreglos y objetos instanciados. Administrado con el puntero libre `H`.

La UI del IDE incluye un **Inspector de Memoria** que lista todas las direcciones escritas en el Stack y el Heap, identificando el tipo de dato, valor y nombre del símbolo en tiempo real.

### Generador de Código C y Compilación Nativa (GCC)

El componente `CSourceCodeGenerator` toma el programa C3D y genera código C99 estándar con la cabecera:

```c
#include <stdio.h>

double stack[100000];
double heap[100000];
double P = 0;
double H = 0;
double t0, t1, t2; // Temporales

int main() {
    // Cuartetas traducidas a C estándar
    return 0;
}
```

El botón **Compilar en C (GCC)** utiliza `GCCCompilerService` para compilar el código resultante de forma silenciosa e invocar el binario generado, retransmitiendo la salida estándar directamente a la terminal del IDE.

---

## Módulos y Componentes del Sistema

### Editor Avanzado con Resaltado de Sintaxis
- Editor multilineal basado en `CodeArea` (RichTextFX) con numeración de líneas.
- Resaltado en tiempo real según el lenguaje activo (`.y`, `.z`, `.pig`).
- Pestañas independientes con gestión de proyectos y archivos.

### Inspector de Memoria en Vivo (Stack y Heap)
- Tablas independientes para el **Stack** y el **Heap** que muestran:
  - **Dirección de Memoria** (`0`, `1`, `2`, ...)
  - **Tipo de Dato** (`ENTERO`, `FLOTANTE`, `CADENA_REF`, `OBJETO_REF`, etc.)
  - **Valor Numérico o Contenido**
  - **Detalle del Símbolo** (nombre de variable, miembro de estructura o índice de arreglo)
- Modos de visualización completa sin paginación para un control total del espacio ocupado.

### Depurador y Visor de Cuartetas C3D
- Visualización de todas las cuartetas generadas con número de instrucción, operación, argumentos y destino.
- Controles de paso a paso (*Step*) y rebobinado (*Previous Step*) en la máquina virtual para observar la evolución de la memoria instrucción por instrucción.

### Control Integral de Errores y Diagnóstico
- Reporte unificado de fallos en la pestaña **PROBLEMS**:
  - **Léxicos:** Caracteres inválidos o literales malformados.
  - **Sintácticos:** Errores de gramática y tokens inesperados.
  - **Identación:** Desalineación de bloques `INDENT`/`DEDENT` en lenguaje Y?.
  - **Semánticos:** Identificadores no declarados, incompatibilidad de tipos y errores de invocación.
- **Garantía de Integridad:** Si existe al menos un error, el compilador bloquea la generación de C3D, anula el código C, limpia los visores de Stack/Heap y previene cualquier ejecución.

### Tabla de Símbolos y Árbol AST
- Explorador interactivo de símbolos con filtros por nombre, ámbito (`GLOBAL`, `LOCAL`), tipo y categoría (`VARIABLE`, `FUNCION`, `ESTRUCTURA`, `CLASE`).

### Terminal Integrada y Salida de Ejecución
- Consola integrada con soporte para salidas categorizadas: `INFO>`, `OK>`, `ERROR>`, `PRINT` y `GCC`.

---

## Estructura del Proyecto

```
nexus-solairy/
├── pom.xml                                   # Configuración de dependencias y plugins Maven
├── mvnw / mvnw.cmd                           # Wrappers de Maven
└── src/
    └── main/
        ├── antlr4/org/nexus/nexussolairy/
        │   ├── YLexer.g4                     # Gramática léxica para lenguaje Y?
        │   ├── YParser.g4                    # Gramática sintáctica para lenguaje Y?
        │   ├── ZetarianoLexer.g4             # Gramática léxica para lenguaje Zetariano
        │   ├── ZetarianoParser.g4            # Gramática sintáctica para lenguaje Zetariano
        │   ├── PigLatinLexer.g4              # Gramática léxica para lenguaje PigLatin
        │   └── PigLatinParser.g4             # Gramática sintáctica para lenguaje PigLatin
        ├── java/
        │   ├── module-info.java              # Configuración modular de JavaFX y dependencias
        │   └── org/nexus/nexussolairy/
        │       ├── Launcher.java             # Entry point para ejecución Fat-JAR
        │       ├── NexuSolairyApp.java       # Entrada principal de la aplicación JavaFX
        │       ├── backend/
        │       │   ├── c/                    # Transpilador C (CSourceCodeGenerator) y servicio GCC
        │       │   └── vm/                   # Máquina Virtual C3D (C3DVirtualMachine)
        │       ├── c3d/                      # Generación de cuartetas (YC3DVisitor, ZetarianoC3DVisitor, MemoryLayout)
        │       ├── controller/               # Controlador de la interfaz gráfica (MainController)
        │       ├── model/                    # Modelos de C3D, análisis semántico, tokens y vistas
        │       ├── patron/                   # Factorías (LexerFactory, ParserFactory, VisitorFactory)
        │       ├── service/
        │       │   ├── analysis/             # Orquestación del pipeline (AnalysisPipeline, ImportResolver)
        │       │   ├── grammar/              # Servicios léxicos y listeners de error
        │       │   ├── parser/               # Servicios sintácticos y YIdentationLexer
        │       │   └── ui/                   # Gestión de archivos, proyectos y workspace
        │       ├── ui/                       # Resaltado léxico y temas (SintaxColor)
        │       ├── utils/                    # Clases auxiliares y contenedores de resultados
        │       ├── view/                     # Diálogos modales y componentes visuales
        │       └── visitor/                  # Intérpretes semánticos y evaluadores por lenguaje
        └── resources/org/nexus/nexussolairy/
            ├── view/MainView.fxml            # Definición de la interfaz gráfica en FXML
            └── styles/styles.css             # Hojas de estilo CSS del IDE
```

---

## Requisitos del Sistema

- **Java Development Kit (JDK):** Versión **21** o superior (ej. OpenJDK 21, Eclipse Temurin 21).
- **Apache Maven:** Versión **3.8+** (o utilizar el wrapper incluido `./mvnw`).
- **Compilador GCC:** Requerido para la compilación y ejecución nativa del código C generado (`gcc` en el `PATH`).
- **Sistema Operativo:** Linux (probado en Debian/Ubuntu/Fedora), macOS o Windows (con MinGW / MSYS2 / GCC).

---

## Compilación y Ejecución

### Ejecución en Desarrollo (Maven)

Para compilar las gramáticas ANTLR4 y ejecutar la interfaz gráfica directamente:

```bash
./mvnw clean javafx:run
```

O si tienes Maven instalado en el sistema:

```bash
mvn clean javafx:run
```

### Construcción del Fat-JAR

El proyecto utiliza `maven-shade-plugin` para empaquetar todas las dependencias (JavaFX, ANTLR4, RichTextFX, BootstrapFX, etc.) en un único archivo JAR ejecutable con `org.nexus.nexussolairy.Launcher` como clase principal:

```bash
./mvnw clean package
```

El archivo JAR generado se ubicará en:
```
target/nexus-solairy-1.0-SNAPSHOT.jar
```

### Ejecución del JAR

```bash
java -jar target/nexus-solairy-1.0-SNAPSHOT.jar
```

---

## Empaquetado Nativo con `jpackage`

Para crear una imagen de aplicación autocontenida (*app-image*) que incluya el runtime de Java embebido:

### 1. Limpiar y empaquetar el JAR

```bash
rm -rf dist
./mvnw clean package
```

### 2. Generar el paquete nativo

```bash
jpackage \
  --type app-image \
  --name NexusSolairy \
  --app-version 1.0 \
  --input target \
  --main-jar nexus-solairy-1.0-SNAPSHOT.jar \
  --main-class org.nexus.nexussolairy.Launcher \
  --dest dist
```

### 3. Ejecutar la aplicación autocontenida

- **En Linux:**
  ```bash
  ./dist/NexusSolairy/bin/NexusSolairy
  ```
- **En Windows:**
  ```cmd
  dist\NexusSolairy\NexusSolairy.exe
  ```
- **En macOS:**
  ```bash
  open dist/NexusSolairy.app
  ```

---

## Ejemplos de Código

### Ejemplo 1: Operaciones y Control de Flujo en Y? (`ejemplo.y`)

```python
%estructuras
estructura Contador:
    entero total

%funciones
definir main():
    entero suma = 0
    para(entero i = 0; i < 5; i++):
        suma = suma + i
    imprimir("Suma calculada: ")
    imprimir(suma)
```

### Ejemplo 2: Clases y Recursividad en Zetariano (`Persona.z`)

```java
public class Persona {
    String nombre;
    int edad;

    public Persona(String nombreParametro, int edadParametro) {
        nombre = nombreParametro;
        edad = edadParametro;
    }

    public void saludar() {
        println("Hola, soy " + nombre + " y tengo " + edad + " anios.");
    }

    public static void main() {
        Persona p = new Persona("Zetariano", 100);
        p.saludar();
    }
}
```

---

## Licencia

Este proyecto ha sido desarrollado con fines académicos en el área de diseño de compiladores, máquinas virtuales y generación de código intermedio.
