package com.rigiresearch.middleware.historian.templates

import com.rigiresearch.middleware.metamodels.monitoring.Array
import com.rigiresearch.middleware.metamodels.monitoring.DataType
import com.rigiresearch.middleware.metamodels.monitoring.Monitor
import com.rigiresearch.middleware.metamodels.monitoring.Property
import com.rigiresearch.middleware.metamodels.monitoring.Root
import com.rigiresearch.middleware.metamodels.monitoring.Schema
import com.rigiresearch.middleware.metamodels.monitoring.Type
import java.util.Date
import java.util.List
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.nio.file.StandardCopyOption

/**
 * Generate classes from the monitoring model. More specifically, from the
 * response schemas.
 *
 * TODO This class could be replaced by an ATL transformation from monitoring
 * to class diagram. Then, the class diagram would be used to generate the
 * classes.
 *
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @date 2019-06-25
 * @version $Id$
 * @since 0.1.0
 */
final class MonitoringTemplate {

    /**
     * The package for the generated sources.
     */
	static final String PACKAGE = "com.rigiresearch.middleware.historian.api"

    /**
     * Generates the Java project for monitoring a specific cloud provider.
     * The project contains Java classes, and a properties and gradle file.
     * The target directory (tree) will be created if necessary.
     * @param root The root model element
     * @param target The target directory where the files are created
     */
    def void generateFiles(Root root, File target) throws IOException {
        // Create the directories if necessary
        val ^package = MonitoringTemplate.PACKAGE.replaceAll("\\.", "/")
        target.mkdirs
        new File(target, '''src/main/java/«^package»''').mkdirs
        new File(target, "src/main/resources").mkdirs

        // Copy template files
        Files.walkFileTree(
            "src/main/resources/templates/gradle".toPath,
            new CopyFileVisitor(
                target.toPath,
                StandardCopyOption.REPLACE_EXISTING
            )
        )
        Files.copy(
            "src/main/resources/templates/log4j2.xml".toPath,
            new File(target, "src/main/resources/log4j2.xml").toPath,
            StandardCopyOption.REPLACE_EXISTING
        )
        Files.copy(
            "src/main/resources/templates/log4j.xml".toPath,
            new File(target, "src/main/resources/log4j.xml").toPath,
            StandardCopyOption.REPLACE_EXISTING
        )
        Files.walkFileTree(
            "src/main/resources/templates/source".toPath,
            new CopyFileVisitor(
                new File(target, "src/main/java").toPath,
                StandardCopyOption.REPLACE_EXISTING
            )
        );

        // Generate new files
        root.monitors.forEach[m|m.asJavafile(target).write(m.asJavaClass)]
        new File(target, "src/main/resources/monitoring.properties")
            .write(root.asProperties)
    }

    /**
     * Creates a {@link File} based on the monitor's path id.
     * @param monitor The monitor instance
     * @param target The target directory
     * @return A {@link File}
     */
    def private asJavafile(Monitor monitor, File target) {
        val ^package = MonitoringTemplate.PACKAGE.replaceAll("\\.", "/")
        new File(target, '''src/main/java/«^package»/«monitor.path.id.toFirstUpper».java''')
    }

    /**
     * Converts a {@link String} to a {@link Path}.
     * @param path The path name
     * @return A {@link Path}
     */
    def private toPath(String path) {
        new File(path).toPath
    }

    /**
     * Writes a file with the given content.
     * @param file The file to write
     * @param content The file content
     * @throws IOException If there is a problem writing the file
     */
    def private write(File file, CharSequence content) throws IOException {
        Files.write(
            Paths.get(file.toURI),
            content.toString.bytes,
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING
        )
    }

