/**
 * Contains customizations to the input/output graph metamodel.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
@XmlSchema(
    namespace = Graph.NAMESPACE,
    elementFormDefault = XmlNsForm.QUALIFIED
)
package com.rigiresearch.middleware.historian.monitoring.graph;

import com.rigiresearch.middleware.graph.Graph;
import javax.xml.bind.annotation.XmlNsForm;
import javax.xml.bind.annotation.XmlSchema;
