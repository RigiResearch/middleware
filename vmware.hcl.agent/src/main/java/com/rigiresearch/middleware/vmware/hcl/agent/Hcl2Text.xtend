package com.rigiresearch.middleware.vmware.hcl.agent

import com.rigiresearch.middleware.metamodels.hcl.Bool
import com.rigiresearch.middleware.metamodels.hcl.Dictionary
import com.rigiresearch.middleware.metamodels.hcl.FunctionCall
import com.rigiresearch.middleware.metamodels.hcl.HclPackage
import com.rigiresearch.middleware.metamodels.hcl.List
import com.rigiresearch.middleware.metamodels.hcl.Number
import com.rigiresearch.middleware.metamodels.hcl.Resource
import com.rigiresearch.middleware.metamodels.hcl.ResourceReference
import com.rigiresearch.middleware.metamodels.hcl.Specification
import com.rigiresearch.middleware.metamodels.hcl.Text
import com.rigiresearch.middleware.metamodels.hcl.TextExpression
import java.util.PriorityQueue
import java.util.Queue
import org.eclipse.emf.ecore.EObject

/**
 * Translates an HCL model instance (a {@link Specification}) to a textual
 * representation.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @date 2019-10-09
 * @version $Id$
 * @since 0.0.1
 */
class Hcl2Text {

    /**
     * Returns a text representation of the given model instance.
     */
    def source(Specification model) {
        return model.asText(new PriorityQueue)
    }

    /**
     * Translates an {@link EObject} from the HCL model to a {@link String}.
     * TODO sort/format the specification before printing it
     */
    def protected String asText(EObject object, Queue<String> context) {
        context.add(object.eClass.class.canonicalName)
        val text = switch (object) {
            Bool:
                object.asText(context)
            Dictionary:
                object.asText(context)
            FunctionCall:
                object.asText(context)
            List:
                object.asText(context)
            Number:
                object.asText(context)
            Resource: {
                val type = if (object.type !== null) '''"«object.type»" '''
                '''«object.specifier» «type»"«object.name»" «object.value.asText(context)»'''
            }
            ResourceReference:
                object.asText(context)
            Specification:
                object.asText(context)
            Text:
                object.asText(context)
            TextExpression:
                object.asText(context)
        }
        context.remove
        return text
    }

    /**
     * Translates a {@link Bool} from the HCL model to a {@link String}.
     */
    def protected String asText(Bool object, Queue<String> context) {
        object.value.toString
    }

    /**
     * Translates a {@link Dictionary} from the HCL model to a {@link String}.
     */
    def protected String asText(Dictionary object, Queue<String> context) {
        val className = HclPackage.eINSTANCE.dictionary.class.canonicalName
        val elements = object.elements
            .filter [ e |
                val text = e.value.asText(context)
                !text.empty && !text.equals('""') && !text.equals('[]')
            ]
            .map [ e |
                val eq = if(!(e.value instanceof Dictionary && context.peek.equals(className))) "= "
                val text = e.value.asText(context)
                '''«e.name» «eq»«text»'''
            ]
        '''
        «IF object.name !== null»
            "«object.name»" «ENDIF»{
            «FOR e : elements»
                «e»
            «ENDFOR»
        }'''
    }

    /**
     * Translates a {@link FunctionCall} from the HCL model to a {@link String}.
     */
    def protected String asText(FunctionCall object, Queue<String> context) {
        '''«object.function»(«FOR e : object.arguments SEPARATOR ', '»«e.asText(context)»«ENDFOR»)'''
    }

    /**
     * Translates a {@link List} from the HCL model to a {@link String}.
     */
    def protected String asText(List object, Queue<String> context) {
        '''[«FOR v : object.elements SEPARATOR ", "»«v.asText(context)»«ENDFOR»]'''
    }

    /**
     * Translates a {@link Number} from the HCL model to a {@link String}.
     */
    def protected String asText(Number object, Queue<String> context) {
        '''«object.value»'''
    }

    /**
     * Translates a {@link ResourceReference} from the HCL model to a {@link String}.
     */
    def protected String asText(ResourceReference object, Queue<String> context) {
        val className = HclPackage.eINSTANCE.functionCall.class.canonicalName
        val qm = if (context.peek.equals(className)) "" else '"'
        '''«qm»«FOR e : object.fullyQualifiedName SEPARATOR '.'»«e»«ENDFOR»«qm»'''
    }

    /**
     * Translates a {@link Specification} from the HCL model to a {@link String}.
     */
    def protected String asText(Specification object, Queue<String> context) {
        '''«FOR r : object.resources SEPARATOR "\n"»«r.asText(context)»«ENDFOR»'''
    }

    /**
     * Translates a {@link Text} from the HCL model to a {@link String}.
     */
    def protected String asText(Text object, Queue<String> context) {
        '''"«object.value?.toString»"'''
    }

    /**
     * Translates a {@link TextExpression} from the HCL model to a {@link String}.
     */
    def protected String asText(TextExpression object, Queue<String> context) {
        '''"${«object.reference.asText(context)»}"'''
    }

}
