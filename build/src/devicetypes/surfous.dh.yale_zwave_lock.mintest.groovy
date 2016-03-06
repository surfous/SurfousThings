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
*
* #TODO: create way to register callback Get/Report pairs.
* #TODO: When a Get is sent, set the corresponding Report as the expect with a reasonable timeout.
* 			Allow callback commands to be registered against the expect such that they are serialzed to state.
* 		 	Does not support differentiation of multiple expects for the same command - figure out something clever here.
* #TODO: parse calls callbax expect handler hook on all commands.
* #TODO: Simplify fingerprinting to list of expected auto-reports. Register an expect for each with timeouts
* 			without sending the commands so that they are reconciled as they come in and also get
* 			resent if they don't come in.
* #TODO: create combination expects so that all reports must come in to satisfy the condition and run the callback
* 			use a stock command to each individual expect to signal completion of their own part of the
* 			combo expect.
* #TODO: expect handler must call maintain method to check for expects that have timed out.
* 			Resend the get command(s) and reset the expect timeout
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
		capability "Sensor"
		capability "Actuator"

		command "runAdHocTest"
		command 'setAutoRelockDelay'

		attribute "blank", "string" //just as the name says...
		attribute "usersNumber", "number"

		attribute "cfgAutoRelockDelay", "number"
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
		standardTile('adHoc', 'device.lock', inactiveLabel: false, decoration: 'flat') {
			state 'default', label:'', action:'runAdHocTest', icon:'st.secondary.test'
		}

/*
		standardTile('autoRelock', 'device.cfgAutoRelock', inactiveLabel: false, decoration: 'flat')
		{
			state 'OFF', label: 'AUTO RELOCK ${name}', action:'cycleAutoRelock', icon:'st.tesla.tesla-unlocked', nextState:'changing'
			state 'ON', label: 'RELOCK IN ${currentValue}s', action:'cycleAutoRelock', icon:'st.tesla.tesla-locked', nextState:'changing'
			state 'changing', label: '...', action:'cycleAutoRelock', icon:'st.unknown.thing.thing-circle'
		}
*/
		controlTile('autoRelockDelaySlider', 'device.cfgAutoRelockDelay', 'slider', inactiveLabel: false, height: 1, width: 2, range:"(5..255)")
		{
			state 'delay', label:'Auto Relock Delay', action:'device.setAutoRelockDelay', backgroundColor:'#ff0000', defaultState: true
		}
		valueTile('autoRelockDelay', 'device.cfgAutoRelockDelay', decoration: "flat")
		{
			state 'delay', label:'${currentValue} sec.', defaultState: true
		}

		//main 'lockMain'
		//details(['lock', 'lockAction', 'unlockAction', 'battery', 'opMode', 'refresh', 'adHoc'])
		details(['autoRelockDelaySlider', 'autoRelockDelay', 'adHoc'])
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
@Field final int MSEC_PER_SEC = 1000
@Field final int SEC_PER_MIN = 60
@Field final int SEC_PER_HOUR = 3600

@Field final int FINGERPRINT_TIMEOUT_SEC = 90

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

@Field final def COMMAND_CLASS_VERSIONS = [0x98: 1, 0x8b: 1, 0x8a: 2, 0x85: 1, 0x86: 1, 0x80: 1, 0x75: 2, 0x72: 1, 0x71: 1, 0x70: 1, 0x63: 1, 0x62: 1, 0x4e: 3, 0x4c: 1]
// Security, Time Parameters, Time, Version, Battery, Protection, Manufacturer Specific, Noticifation, Configuration, User Code, Door Lock, Schedule Entry Lock, Door Lock Logging
@Field final short CONFIG_VALUE_BYTE_SIZE = 1 // All parameter values of the Yale lock are 1 byte

@Field final List INCLUDE_AUTO_QUERY_REPORTS = ['0x9807', '0x7205', '0x9803', '0x8613']
@Field final List FOLLOWUP_QUERY_CMDS = []

