package com.rigiresearch.middleware.historian.templates

import com.rigiresearch.middleware.metamodels.monitoring.Array
import com.rigiresearch.middleware.metamodels.monitoring.DataType
import com.rigiresearch.middleware.metamodels.monitoring.Monitor
import com.rigiresearch.middleware.metamodels.monitoring.Path
import com.rigiresearch.middleware.metamodels.monitoring.Property
import com.rigiresearch.middleware.metamodels.monitoring.Root
import com.rigiresearch.middleware.metamodels.monitoring.Schema
import com.rigiresearch.middleware.metamodels.monitoring.Type
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.util.Date
import java.util.List
import java.util.Map
import org.eclipse.xtend.lib.annotations.Data

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
     * A list of used class names to prevent duplicates.
     */
    final Map<String, List<String>> names

    /**
     * Default constructor.
     */
    new() {
        this.names = newHashMap
    }

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
            "src/main/resources/templates/logback.xml".toPath,
            new File(target, "src/main/resources/logback.xml").toPath,
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
        root.monitors.forEach[m|m.asJavaFile(target).write(m.asJavaClass)]
        new File(target, "src/main/resources/default.properties")
            .write(root.asDefaultProperties)
        new File(target, "src/main/resources/custom.properties")
            .write(root.asCustomProperties)
    }

    /**
     * Creates a {@link File} based on the monitor's path id.
     * @param monitor The monitor instance
     * @param target The target directory
     * @return A {@link File}
     */
    def private asJavaFile(Monitor monitor, File target) {
        val ^package = MonitoringTemplate.PACKAGE.replaceAll("\\.", "/")
        new File(target, '''src/main/java/«^package»/«monitor.path.id.asClassName».java''')
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
    def asDefaultProperties(Root root) '''
        # File generated by Historian («new Date()»)

        ###############################################################################
        # DO NOT MODIFY THIS FILE
        ###############################################################################

        # The following are paths from the API specification
        monitors=«root.monitors.map[m|m.path.id].join(", ")»

        # The pool size for concurrent requests
        thread.pool.size=30

        # Cron expression requesting data every minute
        DEFAULT_DELAY_EXPR=* * * * *

        # The base URL
        base=«root.baseUrl»

        # Configuration
        «FOR m : root.monitors SEPARATOR '\n'»
            «IF !m.path.parameters.empty»
                «m.path.id».inputs=«m.path.parameters.map[p|p.name].join(", ")»
                «FOR p : m.path.parameters»
                    «IF p.required»«m.path.id».inputs.«p.name».required=true«ENDIF»
                    «m.path.id».inputs.«p.name».location=«p.location.toString.toUpperCase»
                «ENDFOR»
            «ENDIF»
            «m.path.id».url=${base}«IF !m.path.url.startsWith("/")»/«ENDIF»«m.path.url»
            «m.path.id».response.class=«MonitoringTemplate.PACKAGE».«m.path.id.asClassName»
            «m.path.id».expression=«IF m.rate.value === null»${DEFAULT_DELAY_EXPR}«ELSE»«m.rate.value»«ENDIF»
        «ENDFOR»
    '''

    /**
     * Creates a properties file for parameter values and configuration.
     * @param root The root model element
     * @return The contents of a properties file
     */
    def asCustomProperties(Root root) '''
        # File generated by Historian («new Date()»)
        #
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
        # MULTILINE VALUES
        # ================
        #
        # You may use multiline values by using the backslash character at the end of
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
        # To collect further details about my-awesome-vm (and the rest of the VMs),
        # another monitor would need the to know its id (i.e., "vm-123"). An output
        # parameter is not enough in this case because there can be many values.
        # This can be configured using the property "children", as follows:
        #
        # VMs.children=VM
        # VMs.children.VM.input=id
        #
        # That configuration assumes that monitor "VM" has an input parameter "id".
        # Such a parameter will be set at run-time automatically. Any value setup in
        # this file will be ignored, as well as any expression setup for monitor "VM".
        # An extra property "selector" must be configured for input "id", which is an
        # Xpath selector. This is to extract the values from the data collected by the
        # parent monitor. Dependent monitors must not be listed in the variable
        # "monitors".

        «FOR m : root.monitors.filter[m1|!m1.path.parameters.empty] SEPARATOR '\n'»
            # Monitor "«m.path.id»"
            «FOR p : m.path.parameters»
                «IF !p.required»# «ENDIF»«m.path.id».inputs.«p.name».value=
            «ENDFOR»
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
        «val className = monitor.path.id.asClassName»
        package «MonitoringTemplate.PACKAGE»;

        import javax.annotation.Generated;

        import lombok.Data;
        import lombok.NoArgsConstructor;
        import lombok.AllArgsConstructor;

        /**
         * Response object for path '<em><b>«monitor.path.id»</b></em>'.
         */
        @Data
        @AllArgsConstructor
        @NoArgsConstructor
        @Generated(value = "Historian", date = "«new Date()»")
        public final class «className» {

            «monitor.asJavaMembers(className)»

        }
    '''

    /**
     * Generate class attributes and inner classes for a given monitor.
     * @param monitor The monitor for which the members are generated
     * @param className The name of the enclosing class
     */
    def asJavaMembers(Monitor monitor, String className) {
        val fields = newArrayList
        val classes = newArrayList
        monitor.schema.properties.forEach[ property |
            if (property.type.hasInnerClass) {
                val result = property.asInnerClasses(className)
                val typeName = '''«result.typeName»«IF result.fromArray»[]«ENDIF»'''
                fields.add(property.asJavaField(typeName))
                classes.addAll(result.classes)
            } else {
                // This branch would only be visited if the request returns a
                // primitive value instead of a JSON/XML document.
                val primitive = property.type.type.asJavaType
                fields.add(property.asJavaField(primitive))
            }
        ]
        '''
            «FOR field : fields.reverseView SEPARATOR '\n'»
                «field»
            «ENDFOR»

            «FOR clazz : classes.reverseView SEPARATOR '\n'»
                «clazz»
            «ENDFOR»
        '''
    }

    /**
     * Maps the given primitive type to its Java version.
     * @param type The primitive type
     * @return The name of the corresponding Java primitive type
     */
    def private asJavaType(Type type) {
        switch type {
            case BOOLEAN: "boolean"
            // TODO Add support for int64 format
            case INTEGER: "long"
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
            '''Unsupported property value "«type.toString»"'''
        );
    }

    /**
     * Converts from snake case to camel case.
     * @param name The original name
     * @return A valid camel-case class name
     */
    def private asClassName(String name) {
        name.split("_")
            .map[t|t.substring(0, 1).toUpperCase + t.substring(1)]
            .join
            .split("-")
            .map[t|t.substring(0, 1).toUpperCase + t.substring(1)]
            .join
            .replaceAll("[^a-zA-Z0-9]", "")
    }

    /**
     * Creates inner classes (as Strings) based on a given property.
     * @param property The schema property
     * @param className The name of the enclosing class
     * @return A pair containing the type name of the source property and the
     *  inner classes generated
     */
    def private asInnerClasses(Property property, String className) {
        this.asInnerClasses(
            className,
            property.name,
            property.type,
            property.type instanceof Array
        )
    }

    /**
     * Creates inner classes (as Strings) based on a given data type. This
     * method is recursive.
     * @param className The name of the enclosing class
     * @param name The name of the associated property
     * @param type The current data type being considered for class creation
     * @return A pair containing the type name of the source property and the
     *  inner classes generated
     */
    def private Result asInnerClasses(String className,
        String name, DataType type, boolean fromArray) {
        var _fromArray = false
        var typeName = ""
        val classes = newArrayList
        switch type {
            Schema: {
                typeName = this.nextName(className, name)
                val clazz = '''
                    /**
                     * Java class for schema object from property '<em><b>«name»</b></em>'.
                     */
                    @Data
                    @AllArgsConstructor
                    @NoArgsConstructor
                    public static final class «typeName» {

                        «FOR property : type.properties SEPARATOR '\n'»
                            ««« Recursive call: is this attribute an Object or Array?
                            «val innerResult = this.asInnerClasses(className, property.name, property.type, false)»
                            «val dummy = classes.addAll(innerResult.classes)»
                            «property.asJavaField('''«innerResult.typeName»«IF innerResult.fromArray»[]«ENDIF»''')»
                        «ENDFOR»

                    }
                '''
                classes.add(clazz)
            }
            Array: {
                // Recursive call: is the sub-type an Object or Array?
                val innerResult = this.asInnerClasses(className, name, type.subtype, true)
                classes.addAll(innerResult.classes)
                _fromArray = true
                typeName = innerResult.typeName
            }
            default: {
                typeName = asJavaType(type.type)
            }
        }
        return new Result(_fromArray, typeName, classes)
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
    def private asJavaField(Property property, String typeName) '''
        /**
         * The value of the '<em><b>«property.name»</b></em>' property.
         */
        private «typeName» «property.name»;
    '''

    /**
     * Generates a new name if the given one already exists.
     * @param className The name of the enclosing class
     * @param innerClassName The name under test
     * @return A name which hasn't been used before in the context of the enclosing class
     */
    def private String nextName(String className, String innerClassName) {
        var List<String> current = null
        var name = innerClassName.asClassName
        var tmp = name
        var number = 1
        if (!this.names.containsKey(className)) {
            current = newArrayList
            this.names.put(className, current)
        } else {
            current = this.names.get(className)
        }
        while (current.contains(tmp)) {
            tmp = name + (number++)
        }
        current.add(tmp)
        return tmp
    }

    /**
     * A partial result from the Java code generation.
     */
    @Data
    static final class Result {

        /**
         * Whether the source property is an array.
         */
        boolean fromArray

        /**
         * The name of the generated type for the source property.
         */
        String typeName

        /**
         * A list of inner classes (their Java code).
         */
        List<String> classes
    }

}
