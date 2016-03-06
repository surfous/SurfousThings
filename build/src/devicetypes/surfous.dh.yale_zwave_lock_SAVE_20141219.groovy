/**
 *	Yale RealLiving Z-Wave Lock
 *	Customized version of reference Z-Wave lock implementation. Nod to Yves Racine for lock
 *	operation timeout setting
 *	Copyright 2014 Kevin Shuk
 *
 *	Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *	in compliance with the License. You may obtain a copy of the License at:
 *
 *			http://www.apache.org/licenses/LICENSE-2.0
 *
 *	Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *	on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *	for the specific language governing permissions and limitations under the License.
 *
 *
 *	state members and their functions:
 * 		lastAssocQueryTime: datetime in ms the association with the hub was checked
 *		assoc: the Z-wave hub's node ID within the association group
 *		lastUnitClockGetTime:
 *		lastBatteryCheckTime:
 *		codeDb: map of codes & names by slot number
 *			elements are: [code: <CODE as string>, name: <NAME string>, uuid: <UUID string>]
 *		pendingCodeDb: temp holding for a code db entry while awaiting UserCodeReport from a get after a set
 *			* done because lock doesn't store slot name
 *			* remove entry after committing to codeDb
 *		codes: number of user code slots the lock supports
 *		code###: code for slot ###   DEPRECATED
 *		requestCode: current slot number for reloadAllCodes
 *		currentPollSlot:
 */
metadata {
	// Automatically generated. Make future change here.
	definition (name: "Yale RealLiving Z-Wave Lock", namespace: "surfous", author: "Kevin Shuk")
	{
		capability "Lock"
		capability "Configuration"
		capability "Lock Codes"
		capability "Polling"
		capability "Battery"
		capability "Refresh"

		command "doAdHoc"

		fingerprint deviceId: '0x4002', inClusters: '0x72,0x86,0x98'
		fingerprint deviceId: '0x4003', inClusters: '0x72,0x86,0x98'
		fingerprint deviceId: '0x4004', inClusters: '0x72,0x86,0x98'
	}

	simulator
	{
		status "locked": "command: 9881, payload: 00 62 03 FF 00 00 FE FE"
		status "unlocked": "command: 9881, payload: 00 62 03 00 00 00 FE FE"
		status "unlocked w/ timeout": "command: 9881, payload: 00 62 03 01 00 00 FE FE"

		reply "9881006201FF,delay 4200,9881006202": "command: 9881, payload: 00 62 03 FF 00 00 FE FE"
		reply "988100620100,delay 4200,9881006202": "command: 9881, payload: 00 62 03 00 00 00 FE FE"
		reply "988100620101,delay 4200,9881006202": "command: 9881, payload: 00 62 03 01 00 00 FE FE"
	}

	preferences
	{
		//input("test_setting", "enum", title: "test enum setting", displayDuringSetup: false, options:[[1:"silent"],[2:"low"],[3:"high"]])
		input("audio_mode", "enum", title: "device audio volume", displayDuringSetup: false, options:[[1:"silent"],[2:"low"],[3:"high"]])
		input("relock_auto", "enum", title: "automatic relock", displayDuringSetup: false, options:[[255:"on"],[0:"off"]])
		input("relock_secs", "number", title: "seconds until automatic relock (5-255, default 30)", range: "5..255", description: "5-255, default 30", displayDuringSetup: false)
		input("wrong_code_entry_limit", "number", title: "number of consecutive wrong entries until keypad is temporarily disabled", range: "1..7", description: "default 5", displayDuringSetup: false)
		input("wrong_code_lockout_secs", "number", title: "device keypad will be disabled for this many seconds after wrong entries exceeded", range: "1..255", description: "1-255 seconds, default 30", displayDuringSetup: false)
		input("language", "enum", title: "language for voice prompts", displayDuringSetup: false, options:[[1:"english"],[2:"spanish"],[3:"french"]])
		input("op_mode", "enum", title: "operating mode", displayDuringSetup: false, options:[[0:"normal"], [1:"vacation"], [2:"privacy"]])
	}


	tiles
	{
		standardTile("toggle", "device.lock", width: 2, height: 2) {
			state "locked", label:'locked', action:"lock.unlock", icon:"st.locks.lock.locked", backgroundColor:"#79b821", nextState:"unlocking"
			state "unlocked", label:'unlocked', action:"lock.lock", icon:"st.locks.lock.unlocked", backgroundColor:"#ffffff", nextState:"locking"
			state "locking", label:'locking', icon:"st.locks.lock.locked", backgroundColor:"#79b821"
			state "unlocking", label:'unlocking', icon:"st.locks.lock.unlocked", backgroundColor:"#ffffff"
		}
		standardTile("lock", "device.lock", inactiveLabel: false, decoration: "flat") {
			state "default", label:'lock', action:"lock.lock", icon:"st.locks.lock.locked", nextState:"locking"
		}
		standardTile("unlock", "device.lock", inactiveLabel: false, decoration: "flat") {
			state "default", label:'unlock', action:"lock.unlock", icon:"st.locks.lock.unlocked", nextState:"unlocking"
		}
		valueTile("battery", "device.battery", inactiveLabel: false, decoration: "flat") {
			state "battery", label:'${currentValue}% battery', action:"getBatteryStatus", unit:"%"
		}
		standardTile("refresh", "device.lock", inactiveLabel: false, decoration: "flat") {
			state "default", label:'', action:"refresh.refresh", icon:"st.secondary.refresh"
		}
		standardTile("adhoc", "device.lock", inactiveLabel: false, decoration: "flat") {
			state "default", label:'AdHoc Test', action:"runAdHocTest", icon:"st.Electronics.electronics13"
		}

		main "toggle"
		details(["toggle", "lock", "unlock", "battery", "refresh", "adhoc"])
	}
}

