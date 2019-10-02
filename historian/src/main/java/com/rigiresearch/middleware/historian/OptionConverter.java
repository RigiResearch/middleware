package com.rigiresearch.middleware.historian;

import com.beust.jcommander.IStringConverter;
import java.util.Locale;

/**
 * A {@link CliArguments.ArtifactType} converter for jCommander.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
public final class OptionConverter
    implements IStringConverter<CliArguments.ArtifactType> {

    @Override
    public CliArguments.ArtifactType convert(final String value) {
        return CliArguments.ArtifactType.valueOf(
            value.toUpperCase(Locale.getDefault())
        );
    }

}
