/*
 * generated by Xtext 2.18.0.M1
 */
package com.rigiresearch.middleware.hcl.interpreter.parser.antlr;

import java.io.InputStream;
import org.eclipse.xtext.parser.antlr.IAntlrTokenFileProvider;

public class HclAntlrTokenFileProvider implements IAntlrTokenFileProvider {

	@Override
	public InputStream getAntlrTokenFile() {
		ClassLoader classLoader = getClass().getClassLoader();
		return classLoader.getResourceAsStream("com/rigiresearch/middleware/hcl/interpreter/parser/antlr/internal/InternalHcl.tokens");
	}
}