import static java.util.UUID.randomUUID
import groovy.transform.Field
import physicalgraph.zwave.commands.doorlockv1.*
import physicalgraph.zwave.commands.usercodev1.*
import physicalgraph.zwave.commands.configurationv1.*

// constants
@Field final TimeZone tzUTC = TimeZone.getTimeZone('UTC')
@Field final int maxClockDeltaMs = 61000
@Field final int mSecondsPerHour = 3600000

@Field final short maxSlotDefault = 10
@Field final short minSlotNum = 1 // 0 is reserved for the master code

@Field final int defaultDelayMs = 4200
@Field final int lockDelayMs = 2200
@Field final int setGetDelayMs = 7000
@Field final int assocDelayMs = 6000

@Field final int checkBatteryEveryNHours = 8
@Field final int getTimeEveryNHours = 1

@Field final int assocGroupingId = 1

@Field pendingSlotNameMap = [:]

def installed()
{
	initState()

	def cmds = []
	cmds.addAll(pollAssociation()?:[])

	if (cmds.size() > 0) cmds << delay()
	cmds << requestMsr()
	cmds << delay()

	cmds << requestVersion()
	cmds << delay()
}

// initStat things like state
def void initState()
{
	if (!state.codeDb) state.codeDb = [:]
	if (!state.pendingCodeDb) state.pendingCodeDb = [:]
	if (!state.settings) state.settings = [:]
}

//parse
def parse(String description)
{
	def result = null
	if (description.startsWith("Err"))
	{
		result = createEvent(descriptionText:description, displayed:true)
	}
	else
	{
		def cmd = zwave.parse(description, [ 0x98: 1, 0x72: 1])
		if (cmd)
		{
			result = zwaveEvent(cmd)
		}
	}
	log.debug "\"$description\" parsed to ${result.inspect()}"
	log.debug "Parse result $result"
	return result
}

// encapsulate a command with a security layer
def secure(physicalgraph.zwave.Command cmd)
{
	log.debug "Encapsulating $cmd"
	def result = zwave.securityV1.securityMessageEncapsulation().encapsulate(cmd).format()
	log.debug "Result $result"
	return result
}

// encapsulate a series of commands to execute with specified delay
def secureSequence(commands, delay=defaultDelayMs)
{
	delayBetween(commands.collect{ secure(it) }, delay)
}

// peel away the security layer, then handle the encapsulated event
def zwaveEvent(physicalgraph.zwave.commands.securityv1.SecurityMessageEncapsulation cmd)
{
	log.debug "handling event securityv1.SecurityMessageEncapsulation. Opening encapsulated message."
	def encapsulatedCommand = cmd.encapsulatedCommand([0x20: 1,0x62: 1, 0x63: 1, 0x70: 1, 0x71: 2, 0x75: 1, 0x80:1, 0x85: 2, 0x4E: 2, 0x4C: 1, 0x8B: 1, 0x5D: 2])
	log.debug "encapsulated: $encapsulatedCommand"
	if (encapsulatedCommand)
	{
		return zwaveEvent(encapsulatedCommand)
	}
}

/*
 * Event handlers for unsolicited report commands
 */

def zwaveEvent(physicalgraph.zwave.commands.doorlockv1.DoorLockOperationReport cmd)
{
	log.debug "handling event doorlockv1.doorLockOperationReport"
	def eventMap = [ name: "lockOperation", displayed: false]
	if (cmd.doorLockMode == 0xFF)
	{
		eventMap.value = "locked"
	}
	else if (cmd.doorLockMode >= 0x40)
	{
		eventMap.value = "unknown"
	}
	else if (cmd.doorLockMode & 1)
	{
		eventMap.value = "unlocked with timeout"
	}
	else
	{
		eventMap.value = "unlocked"
	}
	eventMap.descriptionText = "$device.displayName operation report. Device state is $eventMap.value"
	log.debug "doorLockMode: $cmd.doorLockMode; $eventMap"
	return createEvent(eventMap)
}