@Field Map smartconfig

@Field final String zweh = 'Z-WaveEventHandler' // For handlers of events sent by the device itself
@Field final String dti = 'DeviceTypeInternal' // for commands that are automatically called in a device type's lifecycle
@Field final String ccmd = 'STDeviceCommand' // capability or standalone command
@Field final String ccc = 'CommandClassCommand' // wraps a single command class

@Field pendingSlotNameMap = [:]

// handle system events

// called after a device instance is first installed. This seems to only happen in the simulator
def installed()
{
	log.trace 'called device-type installed method'
}


// called when device preferences are saved in the ST app
def updated()
{
	log.trace 'updated'
}

// generic parser for a z-wave event description, returning an ST event
def parse(String rawZwaveEventDescription)
{
	log.trace "in parse with arg $rawZwaveEventDescription"
	def result = null

	if (rawZwaveEventDescription.startsWith('Err'))
	{
		log.debug 'rawZwaveEventDescription contains an error'
		if (state?.secureKeyVerified)
		{
			// We have a security key, so the error isn't security related
			result = createEvent(descriptionText:rawZwaveEventDescription, displayed:true)
			log.warn 'error is not security related'
		}
		else
		{
			// It's a security error
			result = createEvent(
				descriptionText: 'This lock failed to complete the network security key exchange. If you are unable to control it via SmartThings, you must remove it from your network and add it again.',
				eventType: 'ALERT',
				name: 'secureInclusion',
				value: 'failed',
				displayed: true,
			)
			log.error result.descriptionText
		}
	}
	else if (rawZwaveEventDescription == 'updated')
	{
		// do nothing - handles rogue invocation of parse with the arg set simply to 'updated'
	}
	else
	{
		def cmd = zwave.parse(rawZwaveEventDescription, COMMAND_CLASS_VERSIONS)
		log.trace "in parse with decrypted command $cmd"
		if (cmd)
		{
			log.trace 'parse is handing off to specific zwaveEvent handler overload method'
			result = zwaveEvent(cmd)
		}
		// execute any callbacks here
		try
		{
			Callbax().handle(rawZwaveEventDescription)
		}
		catch (Exception ex)
		{
			log.error "Callbax handler threw an unexpected exception: $ex"
		}
	}
	return result
}

// callback liasons
def registerReportCallback(physicalgraph.zwave.Command zwaveCommand, physicalgraph.zwave.Command expectedReport, callback)
{
	String rawCmd = zwaveCommand.format()

}

// command to test out various commands
def runAdHocTest()
{
	log.trace 'func runAdHocTest'
	physicalgraph.zwave.Command outcmd = zwave.manufacturerSpecificV1.manufacturerSpecificGet()
	log.debug "${outcmd.commandClassIdentifier} ${outcmd.commandIdentifier}"
}

def setAutoRelockDelay(seconds)
{
	Map evt = [:]
	evt.name = 'cfgAutoRelockDelay'
	evt.value = seconds
	evt.unit = 'seconds'
	evt.displayed = false
	sendEvent(createEvent(evt))
}

// capability.Polling
def poll()
{
	log.trace 'capability command Polling.poll'
	Callbax().maintain()
}

// capability.Configuration
def configure()
{
	log.trace "Executing 'capability.Configuration.configure'"
	Callbax().beginFingerprintMonitoring()
	return null
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
	log.warn "Handling event: unhandled or unexpected zwave command $deviceEvent"
	return createEvent(displayed: false, descriptionText: "$device.displayName: $deviceEvent")
}


// CommandClass Security 0x98

