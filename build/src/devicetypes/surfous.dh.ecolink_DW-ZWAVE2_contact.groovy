/**
 *  Ecolink DW-ZWAVE2 Contact Sensor device handler
 *  Copyright ©2015 Kevin Shuk (surfous)
 *
 *	This program is free software: you can redistribute it and/or modify
 *	it under the terms of the GNU Affero General Public License as published
 *	by the Free Software Foundation, either version 3 of the License, or
 *	(at your option) any later version.
 *
 *	This program is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *	GNU Affero General Public License for more details.
 *
 *	You should have received a copy of the GNU Affero General Public License
 *	along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *  Capabilities: Contact Sensor, Battery Indicator
 *
 *  Props to the SmartThings crew and the fantastic community for the support and knowledge
 *  sharing that allowed me to craft this device.
 *
 *  Icon credits:
 *   - Alert by Daouna Jeong from the Noun Project
 *
 *  Author: surfous
 *  Date: {{BUILDDATE}}
 *  Build: {{BUILDTAG}}
 *
 */

import groovy.transform.Field
import org.codehaus.groovy.runtime.StackTraceUtils

import physicalgraph.device.HubAction
import physicalgraph.zwave.Command


// Constants
//

// time constants
@Field final Integer SECOND_MSEC = 1000
@Field final Integer MINUTE_SEC = 60
@Field final Integer MINUTE_MSEC = MINUTE_SEC * SECOND_MSEC
@Field final Integer HOUR_SEC = 60 * MINUTE_SEC
@Field final Integer HOUR_MSEC = HOUR_SEC * SECOND_MSEC

// time defaults
@Field final Integer DEFAULT_INTERCMD_DELAY_MSEC = SECOND_MSEC * 2
@Field final Integer PREF_DEFAULT_WAKE_UP_INTERVAL_HR = 4
@Field final Integer DEFAULT_WAKE_UP_INTERVAL_SEC = PREF_DEFAULT_WAKE_UP_INTERVAL_HR * HOUR_SEC

@Field final Integer DEFAULT_DOORBELL_CLEAR_DELAY_SEC = 15
@Field final Integer MIN_CLEAR_DOORBELL_DELAY_SEC = 3
@Field final Integer MAX_CLEAR_DOORBELL_DELAY_SEC = 60

@Field final Integer MIN_WAKE_UP_INTERVAL_SEC = HOUR_SEC
@Field final Integer MAX_WAKE_UP_INTERVAL_SEC = 7 * 24 * HOUR_SEC
@Field final Integer BATTERY_CHECK_INTERVAL_MSEC = 12 * HOUR_MSEC
@Field final Integer ASSOC_CHECK_INTERVAL_MSEC = 24 * HOUR_MSEC
@Field final Short   ASSOCIATION_GROUP_ID = 1

// tamper handling
@Field final String  TAMPER_DETECTED = 'detected'
@Field final String  TAMPER_CLEAR = 'clear'

@Field final String  TAMPER_CLEAR_INIT = 'device initialization'
@Field final String  TAMPER_CLEAR_AUTO = 'Automatically'
@Field final String  TAMPER_CLEAR_MANUAL = 'Manually'

// binary sensor values and interpretations
@Field final Short 	ZWAVE_TRUE  = 0xFF
@Field final Short	ZWAVE_FALSE = 0x00
@Field final Map 	ZWAVE = [(ZWAVE_TRUE): true, (ZWAVE_FALSE): false].withDefault {false}


@Field final String CONTACT_TRUE = 'closed'
@Field final String CONTACT_FALSE = 'open'
@Field final Map 	CONTACT = [(true): CONTACT_TRUE, (false): CONTACT_FALSE]
@Field final String DOORBELL_TRUE = 'doorbellActive'
@Field final String DOORBELL_FALSE = 'doorbellClear'
@Field final Map 	DOORBELL = [(true): DOORBELL_TRUE, (false): DOORBELL_FALSE]
@Field final String DOORWINDOW_TRUE = 'dwClosed'
@Field final String DOORWINDOW_FALSE = 'dwOpen'
@Field final Map 	DOORWINDOW = [(true): DOORWINDOW_TRUE, (false): DOORWINDOW_FALSE]

@Field final String MAIN_ATTR_TRUE = CONTACT_TRUE
@Field final String MAIN_ATTR_FALSE = CONTACT_FALSE
@Field final Map    MAIN_ATTR = CONTACT


// command classes and their versions supported by device
@Field final Map CMD_CLASS_VERSIONS = [0x20: 1, 0x30: 1, 0x71: 2, 0x72: 1, 0x80: 1, 0x84: 2, 0x85: 2, 0x86: 1, 0x70: 1]

@Field final Boolean IS_IOS_ONLY = true

@Field final Boolean DEBUG_MODE = false

// tile colors
@Field final String COLOR_DKRED  = '#C92424'
@Field final String COLOR_RED    = '#FF0033'
@Field final String COLOR_ORANGE = '#FFA81E'
@Field final String COLOR_YELLOW = '#FFFF00'
@Field final String COLOR_GREEN  = '#79B821'
@Field final String COLOR_CYAN   = '#1EE3FF'
@Field final String COLOR_DKBLUE = '#153591'
@Field final String COLOR_WHITE  = '#FFFFFF'

@Field final String COLOR_BATT_FULL  = COLOR_GREEN
@Field final String COLOR_BATT_GOOD  = COLOR_GREEN
@Field final String COLOR_BATT_OK    = COLOR_YELLOW
@Field final String COLOR_BATT_LOW   = COLOR_RED
@Field final String COLOR_BATT_CRIT  = COLOR_RED

@Field final String COLOR_TMP_COLDER = '#2A4BEE'
@Field final String COLOR_TMP_COLD   = '#7590F3'
@Field final String COLOR_TMP_COOL   = '#E3EAFD'
@Field final String COLOR_TMP_ROOM   = '#FDFDCE'
@Field final String COLOR_TMP_WARM   = '#F9F160'
@Field final String COLOR_TMP_HOT    = '#DF8136'
@Field final String COLOR_TMP_HOTTER = '#D1272B'

// smartlog scopes
@Field final String ZWEH = 'Z-WaveEventHandler' // For handlers of events sent by the device itself
@Field final String DTI = 'DeviceTypeInternal' // for commands that are automatically called in a device type's lifecycle
@Field final String CCMD = 'STDeviceCommand' // capability or standalone command
@Field final String CCC = 'CommandClassCommand' // wraps a single command class


// Global Variables
@Field def smartlog
@Field Long wakeUpPeriod