// handle an alarm report which is how the lock conveys lock/unlock events, faults, tampering and code sets
def zwaveEvent(physicalgraph.zwave.commands.alarmv2.AlarmReport cmd) {
	log.debug "handling event alarmv2.AlarmReport"
	def responses = []
	def eventMap = [ name: "lock", descriptionText: "description", value: "unknown", displayed: false]
	log.debug "$device.displayName AlarmReport type: $cmd.alarmType; level: $cmd.alarmLevel"
	switch (cmd.alarmType)
	{
		case 9:
			// deadbolt jammed
			eventMap.descriptionText = "$device.displayName - deadbolt jammed"
			eventMap.displayed = true
			break
		case 18:
			// Keypad lock (level = user code slot #)
			eventMap.descriptionText = "$device.displayName was locked using keypad with code $cmd.alarmLevel"
			eventMap.value = "locked"
			break
		case 19:
			// Keypad unlock (level = user code slot #)
			eventMap.descriptionText = "$device.displayName was unlocked using keypad with code $cmd.alarmLevel"
			eventMap.value = "unlocked"
			break
		case 21:
			// Manual lock
			// lock by touch
			if (cmd.alarmLevel == 1) eventMap.descriptionText = "$device.displayName was locked manually"
			if (cmd.alarmLevel == 2) eventMap.descriptionText = "$device.displayName was locked using lock & leave"
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
			eventMap.descriptionText = "User code deleted from slot ${cmd.alarmLevel} on $device.displayName"
			break
		case 112:
			// Master code changed (level = 0)
			// User code set (level = 1-249)
			if (cmd.alarmLevel == 0)
			{
				eventMap.name = "masterCode"
				eventMap.descriptionText = "Master Code (slot 0) was changed"
			}
			else
			{
				eventMap.name = "userCode"
				eventMap.descriptionText = "User Code for slot ${cmd.alarmLevel} set or updated on $device.displayName"
			}
			break
		case 113:
			// duplicate PIN code error (level = slot 0-249)
			eventMap.name = "userCode"
			eventMap.descriptionText = "User Code for slot ${cmd.alarmLevel} is a duplicate in another slot on $device.displayName"
			// clead from codeDb?
			break
		case 130:
			// RF module power cycled
			eventMap.descriptionText = "Power has been restored to the RF module in $device.displayName"
			eventMap.displayed = true
			// set the clock, and check the battery on the next poll
			state.forceBatteryGet = true
			state.forceClockSet = true
			responses.addAll(pollClock())
			break
		case 131:
			// Lock handing completed
			eventMap.descriptionText = "Lock handing complete on $device.displayName"
			break
		case 161:
			// tamper alarm
			eventMap.name = "alarm"
			eventMap.displayed = true
			if (cmd.alarmLevel == 1) eventMap.descriptionText = "$device.displayName tamper alarm - incorrect keypad attempts exceed limit"
			if (cmd.alarmLevel == 2) eventMap.descriptionText = "$device.displayName front esctucheon removed from main"
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
			eventMap = [ displayed: false, name: "Unknown Alarm", descriptionText: "$device.displayName: unknown alarm - $cmd" ]
			break
	}
	log.info "Alarm received: ${eventMap.name}: ${eventMap.descriptionText}"
	def actions = [createEvent(eventMap)]
	if (responses.size() > 0) actions.addAll(responses)
	return actions
}

def setAssociation(boolean secureResult=true)
{
	log.debug "setting $device.displayName association to groupingIdentifier $assocGroupingId for nodeId $zwaveHubNodeId"
	def result = zwave.associationV1.associationSet(groupingIdentifier:assocGroupingId, nodeId:[zwaveHubNodeId])
	if (secureResult) result = secure(result)
	return result
}

def requestAssociation(boolean secureResult=true)
{
	log.debug "getting $device.displayName association for groupingIdentifier $assocGroupingId"
	def result = zwave.associationV1.associationGet(groupingIdentifier:assocGroupingId.shortValue())
	if (secureResult) result = secure(result)
	return result
}

def associationRemove(boolean secureResult=true)
{
	log.debug "removing $device.displayName association to groupingIdentifier $assocGroupingId for nodeId $zwaveHubNodeId"
	def result = zwave.associationV1.associationRemove(groupingIdentifier:assocGroupingId, nodeId:[zwaveHubNodeId])
	if (secureResult) result = secure(result)
	return result
}

def zwaveEvent(physicalgraph.zwave.commands.associationv2.AssociationReport cmd)
{
	log.debug "handling event associationv1.associationReport"
	def result = []
	if (cmd.nodeId.any { it == zwaveHubNodeId })
	{
		log.debug "$device.displayName is associated to $zwaveHubNodeId"
		state.assoc = zwaveHubNodeId
		state.lastAssocQueryTime = new Date().time
	}
	else
	{
		log.debug "association not found, setting it to groupingIdentifier $assocGroupingId for nodeId $zwaveHubNodeId"
		result << response(setAssociation())
		state.lastAssocQueryTime = null
	}
	result
}

// handle unknown/unexpected report
def zwaveEvent(physicalgraph.zwave.Command cmd)
{
	log.warn "Handling event: unexpected zwave command $cmd"
	createEvent(displayed: false, descriptionText: "$device.displayName: $cmd")
}

/*
 * Implement commands
 */
def refresh()
{
	log.debug "Executing refresh capability"
	def actions = []

	actions.addAll(pollLockOperation(true))
	if (!state.maxSlotNum)
	{
		actions << requestUsersNumber()
		actions << delay()
	}

	actions.addAll(pollBattery(true))
	actions << delay()

	state.forceClockGet = true
	actions.addAll(pollClock())
	actions << delay()

	actions.addAll(requestConfigAll()) // has trailing delay already

	return actions
}

// manufactuer specific report
def requestMsr()
{
	def cmd = zwave.manufacturerSpecificV2.manufacturerSpecificGet().format()
	log.debug "requesting manufacturer specific report for $device.displayName : $cmd"
	return cmd
}

def zwaveEvent(physicalgraph.zwave.commands.manufacturerspecificv2.ManufacturerSpecificReport cmd)
{
	log.debug "handling event manufacturerspecificv2.ManufacturerSpecificReport"
	def result = []

	def msr = String.format("%04X-%04X-%04X", cmd.manufacturerId, cmd.productTypeId, cmd.productId)
	updateDataValue("MSR", msr)

	state.msr = [:]
	state.msr.manufacturerId = cmd.manufacturerId
	state.msr.productTypeId = cmd.productTypeId
	state.msr.productId = cmd.productId

	// Is this a Yale lock?
	if (cmd.manufacturerId == 129 || (cmd.manufacturerId == 109 && cmd.productTypeId < 5))
	{
		state.msr.manufacturer = "YALE"
	}
	else
	{
		state.msr.manufacturer = "UNKNOWN"
	}

	// which Yale model?
	switch (cmd.productTypeId)
	{
		case 1:
			state.msr.model = "Touchscreen Lever"
			break
		case 2:
			state.msr.model = "Touchscreen Deadbolt"
			break
		case 3:
			state.msr.model = "Push Button Lever"
			break
		case 4:
			state.msr.model = "Push Button Deadbolt"
			break
		default:
			state.msr.model = "Unknown Model $cmd.productTypeId"
	}

	state.msr.generation = cmd.productId

	def niceMsr = "$device.displayName MSR: $msr; Manufacturer: $state.msr.manufacturer; Model: $state.msr.model; Generation: $state.msr.generation"
	log.debug niceMsr
	return createEvent(description: cmd.toMapString(), descriptionText: niceMsr, isStateChange: false)
}

