grammar ModelExpression;

@header {
package com.iris.model.query.antlr;
}

/**
 * Since WS includes all contiguous whitespace, we can use:
 *    "WS" in parser rules where whitespace is required (e.g. around alphanumeric words)
 *    "WS?" in parser rules where whitespace is optional (e.g. around symbolic operators)
 */
WS: [ \t\r\n]+;

LP: '(';
RP: ')';

fragment LETTER: [a-zA-Z];
fragment DIGIT:  [0-9];
fragment COLON:  ':';

/**
 * IDENT must come last in this group, to prevent the others from possibly being mislexed as IDENT.
 */
BOOL:  ('true' | 'TRUE') | ('false' | 'FALSE');
NUM:   [-+]? DIGIT* '.'? DIGIT+;
IDENT: (LETTER | '_' | DIGIT)+; // Should any other characters be allowed here?

ATTR: IDENT COLON IDENT;

STR_TICK:  '\'' ~'\''* '\'';
STR_QUOTE: '"'  ~'"'*  '"';

/**
 * For simplicity/consistency, we should only specify *optional* instances of WS in these "predicate" rules.  Required
 * instances of WS are specified further down in the "operator" rules.
 */

expression: WS? predicate WS? EOF;

predicate:
	binaryPredicate |
	unaryPredicate |
	namespacePredicate |
	attributePredicate | // attribute needs to come before constant
	constantPredicate |
	groupPredicate; // This has to go last or paren matching will be too greedy

binaryPredicate: // everything but itself can go on the left hand side
	(unaryPredicate | namespacePredicate | attributePredicate | constantPredicate | groupPredicate) WS? binaryOperator WS? predicate;
groupPredicate:
	LP WS? predicate WS? RP;
unaryPredicate:
	unaryOperator WS? predicate;
constantPredicate:
	valueBoolean |
	value WS? valueBinaryOperator WS? value;
attributePredicate:
	attribute WS? attributeBinaryOperator WS? value |
	value     WS? valueBinaryOperator WS? attribute |
	attribute WS? attributePostFixOperator;
namespacePredicate:
	namespaceOperator WS? namespace;

attribute: ATTR;
namespace: IDENT;

unaryOperator:
	operatorNot;
binaryOperator:
	operatorAnd |
	operatorOr;
valueBinaryOperator:
	operatorEquals |
	operatorNotEquals |
	operatorLike |
	operatorLessThan |
	operatorLessThanOrEqualTo |
	operatorGreaterThan |
	operatorGreaterThanOrEqualTo;
attributeBinaryOperator:
	operatorEquals |
	operatorNotEquals |
	operatorLike |
	operatorLessThan |
	operatorLessThanOrEqualTo |
	operatorGreaterThan |
	operatorGreaterThanOrEqualTo |
	operatorContains;
attributePostFixOperator:
	operatorIsSupported;
namespaceOperator:
	operatorIsA |
	operatorHasA;

/**
 * For simplicity/consistency, we should only specify *required* instances of WS in these "operator" rules.  Optional
 * instances of WS are specified above in the "predicate" rules.
 */
operatorNot:                  '!' | ('not' | 'NOT') WS;
operatorAnd:                  WS ('and' | 'AND') WS;
operatorOr:                   WS ('or' | 'OR') WS;
operatorEquals:               '=' | '==' | WS ('eq' | 'EQ') WS;
operatorNotEquals:            '!=';
operatorLike:                 '~' | '=~' | WS ('like' | 'LIKE') WS;
operatorLessThan:             '<';
operatorLessThanOrEqualTo:    '<=';
operatorGreaterThan:          '>';
operatorGreaterThanOrEqualTo: '>=';
operatorContains:             WS ('contains' | 'CONTAINS') WS ;
operatorIsSupported:          WS ('is' WS 'supported' | 'IS' WS 'SUPPORTED');
operatorIsA:                  ('is' | 'IS') WS;
operatorHasA:                 ('has' | 'HAS') WS;

value:
	attribute |
	valueBoolean |
	valueNumeric |
	valueString;

valueBoolean: BOOL;
valueNumeric: NUM;
// TODO: Use modes to add a string parser, add escaping
valueString:  STR_TICK | STR_QUOTE;