// Parses a secured z-wave event, returning the unencrypted event contained within
def zwaveEvent(physicalgraph.zwave.commands.securityv1.SecurityMessageEncapsulation secureDeviceEvent)
{
	log.trace "handling event securityv1.SecurityMessageEncapsulation. Opening encapsulated message."
	String commandClassIdentifier = formatOctetAsHex(secureDeviceEvent.commandClassIdentifier as Byte)
	log.debug "Secure message command is class: $commandClassIdentifier; command: $secureDeviceEvent.commandIdentifier"
	def unencapsulatedCommand = secureDeviceEvent.encapsulatedCommand(COMMAND_CLASS_VERSIONS)
	if (unencapsulatedCommand)
	{
		log.debug "unpacked secure message from $device: $unencapsulatedCommand"
		return zwaveEvent(unencapsulatedCommand)
	}
	else
	{
		log.warn "The command we unpacked was null... The SecurityMessageEncapsulation object instance was: $secureDeviceEvent"
	}
}

def zwaveEvent(physicalgraph.zwave.commands.securityv1.NetworkKeyVerify deviceEvent)
{
	log.trace "handling event securityv1.NetworkKeyVerify $deviceEvent"
	state.secureKeyVerified = true
	//def resQ = ResponseQueue()
	//resQ.add(createEvent(name:'secureInclusion', value:'success', descriptionText:'Secure inclusion was successful'))
	//resQ.add(bootstrapCommands())
	return createEvent(name:'secureInclusion', value:'success', descriptionText:'Secure inclusion was successful')
}

// #REG [c: '0x9802', r: '0x9803', method: 'ccSecurityCommandsSupportedGet']
// 0x9802
def ccSecurityCommandsSupportedGet(String callbackMethod=null)
{
	log.trace "Executing zwave.securityV1.securityCommandsSupportedGet"
	if (callbackMethod)
	{
		registerCallbackOnReport(physicalgraph.zwave.commands.securityv1.SecurityCommandsSupportedReport,
			callbackMethod)
	}
	return secure(zwave.securityV1.securityCommandsSupportedGet())
}

	* registerCallbackOnReport
	* doesReportHaveRegisteredCallback
	* runRegisteredCallbackMethod
	* maintainRegisteredCallbacks ( resend raw zwave cmd if timed out)

// 0x9803
def zwaveEvent(physicalgraph.zwave.commands.securityv1.SecurityCommandsSupportedReport cmd)
{
	log.trace "handling event securityv1.SecurityCommandsSupportedReport"
	state.sec = cmd.commandClassSupport.collect { formatOctetAsHex(it as Byte) + ' ' }.join()
	if (cmd.commandClassControl)
	{
		state.secCon = cmd.commandClassControl.collect { formatOctetAsHex(it as Byte) + ' ' }.join()
	}
	log.debug "Command Classes requiring security: $state.sec"

	if (doesReportHaveRegisteredCallback(cmd))
	{
		List callbackCmds = runRegisteredCallbackMethod(cmd)
	}
	return createEvent(name:"secureInclusion", value:"success", descriptionText:"Lock is securely included")
}

// helper to securely encapsulate a Z-wave command before sending it
def secure(physicalgraph.zwave.Command cmd)
{
	log.trace "Encapsulating: $cmd"
	def secureCmd = zwave.securityV1.securityMessageEncapsulation().encapsulate(cmd).format()
	return secureCmd
}

// encapsulate a series of commands separated by the specified delay
def secureSequence(commands, delay=DEFAULT_DELAY_MSEC)
{
	return delayBetween(commands.collect{ secure(it) }, delay)
}


// CommandClass Alarm v1
// alarm reports are how the lock conveys lock/unlock events, faults, tampering and code sets
def zwaveEvent(physicalgraph.zwave.commands.alarmv1.AlarmReport deviceEvent)
{
	log.trace "handling alarmv1.AlarmReport event"
	log.debug "alarm type: $deviceEvent.alarmType, alarm level: $deviceEvent.alarmLevel"
}


