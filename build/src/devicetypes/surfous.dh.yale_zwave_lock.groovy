/**
*  Yale RealLiving Z-wave Lock
*
*  Copyright 2015 Kevin Shuk
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
* xxxxXxxxXxxx_cc:	implementation of a command class command - returns a single command
* xxxxXxxxXxxx_cm:	macro method which wraps one or more command class commands and/or other logic
*					into a macro and returns a CommandQueue ArrayList
* xxxXxxxXxxx_utl:  local method utility method
* zwaveEvent:		overloaded handler for events sent from our device
* other methods:	are implementations of capability methods, declared commands, or DeviceType
*					methods, and are named as declared.
*/
metadata
{
	// Automatically generated. Make future change here.
	definition (name: "Yale RealLiving Locks with Z-wave", namespace: "surfous", author: "Kevin Shuk")
	{
		capability "Lock"
		capability "Lock Codes"
		capability "Configuration"
		capability "Refresh"
		capability "Polling"
		capability "Battery"

		command "unlockWithTimeout"
		command "checkState"
		command "runAdHocTest"

		attribute "blank", "string" //just as the name says...
		attribute "usersNumber", "number"

		// config attributes
		command "cycleOperatingMode"
		attribute "cfgOpMode", "string"

		command "cycleAudioMode"
		attribute "cfgAudio", "string"

		command "cycleAutoRelock"
		command "increaseRelockDelay"
		command "decreaseRelockDelay"
		attribute "cfgAutoRelock", "string"
		attribute "cfgAutoRelockDelay", "number"
		attribute "tileAutoRelock", "string" // string of delay seconds, 'OFF' or 'changing'

		command "cycleLanguage"
		attribute "cfgLanguage", "string"
	}

	fingerprint deviceId: '0x4001', inClusters: '0x72,0x86,0x98'
	fingerprint deviceId: '0x4002', inClusters: '0x72,0x86,0x98'
	fingerprint deviceId: '0x4003', inClusters: '0x72,0x86,0x98'
	fingerprint deviceId: '0x4004', inClusters: '0x72,0x86,0x98'

	simulator
	{
		// TODO: define status and reply messages here
		status "unit locked": zwave.doorLockV1.doorLockOperationReport(doorLockMode: 255).incomingMessage()
		status "unit unlocked": zwave.doorLockV1.doorLockOperationReport(doorLockMode: 0).incomingMessage()
		status "unit unlocked with timeout": zwave.doorLockV1.doorLockOperationReport(doorLockMode: 1).incomingMessage()
	}

	tiles
	{
		standardTile('lockMain', 'device.lock') {
			state 'locked', label:'locked', action:'lock.unlock', icon:'st.locks.lock.locked', backgroundColor:'#79B821', nextState:'unlocking' // green
			state 'unlocked', label:'unlocked', action:'lock.lock', icon:'st.locks.lock.unlocked', backgroundColor:'#FFA81E', nextState:'locking' // orange
			state 'unknown', label:'unknown', action:'checkState', icon:'st.unknown.unknown.unknown', backgroundColor:'#C92424', nextState:'locking' // red
			state 'error', label:'error', action:'checkState', icon:'st.unknown.unknown.unknown', backgroundColor:'#C92424', nextState:'locking' // red
			state 'locking', label:'locking', action:'', icon:'st.locks.lock.locked', backgroundColor:'#DDDDDD' // grey
			state 'unlocking', label:'unlocking', action:'', icon:'st.locks.lock.unlocked', backgroundColor:'#DDDDDD'// grey
			state 'checking', label:'checking', action:'', icon:'st.unknown.thing.thing-circle', backgroundColor:'#DDDDDD' // grey
		}

		standardTile('lock', 'device.lock', decoration:'flat', width: 2, height: 2) {
			state 'locked', label:'locked', action:'lock.unlock', icon:'st.locks.lock.locked', nextState:'unlocking'
			state 'unlocked', label:'unlocked', action:'lock.lock', icon:'st.locks.lock.unlocked', nextState:'locking'
			state 'unknown', label:'unknown', action:'checkState', icon:'st.unknown.unknown.unknown', nextState:'checking'
			state 'error', label:'error', action:'checkState', icon:'st.unknown.unknown.unknown', nextState:'checking'
			state 'locking', label:'locking', action:'', icon:'st.unknown.thing.thing-circle'
			state 'unlocking', label:'unlocking', action:'', icon:'st.unknown.thing.thing-circle'
			state 'checking', label:'checking', action:'', icon:'st.unknown.thing.thing-circle'
		}

		standardTile('lockAction', 'device.lock', inactiveLabel: false, decoration: 'flat') {
			state 'default', label:'lock', action:'lock.lock', icon:'st.locks.lock.locked', nextState:'locking'
		}
		standardTile('unlockAction', 'device.lock', inactiveLabel: false, decoration: 'flat') {
			state 'default', label:'unlock', action:'lock.unlock', icon:'st.locks.lock.unlocked', nextState:'unlocking'
		}
		valueTile('battery', 'device.battery', inactiveLabel: false, decoration: 'flat') {
			state 'default', label:'${currentValue}% battery', action:'ccBatteryGet', unit:'%'
		}
		standardTile('refresh', 'device.lock', inactiveLabel: false, decoration: 'flat') {
			state 'default', label:'', action:'refresh.refresh', icon:'st.secondary.refresh'
		}
		standardTile('adHoc', 'device.lock', inactiveLabel: false, decoration: 'flat') {
			state 'default', label:'', action:'runAdHocTest', icon:'st.secondary.test'
		}
		standardTile('opMode', 'device.cfgOpMode', inactiveLabel: false, decoration: 'flat' ) {
			state 'NORMAL', label: 'NORMAL', action:'cycleOperatingMode', icon:'st.custom.buttons.add-icon', nextState:'changing'
			state 'LOCKOUT', label: 'KEY OFF/RF ON', action:'cycleOperatingMode', icon:'st.custom.buttons.subtract-icon', nextState:'changing'
			state 'PRIVACY', label: 'KEY OFF/RF OFF', action:'cycleOperatingMode', icon:'st.custom.buttons.subtract-icon', nextState:'changing'
			state 'changing', label: '...', action:'cycleOperatingMode', icon:'st.unknown.thing.thing-circle'
		}

		standardTile('audio', 'device.cfgAudio', inactiveLabel: false, decoration: 'flat' ) {
			state 'SILENT', label: 'AUDIO ${name}', action:'cycleAudioMode', icon:'st.custom.sonos.muted', nextState:'changing'
			state 'LOW', label: 'AUDIO ${name}', action:'cycleAudioMode', icon:'st.custom.sonos.unmuted', nextState:'changing'
			state 'HIGH', label: 'AUDIO ${name}', action:'cycleAudioMode', icon:'st.custom.sonos.unmuted', nextState:'changing'
			state 'OFF', label: 'AUDIO ${name}', action:'cycleAudioMode', icon:'st.custom.sonos.muted', nextState:'changing'
			state 'ON', label: 'AUDIO ${name}', action:'cycleAudioMode', icon:'st.custom.sonos.unmuted', nextState:'changing'
			state 'changing', label: '...', action:'', icon:'st.unknown.thing.thing-circle'
		}

		standardTile('language', 'device.cfgLanguage', inactiveLabel: false, decoration: 'flat' ) {
			state 'ENGLISH', label: 'ENGLISH', action:'cycleLanguage', icon:'st.Entertainment.entertainment3', nextState:'changing'
			state 'FRENCH', label: 'FRANCAIS', action:'cycleLanguage', icon:'st.Entertainment.entertainment3', nextState:'changing'
			state 'SPANISH', label: 'ESPANOL', action:'cycleLanguage', icon:'st.Entertainment.entertainment3', nextState:'changing'
			state 'changing', label: '...', action:'', icon:'st.unknown.thing.thing-circle'
			state 'inactive', label: '', action: '', icon: 'st.secondary.secondary'
		}

/*
		standardTile('autoRelock', 'device.cfgAutoRelock', inactiveLabel: false, decoration: 'flat')
		{
			state 'OFF', label: 'AUTO RELOCK ${name}', action:'cycleAutoRelock', icon:'st.tesla.tesla-unlocked', nextState:'changing'
			state 'ON', label: 'RELOCK IN ${currentValue}s', action:'cycleAutoRelock', icon:'st.tesla.tesla-locked', nextState:'changing'
			state 'changing', label: '...', action:'cycleAutoRelock', icon:'st.unknown.thing.thing-circle'
		}

		controlTile('autoRelockDelaySlider', 'device.cfgAutoRelockDelay', 'slider', inactiveLabel: false, height: 1, width: 2)
		{
			state "default", label:'Auto Relock Delay', action:"setAutoRelockDelay", backgroundColor:"#ff0000"
		}
*/

		standardTile('autoRelockDelay', 'device.tileAutoRelock', inactiveLabel: false, decoration: 'flat') {
			//state 'default', label:'auto relock\ndelay ${currentValue}s', action: 'cycleAutoRelock', icon:'', unit:''
			state 'default', label:'${currentValue}', action: 'cycleAutoRelock', icon:'st.secondary.secondary'
			state 'OFF', label:'auto relock\noff', action: 'cycleAutoRelock', icon:'st.secondary.secondary'
			state 'changing', label: '...', action:'cycleAutoRelock', icon:'st.unknown.thing.thing-circle'
		}
		standardTile('autoRelockDelayUp', 'device.tileAutoRelock', inactiveLabel: false, decoration: 'flat') {
			state 'default', label:'', action:'increaseRelockDelay', icon:'st.thermostat.thermostat-right'
			state '255s', label: '', action: '', icon: 'st.secondary.secondary' // max value
			state 'OFF', label: '', action: '', icon: 'st.secondary.secondary'
			state 'changing', label: '', action:'', icon:'st.secondary.secondary'
		}
		standardTile('autoRelockDelayDown', 'device.tileAutoRelock', inactiveLabel: false, decoration: 'flat') {
			state 'default', label:'', action: 'decreaseRelockDelay', icon: 'st.thermostat.thermostat-left'
			state '5s', label: '', action: '', icon: 'st.secondary.secondary' // min value
			state 'OFF', label: '', action: '', icon: 'st.secondary.secondary'
			state 'changing', label: '', action:'', icon:'st.secondary.secondary'
		}

		standardTile('blankTile', 'device.blank', inactiveLabel: false, decoration: 'flat' )
		{
			state 'default', label: '', action: '', icon: ''
		}

		main 'lockMain'
		//details(['lock', 'lockAction', 'unlockAction', 'battery', 'opMode', 'refresh', 'adHoc'])
		details(['lock', 'battery', 'opMode', 'audio', 'language', 'blankTile', 'autoRelockDelayDown', 'autoRelockDelay', 'autoRelockDelayUp', 'refresh', 'adHoc'])
	}

}

import static groovy.json.JsonOutput.*

import groovy.transform.Field
import physicalgraph.device.HubAction
import physicalgraph.zwave.commands.doorlockv1.*

// constants
@Field final DEVICE_TYPE_VERSION = '0.25'

@Field final TimeZone TZ_UTC = TimeZone.getTimeZone('UTC')
@Field final int MAX_CLOCK_DRIFT_MSEC = 61000
@Field final int MSEC_PER_HOUR = 3600000

@Field final int ASSA_ABLOY_OLD_MFR_ID = 0x0109
@Field final int ASSA_ABLOY_OLD_MFR_ID_MAX_PROD_TYPE = 5
@Field final int ASSA_ABLOY_MFR_ID = 0x0129

@Field final def YALE_PROD_TYPE_ID_MAP = [
	0x01: "Touchscreen Lever",
	0x02: "Touchscreen Deadbolt",
	0x03: "Push Button Lever",
	0x04: "Push Button Deadbolt",
]
// FIXME: need model info for the keyless touchscreen and the new keyless NFC lock

// Represent On and Off the z-wave way
@Field final short ZWAVE_OFF = 0x00
@Field final short ZWAVE_ON = 0xFF

@Field final Short RELOCK_DELTA_SECS = 5

@Field final short DEFAULT_MAX_SLOTS = 10
@Field final short MIN_SLOT_NUM = 1 // 0 is reserved for the master code

@Field final int DEFAULT_DELAY_MSEC = 4200
@Field final int SHORT_DELAY_MSEC = 2200
@Field final int LONG_DELAY_MSEC = 8400
@Field final int SET_GET_DELAY_MSEC = 7000
@Field final int ASSOC_DELAY_MSEC = 6000

@Field final int CHECK_BATTERY_EVERY_N_HOURS = 6
@Field final int GET_TIME_EVERY_N_HOURS = 1

@Field final short ASSOCIATION_GROUPING_ID = 1

@Field final def COMMAND_CLASS_VERSIONS = [0x98: 1, 0x8b: 1, 0x8a: 2, 0x86: 1, 0x80: 1, 0x75: 2, 0x72: 1, 0x71: 1, 0x70: 1, 0x63: 1, 0x62: 1, 0x4e: 3, 0x4c: 1]

@Field final short CONFIG_VALUE_BYTE_SIZE = 1 // All parameter values of the Yale lock are 1 byte

@Field Map smartconfig

@Field final String zweh = 'Z-WaveEventHandler' // For handlers of events sent by the device itself
@Field final String dti = 'DeviceTypeInternal' // for commands that are automatically called in a device type's lifecycle
@Field final String ccmd = 'STDeviceCommand' // capability or standalone command
@Field final String ccc = 'CommandClassCommand' // wraps a single command class

@Field pendingSlotNameMap = [:]

// handle system events

// called after a device instance is first installed
def installed()
{
	initialize()
	smartlog(dwi, LEVEL_FINE, 'func installed')

	return bootstrapCommands()
}

private def bootstrapCommands()
{
	def cmdq = CommandQueue()
	cmdq.add(ccManufacturerSpecificGet(), LONG_DELAY_MSEC)
	cmdq.add(ccSecurityCommandsSupportedGet())
	cmdq.add(ccUsersNumberGet())
	cmdq.add(ccAssociationGet())
	cmdq.add(ccVersionGet())
	// cmdq.add(refresh())
	return cmdq.getCommands()
}

