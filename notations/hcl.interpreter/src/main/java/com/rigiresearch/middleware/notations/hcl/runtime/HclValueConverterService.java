package com.rigiresearch.middleware.notations.hcl.runtime;

import org.eclipse.xtext.conversion.IValueConverter;
import org.eclipse.xtext.conversion.ValueConverter;
import org.eclipse.xtext.conversion.impl.AbstractDeclarativeValueConverterService;

/**
 * Customizes the converter for the STRING terminal.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
public final class HclValueConverterService
    extends AbstractDeclarativeValueConverterService {

    /**
     * Customizes how text literals are converted to String.
     * @return A converter instance
     */
    @ValueConverter(rule = "com.rigiresearch.middleware.notations.hcl.Hcl.TextLiteral")
    public IValueConverter<String> textLiteral() {
        return new HclTextLiteralValueConverter();
    }

}
