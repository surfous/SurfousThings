/**
 *  Monoprice NO-LABEL (Vision OEM) Motion Sensor
 *
 *  Capabilities: Motion Sensor, Temperature Measurement, Battery Indicator
 *
 *  Notes: For the Inactivity Timeout to update or Battery level (only for the first time),
 *	you have to open the Motion Sensor and leave it open for a few seconds and then close it.
 *	This triggers the forced Wake up so that the settings can take effect immediately.
 *
 *  Raw Description: 0 0 0x2001 0 0 0 9 0x71 0x85 0x80 0x72 0x30 0x86 0x31 0x70 0x84
 *
 *  Icon credite:
 *   - Battery by Ahmed Elzahra from the Noun Project
 *
 *  Test edit on IDE to upload
 *
 *  Author: surfous
 *  Date: 2016-04-03
 *  Build: 20160403-063217.69332
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

@Field final Integer DEFAULT_INTERCMD_DELAY_MSEC = SECOND_MSEC * 2

@Field final Integer PREF_DEFAULT_WAKE_UP_INTERVAL_HR = 4
@Field final Integer DEFAULT_WAKE_UP_INTERVAL_SEC = PREF_DEFAULT_WAKE_UP_INTERVAL_HR * HOUR_SEC
@Field final Integer MIN_WAKE_UP_INTERVAL_SEC = 10 * MINUTE_SEC
@Field final Integer MAX_WAKE_UP_INTERVAL_SEC = 24 * HOUR_SEC

@Field final Integer ASSOC_CHECK_INTERVAL_MSEC = 24 * HOUR_MSEC // 24 hours
@Field final Short   ASSOCIATION_GROUP_ID = 1

@Field final Integer PREF_DEFAULT_MOTION_TIMEOUT_MIN = 3
@Field final Integer MIN_MOTION_TIMEOUT_MIN = 1
@Field final Integer MAX_MOTION_TIMEOUT_MIN = 255

@Field final Integer TEMP_SENSOR_FILTER_HISTORY_SIZE = 2

// tamper handling
@Field final String  PREF_DEFAULT_TAMPER_FALSE_ON_WAKE = true

@Field Map tamper_attr_map = [:]
tamper_attr_map.NAME = 'tamper'
tamper_attr_map.TRUE = 'detected'
tamper_attr_map.FALSE = 'clear'
tamper_attr_map.FALSE_INIT = 'device initialization'
tamper_attr_map.FALSE_AUTO = 'Automatically'
tamper_attr_map.FALSE_MANUAL = 'Manually'
@Field final Map TAMPER = tamper_attr_map

// binary sensor values and interpretations
@Field final Short ZWAVE_TRUE  = 0xFF
@Field final Short ZWAVE_FALSE = 0x00
@Field final Map   ZWAVE = [ZWAVE_TRUE: true, ZWAVE_FALSE: false].withDefault {false}

@Field Map motion_attr_map = [:]
motion_attr_map.NAME = 'motion'
motion_attr_map.LABEL = motion_attr_map.NAME
motion_attr_map.TRUE = 'active'
motion_attr_map.FALSE = 'inactive'
motion_attr_map.STATEMAP = [(true): motion_attr_map.TRUE, (false): motion_attr_map.FALSE].withDefault {motion_attr_map.FALSE}
@Field final Map MOTION = motion_attr_map

@Field Map temperature_attr_map = [:]
temperature_attr_map.NAME = 'temperature'
temperature_attr_map.LABEL = temperature_attr_map.NAME
@Field final Map TEMPERATURE = temperature_attr_map

@Field Map battery_attr_map = [:]
battery_attr_map.NAME = 'battery'
battery_attr_map.LABEL = battery_attr_map.NAME
@Field final Map BATTERY = battery_attr_map

@Field final Map    MAIN_ATTR = MOTION

@Field final Map CMD_CLASS_VERSIONS = [0x20: 1, 0x30: 1, 0x31: 4, 0x71: 2, 0x72: 1, 0x80: 1, 0x84: 2, 0x85: 2, 0x86: 1]

// REGION BEGIN - ORIGIN colors_snip.groovy main region
// tile colors
@Field final String COLOR_LTGRAY      = '#C1C1C1'
@Field final String COLOR_DKGRAY      = '#9E9E9E'
@Field final String COLOR_DKRED       = '#C92424'
@Field final String COLOR_RED         = '#FF0033'
@Field final String COLOR_ORANGE      = '#FFA81E'
@Field final String COLOR_YELLOW      = '#FFFF00'
@Field final String COLOR_PALE_YELLOW = '#FFFF92'
@Field final String COLOR_GREEN       = '#44b621'
@Field final String COLOR_CYAN        = '#1EE3FF'
@Field final String COLOR_DKBLUE      = '#153591'
@Field final String COLOR_WHITE       = '#FFFFFF'
@Field final String COLOR_BLACK       = '#000000'

// battery colors
@Field final String COLOR_BATT_FULL  = COLOR_GREEN
@Field final Integer PCT_BATT_FULL = 100
@Field final String COLOR_BATT_GOOD  = COLOR_GREEN
@Field final Integer PCT_BATT_GOOD = 90
@Field final String COLOR_BATT_OK   = COLOR_YELLOW
@Field final Integer PCT_BATT_OK = 60
@Field final String COLOR_BATT_LOW   = COLOR_RED
@Field final Integer PCT_BATT_LOW = 20
@Field final String COLOR_BATT_CRIT  = COLOR_RED
@Field final Integer PCT_BATT_CRIT = 5
// REGION END - ORIGIN colors_snip.groovy main region

// REGION BEGIN - ORIGIN temperature_colors_snip.groovy main region
// temperature colors
@Field final String COLOR_TMP_COLDER = '#153591'
@Field final String COLOR_TMP_COLD   = '#1E9CBB'
@Field final String COLOR_TMP_COOL   = '#90D2A7'
@Field final String COLOR_TMP_ROOM   =  COLOR_GREEN
@Field final String COLOR_TMP_WARM   = '#F1D801'
@Field final String COLOR_TMP_HOT    = '#D04E00'
@Field final String COLOR_TMP_HOTTER = '#BC2323'
// REGION END - ORIGIN temperature_colors_snip.groovy main region

// smartlog scopes
@Field final String ZWEH = 'Z-WaveEventHandler' // For handlers of events sent by the device itself
@Field final String DTI = 'DeviceTypeInternal' // for commands that are automatically called in a device type's lifecycle
@Field final String CCMD = 'STDeviceCommand' // capability or standalone command
@Field final String CCC = 'CommandClassCommand' // wraps a single command class

@Field def smartlog
@Field Long wakeUpPeriod

@Field final Boolean DEBUG_MODE = false


preferences
{
	input(name: "motionTimeout", type: "enum", title: "Motion timeout minutes",
		options: ['1','2','3','4','5'],
		defaultValue: PREF_DEFAULT_MOTION_TIMEOUT_MIN, required: true,
		description: "$PREF_DEFAULT_MOTION_TIMEOUT_MIN")

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

metadata {
	definition (name: "Monoprice NO-LABEL Motion Sensor IDE Integration Test", namespace: "surfous", author: "surfous")
	{
		capability "Battery"
		capability "Motion Sensor"
		capability "Temperature Measurement"
		capability "Sensor"

		attribute TAMPER.NAME, "enum", [TAMPER.FALSE, TAMPER.TRUE]
		attribute 'lastUpdated', 'number'
		attribute "blank", "string" // just as the name implies...

		command 'cleanState'
		command 'overrideWakeupInterval', ['number']
		command clearTamperManually

		fingerprint deviceId:"0x2001", inClusters:"0x71, 0x85, 0x80, 0x72, 0x30, 0x86, 0x31, 0x70, 0x84"
	}

	simulator
	{
		status "wake up" : "command: 8407, payload: "

		status 'active': zwave.alarmV1.alarmReport(alarmType:7, alarmLevel:0xff).incomingMessage()
		status 'inactive': new physicalgraph.zwave.Zwave().alarmV1.alarmReport(alarmType:7, alarmLevel:0x00).incomingMessage()
		status 'tamper': new physicalgraph.zwave.Zwave().alarmV1.alarmReport(alarmType:1, alarmLevel:11).incomingMessage()

		status 'batt_ok': new physicalgraph.zwave.Zwave().batteryV1.batteryReport(batteryLevel: 85).incomingMessage()
		status 'batt_low': new physicalgraph.zwave.Zwave().batteryV1.batteryReport(batteryLevel: 12).incomingMessage()
		status 'batt_crit': new physicalgraph.zwave.Zwave().batteryV1.batteryReport(batteryLevel: 0).incomingMessage()
		for (int i = 0; i <= 100; i += 20)
		{
			status "$TEMPERATURE.NAME ${i}F": new physicalgraph.zwave.Zwave().sensorMultilevelV2.sensorMultilevelReport(
				scaledSensorValue: i, precision: 1, sensorType: 1, scale: 1).incomingMessage()
		}

		reply '8002': 'command: 8003, payload: 55 00'
	}

	tiles(scale: 2)
	{
		multiAttributeTile(name:"multi${MAIN_ATTR.NAME}", type:"generic", width:6, height:4) {
			tileAttribute("device.${MOTION.NAME}", key: "PRIMARY_CONTROL") {
				attributeState(MOTION.FALSE, label:"no $MOTION.LABEL", icon:"st.motion.motion.inactive",
					backgroundColor: COLOR_WHITE, defaultState: true)
				attributeState(MOTION.TRUE, label:MOTION.LABEL, icon:"st.motion.motion.active",
					backgroundColor: COLOR_CYAN)
			}
			// tileAttribute("device.${TEMPERATURE.NAME}", key: "SECONDARY_CONTROL") {
			// 	attributeState(TEMPERATURE.NAME, label:TEMPERATURE.LABEL + ' ${currentValue}° F', unit: 'F', icon:"st.alarm.temperature.normal")
			// }
			tileAttribute("device.${BATTERY.NAME}", key: "SECONDARY_CONTROL") {
				attributeState(BATTERY.NAME, label:BATTERY.LABEL + ' ${currentValue}%', unit: '%')
			}
		}

		standardTile(MOTION.NAME, "device.motion", width: 2, height: 2)
		{
			state(MOTION.FALSE, label:"no $MOTION.LABEL", icon:"st.motion.motion.inactive",
				backgroundColor: COLOR_WHITE, defaultState: true)
			state(MOTION.TRUE, label:MOTION.LABEL, icon:"st.motion.motion.active",
				backgroundColor: COLOR_CYAN)
		}

		// XXX: Add a setting for the desired temperature unit (C or F).
		//	  How to change the valueTile's background color scale based
		//	  on the set unit?
		standardTile('clean', 'device.dummy', decoration: 'flat')
		{
			state 'clean', label: 'clean state', icon: 'st.Appliances.appliances13', action: 'cleanState'
		}

		valueTile(TEMPERATURE.NAME, "device.${TEMPERATURE.NAME}", width: 2, height: 2)
		{
			state(TEMPERATURE.NAME, label:'${currentValue}° F', unit: 'F',
				backgroundColors:[
					[value: 31, color: COLOR_TMP_COLDER],
					[value: 44, color: COLOR_TMP_COLD],
					[value: 59, color: COLOR_TMP_COOL],
					[value: 74, color: COLOR_TMP_ROOM],
					[value: 84, color: COLOR_TMP_WARM],
					[value: 95, color: COLOR_TMP_HOT],
					[value: 96, color: COLOR_TMP_HOTTER]
				]
			)
		}

		standardTile(TAMPER.NAME, "device.${TAMPER.NAME}", width: 2, height: 2)
		{
			state(TAMPER.FALSE, label:"device ok", icon:'st.security.alarm.clear',
				backgroundColor:COLOR_WHITE, defaultState: true)
			state(TAMPER.TRUE, label:"> tamper <", icon:'st.security.alarm.alarm',
				backgroundColor:COLOR_RED, action: 'clearTamperManually')
		}

		valueTile(BATTERY.NAME, "device.${BATTERY.NAME}", width: 2, height: 2)
		{
			state(BATTERY.NAME, label:BATTERY.LABEL + ' ${currentValue}%',
			backgroundColors:[
				[value: 100, color: COLOR_BATT_FULL], // green
				[value: 60,  color: COLOR_BATT_GOOD], // green
				[value: 30,  color: COLOR_BATT_OK], // yellow
				[value: 1,   color: COLOR_BATT_CRIT], // red
			])
		}

		standardTile('blankTile', 'device.blank', inactiveLabel: false, decoration: 'flat', width: 2, height: 2 )
		{
			state 'blank', label: '', action: '', icon: '', defaultState: true
		}

		main(["multi${MAIN_ATTR.NAME}"])
		details(["multi${MAIN_ATTR.NAME}", TEMPERATURE.NAME, TAMPER.NAME])
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
//  Device handler interface methods
//

/**
 * called every time preferences are updated. moves the settings into a more permanent form.
 */
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
	sendLoggedEvent([name: 'lastUpdated', value: now()])

	state.pref = [:]
	state.pref.motionTimeout = Integer.valueOf(settings?.motionTimeout?:PREF_DEFAULT_MOTION_TIMEOUT_MIN)
	state.pref.wakeupIntervalHrs = Integer.valueOf(settings?.wakeupIntervalHrs?:PREF_DEFAULT_WAKE_UP_INTERVAL_HR)
	state.pref.wakeupDevFlag = Boolean.valueOf(settings?.wakeupDevFlag?:false)
	state.pref.tamperClearAuto = Boolean.valueOf(settings?.tamperClearAuto?:PREF_DEFAULT_TAMPER_FALSE_ON_WAKE)

	smartlog.debug(DTI, "preferences recorded in state: ${state.pref}")
}