// called when device preferences are saved in the ST app
def updated()
{
	initialize()
	smartlog(dwi, LEVEL_FINE, 'func updated')
	def sc = SmartConfig()
	smartlog(dwi, LEVEL_INFO, "state.config holds: ${state.config}")

	def cmdq = CommandQueue()
	cmdq.add(refreshAutoRelockTileset())
	return cmdq.getCommands()
}

void initialize()
{
	if (isInitialized)
	{
		smartlog(LEVEL_WARN, 'device is already initialized. please call reinitialize')
	}
	initSmartlog()
	smartlog(LEVEL_FINE, 'func initialize')
	setSmartlogOverrideLevel(LEVEL_FINE)
	if (state.codeDb == null) state.codeDb = [:]
	if (state.pendingCodeDb == null) state.pendingCodeDb = [:]
	if (state.settings == null) state.settings = [:]
	if (state.config == null) state.config = [:]
	if (state.protection == null) state.protection = [:]
	if (state.lastTriedMode == null) state.lastTriedMode = [:]
	state.isInitialized = true
}

// initialize SmartConfig
void initSmartConfig()
{
	smartlog(LEVEL_FINE, 'func initSmartConfig')
	if (smartconfig == null)
	{
		smartconfig = SmartConfig()
	}
}

// implement commands for ST capabilities

// command to test out various commands
def runAdHocTest()
{
	smartlog(ccmd, LEVEL_FINE, "func runAdHocTest")
	def cmdq = CommandQueue()
	initialize()

	cmdq.add()

	// done adding commands

	smartlog(ccmd, LEVEL_DEBUG, "This is the adHoc command list:")
	smartlog(ccmd, LEVEL_DEBUG, cmdq.getCommands())
	smartlog(ccmd, LEVEL_DEBUG, "Finished building commands for runAdHocTest - now to ship them off to the device")
	return cmdq.getCommands()
}

def cycleOperatingMode()
{
	smartlog(LEVEL_FINE, "func cycleOperatingMode")
	return setChoiceParameter('op_mode')
}

def cycleAudioMode()
{
	smartlog(LEVEL_FINE, "func cycleAudioMode")
	return setChoiceParameter('audio_mode')
}

def cycleAutoRelock()
{
	smartlog(LEVEL_FINE, "func cycleAutoRelock")
	sendEvent(createEvent([name:'tileAutoRelock', value:'changing'])) // set tileset to changing
	return setChoiceParameter('auto_relock')
}

def cycleLanguage()
{
	smartlog(LEVEL_FINE, "func cycleLanguage")
	return setChoiceParameter('language')
}

private def setChoiceParameter(String parameterName)
{
	smartlog(LEVEL_FINE, "func setChoiceParameter($parameterName)")

	def cmdq = CommandQueue()
	def sc = SmartConfig()
	String attrName = sc.getDeviceAttributeName(parameterName)
	if (attrName)
	{
		ArrayList modeOrder = sc.getChoices(parameterName).sort { a, b -> a.value <=> b.value }*.key
		def defaultValue = sc.getParameterDefaultValue(parameterName)?:modeOrder[0]
		def currentMode = device.currentState(attrName)?.value
		def lastTriedMode = state.lastTriedMode?."$attrName" ?: currentMode ?: defaultValue
		def next = { modeOrder[modeOrder.indexOf(it) + 1] ?: modeOrder[0] }
		def nextMode = next(lastTriedMode)
		state.lastTriedMode?."$attrName" = nextMode
		cmdq.add(setGetConfigMacro(sc.getParameterNum(parameterName), sc.choiceValue(parameterName, nextMode) as Short))
	}
	else
	{
		smartlog(LEVEL_WARN, "config parameter $parameterName has no device attribute name. no action to take for setChoiceParameter")
	}
	return cmdq.getCommands()
}

def increaseRelockDelay()
{
	smartlog(LEVEL_FINE, "func increaseRelockDelay")
	return adjustRelockDelay(RELOCK_DELTA_SECS)
}

def decreaseRelockDelay()
{
	smartlog(LEVEL_FINE, "func decreaseRelockDelay")
	return adjustRelockDelay(-RELOCK_DELTA_SECS)
}

private def adjustRelockDelay(Short delta)
{
	smartlog(LEVEL_FINE, "func adjustRelockDelay by $delta")
	String parameterName = 'auto_relock_delay'
	def cmdq = CommandQueue()
	def sc = SmartConfig()
	Integer defaultRelockDelay = sc.getParameterDefaultValue(parameterName)
	Short oldDelay = (device.currentValue(sc.getDeviceAttributeName(parameterName)) as Integer?:defaultRelockDelay) as Short
	Short newDelay = sc.constrainValueInRange(parameterName, (oldDelay + delta) as Short)
	log.debug("Old delay: $oldDelay; Delta: $delta; New delay: $newDelay")
	// only send an event if we've changed the value
	if (newDelay != oldDelay)
	{
		//sendEvent(createEvent([name:'tileAutoRelock', value:'changing'])) // set tileset to changing
		indicateTileIsChanging('tileAutoRelock')
		cmdq.add(setGetConfigMacro(sc.getParameterNum(parameterName), newDelay))
		//log.debug "sending adjust event: $eventMap"
		//sendEvent(eventMap)
	}
	return cmdq.getCommands()
}

def setAutoRelockDelay(sliderValue)
{
	smartlog(LEVEL_FINE, "func setAutoRelockDelay $sliderValue")
	setRangeParameter('auto_relock', sliderValue)
}

def setRangeParameter(String parameterName, sliderValue)
{
	smartlog(LEVEL_FINE, "func setRangeParameter $parameterName $sliderValue")
	def cmdq = CommandQueue()
	def sc = SmartConfig()
	String attrName = sc.getDeviceAttributeName(parameterName)
	if (attrName)
	{
		cmdq.add(setGetConfigMacro(sc.getParameterNum(parameterName), rangeAdjustedVal as Short))
	}
	else
	{
		smartlog(LEVEL_WARN, "config parameter $parameterName has no device attribute name. no action to take for setRangeParameter")
	}
	return cmdq.getCommands()
}

private void indicateTileIsChanging(String tileAttributeName)
{
	smartlog(LEVEL_FINE, "func indicateTileIsChanging $tileArreibuteName")
	sendEvent(createEvent([name: tileAttributeName, value:'changing'])) // set tileset to changing
}

// capability.Polling
def poll()
{
	smartlog(ccmd, LEVEL_FINE, 'capability command Polling.poll')
	smartlog(ccmd, LEVEL_INFO, 'polling has been disabled for now, just know that it was called')
	def cmdq = CommandQueue()

	if (! state.maxSlotNum)
	{
		cmdq.add(ccUsersNumberGet())
	}

	smartlog(LEVEL_DEBUG, "poll: check main attributes")
	cmdq.add(ccAssociationGet())
	cmdq.add(pollLockOperation())
	cmdq.add(pollBattery())

	if ((!state.codeDb || state.codeDb.size() == 0) && !state.pollUserCodes)
	{
		// no or empty code db, and no polling started? start poll for user codes.
		cmdq.add(pollUserCodes())
	}
	else
	{
		// Check if we need to resume a user code polling that quit
		// i.e., no UserCodeReport for a userCodeGet
		cmdq.add(resumeUserCodePoll())
	}

	// doing this dead last as the clock set tends to get lost
	cmdq.add(pollClock())

	smartlog(LEVEL_DEBUG, "poll is sending ${cmdq.getCommands()}")
	return cmdq.getCommands()
}

// capability.Configuration
def configure() {
	smartlog(LEVEL_FINE, "Executing 'capability.Configuration.configure'")
	initialize()
}

// capability.Refresh
def refresh() {
	smartlog(LEVEL_FINE, "capability command Refresh.refresh")
	def cmdq = CommandQueue()

	if (state?.config_PARAMETER_METADATA)
	{
		state.remove('config_PARAMETER_METADATA')
	}
	if (state?.config_PARMETER_METADATA)
	{
		state.remove('config_PARMETER_METADATA')
	}

	cmdq.add(pollLockOperation(true))
	if (!state.maxSlotNum)
	{
		cmdq.add(ccUsersNumberGet())
	}

	cmdq.add(pollBattery(true))

	state.forceClockGet = true
	cmdq.add(pollClock())

	//cmds.addAll(getConfigAll()) // has trailing delay already

	cmdq.add(refreshAssociation()?:[])

	smartlog(LEVEL_DEBUG, "refresh commands: ${cmdq.getCommands()}")
	return cmdq.getCommands()
}

// capability.Lock + extra related command
def lock()
{
	smartlog(LEVEL_FINE, "capability command Lock.lock")
	def eventMap = [ name: "toggle", value: "locking", displayText: "$device.displayName is locking", displayed: false]
	sendEvent(eventMap)
	return setGetDoorLockOperationMacro(DoorLockOperationSet.DOOR_LOCK_MODE_DOOR_SECURED)
}

def unlock()
{
	smartlog(LEVEL_FINE, "capability command Lock.unlock")
	def eventMap = [ name: "toggle", value: "unlocking", displayText: "$device.displayName is unlocking", displayed: false]
	sendEvent(eventMap)
	return setGetDoorLockOperationMacro(DoorLockOperationSet.DOOR_LOCK_MODE_DOOR_UNSECURED)
}

def unlockWithTimeout()
{
	smartlog(LEVEL_FINE, "custom command unlockWithTimeout")
	def eventMap = [ name: "toggle", value: "unlocking", displayText: "${device.displayName} is unlocking (with timeout)", displayed: false]
	sendEvent(eventMap)
	return setGetDoorLockOperationMacro(DoorLockOperationSet.DOOR_LOCK_MODE_DOOR_UNSECURED_WITH_TIMEOUT)
}

def checkState()
{
	smartlog(LEVEL_FINE, "func checkState")
	return getDoorLockOperation()

}
// capability.LockCodes

// lock and unlock overlap with capability.Lock

def updateCodes(String jsonCodeMap) {
	smartlog(LEVEL_FINE, "Executing 'capability.LockCodes.updateCodes'")
	// TODO: handle 'updateCodes' command
}

def setCode(String codeStr, Short slotNumber) {
	smartlog(LEVEL_FINE, "Executing 'capability.LockCodes.setCode'")
	// validate code
	// validate slot number
	return setCodeMacro(slotNumber, codeStr)
}

def deleteCode(Short slotNumber) {
	smartlog(LEVEL_FINE, "Executing 'capability.LockCodes.deleteCode'")
	// validate slot number
	return deleteCodeMacro(slotNumber)
}

def requestCode(Short slotNumber) {
	smartlog(LEVEL_FINE, "Executing 'capability.LockCodes.requestCode' for $slotNumber")
	// validate slot number
	if (validateSlotNumber(slotNumber))
	return getCodeMacro(slotNumber)
}

// ask the device for all user codes
def reloadAllCodes() {
	smartlog(LEVEL_FINE, "Executing 'capability.LockCodes.reloadAllCodes'")
	def cmdq = CommandQueue()
	def pollCmd

	if (state.pollUserCodes)
	{
		// See if the polling in progress needs a restart
		pollCmd = resumeUserCodePoll()
	}
	else
	{
		// since no polling was in progress, start one
		pollCmd = pollUserCodes()
	}

	if (pollCmd == null)
	{
		smartlog(LEVEL_INFO, "Taking no action for reloadAllCodes")
		smartlog(LEVEL_INFO, "A user code polling for $device is already in progress and not yet considered stalled.")
	}

	return pollCmd
}

// generic parser for a z-wave event description, returning an ST event
def parse(String description)
{
	setSmartlogLevel(LEVEL_FINE)
	setSmartlogLevel('SmartConfig', LEVEL_WARN)
	setSmartlogLevel('CommandQueue', LEVEL_INFO)
	setSmartlogLevel('ResponseQueue', LEVEL_INFO)
	setSmartlogLevel('SmartConfig construction', LEVEL_NONE)
	setSmartlogLevel('CommandQueue construction', LEVEL_NONE)
	setSmartlogLevel('ResponseQueue construction', LEVEL_NONE)

	smartlog(dti, LEVEL_FINE, "parse")
	smartlog(dti, LEVEL_DEBUG, "parse arg: $description")
	def result = null

	if (description.startsWith("Err"))
	{
		if (state?.secureKeyVerified)
		{
			// We have a security key, so the error isn't security related
			result = createEvent(descriptionText:description, displayed:true)
		}
		else
		{
			// It's a security error
			result = createEvent(
				descriptionText: "This lock failed to complete the network security key exchange. If you are unable to control it via SmartThings, you must remove it from your network and add it again.",
				eventType: "ALERT",
				name: "secureInclusion",
				value: "failed",
				displayed: true,
			)
			log.error result.descriptionText
		}
	}
	else if (description == "updated")
	{
		result = updated()
	}
	else
	{
		def cmd = zwave.parse(description, COMMAND_CLASS_VERSIONS)
		if (cmd)
		{
			smartlog(dti, LEVEL_TRACE, 'parse is handing off to specific zwaveEvent handler overload method')
			result = zwaveEvent(cmd)
			smartlog(dti, LEVEL_TRACE, "zwaveEvent handler overload returned: $result")
		}
	}
	return result
}


/*
 Command classes are implemented below, grouped by class and containing methods for:
  * Z-wave events that we handle
  * Z-wave events that we send to the device (commands)
  * helper methods specific to the command class
*/

// handle an unknown/unexpected Z-wave event sent from the device
def zwaveEvent(physicalgraph.zwave.Command deviceEvent)
{
	smartlog(zweh, LEVEL_WARN, "Handling event: unhandled or unexpected zwave command $deviceEvent")
	return createEvent(displayed: false, descriptionText: "$device.displayName: $deviceEvent")
}


// CommandClass Security v1

