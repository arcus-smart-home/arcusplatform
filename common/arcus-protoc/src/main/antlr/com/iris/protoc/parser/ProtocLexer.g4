lexer grammar ProtocLexer;

fragment WS : [ \r\t\n]+ ;

fragment DIGIT    : [0-9] ;
fragment BINDIGIT : [0-1] ;
fragment OCTDIGIT : [0-7] ;
fragment HEXDIGIT : [0-9a-fA-F] ;

COMMENT : '/*' .*? '*/' -> skip ;
LCOMMENT : '//' ~[\r\n]* -> skip ;
STR : '"' -> more, mode(MODE_STR) ;
WHITESPACE : WS -> skip ;

LBRACE : '{' ;
RBRACE : '}' ;
LBRACKET : '[' ;
RBRACKET : ']' ;
LANGLE : '<' ;
RANGLE : '>' ;
SEMI : ';' ;
COMMA : ',' ;
DOT : '.' ;
EQ : '=' ;

INCLUDE : 'include' ;
CONSTANTS : 'constants' ;
CONST : 'const' ;
ALIAS : 'alias' ;

INT : [1-9] DIGIT* | '0x' HEXDIGIT+ | '0b' BINDIGIT+ | '0' OCTDIGIT*;

OPTIONS : 'irpoptions' ;
HEADER_BLK : 'irpheader' WS '{' WS -> more, mode(MODE_HEADER) ;
CODE_BLK : 'irpcode' WS '{' WS -> more, mode(MODE_CODE) ;
STRUCT : 'struct' ;
MESSAGE : 'message' ;
WHEN_EXPR : 'when' WS -> more, mode(MODE_WHEN) ;
ENCODING_EXPR : 'encoding' WS -> more, mode(MODE_ENCODING) ;
DECODING_EXPR : 'decoding' WS -> more, mode(MODE_DECODING) ;
RANDOM_EXPR : 'random' WS -> more, mode(MODE_RANDOM) ;
SIZE_EXPR : 'size' WS -> more, mode(MODE_SIZE) ;

PRIMITIVE_TYPE : 'u8' | 'u16' | 'u32' | 'u64' |
                 'i8' | 'i16' | 'i32' | 'i64' |
                 'f32' | 'f64' ;

ID : [a-zA-Z] [a-zA-Z0-9_]* ;

mode MODE_STR ;
STRING : '"' -> mode(DEFAULT_MODE) ;
STRING_TEXT : . -> more ;

mode MODE_HEADER;
HEADER : '\n}' -> mode(DEFAULT_MODE) ;
HEADER_TEXT : . -> more ;

mode MODE_CODE;
CODE : '\n}' -> mode(DEFAULT_MODE) ;
CODE_TEXT : . -> more ;

mode MODE_WHEN;
WHEN : ';' -> mode(DEFAULT_MODE) ;
WHEN_TEXT : . -> more ;

mode MODE_ENCODING;
ENCODING : '\n' -> mode(DEFAULT_MODE) ;
ENCODING_TEXT : . -> more ;

mode MODE_DECODING;
DECODING : '\n' -> mode(DEFAULT_MODE) ;
DECODING_TEXT : . -> more ;

mode MODE_RANDOM;
RANDOM : '\n' -> mode(DEFAULT_MODE) ;
RANDOM_TEXT : . -> more ;

mode MODE_SIZE;
SIZE : '\n' -> mode(DEFAULT_MODE) ;
SIZE_TEXT : . -> more ;