// version information
def requestVersion()
{
	def result = zwave.versionV1.versionGet()
	log.debug "requestVersion $result"
	return result
}

def zwaveEvent(physicalgraph.zwave.commands.versionv1.VersionReport cmd)
{
	log.debug "handling event versionv1.VersionReport"
	def fw = "${cmd.applicationVersion}.${cmd.applicationSubVersion}"
	updateDataValue("fw", fw)
	def text = "$device.displayName: lock firmware version: {cmd.applicationVersion}, Z-Wave firmware version: ${cmd.applicationSubVersion}, Z-Wave lib type: ${cmd.zWaveLibraryType}, Z-Wave version: ${cmd.zWaveProtocolVersion}.${cmd.zWaveProtocolSubVersion}"

	state.version = [:]
	state.version.lockFirmware = cmd.applicationVersion
	state.version.zwaveFirmware = cmd.applicationSubVersion
	state.version.zwaveLibraryType = cmd.zWaveLibraryType
	state.version.zwaveProtocol = cmd.zWaveProtocolVersion
	state.version.zwaveProtocolSub = cmd.zWaveProtocolSubVersion

	return createEvent(description: cmd.toMapString(), descriptionText: text, isStateChange: false)
}


// battery level
def requestBatteryStatus(boolean secureResult=true)
{
	def result = zwave.batteryV1.batteryGet()
	if (secureResult) result = secure(result)
	log.debug "requestBatteryStatus $result"
	return result
}

def zwaveEvent(physicalgraph.zwave.commands.batteryv1.BatteryReport cmd)
{
	log.debug "handling event batteryv1.BatteryReport"
	def map = [ name: "battery", unit: "%" ]
	if (cmd.batteryLevel == 0xFF)
	{
		map.value = 1
		map.descriptionText = "$device.displayName has a low battery"
	}
	else
	{
		map.value = cmd.batteryLevel
	}
	state.lastBatteryCheckTime = new Date().time
	return createEvent(map)
}

//configuration
def requestConfigAll()
{
	def actions = []
	for (configEntry in configAttributeMap)
	{
		def paramNum = configEntry.key.toInteger()
		actions << requestConfig(paramNum)
		actions << delay(lockDelayMs)

	}
	return actions
}

def requestConfig(int paramNum)
{
	def configEntry = configAttributeMap.get(paramNum, null)
	if (configEntry)
	{
		log.debug "request configuration for parameter $configEntry"
		return secure(zwave.configurationV1.configurationGet(parameterNumber: paramNum))
	}
	log.debug "unknown parameter number $paramNum"
	return null
}

def zwaveEvent(physicalgraph.zwave.commands.configurationv1.ConfigurationReport  cmd)
{
	log.debug "handling event configuration.ConfigurationReport"
	log.debug "$cmd"
	if (! state.config) state.config = [:]
	def eventMap = [ name: "config", value:	cmd.configurationValue]
	if (cmd.parameterNumber)
	{
		def paramNum = cmd.parameterNumber.toInteger()
		def paramIntVal = cmd.configurationValue[0].toInteger()
		def paramDevName = configAttributeMap.get(paramNum)
		state.config[paramNum] = paramIntValue // update the config as held in the state db
		log.debug "parameter value for $paramDevName is $paramIntValue"
		eventMap.descriptionText = "$paramDevName: $paramIntValue"
	}
	return createEvent(eventMap)
}

def setConfiguration(int paramNum, int sizeInBytes, int paramIntValue)
{
	return secure(zwave.configurationV1.configurationSet(parameterNumber: paramNum, size: sizeInBytes, configurationValue: [paramIntValue]))
}


// return saved value from state.settings or the default from metadata
def currentConfigParamValue(String paramDevName)
{
	state.config = [:]
	/*
	int paramNum = configAttributeMap.find{ it.value == paramDevName }.key
	//def paramDevName = configAttributeMap.get(paramNum)
	def paramMetadataEntry = configMetadata.get(paramDevName, [:])

	def savedSettings
	if (state.config)
	{
		savedSettings = state.config
	}
	else
	{
		savedSettings = []
	}

	if (savedSettings.get(paramNum, null))
	{
		return savedSettings.get(paramNum)
	}
	else if (paramMetadataEntry.get('default', null))
	{
		return paramMetadataEntry.get('default')
	}
	*/
	return null
}

/*
def currentConfigParamValue(String paramDevName)
{
	int paramNum = configAttributeMap.find{ it.value == paramDevName }.key
	return currentConfigParamValue(paramNum)
}
*/

def configure()
{
	initState()
	return requestConfigAll()
}

// called after prefs are saved
def updated()
{
	initState()
   	def commands = []

	for (configEntry in configAttributeMap)
	{
		int paramNum = configEntry.key.toInteger()
		def paramDevName = configEntry.value
		def paramMetadataEntry = configMetadata.get(paramDevName, [:])
		int localSavedSettingValue
		if (state.settings.get(paramNum, null)) localSavedSettingValue = state.settings.get(paramNum).toInteger()

		int updatedSettingValue
		if (settings.get(paramDevName, null)) updatedSettingValue = settings.get(paramDevName).toInteger()

		if (updatedSettingValue && updatedSettingValue != localSavedSettingValue)
		{
			if (paramMetadataEntry.max && updatedSettingValue > paramMetadataEntry.max)
			{
				updatedSettingValue = paramMetadataEntry.max.toInteger()
			}
			if (paramMetadataEntry.min && updatedSettingValue < paramMetadataEntry.min)
			{
				updatedSettingValue = paramMetadataEntry.min.toInteger()
			}

			// update & get new value
			commands << setConfiguration(paramNum, 1, updatedSettingValue)
			commands << delay(2200)
			commands << requestConfig(paramNum)
			commands << delay(2200)
		}
	}
	return commands
}