// Parses a secured z-wave event, returning the unencrypted event contained within
def zwaveEvent(physicalgraph.zwave.commands.securityv1.SecurityMessageEncapsulation secureDeviceEvent)
{
	smartlog(zweh, LEVEL_TRACE, "handling event securityv1.SecurityMessageEncapsulation. Opening encapsulated message.")
	def commandClassIdentifier = formatOctetAsHex(secureDeviceEvent.commandClassIdentifier)
	smartlog(zweh, LEVEL_DEBUG, "Secure message command is class: $commandClassIdentifier; command: $secureDeviceEvent.commandIdentifier")
	//def encapsulatedCommand = secureDeviceEvent.encapsulatedCommand([0x20: 1,0x62: 1, 0x63: 1, 0x70: 1, 0x71: 2, 0x75: 1, 0x80:1, 0x85: 2, 0x4E: 2, 0x4C: 1, 0x8B: 1, 0x5D: 2])
	def encapsulatedCommand = secureDeviceEvent.encapsulatedCommand(COMMAND_CLASS_VERSIONS)
	if (encapsulatedCommand)
	{
		smartlog(zweh, LEVEL_DEBUG, "unpacked secure message from $device: $encapsulatedCommand")
		return zwaveEvent(encapsulatedCommand)
	}
	else
	{
		smartlog(zweh, LEVEL_DEBUG, "The command we unpacked was null... The SecurityMessageEncapsulation object instance was: $secureDeviceEvent")
	}
}

def zwaveEvent(physicalgraph.zwave.commands.securityv1.NetworkKeyVerify cmd)
{
	smartlog(zweh, LEVEL_TRACE, "handling event securityv1.NetworkVerify")
	state.secureKeyVerified = true
	def resQ = ResponseQueue()
	resQ.add(createEvent(name:"secureInclusion", value:"success", descriptionText:"Secure inclusion was successful"))
	//resQ.add(bootstrapCommands())
	return resQ.getWrappedCommands()
}

def ccSecurityCommandsSupportedGet()
{
	smartlog(ccc, LEVEL_FINE, "Executing zwave.securityV1.securityCommandsSupportedGet")
	// 0x9803 -> 0x9804
	return secure(zwave.securityV1.securityCommandsSupportedGet())
}

def zwaveEvent(physicalgraph.zwave.commands.securityv1.SecurityCommandsSupportedReport cmd)
{
	smartlog(zweh, LEVEL_FINE, "handling event securityv1.SecurityCommandsSupportedReport")
	state.sec = cmd.commandClassSupport.collect { String.format("%02X ", it) }.join()
	if (cmd.commandClassControl)
	{
		state.secCon = cmd.commandClassControl.collect { String.format("%02X ", it) }.join()
	}
	smartlog(zweh, LEVEL_DEBUG, "Security command classes: $state.sec")
	return createEvent(name:"secureInclusion", value:"success", descriptionText:"Lock is securely included")
}

// helper to securely encapsulate a Z-wave command before sending it
def secure(physicalgraph.zwave.Command cmd)
{
	smartlog(LEVEL_FINE, "Encapsulating: $cmd")
	def secureCmd = zwave.securityV1.securityMessageEncapsulation().encapsulate(cmd).format()
	smartlog(LEVEL_FINE, "After encapsulation: $secureCmd")
	return secureCmd
}

// encapsulate a series of commands separated by the specified delay
def secureSequence(commands, delay=DEFAULT_DELAY_MSEC)
{
	return delayBetween(commands.collect{ secure(it) }, delay)
}


// CommandClass Alarm v1
// alarm reports are how the lock conveys lock/unlock events, faults, tampering and code sets
def zwaveEvent(physicalgraph.zwave.commands.alarmv1.AlarmReport deviceEvent) {
	smartlog(zweh, LEVEL_FINE, "handling alarmv1.AlarmReport event")
	def respq = ResponseQueue()
	def eventMap = [ name: "lock", descriptionText: "description", value: "unknown", displayed: false]
	smartlog(zweh, LEVEL_DEBUG, "$device.displayName AlarmReport type: $deviceEvent.alarmType; level: $deviceEvent.alarmLevel")
	switch (deviceEvent.alarmType as Integer)
	{
		case 9:
			// deadbolt jammed
			eventMap.descriptionText = "$device.displayName - deadbolt jammed"
			eventMap.value = "error"
			eventMap.displayed = true
			break
		case 18:
			// Keypad lock (level = user code slot #)
			eventMap.descriptionText = "$device.displayName was locked using keypad with code $deviceEvent.alarmLevel"
			eventMap.value = "locked"
			break
		case 19:
			// Keypad unlock (level = user code slot #)
			eventMap.descriptionText = "$device.displayName was unlocked using keypad with code $deviceEvent.alarmLevel"
			eventMap.value = "unlocked"
			break
		case 21:
			// Manual lock
			// lock by touch
			if (deviceEvent.alarmLevel == 1) eventMap.descriptionText = "$device.displayName was locked manually"
			if (deviceEvent.alarmLevel == 2) eventMap.descriptionText = "$device.displayName was locked using lock & leave"
			eventMap.value = "locked"
			break
		case 22:
			// Manual unlock
			eventMap.descriptionText = "$device.displayName was unlocked manually"
			eventMap.value = "unlocked"
			break
		case 24:
			// RF lock
			eventMap.descriptionText = "$device.displayName was locked by RF module"
			eventMap.value = "locked"
			break
		case 25:
			// RF unlock
			eventMap.descriptionText = "$device.displayName was unlocked by RF module"
			eventMap.value = "unlocked"
			break
		case 27:
			// Auto locked
			eventMap.descriptionText = "$device.displayName locked by auto-lock"
			eventMap.value = "locked"
			break
		case 33:
			// User deleted (level = user #1-249)
			eventMap.name = "userCode"
			eventMap.descriptionText = "User code deleted from slot ${deviceEvent.alarmLevel} on $device.displayName"
			break
		case 112:
			// Master code changed (level = 0)
			// User code set (level = 1-249)
			if (deviceEvent.alarmLevel == 0)
			{
				eventMap.name = "masterCode"
				eventMap.descriptionText = "Master Code (slot 0) was changed"
			}
			else
			{
				eventMap.name = "userCode"
				eventMap.descriptionText = "User Code for slot ${deviceEvent.alarmLevel} set or updated on $device.displayName"
			}
			break
		case 113:
			// duplicate PIN code error (level = slot 0-249)
			eventMap.name = "userCode"
			eventMap.descriptionText = "User Code for slot ${deviceEvent.alarmLevel} is a duplicate in another slot on $device.displayName"
			// clead from codeDb?
			break
		case 130:
			// RF module power cycled
			eventMap.descriptionText = "Power has been restored to the RF module in $device.displayName"
			eventMap.displayed = true
			// set the clock, and check the battery on the next poll
			respq.add(getDoorLockOperation())
			respq.add(ccBatteryGet())
			respq.add(setDeviceClockToCurrentTimeUtcMacro())
			break
		case 131:
			// Lock handing completed
			eventMap.descriptionText = "Lock handing complete on $device.displayName"
			break
		case 161:
			// tamper alarm
			eventMap.name = "alarm"
			eventMap.displayed = true
			if (deviceEvent.alarmLevel == 1) eventMap.descriptionText = "$device.displayName tamper alarm - incorrect keypad attempts exceed limit"
			if (deviceEvent.alarmLevel == 2) eventMap.descriptionText = "$device.displayName front esctucheon removed from main"
			break
		case 167:
			// battery low
			eventMap.name "battery"
			eventMap.displayed = true
			eventMap.descriptionText = "Low battery level on $device.displayName"
			break
		case 168:
			// battery critical
			eventMap.name "battery"
			eventMap.displayed = true
			eventMap.descriptionText = "Critical battery level on $device.displayName"
			break
		case 169:
			// battery too low to operate
			eventMap.name "battery"
			eventMap.displayed = true
			eventMap.descriptionText = "Battery level too low to operate on $device.displayName"
			break
		default:
			eventMap = [ displayed: false, name: "Unknown Alarm", descriptionText: "$device.displayName: unknown alarm - $deviceEvent" ]
			break
	}
	smartlog(zweh, LEVEL_INFO, "Alarm received: ${eventMap.name}: ${eventMap.descriptionText}")
	// prepend the alarm event to the commands list
	respq.prepend(createEvent(eventMap))
	return respq.getWrappedCommands()
}


// CommandClass Association v2
// This should be v1, but a bug in the ST Zwave utility class necessitates using v2 as a workaround
def ccAssociationSet()
{
	smartlog(ccc, LEVEL_FINE, "setting $device.displayName association to groupingIdentifier $ASSOCIATION_GROUPING_ID for nodeId $zwaveHubNodeId")
	return secure(zwave.associationV2.associationSet(groupingIdentifier:ASSOCIATION_GROUPING_ID, nodeId:zwaveHubNodeId))
}

def ccAssociationRemove()
{
	smartlog(ccc, LEVEL_FINE, "removing $device.displayName association to groupingIdentifier $ASSOCIATION_GROUPING_ID for nodeId $zwaveHubNodeId")
	state.assoc = null // appropriate?
	return secure(zwave.associationV2.associationRemove(groupingIdentifier:ASSOCIATION_GROUPING_ID, nodeId:zwaveHubNodeId))
}

def ccAssociationGet()
{
	smartlog(ccc, LEVEL_FINE, "getting $device.displayName association for groupingIdentifier $ASSOCIATION_GROUPING_ID")
	return secure(zwave.associationV2.associationGet(groupingIdentifier:ASSOCIATION_GROUPING_ID))
}

def zwaveEvent(physicalgraph.zwave.commands.associationv2.AssociationReport deviceEvent)
{
	smartlog(zweh, LEVEL_TRACE, "handling event associationv2.associationReport")
	smartlog(zweh, LEVEL_DEBUG, "Raw associationReport: $deviceEvent")
	def result = []
	if (deviceEvent.nodeId.any { it == zwaveHubNodeId })
	{
		smartlog(zweh, LEVEL_INFO, "Hub $zwaveHubNodeId is associated with $device.displayName association group $deviceEvent.groupingIdentifier")
		state.assoc = zwaveHubNodeId
		state.lastAssocQueryTime = new Date().time
	}
	else
	{
		smartlog(zweh, LEVEL_INFO, "association not found, setting it")
		result << response(ccAssociationSet())
		state.lastAssocQueryTime = null
	}
	return result
}

private def associationSetGetMacro()
{
	smartlog(LEVEL_FINE, "set then check $device association")
	def cmdq = CommandQueue()
	cmdq.add(ccAssociationSet(), ASSOC_DELAY_MSEC)
	cmdq.add(ccAssociationGet(), ASSOC_DELAY_MSEC)
	return cmdq.getCommands()
}

private def refreshAssociation()
{
	smartlog(LEVEL_FINE, "f refreshAssociation")
	def cmdq = CommandQueue()
	if (state.assoc && state.assoc == zwaveHubNodeId)
	{
		smartlog(LEVEL_DEBUG, "$device.displayName is associated to ${state.assoc}")
	}
	else if (!state.lastAssocQueryTime)
	{
		smartlog(LEVEL_DEBUG, "checking $device association")
		cmdq.add(ccAssociationGet())
	}
	else if (secondsPast(state.lastAssocQueryTime, 9*60))
	{
		cmdq.add(associationSetGetMacro())
	}
	return cmdq.getCommands()
}


// CommandClass Battery v1

def ccBatteryGet()
{
	smartlog(ccc, LEVEL_FINE, "battery.ccBatteryGet")
	return secure(zwave.batteryV1.batteryGet())
}

def zwaveEvent(physicalgraph.zwave.commands.batteryv1.BatteryReport deviceEvent)
{
	smartlog(zweh, LEVEL_FINE, "handling event batteryv1.BatteryReport")
	def map = [ name: "battery", unit: "%" ]
	if (deviceEvent.batteryLevel == 0xFF)
	{
		map.value = 1
		map.descriptionText = "$device.displayName has a low battery"
	}
	else
	{
		map.value = deviceEvent.batteryLevel
	}
	state.lastBatteryCheckTime = now()
	return createEvent(map)
}

private pollBattery(Boolean checkBattery=false)
{
	smartlog(LEVEL_FINE, "pollBattery: Check if it's time to ask the $device its battery level")

	if (checkBattery)
	{
		smartlog(LEVEL_INFO, "Battery check forced for $device")
	}
	else if (hasTimePassed(state.lastBatteryCheckTime, CHECK_BATTERY_EVERY_N_HOURS, 0, 0))
	{
		smartlog(LEVEL_INFO, "Battery check is due for $device - it has been at least $CHECK_BATTERY_EVERY_N_HOURS since last checked")
		checkBattery = true
	}

	if (checkBattery)
	{
		smartlog(LEVEL_INFO, "time to request device battery value")
		return  ccBatteryGet()
	}
	return null
}


// CommandClass Configuration v1

def ccConfigurationSet(short paramNum, short paramValue)
{
	smartlog(ccc, LEVEL_FINE, "ccConfigurationSet $paramNum $paramValue")
	def sc = SmartConfig()
	if (sc.validateParameterValue(paramNum, paramValue))
	{
		def paramName = sc.getParameterName(paramNum)
		smartlog(LEVEL_INFO, "Setting config parameter ${paramName} ($paramNum) to $paramValue")
		return secure(zwave.configurationV1.configurationSet(
			parameterNumber: paramNum,
			size: CONFIG_VALUE_BYTE_SIZE,
			configurationValue: [paramValue])
		)
	}
	smartlog(LEVEL_WARN, "skip setting of config parameter due to bad parameter data")
	return false
}

// TODO: resume from here instrumenting functions with an initial finetrace smartlog msg

def ccConfigurationGet(String paramName)
{
	smartlog(ccc, LEVEL_FINE, "ccConfigurationGet $paramName (by name)")
	def sc = SmartConfig()
	Short paramNum = sc.getParameterNum(paramName)
	return ccConfigurationGet(paramNum)
}

def ccConfigurationGet(Short paramNum)
{
	smartlog(ccc, LEVEL_FINE, "ccConfigurationGet $paramNum (by number)")
	def sc = SmartConfig()
	def paramName = sc.getParameterName(paramNum)
	if (paramName)
	{
		smartlog(LEVEL_INFO, "requesting ${device.displayName} config setting for parameter $paramName ($paramNum)")
		return secure(zwave.configurationV1.configurationGet(parameterNumber: paramNum))
	}
	return null
}

