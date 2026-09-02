package org.nexus.nexussolairy.service.grammar;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.Token;
import org.nexus.nexussolairy.PigLatinLexer;
import org.nexus.nexussolairy.model.lexical.TokenInfo;
import org.nexus.nexussolairy.utils.ResultLexer;

public class PigLatinLexerService implements LexerService {

    @Override
    public ResultLexer analyze(String sourceCode) {

        ResultLexer result = new ResultLexer();

        PigLatinLexer lexer = new PigLatinLexer(CharStreams.fromString(sourceCode));
        LexicalErrorListener errorListener = new LexicalErrorListener();
        lexer.removeErrorListeners();
        lexer.addErrorListener(errorListener);

        for (Token token = lexer.nextToken(); token.getType() != Token.EOF; token = lexer.nextToken()) {

            if (token.getChannel() != Token.DEFAULT_CHANNEL) continue;

            String typeName = PigLatinLexer.VOCABULARY.getSymbolicName(token.getType());

            result.tokens.add(new TokenInfo(
                    typeName,
                    token.getText(),
                    token.getLine(),
                    token.getCharPositionInLine() + 1
            ));
        }
        result.errors.addAll(errorListener.getErrors());
        return result;
    }
}
