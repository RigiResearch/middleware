package com.rigiresearch.middleware.coordinator.templates

import com.rigiresearch.middleware.metamodels.hcl.Dictionary
import com.rigiresearch.middleware.metamodels.hcl.Resource
import com.rigiresearch.middleware.metamodels.hcl.Specification
import com.rigiresearch.middleware.metamodels.hcl.Text
import java.util.Collections

/**
 * JSON templates for generating CAM's camtemplate.json and camvariables.json.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
class CamTemplates {

    /**
     * Generates the content for camtemplate.json.
     */
    def String template() '''
        {
          "name": "ImportedVmwareResources",
          "description": "Imported resources from VMware vSphere",
          "type": "userCreated",
          "version": "1.0",
          "manifest": {
            "template_type": "Terraform",
            "template_format": "HCL",
            "template_provider": "VMware vSphere",
            "template": {
              "templateOutput": "",
              "templateVariables": "",
              "templateData": ""
            },
            "template_source": {
              "githubRepoUrl": "",
              "githubAccessToken": "",
              "relativePathToTemplateFolder": "",
              "templateFileName": "main.tf"
            }
          },
          "metadata": {
            "displayName": "ImportedVmwareResources",
            "longDescription": "Imported resources from VMware vSphere",
            "bullets": [
              {
                "title": "Clouds",
                "description": "VMware"
              }
            ]
          }
        }
    '''

    /**
     * Generates the content for camvariables.json.
     */
    def String variables(Specification specification) '''
        {
          "input_datatypes": [
          ],
          "output_datatype": "content_template_output",
          "input_groups": [
            {
              "name": "all",
              "label": "All variables"
            }
          ],
          "output_groups": [
            {
              "name": "content_template_output",
              "label": "Outputs"
            }
          ],
          "template_input_params": [
            «val variables = specification.resources.filter[it.specifier.equals("variable")].toList»
            «Collections.sort(variables, new ResourceComparator())»
            «FOR input : variables SEPARATOR ",\n"»
            {
              "name": "«input.name»",
              "type": "«input.attr("type")»",
              "description": "«input.attr("description")»",
              "group_name": "all"
            }
            «ENDFOR»
          ]
        }
    '''

    def private String attr(Resource resource, String name) {
        ((resource.value as Dictionary).elements
            .findFirst[it.name.equals(name)]
            ?.value as Text)?.value
    }

}
