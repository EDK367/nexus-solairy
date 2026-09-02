lexer grammar YLexer;

// TOKENS VIRTUALES (para el parser)
tokens { INDENT, DEDENT }

// comentarios
LINE_COMMENT  : '//' ~[\r\n]*  -> channel(HIDDEN) ;
BLOCK_COMMENT : '/*' .*? '*/'  -> channel(HIDDEN) ;

// encabezados
SEC_STRUCT      : '%estructuras'    ;
SEC_FUNCTION    : '%funciones'      ;

// declaraciones
ESTRUCTURA  : 'estructura'  ;
DEFINIR     : 'definir'     ;

// FUNCIONES ESPECIALES
// entrada
LEER        : 'leer'        ;
// salida
IMPRIMIR    : 'imprimir'    ;
// asignacion de retorno
RETURN_VALUE : '->' ;

// CONTROL DE FLUJO
// control de flujo principales
SI          : 'si'          ;
ENTONCES    : 'entonces'    ;
SINO        : 'sino'        ;
CONTRARIO   : 'contrario'   ;
ELEGIR      : 'elegir'      ;
CASO        : 'caso'        ;
SIEMPRE     : 'siempre'     ;
PARA        : 'para'        ;
MIENTRAS    : 'mientras'    ;
HACER       : 'hacer'       ;
// control de flujo interno
RETORNAR    : 'retornar'    ;
ROMPER      : 'romper'      ;
CONTINUAR   : 'continuar'   ;

// primitivos
CADENA      : 'cadena'  ;
ENTERO      : 'entero'  ;
FLOTANTE    : 'flotante';
CARACTER    : 'caracter';
BOOL        : 'bool'    ;

// booleanos
VERDADERO   : 'verdadero'   ;
FALSO       : 'falso'       ;

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
ID  : [a-zA-Z_][a-zA-Z0-9_]*    ;

// identacion
NEWLINE     : ('\r'? '\n' | '\r')   ;
SPACE    : [ \t]+ -> skip           ;