/**
 *  baldeagle demo
 *
 *  Copyright 2014 BaldEagle72
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
definition(
    name: "baldeagle demo",
    namespace: "surfous",
    author: "BaldEagle72",
    description: "preferences demo",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	page name:"setupInit"
    page name:"setupFirstTime"
    page name:"setupInstalled"
}

def setupInit() {
	log.debug "setupInit"
	log.debug "state installed: $state.installed"
    log.debug "state someVariable: $state.someVariable"
    log.debug settings.fields
	if (state.installed) {
    	return setupInstalled()
    } else {
    	return setupFirstTime()
    }
}

def setupFirstTime() {
	log.debug "setupFirstTime"
    state.installed = true
    def inputSomeVariable = [
    	name:			"someVariable",
        type:			"string",
        title:			"Type the variable",
        defaultValue:	"default value",
        required:		true
    ]

    def pageProperties = [
    	name:		"setupFirstTime",
        title:		"Preferences",
        install:	true
    ]

    return dynamicPage(pageProperties) {
    	section {
        	input inputSomeVariable
        }
    }

}

def setupInstalled() {
	log.debug "setupInstalled"
    settings.remove('someVariable')// = "lets try this again"
	def inputSomeVariable = [
    	name:			"someVariable",
        type:			"string",
        title:			"Type the variable",
        defaultValue:	"something else",
        required:		true
    ]

    def pageProperties = [
    	name:		"setupInstalled",
        title:		"Preferences",
        install:	true,
        uninstall: true
    ]

    return dynamicPage(pageProperties) {
    	section {
        	input inputSomeVariable
        }
    }
}

def installed()
{
	log.debug "running installed"
}

def uninstalled()
{
	log.debug "running uninstalled"
}

def updated() {
	log.debug "updated"
    if (state.someVariable != settings.someVariable)
    {
    	log.debug "someVariable updated in state from $state.someVariable to $settings.someVariable"
        state.someVariable = settings.someVariable
	}


}
