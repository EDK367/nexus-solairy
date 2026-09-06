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

    public YIdentationLexer(CharStream input) {
        super(input);
        // todos con base nivel 0 para los indent
        indentStack.push(0);
    }

    @Override
    public Token nextToken() {

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
        CommonToken synthetic = new CommonToken(type, type == INDENT ? "<INDENT>" : "<DEDENT>");
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
