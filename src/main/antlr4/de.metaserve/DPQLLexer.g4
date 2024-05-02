lexer grammar DPQLLexer;

channels { DPQLCOMMENT, ERRORCHANNEL }

// SKIP

SPACE:                               [ \t\r\n]+    -> channel(HIDDEN);
SPEC_MYSQL_COMMENT:                  '/*!' .+? '*/' -> channel(DPQLCOMMENT);
COMMENT_INPUT:                       '/*' .*? '*/' -> channel(HIDDEN);
LINE_COMMENT:                        (
                                       ('--' [ \t] | '#') ~[\r\n]* ('\r'? '\n' | EOF)
                                       | '--' ('\r'? '\n' | EOF)
                                     ) -> channel(HIDDEN);

NOT: 'NOT';
SELECT: 'SELECT';
STAR: '*';
PLUS: '+';
MINUS: '-';
DIVIDE: '/';
COMMA: ',';
FROM: 'FROM';
AS: 'AS';
CC: 'CC';
AND: 'AND';
OR: 'OR';
OPEN: '(';
CLOSE: ')';
WHERE: 'WHERE';
MIN: 'MIN';
MAX: 'MAX';
UCC: 'UCC';
IND: 'IND';
FD: 'FD';
SIZE: 'SIZE';
SPLIT: 'SPLIT';
GREATER: '<';
SMALLER: '>';
EQUAL: '=';
CARD: 'CARD' | 'CARDINALITY';
ORDERBY: 'ORDER BY';
GROUPBY: 'GROUP BY';
SUM: 'SUM';
MINIMUM: 'MINIMUM';
MEAN: 'MEAN';
MAXIMUM: 'MAXIMUM';
AVG: 'AVG';
TYPE: 'TYPE';
NULL: 'NULL';
UNIQNESS: 'UNIQNESS';
OVERLAP: 'OVERLAP';
LIMIT: 'LIMIT';
ASC: 'ASC';
DESC: 'DESC';

DECIMAL_OPERATION: STAR | PLUS | MINUS | DIVIDE;
DECIMAL_LITERAL: [0-9]+;
ID: [a-zA-Z]+;

ERROR_RECONGNIGION:                  .    -> channel(ERRORCHANNEL);