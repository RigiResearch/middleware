package com.rigiresearch.middleware.metamodels;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;

/**
 * A pretty printer for having a raw, human-readable string representation of an
 * Ecore model. Based on <a href="https://www.eclipse.org/forums/index.php?t=msg
 * &th=210069&goto=985477&#msg_985477">this</a> post.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
@RequiredArgsConstructor
public final class EcorePrinter {

    /**
     * The indent string.
     */
    private static final String INDENT = "  ";

    /**
     * The model to print.
     */
    private final EObject model;

    /**
     * Creates a raw, human-readable (pretty) string representation of the
     * model.
     * @return A pretty string
     */
    public String asPrettyString() {
        final List<String> lines = prettyPrintAny(this.model, "");
        final StringBuilder builder = new StringBuilder();
        lines.forEach(line -> builder.append(line).append('\n'));
        return builder.toString();
    }

    /**
     * Pretty prints the given Ecore object at the provided intentation level.
     * @param object An Ecore object
     * @param indent The indentation level
     * @return A list of indented lines
     */
    private static List<String> prettyPrintAny(final Object object,
        final String indent) {
        final List<String> lines = new ArrayList<>();
        if (object instanceof EObject) {
            final EObject eobject = (EObject) object;
            final EClass eclass = eobject.eClass();
            lines.add(String.format("%s {", eclass.getName()));
            lines.addAll(
                EcorePrinter.prettyPrintRecursive(
                    eobject,
                    EcorePrinter.INDENT
                )
            );
            lines.add("}");
        } else if (object instanceof Iterable) {
            lines.add("[");
            for (final Object theobj : (Iterable<?>) object) {
                lines.addAll(
                    EcorePrinter.prettyPrintAny(
                        theobj,
                        EcorePrinter.INDENT
                    )
                );
            }
            lines.add("]");
        } else {
            lines.add(
                String.format(
                    "%s ",
                    String.valueOf(object)
                )
            );
        }
        return EcorePrinter.indentLines(lines, indent);
    }

    /**
     * Indents the given lines at the provided indentation level.
     * @param lines The lines to indent
     * @param indent The indentation level
     * @return A new list
     */
    private static List<String> indentLines(final List<String> lines,
        final String indent) {
        return lines.stream()
            .collect(Collectors.toList())
            .stream()
            .map(line -> indent.concat(line))
            .collect(Collectors.toList());
    }

    /**
     * Prints the eObject and its features recursively.
     * @param eobject The Ecore object
     * @param indent The indentiation level
     * @return A list of String lines
     */
    private static List<String> prettyPrintRecursive(final EObject eobject,
        final String indent) {
        final EClass eclass = eobject.eClass();
        final List<String> result = new ArrayList<>();
        eclass.getEAllStructuralFeatures().forEach(feature -> {
            final List<String> list = EcorePrinter.prettyPrintAny(
                eobject.eGet(feature),
                EcorePrinter.INDENT
            );
            list.set(
                0,
                list.get(0)
                    .trim()
            );
            result.add(
                String.format(
                    "%s = %s",
                    feature.getName(),
                    list.get(0)
                )
            );
            list.remove(0);
            result.addAll(list);
        });
        return EcorePrinter.indentLines(result, indent);
    }

}