preferences
{
	input(name: "wakeupIntervalHrs", type: "enum", title: "Hours between wakeups (1-24)",
		options: ['1','2','3','4','5','6','7','8','9','10','11','12','13','14','15','16','17','18','19','20','21','22','23','24'],
		defaultValue: PREF_DEFAULT_WAKE_UP_INTERVAL_HR, required: true,
		description: "$PREF_DEFAULT_WAKE_UP_INTERVAL_HR")

	input(name: 'isDoorbell', type: 'boolean', title: 'Is the sensor for a doorbell?',
		defaultValue: false,
		description: 'Switch on if the device is being used to detect a doorbell ringing by sensing the activation of its electromagnet.')

	input(name: 'doorbellClearDelaySeconds', type: 'enum', required: true,
		options: ['3', '5', '10', '15', '20', '30', '45', '60'],
		title: 'Seconds (3-60) until doorbell alert is cleared?',
		defaultValue: DEFAULT_DOORBELL_CLEAR_DELAY_SEC,
		description: "$DEFAULT_DOORBELL_CLEAR_DELAY_SEC")

	input(name: "tamperClearAuto", type: "boolean", title: "Clear tamper alerts automatically?",
		description: 'Indicate if tamper alerts clear automatically upon wake or state change after the device cover is closed.',
		defaultValue: true)

	input(name: "invertSensorState", type: "boolean", title: "Reverse sensor state?",
		description: "Reverse the sensor's reading. Use for doorbell if Device Handler regularly reads active.")
}


metadata
{
	definition (name: "Ecolink DW-ZWAVE2 Contact Sensor", namespace: "surfous", author: "surfous")
	{
		capability "Battery"
		capability "Contact Sensor"
		capability "Configuration"
		capability "Sensor"

		attribute 'interpretedValue', 'string'
		attribute "tamper", "enum", ["clear", "detected"]
		attribute 'lastUpdated', 'number'
		attribute "blank", "string" // just as the name implies...

		command clearTamperManually
		command clearDoorbellManually

		fingerprint deviceId:"0x2001", inClusters:"0x30, 0x71, 0x72, 0x86, 0x85, 0x84, 0x80, 0x70", outClusters: "0x20"
	}

	simulator
	{
		status "wake_up" : "delay 8407"

		status 'alarm_active': zwave.alarmV2.alarmReport(zwaveAlarmType:7, zwaveAlarmEvent:2, alarmLevel:0xff).incomingMessage()
		status 'alarm_inactive': zwave.alarmV2.alarmReport(zwaveAlarmType:7, zwaveAlarmEvent:2, alarmLevel:0x00).incomingMessage()
		status 'alarm_tamper': zwave.alarmV2.alarmReport(zwaveAlarmType:1, zwaveAlarmEvent:3, zwaveAlarmStatus:0xff).incomingMessage()

		status 'batt_ok': new physicalgraph.zwave.Zwave().batteryV1.batteryReport(batteryLevel: 85).incomingMessage()
		status 'batt_low': new physicalgraph.zwave.Zwave().batteryV1.batteryReport(batteryLevel: 12).incomingMessage()
		status 'batt_crit': new physicalgraph.zwave.Zwave().batteryV1.batteryReport(batteryLevel: 0).incomingMessage()

		status 'wakeup_interval_cap': new physicalgraph.zwave.Zwave().wakeUpV2.wakeUpIntervalCapabilitiesReport(
			defaultWakeUpIntervalSeconds: DEFAULT_WAKE_UP_INTERVAL_SEC,
			minimumWakeUpIntervalSeconds: MIN_WAKE_UP_INTERVAL_SEC,
			maximumWakeUpIntervalSeconds: MAX_WAKE_UP_INTERVAL_SEC,
			wakeUpIntervalStepSeconds: 200
		).incomingMessage()

		reply '8002': 'command: 8003, payload: 55 00' // BatteryReport
		reply '8405': 'command: 8406, payload: 01 0E 10' // WakeUpIntervalReport
		reply '8409': 'command: 840A, payload: 00 02 58 09 3A 80 00 0E 10 00 00 C8' // WakeUpIntervalCapabilitiesReport
		reply '8502': 'command: 8503, payload: 01 05 00 01' // AssociationReport
		reply 'delay 8407': 'command 8407, payload:'
	}

	tiles
	{
		if (IS_IOS_ONLY)
		{
			standardTile('interpretation', 'device.interpretedValue', width: 2, height: 2)
			{
				// as a contact sensor
				state(DOORWINDOW_TRUE, label:"Closed", defaultState: true,
						icon:'st.contact.contact.closed', backgroundColor: COLOR_GREEN)
				state(DOORWINDOW_FALSE, label:"Open",
						icon:'st.contact.contact.open', backgroundColor: COLOR_ORANGE)

				// as a doorbell sensor
				state(DOORBELL_FALSE, label:"~ ~ ~",
						icon:'http://googledrive.com/host/0BwmI66XB_3u8QTlFalNENDhuX2c/st_bell_alert_inactive.png',
						backgroundColor: COLOR_WHITE)
				state(DOORBELL_TRUE
				,   label:"Ding Dong!",
						icon:'http://googledrive.com/host/0BwmI66XB_3u8QTlFalNENDhuX2c/st_bell_alert_active.png',
						backgroundColor: COLOR_CYAN, action: 'clearDoorbellManually',
						nextState: DOORBELL_FALSE)
			}
		}
		else
		{
			standardTile('interpretation', 'device.interpretedValue', width: 2, height: 2)
			{
				// as a contact sensor
				state(DOORWINDOW_TRUE, label:"Closed", defaultState: true,
						icon:'st.contact.contact.closed', backgroundColor: COLOR_GREEN)
				state(DOORWINDOW_FALSE, label:"Open",
						icon:'st.contact.contact.open', backgroundColor: COLOR_ORANGE)

				// as a doorbell sensor
				state(DOORBELL_FALSE, label:"- - -",
						icon:'st.security.alarm.clear', backgroundColor: COLOR_WHITE)
				state(DOORBELL_TRUE,  label:"Ding Dong!",
						icon:'st.security.alarm.alarm', backgroundColor: COLOR_CYAN,
						action: 'clearDoorbellManually', nextState: DOORBELL_FALSE)
			}
		}

		standardTile('contact', 'device.contact')
		{
			state(CONTACT_TRUE, label:"Closed", defaultState: true,
					icon:'st.contact.contact.closed', backgroundColor: COLOR_GREEN)
			state(CONTACT_FALSE, label:"Open",
					icon:'st.contact.contact.open', backgroundColor: COLOR_ORANGE)
		}

		standardTile('tamper', 'device.tamper')
		{
			state('clear',	label:"device ok", icon:'st.security.alarm.clear',
					backgroundColor: COLOR_WHITE, defaultState: true)
			state('detected', label:"> tamper <", icon:'st.security.alarm.alarm',
					backgroundColor: COLOR_RED, action: 'clearTamperManually')
		}

		standardTile("config", "device.configuration", decoration: "flat")
		{
			state("configure", action: "configuration.configure", icon:"st.secondary.configure")
		}

		standardTile('blankTile', 'device.blank', inactiveLabel: false, decoration: 'flat' )
		{
			state 'blank', label: '', action: '', icon: '', defaultState: true
		}

		valueTile('battery', 'device.battery')
		{
			state('battery', label:'battery ${currentValue}%',
			backgroundColors:[
				[value: 100, color: COLOR_BATT_FULL], // green
				[value: 60,  color: COLOR_BATT_GOOD], // green
				[value: 30,  color: COLOR_BATT_OK], // yellow
				[value: 1,   color: COLOR_BATT_CRIT], // red
			])
		}

		main(['interpretation'])
		if (DEBUG_MODE)
		{
			details(['interpretation', 'battery', 'tamper', 'contact', 'blankTile', 'blankTile'])
		}
		else
		{
			details(['interpretation', 'battery', 'tamper'])
		}
	}
}