// CommandClass Association v2 0x85
// This should be v1, but a bug in the ST Zwave utility class necessitates using v2 as a workaround
def ccAssociationSet()
{
	smartlog(ccc, LEVEL_FINE, "setting $device.displayName association to groupingIdentifier $ASSOCIATION_GROUPING_ID for nodeId $zwaveHubNodeId")
	return secure(zwave.associationV1.associationSet(groupingIdentifier:ASSOCIATION_GROUPING_ID, nodeId:zwaveHubNodeId))
}

def ccAssociationRemove()
{
	smartlog(ccc, LEVEL_FINE, "removing $device.displayName association to groupingIdentifier $ASSOCIATION_GROUPING_ID for nodeId $zwaveHubNodeId")
	state.assoc = null // appropriate?
	return secure(zwave.associationV1.associationRemove(groupingIdentifier:ASSOCIATION_GROUPING_ID, nodeId:zwaveHubNodeId))
}

// #REG [c: '0x8502', r: '0x8503', method: 'ccAssociationGet']

def ccAssociationGet()
{
	smartlog(ccc, LEVEL_FINE, "getting $device.displayName association for groupingIdentifier $ASSOCIATION_GROUPING_ID")
	return secure(zwave.associationV1.associationGet(groupingIdentifier:ASSOCIATION_GROUPING_ID))
}

def zwaveEvent(physicalgraph.zwave.commands.associationv1.AssociationReport deviceEvent)
{
	log.trace "handling event associationv1.associationReport"
	def result = []
	if (deviceEvent.nodeId.any { it == zwaveHubNodeId })
	{
		log.info "Hub $zwaveHubNodeId is associated with $device.displayName association group $deviceEvent.groupingIdentifier"
		state.assoc = zwaveHubNodeId
		state.lastAssocQueryTime = new Date().time
	}
	return result
}

// CommandClass Manufacturer Specific v1 0x72

// #REG [c: '0x7204', r: '0x7205', method: 'ccManufacturerSpecificGet']

def ccManufacturerSpecificGet()
{
	log.trace 'ccManufacturerSpecificGet'
	def cmd = zwave.manufacturerSpecificV1.manufacturerSpecificGet()
	log.debug "mfr specific get: $cmd"
	return cmd
}

def zwaveEvent(physicalgraph.zwave.commands.manufacturerspecificv1.ManufacturerSpecificReport deviceEvent)
{
	log.trace "handling event manufacturerspecificv1.ManufacturerSpecificReport"
	log.debug "msr: $deviceEvent"
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

	//def sc = SmartConfig()
	//def resQ = ResponseQueue()

	def niceMsr = "$device.displayName MSR: $msr; Manufacturer: $state.msr.manufacturer; Model: $state.msr.model; Generation: $state.msr.generation"
	if (!state.config)
	{
		//resQ.add(getAllConfigParameters(), SHORT_DELAY_MSEC)
	}
	return createEvent(description: state.msr.toString(), descriptionText: niceMsr, isStateChange: false)
	//return resQ.getWrappedCommands()
}

// CommandClass Battery v1 0x80
// #REG [c: '0x8002', r: '0x8003', method: 'ccBatteryGet']
def ccBatteryGet()
{
	log.trace "battery.ccBatteryGet"
	def cmd = zwave.batteryV1.batteryGet()
	log.debug "battery get: $cmd.format()"
	return secure(cmd)
}

def zwaveEvent(physicalgraph.zwave.commands.batteryv1.BatteryReport deviceEvent)
{
	log.trace "handling event batteryv1.BatteryReport"
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
	state.lastBatteryCheckTime = new Date().time
	return createEvent(map)
}

// command class and command ID helpers (they're represented as bytes in hex)

private String formatByteArrayAsHex(byte[] bytes)
{
	return "0x${bytes.encodeHex()}"
}

private String formatOctetAsHex(Byte octet)
{
	byte[] singleByteArray = [octet]
	return formatByteArrayAsHex(singleByteArray)
}

private String formatOctetAsHex(String hexOctetStr)
{
	if (!hexOctetStr) return null
	byte[] singleByteArray = parseHexStringAsByteArray(hexOctetStr)
	return formatByteArrayAsHex(singleByteArray)
}