def zwaveEvent(physicalgraph.zwave.commands.configurationv1.ConfigurationReport deviceEvent)
{
	smartlog(zweh, LEVEL_TRACE, 'handling zwave event configuration.ConfigurationReport')
	smartlog(zweh, LEVEL_DEBUG, "raw event: $deviceEvent")
	def rq = ResponseQueue()
	def sc = SmartConfig()
	if (! state.config) state.config = [:]
	if (deviceEvent.parameterNumber)
	{
		def eventMap = [ name: "config" ]
		short paramNum = deviceEvent.parameterNumber
		def paramName = sc.getParameterName(paramNum)
		def paramDisplayName = sc.getDisplayName(paramNum)
		short paramValue = deviceEvent.configurationValue[0]
		def paramDisplayValue = paramValue
		String paramDisplayString = "$paramValue"
		if (sc.isChoiceType(paramName))
		{
			paramDisplayValue = sc.choiceName(paramName, paramValue)
			if (paramDisplayString != paramDisplayValue)
			{
				paramDisplayString = "$paramDisplayValue ($paramValue)"
			}
		}
		state.config[paramNum] = paramValue // update the config as held in the state db
		eventMap.value = paramValue
		eventMap.descriptionText = "${device.displayName} config parameter $paramDisplayName ($paramNum) is set to $paramDisplayValue"
		log.info eventMap.descriptionText
		rq.add(createEvent(eventMap))

		// look for a way to abstract out inter-dependency
		if (paramName && paramName.startsWith('auto_relock'))
		{
			smartlog(zweh, LEVEL_DEBUG, ">> special handling for $paramName")
			rq.add(refreshAutoRelockTileset())
		}

		// If the parameter metadata contains a device attr, send an event to set the attribute
		String deviceAttrName = sc.getDeviceAttributeName(paramName)
		if (deviceAttrName)
		{
			def cfgAttrEventMap = [	name: deviceAttrName,
									value: paramDisplayValue,
									descriptionText: "${eventMap.descriptionText}; storing value in device attribute $deviceAttrName"]
			rq.add(createEvent(cfgAttrEventMap))
		}
		rq.add(createEvent(eventMap))
	}
	return rq.getWrappedCommands()
}

def refreshAutoRelockTileset()
{
	smartlog(LEVEL_TRACE, 'func refreshAutoRelockTileset')
	def cmdq = CommandQueue()
	def sc = SmartConfig()
	String arTileAttrName = 'tileAutoRelock'
	String arParamName = 'auto_relock'
	String arDlyParamName = 'auto_relock_delay'
	Short arParamNum = sc.getParameterNum(arParamName)
	Short arParamVal = state.config?."$arParamNum" as Short
	Short arDlyParamNum = sc.getParameterNum(arDlyParamName)
	Short arDlyParamVal = state.config?."$arDlyParamNum" as Short

	smartlog(LEVEL_DEBUG, "state value for auto_relock: $arParamVal; for auto_relock_delay: $arDlyParamVal")

	// if we don't have both auto relock config parameters in state, then fetch the missing
	if (arParamVal == null)
	{
		cmdq.add(ccConfigurationGet(arParamNum), SHORT_DELAY_MSEC)
	}

	if (arDlyParamVal == null)
	{
		cmdq.add(ccConfigurationGet(arDlyParamNum), SHORT_DELAY_MSEC)
	}

	if (arParamVal != null && sc.choiceName(arParamName, arParamVal) == 'ON' && arDlyParamVal != null)
	{
		String delayString = sc.constrainValueInRange(arDlyParamName, arDlyParamVal as Short).toString() + 's'
		def eventMap = createEvent([name: arTileAttrName, value: delayString])
		smartlog(LEVEL_INFO, "setting auto relock tileset to $delayString: $eventMap")
		sendEvent(eventMap)
	}
	else
	{
		// set relock buttons to OFF
		def eventMap = createEvent([name: arTileAttrName, value: 'OFF'])
		smartlog(LEVEL_INFO, "setting auto relock tileset to off: $eventMap")
		sendEvent(eventMap)
	}
	return cmdq.getCommands()
}

private def setGetConfigMacro(short paramNum, short paramValue)
{
	smartlog(LEVEL_FINE, "func setGetConfigMacro $paramNum $paramVal")
	def cmdq = CommandQueue()
	cmdq.add(ccConfigurationSet(paramNum, paramValue), SHORT_DELAY_MSEC)
	cmdq.add(ccConfigurationGet(paramNum))
	return cmdq.getCommands()
}

private def getAllConfigParameters()
{
	smartlog(LEVEL_FINE, "func getAllConfigParameters")
	def sc = SmartConfig()
	def paramNumList = sc.getParameterNumList()
	def cmdq = CommandQueue()
	paramNumList.sort().each {
		cmdq.add(ccConfigurationGet(it.shortValue()))
	}
	return cmdq.getCommands()
}


// CommandClass DoorLock v1

def setDoorLockOperation(Short doorLockModeOption)
{
	smartlog(ccc, LEVEL_FINE, "doorLockOperationSet $doorLockModeOption")
	smartlog(LEVEL_INFO, "set the lock's current mode state to $doorLockModeOption")
	return secure(zwave.doorLockV1.doorLockOperationSet(doorLockMode: doorLockModeOption))
}

def getDoorLockOperation()
{
	smartlog(ccc, LEVEL_FINE, "doorLockOperationGet")
	return secure(zwave.doorLockV1.doorLockOperationGet())
}

def zwaveEvent(physicalgraph.zwave.commands.doorlockv1.DoorLockOperationReport deviceEvent)
{
	smartlog(zweh, LEVEL_FINE, "handling event doorlockv1.doorLockOperationReport")
	def eventMap = [ name: 'lock', displayed: false]
	String deviceModeDescription
	switch(deviceEvent.doorLockMode as Integer)
	{
		case 255:
			eventMap.value = 'locked'
			deviceModeDescription = 'locked'
			break
		case 0:
			eventMap.value = 'unlocked'
			deviceModeDescription = 'unlocked'
			break
		case 1:
			eventMap.value = 'unlocked'
			deviceModeDescription = 'unlocked with timeout'
			break
		case 16:
			eventMap.value = 'unlocked'
			deviceModeDescription = 'unlocked for inside door handle'
			break
		case 17:
			eventMap.value = 'unlocked'
			deviceModeDescription = 'unlocked for inside door handle with timeout'
			break
		case 32:
			eventMap.value = 'unlocked'
			deviceModeDescription = 'unlocked for outside door handle'
			break
		case 33:
			eventMap.value = 'unlocked'
			deviceModeDescription = 'unlocked for outside door handle with timeout'
			break
		default:
			eventMap.value = 'unknown'
			deviceModeDescription = "unknown door lock mode: ${deviceEvent.doorLockMode}"
			break
	}

	eventMap.description = deviceEvent as String
	eventMap.descriptionText = "${device.displayName} has reported its mode as $deviceModeDescription (${deviceEvent.doorLockMode})"
	smartlog(zweh, LEVEL_DEBUG, "DoorLockOperationReport: ${eventMap.descriptionText}")
	return createEvent(eventMap)
}

private def setGetDoorLockOperationMacro(Short doorLockModeOption)
{
	smartlog(LEVEL_FINE, "func setGetDoorLockOperationMacro $doorLockModeOption")
	def cmdq = CommandQueue()
	smartlog(LEVEL_INFO, "Setting lock status to $doorLockModeOption, waiting for $SHORT_DELAY_MSEC milliseconds, then checking it")
	cmdq.add(setDoorLockOperation(doorLockModeOption), SHORT_DELAY_MSEC)
	cmdq.add(getDoorLockOperation())
	smartlog(LEVEL_DEBUG, "setGetDoorLockOperationMacro commands: ${cmdq.getCommands()}")
	return cmdq.getCommands()
}

private def pollLockOperation(Boolean checkLock=false)
{
	smartlog(LEVEL_FINE, "func pollLockOperation checkLock=$checkLock")
	def cmdq = CommandQueue()
	if (!state.lastPoll) state.lastPoll = 0
	def latest = device.currentState("lock")?.date?.time
	if (checkLock)
	{
		smartlog(LEVEL_INFO, "Lock operation check forced.")
		cmdq.add(getDoorLockOperation())
	}
	else if (!latest || hasTimePassed(latest, 0, 6, 0) || hasTimePassed(state.lastPoll, 1, 7, 0))
	{
		smartlog(LEVEL_INFO, "Device lock state check is due.")
		cmdq.add(getDoorLockOperation())
	}

	if (cmdq.qSize() > 0)
	{
		state.forceCheckLock = false
		state.lastPoll = new Date().time
	}
	return cmdq.getCommands()
}


// CommandClass Manufacturer Specific v1

def ccManufacturerSpecificGet()
{
	smartlog(LEVEL_FINE, 'ccManufacturerSpecificGet')
	def cmd = zwave.manufacturerSpecificV1.manufacturerSpecificGet().format()
	return cmd
}

def zwaveEvent(physicalgraph.zwave.commands.manufacturerspecificv1.ManufacturerSpecificReport deviceEvent)
{
	smartlog(zweh, LEVEL_FINE, "handling event manufacturerspecificv1.ManufacturerSpecificReport")
	def msr = String.format("%04X-%04X-%04X", deviceEvent.manufacturerId, deviceEvent.productTypeId, deviceEvent.productId)
	updateDataValue("MSR", msr)

	state.msr = [:]
	state.msr.manufacturerId = deviceEvent.manufacturerId
	state.msr.productTypeId = deviceEvent.productTypeId
	state.msr.productId = deviceEvent.productId


	// Is this a Yale lock?
	if (deviceEvent.manufacturerId == ASSA_ABLOY_MFR_ID ||
			(deviceEvent.manufacturerId == ASSA_ABLOY_OLD_MFR_ID &&
				deviceEvent.productTypeId < ASSA_ABLOY_OLD_MFR_ID_MAX_PROD_TYPE))
	{
		state.msr.manufacturer = "YALE"
	}
	else
	{
		state.msr.manufacturer = "UNKNOWN"
	}

	// which Yale model?
	state.msr.model = YALE_PROD_TYPE_ID_MAP.get(deviceEvent.productTypeId,
			"Unknown Model $deviceEvent.productTypeId")

	state.msr.generation = deviceEvent.productId

	def sc = SmartConfig()
	def resQ = ResponseQueue()

	def niceMsr = "$device.displayName MSR: $msr; Manufacturer: $state.msr.manufacturer; Model: $state.msr.model; Generation: $state.msr.generation"
	if (!state.config)
	{
		resQ.add(getAllConfigParameters(), SHORT_DELAY_MSEC)
	}
	resQ.add(createEvent(description: state.msr.toString(), descriptionText: niceMsr, isStateChange: false))
	return resQ.getWrappedCommands()
}


// CommandClass Protection v2

// NOTE: This CommandClass is deliberately not supported as it's features are all controllable
// within Configuration

// CommandClass TimeParameters v1

def setTimeParameters(Integer year, short month, short day,
			short hour, short min, short sec)
{
	smartlog(ccc, LEVEL_FINE, "ccTimeParametersSet $year $month $day $hour $min $sec")
	smartlog(LEVEL_INFO, "Setting the real-time clock on $device.displayName")
	def cmd = zwave.timeParametersV1.timeParametersSet(
		year:year,
		month:month,
		day:day,
		hourUtc:hour,
		minuteUtc:min,
		secondUtc:sec
	)
	return secure(cmd)
}

// gets time device's current time in UTC
def ccTimeParametersGet()
{
	smartlog(ccc, LEVEL_FINE, 'ccTimeParametersGet')
	return secure(zwave.timeParametersV1.timeParametersGet())
}

/*
Handles the time sent from the device. Checks that it is within a certain range of
"now". If not, it sets a flag value to force setting the device clock at the next poll
interval
*/
def zwaveEvent(physicalgraph.zwave.commands.timeparametersv1.TimeParametersReport deviceEvent)
{
	smartlog(zweh, LEVEL_FINE, "handling event physicalgraph.zwave.commands.timeparametersv1.TimeParametersReport")
	log.debug deviceEvent.format()
	Calendar nowCal = Calendar.getInstance(TZ_UTC) // the current time in UTC
	Calendar lockTimeCal = Calendar.getInstance(TZ_UTC) // Set this to the time the lock reported back
	// NOTE - must subtract 1 from Month as Calendar is zero-based
	lockTimeCal.set(deviceEvent.year, getZeroBasedMonthFromTimeParameters(deviceEvent),
		deviceEvent.day, deviceEvent.hourUtc, deviceEvent.minuteUtc, deviceEvent.secondUtc)
	long clockDeltaMs = nowCal.getTimeInMillis() - lockTimeCal.getTimeInMillis() // How far off is ST Time from the lock's time?
	def dtStr = String.format("%tH:%<tM:%<tS %<tY-%<tB-%<td %<tZ", lockTimeCal)
	smartlog(zweh, LEVEL_INFO, "$device.displayName clock is currently set to $dtStr. Variance is $clockDeltaMs")
	state.clockDeltaMs = clockDeltaMs

	// if unit clock is off by over a certain threshhold, set it
	if (clockDeltaMs.abs() > MAX_CLOCK_DRIFT_MSEC)
	{
		state.forceClockSet = true // force a check/set next poll
	}
	return createEvent(descriptionText: "device clock $dtStr", isStateChange: false)
}

/*
convenience method that sets the device clock to the current time, then gets the
time to ensure that it "took"
*/
private def setDeviceClockToCurrentTimeUtcMacro()
{
	smartlog(LEVEL_FINE, 'func setDeviceClockToCurrentTimeUtcMacro')
	def q = CommandQueue()
	Calendar cal = Calendar.getInstance(TZ_UTC)

	q.add(setTimeParameters(
		cal.get(Calendar.YEAR),
		getOneBasedMonthFromCalendar(cal),
		cal.get(Calendar.DAY_OF_MONTH).shortValue(),
		cal.get(Calendar.HOUR_OF_DAY).shortValue(),
		cal.get(Calendar.MINUTE).shortValue(),
		cal.get(Calendar.SECOND).shortValue()
	), SET_GET_DELAY_MSEC )
	q.add(ccTimeParametersGet())
	return q.getCommands()
}

/**
 * Returns the Calendar.MONTH element + 1 so that it is now one-based which the Yale lock
 * expects and provides in TimeParameters commands
 */
