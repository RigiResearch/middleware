package com.rigiresearch.middleware.notations.hcl.runtime;

import org.eclipse.xtext.conversion.impl.AbstractToStringConverter;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.util.Strings;

/**
 * Customizes how text literals are converted.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
public final class HclTextLiteralValueConverter
    extends AbstractToStringConverter<String> {

    @Override
    protected String internalToValue(final String content, final INode node) {
        final String value;
        if (content.charAt(0) == '"'
            && content.charAt(content.length() - 1) == '"'
            && content.length() > 1) {
            value = content.substring(1, content.length() - 1);
        } else {
            value = content;
        }
        return Strings.convertToJavaString(value, false);
    }

}
