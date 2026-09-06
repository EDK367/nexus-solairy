package com.example.nexusantiquus.ui;

import com.example.nexusantiquus.LatinusLexer;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.Token;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;
import org.nexus.nexussolairy.model.enums.LanguageType;

import java.util.Collection;
import java.util.Collections;

public final class SintaxColor {

    private SintaxColor() {
    }

    public static StyleSpans<Collection<String>> compute(String text) {
        return org.nexus.nexussolairy.ui.SintaxColor.compute(text, LanguageType.PIG_LATIN);
    }

    public static StyleSpans<Collection<String>> compute(String text, LanguageType language) {
        return org.nexus.nexussolairy.ui.SintaxColor.compute(text, language);
    }

    public static StyleSpans<Collection<String>> computeLatinus(String text) {
        if (text == null) {
            text = "";
        }

        if (text.isEmpty()) {
            StyleSpansBuilder<Collection<String>> builder = new StyleSpansBuilder<>();
            builder.add(Collections.emptyList(), 0);
            return builder.create();
        }

        LatinusLexer lexer = new LatinusLexer(CharStreams.fromString(text));
        lexer.removeErrorListeners();

        StyleSpansBuilder<Collection<String>> spans = new StyleSpansBuilder<>();
        int lastEnd = 0;
        Token token;

        while ((token = lexer.nextToken()).getType() != Token.EOF) {
            int start = token.getStartIndex();
            int stop = token.getStopIndex();

            if (start < 0 || stop < start) {
                continue;
            }

            start = Math.max(0, Math.min(start, text.length()));
            stop = Math.max(start, Math.min(stop, text.length() - 1));

            if (start > lastEnd) {
                spans.add(Collections.emptyList(), start - lastEnd);
            }

            int length = stop - start + 1;

            if (length > 0) {
                String cssClass = mapToken(token.getType());

                if (cssClass == null || cssClass.isEmpty()) {
                    spans.add(Collections.emptyList(), length);
                } else {
                    spans.add(Collections.singleton(cssClass), length);
                }

                lastEnd = stop + 1;
            }
        }

        if (lastEnd < text.length()) {
            spans.add(Collections.emptyList(), text.length() - lastEnd);
        }

        if (lastEnd == 0 && text.length() > 0) {
            spans.add(Collections.emptyList(), text.length());
        }

        return spans.create();
    }

    private static String mapToken(int type) {
        if (type == Token.EOF) {
            return "";
        }

        return switch (type) {
            case LatinusLexer.VARIABILES, LatinusLexer.MAIOR, LatinusLexer.FINIS_PROG -> "section";
            case LatinusLexer.ESTO, LatinusLexer.SERIES, LatinusLexer.FINIS, LatinusLexer.IMPORT, LatinusLexer.NOVUS -> "decl";
            case LatinusLexer.SI, LatinusLexer.ALITER, LatinusLexer.DUM, LatinusLexer.FACERE, LatinusLexer.PER,
                 LatinusLexer.PERGE, LatinusLexer.INTERRUMPE -> "control";
            case LatinusLexer.NUMERUS, LatinusLexer.TEXTUM, LatinusLexer.DECIMALIS, LatinusLexer.LITTERA,
                 LatinusLexer.BOOL -> "type";
            case LatinusLexer.VERUM, LatinusLexer.FALSUS -> "boolean";
            case LatinusLexer.NUMBER, LatinusLexer.DECIMAL -> "number";
            case LatinusLexer.STRING -> "string";
            case LatinusLexer.CHAR -> "char";
            case LatinusLexer.NOT, LatinusLexer.EQ, LatinusLexer.NEQ, LatinusLexer.LE, LatinusLexer.GE, LatinusLexer.AND, LatinusLexer.OR,
                 LatinusLexer.INC, LatinusLexer.DEC, LatinusLexer.READ, LatinusLexer.PRINT, LatinusLexer.LT,
                 LatinusLexer.GT, LatinusLexer.PLUS, LatinusLexer.MINUS, LatinusLexer.MULT, LatinusLexer.DIV,
                 LatinusLexer.ASSIGN -> "operator";
            case LatinusLexer.COLON, LatinusLexer.SEMI, LatinusLexer.COMMA, LatinusLexer.DOT, LatinusLexer.LBRACE,
                 LatinusLexer.RBRACE, LatinusLexer.LBRACK, LatinusLexer.RBRACK, LatinusLexer.LPAREN,
                 LatinusLexer.RPAREN -> "punct";
            case LatinusLexer.LINE_COMMENT, LatinusLexer.BLOCK_COMMENT -> "comment";
            default -> "";
        };
    }
}