// -----
// Context initialization
//

/**
 * Called before each invocation by a device event
 * Typically, parse() is the only entry point for this
 */
void initDeviceEvent()
{
	configureLogging()
}

/**
 * Called before each invocation by a user-initiated event
 * This could be any capability or custom command, or updated()
 * if the user modifies preferences
 */
void initUserEvent()
{
	configureLogging()
}

/**
 * Called before each invocation by a SmartThings platform event
 * Typically, these are events fired by the schedule
 */
	void initSmartThingsEvent()
{
	configureLogging()
}

void configureLogging()
{
	smartlog = Smartlog()

	String scriptScopeLevel = smartlog.getLevel()
	smartlog.info "script scope smartlog level is $scriptScopeLevel"
	// educated guess whether debug mode has been turned off since last time smartlog was initialized
	if (DEBUG_MODE == false && scriptScopeLevel == smartlog.LEVEL_FINE)
	{
		smartlog.info 'Debug mode has been turned off - resetting logging '
		smartlog.reset()
	}

	smartlog.setLevel(level: smartlog.LEVEL_INFO)
	smartlog.setLevel(scope: smartlog.type, level: smartlog.LEVEL_NONE)
	smartlog.setLevel(scope: 'CommandQueue', level: smartlog.LEVEL_WARN)

	if (DEBUG_MODE)
	{
		smartlog.setLevel(level: smartlog.LEVEL_FINE)
		smartlog.setLevel(scope: ZWEH, level: smartlog.LEVEL_FINE)
		smartlog.setLevel(scope: DTI, level: smartlog.LEVEL_FINE)
	}
}


// -----
//  Device type interface methods
//
void updated()
{
	initUserEvent()
	smartlog.fine(DTI, 'in updated')

	// bail if we've run updated() in the past 3 seconds
	BigDecimal lastUpdatedBd = device.currentValue('lastUpdated')?:0
	//Long lastUpdated = Long.valueOf(device.currentValue('lastUpdated')?:0)
	if ((now() - lastUpdatedBd.longValue()) < (3 * SECOND_MSEC))
	{
		smartlog.fine(DTI, 'duplicate call of updated(). aborting method.')
		return null
	}
	sendLoggedEvent(name: 'lastUpdated', value: now())

	state.pref = [:]

	state.pref.wakeupIntervalHrs = Integer.valueOf(settings?.wakeupIntervalHrs?:PREF_DEFAULT_WAKE_UP_INTERVAL_HR)

	state.pref.wakeupDevFlag = Boolean.valueOf(settings?.wakeupDevFlag?:false)

	state.pref.doorbellClearDelaySeconds = Integer.valueOf(settings?.doorbellClearDelaySeconds?:DEFAULT_DOORBELL_CLEAR_DELAY_SEC)
	if (state.pref.doorbellClearDelaySeconds < MIN_CLEAR_DOORBELL_DELAY_SEC)
	{
		state.pref.doorbellClearDelaySeconds = MIN_CLEAR_DOORBELL_DELAY_SEC
	}
	else if (state.pref.doorbellClearDelaySeconds > MAX_CLEAR_DOORBELL_DELAY_SEC)
	{
		state.pref.doorbellClearDelaySeconds = MAX_CLEAR_DOORBELL_DELAY_SEC
	}

	state.pref.invertSensorState = Boolean.valueOf(settings?.invertSensorState?:false)
	state.pref.isDoorbell = Boolean.valueOf(settings?.isDoorbell?:false)

	state.pref.tamperClearAuto = Boolean.valueOf(settings?.tamperClearAuto?:false)
	if (state.pref.tamperClearAuto)
	{
		clearTamper(TAMPER_CLEAR_AUTO)
	}

	// check to see if interpreted value would change. If so, send as immediate event
	String currentInterpAttr = device.currentValue('interpretedValue')
	def potentialInterpEvt = buildInterpretedContactEvent(device.currentValue('contact'))
	if (currentInterpAttr != potentialInterpEvt.value)
	{
		sendLoggedEvent(potentialInterpEvt)
	}

	smartlog.debug(DTI, "preferences recorded in state: ${state.pref}")
}

def parse(String rawZwaveEventDescription) {
	initDeviceEvent()
	String parseInstance = now() as String
	smartlog.fine(DTI, "parse $parseInstance: raw incoming event '${rawZwaveEventDescription}'")
	def result = []
	try
	{
		if (rawZwaveEventDescription == 'updated')
		{
			// do nothing - handles rogue invocation of parse with the arg set simply to 'updated'
			smartlog.warn(DTI, 'parse argument rawZwaveEventDescription was the plain string "updated." Bypassing this erroneous command.')
			return null
		}
		def cmd = zwave.parse(rawZwaveEventDescription, CMD_CLASS_VERSIONS)
		smartlog.trace(DTI, "parse $parseInstance: zwave.parse returned $cmd")
		if (cmd)
		{
			def deviceEventResult = zwaveEvent(cmd)
			def cq = CommandQueue()
			// If event handler returned a CommandQueue & prepare it accordingly
			if (deviceEventResult instanceof Map && deviceEventResult?.type == cq.type )
			{
				smartlog.fine(DTI, 'parse: event handler returned a CommandQueue')
				result = deviceEventResult.assembleResponse()
			}
			else
			{
				smartlog.warn(DTI, 'parse: device event handler returned something other than a CommandQueue. I guess we pass it on as-is and see what happens')
				result = deviceEventResult
			}
		}
	}
	catch (Throwable ex)
	{
		sendExceptionEvent(ex, 'exception caught in parse')
	}
	smartlog.debug(DTI, "parse $parseInstance: parse is returning $result")
	return result
}


