parser grammar ProtocParser;

options { 
   tokenVocab = ProtocLexer;
}

opts : OPTIONS LBRACE opt_expr* RBRACE ;
opt_expr : ID EQ STRING SEMI ;

type : PRIMITIVE_TYPE | ID | type LBRACKET RBRACKET | type LBRACKET value RBRACKET;

field : constant | type ID SIZE? ENCODING? DECODING? RANDOM? (WHEN | SEMI) ;
value : INT | ID ;

params : params COMMA ID EQ STRING | ID EQ STRING ;

include : INCLUDE STRING SEMI ;
alias  : ALIAS type ID SEMI ;
constant : CONST type ID EQ value SEMI ;

consts : CONSTANTS qualified LBRACE constant* RBRACE ;
struct :  STRUCT qualified LBRACE field* RBRACE ;
message : MESSAGE qualified LANGLE params RANGLE LBRACE field* RBRACE ;

qualified : qualified DOT ID | ID ;

file : opts? HEADER? (include | alias | consts | message | struct)* CODE? ;