// NOTE! Byte values greater then 127d will display as negative numbers.
private byte[] parseHexStringAsByteArray(String hexStr)
{
	String hexStrNoRadix = hexStr.replaceFirst('0x', '')
	return hexStrNoRadix.decodeHex()
}

// epochTime is the number of seconds elapsed since Jan 1, 1970 00:00:00 GMT
def EpochTime()
{
	Map et = [:]

	et.now =
	{
		return et.fromDate(new Date())
	}

	et.fromDate =
	{
		Date d->
		// remember, we return seconds, not milliseconds
		return d.getTime()/MSEC_PER_SEC as Long
	}

	et.dateFromEpochTime =
	{
		Long epochTimeInSeconds->
		return new Date(epochTimeInSeconds * MSEC_PER_SEC)
	}

	et.hasTimePassed =
	{
		sinceTime, Integer hours=0, Integer minutes=0, Integer seconds=0->
		Long pastEpochTime
		if (sinceTime instanceof Date) pastEpochTime = et.fromDate(sinceTime)
		else if (sinceTime instanceof Number) pastEpochTime = sinceTime as Long
		else
		{
			log.error 'sinceTime is neither Date nor Number. returning false'
			return false
		}
		log.trace "func hasTimePassed ${hours}h ${minutes}m ${seconds}s since $pastEpochTime"
		long deltaSeconds = (hours * SEC_PER_HOUR) + (minutes * SEC_PER_MIN) + (seconds)
		return (et.now() - pastEpochTime) >= deltaSeconds
	}

	return et
}

/**
 * Handle fingerprinting phase of device installation and then regular callback processing
 * Basically, we wish to not make any device calls until fingerprinting is complete.
 * We may have calls we want to make immediately after fingerprinting.
 * After this, we may, at the time we issue a command, specify another command to run when the
 * expected report comes in. However, if the report does not come in a reasonable time, we will bail
 * on the success callback and instead run any fail callback.
 */