private short getOneBasedMonthFromCalendar(Calendar cal)
{
	smartlog(LEVEL_FINE, 'func getOneBasedMonthFromCalendar')
	return (cal.get(Calendar.MONTH) + 1).shortValue()
}

/**
* Returns the TimeParametersReport.month element - 1 so that it is now zero-based to be compatible
* with the Calendar object
*/
private def getZeroBasedMonthFromTimeParameters(physicalgraph.zwave.commands.timeparametersv1.TimeParametersReport tpr)
{
	smartlog(LEVEL_FINE, 'func getZeroBasedMonthFromTimeParameters')
	return tpr.month - 1
}

private def pollClock()
{
	smartlog(LEVEL_DEBUG, "pollClock: Check clock's time every hour, if forced, or if variance is out of spec or unknown")
	def q = CommandQueue()
	if (state.get("forceClockSet", false))
	{
		smartlog(LEVEL_DEBUG, "Clock set forced.")
		q.add(setDeviceClockToCurrentTimeUtcMacro())
	}
	else if (!state.clockDeltaMs)
	{
		smartlog(LEVEL_DEBUG, "Clock variance unknown.")
		q.add(setDeviceClockToCurrentTimeUtcMacro())
	}
	else if (state.clockDeltaMs.abs() > MAX_CLOCK_DRIFT_MSEC)
	{
		smartlog(LEVEL_DEBUG, "Clock variance is outside of permitted max of +-$MAX_CLOCK_DRIFT_MSEC ms")
		q.add(setDeviceClockToCurrentTimeUtcMacro())
	}
	else if (state.get("forceClockGet", false))
	{
		smartlog(LEVEL_DEBUG, "Clock check forced")
		q.add(ccTimeParametersGet())
	}
	else if (hasTimePassed(state.lastUnitClockGetTime, GET_TIME_EVERY_N_HOURS))
	{
		smartlog(LEVEL_DEBUG, "Clock periodic check is due.")
		q.add(ccTimeParametersGet())
	}
	state.forceClockSet = false
	state.forceClockGet = false
	return q.getCommands()
}


// CommandClass Usercode V1

def ccUsersNumberGet()
{
	smartlog(ccc, LEVEL_FINE, 'ccUsersNumberGet')
	return secure(zwave.userCodeV1.usersNumberGet())
}

def zwaveEvent(physicalgraph.zwave.commands.usercodev1.UsersNumberReport deviceEvent)
{
	smartlog(zweh, LEVEL_FINE, "handling event usercodev1.UsersNumberReport")
	def result = []
	log.debug(deviceEvent)
	smartlog(zweh, LEVEL_INFO, "Device $device reports $deviceEvent.supportedUsers supported users")
	state.maxSlotNum = deviceEvent.supportedUsers.shortValue() // record in the state
	result << createEvent(name: usersNumber, value: deviceEvent.supportedUsers.shortValue())
	if (state.pollUserCodes && state.pollUserCodes.current == 1)
	{
		smartlog(zweh, LEVEL_INFO, "Starting reloadAllCodes with slot $currentReloadSlot of $state.maxSlotNum")
		result << response(resumeUserCodePoll())
	}
	return result
}

def ccUserCodeSet(Short slotNumber, code)
{
	smartlog(ccc, LEVEL_FINE, "ccUserCodeSet $slotNumber $code")

	def argMap = [userIdentifier: slotNumber]
	if (code)
	{
		smartlog(LEVEL_DEBUG, "setting slot $slotNumber with code $code")
		argMap.user = code
		argMap.userIdStatus = UserCodeSet.USER_ID_STATUS_OCCUPIED
	}
	else
	{
		smartlog(LEVEL_DEBUG, "marking slot $slotNumber as available")
		argMap.userIdStatus = UserCodeSet.USER_ID_STATUS_AVAILABLE_NOT_SET
	}

	return secure(zwave.userCodeV1.userCodeSet(argMap))
}

def ccUserCodeGet(Short slotNumber)
{
	smartlog(ccc, LEVEL_FINE, "ccUserCodeGet $slotNumber")
	return secure(zwave.userCodeV1.userCodeGet(userIdentifier:slotNumber))
}

def zwaveEvent(physicalgraph.zwave.commands.usercodev1.UserCodeReport deviceEvent)
{
	smartlog(zweh, LEVEL_FINE, "handling event usercodev1.UserCodeReport for slot $deviceEvent.userIdentifier with code $deviceEvent.code")
	def result = []
	Short slotNum = deviceEvent.userIdentifier
	String codeStr = deviceEvent.code
	def codeSlot = state.codeDb.get(slotNum, [:])
	smartlog(zweh, LEVEL_DEBUG, "checking $slotNum and $codeStr")

	def eventMap = [ name: "UserCodeReport", value: slotNum]
	// handle the only two valid states for the Yale lock
	if (deviceEvent.userIdStatus == UserCodeReport.USER_ID_STATUS_OCCUPIED)
	{
		// code has been set into the slot number identified by deviceEvent.userIdentifier
		smartlog(zweh, LEVEL_INFO, "slot $slotNum is reported as OCCUPIED by device")
		commitCodeDbSlot(slotNum, codeStr) // commit it into the local code DB
		eventMap.data = [ status: 'OCCUPIED', code: codeStr ]
		eventMap.descriptionText = "$device.displayName slot $slotNum is set with user code '$codeStr'"
		eventMap.displayed = (slotNum != state?.currentReloadSlot && slotNum != state?.pollUserCodes?.current)
		eventMap.isStateChange = (codeStr != codeSlot.get("code", null))
		result << createEvent(eventMap)
	}
	else
	{
		// code slot is empty - either is already was empty or its value has been deleted
		smartlog(zweh, LEVEL_INFO, "slot $slotNum is reported as AVAILABLE by device")
		removeCodeDbSlot(slotNum) // remove the code from the local code db
		eventMap.data = [ status: 'AVAILABLE', code: "" ]
		if (state.codeDb.slotNum) {
			eventMap.descriptionText = "$device.displayName code slot $slotNum was deleted"
		} else {
			eventMap.descriptionText = "$device.displayName code slot $slotNum is not set"
		}
		eventMap.displayed = (slotNum != state?.currentReloadSlot && slotNum != state?.pollUserCodes?.current)
		eventMap.isStateChange = codeSlot.code as Boolean
		result << createEvent(eventMap)
	}

	// If the slot was part of a current ongoing poll...
	def pollCmd = progressUserCodePoll(slotNum)
	if (pollCmd)
	{
		result << response(pollCmd) // send command to get the next slot right away.
	}

	smartlog(zweh, LEVEL_DEBUG, "commands as a result of this UserCodeReport: ${result.inspect()}")
	return result
}

private def setCodeMacro(Short slotNum, code)
{
	String codeStr
	smartlog(LEVEL_FINE, "func setCodeMacro for slot $slotNum with code $code")
	if (validateSlotNumber(slotNum))
	{
		if (code instanceof String)
		{
			// code was passed as a string - convert to List<Short>
			codeStr = code // store the code as a string
			if (!validateCode(code)) return // need better error handling - invalid code
			code = codeStrToShortList(code)
		}
		else
		{
			// code was (hopefully) passed as a List<Short>
			codeStr = codeShortListToStr(code)
			if (!validateCode(codeStr))
			{
				smartlog(LEVEL_WARN, "code '$codeStr' is invalid. Better do something clever to handle this")
				return // need better error handling - invalid code
			}
		}
	}

	// check for this codestr as a value in codeDb already
	setCodeDbPendingSlot(slotNum, codeStr)
	// set, then get the code slot so that the state is updated.
	def cmdq = CommandQueue()
	cmdq.add(ccUserCodeSet(slotNum, code), SET_GET_DELAY_MSEC)
	cmdq.add(ccUserCodeGet(slotNum))
	return cmdq.getCommands()
}

// macro method involving local code db and checking that the slot is valid
private def getCodeMacro(Short slotNumber)
{
	smartlog(LEVEL_FINE, "func getCodeMacro $slotNumber")
	if (validateSlotNumber(slotNumber))
	{
		// get the current slot entry from the db and pend it or make a placeholder entry
		if (state.codeDb.containsKey(slotNumber))
		{
			state.pendingCodeDb[slotNumber].putAll(state.codeDb[slotNumber])
		}
		return ccUserCodeGet(slotNumber)
	}
}

private def deleteCodeMacro(Short slotNumber)
{
	smartlog(LEVEL_FINE, "func deleteCodeMacro $slotNumber")
	smartlog(LEVEL_DEBUG, "deleting code $slotNumber")
	if (validateSlotNumber(slotNumber))
	{
		// mark the slot as available is how to delete it
		def cmdq = CommandQueue
		cmdq.add(ccUserCodeSet(slotNumber, null), SET_GET_DELAY_MSEC)
		cmdq.add(ccUserCodeGet(slotNumber))
		return cmdq.getCommands()
	}
}

// Initiate or continue polling
private def pollUserCodes()
{
	smartlog(LEVEL_FINE, 'func polluserCodes')
	if (!state.pollUserCodes)
	{
		smartlog(LEVEL_INFO, "Starting poll of user codes for $device")
		state.pollUserCodes = [:]
		state.pollUserCodes.current = 1.shortValue()
		state.pollUserCodes.timestamp = 0
		// send an event indicating that polling is beginning
		sendEvent([name="GetAllUserCodesFromDevice", value="Started", isStateChange=true, descriptionText="Fetching all user codes from $device has begun"])

	}
	else
	{
		smartlog(LEVEL_INFO, "Continuing poll of user codes on $device with slot ${state.pollUserCodes.current} of $state.maxSlotNum")
	}
	// This will be wrapped in a HubAction, if needed, by the calling entity
	def cmd
	if (state.maxSlotNum)
	{
		cmd = getCodeMacro(state.pollUserCodes.current)
		/*
		* Let's mark the timestamp in the pollUserCodes map in case the full poll of user code
		* slots gets derailed. We can resume from poll(), but only if sufficient time elapses
		*/
		Calendar cal = Calendar.getInstance(TZ_UTC)
		state.pollUserCodes.resumeTimestamp = cal.getTimeInMillis()
	}
	else
	{
		// We don't have the
		smartlog(LEVEL_DEBUG, "polling of user codes will start once we know how many code slots $device supports")
		cmd = ccUsersNumberGet()
	}
	log.debug "pollUserCodes returning command: " + $cmd
	return cmd
}

//Checks if there is any polling-related action to take for UserCodeReport
private def progressUserCodePoll(Short slotNumber)
{
	smartlog(LEVEL_FINE, "func progressUserCodePoll $slotNumber")
	def pollNextStep = null
	// is this related to the currently active poll?
	if (isUserCodeSlotPollRelated(slotNumber))
	{
		if ((state.pollUserCodes.current + 1).shortValue() > state.maxSlotNum)
		{
			// We have finished polling all user code slots. Clean up.
			smartlog(LEVEL_INFO, "Polling has finished with slot $state.pollUserCodes.current")
			state.clearStateEntry("pollUserCodes")
			// send an event indicating that polling has finished
			sendEvent(name="GetAllUserCodesFromDevice", value="Complete", isStateChange=true, descriptionText="Fetching all user codes from $device has completed")
		}
		else
		{
			// Continue with the next slot
			state.pollUserCodes.current = (state.pollUserCodes.current + 1).shortValue() // ++ and += don't work here
			smartlog(LEVEL_DEBUG, "Polling progresses to next slot: $state.pollUserCodes.current")
			assert state.pollUserCodes.current > slotNumber
			pollNextStep = pollUserCodes()
		}
	}
	else
	{
		// Code report for slotNumber is not related to poll
		// Do nothing.
	}
	smartlog(LEVEL_DEBUG, "Next step for polling is $pollNextStep")
	return pollNextStep
}

private def isUserCodeSlotPollRelated(Short slotNumber)
{
	smartlog(LEVEL_FINE, "func isUserCodeSlotPollRelated $slotNumber")
	if (slotNumber == state?.pollUserCodes?.current)
	{
		smartlog(LEVEL_DEBUG, "Yup, $slotNumber is a slot we polled for")
		return true
	}
	return false
}

// Abandon user code poll currently in progress
private def abortUserCodePoll()
{
	smartlog(LEVEL_FINE, 'func abortUserCodePoll')
	if (state.pollUserCodes)
	{
		smartlog(LEVEL_INFO, "Aborting in-progress user code polling on $device")
		clearStateEntry('pollUserCodes')
		// Send event indicating that user code polling has been aborted
		sendEvent(name="GetAllUserCodesFromDevice", value="Aborted", isStateChange=true,
			descriptionText="Fetching all user codes from $device has been aborted",
			displayed=true)
	}
	else
	{
		smartlog(LEVEL_DEBUG, "No in-progress user code polling to abort on $device")
	}
}

// Resume a user code polling that got derailed
private def resumeUserCodePoll()
{
	smartlog(LEVEL_FINE, 'func resumeUserCodePoll - check if theres a poll job to pick up again')
	def pollNextStep = null
	if (state.pollUserCodes && state.pollUserCodes.resumeTimestamp && state.pollUserCodes.current)
	{
		Calendar cal = Calendar.getInstance(TZ_UTC)
		def nowTimestamp = cal.getTimeInMillis()
		// was last poll more then 2 min ago?
		if ((nowTimestamp - state.pollUserCodes.resumeTimestamp) >= (MSEC_PER_HOUR/30))
		{
			smartlog(LEVEL_DEBUG, "Resume polling of user codes from $device with slot number $state.pollUserCodes.current")
			pollNextStep = pollUserCodes()
			state.pollUserCodes.resumeTimestamp = nowTimestamp
		}
		else
		{
			smartlog(LEVEL_DEBUG, "There is a poll job, but last userCodeGet was within the past 2 min, so we still consider it active")
		}
	}
	else
	{
		smartlog(LEVEL_DEBUG, "No polling to resume or not enough time elapsed since previous poll step to consider polling stalled")
	}
	return pollNextStep
}

// convert string version of code to a list of Short ASCII values
private def codeStrToShortList(String codeStr)
{
	smartlog(LEVEL_FINE, "func codeStrToShortList $codeStr")
	return codeStr.toList().findResults { if(it > ' ' && it != ',' && it != '-') it.toCharacter() as Short }
}

