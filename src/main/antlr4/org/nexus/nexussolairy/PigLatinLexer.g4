lexer grammar PigLatinLexer;

// comentarios y espacios
LINE_COMMENT  : '//' ~[\r\n]*  -> channel(HIDDEN) ;
BLOCK_COMMENT : '##' .*? '##'  -> channel(HIDDEN) ;
WS            : [ \t\r\n]+     -> channel(HIDDEN) ;


// externos
IMPORT  : 'import'  ;
NOVUS   : 'novus'   ;

// encabezados
VARIABILES  : 'VARIABILES'  ;
MAIOR       : 'MAIOR'       ;
FINIS_PROG  : 'FINIS'       ;


// declaraciones
ESTO        : 'esto'        ;
SERIES      : 'series'      ;
FINIS       : 'finis'       ;

// control de flujo
SI         : 'si'           ;
ALITER     : 'aliter'       ;
DUM        : 'dum'          ;
FACERE     : 'facere'       ;
PER        : 'per'          ;
PERGE      : 'perge'        ;
INTERRUMPE : 'interrumpe'   ;

// primitivos
NUMERUS   : 'numerus'   ;
TEXTUM    : 'textum'    ;
DECIMALIS : 'decimalis' ;
LITTERA   : 'littera'   ;
BOOL      : 'bool'      ;

// booleanos
VERUM  : 'verum'    ;
FALSUS : 'falsus'   ;

// negacion universal
NOT : '!'   ;

// operadores logicos
EQ    : '==' ;
NEQ   : '!=' ;
LE    : '<=' ;
GE    : '>=' ;
AND   : '&&' ;
OR    : '||' ;

// manejo de valor
INC   : '++' ;
DEC   : '--' ;

// entrada
READ  : '<<' ;
// SALIDA
PRINT : '>>' ;

// operadores logicos de valor bajo
LT     : '<' ;
GT     : '>' ;

// operadores aritmeticos
PLUS   : '+' ;
MINUS  : '-' ;
MULT   : '*' ;
DIV    : '/' ;

// asignacion
ASSIGN : '=' ;

// puntuacion
COLON  : ':' ;
SEMI   : ';' ;
COMMA  : ',' ;
DOT    : '.' ;

// contenedores
LBRACE : '{' ;
RBRACE : '}' ;
LBRACK : '[' ;
RBRACK : ']' ;
LPAREN : '(' ;
RPAREN : ')' ;

// datos primitivos del sistema
DECIMAL : [0-9]+ '.' [0-9]+             ;
NUMBER  : [0-9]+                        ;
STRING  : '"' (ESC | ~["\\\r\n])* '"'   ;
CHAR    : '\'' (ESC | ~['\\\r\n]) '\''  ;

// fragmentacion en cadenas
fragment ESC : '\\' [btnrf"'\\] ;

// identificador
ID : [a-zA-Z_][a-zA-Z0-9_]* ;