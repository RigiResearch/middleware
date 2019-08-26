package com.rigiresearch.middleware.historian.templates

import com.rigiresearch.middleware.metamodels.monitoring.ApiKeyAuth
import com.rigiresearch.middleware.metamodels.monitoring.BasicAuth
import com.rigiresearch.middleware.metamodels.monitoring.Monitor
import com.rigiresearch.middleware.metamodels.monitoring.MonitoringFactory
import com.rigiresearch.middleware.metamodels.monitoring.MonitoringPackage
import com.rigiresearch.middleware.metamodels.monitoring.Oauth2Auth
import com.rigiresearch.middleware.metamodels.monitoring.Path
import com.rigiresearch.middleware.metamodels.monitoring.PropertyLocation
import com.rigiresearch.middleware.metamodels.monitoring.Root
import com.rigiresearch.middleware.metamodels.monitoring.Type
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.Date
import org.apache.commons.configuration2.PropertiesConfiguration
import org.apache.commons.configuration2.io.FileHandler
import org.eclipse.emf.ecore.EcorePackage

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
     * Default constructor.
     */
    new() {
    	// Nothing to do
    }

    /**
     * Generates the Java project for monitoring a specific cloud provider.
     * The project contains a properties file and the necessary Gradle files.
     * The target directory (tree) will be created if necessary.
     * @param root The root model element
     * @param target The target directory where the files are created
     */
    def void generateFiles(Root root, File target) throws IOException {
        // Create the directories if necessary
        target.mkdirs
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

        // Generate new files
        new FileHandler(new PropertiesConfiguration().populateDefaultProperties(root))
            .save(new File(target, "src/main/resources/default.properties"))
        new FileHandler(new PropertiesConfiguration().populateCustomProperties(root))
            .save(new File(target, "src/main/resources/custom.properties"))
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
     * Creates a properties file based on the paths and their parameters.
     * @param config A configuration object
     * @param root The root model element
     * @return The corresponding configuration object
     */
    def populateDefaultProperties(PropertiesConfiguration config, Root root) {
        config.layout.globalSeparator = "="
        config.layout.headerComment = '''
        # File generated by Historian («new Date()»)

        ###############################################################################
        # DO NOT MODIFY THIS FILE
        ###############################################################################'''

        config.setProperty("periodicity", "* * * * *")
        config.layout.setBlancLinesBefore("periodicity", 1)
        config.layout.setComment("periodicity", "Cron expression requesting data every minute")

        config.setProperty("base", root.baseUrl)
        config.layout.setBlancLinesBefore("base", 1)
        config.layout.setComment("base", "The base URL")

        for (m : root.monitors) {
            val parameters = m.authParameters
            if (!m.path.parameters.empty) {
                parameters += m.path.parameters
                config.setProperty('''«m.path.id».inputs''', parameters.map[p|p.name].join(", ").toString)
                config.layout.setBlancLinesBefore('''«m.path.id».inputs''', 1)
                for (p : parameters) {
                    if (p.required) {
                        config.setProperty('''«m.path.id».inputs.«p.name».required''', true)
                    }
                    config.setProperty('''«m.path.id».inputs.«p.name».location''', p.location.toString.toUpperCase)
                }
            } else {
                config.layout.setBlancLinesBefore('''«m.path.id».url''', 1)
            }
            config.setProperty(
                '''«m.path.id».url''',
                '''${base}«IF !m.path.url.startsWith("/")»/«ENDIF»«m.path.url»'''.toString
            )
        }
        return config
    }

    /**
     * Creates the necessary parameters based on authentication requirements
     * for the given monitor.
     * @param monitor The monitor
     * @return A list of parameters
     */
    def authParameters(Monitor monitor) {
        val parameters = newArrayList
        val root = monitor.eContainer as Root
        val requirements = if(!monitor.path.authRequirements.empty)
                monitor.path.authRequirements
            else
                root.authRequirements
        for (requirement : requirements) {
            val method = requirement.method
            switch (method) {
                ApiKeyAuth: {
                    parameters += method.property
                }
                BasicAuth: {
                    EcorePackage.eINSTANCE.eClass
                    MonitoringPackage.eINSTANCE.eClass
                    val property = MonitoringFactory.eINSTANCE.
                        createLocatedProperty
                    val type = MonitoringFactory.eINSTANCE.createDataType
                    type.type = Type.STRING
                    property.name = "Authorization"
                    property.location = PropertyLocation.HEADER
                    property.required = true
                    property.type = type
                    parameters += property
                }
                Oauth2Auth: {
                    // TODO Add support for oauth2 authentication
                    throw new UnsupportedOperationException("Not implemented yet")
                }
            }
        }
        return parameters
    }

    /**
     * Creates a properties file for parameter values and configuration.
     * @param config A configuration object
     * @param root The root model element
     * @return The corresponding configuration object
     */
    def populateCustomProperties(PropertiesConfiguration config, Root root) {
        config.layout.headerComment = '''# File generated by Historian («new Date()»)'''
        return config
    }

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

}
