/**
 *  Monoprice NO-LABEL (Vision OEM) Shock Sensor
 *  Monoprice NO-LABEL (Vision OEM) Garage Door Sensor
 *
 *  Capabilities: Acceleration Sensor (shock), Battery Indicator
 *
 *  Notes: After pairing, set device preferences in the app, open the sensor for at least five
 *  seconds then close to wake it and complete device type recognition and initialization.
 *
 *  Raw Description: 0 0 0x2001 0 0 0 7 0x30 0x71 0x85 0x80 0x72 0x86 0x84
 *
 * Shock Sensor:
 *  VersionReport: application version: 4, Z-Wave firmware version: 84, Z-Wave lib type: 6, Z-Wave version: 3.40
 *  MSR: 0109-2003-0302
 *
 * Garage Door Sensor:
 *  VersionReport: application version: 4, Z-Wave firmware version: 84, Z-Wave lib type: 6, Z-Wave version: 3.52
 *  MSR: 0109-200A-0A02
 *
 *  Author: surfous
 *  Version: 20150716r0
 *  Date: 2015-07-16
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
@Field final Integer MIN_WAKE_UP_INTERVAL_SEC = 10 * MINUTE_SEC
@Field final Integer MAX_WAKE_UP_INTERVAL_SEC = 7 * 24 * HOUR_SEC

@Field final Integer ASSOC_CHECK_INTERVAL_MSEC = 24 * HOUR_MSEC
@Field final Short   ASSOCIATION_GROUP_ID = 1

// duration key must match the attribute name
@Field final Map FILTER_REPEAT_DURATIONS = [
	'lastSensorReport': 2 * SECOND_MSEC,
	'lastUpdated': 3 * SECOND_MSEC].withDefault {SECOND_MSEC}

// tamper handling
@Field final String  PREF_DEFAULT_TAMPER_FALSE_ON_WAKE = true
@Field Map tamper_attr_map = [:]
tamper_attr_map.ATTR_NAME = 'tamper'
tamper_attr_map.TRUE = 'detected'
tamper_attr_map.FALSE = 'clear'
tamper_attr_map.FALSE_INIT = 'device initialization'
tamper_attr_map.FALSE_AUTO = 'Automatically'
tamper_attr_map.FALSE_MANUAL = 'Manually'
@Field final Map	TAMPER = tamper_attr_map

// binary sensor values
@Field final Short	ZWAVE_TRUE  = 0xFF
@Field final Short	ZWAVE_FALSE = 0x00
@Field final Map	ZWAVE = [ZWAVE_TRUE: true, ZWAVE_FALSE: false].withDefault {false}

@Field Map shock_attr_map = [:]
shock_attr_map.SENSOR_TYPE = 'shock'
shock_attr_map.SENSOR_LABEL = 'shock'
shock_attr_map.PRODUCT_TYPE_ID = 0x2003
shock_attr_map.ATTR_NAME = 'acceleration'
shock_attr_map.ATTR_LABEL = 'shock'
shock_attr_map.TRUE = 'active'
shock_attr_map.FALSE = 'inactive'
shock_attr_map.STATEMAP = [(true): shock_attr_map.TRUE, (false): shock_attr_map.FALSE].withDefault {shock_attr_map.FALSE}
@Field final Map	SHOCK = shock_attr_map

@Field Map garagedoor_attr_map = [:]
garagedoor_attr_map.SENSOR_TYPE = 'garagedoor'
garagedoor_attr_map.SENSOR_LABEL = 'garage door'
garagedoor_attr_map.PRODUCT_TYPE_ID = 0x200A
garagedoor_attr_map.ATTR_NAME = 'contact'
garagedoor_attr_map.ATTR_LABEL = 'contact'
garagedoor_attr_map.TRUE = 'open'
garagedoor_attr_map.FALSE = 'closed'
garagedoor_attr_map.STATEMAP = [(true): garagedoor_attr_map.TRUE, (false): garagedoor_attr_map.FALSE].withDefault {garagedoor_attr_map.FALSE}
@Field final Map	GARAGEDOOR = garagedoor_attr_map

@Field Map unknown_attr_map = [:]
unknown_attr_map.UNKNOWN = 'unknown'
unknown_attr_map.SENSOR_TYPE = unknown_attr_map.UNKNOWN
unknown_attr_map.SENSOR_LABEL = unknown_attr_map.UNKNOWN
unknown_attr_map.PRODUCT_TYPE_ID = 0X0
unknown_attr_map.ATTR_NAME = unknown_attr_map.UNKNOWN
unknown_attr_map.ATTR_LABEL = unknown_attr_map.UNKNOWN
unknown_attr_map.TRUE = unknown_attr_map.UNKNOWN
unknown_attr_map.FALSE = unknown_attr_map.UNKNOWN
unknown_attr_map.STATEMAP = [:].withDefault {unknown_attr_map.UNKNOWN}
@Field final Map	UNKNOWN = unknown_attr_map

// TODO - find a way to set this based upon SENSOR_TYPE
@Field Map		    MAIN_ATTR = null

@Field final Map CMD_CLASS_VERSIONS = [0x20: 1, 0x30: 1, 0x71: 2, 0x72: 1, 0x80: 1, 0x84: 2, 0x85: 2, 0x86: 1]

// tile colors
@Field final String COLOR_DKRED  = '#C92424'
@Field final String COLOR_RED    = '#FF0033'
@Field final String COLOR_ORANGE = '#FFA81E'
@Field final String COLOR_YELLOW = '#FFFF00'
@Field final String COLOR_GREEN  = '#79B821'
@Field final String COLOR_CYAN   = '#1EE3FF'
@Field final String COLOR_DKBLUE = '#153591'
@Field final String COLOR_WHITE  = '#FFFFFF'
@Field final String COLOR_BLACK  = '#000000'

@Field final String COLOR_BATT_FULL  = COLOR_GREEN
@Field final String COLOR_BATT_GOOD  = COLOR_GREEN
@Field final String COLOR_BATT_OK    = COLOR_YELLOW
@Field final String COLOR_BATT_LOW   = COLOR_RED
@Field final String COLOR_BATT_CRIT  = COLOR_RED

// smartlog scopes

@Field final String ZWEH = 'Z-WaveEventHandler' // For handlers of events sent by the device itself
@Field final String DTI = 'DeviceTypeInternal' // for commands that are automatically called in a device type's lifecycle
@Field final String CCMD = 'STDeviceCommand' // capability or standalone command
@Field final String CCC = 'CommandClassCommand' // wraps a single command class

@Field def smartlog
@Field Long wakeUpPeriod

@Field final Boolean DEBUG_MODE = true


preferences
{
	input(name: "wakeupIntervalHrs", type: "enum", title: "Hours between wakeups (1-24)",
		options: ['1','2','3','4','5','6','7','8','9','10','11','12','13','14','15','16','17','18','19','20','21','22','23','24'],
		defaultValue: PREF_DEFAULT_WAKE_UP_INTERVAL_HR, required: true,
		description: "$PREF_DEFAULT_WAKE_UP_INTERVAL_HR")

	input(name: "tamperClearAuto", type: "boolean", title: "Clear tamper alerts automatically?",
		description: 'Indicate if tamper alerts clear automatically upon wake or state change after the device cover is closed.',
		defaultValue: PREF_DEFAULT_TAMPER_FALSE_ON_WAKE)

	input(name: "wakeupDevFlag", type: "boolean", title: "Development mode",
		description: "Set Wake Up Interval to 10 minutes for testing")
}


metadata
{
	definition (name: 'Monoprice NO-LABEL Shock Sensor or Garage Door Sensor', namespace: 'surfous', author: 'surfous')
	{
		capability 'Battery'
		capability 'Acceleration Sensor'
		capability 'Contact Sensor'
		capability 'Sensor'

		attribute 'sensorType', 'enum', [UNKNOWN_SENSOR_TYPE, 'shock', 'garagedoor']
		attribute 'interpretedValue', 'string'
		attribute 'tamper', 'enum', ['clear', 'detected']
		attribute 'lastUpdated', 'number'
		attribute 'lastSensorReport', 'number'
		attribute 'msr', 'string'
		attribute 'blank', 'string' // just as the name implies...

		command clearTamperManually

		fingerprint deviceId: '0x2001', inClusters: '0x30, 0x71, 0x85, 0x80, 0x72, 0x86, 0x84'
	}

	simulator
	{
		status "wake up" : "command: 8407, payload: "

		status 'alarm_active': zwave.alarmV2.alarmReport(zwaveAlarmType:7, zwaveAlarmEvent:2, alarmLevel:0xff).incomingMessage()
		status 'alarm_inactive': new physicalgraph.zwave.Zwave().alarmV2.alarmReport(zwaveAlarmType:7, zwaveAlarmEvent:2, alarmLevel:0x00).incomingMessage()
		status 'alarm_tamper': new physicalgraph.zwave.Zwave().alarmV2.alarmReport(zwaveAlarmType:1, zwaveAlarmEvent:3).incomingMessage()

		status 'basic_active': new physicalgraph.zwave.Zwave().basicV1.basicSet(value: ZWAVE_TRUE).incomingMessage()
		status 'basic_inactive': new physicalgraph.zwave.Zwave().basicV1.basicSet(value: ZWAVE_FALSE).incomingMessage()

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
	}

	tiles
	{
		standardTile('interpretation', 'device.interpretedValue', width: 2, height: 2)
		{
			state("${SHOCK.SENSOR_TYPE}-${SHOCK.FALSE}", label:"no shock", icon:'st.security.alarm.clear',
				backgroundColor:COLOR_WHITE)
			state("${SHOCK.SENSOR_TYPE}-${SHOCK.TRUE}",   label:"> shock <", icon:'st.security.alarm.alarm',
				backgroundColor:COLOR_RED)

			state("${GARAGEDOOR.SENSOR_TYPE}-${GARAGEDOOR.FALSE}",  label:"closed", icon:'st.doors.garage.garage-closed',
				backgroundColor:COLOR_GREEN)
			state("${GARAGEDOOR.SENSOR_TYPE}-${GARAGEDOOR.TRUE}", label:"open", icon:'st.doors.garage.garage-open',
				backgroundColor:COLOR_ORANGE)

			state(UNKNOWN_SENSOR_TYPE, label: 'unknown', icon:'st.unknown.unknown.unknown', defaultState: true,
				backgroundColor: COLOR_BLACK)
		}

		standardTile('tamper', 'device.tamper')
		{
			state(TAMPER.FALSE, label:"device ok", icon:'st.security.alarm.clear',
				backgroundColor:COLOR_WHITE, defaultState: true)
			state(TAMPER.TRUE, label:"> tamper <", icon:'st.security.alarm.alarm',
				action: 'clearTamperManually', backgroundColor:COLOR_RED)
		}

		standardTile("config", "device.configuration", decoration: "flat")
		{
			state("configure", action: "configuration.configure", icon:"st.secondary.configure")
		}

		valueTile('battery', 'device.battery')
		{
			state('battery', label:'battery ${currentValue}%',
			backgroundColors:[
				[value: 100, color: COLOR_GREEN], // green
				[value: 60,  color: COLOR_GREEN], // green
				[value: 30,  color: COLOR_YELLOW], // yellow
				[value: 1,   color: COLOR_RED], // red
			])
		}

		main(['interpretation'])
		details(['interpretation', 'battery', 'tamper'])
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
	configureMainAttr()
}

/**
 * Called before each invocation by a user-initiated event
 * This could be any capability or custom command, or updated()
 * if the user modifies preferences
 */
