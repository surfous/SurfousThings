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
 *  Author: surfous
 *  Date: {{BUILDDATE}}
 *  Build: {{BUILDTAG}}
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

// ADDIN TARGET colors_snip

// ADDIN TARGET temperature_colors_snip

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
	definition (name: "Monoprice NO-LABEL Motion Sensor", namespace: "surfous", author: "surfous")
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

// ADDIN TARGET parse_command_snip

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

// ADDIN TARGET event-helpers_snip

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
// ADDIN TARGET cc_wakeup_snip


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

// ADDIN TARGET SmartLog_cc

// ADDIN TARGET CommandQueue_cc