// -----
// Capability commands
//
def configure()
{
	initUserEvent()
	smartlog.fine(CCMD, 'in Configure.configure')
	Map evtMapClosed = [name: 'contact', value: CONTACT_TRUE, isStateChange: true, displayed: true]
	Map evtMapOpen = [name: 'contact', value: CONTACT_FALSE, isStateChange: true, displayed: true]
	smartlog.debug(CCMD, "current contact attribute value: ${device.currentValue('contact')}")
	def cq = CommandQueue()
	if (device.currentValue('contact') == null || device.currentValue('contact') == CONTACT_FALSE)
	{
		sendLoggedEvent(evtMapClosed)
	}
	else
	{
		sendLoggedEvent(evtMapOpen)
	}
	//smartlog.debug(CCMD, "configure() is returning ${cq.assemble()}")
	return null
}

// -----
// Custom commands
//
void clearTamperManually()
{
	initUserEvent()
	smartlog.fine(CCMD, 'in clearTamperManually(')
	clearTamper(TAMPER_CLEAR_MANUAL)
}

/**
 * Called when active doorbell tile is tapped. Sends event to clear doorbell activity in case
 * it has gotten stuck.
 */
void clearDoorbellManually()
{
	initUserEvent()
	smartlog.fine(CCMD, 'in clearDoorbellManually')
	//clearDoorbell('Manually')
	String interpretedValue = device.currentValue('interpretedValue')
	smartlog.trace "current interpretedValue attribute is $interpretedValue"
	if (interpretedValue != DOORBELL_FALSE)
	{
		Map doorbellClearEvtMap = [name: 'interpretedValue', value: DOORBELL_FALSE]
		doorbellClearEvtMap.isStateChange = true
		doorbellClearEvtMap.displayed = true
		doorbellClearEvtMap.description = "doorbell active state cleared manually"
		sendLoggedEvent(doorbellClearEvtMap)
	}
}

// -----
//	Local methods
//
private String formatOctetAsHex(Short octet)
{
	return sprintf('%#x', octet)
}

private Boolean isDoorbellMode()
{
	return Boolean.valueOf(state?.pref?.isDoorbell?:false)
}

private void sendLoggedEvent(Map eventMap)
{
	smartlog.info "sendLoggedEvent - sending immediate event: $eventMap"
	sendEvent(eventMap)
}

private Boolean isSensorValueInverted()
{
	return Boolean.valueOf(state?.pref?.invertSensorState?:false)
}

private void clearTamper(String clearMethod)
{
	smartlog.fine "in clearTamper with arg '$clearMethod'"
	smartlog.trace "current tamper attribute value is ${device.currentValue('tamper')}"
	if (device.currentValue('tamper') != TAMPER_CLEAR)
	{
		Map evtMap = [name: 'tamper', value: TAMPER_CLEAR, description: "tamper alert cleared $clearMethod", isStateChange: true, displayed: true]
		sendLoggedEvent(evtMap)
	}
}

void sendExceptionEvent(Throwable t)
{
	sendExceptionEvent(t, 'caught exception')
}

void sendExceptionEvent(Throwable t, String prologue)
{
	String throwableShortDesc = t as String
	Map evtMap = [name: 'EXCEPTION', value: throwableShortDesc, description: throwableShortDesc, descriptionText: throwableShortDesc, displayed: true]
	String msg = "$prologue: $throwableShortDesc"
	try
	{
		String throwableLongDesc = t.getMessage()?:throwableShortDesc
		msg = "$prologue: $throwableLongDesc"
		String throwableName = throwableShortDesc.replaceAll(":.*\$", "").trim()
		evtMap.value = throwableName
		evtMap.descriptionText = msg
	}
	catch (Throwable t2)
	{
		String addlExceptionMsg = t2 as String
		String addlDescText = "Additional error while handling error report: $addlExceptionMsg"
		smartlog.error(addlDescText)
		sendLoggedEvent([name: 'EXCEPTION', value: addlExceptionMsg,
			description: addlExceptionMsg, descriptionText: addlDescText, displayed: true])
	}
	finally
	{
		smartlog.error( msg)
		sendLoggedEvent(evtMap)
	}
}

// -----
// Wakeup check tasks
//
def taskGetAssociation()
{
	smartlog.fine('wakeup tasks: in checkAssociation')
	def cq = CommandQueue()

	try
	{
		def associationLastQueryTime = state?.associationLastQueryTime?:0
		Long msecSinceLastAssocQuery = now() - (associationLastQueryTime as Long)
		String msg = "assoc node id in state: ${state?.associationNodeId} hub: $zwaveHubNodeId; Last query: $associationLastQueryTime msec since: $msecSinceLastAssocQuery; inerval msec: $ASSOC_CHECK_INTERVAL_MSEC (dev flag ${state?.pref?.wakeupDevFlag})"
		log.debug msg
		Map eventMap = [name: 'checkAssociation', displayed: false, descriptionText: msg]
		if (state?.associationNodeId != zwaveHubNodeId || msecSinceLastAssocQuery > ASSOC_CHECK_INTERVAL_MSEC || state?.pref?.wakeupDevFlag)
		{
			if (state?.pref?.wakeupDevFlag)
			{
				smartlog.debug('DEV mode - checking association regardless of time or cached value')
			}
			smartlog.info('need to check hub association')
			Command assnGetCmd = ccAssociationGet()
			eventMap.value = true
			eventMap.description = assnGetCmd as String
			cq.add(assnGetCmd)
		}
		else
		{
			eventMap.value = false
		}
		cq.add(eventMap)
	}
	catch (Throwable ex)
	{
		sendExceptionEvent(ex, 'exception caught in checkAssociation')
	}
	return cq
}

Map taskGetBattery()
{
	smartlog.fine('wakeup tasks: in checkBattery')
	def cq = CommandQueue()
	try
	{
		Command battGetCmd = ccBatteryGet()
		cq.add(battGetCmd)
	}
	catch (Throwable ex)
	{
		sendExceptionEvent(ex, 'exception caught in checkBattery')
	}
	return cq
}

