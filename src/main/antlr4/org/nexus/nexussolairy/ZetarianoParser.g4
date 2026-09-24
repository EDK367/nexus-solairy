parser grammar ZetarianoParser;

options { tokenVocab = ZetarianoLexer; }

// ===========================
// PROGRAMA
// ============================
program : classDecl+ EOF
        ;

// seccion para declarar la clase (se esta viendo si se deja el +)
classDecl : PUBLIC CLASS ID LBRACE classBody RBRACE
          ;

// el cuerpo de la clase
classBody : (fieldDecl | constructorDecl | methodDecl)*
          ;

// tipo de declaraciones
fieldDecl : (PUBLIC | PRIVATE)? type ID SEMI
          | (PUBLIC | PRIVATE)? type ID ASSIGN expression SEMI
          | (PUBLIC | PRIVATE)? type ID LBRACK RBRACK (ASSIGN expression)? SEMI
          | (PUBLIC | PRIVATE)? type ID LBRACK RBRACK LBRACK RBRACK (ASSIGN expression)? SEMI // declaracion para matrices, posibles cambios
          ;

// declaracion del constructor
constructorDecl : (PUBLIC | PRIVATE)? ID LPAREN paramList? RPAREN block
                ;

// declaracion de funciones con o sin retorno
methodDecl : (PUBLIC | PRIVATE)? type ID LPAREN paramList? RPAREN block // con retorno
           | (PUBLIC | PRIVATE)? VOID ID LPAREN paramList? RPAREN block // sin retorno
           ;

// parametros
paramList : param (COMMA param)*
          ;

param : type ID
      | type ID LBRACK RBRACK // arreglos
      | type LBRACK RBRACK ID
      ;

// blockes con { }
block : LBRACE statement* RBRACE
      ;

// statement con posibles cambios
statement : varDecl
          | assignStmt
          | ifStmt
          | switchStmt
          | forStmt
          | whileStmt
          | doWhileStmt
          | returnStmt
          | breakStmt
          | continueStmt
          | printStmt
          | readStmt
          | expression SEMI
          | block
          | SEMI
          ;

// ======================================
// DECLARACIONES
// =======================================
varDecl : type ID SEMI
        | type ID ASSIGN expression SEMI
        | type (LBRACK RBRACK)+ ID (ASSIGN expression)? SEMI
        ;

// ======================================
// ASIGNACIONES
// =======================================
assignStmt : leftValue ASSIGN expression SEMI
           | leftValue ADD_ASSIGN expression SEMI
           | leftValue SUB_ASSIGN expression SEMI
           | leftValue MUL_ASSIGN expression SEMI
           | leftValue DIV_ASSIGN expression SEMI
           | leftValue INC SEMI
           | leftValue DEC SEMI
           ;

// valores de izquierda
leftValue : ID
          | ID LBRACK expression RBRACK
          | ID DOT ID
          | ID DOT ID LBRACK expression RBRACK
          | chainedAccess
          ;

chainedAccess : ID ( (DOT ID) | (LBRACK expression RBRACK) )+
              ;

// statement if
ifStmt : IF LPAREN expression RPAREN ifBody
         (ELSE IF LPAREN expression RPAREN ifBody)*
         (ELSE ifBody)?
       ;

// cuerpo del if (acepta instruccion de una linea
ifBody : block
       | statement
       ;

// statement de switch
switchStmt : SWITCH LPAREN expression RPAREN LBRACE caseBranch* defaultBranch? RBRACE
           ;

// casos para el switch
caseBranch : CASE expression COLON statement*
           ;

// default para el switch
defaultBranch : DEFAULT COLON statement*
              ;

// statement de bucle for
forStmt : FOR LPAREN forInit? SEMI expression? SEMI forUpdate? RPAREN statement
        ;

// inicializar el valor del for
forInit : type ID (ASSIGN expression)?
        | ID ASSIGN expression
        ;

// actualizacion del for
forUpdate : leftValue INC
          | leftValue DEC
          | leftValue ASSIGN expression
          | leftValue ADD_ASSIGN expression
          | leftValue SUB_ASSIGN expression
          | leftValue MUL_ASSIGN expression
          | leftValue DIV_ASSIGN expression
          ;

// statement de bucle while
whileStmt : WHILE LPAREN expression RPAREN block
          ;

// statement de bucle do while
doWhileStmt : DO block WHILE LPAREN expression RPAREN SEMI
            ;

// impresion
printStmt : PRINT LPAREN expression RPAREN SEMI
          | PRINTLN LPAREN expression RPAREN SEMI
          ;

// lectura / no funcional
readStmt : READ LPAREN RPAREN SEMI;

// retorno para las funciones
returnStmt : RETURN expression? SEMI
           ;
// break para bucles
breakStmt : BREAK SEMI
          ;

// continuar para bucles
continueStmt : CONTINUE SEMI
             ;

// ======================================
// TIPOS DE DATOS
// ======================================
type : INT
     | DOUBLE
     | CHAR_TYPE
     | BOOLEAN
     | STRING_TYPE
     | ID
     ;

// ================================
// EXPRESIONES
// ================================
expression : conditionalExpr
     ;

conditionalExpr : orExpr (QUESTION expression COLON expression)?
                ;

orExpr : andExpr (OR andExpr)*
       ;

andExpr : eqExpr (AND eqExpr)*
        ;

eqExpr : relExpr ((EQ | NEQ) relExpr)*
       ;

relExpr : addExpr ((LT | GT | LE | GE) addExpr)*
        ;

addExpr : mulExpr ((PLUS | MINUS) mulExpr)*
        ;

mulExpr : unaryExpr ((MULT | DIV | MOD) unaryExpr)*
        ;

unaryExpr : INC unaryExpr
          | DEC unaryExpr
          | PLUS unaryExpr
          | MINUS unaryExpr
          | NOT unaryExpr
          | postfixExpr
          ;

postfixExpr : primaryExpr
            | postfixExpr DOT ID
            | postfixExpr DOT ID LPAREN argList? RPAREN
            | postfixExpr LBRACK expression RBRACK
            | postfixExpr INC
            | postfixExpr DEC
            ;

primaryExpr : literal
            | NULL
            | READ LPAREN RPAREN
            | ID
            | ID LPAREN argList? RPAREN
            | LPAREN expression RPAREN
            | newExpr
            | arrayInit
            ;

newExpr : NEW type LPAREN argList? RPAREN
        | NEW type LBRACK expression RBRACK (LBRACK expression RBRACK)*
        ;

arrayInit : LBRACE argList? RBRACE
          ;

argList : expression (COMMA expression)*
        ;

// =========================
// DATOS PRIMITIVOS
// =========================
literal : NUMBER
        | DECIMAL
        | STRING
        | CHAR
        | TRUE
        | FALSE
        ;







