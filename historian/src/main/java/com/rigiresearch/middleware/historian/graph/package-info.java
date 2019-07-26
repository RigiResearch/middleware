/**
 * Contains JAXB-annotated classes to create monitoring graphs.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
@XmlSchema(
    namespace = Graph.NAMESPACE,
    elementFormDefault = XmlNsForm.QUALIFIED
)
package com.rigiresearch.middleware.historian.graph;

import javax.xml.bind.annotation.XmlNsForm;
import javax.xml.bind.annotation.XmlSchema;
