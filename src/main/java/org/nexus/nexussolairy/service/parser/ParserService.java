package org.nexus.nexussolairy.service.parser;

import org.antlr.v4.runtime.Lexer;
import org.nexus.nexussolairy.model.syntactic.SyntaxError;

import java.util.List;

public interface ParserService {

    List<SyntaxError> analyze(String source);

    List<SyntaxError> analyze(Lexer lexer);

    List<SyntaxError> getErrors();
}
