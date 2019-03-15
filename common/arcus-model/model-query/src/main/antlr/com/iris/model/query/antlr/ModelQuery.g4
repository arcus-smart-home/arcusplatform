grammar ModelQuery;
import ModelExpression;

WILD:  '*';
COMMA: ',';

ATTR_WILD: IDENT COLON WILD;

query: WS? select WS fields (WS where WS predicate)? WS? EOF;

fields: WILD | field (WS? COMMA WS? field)*;
field:  ATTR_WILD | ATTR;

select: 'select';
where:  'where';
//group:  'group' WS 'by';
