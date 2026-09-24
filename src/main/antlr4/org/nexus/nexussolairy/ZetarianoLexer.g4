lexer grammar ZetarianoLexer;

// comentarios y espacios
LINE_COMMENT  : '//' ~[\r\n]*  -> channel(HIDDEN) ;
BLOCK_COMMENT : '/*' .*? '*/'  -> channel(HIDDEN) ;
WS            : [ \t\r\n]+     -> channel(HIDDEN) ;

// encabezados
PUBLIC      : 'public'  ;
PRIVATE     : 'private' ;
CLASS       : 'class'   ;
VOID        : 'void'    ;
NEW         : 'new'     ;

// CONTROL DE FLUJO
// control de flujo principales
IF          : 'if'      ;
ELSE        : 'else'    ;
SWITCH      : 'switch'  ;
CASE        : 'case'    ;
DEFAULT     : 'default' ;
FOR         : 'for'     ;
WHILE       : 'while'   ;
DO          : 'do'      ;
// control de flujo interno
RETURN      : 'return'  ;
CONTINUE    : 'continue';
BREAK       : 'break'   ;

// ESPECIALES
// lectura
READ    : 'readln'  ;
// escritura
PRINT   : 'print'   ;
PRINTLN : 'println' ;


// primitivos
INT         : 'int'     ;
DOUBLE      : 'double'  ;
CHAR_TYPE   : 'char'    ;
BOOLEAN     : 'boolean' ;
STRING_TYPE : 'String'  ;

// booleanos
TRUE    : 'true'    ;
FALSE   : 'false'   ;

// null
NULL    : 'null'    ;

// manejo de valor con asignacion
ADD_ASSIGN  : '+='  ;
SUB_ASSIGN  : '-='  ;
MUL_ASSIGN  : '*='  ;
DIV_ASSIGN  : '/='  ;

// LOGICOS
NOT : '!'   ;
AND : '&&'  ;
OR  : '||'  ;
// operadores logicos
EQ    : '==' ;
NEQ   : '!=' ;
LE    : '<=' ;
GE    : '>=' ;
// operadores logicos de valor bajo
LT     : '<' ;
GT     : '>' ;

// manejo de valor
INC   : '++' ;
DEC   : '--' ;


// operadores aritmeticos
PLUS    : '+' ;
MINUS   : '-' ;
MULT    : '*' ;
DIV     : '/' ;
MOD     : '%' ;

// asignacion
ASSIGN : '=' ;

// ternario
QUESTION  : '?' ;

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