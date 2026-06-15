package de.metaserve.util.cli;

import de.metaserve.DPQLLexer;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonToken;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class DPQLConsoleHighlighter {

    private static final String RESET = "\u001B[0m";
    private static final String FG_KEYWORD = "\u001B[36m";   // cyan
    private static final String FG_FUNC    = "\u001B[35m";   // magenta
    private static final String FG_IDENT   = "\u001B[37m";   // white
    private static final String FG_NUMBER  = "\u001B[33m";   // yellow
    private static final String FG_PUNCT   = "\u001B[90m";   // bright black
    private static final String FG_COMMENT = "\u001B[32m";   // green
    private static final String BG_ERROR   = "\u001B[41m";   // red background

    // Group token types into buckets for colorization
    private static final Set<Integer> KEYWORDS = tokenTypes(
            DPQLLexer.SELECT, DPQLLexer.FROM, DPQLLexer.WHERE, DPQLLexer.AS,
            DPQLLexer.AND, DPQLLexer.OR, DPQLLexer.NOT,
            DPQLLexer.ORDERBY, DPQLLexer.GROUPBY, DPQLLexer.LIMIT,
            DPQLLexer.ASC, DPQLLexer.DESC, DPQLLexer.NULL
    );

    private static final Set<Integer> FUNCTIONS = tokenTypes(
            DPQLLexer.MIN, DPQLLexer.MAX, DPQLLexer.UCC, DPQLLexer.IND, DPQLLexer.FD,
            DPQLLexer.SIZE, DPQLLexer.SPLIT, DPQLLexer.CONTAINS, DPQLLexer.COALESCE,
            DPQLLexer.CARD, DPQLLexer.SUM, DPQLLexer.MINIMUM, DPQLLexer.MEAN,
            DPQLLexer.MAXIMUM, DPQLLexer.AVG, DPQLLexer.TYPE, DPQLLexer.UNIQNESS,
            DPQLLexer.OVERLAP
    );

    private static final Set<Integer> NUMBERS = tokenTypes(DPQLLexer.DECIMAL_LITERAL);
    private static final Set<Integer> PUNCT   = tokenTypes(
            DPQLLexer.OPEN, DPQLLexer.CLOSE, DPQLLexer.COMMA,
            DPQLLexer.PLUS, DPQLLexer.MINUS, DPQLLexer.DIVIDE,
            DPQLLexer.EQUAL, DPQLLexer.SMALLER, DPQLLexer.GREATER, DPQLLexer.STAR
    );

    // Any lexing miss (ERRORCHANNEL)
    private static final int ERROR_CHANNEL = DPQLLexer.ERRORCHANNEL;
    private static final int COMMENT_CHANNEL = DPQLLexer.DPQLCOMMENT;

    private DPQLConsoleHighlighter() {}

    private static Set<Integer> tokenTypes(int... t) {
        return Arrays.stream(t).boxed().collect(Collectors.toSet());
    }

    /** Returns the highlighted string (ANSI) and a list of error ranges to underline. */
    public static String highlight(String input){
        return highlight(input, true);
    }
    public static String highlight(String input, boolean removeEOF) {
        DPQLLexer lexer = new DPQLLexer(CharStreams.fromString(input));
        CommonTokenStream stream = new CommonTokenStream(lexer);
        stream.fill();

        StringBuilder out = new StringBuilder(input.length() + 32);
        List<Token> tokens = stream.getTokens();
        if (removeEOF) {
            tokens.remove(tokens.size()-1);
            tokens.add(new CommonToken(0, ""));
        }

        for (Token t : tokens) {
            String text = t.getText();
            if (text == null) continue;

            // Comments (your lexer already routes MySQL hints to DPQLCOMMENT)
            if (t.getChannel() == COMMENT_CHANNEL) {
                out.append(FG_COMMENT).append(text).append(RESET);
                continue;
            }

            // Whitespace (HIDDEN channel) -> print as is
            if (t.getChannel() == Token.DEFAULT_CHANNEL || t.getChannel() == Token.HIDDEN_CHANNEL) {
                String colored = colorFor(t.getType(), text);
                out.append(colored);
                continue;
            }

            // Error channel: paint with red background
            if (t.getChannel() == ERROR_CHANNEL) {
                out.append(BG_ERROR).append(text).append(RESET);
            } else {
                out.append(text); // any other channel
            }
        }
        return out.toString();
    }

    private static String colorFor(int type, String text) {
        if (KEYWORDS.contains(type)) return FG_KEYWORD + text + RESET;
        if (FUNCTIONS.contains(type)) return FG_FUNC + text + RESET;
        if (NUMBERS.contains(type))  return FG_NUMBER + text + RESET;
        if (PUNCT.contains(type))    return FG_PUNCT + text + RESET;

        // Default: identifiers and everything else
        return FG_IDENT + text + RESET;
    }

    /** Pretty error line with a caret span under the offending range. */
    public static String underlineRange(String input, int line, int startCol, int stopCol) {
        // lines are 1-based from ANTLR
        String[] lines = input.split("\r?\n", -1);
        String srcLine = (line >= 1 && line <= lines.length) ? lines[line - 1] : "";
        StringBuilder sb = new StringBuilder();
        sb.append(srcLine).append('\n');
        for (int i = 0; i < Math.max(0, startCol); i++) sb.append(' ');
        int len = Math.max(1, stopCol - startCol + 1);
        for (int i = 0; i < len; i++) sb.append('^');
        return sb.toString();
    }
}
