parser grammar DPQLParser;

options { tokenVocab=DPQLLexer; }

dpqlStatement :
    select from where groupBy? orderBy? limit? EOF
    ;

select :
    SELECT selectelements
    ;

selectelements :
    selectelement (COMMA selectelement)*
    ;

selectelement :
    (functionOrID (AS id)?)
    ;

from :
    FROM fromelements
    ;
fromelements :
    fromelement (COMMA fromelement)*
    ;

fromelement :
    CC OPEN tableName (COMMA tableName)* CLOSE (AS)? id
    ;

where :
    WHERE whereelements
    ;

whereelements :
    whereelement ( (AND|OR) whereelement)*
    ;

whereelement :
    NOT?
    (
     ((MIN|MAX|UCC) OPEN id CLOSE) |
     ((IND | FD | SPLIT) OPEN id COMMA id CLOSE) |
     (numericFunctions (SMALLER|GREATER|((SMALLER|GREATER) EQUAL)|EQUAL)  (DECIMAL_LITERAL | numericFunctions))
    )
    ;

numericFunctions :
    (DECIMAL_LITERAL DECIMAL_OPERATION numericFunctions)
    | ((SIZE | CARD | SUM | MINIMUM | MEAN | MAXIMUM | AVG | TYPE | NULL | UNIQNESS | OVERLAP) OPEN id CLOSE)
;

limit: LIMIT DECIMAL_LITERAL;

orderBy: ORDERBY functionOrID (ASC|DESC)?;

groupBy: GROUPBY functionOrID;

functionOrID: numericFunctions | id;

id: (ID | STAR);

tableName: (ID | STAR);
