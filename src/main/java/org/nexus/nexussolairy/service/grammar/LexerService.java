package org.nexus.nexussolairy.service.grammar;

import org.nexus.nexussolairy.utils.ResultLexer;

public interface LexerService {

    ResultLexer analyze(String source);
}