Map taskGetWakeupInterval()
{
	smartlog.trace('wakeup tasks: in checkWakeupInterval')
	def cq = CommandQueue()
	try
	{
		Integer wakeupHours = (state?.pref?.wakeupIntervalHrs?:PREF_DEFAULT_WAKE_UP_INTERVAL_HR) as Integer
		Integer wakeupSeconds = wakeupHours * HOUR_SEC
		if (state?.pref?.wakeupDevFlag)
		{
			smartlog.debug('DEV mode - forcing wakeup interval to 600 sec (10 min). Will set if not already at this interval.')
			wakeupSeconds = MIN_WAKE_UP_INTERVAL_SEC
		}
		String msg = "pref wakeupIntervalHrs: $wakeupHours; wakeupSeconds $wakeupSeconds (dev flag ${state?.pref?.wakeupDevFlag})"
		log.debug msg
		Map eventMap = [name: 'changeWakeupInterval', value: false, descriptionText: msg, displayed: false]
		if (state?.cfg?.wakeupSeconds as Integer != wakeupSeconds)
		{
			smartlog.info("wakeup interval pref '$wakeupSeconds' seconds does not match state value of '${state?.cfg?.wakeupSeconds}' seconds. Doing a set/get to $wakeupSeconds")
			eventMap.value = true
			cq.add(macroSetGetWakeUpInterval(wakeupSeconds))
		}
		cq.add(eventMap)
	}
	catch (Throwable ex)
	{
		sendExceptionEvent(ex, 'exception caught in checkWakeupInterval')
	}
	return cq
}

void taskCheckTamperState()
{
	smartlog.trace('wakeup tasks: in taskCheckTamperState')
	try
	{
		Boolean tamperClearAuto = (state?.pref?.tamperClearAuto)
		smartlog.debug "tamper attribute is ${device.currentValue('tamper')}; auto clear is set to $tamperClearAuto"
		if (device.currentValue('tamper') == TAMPER_DETECTED && tamperClearAuto) // check tamper attribute.
		{
			// if it's 'detected' set it to 'clear' as it seems that WakeUpNotification is not sent during
			// tamper, but device wakes immediately after tamper clears
			smartlog.debug 'Clearing tamper automatically when device indicates cover has been closed'
			clearTamper(TAMPER_CLEAR_AUTO)
		}
	}
	catch (Throwable ex)
	{
		sendExceptionEvent(ex, 'exception caught in taskCheckTamperState')
	}
}

// TODO: Debug this - it seems problematic
Map taskGetCurrentSensorValue()
{
	smartlog.trace('wakeup tasks: in taskGetCurrentSensorValue')
	def cq = CommandQueue()
	try
	{
		cq.add(ccSensorBinaryGet())
	}
	catch (Throwable ex)
	{
		sendExceptionEvent(ex, 'exception caught in taskGetCurrentSensorValue')
	}
	return cq
}

Map taskClearDoorbell()
{
	smartlog.trace('wakeup tasks: in taskClearDoorbell')
	def cq = CommandQueue()
	try
	{
		if (isDoorbellMode())
		{
			cq.add(macroBuildClearDoorbellEvents())
		}
		else
		{
			smartlog.debug 'not in doorbell mode - exiting taskClearDoorbell'
		}
	}
	catch (Throwable ex)
	{
		sendExceptionEvent(ex, 'exception caught in checkCurrentSensorValue')
	}
	return cq

}

/**
 * This is a chain controller for metadata get/reports
 * it works like this:
 * first call checks the stored metadata in state.
 * @return [description]
 */
def chainDeviceMetadata(Boolean fromWakeUpRitual=false)
{
	smartlog.trace('wakeup tasks: in chainDeviceMetadata')
	def cq = CommandQueue()
	try
	{
		if (isDeviceMetadataChainComplete())
		{
			smartlog.trace " * stored version: $state.deviceMeta.version"
			smartlog.trace " * stored msr: $state.deviceMeta.msr"
			smartlog.trace "$device.displayName metadata is stored in state:"
			if (fromWakeUpRitual == false)
			{
				cq.add(macroWakeUpRitual())
			}
		}
		else if (!state.deviceMeta?.version)
		{
			smartlog.debug('device metadata chain requires version link')
			cq.add(ccVersionGet())
		}
		else if (!state.deviceMeta?.msr)
		{
			smartlog.debug('device metadata chain requires msr link')
			cq.add(ccManufacturerSpecificGet())
		}
		else if (!state.deviceMeta?.wakeup)
		{
			smartlog.debug('device metadata chain requires wakeup interval capabilities link')
			cq.add(ccWakeUpIntervalCapabilitiesGet())
		}
		else
		{
			smartlog.error 'chainDeviceMetadata should never fall to this level. Check that chain boolean includes all links.'
		}

	}
	catch (Throwable ex)
	{
		sendExceptionEvent(ex, 'exception caught in chainDeviceMetadata')
	}
	return cq
}

Boolean isDeviceMetadataChainComplete()
{
	Boolean isComplete = false
	if (state?.deviceMeta == null)
	{
		state.deviceMeta = [:]
	}
	else if (state.deviceMeta?.msr &&
			state.deviceMeta?.version &&
			state.deviceMeta?.wakeup)
	{
		isComplete = true
	}
	return isComplete
}


// -----
// CommandClass commands, event handlers and helper methods
//

// // CommandClass WakeUp

def zwaveEvent(physicalgraph.zwave.commands.wakeupv2.WakeUpNotification deviceEvent) {
	smartlog.fine(ZWEH, "handling WakeUpNotification '$deviceEvent'")
	def cq = CommandQueue()
	wakeUpPeriod = now()
	sendLoggedEvent([name: "wakeup-$wakeUpPeriod", value: 'wakeUpNotification', isStateChange: true, description: deviceEvent as String, descriptionText: "${device.displayName} woke up.", displayed: false])

	// the wakeup tasks to evaluate and send to the device
	// cq.add(ccAlarmTypeSupportedGet())
	cq.add(macroWakeUpRitual())
	return cq
}

def macroWakeUpRitual()
{
	//if (!state?.ccVersions) state.ccVersions = [:]
	def cq = CommandQueue()

	// check if we need to clear/init tamper attribute
	if (!state?.firstWakeInitComplete)
	{
		clearTamper(TAMPER_CLEAR_INIT)
		state.firstWakeInitComplete = true
	}

	if (!isDeviceMetadataChainComplete())
	{
		cq.add(chainDeviceMetadata(true))
	}
	else
	{
		smartlog.info "compiling standard WakeUp ritual for ${device.displayName}"
		taskCheckTamperState()
		//cq.add(taskGetCurrentSensorValue())
		cq.add(taskGetWakeupInterval())
		cq.add(taskGetAssociation())
		cq.add(taskGetBattery())
		cq.add(macroSendToSleep())
	}

	return cq
}

