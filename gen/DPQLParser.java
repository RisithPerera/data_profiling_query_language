// Generated from de.metaserve/DPQLParser.g4 by ANTLR 4.13.2
package de.metaserve;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast", "CheckReturnValue", "this-escape"})
public class DPQLParser extends Parser {
	static { RuntimeMetaData.checkVersion("4.13.2", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		SPACE=1, SPEC_MYSQL_COMMENT=2, COMMENT_INPUT=3, LINE_COMMENT=4, NOT=5, 
		SELECT=6, STAR=7, PLUS=8, MINUS=9, DIVIDE=10, COMMA=11, FROM=12, AS=13, 
		CC=14, AND=15, OR=16, OPEN=17, CLOSE=18, WHERE=19, MIN=20, MAX=21, UCC=22, 
		IND=23, FD=24, SIZE=25, SPLIT=26, CONTAINS=27, COALESCE=28, GREATER=29, 
		SMALLER=30, EQUAL=31, CARD=32, ORDERBY=33, GROUPBY=34, SUM=35, MINIMUM=36, 
		MEAN=37, MAXIMUM=38, AVG=39, TYPE=40, NULL=41, UNIQNESS=42, OVERLAP=43, 
		LIMIT=44, ASC=45, DESC=46, DECIMAL_OPERATION=47, DECIMAL_LITERAL=48, ID=49, 
		ERROR_RECONGNIGION=50;
	public static final int
		RULE_dpqlStatement = 0, RULE_select = 1, RULE_selectelements = 2, RULE_selectelement = 3, 
		RULE_from = 4, RULE_fromelements = 5, RULE_fromelement = 6, RULE_where = 7, 
		RULE_whereelements = 8, RULE_whereelement = 9, RULE_numericFunctions = 10, 
		RULE_limit = 11, RULE_orderBy = 12, RULE_groupBy = 13, RULE_functionOrID = 14, 
		RULE_id = 15, RULE_tableName = 16;
	private static String[] makeRuleNames() {
		return new String[] {
			"dpqlStatement", "select", "selectelements", "selectelement", "from", 
			"fromelements", "fromelement", "where", "whereelements", "whereelement", 
			"numericFunctions", "limit", "orderBy", "groupBy", "functionOrID", "id", 
			"tableName"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, null, null, null, null, "'NOT'", "'SELECT'", "'*'", "'+'", "'-'", 
			"'/'", "','", "'FROM'", "'AS'", "'CC'", "'AND'", "'OR'", "'('", "')'", 
			"'WHERE'", "'MIN'", "'MAX'", "'UCC'", "'IND'", "'FD'", "'SIZE'", "'SPLIT'", 
			"'CONTAINS'", "'COALESCE'", "'<'", "'>'", "'='", null, "'ORDER BY'", 
			"'GROUP BY'", "'SUM'", "'MINIMUM'", "'MEAN'", "'MAXIMUM'", "'AVG'", "'TYPE'", 
			"'NULL'", "'UNIQNESS'", "'OVERLAP'", "'LIMIT'", "'ASC'", "'DESC'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, "SPACE", "SPEC_MYSQL_COMMENT", "COMMENT_INPUT", "LINE_COMMENT", 
			"NOT", "SELECT", "STAR", "PLUS", "MINUS", "DIVIDE", "COMMA", "FROM", 
			"AS", "CC", "AND", "OR", "OPEN", "CLOSE", "WHERE", "MIN", "MAX", "UCC", 
			"IND", "FD", "SIZE", "SPLIT", "CONTAINS", "COALESCE", "GREATER", "SMALLER", 
			"EQUAL", "CARD", "ORDERBY", "GROUPBY", "SUM", "MINIMUM", "MEAN", "MAXIMUM", 
			"AVG", "TYPE", "NULL", "UNIQNESS", "OVERLAP", "LIMIT", "ASC", "DESC", 
			"DECIMAL_OPERATION", "DECIMAL_LITERAL", "ID", "ERROR_RECONGNIGION"
		};
	}
	private static final String[] _SYMBOLIC_NAMES = makeSymbolicNames();
	public static final Vocabulary VOCABULARY = new VocabularyImpl(_LITERAL_NAMES, _SYMBOLIC_NAMES);

	/**
	 * @deprecated Use {@link #VOCABULARY} instead.
	 */
	@Deprecated
	public static final String[] tokenNames;
	static {
		tokenNames = new String[_SYMBOLIC_NAMES.length];
		for (int i = 0; i < tokenNames.length; i++) {
			tokenNames[i] = VOCABULARY.getLiteralName(i);
			if (tokenNames[i] == null) {
				tokenNames[i] = VOCABULARY.getSymbolicName(i);
			}

			if (tokenNames[i] == null) {
				tokenNames[i] = "<INVALID>";
			}
		}
	}

	@Override
	@Deprecated
	public String[] getTokenNames() {
		return tokenNames;
	}

	@Override

	public Vocabulary getVocabulary() {
		return VOCABULARY;
	}

	@Override
	public String getGrammarFileName() { return "DPQLParser.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public ATN getATN() { return _ATN; }

	public DPQLParser(TokenStream input) {
		super(input);
		_interp = new ParserATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@SuppressWarnings("CheckReturnValue")
	public static class DpqlStatementContext extends ParserRuleContext {
		public SelectContext select() {
			return getRuleContext(SelectContext.class,0);
		}
		public FromContext from() {
			return getRuleContext(FromContext.class,0);
		}
		public WhereContext where() {
			return getRuleContext(WhereContext.class,0);
		}
		public TerminalNode EOF() { return getToken(DPQLParser.EOF, 0); }
		public GroupByContext groupBy() {
			return getRuleContext(GroupByContext.class,0);
		}
		public OrderByContext orderBy() {
			return getRuleContext(OrderByContext.class,0);
		}
		public LimitContext limit() {
			return getRuleContext(LimitContext.class,0);
		}
		public DpqlStatementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_dpqlStatement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DPQLParserListener ) ((DPQLParserListener)listener).enterDpqlStatement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DPQLParserListener ) ((DPQLParserListener)listener).exitDpqlStatement(this);
		}
	}

	public final DpqlStatementContext dpqlStatement() throws RecognitionException {
		DpqlStatementContext _localctx = new DpqlStatementContext(_ctx, getState());
		enterRule(_localctx, 0, RULE_dpqlStatement);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(34);
			select();
			setState(35);
			from();
			setState(36);
			where();
			setState(38);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==GROUPBY) {
				{
				setState(37);
				groupBy();
				}
			}

			setState(41);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==ORDERBY) {
				{
				setState(40);
				orderBy();
				}
			}