// time parameters
def setUnitToCurrentTimeUtc()
{
	log.debug "Setting the real-time clock on $device.displayName"
	def actions = []
	Calendar cal = Calendar.getInstance(tzUTC)
	actions << secure(zwave.timeParametersV1.timeParametersSet(
		day:cal.get(Calendar.DAY_OF_MONTH).shortValue(),
		hourUtc:cal.get(Calendar.HOUR_OF_DAY).shortValue(),
		minuteUtc:cal.get(Calendar.MINUTE).shortValue(),
		month:cal.get(Calendar.MONTH).shortValue(),
		secondUtc:cal.get(Calendar.SECOND).shortValue(),
		year:cal.get(Calendar.YEAR) ) )
}

def requestUnitTimeUtc()
{
	log.debug "Executing 'requestUnitTimeUtc'"
	return secure(zwave.timeParametersV1.timeParametersGet())
}

def zwaveEvent(physicalgraph.zwave.commands.timeparametersv1.TimeParametersReport cmd)
{
	log.debug "handling event physicalgraph.zwave.commands.timeparametersv1.TimeParametersReport"
	log.debug "$cmd"
	def actions = []
	Calendar nowCal = Calendar.getInstance(tzUTC) // the current time in UTC
	Calendar lockTimeCal = Calendar.getInstance(tzUTC) // Set this to the time the lock reported back
	lockTimeCal.set(cmd.year, cmd.month, cmd.day, cmd.hourUtc, cmd.minuteUtc, cmd.secondUtc)
	long clockDeltaMs = nowCal.getTimeInMillis() - lockTimeCal.getTimeInMillis() // How far off is ST Time from the lock's time?
	def dtStr = String.format("%tH:%<tM:%<tS %<tY-%<tB-%<td %<tZ", lockTimeCal)
	log.info "$device.displayName clock is currently set to $dtStr. Variance is $clockDeltaMs"
	state.clockDeltaMs = clockDeltaMs

	// if unit clock is off by over a certain threshhold, set it
	if (clockDeltaMs.abs() > maxClockDeltaMs)
	{
		state.forceClockSet = true // force a check/set next poll
	}
	return createEvent(descriptionText: "device clock $dtStr", isStateChange: false)
}

def poll()
{
	def actions = []
	log.debug "Executing 'poll'"
	def maxSlotNum = [maxSlotDefault, state.maxSlotNum].max()
	if (! state.maxSlotNum)
	{
		actions << requestUsersNumber()
		actions << delay()
	}
	if (state.assoc != zwaveHubNodeId && secondsPast(state.lastAssocQueryTime, 19*60))
	{
		log.debug "poll: setting association"
		actions << setAssociation()
		actions << delay(assocDelayMs)
		actions << requestAssociation()
		actions << delay()
	}
	else
	{
		log.debug "poll: check main attributes"
		actions.addAll(pollLockOperation()?:[])
		actions.addAll(pollBattery()?:[])

		if ((!state.codeDb || state.codeDb.size() == 0) && !state.currentPollSlot)
		{
			log.info "preparing for a slot poll beginning at slot 1 with the next polling period"
			initState()
			state.currentPollSlot = 1
		}
		if (state.currentPollSlot && state.currentPollSlot <= maxSlotNum)
		{
			log.info "polling for next slot: $state.currentPollSlot"
			actions << requestCode(state.currentPollSlot)
			actions << delay(setGetDelayMs)
		}
		// doing this dead last as the clock set tends to get lost
		actions.addAll(pollClock()?:[])
	}

	actions.addAll(pollAssociation()?:[])

	log.debug "poll is sending ${actions.inspect()}, state: ${state.inspect()}"
	//device.activity()

	return actions?:null
}

private delay()
{
	return delay(defaultDelayMs)
}

private delay(int msecToDelay)
{
	return "delay $msecToDelay"
}

private pollAssociation()
{
	def actions = []
	if (state.assoc && state.assoc == zwaveHubNodeId)
	{
		log.debug "$device.displayName is associated to ${state.assoc}"
	}
	else if (!state.lastAssocQueryTime)
	{
		log.debug "checking $device association"
		actions << requestAssociation()
	}
	else if (secondsPast(state.lastAssocQueryTime, 9*60))
	{
		log.debug "setting association"
		actions << setAssociation()
		actions << delay(assocDelayMs)
		actions << requestAssociation()
	}
	return actions
}

private pollLockOperation(boolean checkLock=false)
{
	def cmds = []
	if (!state.lastPoll) state.lastPoll = 0
	def latest = device.currentState("lock")?.date?.time
	if (checkLock)
	{
		log.debug "Lock operation check forced."
	}
	else if (!latest || timePast(latest, 0, 6, 0) || timePast(state.lastPoll, 1, 7, 0))
	{
		log.debug "Device lock state check is due."
		checkLock = true
	}

	if (checkLock)
	{
		log.info "checking lock state"
		state.lastPoll = new Date().time
		cmds << requestLockOperationStatus()
		cmds << delay()
		state.forceCheckLock = false
	}
	return cmds?:null
}

