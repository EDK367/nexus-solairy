parser grammar PigLatinParser;

options { tokenVocab = PigLatinLexer; }

// ========================
// PROGRAMA
// ========================
program : importSection? varSection? mainSection EOF
        ;

// seccion para importar
importSection : importStmt+
              ;

// importaciones
importStmt : IMPORT path SEMI
           ;

// gramatica de prueba para verificar el path de las importaciones
path : ID (DOT ID)* DOT ID
     | STRING
     ;

// seccion para variables
varSection : VARIABILES GT varDecl*
           ;

// seccion principal obligatoria
mainSection : MAIOR GT statement* FINIS_PROG SEMI
            ;

// ======================================
// DECLARACIONES
// =======================================
varDecl : ESTO ID COLON type expression? SEMI
        | SERIES ID LBRACK expression RBRACK COLON type arrayInit? SEMI
        ;

// ========================
// TIPOS
// ========================
type : NUMERUS
     | TEXTUM
     | DECIMALIS
     | LITTERA
     | BOOL
     | ID              // para usar mismas declaraciones o importaciones
     ;

// inicializar el array
arrayInit : LBRACE expressionList? RBRACE
          ;

// lista de expresiones
expressionList : expression (COMMA expression)*
               ;

// ==============================
// STATEMENT
// ==============================
statement : varDecl
          | assignStmt
          | ifStmt
          | whileStmt
          | doWhileStmt
          | forStmt
          | printStmt
          | readStmt
          | breakStmt
          | continueStmt
          | expression SEMI
          ;

// asignacion
assignStmt : ID ASSIGN expression SEMI
           | ID LBRACK expression RBRACK ASSIGN expression SEMI
           | ID DOT ID ASSIGN expression SEMI
           | ID DOT ID LBRACK expression RBRACK ASSIGN expression SEMI
           ;

// if, else if and else
ifStmt : SI LPAREN expression RPAREN LBRACE statement* RBRACE
         (ALITER LPAREN expression RPAREN LBRACE statement* RBRACE)*
         (ALITER LBRACE statement* RBRACE)?
         FINIS SEMI
       ;

// bucle while
whileStmt : DUM LPAREN expression RPAREN LBRACE statement* RBRACE FINIS SEMI
          ;

// bucle do while
doWhileStmt : FACERE LBRACE statement* RBRACE DUM LPAREN expression RPAREN SEMI
            ;

// bucle for y componentes
forStmt : PER LPAREN forInit? SEMI expression? SEMI forUpdate? RPAREN LBRACE statement* RBRACE
        ;

forInit : ESTO ID COLON type expression
        | ID ASSIGN expression
        ;

forUpdate : ID INC
          | ID DEC
          | ID ASSIGN expression
          ;

// impresion
printStmt : PRINT expression (PRINT expression)* SEMI
          ;

// lectura
readStmt : READ
         | ID READ
         ;

breakStmt : INTERRUMPE SEMI
          ;

continueStmt : PERGE SEMI
             ;

// =================================
// EXPRESIONES
// =================================
expression : orExpression
           ;

// procedencia de valores de menor a mayor
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

postfixExpression : primaryExpression (INC | DEC)?
                  ;

primaryExpression : literal
                  | ID
                  | ID LBRACK expression RBRACK
                  | ID DOT ID
                  | ID DOT ID LBRACK expression RBRACK
                  | ID LPAREN argumentList? RPAREN
                  | ID DOT ID LPAREN argumentList? RPAREN
                  | LPAREN expression RPAREN
                  | structLiteral
                  | NOVUS ID LPAREN argumentList? RPAREN
                  ;

argumentList : expression (COMMA expression)*
             ;

literal : NUMBER
        | DECIMAL
        | STRING
        | CHAR
        | VERUM
        | FALSUS
        ;

structLiteral : LBRACE expressionList? RBRACE
              ;