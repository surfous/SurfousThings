/**
 *  RealLiving codes
 *
 *  Copyright 2014 Kevin Shuk
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *	  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
definition(
	name: "RealLiving codes",
	namespace: "surfous",
	author: "Kevin Shuk",
	description: "Manage user codes in a Yale RealLiving smart lock.",
	category: "Safety & Security",
	iconUrl: "https://s3.amazonaws.com/smartthings-device-icons/Home/home3-icn.png",
	iconX2Url: "https://s3.amazonaws.com/smartthings-device-icons/Home/home3-icn@2x.png"
)

preferences
{
	page(name:"mainPage", title:"User Code Setup", content:"mainPage", refreshTimeout:5)
	page(name:"editCode")
}

import static java.util.UUID.randomUUID

def installed()
{
	log.debug "Installed with settings: ${settings}"
	initialize()
}

def updated()
{
	log.debug "Updated with settings: ${settings}"
	if (settings.editSlot)
	{
		state.codeDb[settings.editSlot] = [name: settings.editName, code: settings.editCode]
		settings.remove('editSlot')
		settings.remove('editName')
		settings.remove('editCode')
	}
	unsubscribe()
	initialize()
}

def initialize()
{
	// TODO: subscribe to attributes, devices, locations, etc.
	if (!state.codeDb)
	{
		state.codeDb = []
		state.codeDb[1] = buildDbEntry[1, 0, "123456", "Franco"]
        state.codeDb[2] = buildDbEntry[2,0, "65535", "Kevin"]
        state.codeDb[3] = buildDbEntry[3,0, "09172005", "Tycho"]
        state.codeDb[4] = buildDbEntry[2,0, "4444", "Boing"]
        state.codeDb[10]= buildDbEntry[10,0, "75757", "Mollo"]
        state.codeDb[22] = buildDbEntry[22,0, "97315", "Otto"]
        state.codeDb[23] = buildDbEntry[23,0, "11512", "Zoie"]

	   	settings.keySet.each
		{
			settings.remove(it)
		}
	}
}

def generateUuid()
{
	var uuid = randomUUID() as String​
	return uuid
}

def buildEmptyDbEntry()
{
    return [serial: 0, code: null, name: null, uuid: generateUuid()]
}

def buildDbEntry(int slotNumber, int slotSerial=0, String codeStr, String slotName, String uuid = null)
{
	if (slotName == null) slotName = slotNumber.toString()
	if (uuid == null) uuid = generateUuid()
	return [serial: slotSerial, code: codeStr, name: slotName, uuid: uuid]
}

def mainPage()
{
	if (!state.codeDb) initialize()
	dynamicPage(name:"mainPage", title:"User Codes", nextPage:"", uninstall: true)
	{
		state.codeDb.keySet().sort {a, b -> a.toInteger() <=> b.toInteger()}.each
		{
			String slotNum = it.toString()
			def secTuple = state.codeDb[slotNum]
			log.debug "displaying slot $slotNum: $secTuple"
			String linkName = String.format("%-30s %8s", secTuple.name, secTuple.code)
			section()
			{
				href "editCode", title: linkName, description: "edit this code", params: [slotNum: slotNum]
			}
		}
	}
}

def nameFieldInputName(int slotNum, int serial)
{
    return "editName-$slotNum-$serial"
}

def nameFieldInputCode(int slotNum, int serial)
{
    return "editCode-$slotNum-$serial"
}

def editCode(params=[:])
{
	dynamicPage(name: "editCode", title: "edit code", nextPage: "mainPage", uninstall: false, install: true)
	{
		// set up the state.editcode map

		// if editing a slot, see if the setting value differs from the db


		if (params.slotNum)
		{
			// we are editing an existing slot
			log.debug params
			log.debug params.sval.name
			log.debug "${settings}"
			def slotTuple = state.codeDb[params.slotNum]
            def editNameInputName = "editName-$slotNum-$slotTuple.serial"
            def editCodeInputName = "editCode-$slotNum-$slotTuple.serial"
            if (settings[editNameInputName] != slotTuple.name || settings[editCodeInputName] != slotTuple.code)
            {
                // increment the slot serial so we get blank inputs
                slotTuple.serial++
                editNameInputName = "editName-$slotNum-$slotTuple.serial"
                editCodeInputName = "editCode-$slotNum-$slotTuple.serial"
            }
			section()
			{
				state.actionEntry = slotTuple
				input name: editNameInputName, title: "name", type: "String", description: "who is this code for?", required: true, multiple: false
				input name: editCodeInputName, title: "code", type: "Number", description: "4 to 8 digits", required: true, multiple: false
			}
		}
		else
		{
			// we are creating a new code
		}
	}
}