def macroSendToSleep()
{
	def cq = CommandQueue()
	cq.add('delay 10000')
	Command wakeupNMICmd = ccWakeUpNoMoreInformation()
	String nmiMsg = "no more information for ${device.displayName}. sending it back to sleep"
	smartlog.debug(ZWEH, nmiMsg)
	cq.add(wakeupNMICmd)
	sendLoggedEvent([name: "wakeup-$wakeUpPeriod", value: 'noMoreInformation', description: wakeupNMICmd.format(), descriptionText: nmiMsg, displayed: false])
	return cq
}

def macroSetGetWakeUpInterval(Integer seconds)
{
	smartlog.trace('macroSetGetWakeUpInterval')
	def cq = CommandQueue()
	cq.add(ccWakeUpIntervalSet(seconds))
	cq.add(ccWakeUpIntervalGet())
	return cq
}

def ccWakeUpIntervalSet(Integer seconds)
{
	// 0x8404
	smartlog.trace(CCC, 'issuing WakeUpIntervalSet')
	if (!seconds || seconds < MIN_WAKE_UP_INTERVAL_SEC) seconds = state.deviceMeta?.wakeup?.minSeconds?:MIN_WAKE_UP_INTERVAL_SEC
	if (seconds > MAX_WAKE_UP_INTERVAL_SEC) seconds = state.deviceMeta?.wakeup?.maxSeconds?:MAX_WAKE_UP_INTERVAL_SEC
	GString logMsg = "WakeUpIntervalSet to $seconds seconds"
	if (state.deviceMeta?.wakeup?.overrideSeconds)
	{
		logMsg += ' (override wakeup interval found in state - using its value)'
		seconds = state.deviceMeta?.wakeup?.overrideSeconds as Integer
	}
	smartlog.info(CCC, logMsg as String)
	return zwave.wakeUpV2.wakeUpIntervalSet(nodeid: zwaveHubNodeId as Short, seconds: seconds)
}

def ccWakeUpIntervalGet()
{
	// 0x8405
	smartlog.trace(CCC, 'issuing WakeUpIntervalGet')
	return zwave.wakeUpV2.wakeUpIntervalGet()
}

def zwaveEvent(physicalgraph.zwave.commands.wakeupv2.WakeUpIntervalReport deviceEvent)
{
	smartlog.trace(ZWEH, "handling WakeUpIntervalReport '$deviceEvent'")
	Integer intervalSeconds = deviceEvent.seconds
	Short nodeId = deviceEvent.nodeid
	if (!state?.cfg) state.cfg = [:]
	state.cfg.wakeupSeconds = intervalSeconds
	String msg = "Device ${device.displayName} reports a wake up interval of $intervalSeconds for node $nodeId"
	smartlog.info(ZWEH, msg)
	Map evtMap = [name: 'wakeInterval', value: intervalSeconds, unit: 'seconds', description: deviceEvent as String, descriptionText: msg, displayed: true]
	return createEvent(evtMap)
}

def ccWakeUpIntervalCapabilitiesGet()
{
	// 0x8409
	smartlog.trace(CCC, 'issuing WakeUpIntervalCapabilitiesGet')
	return zwave.wakeUpV2.wakeUpIntervalCapabilitiesGet()
}

/**
 * handles wakeupv2.WakeUpIntervalCapabilitiesReport. Member of the deviceMetadata chain gang
 * @param  deviceEvent zwave device event from zwave.parse methos
 * @return			 CommandQueue Map
 */
def zwaveEvent(physicalgraph.zwave.commands.wakeupv2.WakeUpIntervalCapabilitiesReport deviceEvent)
{
	smartlog.trace(ZWEH, "handling WakeUpIntervalCapabilitiesReport '$deviceEvent'")
	state.deviceMeta.wakeup = [:]
	state.deviceMeta.wakeup.defaultSeconds = deviceEvent.defaultWakeUpIntervalSeconds
	state.deviceMeta.wakeup.maxSeconds = deviceEvent.maximumWakeUpIntervalSeconds
	state.deviceMeta.wakeup.minSeconds = deviceEvent.minimumWakeUpIntervalSeconds
	state.deviceMeta.wakeup.stepSeconds = deviceEvent.wakeUpIntervalStepSeconds
	String msg = "Device ${device.displayName} reports these wakeup interval capabilities: default ${state.deviceMeta.wakeup.defaultSeconds}s; min ${state.deviceMeta.wakeup.minSeconds}s; max ${state.deviceMeta.wakeup.maxSeconds}s; step ${state.deviceMeta.wakeup.stepSeconds}s"
	smartlog.info(ZWEH, msg)
	Map evtMap = [name: 'wakeupCapabilities', value: '', unit: 'seconds', description: deviceEvent as String, descriptionText: msg, displayed: true]
	def cq = CommandQueue()
	cq.add(evtMap)
	cq.add(chainDeviceMetadata())
	return cq
}

def ccWakeUpNoMoreInformation()
{
	// 0x8408
	smartlog.trace(CCC, 'issuing WakeUpNoMoreInformation')
	return zwave.wakeUpV2.wakeUpNoMoreInformation()
}

// // CommandClass Association V2
def ccAssociationSet()
{
	// 0x8501
	smartlog.trace(CCC, "setting ${device.displayName} association to group id $ASSOCIATION_GROUP_ID for nodeId $zwaveHubNodeId")
	return zwave.associationV2.associationSet(groupingIdentifier:ASSOCIATION_GROUP_ID, nodeId:zwaveHubNodeId)
}

def ccAssociationRemove()
{
	// 0x8504
	smartlog.fine(CCC, "removing ${device.displayName} association to group id $ASSOCIATION_GROUP_ID for nodeId $zwaveHubNodeId")
	state.associationNodeId = null // appropriate?
	return zwave.associationV2.associationRemove(groupingIdentifier:ASSOCIATION_GROUP_ID, nodeId:zwaveHubNodeId)
}

def ccAssociationGet()
{
	// 0x8502
	smartlog.fine(CCC, "getting ${device.displayName} association for group id $ASSOCIATION_GROUP_ID")
	return zwave.associationV2.associationGet(groupingIdentifier: ASSOCIATION_GROUP_ID)
}

def zwaveEvent(physicalgraph.zwave.commands.associationv2.AssociationReport deviceEvent)
{
	smartlog.fine(ZWEH, "handling event associationv2.associationReport '$deviceEvent'")
	def cq = CommandQueue()
	if (deviceEvent.nodeId.any { it == zwaveHubNodeId })
	{
		smartlog.info(ZWEH, "Hub $zwaveHubNodeId is associated with $device.displayName association group $deviceEvent.groupingIdentifier")
		state.associationNodeId = zwaveHubNodeId
		state.associationLastQueryTime = now()
	}
	else
	{
		smartlog.info(ZWEH, 'association not found, setting it, then getting it again')
		cq.add(ccAssociationSet(), 4000)
		cq.add(ccAssociationGet())
		state.associationLastQueryTime = null
	}
	return cq
}