// convert code as list ASCII Short ints to a string
private def codeShortListToStr(code)
{
	smartlog(LEVEL_FINE	, "func codeShortListToStr $code")
	return code.collect{ it as Character }.join()
}

private boolean validateSlotNumber(int slotNumber)
{
	smartlog(LEVEL_FINE	, "func validateSlotNumber $slotNumber")
	// slots run from 1-249. Slot 0 is master code and cannot be modified by RF
	if (state.maxSlotNum && MIN_SLOT_NUM <= slotNumber && slotNumber <= state.maxSlotNum)
	{
		return true
	}
	else if (state.maxSlotNum)
	{
		smartlog(LEVEL_WARN, "$slotNumber is not a valid slot number (${MIN_SLOT_NUM}-${state.maxSlotNum})")
	}
	else
	{
		smartlog(LEVEL_WARN, "Maximum slot number for $device isn't known. Refresh the device to get this information.")
	}
	return false
}

private boolean validateCode(codeStr)
{
	smartlog(LEVEL_FINE, "func validateCode $codeStr")
	// code must be 4-8 digits
	return codeStr ==~ /\d{4,8}/
}


// CommandClass Version v1

def ccVersionGet()
{
	smartlog(ccc, LEVEL_FINE, 'ccVersionGet')
	return zwave.versionV1.versionGet().format()
}

def zwaveEvent(physicalgraph.zwave.commands.versionv1.VersionReport deviceEvent)
{
	smartlog(zweh, LEVEL_FINE, "handling event versionv1.VersionReport")
	def fw = "${deviceEvent.applicationVersion}.${deviceEvent.applicationSubVersion}"
	updateDataValue("fw", fw)
	def text = "$device.displayName: lock firmware version: {deviceEvent.applicationVersion}, Z-Wave firmware version: ${deviceEvent.applicationSubVersion}, Z-Wave lib type: ${deviceEvent.zWaveLibraryType}, Z-Wave version: ${deviceEvent.zWaveProtocolVersion}.${deviceEvent.zWaveProtocolSubVersion}"

	state.version = [:]
	state.version.lockFirmware = deviceEvent.applicationVersion
	state.version.zwaveFirmware = deviceEvent.applicationSubVersion
	state.version.zwaveLibraryType = deviceEvent.zWaveLibraryType
	state.version.zwaveProtocol = deviceEvent.zWaveProtocolVersion
	state.version.zwaveProtocolSub = deviceEvent.zWaveProtocolSubVersion

	return createEvent(description: deviceEvent.toString(), descriptionText: text, isStateChange: false)
}

def ccVersionGetCommandClass(Short ccId)
{
	smartlog(ccc, LEVEL_FINE, "ccVersionGetCommandClass $ccId")
	def cmd = zwave.versionV1.versionCommandClassGet(requestedCommandClass: ccId).format()
	return cmd
}

def ccVersionGetCommandClass(Integer ccId)
{
	return ccVersionGetCommandClass(ccId as Short)
}

/*
NOTE: The device may lie about the command class version it support. Or maybe it's a bug in ST's
Z-wave utility library. UPDATE: it's a bug
*/
def zwaveEvent(physicalgraph.zwave.commands.versionv1.VersionCommandClassReport deviceEvent)
{
	smartlog(zweh, LEVEL_FINE, "handling event versionv1.VersionCommandClassReport")
	log.debug deviceEvent
	def hexCCNumber = formatOctetAsHex(deviceEvent.requestedCommandClass)
	smartlog(zweh, LEVEL_DEBUG, "CommandClass $hexCCNumber, Version ${deviceEvent.commandClassVersion} - $hexCCNumber: ${deviceEvent.commandClassVersion}")
}

private String formatOctetAsHex(short octet)
{
	smartlog(LEVEL_FINE, "func formatOctetAsHex $octet")
	return sprintf('%#x', octet)
}


//codeDb assist methods
private Map buildDbEntry(int slotNumber, String codeStr)
{
	smartlog(codedb, LEVEL_FINE, "buildDbEntry $slotNumber $codeStr")
	return [code: codeStr]
}

private void setCodeDbPendingSlot(int slotNumber, String code)
{
	smartlog(codedb, LEVEL_FINE, "setCodeDbPendingSlot $slotNumber $code")
	// when slot name is not provided, use slot number as slot name
	state.pendingCodeDb[slotNumber] = buildDbEntry(slotNumber, codeStr)
}

// called when handling a UserCodeReport
private void commitCodeDbSlot(int slotNumber, String codeStr)
{
	smartlog(codedb, LEVEL_FINE, "commitCodeDbSlot $slotNumber $codeStr")
	// get the existing entry, or create a slot named for its number if there isn't one
	def newEntry = state.codeDb.get(slotNumber, [name: slotNumber])

	// get the pending entry, if one, or use a blank map.
	def pendingEntry = state.pendingCodeDb.get(slotNumber, [:])
	if (pendingEntry.code && pendingEntry.code != codeStr)
	{
		smartlog(codedb, LEVEL_WARN, "pending code $pendingEntry.code doesn't match code in the UserCodeReport, $codeStr")
	}
	pendingEntry.code = codeStr

	// layer pending entry onto any existing info and insert it
	newEntry.putAll(pendingEntry)
	smartlog(codedb, LEVEL_DEBUG, "setting codeDb slot $slotNumber to ${newEntry.inspect()}")
	state.codeDb[slotNumber] = newEntry

	// remove pending entry if necessary
	if (state.pendingCodeDb[slotNumber]) state.pendingCodeDb.remove(slotNumber)
}

private void removeCodeDbSlot(int slotNumber)
{
	smartlog(codedb, LEVEL_FINE, "removeCodeDbSlot $slotNumber")
	if (state.codeDb[slotNumber]) state.codeDb.remove(slotNumber)
	if (state.pendingCodeDb[slotNumber]) state.pendingCodeDb.remove(slotNumber)
}

private def findFirstEmptyCodeDbSlot()
{
	smartlog(codedb, LEVEL_FINE, 'findFirstEmptyCodeDbSlot')
	int emptySlotNum = -1
	for (int currSlot = MIN_SLOT_NUM; currSlot <= state.maxSlotNum; currSlot++)
	{
		if (!state.codeDb[currSlot])
		{
			emptySlotNum = currSlot
			break
		}
	}
	if (emptySlotNum >= MIN_SLOT_NUM)
	{
		return emptySlotNum
	}
	// There are no more empty slots
	return null
}

private void clearCodeDb()
{
	smartlog(codedb, LEVEL_FINE, 'clearCodeDb')
	initialize()
	state.codeDb.clear()
	state.pendingCodeDb.clear()
}


// other helper methods

// deletes any top level state entry by key
private void clearStateEntry(Object entryKey)
{
	smartlog(LEVEL_FINE, "func clearStateEntry $entryKey")
	if (state.containsKey(entryKey))
	{
		smartlog(codedb, LEVEL_INFO, "Removing state db entry $entryKey")
		state.remove(entryKey)
		initialize() // in case we removed something that needs to be present
	}
	else
	{
		smartlog(codedb, LEVEL_WARN, "No state db entry with key $entryKey to remove")
	}
}

private String delay()
{
	smartlog(LEVEL_FINE, 'func delay (default duration)')
	return delay(DEFAULT_DELAY_MSEC)
}

private String delay(int msecToDelay)
{
	smartlog(LEVEL_FINE, "func delay $msecToDelay milliseconds")
	return "delay $msecToDelay"
}

private long timestampToMillis(timestamp)
{
	smartlog(LEVEL_FINE, "finc timestampToMillis $timestamp")
	if (!(timestamp instanceof Number))
	{
		if (timestamp instanceof Date)
		{
			timestamp = timestamp.time
		}
		else if ((timestamp instanceof String) && timestamp.isNumber())
		{
			timestamp = timestamp.toLong()
		}
		else timestamp = 0
	}
	return timestamp
}

private Boolean hasTimePassed(timestamp, hours=0, minutes=0, seconds=0)
{
	smartlog(LEVEL_FINE, "hasTimePassed since $timestamp, $hours:$minutes:$seconds")
	timestamp = timestampToMillis(timestamp)
	long millisPast = (hours * MSEC_PER_HOUR) + (minutes * 60000) + (seconds * 1000)
	return (new Date().time - timestamp) > millisPast
}

// VV  ----  ----  ClosureClasses BELOW THIS LINE  ----  ----  VV

// BEGIN SmartLog
@Field final String LEVEL_NONE = null
@Field final String LEVEL_ERROR = 'error'
@Field final String LEVEL_WARN = 'warn'
@Field final String LEVEL_INFO = 'info'
@Field final String LEVEL_DEBUG = 'debug'
@Field final String LEVEL_TRACE = 'trace'
@Field final String LEVEL_FINE = 'FINEtrace'

@Field final List SMARTLOG_LEVELS = [LEVEL_NONE, LEVEL_ERROR, LEVEL_WARN, LEVEL_INFO, LEVEL_DEBUG, LEVEL_TRACE, LEVEL_FINE]
@Field final String SMARTLOG_DEFAULT_LEVEL = LEVEL_FINE

void smartlog(String scope, String level, String msg)
{
	initSmartlog()
	if (!level || !SMARTLOG_LEVELS.contains(level))
	{
		// set a default level
		level = SMARTLOG_DEFAULT_LEVEL
	}

	String scopeActiveLevel = getSmartlogLevel(scope)
	if (scopeActiveLevel == LEVEL_NONE) return // logging is off
	if (SMARTLOG_LEVELS.indexOf(scopeActiveLevel) >= SMARTLOG_LEVELS.indexOf(level))
	{
		String loglevel = level.replaceAll('[^a-z]', '')
		if (scope) msg = "$scope: $msg"
		log."${loglevel}"(msg)
	}
}

void smartlog(String level, String msg)
{
	smartlog(null, level, msg)
}

void smartlog(String msg)
{
	smartlog(null, null, msg)
}

void initSmartlog()
{
	if (state?.smartlog == null)
	{
		state.smartlog = [:]
		setSmartlogLevel()
	}
}

void resetSmartlog()
{
	state.smartlog = null
	initSmartlog()
}

void setSmartlogLevel(String scope, String level=SMARTLOG_DEFAULT_LEVEL)
{
	if (state?.smartlog == null) state.smartlog = [:]
	if (SMARTLOG_LEVELS.contains(level))
	{
		state.smartlog.put(scope,  level)
	}
}

void setSmartlogLevel()
{
	setSmartlogLevel(null)
}

void setSmartlogOverrideLevel(String level)
{
	if (SMARTLOG_LEVELS.contains(level))
	{
		state.smartlog = [:]
		setSmartlogLevel(level)
	}
}

String getSmartlogLevel(String scope=null)
{
	initSmartlog()
	return state.smartlog.get(scope, state.smartlog.get(null, SMARTLOG_DEFAULT_LEVEL))
}
// END SmartLog