    /**
     * Creates a properties file based on the paths and their parameters.
     * @param root The root model element
     * @return The contents of a properties file
     */
    def asProperties(Root root) '''
        # File generated by Historian («new Date()»)

        # The thread pool size
        thread.pool.size=30

        # The base URL
        base=«root.baseUrl»

        # The following are paths from the API specification
        monitors=«root.monitors.map[m|m.path.id].join(", ")»

        # AUTHENTICATION
        # ==============
        #
        # If the API calls require authentication, specify a monitor id from the
        # previous list to request an authentication token:
        #
        # auth=<monitor-id>
        #
        # Now, setup basic HTTP authentication for the auth monitor. In this case,
        # the values are taken from the environment variables API_AUTH_USERNAME and
        # API_AUTH_PASSWORD.
        #
        # <monitor-id>.username=${env:API_AUTH_USERNAME}
        # <monitor-id>.password=${env:API_AUTH_PASSWORD}
        #
        # INPUTS AND OUTPUTS
        # ==================
        #
        # Specify output parameters using an Xpath selector. For example, for the
        # following JSON response (from an authentication request):
        #
        # {
        #   "auth_token" = "..."
        # }
        #
        # The following is a valid configuration:
        #
        # <monitor-id>.outputs=token
        # <monitor-id>.outputs.token.selector=auth_token
        #
        # You may use the value "${<monitor-id>.outputs.token.value}" as an input
        # value for other monitors. For example:
        #
        # myMonitor.inputs.param1.value=${myAuthMonitor.outputs.token.value}
        #
        # You may use multiline strings by using the backslash character at the end of
        # each line. Notice that you may need to escape comma characters. For example:
        #
        # myMonitor.inputs.param1.value=\
        # {\
        #   "attr": {\
        #     "prop1": ["value"]\,\
        #     "prop2": {\
        #       ...
        #     }\
        #   }\
        # }
        #
        # CRON EXPRESSIONS
        # ================
        #
        # Update the cron expressions to reflect your needs. Here is the documentation
        # for the scheduling patterns:
        #
        # http://www.sauronsoftware.it/projects/cron4j/manual.php#p02
        #
        # In the case of the auth monitor, choose an appropriate expression to request
        # an authentication token before the current one expires.
        #
        # DEPENDENT MONITORS
        # ==================
        #
        # Dependent monitors are useful when one monitor requires input data provided
        # by another one to collect further details. For example, let's assume monitor
        # "VMs" collects the following data:
        #
        # {
        #   "value": [
        #     {
        #       "vm": "vm-123",
        #       "name": "my-awesome-vm",
        #       ...
        #     },
        #     ...
        #   ]
        # }
        #
        # To collect further details about my-awesome-vm (and the rest of VMs), another
        # monitor would need the to know its id (i.e., "vm-123"). An output parameter
        # is not enough in this case because there can be many values. This can be
        # configured using the property "children", as follows:
        #
        # VMs.children=VM
        # VMs.children.VM.input=id
        #
        # That configuration assumes that monitor "VM" has an input parameter "id".
        # Such a parameter will be set at run-time automatically. Any value setup in
        # this file will be ignored, as well as any expression setup for monitor "VM".
        # An extra property "selector" must be configured for input "id", which is an
        # Xpath selector. This is to extract the values from the data collected by the
        # parent monitor. Any child monitor must not be listed in the variable "monitors".
        #
        «FOR m : root.monitors SEPARATOR '\n'»
            «m.path.id».url=${base}«IF !m.path.url.startsWith("/")»/«ENDIF»«m.path.url»
            «m.path.id».expression=«m.rate.value»
            «m.path.id».response.class=«MonitoringTemplate.PACKAGE».«m.path.id.asClassName»
            # «m.path.id».children=
            «IF !m.path.parameters.empty»
                «m.path.id».inputs=«m.path.parameters.map[p|p.name].join(", ")»
                «FOR p : m.path.parameters»
                    «m.path.id».inputs.«p.name».location=«p.location.toString.toUpperCase»
                    # «m.path.id».inputs.«p.name».value=
                    # «m.path.id».inputs.«p.name».selector=
                «ENDFOR»
            «ENDIF»
        «ENDFOR»
    '''

    /**
     * Puts together the base URL.
     * @param root The root model element
     * @return The base URL
     */
    def private baseUrl(Root root) {
        var base = '''http«IF root.https»s«ENDIF»://«root.host»«root.basePath»'''
        if (base.endsWith("/")) {
            base = base.substring(0, base.length)
        }
        return base
    }