// // CommandClass Sensor Binary
def ccSensorBinaryGet()
{
	smartlog.fine(CCC, 'issuing SensorBinaryGet')
	return zwave.sensorBinaryV1.sensorBinaryGet()
}

def zwaveEvent(physicalgraph.zwave.commands.sensorbinaryv1.SensorBinaryReport deviceEvent)
{
	smartlog.fine(ZWEH, "handling SensorBinaryReport '$deviceEvent'")
	def cq = CommandQueue()
	taskCheckTamperState()
	String sensorHexValue = formatOctetAsHex(deviceEvent.sensorValue)

	Map contactEvtMap = [:]
	contactEvtMap.name = 'contact'
	contactEvtMap.description = deviceEvent as String

	// contact sensor is a "normally closed" device, meaning 0x00 == off == closed == normal state
	if (ZWAVE[deviceEvent.sensorValue as Short] == false)
	{
		contactEvtMap.value = CONTACT[true]
	}
	else
	{
		contactEvtMap.value = CONTACT[false]
	}
	String msg = "contact sensor reports $contactEvtMap.value ($sensorHexValue)"
	smartlog.info msg
	contactEvtMap.descriptionText = msg
	contactEvtMap.isStateChange = true
	contactEvtMap.displayed = true

	// interpret the contact sensor event to the actual usage
	def interpretedEvt = buildInterpretedContactEvent(contactEvtMap)
	if (isDoorbellMode() && interpretedEvt.value == DOORBELL[true])
	{
		// return the active events for immediate sending from the parse method
		cq.add(interpretedEvt)
		cq.add(contactEvtMap)

		// Now, build & schedule the corresponding clear events to fire in the future
		initFutureEventStash()
		macroBuildClearDoorbellEvents().each { stashFutureEvent(it) }

		// find how long to wait before firing these events
		Integer clearDelay = state?.pref?.doorbellClearDelaySeconds?:DEFAULT_DOORBELL_CLEAR_DELAY_SEC
		smartlog.trace "scheduling stashed events to run in $clearDelay seconds"
		runIn(clearDelay, "runStashedFutureEvent", [overwrite: true])
	}
	else if (isDoorbellMode() && interpretedEvt.value == DOORBELL[false])
	{
		// we suppress the interpreted event in favor of the timed event already scheduled
		// but if we catch the raw contact sensor event, we'll send it along.
		cq.add(contactEvtMap)
	}
	else
	{	// in contact mode, events always get queued for sending ASAP
		cq.add(interpretedEvt)
		cq.add(contactEvtMap)
	}

	return cq
}

List macroBuildClearDoorbellEvents()
{
	// build event to reset contact sttribute
	Map contactClearEventMap = [name: 'contact', value: CONTACT[false]]
	contactClearEventMap.description = "contact attribute scheduled reset to  $CONTACT[false]"
	contactClearEventMap.isStateChange = true
	contactClearEventMap.displayed = true

	// build interpreted reset event for the doorbell
	Map doorbellClearEventMap = buildInterpretedContactEvent(contactClearEventMap)
	doorbellClearEventMap.descriptionText = "doorbell attribute scheduled reset to $doorbellClearEventMap.value"

	return [contactClearEventMap, doorbellClearEventMap]
}

Map buildInterpretedContactEvent(Map contactEvent)
{
	return buildInterpretedContactEvent(contactEvent.value)
}

Map buildInterpretedContactEvent(String contactValue)

{
	Boolean contactBoolean = (contactValue == CONTACT[true])
	String currentInterpAttr = device.currentValue('interpretedValue')
	Map interpretedEvtMap = [name: 'interpretedValue']
	if (isSensorValueInverted()) contactBoolean = !contactBoolean
	String interpretedValue
	String msg
	if (isDoorbellMode())
	{
		interpretedValue = DOORBELL[contactBoolean]
		msg = "doorbell is $interpretedValue"
	}
	else
	{
		interpretedValue = DOORWINDOW[contactBoolean]
		msg = "contact sensor is $interpretedValue"
	}
	interpretedEvtMap.value = interpretedValue
	interpretedEvtMap.description = msg
	interpretedEvtMap.descriptionText = msg
	interpretedEvtMap.isStateChange = true
	interpretedEvtMap.displayed = true

	smartlog.debug "interpreted '$contactValue' into '$msg'; as event $interpretedEvtMap"
	return interpretedEvtMap
}


void initFutureEventStash()
{
	state.stashedEvents = []
}

Integer stashFutureEvent(Map event)
{
	smartlog.fine "stashFutureEvent $event"
	if (!state?.stashedEvents instanceof List)
	{
		state.stashedEvents = [event]
	}
	else
	{
		state.stashedEvents << event
	}
	return state.stashedEvents.size()
}

/**
 * scheduled device handler entry point
 */
void runStashedFutureEvent()
{
	initSmartThingsEvent() // because this is en entry point
	smartlog.fine "in runStashedFutureEvent"
	List stashList = []
	if (state?.stashedEvents instanceof Map)
	{
		stashList.add(state.stashedEvents)
	}
	else if (state?.stashedEvents instanceof List)
	{
		stashList = state?.stashedEvents
	}

	// clear stashed events from state
	if (state?.stashedEvent != null)
	{
		state.remove('stashedEvents')
	}

	smartlog.info ("found ${stashList.size()} stashed events to run")
	for (Map stashedEvent : stashList)
	{
		smartlog.debug "firing stashed event: ${stashedEvent}"
		sendLoggedEvent(stashedEvent) // send it
	}
}


// // CommandClass Battery
def ccBatteryGet()
{
	smartlog.fine(CCC, 'issuing batteryGet')
	def cmd = zwave.batteryV1.batteryGet()
	sendLoggedEvent([ name: 'command', value:'', description: cmd.format(), descriptionText: "Requested battery update from ${device.displayName}."])
	return cmd
}

def zwaveEvent(physicalgraph.zwave.commands.batteryv1.BatteryReport deviceEvent)
{
	smartlog.fine(ZWEH, "handling BatteryReport '$deviceEvent'")
	Integer batteryLevel = deviceEvent.batteryLevel
	state.batteryLastUpdateTime = now()
	String batteryMessage = "battery level is reported as $batteryLevel"
	Map evtMap = [ name: "battery", value: batteryLevel, unit: "%" , displayed: true, description: deviceEvent as String, descriptionText: message]
	log.info batteryMessage
	if (batteryLevel == 0xFF || batteryLevel == 0 )
	{
		evtMap.value = 1
		evtMap.descriptionText = "${device.displayName}: battery is almost dead!"
	}
	else if (batteryLevel < 15 )
	{
		evtMap.descriptionText = "${device.displayName}: battery is low!"
	}
	return createEvent(evtMap)
}