/*
Low-rent "class" that is a queue for z-wave commands. Commands may be added one at a time, or as a
list. Default delays are automatically added between commands unless a specific duration delay is
specified after a command. Default delays are not added after another delay command or if the
command itself is a delay command. The list of commands may be extracted at any time.
*/
def CommandQueue()
{
	def commandQ = [:] // the "object" map
	commandQ.type = 'CommandQueue'
	commandQ.entryList = [] // list to hold the commands

	smartlog("${commandQ.type} construction", LEVEL_FINE, "constructing ${commandQ.type} instance")

	/**
	* Add a command or list of commands to the end of the queue
	*
	* @param cmd single Text command to add or list of string commands to add
	* @param delay (optional) custom delay in milliseconds to add after each command
	*/
	commandQ.add =
	{
		entry, Number delayMs = null ->
		smartlog(commandQ.type, LEVEL_FINE, "add '$entry' $delayMs")
		String entryStr =  "entry: $entry ; delayMs: $delayMs"
		if (!entry)
		{
			smartlog(commandQ.type, LEVEL_DEBUG, "entry evaluates to false, discarding. ($entryStr)")
		}
		else if (entry instanceof List)
		{
			smartlog(commandQ.type, LEVEL_DEBUG, "entry is a list, adding each entry. ($entryStr)")
			// if custom delay is specified, each command will have this delay.
			entry.each { oneEntry -> commandQ.add(oneEntry, delayMs) }
			smartlog(commandQ.type, LEVEL_DEBUG, "finished adding list.")
		}
		else if (commandQ.isCommand(entry))
		{
			smartlog(commandQ.type, LEVEL_DEBUG, "entry is a command, adding. ($entryStr)")
			commandQ.__addCommand(entry, delayMs)
		}
		else if (commandQ.isDelay(entry))
		{
			smartlog(commandQ.type, LEVEL_DEBUG, "entry is a delay, adding. ($entryStr)")
			commandQ.__addDelay(entry)
		}
		else
		{
			smartlog(commandQ.type, LEVEL_WARN, "entry parameter to add() was not a command or delay. discarding. ($entryStr)")
		}
	}

	commandQ.__addCommand =
	{
		String entry, Number delayMs=null ->
		smartlog(commandQ.type, LEVEL_FINE, "__addCommand '$entry' $delayMs")
		if (commandQ.isCommand(entry))
		{
			// first, add a delay if the previous entry was a command and this command isn't a delay
			if  (commandQ.qSize() > 0 && !commandQ.isDelay(entry) && commandQ.isLastEntryACommand())
			{
				commandQ.__addDelay(commandQ.formatDelayCmd(DEFAULT_DELAY_MSEC)) // always the default delay
			}

			commandQ.entryList << entry // now, add the command to the queue

			// Add a delay afterwards if a custom delay is specified
			if (delayMs)
			{
				commandQ.__addDelay(commandQ.formatDelayCmd(delayMs))
			}
		}
		else
		{
			smartlog(commandQ.type, LEVEL_WARN, "entry parameter to __addCommand is not a command: ${entry}. discarding.")
		}
	}

	/**
	* Add a delay command to the end of the queue. If no delay is specified, or it's not an integer,
	* a default delay will be added.
	*
	* @param delay The delay duration in milliseconds (optional)
	*/
	commandQ.__addDelay =
	{
		String entry ->
		smartlog(commandQ.type, LEVEL_FINE, "__addDelay '$entry'")
		if (entry && commandQ.isDelay(entry))
		{
			commandQ.entryList << entry
		}
		else
		{
			smartlog(commandQ.type, LEVEL_WARN, "entry parameter to __addDelay is not a delay command: ${entry}. discarding.")
		}
	}

	/**
	* Add a single command to the beginning of the queue. If cmd is not a text string, nothing
	* will be done. A delay command will be added after the cmd if cmd is not, itself, a delay
	* command. The durtation of this delay will be the default duration unless the optional delay
	* parameter is provided.
	*
	* @param cmd	 single text command to add to the front of the queue
	* @param delay (optional) delay in milliseconds to add after the command, otherwise the default
	* delay will be added if the cmd itself is not a delay
	*/
	commandQ.prepend =
	{
		// Can only prepend a command
		entry, Number delayMs=null ->
		smartlog(commandQ.type, LEVEL_FINE, "prepend '$entry' $delayMs")
		String entryStr =  "entry: $entry ; delayMs: $delayMs"
		if (commandQ.isCommand(entry))
		{
			commandQ.__prependCommand(entry, delayMs)
		}
		else
		{
			smartlog(commandQ.type, LEVEL_WARN, "entry parameter to prepend is not a command: ${entry}. discarding.")
		}
	}

	commandQ.__prependCommand =
	{
		cmd, Number delayMs=null ->
		smartlog(commandQ.type, LEVEL_FINE, "__prependCommand '$cmd' $delayMs")
		if (commandQ.isCommand(cmd))
		{
			// first, prepend a delay to the front of the queue if there are already commands in it
			if (commandQ.qSize() > 0)
			{
				commandQ.__prependDelay(commandQ.formatDelayCmd(delayMs))
			}
			commandQ.entryList.add(0, cmd)
		}
		else
		{
			smartlog(commandQ.type, LEVEL_WARN, "parameter to __prependCommand is not a command: ${cmd}. discarding.")
		}

	}

	/**
	* Add a delay command to the front of the queue. If no delay is specified, or it's not an integer,
	* a default delay will be added.
	*
	* @param delay The delay duration in milliseconds (optional)
	*/
	commandQ.__prependDelay =
	{
		String entry ->
		smartlog(commandQ.type, LEVEL_FINE, "__prependDelay $entry")
		if (commandQ.isDelay(entry))
		{
			commandQ.entryList.add(0, entry)
		}
	}

	commandQ.isCommand =
	{
		def entry ->
		smartlog(commandQ.type, LEVEL_FINE, "isCommand $entry")
		return entry instanceof String && ! entry.startsWith('delay')
	}

	commandQ.isDelay =
	{
		def entry ->
		smartlog(commandQ.type, LEVEL_FINE, "isDelay $entry")
		return entry instanceof String && entry.startsWith('delay')
	}

	/**
	* Checks if the last entry added to the queue is a Command or not
	* @return true if the last entry on the queue is a non-delay string, false if not or the queue is empty
	*/
	commandQ.isLastEntryACommand =
	{
		smartlog(commandQ.type, LEVEL_FINE, 'isLastEntryACommand')
		if ( commandQ.entryList && commandQ.isCommand(commandQ.entryList.last()) )
		{
			return true
		}
		return false
	}

	/**
	* formats the delay command. Behavior if delayArgVal is null or non-Integer depends on
	* noDefaultDelay. If True, returns null,
	* @param delay	The delay duration in milliseconds.
	* @param noDefaultDelay If true, and delay parameter is null or non-integer, do not generate a delay command string
	* @return delay command or null
	*/
	commandQ.formatDelayCmd =
	{
		Number delayMs=null ->
		smartlog(commandQ.type, LEVEL_FINE, "formatDelayCmd $delayMs")
		if (delayMs)
		{
			Integer delayMsInt = delayMs.intValue()
			return "delay $delayMsInt"
		}
		else
		{
			return "delay $DEFAULT_DELAY_MSEC"
		}
		return null
	}

	/**
	* returns the current size of the command queue, including automatically generated delay commands
	* @return the number of commands in the command queue
	*/
	commandQ.qSize =
	{
		smartlog(commandQ.type, LEVEL_FINE, 'qSize')
		return commandQ.entryList.size()
	}

	/**
	* returns the command queue
	* @return list of commands in the command queue
	*/
	// get the list of commands
	commandQ.getCommands =
	{
		smartlog(commandQ.type, LEVEL_FINE, 'getCommands')
		return commandQ.entryList
	}

	return commandQ
}

/**
* a CommandQueue for parse() and its zwave event handler methods
* collects commands and delays plus event Maps
* upon requesting the compiled list, commands and delays are wrapped in a HubAction.
* This is ultimately returned from parse() at which the execution of the commands commences
*/
def ResponseQueue()
{
	def responseQ = CommandQueue()
	responseQ.type = 'ResponseQueue'

	smartlog("${responseQ.type} construction", LEVEL_FINE, "constructing ${responseQ.type} instance")

	/**
	* Add a command or list of commands to the end of the queue
	*
	* @param cmd single Text command to add or list of string commands to add
	* @param delay (optional) custom delay in milliseconds to add after each command
	*/
	responseQ.add =
	{
		entry, Number delayMs=null ->
		smartlog(responseQ.type, LEVEL_FINE, "add '$entry' $delayMs")
		String entryStr =  "entry: $entry ; delayMs: $delayMs"
		if (!entry)
		{
			smartlog(responseQ.type, LEVEL_DEBUG, "entry evaluates to false, discarding. ($entryStr)")
		}
		else if (entry instanceof List)
		{
			smartlog(responseQ.type, LEVEL_DEBUG, "entry is a list, adding each entry. ($entryStr)")
			// if custom delay is specified, each command will have this delay.
			entry.each { oneEntry -> responseQ.add(oneEntry, delayMs) }
			smartlog(responseQ.type, LEVEL_DEBUG, "finished adding list.")
		}
		else if (responseQ.isEvent(entry))
		{
			smartlog(responseQ.type, LEVEL_DEBUG, "entry is an event, adding. ($entryStr)")
			responseQ.__addEvent(entry)
		}
		else if (responseQ.isCommand(entry))
		{
			smartlog(responseQ.type, LEVEL_DEBUG, "entry is a command, adding. ($entryStr)")
			responseQ.__addCommand(entry, delayMs)
		}
		else if (responseQ.isDelay(entry))
		{
			smartlog(responseQ.type, LEVEL_DEBUG, "entry is a delay, adding. ($entryStr)")
			responseQ.__addDelay(entry)
		}
		else
		{
			smartlog(responseQ.type, LEVEL_WARN, "entry parameter to add() was not a command, delay or event. discarding. ($entryStr)")
		}
	}

	responseQ.prepend =
	{
		// Can only prepend a command
		// prepend is a little less "smart" regarding delays
		entry, Number delayMs=null ->
		smartlog(responseQ.type, LEVEL_FINE, "prepend '$entry' $delayMs")
		String entryStr =  "entry: $entry ; delayMs: $delayMs"
		if (responseQ.isEvent(entry))
		{
			smartlog(responseQ.type, LEVEL_DEBUG, "entry is an event, prepending. ($entryStr)")
			responseQ.__prependEvent(entry)
		}
		else if (responseQ.isCommand(entry))
		{
			smartlog(responseQ.type, LEVEL_DEBUG, "entry is a command, prepending. ($entryStr)")
			responseQ.__prependCommand(entry, delayMs)
		}
		else
		{
			smartlog(responseQ.type, LEVEL_WARN, "entry parameter to prepend is not a command nor an event: ${entryStr}. discarding.")
		}
	}

	responseQ.__addEvent =
	{
		Map event ->
		smartlog(responseQ.type, LEVEL_FINE, "__addEvent $event")
		if (responseQ.isEvent(event))
		{
			// wrapping the event simply fills out other members of the event.
			// wrapping an already wrapped event is safe.
			responseQ.entryList << createEvent(event)
		}
		else
		{
			smartlog(responseQ.type, LEVEL_WARN, "parameter to __addEvent is not an event: ${event}. discarding.")
		}
	}

	responseQ.__prependEvent =
	{
		Map event ->
		smartlog(responseQ.type, LEVEL_FINE, "__prependEvent $event")
		if (responseQ.isEvent(event))
		{
			// wrapping the event simply fills out other members of the event.
			// wrapping an already wrapped event is safe.
			responseQ.entryList.add(0, createEvent(event))
		}
		else
		{
			smartlog(responseQ.type, LEVEL_WARN, "parameter to __prependEvent is not an event: ${event}. discarding.")
		}
	}

	responseQ.isEvent =
	{
		entry ->
		smartlog(responseQ.type, LEVEL_FINE, "isEvent $entry")
		return entry instanceof Map
	}

	responseQ.isResponse =
	{
		entry ->
		smartlog(responseQ.type, LEVEL_FINE, "isResponse $entry")
		return entry instanceof HubAction
	}

	responseQ.wrapResponse =
	{
		smartlog(responseQ.type, LEVEL_FINE, "wrapResponse")
		return (responseQ.isCommand(it) || responseQ.isDelay(it)) ? response(it.toString()) : it
	}

	/**
	* return a copy of entryList where each command or a delay entry is wrapped in a HubAction
	*/
	responseQ.getWrappedCommands =
	{
		smartlog(responseQ.type, LEVEL_FINE, "getWrappedCommands")
		List wrappedEntryList = []
		responseQ.entryList.each { wrappedEntryList << responseQ.wrapResponse(it) }
		return wrappedEntryList
	}

	return responseQ
}


