parser grammar YParser;

options { tokenVocab = YLexer; }

// ===========================
// PROGRAMA
// ============================
program : newLine* structSection? newLine* funcSection newLine* EOF
        ;

// seccion para la estructuras (opcional)
structSection : SEC_STRUCT newLine* (structDef newLine*)*
              ;

// seccion para la funciones (obligatoria)
funcSection : SEC_FUNCTION newLine* (funcDef newLine*)*
            ;

// definicion para la estructura
structDef : ESTRUCTURA ID COLON newLine+ INDENT structField+ DEDENT
          ;

structField : type ID newLine+
            | type ID LBRACK expression RBRACK newLine+
            | ID ID newLine+    // manejar estructuras anidadas
            ;

// se puede tener dos funciones
funcDef : voidFunction
        | returnFunction
        ;

// funcion sin retorno
voidFunction : DEFINIR ID LPAREN parameterList? RPAREN COLON newLine+ block
             ;

// funcion con retorno
returnFunction : DEFINIR ID LPAREN parameterList? RPAREN RETURN_VALUE type COLON newLine+ block
               ;

// parametros
parameterList : parameter (COMMA parameter)*
              ;

// metodos de parametros
parameter : type ID
          | LBRACK RBRACK type ID    // para pasar los arreglos
          | LBRACE RBRACE ID ID      // pasar struct
          ;

// ======================================
// DECLARACIONES
// =======================================
varDecl : type ID (ASSIGN expression)?
        | type ID (LBRACK expression RBRACK)+ (ASSIGN arrayInit)?
        | ID ID (ASSIGN structLiteral)?
        ;

// arreglos con valores
arrayInit : LBRACE expressionList? RBRACE
          ;

// struct pendiente para probar
structLiteral : LBRACE expressionList? RBRACE
              ;

// blockes con identacion (dentro del codigo)
block : INDENT statement+ DEDENT
      ;

// ===========================
// STATEMENT
//============================
statement : varDecl newLine+
          | assignStmt newLine+
          | ifStmt
          | switchStmt
          | forStmt
          | whileStmt
          | doWhileStmt newLine+
          | printStmt newLine+
          | readStmt newLine+
          | returnStmt newLine+
          | breakStmt newLine+
          | continueStmt newLine+
          | structDef
          | newLine
          ;

// asignaciones
assignStmt : target ASSIGN expression
           | target INC
           | target DEC
           ;

// =================================================================================
// TARGET / MANEJA LAS MANERAS EN QUE UN ID PUEDE RECIBIR ASIGNACIONES
// =================================================================================
target : ID
       | ID (LBRACK expression RBRACK)+
       | ID (DOT ID)+
       | ID (DOT ID)+ (LBRACK expression RBRACK)+
       | chainedTarget
       ;

chainedTarget : ID ( (DOT ID) | (LBRACK expression RBRACK) )+
              ;

// statement if
ifStmt : SI LPAREN expression RPAREN ENTONCES newLine+ block
        (SINO LPAREN expression RPAREN ENTONCES newLine+ block)*
        (CONTRARIO newLine+ block)?
        ;

// statement de switch
switchStmt : ELEGIR LPAREN expression RPAREN COLON newLine+ INDENT
            caseBranch+ defaultBranch? DEDENT
           ;

// casos para el switch
caseBranch : CASO expression COLON newLine+ block
           ;

// default para el switch
defaultBranch : SIEMPRE COLON newLine+ block
              ;

// statement de bucle for
forStmt : PARA LPAREN forInit? SEMI expression? SEMI forUpdate? RPAREN COLON newLine+ block
        ;

// inicializar el valor del for
forInit : type ID ASSIGN expression
        | target ASSIGN expression
        ;

// actualizacion del for
forUpdate : target INC
          | target DEC
          | target ASSIGN expression
          ;

// statement de bucle while
whileStmt : MIENTRAS LPAREN expression RPAREN HACER newLine+ block
          ;

// statement de bucle do while
doWhileStmt : HACER COLON newLine+ block MIENTRAS LPAREN expression RPAREN
            ;

// impresion
printStmt : IMPRIMIR LPAREN expression RPAREN
          ;

// lectura / no funcional
readStmt : LEER LPAREN RPAREN
         ;

// retorno para las funciones
returnStmt : RETORNAR expression?
           ;

// break para bucles
breakStmt : ROMPER
          ;

// continuar para bucles
continueStmt : CONTINUAR SEMI?
             ;

// ======================================
// TIPOS DE DATOS
// ======================================
type : ENTERO
     | FLOTANTE
     | CADENA
     | CARACTER
     | BOOL
     | ID
     ;


// ================================
// EXPRESIONES
// ================================
expression : orExpression
           ;

orExpression : andExpression (OR andExpression)*
             ;

andExpression : relationalExpression (AND relationalExpression)*
              ;

relationalExpression : additiveExpression ((EQ | NEQ | LE | GE | LT | GT) additiveExpression)*
                     ;

additiveExpression : multiplicativeExpression ((PLUS | MINUS) multiplicativeExpression)*
                   ;

multiplicativeExpression : unaryExpression ((MULT | DIV) unaryExpression)*
                         ;

unaryExpression : NOT unaryExpression
                | MINUS unaryExpression
                | postfixExpression
                ;

postfixExpression : primaryExpression
                  | postfixExpression DOT ID
                  | postfixExpression DOT ID LPAREN argumentList? RPAREN
                  | postfixExpression LBRACK expression RBRACK
                  | postfixExpression (INC | DEC)
                  ;

primaryExpression : literal
                  | LEER LPAREN RPAREN
                  | target
                  | ID LPAREN argumentList? RPAREN
                  | LPAREN expression RPAREN
                  | structLiteral
                  ;

argumentList : expression (COMMA expression)*
             ;

expressionList : expression (COMMA expression)*
               ;

// =========================
// DATOS PRIMITIVOS
// =========================
literal : NUMBER
        | DECIMAL
        | STRING
        | CHAR
        | VERDADERO
        | FALSO
        ;

// ===========================================
// MANEJO DE SALTOS DE LINEA
// ===========================================
newLine : NEWLINE   ;