private pollBattery(boolean checkBattery=false)
{
	def actions = []
	log.debug "pollBattery: Check if it's time to ask the lock its battery level"

	if (checkBattery)
	{
		log.debug "Battery check forced."
	}
	else if (timePast(state.lastBatteryCheckTime, checkBatteryEveryNHours, 0, 0))
	{
		log.debug "Battery check is due."
		checkBattery = true
	}

	if (checkBattery)
	{
		log.info "time to request device battery value"
		actions << requestBatteryStatus()
		actions << delay()
	}
	return actions?:null
}

private pollClock()
{
	log.debug "pollClock: Check clock's time every hour, if forced, or if variance is unknown"
	def actions = []
	boolean setClock = false
	boolean getClock = false
	if (state.get("forceClockSet", false))
	{
		log.debug "Clock set forced."
		setClock = true
	}
	else if (!state.clockDeltaMs)
	{
		log.debug "Clock variance unknown."
		setClock = true
	}
	else if (state.clockDeltaMs.abs() > maxClockDeltaMs)
	{
		log.debug "Clock variance is outside of permitted max of +-$maxClockDeltaMs ms"
		setClock = true
	}

	if (setClock)
	{
		log.info "Setting clock, then check time"
		actions << setUnitToCurrentTimeUtc()
		actions << delay(setGetDelayMs)
		getClock = true
		state.forceClockSet = false
	}
	else if (state.get("forceClockGet", false))
	{
		log.debug "Clock check forced"
		getClock = true
	}
	else if (timePast(state.lastUnitClockGetTime, getTimeEveryNHours))
	{
		log.debug "Clock periodic check is due."
		getClock = true
	}

	if (getClock)
	{
		log.info "Getting time from device clock"
		actions << requestUnitTimeUtc()
		state.forceClockGet = false
	}
	return actions?:null
}

// locking and unlocking
def lockAndCheck(doorLockModeOption, delay=defaultDelayMs)
{
	secureSequence([
		zwave.doorLockV1.doorLockOperationSet(doorLockMode: doorLockModeOption),
		requestLockOperationStatus(false)
	], delay)
}

def lock()
{
	log.debug "Executing 'lock' - set door lock operation to $DoorLockOperationSet.DOOR_LOCK_MODE_DOOR_SECURED"
	lockAndCheck(DoorLockOperationSet.DOOR_LOCK_MODE_DOOR_SECURED, lockDelayMs)
}

def unlock()
{
	log.debug "Executing 'unlock' - set door lock operation to $DoorLockOperationSet.DOOR_LOCK_MODE_DOOR_UNSECURED"
	lockAndCheck(DoorLockOperationSet.DOOR_LOCK_MODE_DOOR_UNSECURED)
}

def unlockwtimeout()
{
	log.debug "Executing 'unlock with timeout' - set door lock operation to $DoorLockOperationSet.DOOR_LOCK_MODE_DOOR_UNSECURED_WITH_TIMEOUT"
	lockAndCheck(DoorLockOperationSet.DOOR_LOCK_MODE_DOOR_UNSECURED_WITH_TIMEOUT)
}

def requestLockOperationStatus(boolean secureResult=true)
{
	def result = zwave.doorLockV1.doorLockOperationGet()
	if (secureResult) result = secure(result)
	return result
}

// code management
private validateSlotNumber(int slot)
{
	// slots run from 1-249. Slot 0 is master code and cannot be modified by RF
	0 < slot && slot <= state.get("maxSlotNum", maxSlotDefault)
}

private validateCode(codeStr)
{
	// code must be 4-8 digits
	codeStr ==~ /\d{4,8}/
}

//set all codes from a JSON input
def updateCodes(codeSettings)
{
	if(codeSettings instanceof String) codeSettings = util.parseJson(codeSettings)
	def set_cmds = []
	def get_cmds = []
	codeSettings.each
	{ name, updated ->
		def current = state[name]
		if (name.startsWith("code"))
		{
			def n = name[4..-1].toInteger()
			log.debug "$name was $current, set to $updated"
			if (updated?.size() >= 4 && updated?.size() <= 8 && updated != current)
			{
				def cmds = setCode(n, updated)
				set_cmds << cmds.first()
				get_cmds << cmds.last()
			}
			else if ((current && updated == "") || updated == "0")
			{
				def cmds = deleteCode(n)
				set_cmds << cmds.first()
				get_cmds << cmds.last()
			}
			else if (updated && (updated.size() < 4 || updated.size() > 8))
			{
				// Entered code was too short or too long
				codeSettings["code$n"] = current
			}
		}
		else log.warn("unexpected entry $name: $updated")
	}
	if (set_cmds)
	{
		return response(delayBetween(set_cmds, lockDelayMs) + ["delay $lockDelayMs"] + delayBetween(get_cmds, defaultDelayMs))
	}
}

def generateUuid()
{
	var uuid = randomUUID() as String
	return uuid
}

def setCode(int slotNum, code)
{
	setCode(slotNum, code, null, null)
}

def setCode(int slotNum, code, String slotName)
{
	setCode(slotNum, code, slotName, null)
}

def setCode(int slotNum, code, String slotName, String slotUuid)
{
	log.debug "Executing 'setCode'for slot $slotNum with code $code"
	if (!validateSlotNumber(slotNum))
	{
		log.debug "invalid slot number $slotNum. Do something clever here"
		return // need better error handling - invalid slot #
	}

	if (slotName == null)
	{
		slotName = slotNum.toString()
	}

	if (slotUuid == null)
	{
		slotUuid = generateUuid()

	}

	String codeStr
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
			log.warn "code '$codeStr' is invalid. Better do something clever to handle this"
			return // need better error handling - invalid code
		}
	}

	// check for this codestr as a value in codeDb already
	setCodeDbPendingSlot(slotNum, codeStr, slotName, slotUuid)
	// set, then get the code slot so that the state is updated.
	secureSequence([
		zwave.userCodeV1.userCodeSet(userIdentifier:slotNum, userIdStatus:UserCodeSet.USER_ID_STATUS_OCCUPIED, user:code),
		zwave.userCodeV1.userCodeGet(userIdentifier:slotNum)
	], setGetDelayMs)
}

