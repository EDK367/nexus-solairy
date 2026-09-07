package org.nexus.nexussolairy.ui;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;
import org.nexus.nexussolairy.*;
import org.nexus.nexussolairy.model.enums.LanguageType;
import org.nexus.nexussolairy.service.parser.YIdentationLexer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public final class SintaxColor {

    private SintaxColor() {
    }

    public static StyleSpans<Collection<String>> compute(String text) {
        return compute(text, LanguageType.PIG_LATIN);
    }

    public static StyleSpans<Collection<String>> compute(String text, LanguageType language) {
        if (language == null) {
            language = LanguageType.PIG_LATIN;
        }

        return switch (language) {
            case PIG_LATIN -> computePigLatin(text);
            case Y_LANG -> computeY(text);
            case ZETARIANO -> computeZetariano(text);
            default -> computeFallback(text);
        };
    }

    public static StyleSpans<Collection<String>> computePigLatin(String text) {
        if (text == null) {
            text = "";
        }

        if (text.isEmpty()) {
            StyleSpansBuilder<Collection<String>> builder = new StyleSpansBuilder<>();
            builder.add(Collections.emptyList(), 0);
            return builder.create();
        }

        List<int[]> errorRanges = collectPigLatinErrors(text);

        PigLatinLexer lexer = new PigLatinLexer(CharStreams.fromString(text));
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
                addSpanWithErrors(spans, lastEnd, start, "", errorRanges);
            }

            int length = stop - start + 1;

            if (length > 0) {
                String cssClass = mapTokenPigLatin(token.getType());
                addSpanWithErrors(spans, start, stop + 1, cssClass, errorRanges);
                lastEnd = stop + 1;
            }
        }

        if (lastEnd < text.length()) {
            addSpanWithErrors(spans, lastEnd, text.length(), "", errorRanges);
        }

        if (lastEnd == 0 && text.length() > 0) {
            addSpanWithErrors(spans, 0, text.length(), "", errorRanges);
        }

        return spans.create();
    }

    public static StyleSpans<Collection<String>> computeY(String text) {
        if (text == null) {
            text = "";
        }

        if (text.isEmpty()) {
            StyleSpansBuilder<Collection<String>> builder = new StyleSpansBuilder<>();
            builder.add(Collections.emptyList(), 0);
            return builder.create();
        }

        List<int[]> errorRanges = collectYErrors(text);

        YLexer lexer = new YLexer(CharStreams.fromString(text));
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
                addSpanWithErrors(spans, lastEnd, start, "", errorRanges);
            }

            int length = stop - start + 1;

            if (length > 0) {
                String cssClass = mapTokenY(token.getType());
                addSpanWithErrors(spans, start, stop + 1, cssClass, errorRanges);
                lastEnd = stop + 1;
            }
        }

        if (lastEnd < text.length()) {
            addSpanWithErrors(spans, lastEnd, text.length(), "", errorRanges);
        }

        if (lastEnd == 0 && text.length() > 0) {
            addSpanWithErrors(spans, 0, text.length(), "", errorRanges);
        }

        return spans.create();
    }

    public static StyleSpans<Collection<String>> computeZetariano(String text) {
        if (text == null) {
            text = "";
        }

        if (text.isEmpty()) {
            StyleSpansBuilder<Collection<String>> builder = new StyleSpansBuilder<>();
            builder.add(Collections.emptyList(), 0);
            return builder.create();
        }
        List<int[]> errorRanges = collectZetarianoErrors(text);

        ZetarianoLexer lexer = new ZetarianoLexer(CharStreams.fromString(text));
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
                addSpanWithErrors(spans, lastEnd, start, "", errorRanges);
            }

            int length = stop - start + 1;

            if (length > 0) {
                String cssClass = mapTokenZetariano(token.getType());
                addSpanWithErrors(spans, start, stop + 1, cssClass, errorRanges);
                lastEnd = stop + 1;
            }
        }

        if (lastEnd < text.length()) {
            addSpanWithErrors(spans, lastEnd, text.length(), "", errorRanges);
        }

        if (lastEnd == 0 && text.length() > 0) {
            addSpanWithErrors(spans, 0, text.length(), "", errorRanges);
        }

        return spans.create();
    }

    public static StyleSpans<Collection<String>> computeFallback(String text) {
        if (text == null) {
            text = "";
        }
        StyleSpansBuilder<Collection<String>> builder = new StyleSpansBuilder<>();
        builder.add(Collections.emptyList(), text.length());
        return builder.create();
    }

    private static List<int[]> collectPigLatinErrors(String text) {
        List<int[]> errorRanges = new ArrayList<>();
        try {
            PigLatinLexer lexer = new PigLatinLexer(CharStreams.fromString(text));
            lexer.removeErrorListeners();
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            PigLatinParser parser = new PigLatinParser(tokens);
            parser.removeErrorListeners();
            parser.addErrorListener(new BaseErrorListener() {
                @Override
                public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
                    int start = -1;
                    int stop = -1;
                    if (offendingSymbol instanceof Token t) {
                        start = t.getStartIndex();
                        stop = t.getStopIndex();
                    }
                    if (start < 0 || stop < start) {
                        start = getOffset(text, line, charPositionInLine);
                        stop = start;
                    }
                    start = Math.max(0, Math.min(start, text.length() - 1));
                    stop = Math.max(start, Math.min(stop, text.length() - 1));
                    errorRanges.add(new int[]{start, stop});
                }
            });
            parser.program();
        } catch (Throwable ignored) {
        }
        return errorRanges;
    }

    private static List<int[]> collectYErrors(String text) {
        List<int[]> errorRanges = new ArrayList<>();
        try {
            YIdentationLexer lexer = new YIdentationLexer(CharStreams.fromString(text));
            lexer.removeErrorListeners();
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            YParser parser = new YParser(tokens);
            parser.removeErrorListeners();
            parser.addErrorListener(new BaseErrorListener() {
                @Override
                public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
                    int start = -1;
                    int stop = -1;
                    if (offendingSymbol instanceof Token t) {
                        start = t.getStartIndex();
                        stop = t.getStopIndex();
                    }
                    if (start < 0 || stop < start) {
                        start = getOffset(text, line, charPositionInLine);
                        stop = start;
                    }
                    start = Math.max(0, Math.min(start, text.length() - 1));
                    stop = Math.max(start, Math.min(stop, text.length() - 1));
                    errorRanges.add(new int[]{start, stop});
                }
            });
            parser.program();
        } catch (Throwable ignored) {
        }
        return errorRanges;
    }

    private static List<int[]> collectZetarianoErrors(String text) {
        List<int[]> errorRanges = new ArrayList<>();
        try {
            ZetarianoLexer lexer = new ZetarianoLexer(CharStreams.fromString(text));
            lexer.removeErrorListeners();
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            ZetarianoParser parser = new ZetarianoParser(tokens);
            parser.removeErrorListeners();
            parser.addErrorListener(new BaseErrorListener() {
                @Override
                public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
                    int start = -1;
                    int stop = -1;
                    if (offendingSymbol instanceof Token t) {
                        start = t.getStartIndex();
                        stop = t.getStopIndex();
                    }
                    if (start < 0 || stop < start) {
                        start = getOffset(text, line, charPositionInLine);
                        stop = start;
                    }
                    start = Math.max(0, Math.min(start, text.length() - 1));
                    stop = Math.max(start, Math.min(stop, text.length() - 1));
                    errorRanges.add(new int[]{start, stop});
                }
            });
            parser.program();
        } catch (Throwable ignored) {
        }
        return errorRanges;
    }

    private static int getOffset(String text, int line, int charPos) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        int currentLine = 1;
        int index = 0;
        while (index < text.length() && currentLine < line) {
            if (text.charAt(index) == '\n') {
                currentLine++;
            }
            index++;
        }
        int offset = index + Math.max(0, charPos);
        return Math.max(0, Math.min(offset, text.length() - 1));
    }

    private static void addSpanWithErrors(StyleSpansBuilder<Collection<String>> spans, int from, int to, String cssClass, List<int[]> errorRanges) {
        if (to <= from) {
            return;
        }

        if (errorRanges == null || errorRanges.isEmpty()) {
            int length = to - from;
            if (cssClass == null || cssClass.isEmpty()) {
                spans.add(Collections.emptyList(), length);
            } else {
                spans.add(Collections.singleton(cssClass), length);
            }
            return;
        }

        boolean hasAnyError = false;
        for (int[] err : errorRanges) {
            if (Math.max(from, err[0]) <= Math.min(to - 1, err[1])) {
                hasAnyError = true;
                break;
            }
        }

        if (!hasAnyError) {
            int length = to - from;
            if (cssClass == null || cssClass.isEmpty()) {
                spans.add(Collections.emptyList(), length);
            } else {
                spans.add(Collections.singleton(cssClass), length);
            }
            return;
        }

        int cur = from;
        while (cur < to) {
            boolean errState = isError(cur, errorRanges);
            int next = cur + 1;
            while (next < to && isError(next, errorRanges) == errState) {
                next++;
            }
            int len = next - cur;
            if (errState) {
                if (cssClass == null || cssClass.isEmpty()) {
                    spans.add(Collections.singleton("syntax-error"), len);
                } else {
                    spans.add(List.of(cssClass, "syntax-error"), len);
                }
            } else {
                if (cssClass == null || cssClass.isEmpty()) {
                    spans.add(Collections.emptyList(), len);
                } else {
                    spans.add(Collections.singleton(cssClass), len);
                }
            }
            cur = next;
        }
    }

    private static boolean isError(int index, List<int[]> errorRanges) {
        for (int[] err : errorRanges) {
            if (index >= err[0] && index <= err[1]) {
                return true;
            }
        }
        return false;
    }

    public static String mapToken(int type) {
        return mapTokenPigLatin(type);
    }

    public static String mapTokenPigLatin(int type) {
        if (type == Token.EOF) {
            return "";
        }

        return switch (type) {
            case PigLatinLexer.VARIABILES, PigLatinLexer.MAIOR, PigLatinLexer.FINIS_PROG -> "section";
            case PigLatinLexer.ESTO, PigLatinLexer.SERIES, PigLatinLexer.FINIS, PigLatinLexer.IMPORT,
                 PigLatinLexer.NOVUS -> "decl";
            case PigLatinLexer.SI, PigLatinLexer.ALITER, PigLatinLexer.DUM, PigLatinLexer.FACERE, PigLatinLexer.PER,
                 PigLatinLexer.PERGE, PigLatinLexer.INTERRUMPE -> "control";
            case PigLatinLexer.NUMERUS, PigLatinLexer.TEXTUM, PigLatinLexer.DECIMALIS, PigLatinLexer.LITTERA,
                 PigLatinLexer.BOOL -> "type";
            case PigLatinLexer.VERUM, PigLatinLexer.FALSUS -> "boolean";
            case PigLatinLexer.NUMBER, PigLatinLexer.DECIMAL -> "number";
            case PigLatinLexer.STRING -> "string";
            case PigLatinLexer.CHAR -> "char";
            case PigLatinLexer.NOT, PigLatinLexer.EQ, PigLatinLexer.NEQ, PigLatinLexer.LE, PigLatinLexer.GE,
                 PigLatinLexer.AND, PigLatinLexer.OR, PigLatinLexer.INC, PigLatinLexer.DEC, PigLatinLexer.READ,
                 PigLatinLexer.PRINT, PigLatinLexer.LT, PigLatinLexer.GT, PigLatinLexer.PLUS, PigLatinLexer.MINUS,
                 PigLatinLexer.MULT, PigLatinLexer.DIV, PigLatinLexer.ASSIGN -> "operator";
            case PigLatinLexer.COLON, PigLatinLexer.SEMI, PigLatinLexer.COMMA, PigLatinLexer.DOT, PigLatinLexer.LBRACE,
                 PigLatinLexer.RBRACE, PigLatinLexer.LBRACK, PigLatinLexer.RBRACK, PigLatinLexer.LPAREN,
                 PigLatinLexer.RPAREN -> "punct";
            case PigLatinLexer.LINE_COMMENT, PigLatinLexer.BLOCK_COMMENT -> "comment";
            default -> "";
        };
    }

    public static String mapTokenY(int type) {
        if (type == Token.EOF) {
            return "";
        }

        return switch (type) {
            case YLexer.SEC_STRUCT, YLexer.SEC_FUNCTION -> "section";
            case YLexer.ESTRUCTURA, YLexer.DEFINIR -> "decl";
            case YLexer.SI, YLexer.ENTONCES, YLexer.SINO, YLexer.CONTRARIO, YLexer.ELEGIR, YLexer.CASO, YLexer.SIEMPRE,
                 YLexer.PARA, YLexer.MIENTRAS, YLexer.HACER, YLexer.ROMPER, YLexer.CONTINUAR -> "control";
            case YLexer.LEER, YLexer.IMPRIMIR, YLexer.RETORNAR -> "function";
            case YLexer.CADENA, YLexer.ENTERO, YLexer.FLOTANTE, YLexer.CARACTER, YLexer.BOOL -> "type";
            case YLexer.VERDADERO, YLexer.FALSO -> "boolean";
            case YLexer.NUMBER, YLexer.DECIMAL -> "number";
            case YLexer.STRING -> "string";
            case YLexer.CHAR -> "char";
            case YLexer.RETURN_VALUE, YLexer.NOT, YLexer.AND, YLexer.OR, YLexer.EQ, YLexer.NEQ, YLexer.LE, YLexer.GE,
                 YLexer.LT, YLexer.GT, YLexer.INC, YLexer.DEC, YLexer.PLUS, YLexer.MINUS, YLexer.MULT, YLexer.DIV,
                 YLexer.ASSIGN -> "operator";
            case YLexer.COLON, YLexer.SEMI, YLexer.COMMA, YLexer.DOT, YLexer.LBRACE, YLexer.RBRACE, YLexer.LBRACK,
                 YLexer.RBRACK, YLexer.LPAREN, YLexer.RPAREN -> "punct";
            case YLexer.LINE_COMMENT, YLexer.BLOCK_COMMENT -> "comment";
            default -> "";
        };
    }

    public static String mapTokenZetariano(int type) {
        if (type == Token.EOF) {
            return "";
        }

        return switch (type) {
            case ZetarianoLexer.PUBLIC, ZetarianoLexer.CLASS, ZetarianoLexer.NEW -> "decl";
            case ZetarianoLexer.IF, ZetarianoLexer.ELSE, ZetarianoLexer.SWITCH, ZetarianoLexer.CASE,
                 ZetarianoLexer.DEFAULT, ZetarianoLexer.FOR, ZetarianoLexer.WHILE, ZetarianoLexer.DO,
                 ZetarianoLexer.RETURN, ZetarianoLexer.CONTINUE, ZetarianoLexer.BREAK -> "control";
            case ZetarianoLexer.READ, ZetarianoLexer.PRINT, ZetarianoLexer.PRINTLN -> "function";
            case ZetarianoLexer.VOID, ZetarianoLexer.INT, ZetarianoLexer.DOUBLE, ZetarianoLexer.CHAR_TYPE,
                 ZetarianoLexer.BOOLEAN, ZetarianoLexer.STRING_TYPE -> "type";
            case ZetarianoLexer.TRUE, ZetarianoLexer.FALSE, ZetarianoLexer.NULL -> "boolean";
            case ZetarianoLexer.NUMBER, ZetarianoLexer.DECIMAL -> "number";
            case ZetarianoLexer.STRING -> "string";
            case ZetarianoLexer.CHAR -> "char";
            case ZetarianoLexer.ADD_ASSIGN, ZetarianoLexer.SUB_ASSIGN, ZetarianoLexer.MUL_ASSIGN,
                 ZetarianoLexer.DIV_ASSIGN, ZetarianoLexer.NOT, ZetarianoLexer.AND, ZetarianoLexer.OR,
                 ZetarianoLexer.EQ, ZetarianoLexer.NEQ, ZetarianoLexer.LE, ZetarianoLexer.GE, ZetarianoLexer.LT,
                 ZetarianoLexer.GT, ZetarianoLexer.INC, ZetarianoLexer.DEC, ZetarianoLexer.PLUS, ZetarianoLexer.MINUS,
                 ZetarianoLexer.MULT, ZetarianoLexer.DIV, ZetarianoLexer.MOD, ZetarianoLexer.ASSIGN,
                 ZetarianoLexer.QUESTION -> "operator";
            case ZetarianoLexer.COLON, ZetarianoLexer.SEMI, ZetarianoLexer.COMMA, ZetarianoLexer.DOT,
                 ZetarianoLexer.LBRACE, ZetarianoLexer.RBRACE, ZetarianoLexer.LBRACK, ZetarianoLexer.RBRACK,
                 ZetarianoLexer.LPAREN, ZetarianoLexer.RPAREN -> "punct";
            case ZetarianoLexer.LINE_COMMENT, ZetarianoLexer.BLOCK_COMMENT -> "comment";
            default -> "";
        };
    }
}