// // CommandClass Version
def ccVersionGet()
{
	smartlog.fine(CCC, 'issuing VersionGet')
	return zwave.versionV1.versionGet()
}

def zwaveEvent(physicalgraph.zwave.commands.versionv1.VersionReport deviceEvent)
{
	Map cq = CommandQueue()
	smartlog.fine(ZWEH, 'handling event versionv1.VersionReport')
	def fw = "${deviceEvent.applicationVersion}.${deviceEvent.applicationSubVersion}"
	updateDataValue("fw", fw)
	def text = "$device.displayName: application version: ${deviceEvent.applicationVersion}, Z-Wave firmware version: ${deviceEvent.applicationSubVersion}, Z-Wave lib type: ${deviceEvent.zWaveLibraryType}, Z-Wave version: ${deviceEvent.zWaveProtocolVersion}.${deviceEvent.zWaveProtocolSubVersion}"

	state.deviceMeta.version = [:]
	state.deviceMeta.version.application = deviceEvent.applicationVersion
	state.deviceMeta.version.zwaveFirmware = deviceEvent.applicationSubVersion
	state.deviceMeta.version.zwaveLibraryType = deviceEvent.zWaveLibraryType
	state.deviceMeta.version.zwaveProtocol = deviceEvent.zWaveProtocolVersion
	state.deviceMeta.version.zwaveProtocolSub = deviceEvent.zWaveProtocolSubVersion

	smartlog.info text
	sendLoggedEvent(name: 'version', value: '', description: deviceEvent as String, descriptionText: text, displayed: true)
	cq.add(chainDeviceMetadata())
	return cq
}

def ccVersionCommandClassGet(Short ccId)
{
	smartlog.debug(CCC, "ccVersionCommandClassGet ${formatOctetAsHex(ccId as Short)} ($ccId)")
	def cmd = zwave.versionV1.versionCommandClassGet(requestedCommandClass: ccId)
	return cmd
}

def zwaveEvent(physicalgraph.zwave.commands.versionv1.VersionCommandClassReport deviceEvent)
{
	Map cq = CommandQueue()
	smartlog.fine(ZWEH, "handling event versionv1.VersionCommandClassReport: $deviceEvent")
	String ccIdHex = formatOctetAsHex(deviceEvent.requestedCommandClass as Short)
	if (state?.ccVersions == null) state.ccVersions = [:]
	state.ccVersions.put(ccIdHex, deviceEvent.commandClassVersion as Integer)

	smartlog.info "CommandClass $ccIdHex, Version ${deviceEvent.commandClassVersion}"
	sendLoggedEvent(name: "cc${ccIdHex}_v", value: deviceEvent.commandClassVersion, displayed: true)
	cq.add(macroWakeUpRitual())
	return cq
}


// // CommandClass Manufacturer Specific V1
def ccManufacturerSpecificGet()
{
	smartlog.fine(CCC, 'issuing ccManufacturerSpecificGet')
	def cmd = zwave.manufacturerSpecificV1.manufacturerSpecificGet()
	return cmd
}

def zwaveEvent(physicalgraph.zwave.commands.manufacturerspecificv1.ManufacturerSpecificReport deviceEvent)
{
	smartlog.fine(ZWEH, 'handling event manufacturerspecificv1.ManufacturerSpecificReport')
	def msr = String.format("%04X-%04X-%04X", deviceEvent.manufacturerId, deviceEvent.productTypeId, deviceEvent.productId)
	updateDataValue("MSR", msr)

	state.deviceMeta.msr = [msr: "$msr"]
	state.deviceMeta.msr.manufacturerId = deviceEvent.manufacturerId
	state.deviceMeta.msr.productTypeId = deviceEvent.productTypeId
	state.deviceMeta.msr.productId = deviceEvent.productId

	String niceMsr = "$device.displayName MSR: $msr; Manufacturer: $state.deviceMeta.msr.manufacturerId; Product Type: $state.deviceMeta.msr.productTypeId; Product: $state.deviceMeta.msr.productId"

	smartlog.info niceMsr
	sendLoggedEvent([name: 'msr', value: msr, description: state.deviceMeta.msr.toString(), description: deviceEvent as String, descriptionText: niceMsr, displayed: true])
	def cq = CommandQueue()
	cq.add(chainDeviceMetadata())
	return cq
}

// // CommandClass Alarm V2
def zwaveEvent(physicalgraph.zwave.commands.alarmv2.AlarmReport deviceEvent)
{
	smartlog.fine(ZWEH, "handling AlarmReport '$deviceEvent'")
	String alarmMsg = "${device.displayName}: Alarm Type ${deviceEvent.zwaveAlarmType}; Event ${deviceEvent.zwaveAlarmEvent}; Status ${deviceEvent.zwaveAlarmStatus};   (V1 info Type:${deviceEvent.alarmType}; Level:${deviceEvent.alarmLevel})"

	Map evtMap = [name: "alarm${deviceEvent.zwaveAlarmType}", value: "v2event ${deviceEvent.zwaveAlarmEvent}, v2status ${deviceEvent.zwaveAlarmStatus}, v1level ${deviceEvent.alarmLevel}", description: deviceEvent as String]
	smartlog.debug alarmMsg

	switch (deviceEvent)
	{
		case {it.zwaveAlarmType == 7 && it.zwaveAlarmEvent == 3 && ZWAVE[it.alarmLevel]}:
			// this is the alarm for tamper - There seems to be no clear alarm for this
			evtMap.name = 'tamper'
			evtMap.value = TAMPER_DETECTED
			evtMap.displayed = true
			alarmMsg = "${device.displayName}: sensor cover has been opened"
			break
		default:
			evtMap.displayed = true
			smartlog.warn "unhandled alarm $alarmMsg"
	}
	evtMap.descriptionText = alarmMsg
	smartlog.info alarmMsg
	return createEvent(evtMap)
}


// Catch-all handler.for any unsupported zwave events the device might send at us
def zwaveEvent(physicalgraph.zwave.Command deviceEvent)
{
	String message = "Unhandled device event: '${deviceEvent}'"
	smartlog.warn(ZWEH, message)
	return createEvent([name: 'unhandled', value:'', description: deviceEvent as String, descriptionText: message, displayed: false])
}

/*
	ClosureClasses down here.
 */

// ADDIN TARGET Smartlog_cc

// ADDIN TARGET CommandQueue_cc