// convert string version of code to a list of Short ASCII values
def codeStrToShortList(String codeStr)
{
	return codeStr.toList().findResults { if(it > ' ' && it != ',' && it != '-') it.toCharacter() as Short }
}

// convert code as list ASCII Short ints to a string
def codeShortListToStr(code)
{
	return code.collect{ it as Character }.join()
}

def deleteCode(int slotNumber)
{
	log.debug "Executing 'deleteCode'"
	log.debug "deleting code $slotNumber"
	if (validateSlotNumber(slotNumber))
	{
		// mark the slot as available is how to delete it
		secureSequence([
				zwave.userCodeV1.userCodeSet(userIdentifier:slotNumber, userIdStatus:UserCodeSet.USER_ID_STATUS_AVAILABLE_NOT_SET ),
				zwave.userCodeV1.userCodeGet(userIdentifier:slotNumber)
		], setGetDelayMs)
	}
	else
	{
		log.warn "'$slotNumber' is not a valid slot number (1-249)"
	}
}

def requestCode(int slotNumber) // ask device for the code in a given slot number
{
	if (!state.codeDb || !state.pendingCodeDb) initState()
	log.debug "Executing 'requestCode'"
	if (validateSlotNumber(slotNumber))
	{
		// get the current slot entry from the db and pend it or make a placeholder entry
		state.pendingCodeDb[slotNumber] = [name: slotNumber.toString()]
		if (state.codeDb.containsKey(slotNumber))
		{
			state.pendingCodeDb[slotNumber].putAll(state.codeDb[slotNumber])
		}
		return secure(zwave.userCodeV1.userCodeGet(userIdentifier:slotNumber))
	}
	else
	{
		log.warn "'$slotNumber' is not a valid slot number (1-249)"
	}
}

def zwaveEvent(physicalgraph.zwave.commands.usercodev1.UserCodeReport cmd)
{
	log.debug "handling event usercodev1.UserCodeReport for slot $cmd.userIdentifier with code $cmd.code"
	initState()
	def result = []
	//def name = "code$cmd.userIdentifier"
	def slotNum = cmd.userIdentifier
	String codeStr = cmd.code
	//string strCode = codeShortListToStr(code)
	// get slot map from codeDb if it is there
	def codeSlot = state.codeDb.get(slotNum, [:])
	log.debug "checking $slotNum and $codeStr"

		// handle the only two valid states for the Yale lock
	if (cmd.userIdStatus == UserCodeReport.USER_ID_STATUS_OCCUPIED)
	{
		// code has been set into the slot number identified by cmd.userIdentifier
		log.debug "slot is reported as OCCUPIED by device"
		commitCodeDbSlot(slotNum, codeStr) // commit it into the local code DB
		def eventMap = [ name: "codeReport", value: slotNum, data: [ code: codeStr ] ]
		eventMap.descriptionText = "$device.displayName code $cmd.userIdentifier is set"
		eventMap.displayed = (slotNum != state.get("currentReloadSlot",0) && slotNum != state.get("currentPollSlot", 0))
		eventMap.isStateChange = (codeStr != codeSlot.get("code", null))
		result << createEvent(eventMap)
	}
	else
	{
		// code has been deleted, slot is now empty
		log.debug "slot is reported as AVAILABLE by device"
		def eventMap = [ name: "codeReport", value: slotNum, data: [ code: "" ] ]
		if (state[name]) {
				eventMap.descriptionText = "$device.displayName code slot $slotNum was deleted"
		} else {
				eventMap.descriptionText = "$device.displayName code slot $slotNum is not set"
		}
		eventMap.displayed = (slotNum != state.get("currentReloadSlot",0) && slotNum != state.get("currentPollSlot", 0))
		eventMap.isStateChange = codeSlot.code as Boolean
		result << createEvent(eventMap)
		removeCodeDbSlot(slotNum) // now remove the code from the local code db
	}
	log.debug "before: slot $slotNum; reload ${state.get('currentReloadSlot')}; poll ${state.get('currentPollSlot')}"
	if (state.currentReloadSlot && slotNum == state.currentReloadSlot) // reloadAllCodes() was called, keep requesting the codes in order
	{
		if (state.currentReloadSlot + 1 > state.maxSlotNum)
		{
			log.info "finished reload of local codeDb with slot $state.currentReloadSlot"
			state.remove("currentReloadSlot")	// done
		}
		else
		{
			state.currentReloadSlot = state.currentReloadSlot + 1
			log.info "continuing reload with a request of next slot: $state.currentReloadSlot of $state.maxSlotNum"
			result << response(requestCode(state.currentReloadSlot)) // get next
		}
	}
	if (state.currentPollSlot && slotNum == state.currentPollSlot)
	{
		if (state.currentPollSlot + 1 > state.maxSlotNum)
		{
			log.info "finished polling with slot $state.currentPollSlot"
			state.remove("currentPollSlot")	// done
		}
		else
		{
			// next poll interval will pull next code in turn
			state.currentPollSlot = state.currentPollSlot + 1
		}
	}
	log.debug "after: slot $slotNum; reload ${state.get('currentReloadSlot')}; poll ${state.get('currentPollSlot')}"
	log.debug "code report parsed to ${result.inspect()}"
	return result
}

def requestUsersNumber(boolean secureCommand=true)
{
	log.debug "Executing 'UsersNumberGet'"
	def cmd = zwave.userCodeV1.usersNumberGet()
	if (secureCommand) cmd = secure(cmd)
	return cmd
}