// REGION BEGIN - ORIGIN parse_command_snip.groovy main region
/**
 * called each time the device sends a message to the hub. parse then dispatches control to the
 * appropriate handler method
 * @param  rawZwaveEventDescription string holding the message from the device
 * @return                          commands and events, formatted & ready to be sent to the device
 */
def parse(String rawZwaveEventDescription)
{
	initDeviceEvent()
	String parseInstance = now() as String
	smartlog.fine(DTI, "parse $parseInstance: raw incoming event '${rawZwaveEventDescription}'")
	def result = []
	try
	{
		if (rawZwaveEventDescription == 'updated')
		{
			// do nothing - handles rogue invocation of parse with the arg set simply to 'updated'
			smartlog.warn(DTI, 'parse argument rawZwaveEventDescription was the plain string, "updated." Bypassing this erroneous command.')
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
				smartlog.fine(DTI, "parse: event handler returned a CommandQueue with ${deviceEventResult.length()} entries")
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
// REGION END - ORIGIN parse_command_snip.groovy main region

// -----
// Capability commands
//
def configure()
{
	initUserEvent()
	smartlog.fine(CCMD, "state contains: $state")
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

/**
 * Given a value in degrees Celsius, return the value as converted to degrees Fahrenheit
 * @param  degreesCelsius temperature measurement as an Integer in degrees Celsius
 * @return                temperature measurement as an Integer in degrees Fahrenheit
 */
Integer convertCelsiusToFahrenheit(Integer degreesCelsius)
{
	return (degreesCelsius * 9/5 + 32) as Integer
}

/**
 * The temperature reading reported often quickly bounces between two values,
 * adding a lot of noise in the activity smartlog. In order to filter out the
 * noise, a basic low pass filter is applied to the measurements
 * @return Integer
 */
Integer filterLowPass(String filterName, Integer historySize, Integer value)
{
	if (!state?.filterLP) state.filterLP = [:]
	if (!state.filterLP[filterName]) state.filterLP.put(filterName, [])
	state.filterLP[filterName] << value
	state.filterLP[filterName] = state.filterLP[filterName].drop(state.filterLP[filterName].size() - TEMP_SENSOR_FILTER_HISTORY_SIZE)
	Integer filteredValue = (state.filterLP[filterName].sum() / state.filterLP[filterName].size()) as Integer
	smartlog.debug("latest $filterName value: $value; filtered value: $filteredValue")
	return filteredValue
}

private String formatOctetAsHex(Short octet)
{
	return sprintf('%#x', octet)
}

private void clearTamper(String clearMethod)
{
	smartlog.fine "in clearTamper with arg '$clearMethod'"
	smartlog.trace "current tamper attribute value is ${device.currentValue('tamper')}"
	if (device.currentValue(TAMPER.NAME) != TAMPER.FALSE)
	{
		Map evtMap = [name: TAMPER.NAME, value: TAMPER.FALSE, description: "tamper alert cleared $clearMethod", isStateChange: true, displayed: true]
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

// REGION BEGIN - ORIGIN event_helpers_snip.groovy main region
/**
 * sendLoggedEvent - alternate to sendEvent which first logs the event map at the info level
 * @param eventMap map with the key-value pairs that represent an event, name & value requires
 */
private void sendLoggedEvent(Map eventMap)
{
	smartlog.info "sendLoggedEvent - sending immediate event: $eventMap"
	sendEvent(eventMap)
}

/**
 * sendExceptionEvent overload which calls sendExceptionEvent with a default prologue of 'caught exception'
 * @param t object which inherits from throwable (e.g., an exception)
 */
void sendExceptionEvent(Throwable t)
{
	sendExceptionEvent(t, 'caught exception')
}

/**
 * sendExceptionEvent  Logs and sends an event to record catching an exception
 * @param  t the causght excetion
 * @param  prologue  string to prefix the exception log entry
 */
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
		smartlog.error(msg)
		sendEvent(evtMap)
	}
}
// REGION END - ORIGIN event_helpers_snip.groovy main region

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

Map taskGetTemperature()
{
	smartlog.fine('wakeup tasks: in taskGetTemperature')
	def cq = CommandQueue()
	try
	{
		cq.add(ccSensorMultilevelGet())
	}
	catch (Throwable ex)
	{
		sendExceptionEvent(ex, 'exception caught in taskGetTemperature')
	}
	return cq
}

Map taskSetMotionTimeout()
{
	smartlog.trace('wakeup tasks: in taskSetMotionTimeout')
	def cq = CommandQueue()
	try
	{
		Integer motionTimeoutMinutesPref = state?.pref?.motionTimeout?:PREF_DEFAULT_MOTION_TIMEOUT_MIN
		if (motionTimeoutMinutesPref != state?.cfg?.motionTimeout)
		{
			cq.add(macroConfigureMotionTimeout(motionTimeoutMinutesPref))
		}
		else
		{
			smartlog.debug "Motion Timeout config matches preference at ${motionTimeoutMinutesPref}"
		}
	}
	catch (Throwable ex)
	{
		sendExceptionEvent(ex, 'exception caught in taskSetMotionTimeout')
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

def chainVersionCommandClassChecks()
{
	def cq = CommandQueue()
	if (state?.commandClassIds == null)
	{
		state.commandClassIds = []
		state.commandClassIds.addAll(CMD_CLASS_VERSIONS.keySet())
	}
	if (state.commandClassIds.size() > 0)
	{
		String ccId = state.commandClassIds.remove(0)
		cq.add ccVersionCommandClassGet(Short.valueOf(ccId))
	}
	return cq
}

Boolean isCommandClassVersionChainComplete()
{
	if (state?.commandClassIds != null && state.commandClassIds.size() == 0) return true
	return false
}

// -----
// CommandClass commands, event handlers and helper methods
//

// // CommandClass WakeUp
// REGION BEGIN - ORIGIN cc_wakeup_snip.groovy main region

// REGION END - ORIGIN cc_wakeup_snip.groovy main region


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
		smartlog.info "running metadata command chain for ${device.displayName}"
		cq.add(chainDeviceMetadata(true))
	}
	// else if (!isCommandClassVersionChainComplete())
	// {
	// 	smartlog.info "running command class version command chain for ${device.displayName}"
	// 	cq.add(chainVersionCommandClassChecks())
	// }
	else
	{
		smartlog.info "compiling standard WakeUp ritual for ${device.displayName}"
		taskCheckTamperState()
		cq.add(taskGetTemperature())
		cq.add(taskGetWakeupInterval())
		cq.add(taskSetMotionTimeout())
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
	cq.add(ccWakeUpIntervalSet(seconds), DEFAULT_INTERCMD_DELAY_MSEC * 2)
	cq.add(ccWakeUpIntervalGet())
	return cq
}


// // CommandClass Association v2
def ccAssociationSet()
{
	// 0x8501
	smartlog.trace(CCC, "setting ${device.displayName} association to group id $ASSOCIATION_GROUP_ID for nodeid $zwaveHubNodeId")
	return zwave.associationV1.associationSet(groupingIdentifier:ASSOCIATION_GROUP_ID, nodeId:zwaveHubNodeId)
}

def ccAssociationRemove()
{
	// 0x8504
	smartlog.fine(CCC, "removing ${device.displayName} association to group id $ASSOCIATION_GROUP_ID for nodeid $zwaveHubNodeId")
	state.associationNodeId = null // appropriate?
	return zwave.associationV1.associationRemove(groupingIdentifier:ASSOCIATION_GROUP_ID, nodeId:zwaveHubNodeId)
}

def ccAssociationGet()
{
	// 0x8502
	smartlog.fine(CCC, "getting ${device.displayName} association for group id $ASSOCIATION_GROUP_ID")
	return zwave.associationV1.associationGet(groupingIdentifier: ASSOCIATION_GROUP_ID)
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
	String sensorHexValue = formatOctetAsHex(deviceEvent.sensorValue)


	Map evtMap = [:]
	evtMap.name = MAIN_ATTR.NAME
	evtMap.description = deviceEvent as String

	if (deviceEvent.sensorValue == physicalgraph.zwave.commands.sensorbinaryv1.SensorBinaryReport.SENSOR_VALUE_DETECTED_AN_EVENT)
	{
		evtMap.value = MAIN_ATTR.TRUE
		evtMap.descriptionText = "${device.displayName}: $MAIN_ATTR.LABEL detected (via SensorBinary)"
	}
	else
	{
		evtMap.value = MAIN_ATTR.FALSE
		evtMap.descriptionText = "${device.displayName}: $MAIN_ATTR.LABEL has passed (via SensorBinary)"
	}
	smartlog.info evtMap.descriptionText
	evtMap.displayed = false // hide potentially duplicitive events
	cq.add(evtMap)
	return cq
}


// // CommandClass Basic
def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicSet deviceEvent)
{
	smartlog.fine(ZWEH, "handling BasicSet '$deviceEvent'")
	def cq = CommandQueue()
	String deviceAttrValue = MAIN_ATTR.FALSE
	String msg = "${device.displayName}: $MAIN_ATTR.LABEL has passed (via Basic)"
	if (deviceEvent.value)
	{
		deviceAttrValue = MAIN_ATTR.TRUE
		msg = msg.replaceAll('has passed', 'detected')
	}
	smartlog.info msg
	Map evtMap = [:]
	evtMap.name = MAIN_ATTR.NAME
	evtMap.description = deviceEvent as String
	evtMap.value = deviceAttrValue
	evtMap.descriptionText = msg
	evtMap.displayed = true
	cq.add evtMap
	return cq
}


// // Command Class SensorMultilevel
def ccSensorMultilevelGet()
{
	smartlog.trace('issuing SensorMultilevelGet')
	return zwave.sensorMultilevelV4.sensorMultilevelGet()
}

def zwaveEvent(physicalgraph.zwave.commands.sensormultilevelv4.SensorMultilevelReport deviceEvent)
{
	smartlog.trace("handling SensorMultilevelReport '$deviceEvent'")
	def cq = CommandQueue()
	Map evtMap = [description: deviceEvent as String]
	if (deviceEvent.sensorType == physicalgraph.zwave.commands.sensormultilevelv4.SensorMultilevelReport.SENSOR_TYPE_TEMPERATURE_VERSION_1)
	{
		evtMap.name = TEMPERATURE.NAME
		// If the sensor returns the temperature value in degrees centigrade,
		// convert to degrees Fahrenheit. Also, apply a basic low-pass filter
		// to the scaled sensor value input.
		def scaledSensorValue = (deviceEvent.scaledSensorValue instanceof List) ? deviceEvent.scaledSensorValue[0] : deviceEvent.scaledSensorValue
		//Integer filteredSensorValue = filterSensorValue(scaledSensorValue as Integer)
		Integer filteredSensorValue = filterLowPass(TEMPERATURE.NAME, TEMP_SENSOR_FILTER_HISTORY_SIZE, scaledSensorValue as Integer)
		if (deviceEvent.scale == 1)
		{
			evtMap.value = filteredSensorValue as String
			evtMap.unit = "F"
		}
		else
		{
			evtMap.value = convertCelsiusToFahrenheit(filteredSensorValue) as String
			evtMap.unit = "F"
		}
		String msg = "${device.displayName} reports temperature as $scaledSensorValue scale ${deviceEvent.scale}. Filtered value is $filteredSensorValue. Return temperature value is ${evtMap.value} ${evtMap.unit}."
		smartlog.info msg
		evtMap.descriptionText = msg
	}
	cq.add evtMap
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
	Map evtMap = [ name: BATTERY.NAME, value: batteryLevel, unit: "%" , displayed: true, description: deviceEvent as String, descriptionText: message]
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


// // Command Class Configuration
def macroConfigureMotionTimeout(Integer minutes)
{
	Short parameterNumber = 1
	def cq = CommandQueue()
	if (!minutes || minutes < 0) minutes = 1
	if (minutes > MAX_MOTION_TIMEOUT_MIN) minutes = MAX_MOTION_TIMEOUT_MIN
	Command setCmd = ccConfigurationSet(parameterNumber, [minutes], 1 as Short)
	Command getCmd = ccConfigurationGet(parameterNumber)
	cq.add([ name: 'command', value:'', description: setCmd.format(), descriptionText: "set config value for motion timeout config parameter ($parameterNumber) of $minutes min."])
	cq.add(setCmd, 2000)
	cq.add([ name: 'command', value:'', description: getCmd.format(), descriptionText: "getting value of motion timeout config parameter ($parameterNumber)."])
	cq.add getCmd
	return cq
}

def ccConfigurationSet(Short parameterNumber, List configurationValue, Short size)
{
	smartlog.trace("issuing ConfigurationSet for parameter $parameterNumber to value $configurationValue")
	return zwave.configurationV1.configurationSet(
		parameterNumber: parameterNumber,
		configurationValue: configurationValue,
		size: size)
}

def ccConfigurationGet(Integer parameterNumber)
{
	smartlog.trace("issuing ConfigurationGet for parameter ${parameterNumber}")
	return zwave.configurationV1.configurationGet(parameterNumber: parameterNumber)
}

def zwaveEvent(physicalgraph.zwave.commands.configurationv1.ConfigurationReport deviceEvent)
{
	smartlog.trace("handling ConfigurationReport '$deviceEvent'")
	def cq = CommandQueue()
	Integer parameterNumber = deviceEvent.parameterNumber
	Integer configurationValue = deviceEvent.configurationValue[0]
	Map evtMap = [name: "config${parameterNumber}", value: configurationValue, description: deviceEvent as String]
	if (state?.cfg == null) state.cfg = [:]
	if (parameterNumber == 1) // motionTimeout
	{
		state.cfg.motionTimeout = configurationValue
		evtMap.name = 'motionTimeout'
		evtMap.descriptionText = "Motion timeout is $configurationValue minutes (config parameter $parameterNumber)"
		evtMap.unit = 'minutes'
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
	cq.add(chainVersionCommandClassChecks())
	return cq
}

// // CommandClass Manufacturer Specific v1

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


// // Command Class Alarm

def zwaveEvent(physicalgraph.zwave.commands.alarmv2.AlarmReport deviceEvent)
{
	smartlog.fine(ZWEH, "handling AlarmReport '$deviceEvent'")
	def cq = CommandQueue()
	String alarmMsg = "${device.displayName}: Alarm Type ${deviceEvent.zwaveAlarmType}; Event ${deviceEvent.zwaveAlarmEvent}; Status ${deviceEvent.zwaveAlarmStatus};   (V1 info Type:${deviceEvent.alarmType}; Level:${deviceEvent.alarmLevel})"

	Map evtMap = [name: "alarm${deviceEvent.zwaveAlarmType}", value: "v2event ${deviceEvent.zwaveAlarmEvent}, v2status ${deviceEvent.zwaveAlarmStatus}, v1level ${deviceEvent.alarmLevel}", description: deviceEvent as String]
	smartlog.debug alarmMsg
	switch (deviceEvent)
	{
		case {it.zwaveAlarmType == 7 && it.zwaveAlarmEvent == 2 && it.alarmLevel == ZWAVE_TRUE}:
			// this is the alarm for motion detected
			evtMap.name = MAIN_ATTR.NAME
			evtMap.value = MAIN_ATTR.TRUE
			alarmMsg = "${device.displayName}: $MAIN_ATTR.LABEL detected (via Alarm)"
			break
		case {it.zwaveAlarmType == 7 && it.zwaveAlarmEvent == 2 && it.alarmLevel == ZWAVE_FALSE}:
			// this is the alarm for motion ceased
			evtMap.name = MAIN_ATTR.NAME
			evtMap.value = MAIN_ATTR.FALSE
			alarmMsg = "${device.displayName}: $MAIN_ATTR.LABEL has ceased (via Alarm)"
			break
		case {it.zwaveAlarmType == 7 && it.zwaveAlarmEvent == 3}:
			// this is the alarm for tamper - There seems to be no cleared alarm for this
			evtMap.name = TAMPER.NAME
			evtMap.value = TAMPER.TRUE
			evtMap.displayed = true
			alarmMsg = "${device.displayName}: sensor cover has been opened"
			break
		default:
			evtMap.displayed = true
	}
	evtMap.descriptionText = alarmMsg
	smartlog.info alarmMsg
	cq.add evtMap
	return cq
}


// // Catch-all handler.for any unsupported zwave events the device might send at us
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

// REGION BEGIN - ORIGIN SmartLog_cc.groovy main region
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
		state.smartlog = null
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
// REGION END - ORIGIN SmartLog_cc.groovy main region

// REGION BEGIN - ORIGIN CommandQueue_cc.groovy main region
/**
 * Low-rent "class" that is a queue for z-wave commands. Commands may be added one at a time,
 * or as a list. Default delays are automatically added between commands unless a specific
 * duration delay is specified after a command. Default delays are not added after another delay
 * command or if the command itself is a delay command. The list of commands may be extracted at
 * any time.
 */

def CommandQueue(Integer defaultDelayMsec=null)
{
	def cq = [:] // the "object" map
	cq.type = 'CommandQueue'
	cq.version = '20150623r1'

	cq.defaultDelayMsec = defaultDelayMsec?:400 // define a backup default intercommand delay
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
				"the default delay of $cq.defaultDelayMsec ms"
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
			cq.smartlog.warn("entry parameter to add() was not a Command, List, CommandQueue, ResponseQueue, Event or delay. discarding. ($entryStr)")
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
				cq.__addDelay(cq.formatDelayCmd(cq.defaultDelayMsec)) // always the default delay
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
			return "delay $cq.defaultDelayMsec"
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
// REGION END - ORIGIN CommandQueue_cc.groovy main region