			setState(44);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==LIMIT) {
				{
				setState(43);
				limit();
				}
			}

			setState(46);
			match(EOF);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class SelectContext extends ParserRuleContext {
		public TerminalNode SELECT() { return getToken(DPQLParser.SELECT, 0); }
		public SelectelementsContext selectelements() {
			return getRuleContext(SelectelementsContext.class,0);
		}
		public SelectContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_select; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DPQLParserListener ) ((DPQLParserListener)listener).enterSelect(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DPQLParserListener ) ((DPQLParserListener)listener).exitSelect(this);
		}
	}

	public final SelectContext select() throws RecognitionException {
		SelectContext _localctx = new SelectContext(_ctx, getState());
		enterRule(_localctx, 2, RULE_select);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(48);
			match(SELECT);
			setState(49);
			selectelements();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class SelectelementsContext extends ParserRuleContext {
		public List<SelectelementContext> selectelement() {
			return getRuleContexts(SelectelementContext.class);
		}
		public SelectelementContext selectelement(int i) {
			return getRuleContext(SelectelementContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(DPQLParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(DPQLParser.COMMA, i);
		}
		public SelectelementsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_selectelements; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DPQLParserListener ) ((DPQLParserListener)listener).enterSelectelements(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DPQLParserListener ) ((DPQLParserListener)listener).exitSelectelements(this);
		}
	}

	public final SelectelementsContext selectelements() throws RecognitionException {
		SelectelementsContext _localctx = new SelectelementsContext(_ctx, getState());
		enterRule(_localctx, 4, RULE_selectelements);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(51);
			selectelement();
			setState(56);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(52);
				match(COMMA);
				setState(53);
				selectelement();
				}
				}
				setState(58);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class SelectelementContext extends ParserRuleContext {
		public FunctionOrIDContext functionOrID() {
			return getRuleContext(FunctionOrIDContext.class,0);
		}
		public TerminalNode AS() { return getToken(DPQLParser.AS, 0); }
		public IdContext id() {
			return getRuleContext(IdContext.class,0);
		}
		public SelectelementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_selectelement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DPQLParserListener ) ((DPQLParserListener)listener).enterSelectelement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DPQLParserListener ) ((DPQLParserListener)listener).exitSelectelement(this);
		}
	}

	public final SelectelementContext selectelement() throws RecognitionException {
		SelectelementContext _localctx = new SelectelementContext(_ctx, getState());
		enterRule(_localctx, 6, RULE_selectelement);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			{
			setState(59);
			functionOrID();
			setState(62);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==AS) {
				{
				setState(60);
				match(AS);
				setState(61);
				id();
				}
			}

			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class FromContext extends ParserRuleContext {
		public TerminalNode FROM() { return getToken(DPQLParser.FROM, 0); }
		public FromelementsContext fromelements() {
			return getRuleContext(FromelementsContext.class,0);
		}
		public FromContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_from; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DPQLParserListener ) ((DPQLParserListener)listener).enterFrom(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DPQLParserListener ) ((DPQLParserListener)listener).exitFrom(this);
		}
	}

	public final FromContext from() throws RecognitionException {
		FromContext _localctx = new FromContext(_ctx, getState());
		enterRule(_localctx, 8, RULE_from);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(64);
			match(FROM);
			setState(65);
			fromelements();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class FromelementsContext extends ParserRuleContext {
		public List<FromelementContext> fromelement() {
			return getRuleContexts(FromelementContext.class);
		}
		public FromelementContext fromelement(int i) {
			return getRuleContext(FromelementContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(DPQLParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(DPQLParser.COMMA, i);
		}
		public FromelementsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_fromelements; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DPQLParserListener ) ((DPQLParserListener)listener).enterFromelements(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DPQLParserListener ) ((DPQLParserListener)listener).exitFromelements(this);
		}
	}

	public final FromelementsContext fromelements() throws RecognitionException {
		FromelementsContext _localctx = new FromelementsContext(_ctx, getState());
		enterRule(_localctx, 10, RULE_fromelements);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(67);
			fromelement();
			setState(72);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(68);
				match(COMMA);
				setState(69);
				fromelement();
				}
				}
				setState(74);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class FromelementContext extends ParserRuleContext {
		public TerminalNode CC() { return getToken(DPQLParser.CC, 0); }
		public TerminalNode OPEN() { return getToken(DPQLParser.OPEN, 0); }
		public List<TableNameContext> tableName() {
			return getRuleContexts(TableNameContext.class);
		}
		public TableNameContext tableName(int i) {
			return getRuleContext(TableNameContext.class,i);
		}
		public TerminalNode CLOSE() { return getToken(DPQLParser.CLOSE, 0); }
		public IdContext id() {
			return getRuleContext(IdContext.class,0);
		}
		public List<TerminalNode> COMMA() { return getTokens(DPQLParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(DPQLParser.COMMA, i);
		}
		public TerminalNode AS() { return getToken(DPQLParser.AS, 0); }
		public FromelementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_fromelement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DPQLParserListener ) ((DPQLParserListener)listener).enterFromelement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DPQLParserListener ) ((DPQLParserListener)listener).exitFromelement(this);
		}
	}

	public final FromelementContext fromelement() throws RecognitionException {
		FromelementContext _localctx = new FromelementContext(_ctx, getState());
		enterRule(_localctx, 12, RULE_fromelement);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(75);
			match(CC);
			setState(76);
			match(OPEN);
			setState(77);
			tableName();
			setState(82);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(78);
				match(COMMA);
				setState(79);
				tableName();
				}
				}
				setState(84);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(85);
			match(CLOSE);
			setState(87);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==AS) {
				{
				setState(86);
				match(AS);
				}
			}

			setState(89);
			id();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class WhereContext extends ParserRuleContext {
		public TerminalNode WHERE() { return getToken(DPQLParser.WHERE, 0); }
		public WhereelementsContext whereelements() {
			return getRuleContext(WhereelementsContext.class,0);
		}
		public WhereContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_where; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DPQLParserListener ) ((DPQLParserListener)listener).enterWhere(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DPQLParserListener ) ((DPQLParserListener)listener).exitWhere(this);
		}
	}

	public final WhereContext where() throws RecognitionException {
		WhereContext _localctx = new WhereContext(_ctx, getState());
		enterRule(_localctx, 14, RULE_where);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(91);
			match(WHERE);
			setState(92);
			whereelements();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class WhereelementsContext extends ParserRuleContext {
		public List<WhereelementContext> whereelement() {
			return getRuleContexts(WhereelementContext.class);
		}
		public WhereelementContext whereelement(int i) {
			return getRuleContext(WhereelementContext.class,i);
		}
		public List<TerminalNode> AND() { return getTokens(DPQLParser.AND); }
		public TerminalNode AND(int i) {
			return getToken(DPQLParser.AND, i);
		}
		public List<TerminalNode> OR() { return getTokens(DPQLParser.OR); }
		public TerminalNode OR(int i) {
			return getToken(DPQLParser.OR, i);
		}
		public WhereelementsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_whereelements; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DPQLParserListener ) ((DPQLParserListener)listener).enterWhereelements(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DPQLParserListener ) ((DPQLParserListener)listener).exitWhereelements(this);
		}
	}

	public final WhereelementsContext whereelements() throws RecognitionException {
		WhereelementsContext _localctx = new WhereelementsContext(_ctx, getState());
		enterRule(_localctx, 16, RULE_whereelements);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(94);
			whereelement();
			setState(99);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==AND || _la==OR) {
				{
				{
				setState(95);
				_la = _input.LA(1);
				if ( !(_la==AND || _la==OR) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(96);
				whereelement();
				}
				}
				setState(101);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class WhereelementContext extends ParserRuleContext {
		public TerminalNode NOT() { return getToken(DPQLParser.NOT, 0); }
		public TerminalNode OPEN() { return getToken(DPQLParser.OPEN, 0); }
		public List<IdContext> id() {
			return getRuleContexts(IdContext.class);
		}
		public IdContext id(int i) {
			return getRuleContext(IdContext.class,i);
		}
		public TerminalNode CLOSE() { return getToken(DPQLParser.CLOSE, 0); }
		public TerminalNode COMMA() { return getToken(DPQLParser.COMMA, 0); }
		public List<NumericFunctionsContext> numericFunctions() {
			return getRuleContexts(NumericFunctionsContext.class);
		}
		public NumericFunctionsContext numericFunctions(int i) {
			return getRuleContext(NumericFunctionsContext.class,i);
		}
		public TerminalNode MIN() { return getToken(DPQLParser.MIN, 0); }
		public TerminalNode MAX() { return getToken(DPQLParser.MAX, 0); }
		public TerminalNode UCC() { return getToken(DPQLParser.UCC, 0); }
		public TerminalNode IND() { return getToken(DPQLParser.IND, 0); }
		public TerminalNode FD() { return getToken(DPQLParser.FD, 0); }
		public TerminalNode SPLIT() { return getToken(DPQLParser.SPLIT, 0); }
		public TerminalNode CONTAINS() { return getToken(DPQLParser.CONTAINS, 0); }
		public TerminalNode COALESCE() { return getToken(DPQLParser.COALESCE, 0); }
		public TerminalNode SMALLER() { return getToken(DPQLParser.SMALLER, 0); }
		public TerminalNode GREATER() { return getToken(DPQLParser.GREATER, 0); }
		public TerminalNode EQUAL() { return getToken(DPQLParser.EQUAL, 0); }
		public TerminalNode DECIMAL_LITERAL() { return getToken(DPQLParser.DECIMAL_LITERAL, 0); }
		public WhereelementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_whereelement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DPQLParserListener ) ((DPQLParserListener)listener).enterWhereelement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DPQLParserListener ) ((DPQLParserListener)listener).exitWhereelement(this);
		}
	}

	public final WhereelementContext whereelement() throws RecognitionException {
		WhereelementContext _localctx = new WhereelementContext(_ctx, getState());
		enterRule(_localctx, 18, RULE_whereelement);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(103);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==NOT) {
				{
				setState(102);
				match(NOT);
				}
			}

			setState(129);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case MIN:
			case MAX:
			case UCC:
				{
				{
				setState(105);
				_la = _input.LA(1);
				if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 7340032L) != 0)) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(106);
				match(OPEN);
				setState(107);
				id();
				setState(108);
				match(CLOSE);
				}
				}
				break;
			case IND:
			case FD:
			case SPLIT:
			case CONTAINS:
			case COALESCE:
				{
				{
				setState(110);
				_la = _input.LA(1);
				if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 494927872L) != 0)) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(111);
				match(OPEN);
				setState(112);
				id();
				setState(113);
				match(COMMA);
				setState(114);
				id();
				setState(115);
				match(CLOSE);
				}
				}
				break;
			case SIZE:
			case CARD:
			case SUM:
			case MINIMUM:
			case MEAN:
			case MAXIMUM:
			case AVG:
			case TYPE:
			case NULL:
			case UNIQNESS:
			case OVERLAP:
			case DECIMAL_LITERAL:
				{
				{
				setState(117);
				numericFunctions();
				setState(123);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,10,_ctx) ) {
				case 1:
					{
					setState(118);
					match(SMALLER);
					}
					break;
				case 2:
					{
					setState(119);
					match(GREATER);
					}
					break;
				case 3:
					{
					{
					setState(120);
					_la = _input.LA(1);
					if ( !(_la==GREATER || _la==SMALLER) ) {
					_errHandler.recoverInline(this);
					}
					else {
						if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
						_errHandler.reportMatch(this);
						consume();
					}
					setState(121);
					match(EQUAL);
					}
					}
					break;
				case 4:
					{
					setState(122);
					match(EQUAL);
					}
					break;
				}
				setState(127);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,11,_ctx) ) {
				case 1:
					{
					setState(125);
					match(DECIMAL_LITERAL);
					}
					break;
				case 2:
					{
					setState(126);
					numericFunctions();
					}
					break;
				}
				}
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class NumericFunctionsContext extends ParserRuleContext {
		public TerminalNode DECIMAL_LITERAL() { return getToken(DPQLParser.DECIMAL_LITERAL, 0); }
		public TerminalNode DECIMAL_OPERATION() { return getToken(DPQLParser.DECIMAL_OPERATION, 0); }
		public NumericFunctionsContext numericFunctions() {
			return getRuleContext(NumericFunctionsContext.class,0);
		}
		public TerminalNode OPEN() { return getToken(DPQLParser.OPEN, 0); }
		public IdContext id() {
			return getRuleContext(IdContext.class,0);
		}
		public TerminalNode CLOSE() { return getToken(DPQLParser.CLOSE, 0); }
		public TerminalNode SIZE() { return getToken(DPQLParser.SIZE, 0); }
		public TerminalNode CARD() { return getToken(DPQLParser.CARD, 0); }
		public TerminalNode SUM() { return getToken(DPQLParser.SUM, 0); }
		public TerminalNode MINIMUM() { return getToken(DPQLParser.MINIMUM, 0); }
		public TerminalNode MEAN() { return getToken(DPQLParser.MEAN, 0); }
		public TerminalNode MAXIMUM() { return getToken(DPQLParser.MAXIMUM, 0); }
		public TerminalNode AVG() { return getToken(DPQLParser.AVG, 0); }
		public TerminalNode TYPE() { return getToken(DPQLParser.TYPE, 0); }
		public TerminalNode NULL() { return getToken(DPQLParser.NULL, 0); }
		public TerminalNode UNIQNESS() { return getToken(DPQLParser.UNIQNESS, 0); }
		public TerminalNode OVERLAP() { return getToken(DPQLParser.OVERLAP, 0); }
		public NumericFunctionsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_numericFunctions; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DPQLParserListener ) ((DPQLParserListener)listener).enterNumericFunctions(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DPQLParserListener ) ((DPQLParserListener)listener).exitNumericFunctions(this);
		}
	}

	public final NumericFunctionsContext numericFunctions() throws RecognitionException {
		NumericFunctionsContext _localctx = new NumericFunctionsContext(_ctx, getState());
		enterRule(_localctx, 20, RULE_numericFunctions);
		int _la;
		try {
			setState(139);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case DECIMAL_LITERAL:
				enterOuterAlt(_localctx, 1);
				{
				{
				setState(131);
				match(DECIMAL_LITERAL);
				setState(132);
				match(DECIMAL_OPERATION);
				setState(133);
				numericFunctions();
				}
				}
				break;
			case SIZE:
			case CARD:
			case SUM:
			case MINIMUM:
			case MEAN:
			case MAXIMUM:
			case AVG:
			case TYPE:
			case NULL:
			case UNIQNESS:
			case OVERLAP:
				enterOuterAlt(_localctx, 2);
				{
				{
				setState(134);
				_la = _input.LA(1);
				if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 17562154827776L) != 0)) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(135);
				match(OPEN);
				setState(136);
				id();
				setState(137);
				match(CLOSE);
				}
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class LimitContext extends ParserRuleContext {
		public TerminalNode LIMIT() { return getToken(DPQLParser.LIMIT, 0); }
		public TerminalNode DECIMAL_LITERAL() { return getToken(DPQLParser.DECIMAL_LITERAL, 0); }
		public LimitContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_limit; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DPQLParserListener ) ((DPQLParserListener)listener).enterLimit(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DPQLParserListener ) ((DPQLParserListener)listener).exitLimit(this);
		}
	}

	public final LimitContext limit() throws RecognitionException {
		LimitContext _localctx = new LimitContext(_ctx, getState());
		enterRule(_localctx, 22, RULE_limit);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(141);
			match(LIMIT);
			setState(142);
			match(DECIMAL_LITERAL);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class OrderByContext extends ParserRuleContext {
		public TerminalNode ORDERBY() { return getToken(DPQLParser.ORDERBY, 0); }
		public FunctionOrIDContext functionOrID() {
			return getRuleContext(FunctionOrIDContext.class,0);
		}
		public TerminalNode ASC() { return getToken(DPQLParser.ASC, 0); }
		public TerminalNode DESC() { return getToken(DPQLParser.DESC, 0); }
		public OrderByContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_orderBy; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DPQLParserListener ) ((DPQLParserListener)listener).enterOrderBy(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DPQLParserListener ) ((DPQLParserListener)listener).exitOrderBy(this);
		}
	}

	public final OrderByContext orderBy() throws RecognitionException {
		OrderByContext _localctx = new OrderByContext(_ctx, getState());
		enterRule(_localctx, 24, RULE_orderBy);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(144);
			match(ORDERBY);
			setState(145);
			functionOrID();
			setState(147);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==ASC || _la==DESC) {
				{
				setState(146);
				_la = _input.LA(1);
				if ( !(_la==ASC || _la==DESC) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class GroupByContext extends ParserRuleContext {
		public TerminalNode GROUPBY() { return getToken(DPQLParser.GROUPBY, 0); }
		public FunctionOrIDContext functionOrID() {
			return getRuleContext(FunctionOrIDContext.class,0);
		}
		public GroupByContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_groupBy; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DPQLParserListener ) ((DPQLParserListener)listener).enterGroupBy(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DPQLParserListener ) ((DPQLParserListener)listener).exitGroupBy(this);
		}
	}

	public final GroupByContext groupBy() throws RecognitionException {
		GroupByContext _localctx = new GroupByContext(_ctx, getState());
		enterRule(_localctx, 26, RULE_groupBy);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(149);
			match(GROUPBY);
			setState(150);
			functionOrID();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class FunctionOrIDContext extends ParserRuleContext {
		public NumericFunctionsContext numericFunctions() {
			return getRuleContext(NumericFunctionsContext.class,0);
		}
		public IdContext id() {
			return getRuleContext(IdContext.class,0);
		}
		public FunctionOrIDContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_functionOrID; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DPQLParserListener ) ((DPQLParserListener)listener).enterFunctionOrID(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DPQLParserListener ) ((DPQLParserListener)listener).exitFunctionOrID(this);
		}
	}

	public final FunctionOrIDContext functionOrID() throws RecognitionException {
		FunctionOrIDContext _localctx = new FunctionOrIDContext(_ctx, getState());
		enterRule(_localctx, 28, RULE_functionOrID);
		try {
			setState(154);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case SIZE:
			case CARD:
			case SUM:
			case MINIMUM:
			case MEAN:
			case MAXIMUM:
			case AVG:
			case TYPE:
			case NULL:
			case UNIQNESS:
			case OVERLAP:
			case DECIMAL_LITERAL:
				enterOuterAlt(_localctx, 1);
				{
				setState(152);
				numericFunctions();
				}
				break;
			case STAR:
			case ID:
				enterOuterAlt(_localctx, 2);
				{
				setState(153);
				id();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class IdContext extends ParserRuleContext {
		public TerminalNode ID() { return getToken(DPQLParser.ID, 0); }
		public TerminalNode STAR() { return getToken(DPQLParser.STAR, 0); }
		public IdContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_id; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DPQLParserListener ) ((DPQLParserListener)listener).enterId(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DPQLParserListener ) ((DPQLParserListener)listener).exitId(this);
		}
	}

	public final IdContext id() throws RecognitionException {
		IdContext _localctx = new IdContext(_ctx, getState());
		enterRule(_localctx, 30, RULE_id);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(156);
			_la = _input.LA(1);
			if ( !(_la==STAR || _la==ID) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class TableNameContext extends ParserRuleContext {
		public TerminalNode ID() { return getToken(DPQLParser.ID, 0); }
		public TerminalNode STAR() { return getToken(DPQLParser.STAR, 0); }
		public TableNameContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_tableName; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DPQLParserListener ) ((DPQLParserListener)listener).enterTableName(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DPQLParserListener ) ((DPQLParserListener)listener).exitTableName(this);
		}
	}

	public final TableNameContext tableName() throws RecognitionException {
		TableNameContext _localctx = new TableNameContext(_ctx, getState());
		enterRule(_localctx, 32, RULE_tableName);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(158);
			_la = _input.LA(1);
			if ( !(_la==STAR || _la==ID) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static final String _serializedATN =
		"\u0004\u00012\u00a1\u0002\u0000\u0007\u0000\u0002\u0001\u0007\u0001\u0002"+
		"\u0002\u0007\u0002\u0002\u0003\u0007\u0003\u0002\u0004\u0007\u0004\u0002"+
		"\u0005\u0007\u0005\u0002\u0006\u0007\u0006\u0002\u0007\u0007\u0007\u0002"+
		"\b\u0007\b\u0002\t\u0007\t\u0002\n\u0007\n\u0002\u000b\u0007\u000b\u0002"+
		"\f\u0007\f\u0002\r\u0007\r\u0002\u000e\u0007\u000e\u0002\u000f\u0007\u000f"+
		"\u0002\u0010\u0007\u0010\u0001\u0000\u0001\u0000\u0001\u0000\u0001\u0000"+
		"\u0003\u0000\'\b\u0000\u0001\u0000\u0003\u0000*\b\u0000\u0001\u0000\u0003"+
		"\u0000-\b\u0000\u0001\u0000\u0001\u0000\u0001\u0001\u0001\u0001\u0001"+
		"\u0001\u0001\u0002\u0001\u0002\u0001\u0002\u0005\u00027\b\u0002\n\u0002"+
		"\f\u0002:\t\u0002\u0001\u0003\u0001\u0003\u0001\u0003\u0003\u0003?\b\u0003"+
		"\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0005\u0001\u0005\u0001\u0005"+
		"\u0005\u0005G\b\u0005\n\u0005\f\u0005J\t\u0005\u0001\u0006\u0001\u0006"+
		"\u0001\u0006\u0001\u0006\u0001\u0006\u0005\u0006Q\b\u0006\n\u0006\f\u0006"+
		"T\t\u0006\u0001\u0006\u0001\u0006\u0003\u0006X\b\u0006\u0001\u0006\u0001"+
		"\u0006\u0001\u0007\u0001\u0007\u0001\u0007\u0001\b\u0001\b\u0001\b\u0005"+
		"\bb\b\b\n\b\f\be\t\b\u0001\t\u0003\th\b\t\u0001\t\u0001\t\u0001\t\u0001"+
		"\t\u0001\t\u0001\t\u0001\t\u0001\t\u0001\t\u0001\t\u0001\t\u0001\t\u0001"+
		"\t\u0001\t\u0001\t\u0001\t\u0001\t\u0001\t\u0003\t|\b\t\u0001\t\u0001"+
		"\t\u0003\t\u0080\b\t\u0003\t\u0082\b\t\u0001\n\u0001\n\u0001\n\u0001\n"+
		"\u0001\n\u0001\n\u0001\n\u0001\n\u0003\n\u008c\b\n\u0001\u000b\u0001\u000b"+
		"\u0001\u000b\u0001\f\u0001\f\u0001\f\u0003\f\u0094\b\f\u0001\r\u0001\r"+
		"\u0001\r\u0001\u000e\u0001\u000e\u0003\u000e\u009b\b\u000e\u0001\u000f"+
		"\u0001\u000f\u0001\u0010\u0001\u0010\u0001\u0010\u0000\u0000\u0011\u0000"+
		"\u0002\u0004\u0006\b\n\f\u000e\u0010\u0012\u0014\u0016\u0018\u001a\u001c"+
		"\u001e \u0000\u0007\u0001\u0000\u000f\u0010\u0001\u0000\u0014\u0016\u0002"+
		"\u0000\u0017\u0018\u001a\u001c\u0001\u0000\u001d\u001e\u0003\u0000\u0019"+
		"\u0019  #+\u0001\u0000-.\u0002\u0000\u0007\u000711\u00a2\u0000\"\u0001"+
		"\u0000\u0000\u0000\u00020\u0001\u0000\u0000\u0000\u00043\u0001\u0000\u0000"+
		"\u0000\u0006;\u0001\u0000\u0000\u0000\b@\u0001\u0000\u0000\u0000\nC\u0001"+
		"\u0000\u0000\u0000\fK\u0001\u0000\u0000\u0000\u000e[\u0001\u0000\u0000"+
		"\u0000\u0010^\u0001\u0000\u0000\u0000\u0012g\u0001\u0000\u0000\u0000\u0014"+
		"\u008b\u0001\u0000\u0000\u0000\u0016\u008d\u0001\u0000\u0000\u0000\u0018"+
		"\u0090\u0001\u0000\u0000\u0000\u001a\u0095\u0001\u0000\u0000\u0000\u001c"+
		"\u009a\u0001\u0000\u0000\u0000\u001e\u009c\u0001\u0000\u0000\u0000 \u009e"+
		"\u0001\u0000\u0000\u0000\"#\u0003\u0002\u0001\u0000#$\u0003\b\u0004\u0000"+
		"$&\u0003\u000e\u0007\u0000%\'\u0003\u001a\r\u0000&%\u0001\u0000\u0000"+
		"\u0000&\'\u0001\u0000\u0000\u0000\')\u0001\u0000\u0000\u0000(*\u0003\u0018"+
		"\f\u0000)(\u0001\u0000\u0000\u0000)*\u0001\u0000\u0000\u0000*,\u0001\u0000"+
		"\u0000\u0000+-\u0003\u0016\u000b\u0000,+\u0001\u0000\u0000\u0000,-\u0001"+
		"\u0000\u0000\u0000-.\u0001\u0000\u0000\u0000./\u0005\u0000\u0000\u0001"+
		"/\u0001\u0001\u0000\u0000\u000001\u0005\u0006\u0000\u000012\u0003\u0004"+
		"\u0002\u00002\u0003\u0001\u0000\u0000\u000038\u0003\u0006\u0003\u0000"+
		"45\u0005\u000b\u0000\u000057\u0003\u0006\u0003\u000064\u0001\u0000\u0000"+
		"\u00007:\u0001\u0000\u0000\u000086\u0001\u0000\u0000\u000089\u0001\u0000"+
		"\u0000\u00009\u0005\u0001\u0000\u0000\u0000:8\u0001\u0000\u0000\u0000"+
		";>\u0003\u001c\u000e\u0000<=\u0005\r\u0000\u0000=?\u0003\u001e\u000f\u0000"+
		"><\u0001\u0000\u0000\u0000>?\u0001\u0000\u0000\u0000?\u0007\u0001\u0000"+
		"\u0000\u0000@A\u0005\f\u0000\u0000AB\u0003\n\u0005\u0000B\t\u0001\u0000"+
		"\u0000\u0000CH\u0003\f\u0006\u0000DE\u0005\u000b\u0000\u0000EG\u0003\f"+
		"\u0006\u0000FD\u0001\u0000\u0000\u0000GJ\u0001\u0000\u0000\u0000HF\u0001"+
		"\u0000\u0000\u0000HI\u0001\u0000\u0000\u0000I\u000b\u0001\u0000\u0000"+
		"\u0000JH\u0001\u0000\u0000\u0000KL\u0005\u000e\u0000\u0000LM\u0005\u0011"+
		"\u0000\u0000MR\u0003 \u0010\u0000NO\u0005\u000b\u0000\u0000OQ\u0003 \u0010"+
		"\u0000PN\u0001\u0000\u0000\u0000QT\u0001\u0000\u0000\u0000RP\u0001\u0000"+
		"\u0000\u0000RS\u0001\u0000\u0000\u0000SU\u0001\u0000\u0000\u0000TR\u0001"+
		"\u0000\u0000\u0000UW\u0005\u0012\u0000\u0000VX\u0005\r\u0000\u0000WV\u0001"+
		"\u0000\u0000\u0000WX\u0001\u0000\u0000\u0000XY\u0001\u0000\u0000\u0000"+
		"YZ\u0003\u001e\u000f\u0000Z\r\u0001\u0000\u0000\u0000[\\\u0005\u0013\u0000"+
		"\u0000\\]\u0003\u0010\b\u0000]\u000f\u0001\u0000\u0000\u0000^c\u0003\u0012"+
		"\t\u0000_`\u0007\u0000\u0000\u0000`b\u0003\u0012\t\u0000a_\u0001\u0000"+
		"\u0000\u0000be\u0001\u0000\u0000\u0000ca\u0001\u0000\u0000\u0000cd\u0001"+
		"\u0000\u0000\u0000d\u0011\u0001\u0000\u0000\u0000ec\u0001\u0000\u0000"+
		"\u0000fh\u0005\u0005\u0000\u0000gf\u0001\u0000\u0000\u0000gh\u0001\u0000"+
		"\u0000\u0000h\u0081\u0001\u0000\u0000\u0000ij\u0007\u0001\u0000\u0000"+
		"jk\u0005\u0011\u0000\u0000kl\u0003\u001e\u000f\u0000lm\u0005\u0012\u0000"+
		"\u0000m\u0082\u0001\u0000\u0000\u0000no\u0007\u0002\u0000\u0000op\u0005"+
		"\u0011\u0000\u0000pq\u0003\u001e\u000f\u0000qr\u0005\u000b\u0000\u0000"+
		"rs\u0003\u001e\u000f\u0000st\u0005\u0012\u0000\u0000t\u0082\u0001\u0000"+
		"\u0000\u0000u{\u0003\u0014\n\u0000v|\u0005\u001e\u0000\u0000w|\u0005\u001d"+
		"\u0000\u0000xy\u0007\u0003\u0000\u0000y|\u0005\u001f\u0000\u0000z|\u0005"+
		"\u001f\u0000\u0000{v\u0001\u0000\u0000\u0000{w\u0001\u0000\u0000\u0000"+
		"{x\u0001\u0000\u0000\u0000{z\u0001\u0000\u0000\u0000|\u007f\u0001\u0000"+
		"\u0000\u0000}\u0080\u00050\u0000\u0000~\u0080\u0003\u0014\n\u0000\u007f"+
		"}\u0001\u0000\u0000\u0000\u007f~\u0001\u0000\u0000\u0000\u0080\u0082\u0001"+
		"\u0000\u0000\u0000\u0081i\u0001\u0000\u0000\u0000\u0081n\u0001\u0000\u0000"+
		"\u0000\u0081u\u0001\u0000\u0000\u0000\u0082\u0013\u0001\u0000\u0000\u0000"+
		"\u0083\u0084\u00050\u0000\u0000\u0084\u0085\u0005/\u0000\u0000\u0085\u008c"+
		"\u0003\u0014\n\u0000\u0086\u0087\u0007\u0004\u0000\u0000\u0087\u0088\u0005"+
		"\u0011\u0000\u0000\u0088\u0089\u0003\u001e\u000f\u0000\u0089\u008a\u0005"+
		"\u0012\u0000\u0000\u008a\u008c\u0001\u0000\u0000\u0000\u008b\u0083\u0001"+
		"\u0000\u0000\u0000\u008b\u0086\u0001\u0000\u0000\u0000\u008c\u0015\u0001"+
		"\u0000\u0000\u0000\u008d\u008e\u0005,\u0000\u0000\u008e\u008f\u00050\u0000"+
		"\u0000\u008f\u0017\u0001\u0000\u0000\u0000\u0090\u0091\u0005!\u0000\u0000"+
		"\u0091\u0093\u0003\u001c\u000e\u0000\u0092\u0094\u0007\u0005\u0000\u0000"+
		"\u0093\u0092\u0001\u0000\u0000\u0000\u0093\u0094\u0001\u0000\u0000\u0000"+
		"\u0094\u0019\u0001\u0000\u0000\u0000\u0095\u0096\u0005\"\u0000\u0000\u0096"+
		"\u0097\u0003\u001c\u000e\u0000\u0097\u001b\u0001\u0000\u0000\u0000\u0098"+
		"\u009b\u0003\u0014\n\u0000\u0099\u009b\u0003\u001e\u000f\u0000\u009a\u0098"+
		"\u0001\u0000\u0000\u0000\u009a\u0099\u0001\u0000\u0000\u0000\u009b\u001d"+
		"\u0001\u0000\u0000\u0000\u009c\u009d\u0007\u0006\u0000\u0000\u009d\u001f"+
		"\u0001\u0000\u0000\u0000\u009e\u009f\u0007\u0006\u0000\u0000\u009f!\u0001"+
		"\u0000\u0000\u0000\u0010&),8>HRWcg{\u007f\u0081\u008b\u0093\u009a";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}