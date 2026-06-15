package de.metaserve.parser;

import de.metaserve.util.cli.DPQLConsoleHighlighter;
import de.metaserve.util.exceptions.ParseException;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;

public class PrettyErrorListener extends BaseErrorListener {

    private final String source;

    public PrettyErrorListener(String source) {
        this.source = source;
    }

    @Override
    public void syntaxError(Recognizer<?, ?> recognizer,
                            Object offendingSymbol,
                            int line,
                            int charPositionInLine,
                            String msg,
                            RecognitionException e) {

        int stopCol = charPositionInLine;
        if (offendingSymbol instanceof Token) {
            Token tok = (Token) offendingSymbol;
            if (tok.getStopIndex() >= 0 && tok.getStartIndex() >= 0) {
                // make a best effort to span the token
                stopCol = charPositionInLine + Math.max(0, tok.getText() != null ? tok.getText().length() - 1 : 0);
            }
        }

        String underline = DPQLConsoleHighlighter.underlineRange(
                source, line, charPositionInLine, stopCol
        );
        String colorCoded = DPQLConsoleHighlighter.highlight(underline, true);
        colorCoded = colorCoded.substring(0, colorCoded.length() - 9);
        String message = "Syntax error at line " + line + ", col " + charPositionInLine + ":\n"
                + colorCoded + "\n" + msg;

        throw new ParseException(message, e);
    }
}