void initUserEvent()
{
	configureLogging()
	configureMainAttr()
}

/**
 * Called before each invocation by a SmartThings platform event
 * Typically, these are events fired by the schedule
 */
void initSmartThingsEvent()
{
	configureLogging()
	configureMainAttr()
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

void configureMainAttr()
{
	smartlog.fine 'in configureMainAttr'
	String sensorType = getAttributeSensorType()
	switch (sensorType)
	{
		case GARAGEDOOR.SENSOR_TYPE:
			MAIN_ATTR = GARAGEDOOR
		break
		case SHOCK.SENSOR_TYPE:
			MAIN_ATTR = SHOCK
		break
		default:
			MAIN_ATTR = UNKNOWN
		break
	}
	smartlog.debug "configured MAIN_ATTR sensor type to $MAIN_ATTR.SENSOR_TYPE"
}

/**
 * checks for a repeat event within a configured duration of a previous event
 * @param  filterName name of the filtering event and the attribute where the timestamp is stored
 * @return            true if event is a repeat, false if not
 */
Boolean filterRepeats(String filterName)
{
	smartlog.fine "in filterRepeats for $filterName"
	Long nowTimestamp = now()
	Integer filterDelta = FILTER_REPEAT_DURATIONS[filterName]
	BigDecimal lastFilterTimestamp = device.currentValue(filterName)?:0
	if ((nowTimestamp - lastFilterTimestamp.longValue()) < filterDelta)
	{
		smartlog.debug "repeat of filter event $filterName in less than $filterDelta msec"
		return true
	}
	sendLoggedEvent(name: filterName, value: nowTimestamp)
	return false
}


// -----
//  Device type interface methods
//

/**
 * called every time preferences are updated. moves the settings into a more permanent form.
 */
void updated()
{
	initUserEvent()
	smartlog.fine(DTI, 'in updated')

	// bail if we've run updated() in the past 3 seconds
	if (filterRepeats('lastUpdated')) return null

	state.pref = [:]
	state.pref.wakeupIntervalHrs = Integer.valueOf(settings?.wakeupIntervalHrs?:PREF_DEFAULT_WAKE_UP_INTERVAL_HR)
	state.pref.wakeupDevFlag = Boolean.valueOf(settings?.wakeupDevFlag?:false)
	state.pref.tamperClearAuto = Boolean.valueOf(settings?.tamperClearAuto?:PREF_DEFAULT_TAMPER_FALSE_ON_WAKE)

	smartlog.debug(DTI, "preferences recorded in state: ${state.pref}")
}

/**
 * called each time the device sends a message to the hub. parse then dispatches control to the
 * appropriate handler method
 * @param  rawZwaveEventDescription string holding the message from the device
 * @return                          commands and events, formatted & ready to be sent to the device
 */
def parse(String rawZwaveEventDescription) {
	initDeviceEvent()
	String parseInstance = now() as String
	smartlog.fine(DTI, "parse $parseInstance: raw incoming event '${rawZwaveEventDescription}'")
	def cqReference = CommandQueue()
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
			// If event handler returned a CommandQueue & prepare it accordingly
			if (deviceEventResult instanceof Map && deviceEventResult?.type == cqReference.type )
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
	smartlog.fine(CCMD, "state contains: $state")
	return ccAssociationGet()
}


// -----
// Custom commands
//
def clearTamperManually()
{
	initUserEvent()
	smartlog.fine(CCMD, 'in clearTamperManually')
	return clearTamper(TAMPER.FALSE_MANUAL)
}


// -----
//	Local methods
//

private String formatNumberAsHex(Number num)
{
	if (num != null) return sprintf('%#x', num)
}

private void sendLoggedEvent(Map eventMap)
{
	smartlog.info "sendLoggedEvent - sending immediate event: $eventMap"
	sendEvent(eventMap)
}

private void clearTamper(String clearMethod)
{
	smartlog.fine "in clearTamper with arg '$clearMethod'"
	smartlog.trace "current tamper attribute value is ${device.currentValue('tamper')}"
	if (device.currentValue(TAMPER.ATTR_NAME) != TAMPER.FALSE)
	{
		Map evtMap = [name: TAMPER.ATTR_NAME, value: TAMPER.FALSE, description: "tamper alert cleared $clearMethod", isStateChange: true, displayed: true]
		sendLoggedEvent(evtMap)
	}
}

void overrideWakeupInterval(Number seconds)
{
	String ovrd = 'overrideSeconds'
	if (!state?.wakeup) state.wakeup = [:]
	if (seconds)
	{
		Integer intSeconds = seconds as Integer
		smartlog.info("setting an override wakeup interval of $intSeconds seconds - will set on device next time it wakes up")
		state.wakeup[ovrd] = intSeconds
	}
	else if (state.wakeup.containsKey(ovrd))
	{
		smartlog.info("clearing any override wakeup interval - will return device to normal interval on next wake")
		state.wakeup.remove(ovrd)
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
		sendEvent([name: 'EXCEPTION', value: addlExceptionMsg,
			description: addlExceptionMsg, descriptionText: addlDescText, displayed: true])
	}
	finally
	{
		smartlog.error( msg)
		sendEvent(evtMap)
	}
}

/**
 * Gets the name of a zwave command method
 * @param  zwaveCmd a zwave command received from the device and parsed by the zwave utility class
 * @return          The name of the Z-Wave command method
 */
String getZwaveCommandName(physicalgraph.zwave.Command zwaveCmd)
{
	String zwaveCmdStr = zwaveCmd as String
	return zwaveCmdStr.replaceAll(~/\(.*$/, '')
}

// -----
//  Tasks
//
def taskGetWakeupCapabilities()
{
	smartlog.fine('wakeup tasks: in taskGetWakeupCapabilities')
	def cq = CommandQueue()
	try
	{
		if (! state?.wakeup)
		{
			String msg = 'state.wakeup evals to false - checking wakeup interval capabilities'
			smartlog.debug(msg)
			Command wakeupGetCmd = ccWakeUpIntervalCapabilitiesGet()
			cq.add([name: 'taskGetWakeupCapabilities', value: true, description: wakeupGetCmd as String, descriptionText: msg, displayed: false])
			cq.add(wakeupGetCmd)
		}
		else
		{
			smartlog.trace('state.wakeup evals to true - no need to fetch wakeup interval capabilities again')
		}
	}
	catch (Throwable ex)
	{
		sendExceptionEvent(ex, 'exception caught in taskGetWakeupCapabilities')
	}
	return cq
}

def taskGetAssociation()
{
	smartlog.fine('wakeup tasks: in taskGetAssociation')
	def cq = CommandQueue()

	try
	{
		def associationLastQueryTime = state?.associationLastQueryTime?:0
		Long msecSinceLastAssocQuery = now() - (associationLastQueryTime as Long)
		String msg = "assoc node id in state: ${state?.associationNodeId} hub: $zwaveHubNodeId; Last query: $associationLastQueryTime msec since: $msecSinceLastAssocQuery; inerval msec: $ASSOC_CHECK_INTERVAL_MSEC (dev flag ${state?.pref?.wakeupDevFlag})"
		log.debug msg
		Map eventMap = [name: 'taskGetAssociation', displayed: false, descriptionText: msg]
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
		sendExceptionEvent(ex, 'exception caught in taskGetAssociation')
	}
	return cq
}

Map taskGetBattery()
{
	smartlog.fine('wakeup tasks: in taskGetBattery')
	def cq = CommandQueue()
	try
	{
		Command battGetCmd = ccBatteryGet()
		cq.add(battGetCmd)
	}
	catch (Throwable ex)
	{
		sendExceptionEvent(ex, 'exception caught in taskGetBattery')
	}
	return cq
}

Map taskGetWakeupInterval()
{
	smartlog.trace('wakeup tasks: in taskGetWakeupInterval')
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
		sendExceptionEvent(ex, 'exception caught in taskGetWakeupInterval')
	}
	return cq
}

void taskCheckTamperState()
{
	smartlog.trace('wakeup tasks: in taskCheckTamperState')
	try
	{
		Boolean tamperAutoClear = state?.pref?.tamperClearAuto?:PREF_DEFAULT_TAMPER_FALSE_ON_WAKE
		smartlog.debug "tamperClearAuto on wake: $tamperAutoClear; default: $PREF_DEFAULT_TAMPER_FALSE_ON_WAKE"
		if (tamperAutoClear && device.currentValue('tamper') == TAMPER.TRUE) // check tamper attribute.
		{
			// if it's 'detected' set it to 'clear' as it seems that WakeUpNotification is not sent during
			// tamper, but device wakes immediately after tamper clears
			smartlog.debug 'Clearing tamper automatically on wake'
			clearTamper(TAMPER.FALSE_ON_WAKE)
		}
	}
	catch (Throwable ex)
	{
		sendExceptionEvent(ex, 'exception caught in taskCheckTamperState')
	}
}

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

void initDeviceMetadata()
{
	if (state?.deviceMeta == null)
	{
		state.deviceMeta = [:]
	}
}

Boolean isDeviceMetadataChainComplete()
{
	Boolean isComplete = false
	initDeviceMetadata()
	if (state.deviceMeta?.msr &&
			state.deviceMeta?.version &&
			state.deviceMeta?.wakeup)
	{
		isComplete = true
	}
	return isComplete
}

/**
 * takes the productTypeId from the msr stored in state and looks up the corresponding sensor type
 * @return String describing the sensor type
 */
String determineSensorType()
{
	smartlog.fine 'in determineSensorType'
	String sensorType = UNKNOWN_SENSOR_TYPE
	if (state?.deviceMeta?.msr?.productTypeId != null)
	{
		String productTypeIdString = formatNumberAsHex(state?.deviceMeta?.msr?.productTypeId)
		switch(productTypeIdString)
		{
			case formatNumberAsHex(GARAGEDOOR.PRODUCT_TYPE_ID):
				sensorType = GARAGEDOOR.SENSOR_TYPE
			break
			case formatNumberAsHex(SHOCK.PRODUCT_TYPE_ID):
				sensorType = SHOCK.SENSOR_TYPE
			break
		}
	}
	smartlog.debug "determineSensorType has determined sensor to be of type '$sensorType'"
	return sensorType
}

/**
 * uses the sensorType return from determineSensorType and the sensorType device attribute
 * to set the sensorType device attribute if it has changed.
 */
void setAttributeSensorType()
{
	smartlog.fine 'in setAttributeSensorType'
	String sensorType = determineSensorType()
	String sensorTypeAttr = getAttributeSensorType()
	if (sensorTypeAttr != sensorType)
	{
		String actionDesc = "sensorType attr is being changed to $sensorType"
		sendLoggedEvent([name: 'sensorType', value: sensorType, isStateChange: true, displayed: true, description: actionDesc])
		smartlog.info actionDesc
	}
}

/**
 * accessor for sensorType with a default value for a null attribute
 * @return String attribute sensorType
 */
String getAttributeSensorType()
{
	String sensorType = device.currentValue('sensorType')
	String logMsg = "device attr 'sensorType' is $sensorType."
	if (sensorType == null)
	{
		sensorType = UNKNOWN.SENSOR_TYPE
		logMsg += " returning null sensorType as $sensorType."
	}
	smartlog.debug
	return sensorType
}

def reinterpretUnknownSensorReport()
{
	smartlog.fine 'in reinterpretUnknownSensorReport'
	if (MAIN_ATTR.SENSOR_TYPE != UNKNOWN.SENSOR_TYPE)
	{
		String logMsg = "reinterpret sensor value '$sensorValue' as $MAIN_ATTR.SENSOR_TYPE"
		Boolean sensorValue = state?.lastUnknownSensorReport?:MAIN_ATTR.FALSE
		Map replayEvt = [name: 'intertpretedValue',
			value: "$MAIN_ATTR.SENSOR_TYPE-${MAIN_ATTR.STATEMAP[sensorValue]}",
			description: logMsg,
			isStateChange: true, displayed: true]
		sendLoggedEvent(replayEvt)
		state.remove('lastUnknownSensorReport')
	}
}

def handleSensorReport(Boolean sensorValue, physicalgraph.zwave.Command deviceEvent)
{
	smartlog.fine "in handleSensorReport with value '$sensorValue' and zwave cmd '$deviceEvent'"

	// bail if we've handled a sensor report in the past 2 seconds
	if (filterRepeats('lastSensorReport')) return null

	def cq = CommandQueue()
	configureMainAttr()
	//String sensorType = getAttributeSensorType()
	String commandName = getZwaveCommandName(deviceEvent)

	Map sensorEvent = [:]
	Map interpretedEvent = [name: 'interpretedValue', value: UNKNOWN.UNKNOWN, isStateChange: true]
	if (MAIN_ATTR.SENSOR_TYPE != UNKNOWN.SENSOR_TYPE)
	{
		sensorEvent.name = MAIN_ATTR.ATTR_NAME
		sensorEvent.value = MAIN_ATTR.STATEMAP[sensorValue]
		sensorEvent.description = deviceEvent as String
		sensorEvent.descriptionText = "${device.displayName}: $MAIN_ATTR.ATTR_LABEL is $sensorEvent.value (via $commandName)"

		if (device.currentValue(sensorEvent.name) != sensorEvent.value)
		{
			smartlog.debug "device attribute $sensorEvent.name has changed"
			sensorEvent.isStateChange = true
			sensorEvent.displayed = true

			interpretedEvent.isStateChange = true
			interpretedEvent.displayed = true
		}
		cq.add sensorEvent

		interpretedEvent.value = "$MAIN_ATTR.SENSOR_TYPE-$sensorEvent.value"
	}
	else
	{
		state.lastUnknownSensorReport = sensorValue
	}

	smartlog.debug "setting interpretedValue attr to $interpretedEvent.value"
	cq.add interpretedEvent

	return cq
}


// -----
// CommandClass commands, event handlers and helper methods
//

// // CommandClass WakeUp

def zwaveEvent(physicalgraph.zwave.commands.wakeupv2.WakeUpNotification deviceEvent) {
	smartlog.trace("handling WakeUpNotification '$deviceEvent'")
	Long period = now()
	def cq = CommandQueue()
	sendEvent([name: "wakeup-$period", value: 'wakeUpNotification', isStateChange: true, description: deviceEvent as String, descriptionText: "${device.displayName} woke up.", displayed: false])

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
		clearTamper(TAMPER.FALSE_INIT)
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
	if (!seconds || seconds < MIN_WAKE_UP_INTERVAL_SEC) seconds = state?.wakeup?.minSeconds?:MIN_WAKE_UP_INTERVAL_SEC
	if (seconds > MAX_WAKE_UP_INTERVAL_SEC) seconds = state?.wakeup?.maxSeconds?:MAX_WAKE_UP_INTERVAL_SEC
	GString logMsg = "WakeUpIntervalSet to $seconds seconds"
	if (state?.wakeup?.overrideSeconds)
	{
		logMsg += ' (override wakeup interval found in state - using its value)'
		seconds = state?.wakeup?.overrideSeconds as Integer
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
	smartlog.trace("handling WakeUpIntervalReport '$deviceEvent'")
	def cq = CommandQueue()
	Integer intervalSeconds = deviceEvent.seconds
	Short nodeId = deviceEvent.nodeid
	if (!state?.cfg) state.cfg = [:]
	state.cfg.wakeupSeconds = intervalSeconds
	String msg = "Device ${device.displayName} reports a wake up interval of $intervalSeconds for node $nodeId"
	smartlog.info msg
	Map evtMap = [name: 'wakeInterval', value: intervalSeconds, unit: 'seconds', description: deviceEvent as String, descriptionText: msg, displayed: true]
	cq.add evtMap
	return cq
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

// // CommandClass Association v2
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
		cq.add(ccAssociationSet(), DEFAULT_INTERCMD_DELAY_MSEC * 2)
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

	Boolean sensorValue = false
	if (deviceEvent.sensorValue == physicalgraph.zwave.commands.sensorbinaryv1.SensorBinaryReport.SENSOR_VALUE_DETECTED_AN_EVENT)
	{
		sensorValue = true
	}

	cq.add handleSensorReport(sensorValue, deviceEvent)
	return cq
}

// // CommandClass Basic
def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicSet deviceEvent)
{
	smartlog.fine(ZWEH, "handling BasicSet '$deviceEvent'")
	def cq = CommandQueue()

	Boolean sensorValue = false
	if (deviceEvent.value == ZWAVE_TRUE)
	{
		sensorValue = true
	}
	cq.add handleSensorReport(sensorValue, deviceEvent)
	return cq
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
	def cq = CommandQueue()
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
	cq.add evtMap
	return cq
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
	def versionReportText = "$device.displayName: application version: ${deviceEvent.applicationVersion}, Z-Wave firmware version: ${deviceEvent.applicationSubVersion}, Z-Wave lib type: ${deviceEvent.zWaveLibraryType}, Z-Wave version: ${deviceEvent.zWaveProtocolVersion}.${deviceEvent.zWaveProtocolSubVersion}"

	initDeviceMetadata()
	state.deviceMeta.version = [:]
	state.deviceMeta.version.application = deviceEvent.applicationVersion
	state.deviceMeta.version.zwaveFirmware = deviceEvent.applicationSubVersion
	state.deviceMeta.version.zwaveLibraryType = deviceEvent.zWaveLibraryType
	state.deviceMeta.version.zwaveProtocol = deviceEvent.zWaveProtocolVersion
	state.deviceMeta.version.zwaveProtocolSub = deviceEvent.zWaveProtocolSubVersion

	smartlog.info "VersionReport: $versionReportText"
	sendLoggedEvent(name: 'version', value: fw, description: deviceEvent as String, descriptionText: versionReportText, displayed: true)
	cq.add(chainDeviceMetadata())
	return cq
}

// CommandClass Manufacturer Specific v1

def ccManufacturerSpecificGet()
{
	smartlog.fine(CCC, 'issuing ccManufacturerSpecificGet')
	def cmd = zwave.manufacturerSpecificV1.manufacturerSpecificGet()
	return cmd
}

def zwaveEvent(physicalgraph.zwave.commands.manufacturerspecificv1.ManufacturerSpecificReport deviceEvent)
{
	smartlog.fine(ZWEH, 'handling event manufacturerspecificv1.ManufacturerSpecificReport')
	def shortMsr = String.format("%04X-%04X-%04X", deviceEvent.manufacturerId, deviceEvent.productTypeId, deviceEvent.productId)
	updateDataValue("msr", shortMsr)

	initDeviceMetadata()
	state.deviceMeta.msr = [msr: "$shortMsr"]
	state.deviceMeta.msr.manufacturerId = deviceEvent.manufacturerId
	state.deviceMeta.msr.productTypeId = deviceEvent.productTypeId
	state.deviceMeta.msr.productId = deviceEvent.productId

	// set the sensor type if necessary
	setAttributeSensorType()
	reinterpretUnknownSensorReport()

	String niceMsr = "$device.displayName MSR: $shortMsr; Manufacturer: $state.deviceMeta.msr.manufacturerId; Product Type: $state.deviceMeta.msr.productTypeId; Product: $state.deviceMeta.msr.productId"

	smartlog.info "ManufacturerSpecificReport: $niceMsr"
	sendLoggedEvent([name: 'msr', value: shortMsr, description: state.deviceMeta.msr.toString(), description: deviceEvent as String, descriptionText: niceMsr, displayed: true, isStateChange: true])
	def cq = CommandQueue()
	cq.add(chainDeviceMetadata())
	return cq
}


// CommandClass Alarm
def zwaveEvent(physicalgraph.zwave.commands.alarmv2.AlarmReport deviceEvent)
{
	smartlog.fine(ZWEH, "handling AlarmReport '$deviceEvent'")
	def cq = CommandQueue()
	String alarmMsg = "${device.displayName}: Alarm Type ${deviceEvent.zwaveAlarmType}; Event ${deviceEvent.zwaveAlarmEvent}; Status ${deviceEvent.zwaveAlarmStatus};   (V1 info Type:${deviceEvent.alarmType}; Level:${deviceEvent.alarmLevel})"

	smartlog.debug alarmMsg
	switch (deviceEvent)
	{
		case {it.zwaveAlarmType == 7 && it.zwaveAlarmEvent == 2 && it.alarmLevel == ZWAVE_TRUE}:
			// this is the alarm for motion detected
			cq.add handleSensorReport(true, deviceEvent)
			break
		case {it.zwaveAlarmType == 7 && it.zwaveAlarmEvent == 2 && it.alarmLevel == ZWAVE_FALSE}:
			// this is the alarm for motion ceased
			cq.add handleSensorReport(false, deviceEvent)
			break
		case {it.zwaveAlarmType == 7 && it.zwaveAlarmEvent == 3}:
			// this is the alarm for tamper - There seems to be no cleared alarm for this
			Map tamperEvt = [name: TAMPER.ATTR_NAME, value: TAMPER.TRUE]
			tamperEvt.displayed = true
			tamperEvt.description = deviceEvent as String
			tamperEvt.descriptionText = "${device.displayName}: sensor cover has been opened"
			smartlog.info tamperEvt.descriptionText
			cq.add tamperEvt
			break
		default:
			Map defaultEvt = [name: "alarm${deviceEvent.zwaveAlarmType}"]
			defaultEvt.value =  "v2event ${deviceEvent.zwaveAlarmEvent}, v2status ${deviceEvent.zwaveAlarmStatus}, v1level ${deviceEvent.alarmLevel}"
			defaultEvt.description = deviceEvent as String
			defaultEvt.descriptionText = alarmMsg
			defaultEvt.displayed = true
			cq.add defaultEvt
	}
	return cq
}

def ccAlarmTypeSupportedGet()
{
	smartlog.fine(CCC, ' issuing AlarmTypeSupportedGet')
	return zwave.alarmV2.alarmTypeSupportedGet()
}

def zwaveEvent(physicalgraph.zwave.commands.alarmv2.AlarmTypeSupportedReport deviceEvent)
{
	smartlog.fine(ZWEH, "handling AlarmTypeSupportedReport '$deviceEvent'")
	smartlog.info("AlarmTypeSupportedReport: '$deviceEvent'")
}

// Catch-all handler.for any unsupported zwave events the device might send at us
def zwaveEvent(physicalgraph.zwave.Command deviceEvent)
{
	// Catch-all handler. The sensor does return some alarm values, which
	// could be useful if handled correctly (tamper alarm, etc.)
	String message = "Unhandled device event: ${deviceEvent}"
	smartlog.warn message
	def cq = CommandQueue()
	cq.add([name: 'unhandled', value:'', description: deviceEvent as String, descriptionText: message, displayed: false])
	return cq
}


/*
	ClosureClasses down here.
 */

// BEGIN Smartlog
def Smartlog(String defaultScope=null)
{
	Map sl = [:]
	sl.type = 'Smartlog'
	sl.version = '20150530a'

	sl.SCRIPT_SCOPE = 'script'
	sl.INSTANCE_DEFAULT_SCOPE = defaultScope?:sl.SCRIPT_SCOPE
	sl.OVERRIDE_SCOPE = 'OVERRIDE'

	sl.LEVEL_NONE = null
	sl.LEVEL_ERROR = 'error'
	sl.LEVEL_WARN = 'warn'
	sl.LEVEL_INFO = 'info'
	sl.LEVEL_DEBUG = 'debug'
	sl.LEVEL_TRACE = 'trace'
	sl.LEVEL_FINE = 'FINEtrace'

	sl.SMARTLOG_LEVELS = [sl.LEVEL_NONE, sl.LEVEL_ERROR, sl.LEVEL_WARN, sl.LEVEL_INFO, sl.LEVEL_DEBUG, sl.LEVEL_TRACE, sl.LEVEL_FINE]

	sl.SMARTLOG_DEFAULT_LEVEL = sl.LEVEL_DEBUG

	sl.format =
	{
		String scope, String msg ->
		if (scope && scope != sl.SCRIPT_SCOPE) return "[$scope] $msg"
		return msg
	}

	sl.callWrappedLogMethod =
	{
		String scope, String level, String msg ->
		def logLevelMatcher = (level =~ /^([A-Z]+)?([a-z]+)$/)
		String logLevel = logLevelMatcher[0][2]
		log."${logLevel}"(sl.format(scope, msg))
	}

	// for logging within Smartlog
	sl.METALOG_DEFAULT_LEVEL = sl.LEVEL_NONE
	sl.metalog =
	{
		String level, String msg ->
		String scope = sl.type
		String metalogLevel = state.smartlog.get(scope, sl.METALOG_DEFAULT_LEVEL)
		if (sl.SMARTLOG_LEVELS.indexOf(metalogLevel) >= sl.SMARTLOG_LEVELS.indexOf(level))
		{
			sl.callWrappedLogMethod(scope, level, msg)
		}
	}

	sl.log =
	{
		Object[] varArgs ->
		List argList = ([null, null] + varArgs).flatten()
		if (argList.size() < 3)
		{
			// At least one arg, the message, must be passed in
			sl.metalog(sl.LEVEL_ERROR, "log called with no arguments")
			return
		}
		String scope = argList[-3]?:sl.INSTANCE_DEFAULT_SCOPE
		String level = argList[-2]
		String msg = argList[-1]

		sl.metalog(sl.LEVEL_DEBUG, "in log with args scope: $scope; lvl: $level; '$msg'")

		if (!level || !sl.SMARTLOG_LEVELS.contains(level))
		{
			// set a default level
			level = sl.SMARTLOG_DEFAULT_LEVEL
		}

		String scopeActiveLevel = sl.getLevel(scope)
		if (scopeActiveLevel == sl.LEVEL_NONE) return // logging is off
		if (sl.SMARTLOG_LEVELS.indexOf(scopeActiveLevel) >= sl.SMARTLOG_LEVELS.indexOf(level))
		{
			sl.callWrappedLogMethod(scope, level, msg)
		}
	}

	sl.convertLogArgs =
	{
		String level, Object[] varArgs ->
		List argList = ([sl.INSTANCE_DEFAULT_SCOPE] + varArgs).flatten()
		if (argList.size() < 2)
		{
			// At least one arg, the message, must be passed in
			sl.metalog(sl.LEVEL_ERROR, "log level $level helper called with no arguments")
			return
		}
		List logArgs = [argList[-2] as String, level, argList[-1] as String]
		sl.metalog(sl.LEVEL_DEBUG, "convertLogArgs is returning $logArgs")
		return logArgs
	}

	// log level helpers
	//
	sl.error =
	{
		Object[] varArgs ->
		List logArgs = sl.convertLogArgs(sl.LEVEL_ERROR, varArgs)
		sl.log(*logArgs)
	}

	sl.warn =
	{
		Object[] varArgs ->
		List logArgs = sl.convertLogArgs(sl.LEVEL_WARN, varArgs)
		sl.log(*logArgs)
	}

	sl.info =
	{
		Object[] varArgs ->
		List logArgs = sl.convertLogArgs(sl.LEVEL_INFO, varArgs)
		sl.log(*logArgs)
	}

	sl.debug =
	{
		Object[] varArgs ->
		List logArgs = sl.convertLogArgs(sl.LEVEL_DEBUG, varArgs)
		sl.log(*logArgs)
	}

	sl.trace =
	{
		Object[] varArgs ->
		List logArgs = sl.convertLogArgs(sl.LEVEL_TRACE, varArgs)
		sl.log(*logArgs)
	}

	sl.fine =
	{
		Object[] varArgs ->
		List logArgs = sl.convertLogArgs(sl.LEVEL_FINE, varArgs)
		sl.log(*logArgs)
	}

	sl.initialize =
	{
		->
		if (state?.smartlog == null)
		{
			state.smartlog = [:]
			sl.setLevel(scope: sl.INSTANCE_DEFAULT_SCOPE, level: sl.SMARTLOG_DEFAULT_LEVEL)
			sl.setLevel(scope: sl.type, level: sl.LEVEL_NONE) // set our own level off by default
			sl.metalog(sl.LEVEL_DEBUG, 'state initialized' )
		}
	}

	sl.reset =
	{
		->
		sl.metalog(sl.LEVEL_TRACE, 'resetting state' )
		if (state?.smartlog != null) state.remove('smartlog')
		sl.initialize()
	}

	sl.setLevel =
	{
		Map optArgs = [:] ->
		String scope = optArgs?.scope?:sl.INSTANCE_DEFAULT_SCOPE
		String level = optArgs?.level
		if (!optArgs.containsKey('level')) level = sl.SMARTLOG_DEFAULT_LEVEL

		sl.metalog(sl.LEVEL_TRACE, "in setSmarlLogLevel(scope: ${optArgs?.scope}, level: ${optArgs?.level})")
		if (sl.SMARTLOG_LEVELS.contains(level)) // check validity of level
		{
			sl.metalog(sl.LEVEL_DEBUG, "Actually setting log level for scope '$scope' to '$level'")
			state.smartlog[scope] = level
			sl.metalog(sl.LEVEL_FINE, "Now the smartog state contains ${state.smartlog}")
		}
	}

	sl.setOverrideLevel =
	{
		String level ->
		sl.metalog(sl.LEVEL_TRACE, "in setOverrideLevel($level)")
		if (sl.SMARTLOG_LEVELS.contains(level))
		{
			sl.metalog(sl.LEVEL_DEBUG, "Actually setting override log level for all scopes to '$level'")
			state.smartlog[sl.OVERRIDE_SCOPE] = level
		}
	}

	sl.clearOverride =
	{
		->
		sl.metalog(sl.LEVEL_TRACE, "in clearOverride")
		if (state.smartlog.get(sl.OVERRIDE_SCOPE))
		{
			state.smartlog.remove(sl.OVERRIDE_SCOPE)
			sl.metalog(sl.LEVEL_DEBUG, "Removed override log level")
		}
	}

	// no logging from this member!!
	sl.getLevel =
	{
		String scope->
		scope = scope?:sl.INSTANCE_DEFAULT_SCOPE
		sl.metalog(sl.LEVEL_TRACE, "in getLevel for $scope")

		// if override is set, return it
		String overrideLevel = state.smartlog.get(sl.OVERRIDE_SCOPE)
		if (overrideLevel) return overrideLevel

		String scriptScopeLevel = state.smartlog.get(sl.SCRIPT_SCOPE, sl.SMARTLOG_DEFAULT_LEVEL)
		String level = state.smartlog.get(scope, scriptScopeLevel)
		return level
	}

	sl.initialize()
	sl.metalog(sl.LEVEL_DEBUG, "Constructed Smartlog instance with state of $state.smartlog")

	return sl
}
// END Smartlog

/*BEGIN-CUT-CommandQueue*/
/**
 * Low-rent "class" that is a queue for z-wave commands. Commands may be added one at a time,
 * or as a list. Default delays are automatically added between commands unless a specific
 * duration delay is specified after a command. Default delays are not added after another delay
 * command or if the command itself is a delay command. The list of commands may be extracted at
 * any time.
 */

def CommandQueue()
{
	def cq = [:] // the "object" map
	cq.type = 'CommandQueue'
	cq.version = '20150623r1'
	cq.entryList = [] // list to hold the commands

	cq.smartlog = Smartlog(cq.type)

	cq.smartlog.fine("constructing ${cq.type} instance")

	/**
	* Add a command or list of commands to the end of the queue
	*
	* @param cmd single Text command to add or list of string commands to add
	* @param delay (optional) custom delay in milliseconds to add after each command
	*/
	cq.add =
	{
		entry, Number delayMs = null ->
		cq.smartlog.fine("add '$entry' $delayMs")
		String entryStr =  "entry: $entry ; delayMs: $delayMs"
		if (!entry)
		{
			cq.smartlog.debug("entry evaluates to false, discarding. ($entryStr)")
		}
		else if (entry instanceof List)
		{
			String delayStr = "a delay of $delayMs ms"
			if (delayMs == null)
			{
				"the default delay of $DEFAULT_INTERCMD_DELAY_MSEC ms"
			}

			cq.smartlog.debug("entry is a list with ${entry.size()} members - adding each entry with $delayStr where a delay isn't explicitly added between commands")
			// if custom delay is specified, each command will have this delay.
			entry.each { oneEntry -> cq.add(oneEntry, delayMs) }
			cq.smartlog.debug("finished adding list.")
		}
		else if (cq.isCommandQueue(entry))
		{
			String delayStr = ''
			if (delayMs == null) delayStr = " Ignoring supplied delay of $delayMs ms as the CommandQueue will already have delays where needed"
			cq.smartlog.debug("entry is a CommandQueue, adding as list of member entries.$delayStr")
			cq.add(entry.getEntries())
			cq.smartlog.debug("finished adding CommandQueue.")
		}
		else if (cq.isCommand(entry))
		{
			cq.smartlog.debug("entry is a command, adding. ($entryStr)")
			cq.__addCommand(entry, delayMs)
		}
		else if (cq.isDelay(entry))
		{
			cq.smartlog.debug("entry is a delay, adding. ($entryStr)")
			cq.__addDelay(entry)
		}
		else if (cq.isEvent(entry))
		{
			cq.smartlog.debug("entry is an event, adding. ($entryStr)")
			cq.__addEvent(entry)
		}
		else
		{
			cq.smartlog.warn("entry parameter to add() was not a Command, List, CommandQueue, Event or delay. discarding. ($entryStr)")
		}
	}

	cq.__addCommand =
	{
		Command entry, Number delayMs=null ->
		cq.smartlog.fine("__addCommand '$entry' $delayMs")
		if (cq.isCommand(entry))
		{
			// first, add a delay if the previous entry was a command and this command isn't a delay
			if  (cq.length() > 0 && !cq.isDelay(entry) && cq.isLastEntryACommand())
			{
				cq.__addDelay(cq.formatDelayCmd(DEFAULT_INTERCMD_DELAY_MSEC)) // always the default delay
			}

			cq.entryList << entry // now, add the command to the queue

			// Add a delay afterwards if a custom delay is specified
			if (delayMs)
			{
				cq.__addDelay(cq.formatDelayCmd(delayMs))
			}
		}
		else
		{
			cq.smartlog.warn("entry parameter to __addCommand is not a command: ${entry}. discarding.")
		}
	}

	/**
	* Add a delay command to the end of the queue. If no delay is specified, or it's not an integer,
	* a default delay will be added.
	*
	* @param delay The delay duration in milliseconds (optional)
	*/
	cq.__addDelay =
	{
		String entry ->
		cq.smartlog.fine("__addDelay '$entry'")
		if (entry && cq.isDelay(entry))
		{
			cq.entryList << entry
		}
		else
		{
			cq.smartlog.warn("entry parameter to __addDelay is not a delay command: ${entry}. discarding.")
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
	cq.prepend =
	{
		// Can only prepend a command
		entry, Number delayMs=null ->
		cq.smartlog.fine("prepend '$entry' $delayMs")
		String entryStr =  "entry: $entry ; delayMs: $delayMs"
		if (cq.isEvent(entry))
		{
			cq.smartlog.debug("entry is an event, prepending. ($entryStr)")
			cq.__prependEvent(entry)
		}
		else if (cq.isCommand(entry))
		{
			cq.__prependCommand(entry, delayMs)
		}
		else
		{
			cq.smartlog.warn("entry parameter to prepend is not a command: ${entry}. discarding.")
		}
	}

	cq.__prependCommand =
	{
		Command cmd, Number delayMs=null ->
		cq.smartlog.fine("__prependCommand '$cmd' $delayMs")
		if (cq.isCommand(cmd))
		{
			// first, prepend a delay to the front of the queue if there are already commands in it
			if (cq.length() > 0)
			{
				cq.__prependDelay(cq.formatDelayCmd(delayMs))
			}
			cq.entryList.add(0, cmd)
		}
		else
		{
			cq.smartlog.warn("parameter to __prependCommand is not a command: ${cmd}. discarding.")
		}
	}

	/**
	* Add a delay command to the front of the queue. If no delay is specified, or it's not an integer,
	* a default delay will be added.
	*
	* @param delay The delay duration in milliseconds (optional)
	*/
	cq.__prependDelay =
	{
		String entry ->
		cq.smartlog.fine("__prependDelay $entry")
		if (cq.isDelay(entry))
		{
			cq.entryList.add(0, entry)
		}
	}

	cq.__addEvent =
	{
		Map event ->
		cq.smartlog.fine("__addEvent $event")
		if (cq.isEvent(event))
		{
			// wrapping the event simply fills out other members of the event.
			// wrapping an already wrapped event is safe.
			cq.entryList << createEvent(event)
		}
		else
		{
			cq.smartlog.warn("parameter to __addEvent is not an event: ${event}. discarding.")
		}
	}

	cq.__prependEvent =
	{
		Map event ->
		cq.smartlog.fine("__prependEvent $event")
		if (cq.isEvent(event))
		{
			// wrapping the event simply fills out other members of the event.
			// wrapping an already wrapped event is safe.
			cq.entryList.add(0, createEvent(event))
		}
		else
		{
			cq.smartlog.warn("parameter to __prependEvent is not an event: ${event}. discarding.")
		}
	}

	cq.isEvent =
	{
		entry ->
		Boolean testResult = entry instanceof Map && entry?.name && (entry?.type == null || entry?.getEntries == null)
		cq.smartlog.fine("'$entry': $testResult")
		return testResult
	}

	cq.isCommand =
	{
		def entry ->
		Boolean testResult = entry instanceof Command
		cq.smartlog.fine("isCommand '$entry': $testResult")
		return testResult
	}

	cq.isDelay =
	{
		def entry ->
		Boolean testResult = entry instanceof String && (entry ==~ /delay\s\d+?/)
		cq.smartlog.fine("isDelay '$entry': $testResult")
		return testResult
	}

	cq.isCommandQueue =
	{
		def entry ->
		Boolean testResult = entry instanceof Map && entry?.getEntries && entry.getEntries instanceof Closure
		cq.smartlog.fine("isCommandQueue: $testResult")
		return testResult
	}

	// has the entry been wrapped in a HubAction
	cq.isResponse =
	{
		entry ->
		cq.smartlog.fine("isResponse $entry")
		return entry instanceof HubAction
	}

	/**
	* Checks if the last non-event entry added to the queue is a Command or not
	* @return true if the last entry on the queue is a non-delay string, false if not or the queue is empty
	*/
	cq.isLastEntryACommand =
	{
		cq.smartlog.fine('isLastEntryACommand')

		// strip away events
		if ( cq.length() > 0)
		{
			// now, in the filtered list, is the last entry a command, or delay (or are there no entries)?
			List justCommands = cq.entryList.findAll{ !cq.isEvent(it) }
			if (justCommands && cq.isCommand(justCommands.last() ) ) return true
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
	cq.formatDelayCmd =
	{
		Number delayMs=null ->
		cq.smartlog.fine("formatDelayCmd $delayMs")
		if (delayMs)
		{
			Integer delayMsInt = delayMs.intValue()
			return "delay $delayMsInt"
		}
		else
		{
			return "delay $DEFAULT_INTERCMD_DELAY_MSEC"
		}
		return null
	}

	/**
	* returns the current size of the command queue, including automatically generated delay commands
	* @return the number of commands in the command queue
	*/
	cq.length =
	{
		cq.smartlog.fine('length')
		return cq.entryList.size()
	}

	/**
	* returns the raw entry list
	* @return list of entries in the command queue
	*/
	cq.getEntries =
	{
		cq.smartlog.fine('getEntries')
		return cq.entryList
	}

	/**
	 * commands have their format method run to generate the raw zwave command in preparation
	 * for sending to the device as an initiating command (not a response)
	 */
	cq.formatEntry =
	{
		if (cq.isCommand(it)) return it.format()
		return it
	}

	/**
	 * Commands or delays are each wrapped individually in a HubAction, while events are not
	 * @return an appropriately formatted entry
	 */
	cq.formatEntryForResponse =
	{
		cq.smartlog.fine("formatEntryForResponse")
		return (cq.isCommand(it) || cq.isDelay(it)) ? response(it) : it
	}

	/**
	* returns the command queue
	* @return list of commands in the command queue prepared to return from parse
	*/
	// get the list of commands
	cq.prepare =
	{
		return cq.assemble()
	}

	/**
	 * Generates a response suitable to send to the device
	 * @return List of formatted commands and delays (but events are removed)
	 */
	cq.assemble =
	{
		List assembledEntryList = []
		cq.smartlog.fine('prepare')
		cq.entryList.each { assembledEntryList << cq.formatEntry(it) }
		return assembledEntryList
	}

	/**
	 * Generates a response suitable to return from parse()
	 * @return List of formatted commands, delays and events ready to return from parse()
	 */
	cq.assembleResponse =
	{
		cq.smartlog.fine("assembleResponse")
		// then prepare the remaining commands and delays in order
		List assembledResponseList = []
		cq.entryList.each { assembledResponseList << cq.formatEntryForResponse(it) }
		return assembledResponseList
	}

	return cq
}
/*END-CUT-CommandQueue*/