    /**
     * Generates a Java (value) class for a particular schema object.
     * This template assumes that a path always returns an object, therefore
     * its properties are rendered as class attributes. TODO In case a primitive
     * value is returned instead, that primitive value would need to be rendered
     * as an attribute itself.
     *
     * <b>Not supported types</b>: FILE, NULL, UNSPECIFIED (See enum Type in the
     * monitoring model)
     * @param monitor The associated monitor
     * @return The contents of a Java class
     */
    def asJavaClass(Monitor monitor) '''
        package «MonitoringTemplate.PACKAGE»;

        import javax.annotation.Generated;

        import lombok.Value;
        import lombok.experimental.Accessors;

        /**
         * Response object for path '<em><b>«monitor.path.id»</b></em>'.
         */
        @Accessors(fluent = true)
        @Value
        @Generated(value = "Historian", date = "«new Date()»")
        public final class «monitor.path.id.asClassName» {

            «FOR property : monitor.schema.properties SEPARATOR '\n'»
                «property.asJavaField»
            «ENDFOR»

            «FOR property : monitor.schema.properties.filter[p|p.type.hasInnerClass]»
                «property.asInnerClasses»
            «ENDFOR»

        }
    '''

    /**
     * Maps the given property to its Java type version.
     * @param property The schema property
     * @return The name of the corresponding Java type
     */
    def private asJavaType(Property property) {
        property.type.asJavaType(property.name, 0)
    }

    /**
     * Maps the given data type to its Java version.
     * @param type The data type
     * @param name The name of the associated property
     * @param calls The number of inner calls
     * @return The name of the corresponding Java type
     */
    def private String asJavaType(DataType type, String name, int calls) {
        switch type {
            Schema: '''«name.asClassName»«calls»'''
            Array: '''«type.subtype.asJavaType(name + type.hashCode, calls + 1)»[]'''
            default: type.type.asJavaType
        }
    }

    /**
     * Maps the given primitive type to its Java version.
     * @param type The primitive type
     * @return The name of the corresponding Java primitive type
     */
    def private asJavaType(Type type) {
        switch type {
            case BOOLEAN: "boolean"
            case INTEGER: "int"
            case NUMBER: "double"
            case STRING: "String"
            default: this.fail(type)
        }
    }

    /**
     * Throws {@link IllegalArgumentException} to notify about an unsupported
     * type.
     * @param type The unsupported type.
     */
    def private fail(Type type) {
        throw new IllegalArgumentException(
            '''Unsupported property value «type.toString»'''
        );
    }

    /**
     * Converts from snake case to camel case.
     * @param name The original name
     * @return A valid camel-case class name
     */
    def private asClassName(String name) {
        name.split("_").map[t|t.substring(0, 1).toUpperCase + t.substring(1)].join
    }

    /**
     * Creates inner classes (as Strings) based on a given property.
     * @param property The schema property
     * @return Java code containing the inner classes and nothing else
     */
    def private asInnerClasses(Property property) {
        this.asInnerClasses(property.name, property.type, 0).join("\n")
    }

    /**
     * Creates inner classes (as Strings) based on a given data type. This
     * method is recursive.
     * @param name The associated property's name
     * @param type The current data type being consider for class creation
     * @param calls The number of inner calls
     * @return A list of inner classes
     */
    def private List<String> asInnerClasses(String name, DataType type, int calls) {
        val classes = newArrayList
        switch type {
            Schema: {
                val clazz = '''
                /**
                 * Java class for schema object from property '<em><b>«name»</b></em>'.
                 */
                @Accessors(fluent = true)
                @Value
                public final class «name.asClassName»«calls» {

                    «FOR property : type.properties SEPARATOR '\n'»
                        «property.asJavaField»
                        ««« Recursive call: is this attribute an Object or Array?
                        «val dummy = classes.addAll(this.asInnerClasses(property.name, property.type, 0))»
                    «ENDFOR»

                }
                '''
                classes.add(clazz)
            }
            Array: {
                classes.addAll(
                    // Recursive call: is the sub-type an Object or Array?
                    this.asInnerClasses(
                        name + type.hashCode,
                        type.subtype,
                        calls + 1
                    )
                )
            }
        }
        return classes
    }

    /**
     * Determines whether an inner class is necessary. That is, the property
     * type references an object or is an object itself.
     * @param type The data type
     * @return Whether an inner class is necessary
     */
    def private boolean hasInnerClass(DataType type) {
        switch type {
            Schema: true
            Array: type.subtype.hasInnerClass
            default: false
        }
    }

    /**
     * Creates a valid Java field declaration without initialization.
     * @param property The associated property
     * @return A field declaration
     */
    def private asJavaField(Property property) '''
        /**
         * The value of the '<em><b>«property.name»</b></em>' property.
         */
        private final «property.asJavaType» «property.name»;
    '''

}