// Info Frames for YRD incude
// 0x98 NetworkKeyVerify
// 0x98 SecurityCommandsSupportedReport
// 0x72 ManufacturerSpecificReport
// 0x86 VersionReport
def Callbax()
{
	Map cx = [:]
	cx.type = 'Callbax'
	cx.CALLBACK = 'callback'
	cx.FINGERPRINT = 'fingerprint'
	cx.validHandlers = [cx.CALLBACK, cx.FINGERPRINT] as Set

	cx.initialize =
	{
		log.trace 'func initialize'
		if (state?.callbax == null)
		{
			log.debug 'init callbax state'
			state.callbax = [:]
		}
		cx.checkValidHandler()
		if (state.callbax?.fp == null)
		{
			log.trace 'init callbax.fp'
			state.callbax.fp = [isComplete: true]
		}
		else if ( !(state.callbax.fp.isComplete instanceof Boolean) )
		{
			log.debug 'callbax.fp.isComplete is corrupt - reset to true'
			state.callbax.fp.isComplete = true
		}
		log.trace "state.callbax after init: ${state.callbax}"
	}

	cx.checkValidHandler =
	{
		if (state.callbax?.handler == null || !cx.validHandlers.contains(state.callbax?.handler))
		{
			cx.setCallbackHandler()
		}
	}

	cx.setCallbackHandler =
	{
		state.callbax.handler = cx.CALLBACK
	}

	cx.setFingerprintHandler =
	{
		state.callbax.handler = cx.FINGERPRINT
	}

	/**
	 * Only register Get commands here - there is no safety checking.
	 * Report id is always cmd id + 1
	 */
	cx.registerCallbackGetCmd =
	{
		String getCmdMethodName, def getCmd->

	}

	cx.registerCallbackReportCmd =
	{
		String getCmdMethodName, def reportCmd->

	}

	cx.maintain =
	{
		cx.checkValidHandler()
		return cx."maintain${state.callbax.handler.capitalize()}"
	}

	cx.maintainFingerprint
	{
		log.trace "callbax maintainFingerprint"
		cx.isFingerprintingComplete()
	}

	cx.maintainCallback
	{
		log.trace "callbax maintainCallback"
		return null
	}

	cx.handle =
	{
		String rawZwaveEvent->
		cx.checkValidHandler()
		return cx."handle${state.callbax.handler.capitalize()}"(rawZwaveEvent)
	}

	cx.handleFingerprint =
	{
		String rawZwaveEvent->
		log.trace "handleFingerprint $rawZwaveEvent"
		return cx.checkDeviceEventAgainstFingerprint(rawZwaveEvent)
	}

	cx.handleCallback =
	{
		String rawZwaveEvent->
		log.trace "handleCallback $rawZwaveEvent"
		return null
	}

	// Fingerprint methods
	cx.beginFingerprintMonitoring =
	{
		log.trace "func beginFingerprintMonitoring"
		state.callbax = null // nuke any callbax state
		state.remove('callbax')
		cx.initialize()
		cx.parseRawDescriptionForFingerprint()
		if (state.callbax?.fp?.in == null)
		{
			// no fingerprint to watch for - end fingerprinting
			cx.endFingerprintMonitoring()
		}
		else
		{
			cx.setFingerprintHandler()
			state.callbax.fp.startTime = EpochTime().now()
		}
		return null
	}

	cx.endFingerprintMonitoring =
	{
		log.trace "func endFingerprintMonitoring"
		cx.setCallbackHandler()
		state.callbax.fp.isComplete = true
	}

	cx.isFingerprintingComplete =
	{
		log.trace 'func isFingerprintingComplete'
		cx.initialize()
		if (state.callbax.fp.isComplete == true)
		{
			log.warn 'fingerprinting now complete because the status value in the state db is set to true'
			cx.endFingerprintMonitoring()
		}
		else if (EpochTime().hasTimePassed(state.callbax.fp?.startTime?:0, 0, 0, FINGERPRINT_TIMEOUT_SEC))
		{
			log.warn "fingerprinting timed out - incomplete after $FINGERPRINT_TIMEOUT_SEC seconds"
			cx.cleanupFingerprintMonitoring()
		}
		log.debug "fingerprint completion state var is $state.callbax.fp.isComplete"
		return state.callbax.fp.isComplete
	}

	// find out which expected commands have not completed and start the query phase with them.
	cx.cleanupFingerprintTimeout =
	{
		log.debug "incoming command class ids expected: $state.callbax.fp.in"
		log.debug "incoming command class ids received: $state.callbax.fp.inSeen"

		cx.endFingerprintMonitoring()
	}

	cx.checkDeviceEventAgainstFingerprint =
	{
		String rawZwaveEvent->
		log.trace 'func checkDeviceEventAgainstFingerprint'
		String classAndCmdId = cx.parseZwaveCommandId(rawZwaveEvent)

		if (!cx.isFingerprintingComplete() && (classAndCmdId != null))
		{
			String classId = classAndCmdId.substring(0,4)
			log.debug "Checking if we've seen class id $classId for fingerprinting yet"
			if (state.callbax.fp.in.contains(classId))
			{
				if (!state.callbax.fp.inSeen.contains(classId)) state.callbax.fp.inSeen.add(classId)
				log.debug "${state.callbax.fp.inSeen.size()} of ${state.callbax.fp.in.size()} expected command classes of device fingerprint have been seen"
				log.debug "in:     ${state.callbax.fp.in}"
				log.debug "inSeen: ${state.callbax.fp.inSeen}"
				if (state.callbax.fp.inSeen.containsAll(state.callbax.fp.in))
				{
					log.info 'all expected incoming command class ids have been seen - fingerprinting is complete'
					cx.endFingerprintMonitoring()
				}
			}
		}
		return null
	}

	// parsing methods
	cx.parseRawDescriptionForFingerprint =
	{
		log.trace 'func parseRawDescriptionForFingerprint'
		String rawDesc = device.device.rawDescription
		def fingerprintMatcher = (rawDesc =~ /(?:\d+\s){2}0x....(?:\s\d)+\s(\d+)\s((?:0x..\s?)+)(?:(\d+)\s((?:0x\w\w\s?)+))?$/)
		try
		{
			state.callbax.fp = [isComplete: false, inCount: 0, in: [], inSeen: [], outCount: 0, out: []]
			if (fingerprintMatcher[0][2])
			{
				String inClusterString = fingerprintMatcher[0][2]
				state.callbax.fp.inCount = fingerprintMatcher[0][1]
				inClusterString.tokenize().each { state.callbax.fp.in << formatOctetAsHex(it) }
			}
			if (fingerprintMatcher[0][4])
			{
				String outClusterString = fingerprintMatcher[0][4]
				state.callbax.fp.outCount = fingerprintMatcher[0][3]
				outClusterString.tokenize().each { state.callbax.fp.out << formatOctetAsHex(it) }
			}
			log.info "fingerprint parsing found in clusters $state.callbax.fp.in and out clusters $state.callbax.fp.out"
		}
		catch (IndexOutOfBoundsException ex)
		{
			// no fingerprint to monitor
			log.error ex
			log.warn "fingerprint parsing failed with raw description $rawDesc"
			state.callbax.fp = [isComplete: true]
		}
	}

	cx.parseZwaveCommandId =
	{
		String rawZwaveEvent->
		log.trace 'func parseZwaveCommandId'
		String classAndCmdId
		def ccIdMatcher = (rawZwaveEvent =~ /command:\s(\d\d\d\d),\spayload:\s\d\d\s(\d\d\s\d\d)\s?/)
		try
		{
			log.debug "matcher result: ${ccIdMatcher[0]}"
			classAndCmdId = ccIdMatcher[0][1]
			if (classAndCmdId == '9881')
			{
				// Command is encapsulated. Look at the second and third octets of the payload instead
				classAndCmdId = ccIdMatcher[0][2]
				classAndCmdId = classAndCmdId.replaceAll(/\s/, '') // nuke that space, though
			}
		}
		catch (IndexOutOfBoundsException ex)
		{
			// no fingerprint to monitor
			log.warn ex
			log.warn "failed to parse command class id and command id from $rawZwaveEvent"
			return null
		}
		catch (Exception ex)
		{
			log.error ex
			log.error 'Unexpected exception in parseZwaveCommandId'
		}
		// format them nicely as a hexadecimal octet strings
		return (classAndCmdId == null)?:formatOctetAsHex(classAndCmdId)
	}

	cx.parseZwaveCommand =
	{
		String formattedZwaveCommand->
		String classAndCommandIdentifiers = formattedZwaveCommand.substring(0,4)
		String cmdAndClassId = formatOctetAsHex(classAndCommandIdentifiers)
		return cmdAndClassId
	}

	cx.initialize()

	return cx
}

// BEGIN smartlog
@Field final String LEVEL_NONE = null
@Field final String LEVEL_ERROR = 'error'
@Field final String LEVEL_WARN = 'warn'
@Field final String LEVEL_INFO = 'info'
@Field final String LEVEL_DEBUG = 'debug'
@Field final String LEVEL_TRACE = 'trace'
@Field final String LEVEL_FINE = 'FINEtrace'

@Field final List SMARTLOG_LEVELS = [LEVEL_NONE, LEVEL_ERROR, LEVEL_WARN, LEVEL_INFO, LEVEL_DEBUG, LEVEL_TRACE, LEVEL_FINE]
@Field final String SMARTLOG_DEFAULT_LEVEL = LEVEL_DEBUG

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
		String loglevel = level.replaceAll('[A-Z]', '')
		if (scope) msg = "[$scope] $msg"
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
// END smartlog