def SmartConfig()
{
	Map scObj = [:]
	scObj.type = 'SmartConfig'
	scObj.version = '20150320A'
	scObj.deviceModelId = state?.msr?.productTypeId?:''
	scObj.PARAMETER_METADATA = [:]

	String constructScope = "${scObj.type} construction"
	smartlog(constructScope, LEVEL_FINE, "Constructing ${scObj.type} instance")

	String CHOICE = 'choice'
	String RANGE = 'range'

	if (!scObj.deviceModelId)
	{
		smartlog(constructScope, LEVEL_INFO, 'device model is unknown. initializing general config map.')
	}

	def deepMerge
	deepMerge =
	{
		Map[] sources ->
		smartlog(constructScope, LEVEL_FINE, 'deepMerge. two nested Maps. recursively.')
		if (sources.length == 0) return [:]
		if (sources.length == 1) return sources[0]
		sources.inject([:])
		{
			result, source ->
			source.each
			{
				k, v ->
				// we don't merge the choicemap - replace only
				result[k] = (result[k] instanceof Map && k != 'choices') ? deepMerge(result[k], v) : v
			}
			return result
		}
	}

	def build =
	{
		smartlog(constructScope, LEVEL_FINE, 'entering build')
		// slice product groups
		def touchscreenModels = []
		YALE_PROD_TYPE_ID_MAP.findAll { it.value.toLowerCase().contains('touchscreen')}.each {touchscreenModels << it ?.key}

		def pushbuttonModels = []
		YALE_PROD_TYPE_ID_MAP.findAll { it.value.toLowerCase().contains('push button')}.each {pushbuttonModels << it ?.key}

		def deadboltModels = []
		YALE_PROD_TYPE_ID_MAP.findAll { it.value.toLowerCase().contains('deadbolt')}.each {deadboltModels << it ?.key}

		def leverModels = []
		YALE_PROD_TYPE_ID_MAP.findAll { it.value.toLowerCase().contains('lever')}.each {leverModels << it ?.key}

		Map PARAMETERS = [
			audio_mode: [
				displayName: 'Audio',
				deviceAttributeName: 'cfgAudio',
				num: 1,
				type: CHOICE,
				choices: [OFF: 1, ON: 3], // model variances may apply
				default_: 3,
				size: CONFIG_VALUE_BYTE_SIZE
			],
			auto_relock: [
				displayName: 'Auto relock',
				deviceAttributeName: 'cfgAutoRelock',
				num: 2,
				type: CHOICE,
				choices: [OFF: 0x00, ON: 0xFF],
				default_: 0x00 // default differs by model
			],
			auto_relock_delay: [
				displayName: 'Auto relock delay',
				deviceAttributeName: 'cfgAutoRelockDelay',
				num: 3,
				type: RANGE,
				unit: "seconds",
				min: 5,
				max: 255,
				default_: 30,
				size: CONFIG_VALUE_BYTE_SIZE
			],
			wrong_code_entry_limit: [
				displayName: 'Wrong code entry limit',
				deviceAttributeName: 'cfgCodeErrLimit',
				num: 4,
				type: RANGE,
				unit: "time",
				units: "times",
				min: 1,
				max: 7,
				default_: 5,
				size: CONFIG_VALUE_BYTE_SIZE
			],
			wrong_code_entry_lockout_time: [
				displayName: 'Wrong code entry lockout delay',
				deviceAttributeName: 'cfgLockoutDelay',
				num: 7,
				type: RANGE,
				unit: "second",
				units: "seconds",
				min: 1,
				max: 255,
				default_: 30,
				size: CONFIG_VALUE_BYTE_SIZE
			],
			op_mode: [
				displayName: 'Operating mode',
				deviceAttributeName: 'cfgOpMode',
				num: 8,
				type: CHOICE,
				choices: [NORMAL: 0, LOCKOUT: 1, PRIVACY: 2],
				default_: 0,
				size: CONFIG_VALUE_BYTE_SIZE
			],
			touch_to_lock: [num: 11],
			privacy_button: [num: 12],
			status_led: [num: 13]
		]

		Map VARIANTS = [
			touchscreenVariant: [
				name: 'touchscreen',
				ids: touchscreenModels,
				id_type: 'model id',
				data: [
					audio_mode: [
						choices: [SILENT: 1, LOW: 2, HIGH: 3]
					],
					language: [
						displayName: 'Language',
						deviceAttributeName: 'cfgLanguage',
						num: 5,
						type: CHOICE,
						choices: [ENGLISH: 1, SPANISH: 2, FRENCH: 3],
						default_: 1
					]
				]
			],

			pushbuttonVariant: [
				name: 'push button',
				ids: pushbuttonModels,
				id_type: 'model id',
				data: [
					audio_mode: [
						choices: [OFF: 1, ON: 3]
					]
				]
			],

			deadboltVariant: [
				name: 'deadbolt',
				ids: deadboltModels,
				id_type: 'model id',
				data: [
					auto_relock: [
						default_: 0x00
					]
				]
			],

			leverVariant: [
				name: 'lever',
				ids: leverModels,
				id_type: 'model id',
				data: [
					auto_relock: [
						default_: 0xFF
					]
				]
			]
		]

		// apply any variants for this device model id
		if (scObj.deviceModelId)
		{
			VARIANTS.each
			{
				String key, Map variant ->
				if (variant.ids.contains(scObj.deviceModelId))
				{
					smartlog(constructScope, LEVEL_DEBUG, "applying variant ${variant.name} to parameter metadata map")
					PARAMETERS = deepMerge(PARAMETERS, variant.data)
				}
			}
		}
		return PARAMETERS
	}

	def saveMetadata =
	{
		smartlog(constructScope, LEVEL_FINE, 'saveMetadata')
		if (scObj?.PARAMETER_METADATA)
		{
			state.config_PARAMETER_METADATA = scObj.PARAMETER_METADATA.findAll {it.value.containsKey('num')}
			// mark the saved metadata with version info
			state.config_PARAMETER_METADATA.SCVersion = [schema: scObj.version, modelId: scObj.deviceModelId]
		}
	}

	def restoreMetadata =
	{
		smartlog(constructScope, LEVEL_FINE, 'restoreMetadata')
		if (state?.config_PARAMETER_METADATA &&
			state.config_PARAMETER_METADATA?.SCVersion?.schema == scObj.version &&
			state.config_PARAMETER_METADATA?.SCVersion?.modelId == scObj.deviceModelId)
		{
			smartlog(scObj.type, LEVEL_TRACE, "restoring parameter metadata")
			scObj.PARAMETER_METADATA = state.config_PARAMETER_METADATA.findAll {it.key != 'SCVersion'}
			smartlog(constructScope, LEVEL_DEBUG, "restored parameter metadata keyset: " + scObj.PARAMETER_METADATA.keySet())
		}
	}

	restoreMetadata() // if it's stored in state
	if (!scObj?.PARAMETER_METADATA) // nothing was restored, build & store for the device
	{
		if (state?.config_PARAMETER_METADATA)
		{
			smartlog(scObj.type, LEVEL_TRACE, "rebuilding parameter metadata")
		}
		else
		{
			smartlog(scObj.type, LEVEL_TRACE, "building parameter metadata")
		}
		scObj.PARAMETER_METADATA = build()
		saveMetadata()
	}
	scObj.CHOICE_TYPE = CHOICE
	scObj.RANGE_TYPE = RANGE


	scObj.getParameterMetadata = {
		def it ->
		smartlog(scObj.type, LEVEL_FINE, "getParameterMetadata $it")
		Map parameterMetadata = scObj.PARAMETER_METADATA?."$it"
		if (! parameterMetadata)
		{
			smartlog(scObj.type, LEVEL_WARN, "no parameter with key name $it. options are ${scObj.PARAMETER_METADATA.keySet()}")
		}
		return parameterMetadata
	}

	// parameter name to type
	scObj.getParameterType = {
		smartlog(scObj.type, LEVEL_FINE, "getParameterType")
		return scObj.getParameterMetadata(it)?.type
	}

	// parameter name to number
	scObj.getParameterNum = {
		smartlog(scObj.type, LEVEL_FINE, "getParameterNum")
		return scObj.getParameterMetadata(it)?.num as Short
	}

	// map of parameter number to name
	scObj.getParameterIndex = {
		smartlog(scObj.type, LEVEL_FINE, "getParameterIndex")
		Map paramIndex = [:]
		scObj.PARAMETER_METADATA.each
		{
			log.debug "$it"
			paramIndex.put(it.value.num as Short, it.key)
		}
		return paramIndex
	}

	// list of parameter numbers
	scObj.getParameterNumList = {
		smartlog(scObj.type, LEVEL_FINE, "getParameterNumList")
		ArrayList pNumbers = scObj.getParameterIndex().keySet() as ArrayList
		return pNumbers.sort()
	}

	// parameter number to name
	scObj.getParameterName = {
		Number paramNum ->
		smartlog(scObj.type, LEVEL_FINE, "getParameterName $paramNum")
		def paramName = scObj.PARAMETER_METADATA.find { it.value?.num == paramNum as Integer }?.key
		if (!paramName)
		{
			smartlog(scObj.type, LEVEL_WARN, "no parameter having num $paramNum. options are ${scObj.getParameterIndex()}")
		}
		return paramName
	}

	scObj.normalizeToParamName = {
		def paramNameOrNum ->
		smartlog(scObj.type, LEVEL_FINE, "normalizeToParamName $paramNameOrNum")
		def debugMsg = "normalizing $paramNameOrNum to parameter name"
		String paramName
		if (paramNameOrNum instanceof Number)
		{
			paramName = scObj.getParameterName(paramNameOrNum)
			if (paramName == null)
			{
				debugMsg += ", but it does not resolve to any known parameter name."
			}
			else
			{
				debugMsg += ", and we found ${paramName}."
			}
		}
		else if (paramNameOrNum instanceof String)
		{
			paramName = paramNameOrNum
			debugMsg += ", which is already the parameter's name, so we return it."
		}
		else
		{
			smartlog(scObj.type, LEVEL_WARN, "argument $paramNameOrNum cannot be resolved to Number or String")
		}
		smartlog(scObj.type, LEVEL_DEBUG, "$debugMsg")
		return paramName
	}

	// If I could have a real class, I could overload for number and string
	// parameter number or name to display name
	scObj.getDisplayName = {
		def paramNameOrNum ->
		smartlog(scObj.type, LEVEL_FINE, "getDisplayName $paramNameOrNum")
		String paramName = scObj.normalizeToParamName(paramNameOrNum)
		if (paramName)
		{
			def pMeta = scObj.getParameterMetadata(paramName)
			if  (pMeta)
			{
				// this is a known parameter. If no display name, paramName is the display name
				return pMeta?.displayName?:paramName
			}
			smartlog(scObj.type, LEVEL_WARN, "parameter $paramName has no display name associated with it. using the base parameter name.")
		}
		return null // an error has already been written to the log by normalize
	}

	scObj.getDeviceAttributeName = {
		def paramNameOrNum ->
		smartlog(scObj.type, LEVEL_FINE, "getDeviceAttributeName $paramNameOrNum")
		String paramName = scObj.normalizeToParamName(paramNameOrNum)
		if (paramName)
		{
			def pMeta = scObj.getParameterMetadata(paramName)
			if  (pMeta)
			{
				log.debug pMeta
				// this is a known parameter. If no deviceAttributeName, then we return null
				return pMeta?.deviceAttributeName
			}
			smartlog(scObj.type, LEVEL_WARN, "parameter $paramName has no device attribute name associated with it. returning null.")
		}
		return null // an error has already been written to the log by normalize
	}

	scObj.validateParameterValue = {
		Number paramNum, Number paramValue ->
		smartlog(scObj.type, LEVEL_FINE, "validateParameterValue $paramNum $paramValue")
		def paramName = scObj.getParameterName(paramNum)
		if (paramName)
		{
			Integer paramIntValue = paramValue as Integer
			if (scObj.isChoiceType(paramName))
			{
				return scObj.isValueAChoice(paramName, paramValue)
			}
			else if (scObj.isRangeType(paramName))
			{
				return scObj.isValueInRange(paramName, paramValue)
			}
		}
		return false
	}

	scObj.getParameterDefaultValue = {
		String parameterName ->
		smartlog(scObj.type, LEVEL_FINE, "getParameterDefaultValue $parameterName")
		def defaultValue = scObj.getParameterMetadata(parameterName)?.default_
		if (defaultValue == null)
		{
			smartlog(scObj.type, LEVEL_WARN, "Parameter $parameterName has no default value")
		}
		return defaultValue
	}

	// for choice type parameters
	scObj.isChoiceType = {
		String parameterName ->
		smartlog(scObj.type, LEVEL_FINE, "isChoiceType $parameterName")
		if (scObj.CHOICE_TYPE != scObj.getParameterType(parameterName))
		{
			smartlog(scObj.type, LEVEL_WARN, "Parameter $parameterName is not a 'choice' type parameter.")
			return false
		}
		return true
	}

	scObj.getChoices = {
		String parameterName ->
		smartlog(scObj.type, LEVEL_FINE, "getChoices $parameterName")
		scObj.isChoiceType(parameterName)
		return scObj.getParameterMetadata(parameterName)?.choices
	}

	scObj.choiceName = {
		String parameterName, Number choiceValue ->
		smartlog(scObj.type, LEVEL_FINE, "choiceName $parameterName $choiceValue")
		String choiceKey = scObj.getChoices(parameterName)?.find { it.value == choiceValue as Integer}?.key
		if (choiceKey == null)
		{
			smartlog(scObj.type, LEVEL_WARN, "Choice value $choiceValue is invalid for parameter $parameterName. Options are: ${scObj.getChoices(parameterName)}")
			return null
		}
		return choiceKey
	}

	scObj.choiceValue = {
		String parameterName, String choiceName ->
		smartlog(scObj.type, LEVEL_FINE, "choiceValue $parameterName $choiceName")
		choiceName = choiceName.toUpperCase()
		Short choiceValue = scObj.getChoices(parameterName)?."$choiceName" as Short
		if (choiceValue == null)
		{
			smartlog(scObj.type, LEVEL_WARN, "Choice name $choiceName is invalid for parameter $parameterName. Options are: ${scObj.getChoices(parameterName)}")
			return null
		}
		return choiceValue
	}

	scObj.isValueAChoice = {
		String parameterName, Number choiceValue ->
		smartlog(scObj.type, LEVEL_FINE, "isValueAChoice $parameterName $choiceValue")
		if (scObj.choiceName(parameterName, choiceValue))
		{
			return true
		}
		return false
	}

	// for range type parameters
	scObj.isRangeType = {
		String parameterName ->
		smartlog(scObj.type, LEVEL_FINE, "isRangeType $parameterName")
		if (scObj.RANGE_TYPE != scObj.getParameterType(parameterName))
		{
			smartlog(scObj.type, LEVEL_WARN, "Parameter $parameterName is not a 'range' type parameter.")
			return false
		}
		return true
	}

	scObj.rangeMin = {
		String parameterName ->
		smartlog(scObj.type, LEVEL_FINE, "rangeMin $parameterName")
		Short rangeMin
		if (scObj.isRangeType(parameterName))
		{
			rangeMin = scObj.getParameterMetadata(parameterName)?.min?.shortValue()?:Short.MIN_VALUE
		}
		return rangeMin
	}

	scObj.rangeMax = {
		String parameterName ->
		smartlog(scObj.type, LEVEL_FINE, "rangeMax $parameterName")
		Short rangeMax
		if (scObj.isRangeType(parameterName))
		{
			rangeMax = scObj.getParameterMetadata(parameterName)?.max?.shortValue()?:Short.MAX_VALUE
		}
		return rangeMax
	}

	scObj.isValueInRange = {
		String parameterName, Integer rangeVal ->
		smartlog(scObj.type, LEVEL_FINE, "isValueInRange $parameterName $rangeVal")
		if (scObj.isRangeType(parameterName) &&
			scObj.rangeMin(parameterName).shortValue() <= rangeVal.shortValue() &&
			rangeVal.shortValue() <= scObj.rangeMax(parameterName).shortValue() )
		{
			return true
		}
		return false
	}

	scObj.constrainValueInRange = {
		String parameterName, Short rangeVal ->
		smartlog(scObj.type, LEVEL_FINE, "constrainValueInRange $parameterName $rangeVal")
		if (scObj.isRangeType(parameterName))
		{
			if (rangeVal < scObj.rangeMin(parameterName))
			{
				rangeVal = scObj.rangeMin(parameterName)
			}
			else if (rangeVal > scObj.rangeMax(parameterName))
			{
				rangeVal = scObj.rangeMax(parameterName)
			}
			return rangeVal
		}
		return scObj.getDefautParameterValue(parameterName) as Short
	}

	// singular
	scObj.getUnit = {
		String parameterName ->
		smartlog(scObj.type, LEVEL_FINE, "getUnit $parameterName")
		String unit = ''
		if (scObj.isRangeType(parameterName))
		{
			unit = scObj.getParameterMetadata(parameterName)?.unit?:scObj.getParameterMetadata(parameterName)?.units
		}
		if (!unit)
		{
			unit = ''
		}
		return unit
	}

	// plural
	scObj.getUnits = {
		String parameterName ->
		smartlog(scObj.type, LEVEL_FINE, "getUnits $parameterName")
		String units = ''
		if (scObj.isRangeType(parameterName))
		{
			units = scObj.getParameterMetadata(parameterName)?.units?:scObj.getParameterMetadata(parameterName)?.unit
		}
		if (!units)
		{
			units = ''
		}
		return units
	}

	scObj.formatRangeValueWithUnits = {
		String parameterName, Integer parameterValue ->
		smartlog(scObj.type, LEVEL_FINE, "formatRangeValueWithUnits $parameterName $parameterValue")
		String formattedValueWithUnits = ''
		if (parameterValue.abs() == 1)
		{
			formattedValueWithUnits = "$parameterValue " + scObj.getUnit(parameterName)
		}
		else
		{
			formattedValueWithUnits = "$parameterValue " + scObj.getUnits(parameterName)
		}

		return formattedValueWithUnits.trim()
	}

	// for all types
	scObj.getSizeInBytes = {
		String parameterName ->
		smartlog(scObj.type, LEVEL_FINE, "getSizeInBytes $parameterName")
		return scObj.getParameterMetadata(parameterName)?.size as Short
	}

	// Intended to pass the config metadata map out to a smartapp requesting it.
	scObj.getConfigMetadata = {
		smartlog(scObj.type, LEVEL_FINE, "getConfigMetadata")
		return scObj.PARAMETER_METADATA
	}

	scObj.getDefaultConfig = {
		smartlog(scObj.type, LEVEL_FINE, "getDefaultConfig")
		return scObj.PARAMETER_METADATA.findAll { it.value.containsKey('num') && it.value.containsKey('default_') && it.value.containsKey('size') }
	}

	return scObj
}
