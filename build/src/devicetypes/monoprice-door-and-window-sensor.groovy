/**
 *  Monoprice NO-LABEL (Vision OEM) Door & Window Sensor
 *
 *  Capabilities: Contact Sensor, Battery Indicator
 *
 *  Notes:
 *  *	After pairing, set device preferences in the app, open the sensor for at least five
 *  	seconds then close to wake it and complete device type recognition and initialization.
 *  *	The Monoprice Garage Door and Shock sensors have identical device fingerprints.
 *  	If a device of that type pairs with this handler on inclusion, change it to use the
 *  	"Monoprice NO-LABEL Door & Window Sensor" handler from the "My Devices" tab.
 *
 *  Raw Description: 0 0 0x2001 0 0 0 7 0x71 0x85 0x80 0x72 0x30 0x86 0x84
 *  Raw Description: zw:S type:2001 mfr:0109 prod:2001 model:0102 ver:4.84 zwv:3.52 lib:06 cc:71,85,80,72,30,86,84
 *
 * Door & Window Sensor:
 *  VersionReport: application version: 4, Z-Wave firmware version: 84, Z-Wave lib type: 6, Z-Wave version: 3.52
 *  MSR: 0109-2001-0102
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
tamper_attr_map.NAME = 'tamper'
tamper_attr_map.TRUE = 'detected'
tamper_attr_map.FALSE = 'clear'
tamper_attr_map.FALSE_INIT = 'device initialization'
tamper_attr_map.FALSE_AUTO = 'Automatically'
tamper_attr_map.FALSE_MANUAL = 'Manually'
@Field final Map	TAMPER = tamper_attr_map

// binary sensor values
@Field final Short ZWAVE_TRUE  = 0xFF
@Field final Short ZWAVE_FALSE = 0x00
@Field final Map ZWAVE = [ZWAVE_TRUE: true, ZWAVE_FALSE: false].withDefault {false}

@Field Map contact_attr_map = [:]
contact_attr_map.SENSOR_TYPE = 'contact'
contact_attr_map.SENSOR_LABEL = 'contact'
contact_attr_map.PRODUCT_TYPE_ID = 0x2001
contact_attr_map.NAME = 'acceleration'
contact_attr_map.LABEL = 'contact'
contact_attr_map.TRUE = 'open'
contact_attr_map.FALSE = 'closed'
contact_attr_map.STATEMAP = [(true): contact_attr_map.TRUE, (false): contact_attr_map.FALSE].withDefault {contact_attr_map.FALSE}
@Field final Map	CONTACT = contact_attr_map

@Field Map battery_attr_map = [:]
battery_attr_map.NAME = 'battery'
battery_attr_map.LABEL = battery_attr_map.NAME
@Field final Map BATTERY = battery_attr_map

@Field Map unknown_attr_map = [:]
unknown_attr_map.UNKNOWN = 'unknown'
unknown_attr_map.SENSOR_TYPE = unknown_attr_map.UNKNOWN
unknown_attr_map.SENSOR_LABEL = unknown_attr_map.UNKNOWN
unknown_attr_map.PRODUCT_TYPE_ID = 0x0
unknown_attr_map.NAME = unknown_attr_map.UNKNOWN
unknown_attr_map.LABEL = unknown_attr_map.UNKNOWN
unknown_attr_map.TRUE = unknown_attr_map.UNKNOWN
unknown_attr_map.FALSE = unknown_attr_map.UNKNOWN
unknown_attr_map.STATEMAP = [:].withDefault {unknown_attr_map.UNKNOWN}
@Field final Map	UNKNOWN = unknown_attr_map

// TODO - find a way to set this based upon SENSOR_TYPE
@Field Map		    MAIN_ATTR = null

// ADDIN TARGET monoprice_snip disambiguation

@Field final Map CMD_CLASS_VERSIONS = [0x20: 1, 0x30: 1, 0x71: 2, 0x72: 1, 0x80: 1, 0x84: 2, 0x85: 2, 0x86: 1]

// ADDIN TARGET colors_snip

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
	definition (name: 'Monoprice Door and Window Sensor', namespace: 'surfous', author: 'surfous')
	{
		capability 'Battery'
		capability 'Contact Sensor'
		capability 'Sensor'

		attribute 'sensorType', 'enum', [UNKNOWN_SENSOR_TYPE, 'contact']
		attribute 'tamper', 'enum', ['open', 'closed']
		attribute 'lastUpdated', 'number'
		attribute 'lastSensorReport', 'number'
		attribute 'msr', 'string'
		attribute 'blank', 'string' // just as the name implies...

		command clearTamperManually

		fingerprint mfr: '0109',  prod: '2001',  model: '0102',  cc: '71,85,80,72,30,86,84'
		// old-style fingerprint for V1 hub backwards compatibility
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
		standardTile(CONTACT.NAME, "device.${CONTACT.NAME}", width: 2, height: 2)
		{
			state("${CONTACT.FALSE}", label:"$CONTACT.FALSE", icon:'st.contact.contact.closed',
				backgroundColor:COLOR_GREEN)
			state("${CONTACT.TRUE}",   label:"$CONTACT.TRUE", icon:'st.contact.contact.open',
				backgroundColor:COLOR_YELLOW)
			state(UNKNOWN.SENSOR_TYPE, label: UNKNOWN.SENSOR_TYPE, icon:'st.unknown.zwave.sensor',
				backgroundColor: COLOR_BLACK)
		}

		standardTile(TAMPER.NAME, "device.${TAMPER.NAME}")
		{
			state(TAMPER.FALSE, label:"device ok", icon:'st.security.alarm.clear',
				backgroundColor:COLOR_WHITE, defaultState: true)
			state(TAMPER.TRUE, label:"> $TAMPER.LABEL <", icon:'st.security.alarm.alarm',
				backgroundColor:COLOR_RED, action: 'clearTamperManually')
		}

		standardTile("config", "device.configuration", decoration: "flat")
		{
			state("configure", action: "configuration.configure", icon:"st.secondary.configure")
		}

		valueTile(BATTERY.NAME, "device.${BATTERY.NAME}")
		{
			state(BATTERY.NAME, label:"$BATTERY.LABEL ${currentValue}%",
			backgroundColors:[
				[value: 100, color: COLOR_GREEN], // green
				[value: 60,  color: COLOR_GREEN], // green
				[value: 30,  color: COLOR_YELLOW], // yellow
				[value: 1,   color: COLOR_RED], // red
			])
		}

		main([CONTACT.NAME])
		details([CONTACT.NAME, BATTERY.NAME, TAMPER.NAME])
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
		case CONTACT.SENSOR_TYPE:
			MAIN_ATTR = CONTACT
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

	initpref = [:]
	initpref.wakeupIntervalHrs = Integer.valueOf(settings?.wakeupIntervalHrs?:PREF_DEFAULT_WAKE_UP_INTERVAL_HR)
	initpref.wakeupDevFlag = Boolean.valueOf(settings?.wakeupDevFlag?:false)
	initpref.tamperClearAuto = Boolean.valueOf(settings?.tamperClearAuto?:PREF_DEFAULT_TAMPER_FALSE_ON_WAKE)
	state.pref = initpref

	smartlog.debug(DTI, "preferences recorded in state: ${state.pref}")
}

// ADDIN TARGET parse_command_snip


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

// ADDIN TARGET event_helpers_snip

private String formatNumberAsHex(Number num)
{
	if (num != null) return sprintf('%#x', num)
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
		else if (getAttributeSensorType() != determineSensorType())
		{
			smartlog.debug "device metadata requires sensorType attribute be equal to the determined sensor type"
			macroInitializeSensorType()
			chainDeviceMetadata()
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
		state.deviceMeta?.wakeup &&
		getAttributeSensorType() == determineSensorType() )
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
	String sensorType = UNKNOWN.SENSOR_TYPE
	String sensorLabel = UNKNOWN.SENSOR_TYPE
	if (state?.deviceMeta?.msr?.productTypeId != null)
	{
		def productTypeId = state?.deviceMeta?.msr?.productTypeId
		smartlog.debug "product type id: $productTypeId"
		String productTypeIdString = formatNumberAsHex(productTypeId)
		smartlog.debug "product type id as hex str: $productTypeIdString"
		sensorLabel = DEVICE_PRODUCT_ID_DISAMBIGUATION[productTypeIdString]
		String deviceHandlerProductTypeIdString = formatNumberAsHex(CONTACT.PRODUCT_TYPE_ID)
		smartlog.debug "device handler product type id: $deviceHandlerProductTypeIdString"
		switch(productTypeIdString)
		{
			case deviceHandlerProductTypeIdString:
				sensorType = CONTACT.SENSOR_TYPE
				break
			default:
				sensorType = UNKNOWN.SENSOR_TYPE
				break
		}
	}

	smartlog.debug "determineSensorType has determined sensor to be of type '$sensorLabel' ($sensorType)"
	if (sensorType == UNKNOWN.SENSOR_TYPE && sensorLabel != UNKNOWN.SENSOR_LABEL)
	{
		smartlog.warn "Please set the device handler for this device to one written for a $CONTACT.SENSOR_LABEL in the IDE"
	}
	else
	{
		smartlog.warn "Please find a more appropriate device handler for this device and set it in the IDE"
	}
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
	smartlog.fine 'in getAttributeSensorType'
	String sensorType = device.currentValue('sensorType')
	String logMsg = "device attr 'sensorType' is $sensorType."
	if (sensorType == null)
	{
		sensorType = UNKNOWN.SENSOR_TYPE
		logMsg += " returning null sensorType as $sensorType."
	}
	smartlog.debug logMsg
	return sensorType
}

Boolean macroInitializeSensorType()
{
	smartlog.fine "in macroInitializeSensorType()"
	setAttributeSensorType()
	configureMainAttr()
	if (MAIN_ATTR && MAIN_ATTR.NAME != UNKNOWN.UNKNOWN)
	{
		Map initSensorEvent = [name: MAIN_ATTR.NAME, value: MAIN_ATTR.FALSE,
			isStateChange: true, displayed: true]
		sendLoggedEvent(initSensorEvent)
		return true
	}
	return false
}

def handleSensorReport(Boolean sensorValue, physicalgraph.zwave.Command deviceEvent)
{
	smartlog.fine "in handleSensorReport with value '$sensorValue' and zwave cmd '$deviceEvent'"
	configureMainAttr()

	def currentSensorAttrValue = device.currentValue(MAIN_ATTR.NAME)
	def newSensorAttrValue = MAIN_ATTR.STATEMAP[sensorValue]

	// bail if we've handled an identical sensor report in the past 2 seconds
	if (filterRepeats('lastSensorReport') && currentSensorAttrValue == newSensorAttrValue)
	{
		smartlog.trace "discarding duplicate report within 2 seconds: $deviceEvent"
		return null
	}

	def cq = CommandQueue()
	//String sensorType = getAttributeSensorType()
	String commandName = getZwaveCommandName(deviceEvent)

	Map sensorEvent = [:]
	if (MAIN_ATTR.SENSOR_TYPE != UNKNOWN.SENSOR_TYPE)
	{
		sensorEvent.name = MAIN_ATTR.NAME
		sensorEvent.value = newSensorAttrValue
		sensorEvent.description = deviceEvent as String
		sensorEvent.descriptionText = "${device.displayName}: $MAIN_ATTR.LABEL is $sensorEvent.value (via $commandName)"

		if (currentSensorAttrValue != sensorEvent.value)
		{
			smartlog.debug "device attribute $sensorEvent.name has changed from '$currentSensorAttrValue' to '$newSensorAttrValue'"
			sensorEvent.isStateChange = true
			sensorEvent.displayed = true
		}
		cq.add sensorEvent
	}
	return cq
}


// -----
// CommandClass commands, event handlers and helper methods
//

// ADDIN TARGET cc_wakeup_snip v2

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
	def lowValue = 15
	def nearDeadValue = 5
	smartlog.fine(ZWEH, "handling BatteryReport '$deviceEvent'")
	def cq = CommandQueue()
	Integer batteryLevel = deviceEvent.batteryLevel
	state.batteryLastUpdateTime = now()
	String batteryMessage = "battery level is reported as $batteryLevel"
	Map evtMap = [ name: "battery", value: batteryLevel, unit: "%" , displayed: true, description: deviceEvent as String, descriptionText: message]
	log.info batteryMessage
	if (batteryLevel == 0xFF)
	{
		evtMap.value = 1
		evtMap.descriptionText = "${device.displayName}: battery is almost dead!"
	}
	else if ( batteryLevel <= nearDeadValue )
	{
		evtMap.value = 1
		evtMap.descriptionText = "${device.displayName}: battery is almost dead!"
	}
	else if (batteryLevel < lowValue )
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


// ADDIN TARGET cc_manufacturer_specific_snip v1


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
			// this is the alarm for the sensor detecting something
			cq.add handleSensorReport(true, deviceEvent)
			break
		case {it.zwaveAlarmType == 7 && it.zwaveAlarmEvent == 2 && it.alarmLevel == ZWAVE_FALSE}:
			// this is the alarm for no longer detecting something
			cq.add handleSensorReport(false, deviceEvent)
			break
		case {it.zwaveAlarmType == 7 && it.zwaveAlarmEvent == 3}:
			// this is the alarm for tamper - There seems to be no cleared alarm for this
			Map tamperEvt = [name: TAMPER.NAME, value: TAMPER.TRUE]
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
	smartlog.fine(CCC, 'issuing AlarmTypeSupportedGet')
	return zwave.alarmV2.alarmTypeSupportedGet()
}

def zwaveEvent(physicalgraph.zwave.commands.alarmv2.AlarmTypeSupportedReport deviceEvent)
{
	smartlog.fine(ZWEH, "handling AlarmTypeSupportedReport '$deviceEvent'")
	smartlog.info("AlarmTypeSupportedReport: '$deviceEvent'")
}


// ADDIN TARGET cc_generic_command_snip


/*
	ClosureClasses down here.
 */

// ADDIN TARGET SmartLog_cc

// ADDIN TARGET CommandQueue_cc
