package org.nexus.nexussolairy.service.parser;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonToken;
import org.antlr.v4.runtime.Token;
import org.nexus.nexussolairy.YLexer;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Queue;

public class YIdentationLexer extends YLexer {

    // control de cola para poder llevar los indent
    private final Deque<Integer> indentStack = new ArrayDeque<>();
    private final Queue<Token> tokenQueue = new ArrayDeque<>();
    private boolean atStartOfLine = true;
    // control para los () [] {}
    private int openedBrackets = 0;
    private int lastTokenType = Token.INVALID_TYPE;
    private final java.util.List<org.nexus.nexussolairy.model.syntactic.SyntaxError> indentationErrors = new java.util.ArrayList<>();

    public java.util.List<org.nexus.nexussolairy.model.syntactic.SyntaxError> getIndentationErrors() {
        return indentationErrors;
    }

    public YIdentationLexer(CharStream input) {
        super(input);
        // todos con base nivel 0 para los indent
        indentStack.push(0);
    }

    @Override
    public Token nextToken() {
        Token token = doNextToken();
        if (token != null) {
            lastTokenType = token.getType();
        }
        return token;
    }

    private Token doNextToken() {
        if (!tokenQueue.isEmpty()) {
            return tokenQueue.poll();
        }

        Token token = getNextNonCommentToken();

        while (token.getType() != Token.EOF) {

            // verificar si son ( [ {
            if (token.getType() == LPAREN || token.getType() == LBRACK || token.getType() == LBRACE) {
                openedBrackets++;
                // verificacion del cierre
            } else if (token.getType() == RPAREN || token.getType() == RBRACK || token.getType() == RBRACE) {
                if (openedBrackets > 0) openedBrackets--;
            }

            if (token.getType() == NEWLINE) {
                atStartOfLine = true;

                if (openedBrackets > 0) {
                    token = getNextNonCommentToken();
                    continue;
                }
                return token;
            }

            if (atStartOfLine) {
                int spaces = 0;

                // esta parte hay que cambiar el nombre WS para TAB
                if (token.getType() == TAB) {
                    // cuantos espacios lleva
                    spaces = calculateIndentation(token.getText());

                    Token next = getNextNonCommentToken();

                    // espacios antes de una nueva linea o final de codigo
                    if (next.getType() == NEWLINE || next.getType() == EOF) {
                        token = next;
                        continue;
                    }

                    processIndent(spaces, next);
                    token = tokenQueue.poll();
                } else {
                    processIndent(0, token);
                    token = tokenQueue.poll();
                }

                atStartOfLine = false;
                break;
            } else {
                if (token.getType() == TAB) {
                    token = getNextNonCommentToken();
                    continue;
                }
                break;
            }
        }

        // al llegar al final se vacia la pila y se va con los DEDENT
        if (token.getType() == Token.EOF) {
            if (lastTokenType != Token.INVALID_TYPE && lastTokenType != NEWLINE) {
                tokenQueue.offer(createSyntheticToken(NEWLINE));
            }
            while (indentStack.peek() > 0) {
                indentStack.pop();
                tokenQueue.offer(createSyntheticToken(DEDENT));
            }
            if (!tokenQueue.isEmpty()) {
                tokenQueue.offer(token);
                return tokenQueue.poll();
            }
        }

        return token;
    }


    private void processIndent(int spaces, Token current) {
        int currentIndent = indentStack.peek();

        if (spaces > currentIndent) {
            indentStack.push(spaces);
            tokenQueue.offer(createSyntheticToken(INDENT));
        } else if (spaces < currentIndent) {
            while (indentStack.peek() > spaces) {
                indentStack.pop();
                tokenQueue.offer(createSyntheticToken(DEDENT));
            }

            if (indentStack.peek() != spaces) {
                System.out.println("Error con la identacion" + current.getLine());
                int errLine = current != null ? Math.max(1, current.getLine()) : 1;
                int errCol = current != null ? Math.max(1, current.getCharPositionInLine() + 1) : 1;
                indentationErrors.add(new org.nexus.nexussolairy.model.syntactic.SyntaxError("Error", errLine, errCol, "Error de identación: el nivel de desidentación (" + spaces + " espacios) no coincide con ningún nivel exterior."));
            }
        }
        tokenQueue.offer(current);
    }

    private int calculateIndentation(String text) {
        int count = 0;

        for (char c : text.toCharArray()) {
            if (c == '\t') count += 4;
            else if (c == ' ') count += 1;
        }

        return count;
    }

    private CommonToken createSyntheticToken(int type) {
        String text = switch (type) {
            case INDENT -> "<INDENT>";
            case DEDENT -> "<DEDENT>";
            case NEWLINE -> "\n";
            default -> "";
        };
        CommonToken synthetic = new CommonToken(type, text);
        synthetic.setLine(getLine());
        synthetic.setCharPositionInLine(getCharPositionInLine());
        synthetic.setChannel(Token.DEFAULT_CHANNEL);
        return synthetic;
    }

    private Token getNextNonCommentToken() {
        Token t = super.nextToken();
        while (t.getType() == LINE_COMMENT || t.getType() == BLOCK_COMMENT) {
            t = super.nextToken();
        }
        return t;
    }
}