def zwaveEvent(UsersNumberReport cmd)
{
	log.debug "handling event usercodev1.UsersNumberReport"
	def result = []
	log.debug(cmd)
	log.debug("$cmd.supportedUsers supported users")
	state.maxSlotNum = cmd.supportedUsers // record in the state
	if (state.currentReloadSlot && state.currentReloadSlot == 1)
	{
		log.info "now begin reloadAllCodes with slot $currentReloadSlot of $state.maxSlotNum"
		result << response(requestCode(state.currentReloadSlot))
	}
	return result
}

def getCode(slotNumber) // from local data
{
	initState()
	state.codeDb[slotNumber]
}

def getAllCodes() // from local data
{
	log.debug "Executing 'getAllCodes'"
	initState()
	return state.codeDb
}

// request all codes from the lock in order to have an accurate local database
// do this sparingly!
def reloadAllCodes()
{
	log.debug "Executing 'reloadAllCodes'"
	def cmds = []
	if (!state.codeDb || !state.maxCodeSlot)
	{
		log.info "kicking off reloadAllCodes with slot 1, but need to initialize codeDb or find how many codes the device supports first"
		initState()
		state.currentReloadSlot = 1
		cmds << requestUsersNumber()
	}
	else
	{
		log.info "kicking off reloadAllCodes with slot 1 of $state.maxSlotNum"
		if(!state.currentReloadSlot) state.currentReloadSlot = 1
		cmds << requestCode(state.currentReloadSlot)
	}
	return cmds
}

//codeDb assist methods
def buildDbEntry(int slotNumber, String codeStr, String slotName, String uuid)
{
	if (slotName == null) slotName = slotNumber.toString()
	if (uuid == null) uuid = generateUuid()
	return [code: codeStr, name: slotName, uuid: uuid]
}

def setCodeDbPendingSlot(int slotNumber, String codeStr, String slotName, String uuid)
{
	if (slotName == null || slotName == '')
	(
		slotName = slotNumber.toString()
	)
	state.pendingCodeDb[slotNumber] = buildDbEntry(slotNumber, codeStr, slotName, uuid)
}

def setCodeDbPendingSlot(int slotNumber, String code)
{
	// when slot name is not provided, use slot number as slot name
	string slotName = state.codeDb.containsKey(slotNumber) ? state.codeDb.get(slotNumber).name : slotNumber.toString()
	setCodeDbPendingSlot(slotNumber, code, slotNumber.toString(), null)
}

// called on a UserCodeReport
def commitCodeDbSlot(int slotNumber, String codeStr)
{
	// get the existing entry, or create a slot named for its number if there isn't one
	def newEntry = state.codeDb.get(slotNumber, [name: slotNumber])

	// get the pending entry, if one, or use a blank map.
	def pendingEntry = state.pendingCodeDb.get(slotNumber, [:])
	if (pendingEntry.code && pendingEntry.code != codeStr)
	{
		log.warn "pending code $pendingEntry.code doesn't match code in the UserCodeReport, $codeStr"
	}
	pendingEntry.code = codeStr

	// layer pending entry onto any existing info and insert it
	newEntry.putAll(pendingEntry)
	log.debug "setting codeDb slot $slotNumber to ${newEntry.inspect()}"
	state.codeDb[slotNumber] = newEntry

	// remove pending entry if necessary
	if (state.pendingCodeDb[slotNumber]) state.pendingCodeDb.remove(slotNumber)
}

def removeCodeDbSlot(int slotNumber)
{
	if (state.codeDb[slotNumber]) state.codeDb.remove(slotNumber)
	if (state.pendingCodeDb[slotNumber]) state.pendingCodeDb.remove(slotNumber)
}

def findFirstEmptyCodeDbSlot()
{
	int emptySlotNum = -1
	for (int currSlot = minSlotNum; currSlot <= state.maxSlotNum; currSlot++)
	{
		if (!state.codeDb[currSlot])
		{
			emptySlotNum = currSlot
			break
		}
	}
	if (emptySlotNum >= minSlotNum)
	{
		return emptySlotNum
	}
	return null
}

def clearCodeDb()
{
	initState()
	state.codeDb.clear()
	state.pendingCodeDb.clear()
}

private long timestampToMillis(timestamp)
{
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

private Boolean timePast(timestamp, hours=0, minutes=0, seconds=0)
{
	timestamp = timestampToMillis(timestamp)
	long millisPast = (hours * mSecondsPerHour) + (minutes * 60000) + (seconds * 1000)
	return (new Date().time - timestamp) > millisPast
}

def doAdHoc()
{
	log.debug "executing doAdhoc"
	requestMsr()
}


// configuration mappings
@Field final configAttributeMap = [
	1: "audio_mode",
	2: "relock_auto",
	3: "relock_secs",
	4: "wrong_code_entry_limit",
	5: "language",
	7: "wrong_code_entry_lockout_secs",
	8: "op_mode",
	11: "touch_to_lock", //??
	12: "privacy_button", //??
	13: "status_led"] //??

@Field final configMetadata = [
	"audio_mode": [
		"default": 3 ],
	"relock_auto": [
		"default": 255 ],
	"relock_secs": [
		"min": 5,
		"max": 255,
		"default": 30 ],
	"wrong_code_entry_limit": [
		"min": 1,
		"max": 7,
		"default": 5],
	"language": [
		"default": 1 ],
	"wrong_code_entry_lockout_secs": [
		"min": 1,
		"max": 255,
		"default": 30 ],
	"op_mode": [
		"default": 0 ],
	"touch_to_lock": [
		"default": 0 ],
	"privacy_button": [],
	"status_led": []
]
