// Generated from de.metaserve/DPQLParser.g4 by ANTLR 4.13.2
package de.metaserve;
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link DPQLParser}.
 */
public interface DPQLParserListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link DPQLParser#dpqlStatement}.
	 * @param ctx the parse tree
	 */
	void enterDpqlStatement(DPQLParser.DpqlStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link DPQLParser#dpqlStatement}.
	 * @param ctx the parse tree
	 */
	void exitDpqlStatement(DPQLParser.DpqlStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link DPQLParser#select}.
	 * @param ctx the parse tree
	 */
	void enterSelect(DPQLParser.SelectContext ctx);
	/**
	 * Exit a parse tree produced by {@link DPQLParser#select}.
	 * @param ctx the parse tree
	 */
	void exitSelect(DPQLParser.SelectContext ctx);
	/**
	 * Enter a parse tree produced by {@link DPQLParser#selectelements}.
	 * @param ctx the parse tree
	 */
	void enterSelectelements(DPQLParser.SelectelementsContext ctx);
	/**
	 * Exit a parse tree produced by {@link DPQLParser#selectelements}.
	 * @param ctx the parse tree
	 */
	void exitSelectelements(DPQLParser.SelectelementsContext ctx);
	/**
	 * Enter a parse tree produced by {@link DPQLParser#selectelement}.
	 * @param ctx the parse tree
	 */
	void enterSelectelement(DPQLParser.SelectelementContext ctx);
	/**
	 * Exit a parse tree produced by {@link DPQLParser#selectelement}.
	 * @param ctx the parse tree
	 */
	void exitSelectelement(DPQLParser.SelectelementContext ctx);
	/**
	 * Enter a parse tree produced by {@link DPQLParser#from}.
	 * @param ctx the parse tree
	 */
	void enterFrom(DPQLParser.FromContext ctx);
	/**
	 * Exit a parse tree produced by {@link DPQLParser#from}.
	 * @param ctx the parse tree
	 */
	void exitFrom(DPQLParser.FromContext ctx);
	/**
	 * Enter a parse tree produced by {@link DPQLParser#fromelements}.
	 * @param ctx the parse tree
	 */
	void enterFromelements(DPQLParser.FromelementsContext ctx);
	/**
	 * Exit a parse tree produced by {@link DPQLParser#fromelements}.
	 * @param ctx the parse tree
	 */
	void exitFromelements(DPQLParser.FromelementsContext ctx);
	/**
	 * Enter a parse tree produced by {@link DPQLParser#fromelement}.
	 * @param ctx the parse tree
	 */
	void enterFromelement(DPQLParser.FromelementContext ctx);
	/**
	 * Exit a parse tree produced by {@link DPQLParser#fromelement}.
	 * @param ctx the parse tree
	 */
	void exitFromelement(DPQLParser.FromelementContext ctx);
	/**
	 * Enter a parse tree produced by {@link DPQLParser#where}.
	 * @param ctx the parse tree
	 */
	void enterWhere(DPQLParser.WhereContext ctx);
	/**
	 * Exit a parse tree produced by {@link DPQLParser#where}.
	 * @param ctx the parse tree
	 */
	void exitWhere(DPQLParser.WhereContext ctx);
	/**
	 * Enter a parse tree produced by {@link DPQLParser#whereelements}.
	 * @param ctx the parse tree
	 */
	void enterWhereelements(DPQLParser.WhereelementsContext ctx);
	/**
	 * Exit a parse tree produced by {@link DPQLParser#whereelements}.
	 * @param ctx the parse tree
	 */
	void exitWhereelements(DPQLParser.WhereelementsContext ctx);
	/**
	 * Enter a parse tree produced by {@link DPQLParser#whereelement}.
	 * @param ctx the parse tree
	 */
	void enterWhereelement(DPQLParser.WhereelementContext ctx);
	/**
	 * Exit a parse tree produced by {@link DPQLParser#whereelement}.
	 * @param ctx the parse tree
	 */
	void exitWhereelement(DPQLParser.WhereelementContext ctx);
	/**
	 * Enter a parse tree produced by {@link DPQLParser#numericFunctions}.
	 * @param ctx the parse tree
	 */
	void enterNumericFunctions(DPQLParser.NumericFunctionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link DPQLParser#numericFunctions}.
	 * @param ctx the parse tree
	 */
	void exitNumericFunctions(DPQLParser.NumericFunctionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link DPQLParser#limit}.
	 * @param ctx the parse tree
	 */
	void enterLimit(DPQLParser.LimitContext ctx);
	/**
	 * Exit a parse tree produced by {@link DPQLParser#limit}.
	 * @param ctx the parse tree
	 */
	void exitLimit(DPQLParser.LimitContext ctx);
	/**
	 * Enter a parse tree produced by {@link DPQLParser#orderBy}.
	 * @param ctx the parse tree
	 */
	void enterOrderBy(DPQLParser.OrderByContext ctx);
	/**
	 * Exit a parse tree produced by {@link DPQLParser#orderBy}.
	 * @param ctx the parse tree
	 */
	void exitOrderBy(DPQLParser.OrderByContext ctx);
	/**
	 * Enter a parse tree produced by {@link DPQLParser#groupBy}.
	 * @param ctx the parse tree
	 */
	void enterGroupBy(DPQLParser.GroupByContext ctx);
	/**
	 * Exit a parse tree produced by {@link DPQLParser#groupBy}.
	 * @param ctx the parse tree
	 */
	void exitGroupBy(DPQLParser.GroupByContext ctx);
	/**
	 * Enter a parse tree produced by {@link DPQLParser#functionOrID}.
	 * @param ctx the parse tree
	 */
	void enterFunctionOrID(DPQLParser.FunctionOrIDContext ctx);
	/**
	 * Exit a parse tree produced by {@link DPQLParser#functionOrID}.
	 * @param ctx the parse tree
	 */
	void exitFunctionOrID(DPQLParser.FunctionOrIDContext ctx);
	/**
	 * Enter a parse tree produced by {@link DPQLParser#id}.
	 * @param ctx the parse tree
	 */
	void enterId(DPQLParser.IdContext ctx);
	/**
	 * Exit a parse tree produced by {@link DPQLParser#id}.
	 * @param ctx the parse tree
	 */
	void exitId(DPQLParser.IdContext ctx);
	/**
	 * Enter a parse tree produced by {@link DPQLParser#tableName}.
	 * @param ctx the parse tree
	 */
	void enterTableName(DPQLParser.TableNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link DPQLParser#tableName}.
	 * @param ctx the parse tree
	 */
	void exitTableName(DPQLParser.TableNameContext ctx);
}