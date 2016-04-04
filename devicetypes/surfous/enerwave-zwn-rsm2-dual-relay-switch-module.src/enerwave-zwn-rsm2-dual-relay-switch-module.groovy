/* ZWN-RSM2 Enerwave Dual Load ZWN-RSM2
 *
 * Based on Matt Frank's handler for this device
 * which was based on the work of chrisb for AEON Power Strip.
 * Made solid by surfous.
 *
 *  Date: 2016-04-03
 *  Build: 20160404-042609.79664
 *
 *  URL: https://graph.api.smartthings.com/ide/device/editor/f8df0662-222f-4737-b429-baec0c8109aa
 */
import groovy.transform.Field

import physicalgraph.device.HubAction
import physicalgraph.zwave.Command

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

// smartlog scopes
@Field final String ZWEH = 'Z-WaveEventHandler' // For handlers of events sent by the device itself
@Field final String DTI = 'DeviceTypeInternal' // for commands that are automatically called in a device type's lifecycle
@Field final String CCMD = 'STDeviceCommand' // capability or standalone command
@Field final String CCC = 'CommandClassCommand' // wraps a single command class

@Field final List ENDPOINTS = [1,2]

@Field final Short ZWAVE_OFF = 0x00
@Field final Short ZWAVE_ON  = 0xFF

@Field final Map CMD_CLASS_VERSIONS = [0x60:3, 0x25:1, 0x70:1, 0x72:1]
@Field def smartlog

@Field final Boolean DEBUG_MODE = true

// for the UI
metadata
{
	definition (name: "Enerwave ZWN-RSM2 Dual Relay Switch Module", namespace: "surfous", author: "Kevin Shuk") {
		capability 'Actuator'
		capability 'Switch'
		capability 'Configuration'
		capability 'Polling'
		capability 'Refresh'

		attribute 'dualRelay',  'enum', ['off-off', 'on-off', 'on-on', 'off-on']
		attribute 'switch1', 'enum', ['on', 'off']
		attribute 'switch2', 'enum', ['on', 'off']

		command 'switch1on'
		command 'switch1off'
		command 'switch2on'
		command 'switch2off'

		command 'allOn'
		command 'allOff'

		command 'cycleSwitch'
		command 'test'

		fingerprint deviceId: '0x1001', inClusters:'0x25, 0x27, 0x60, 0x70, 0x72, 0x86'
	}
}

simulator
{
	// TODO: define status and reply messages here
}

tiles(scale: 2)
{
	multiAttributeTile(name: 'richDualRelay', type:'generic', width: 6, height: 4)
	{
		tileAttribute('device.dualRelay', key: 'PRIMARY_CONTROL')
		{
			attributeState 'off-off', action: 'cycleSwitch',  icon: 'https://sites.google.com/a/surfous.com/external-assets/st/dual_switch_off_off.png', backgroundColor: COLOR_GRAY, nextState: 'on-off', defaultState: true
			attributeState 'on-off',  action: 'cycleSwitch', icon: 'https://sites.google.com/a/surfous.com/external-assets/st/dual_switch_on_off.png', backgroundColor: COLOR_PALE_YELLOW, nextState: 'on-on'
			attributeState 'on-on',   action: 'cycleSwitch', icon: 'https://sites.google.com/a/surfous.com/external-assets/st/dual_switch_on_on.png', backgroundColor: COLOR_YELLOW, nextState: 'off-on'
			attributeState 'off-on',  action: 'cycleSwitch', icon: 'https://sites.google.com/a/surfous.com/external-assets/st/dual_switch_off_on.png', backgroundColor: COLOR_PALE_YELLOW, nextState: 'off-off'
		}
	}

	standardTile("dualRelay", "device.dualRelay", width: 2, height: 2, canChangeIcon: false)
	{
		state 'off-off', action: 'cycleSwitch',  icon: 'https://sites.google.com/a/surfous.com/external-assets/st/dual_switch_off_off.png', backgroundColor: COLOR_GRAY, nextState: 'on-off', defaultState: true
		state 'on-off',  action: 'cycleSwitch', icon: 'https://sites.google.com/a/surfous.com/external-assets/st/dual_switch_on_off.png', backgroundColor: COLOR_PALE_YELLOW, nextState: 'on-on'
		state 'on-on',   action: 'cycleSwitch', icon: 'https://sites.google.com/a/surfous.com/external-assets/st/dual_switch_on_on.png', backgroundColor: COLOR_YELLOW, nextState: 'off-on'
		state 'off-on',  action: 'cycleSwitch', icon: 'https://sites.google.com/a/surfous.com/external-assets/st/dual_switch_off_on.png', backgroundColor: COLOR_PALE_YELLOW, nextState: 'off-off'
	}

	standardTile("switch1", "device.switch1", width: 2, height: 2, canChangeIcon: false)
	{
		state "off", label: "load 1 off", action: "switch1on",  icon: "https://sites.google.com/a/surfous.com/external-assets/st/single_switch_off.png",  backgroundColor: COLOR_LTGRAY, defaultState: true
		state "on",  label: "load 1 on",  action: "switch1off", icon: "https://sites.google.com/a/surfous.com/external-assets/st/single_switch_on.png", backgroundColor: COLOR_WHITE
	}

	standardTile("switch2", "device.switch2", width: 2, height: 2, canChangeIcon: false)
	{
		state "off", label: "load 2 off", action: "switch2on",  icon: "https://sites.google.com/a/surfous.com/external-assets/st/single_switch_off.png",  backgroundColor: COLOR_LTGRAY, defaultState: true
		state "on",  label: "load 2 on",  action: "switch2off", icon: "https://sites.google.com/a/surfous.com/external-assets/st/single_switch_on.png", backgroundColor: COLOR_WHITE
	}

	standardTile("refresh", "device.switch", width: 2, height: 2, inactiveLabel: false, decoration: "flat")
	{
		state "refresh", action:"refresh.refresh", icon:"st.secondary.refresh", defaultState: true
	}

	standardTile('test', 'device.switch', width: 2, height: 2, inactiveLabel: false, decoration: 'flat')
	{
		state 'test', label:'Test', action:'runAdHocTest', icon:'st.secondary.test', defaultState: true
	}

	main(['dualRelay'])
	details(['richDualRelay', 'switch1', 'switch2', 'refresh'])
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

List getStates()
{
	return ['off-off', 'on-off', 'on-on', 'off-on']
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
		smartlog.setLevel(scope: CCC, level: smartlog.LEVEL_FINE)
		smartlog.setLevel(scope: DTI, level: smartlog.LEVEL_FINE)
	}
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
private def refreshCmdQueue()
{
	initUserEvent()
	smartlog.trace(CCMD, 'capability Refresh.refresh')
	def cq = CommandQueue()
	ENDPOINTS.each {
		cq.add mccEncapSwitchBinaryGet(it)
	}
	return cq
}

def refresh()
{
	return refreshCmdQueue().assemble()
}

def poll()
{
	initUserEvent()
	smartlog.trace(CCMD, 'capability Polling.poll')
	return refreshCmdQueue().assemble()
}

def configure()
{
	initUserEvent()
	smartlog.trace(CCMD, 'capability  Configuration.configure')
	def cq = CommandQueue()
	if (state?.cfg?.(3) != zwaveHubNodeId)
	{
		cq.add macroConfigurationSetGet(3 as Short, zwaveHubNodeId) // send reports to ST hub
	}
	if (state?.deviceMeta?.msr == null)
	{
		cq.add ccManufacturerSpecificGet()
	}
	cq.add(refreshCmdQueue())
	return cq.assemble()
}

// -----
// Custom commands
//
def test()
{
	initUserEvent()
	return zwave.manufacturerSpecificV1.manufacturerSpecificGet().format()
}

def switch1on()
{
	initUserEvent()
	smartlog.trace(CCMD, 'switch1on: turn endpoint1 on')
	return macroMultiChannelSwitchBinarySetGet(1, true).assemble()
}

def switch1off()
{
	initUserEvent()
	smartlog.trace(CCMD, 'switch1off: turn endpoint1 off')
	return macroMultiChannelSwitchBinarySetGet(1, false).assemble()
}

def switch2on()
{
	initUserEvent()
	smartlog.trace(CCMD, 'switch2on: turn endpoint2 on')
	return macroMultiChannelSwitchBinarySetGet(2, true).assemble()
}

def switch2off()
{
	initUserEvent()
	smartlog.trace(CCMD, 'switch2off: turn endpoint2 off')
	return macroMultiChannelSwitchBinarySetGet(2, false).assemble()
}

def allOn()
{
	initUserEvent()
	smartlog.trace(CCMD, 'executing allOn: switch all endpoints off')
	return macroMultiChannelSwitchBinarySetGet(true).assemble()
}

def allOff()
{
	initUserEvent()
	smartlog.trace(CCMD, 'executing allOff: switch all endpoints off')
	return macroMultiChannelSwitchBinarySetGet(false).assemble()
}

def cycleSwitch()
{
	initUserEvent()
	def defaultStateIndex = 0
	smartlog.trace(CCMD, 'executing cycleSwitch: setting dual relay to next combined state')
	def currentState = device.currentState("dualRelay")?.value
	def lastTriedState = state.lastTriedState ?: currentState ?: getStates()[defaultStateIndex]
 	Integer nextIndex = (getStates().indexOf(lastTriedState) + 1) % getStates().size()
	def nextState = getStates()[nextIndex] ?: getStates()[defaultStateIndex]

	def cq = CommandQueue()
	switch (nextState)
	{
		case 'on-off':
			cq.add macroMultiChannelSwitchBinarySetGet(1, true)
			break
		case 'on-on':
			cq.add macroMultiChannelSwitchBinarySetGet(2, true)
			break
		case 'off-on':
			cq.add macroMultiChannelSwitchBinarySetGet(1, false)
		break
		case 'off-off':
			cq.add macroMultiChannelSwitchBinarySetGet(2, false)
			break
	}
	state.lastTriedState = nextState
	return cq.assemble()
}

//Reports

// REGION BEGIN - ORIGIN cc_generic_command_snip.groovy main region
//
private String extractCommandId(physicalgraph.zwave.Command deviceEvent)
{
	String command
	try
	{
		Byte[] commandBytes = []
		commandBytes.add deviceEvent?.commandClass as Byte
		commandBytes.add deviceEvent?.command as Byte
		command = formatAsHex(commandBytes)
	}
	catch (exc)
	{
		command = 'could not format command class + id as hex. see description for raw value'
	}
	return command
}

/**
 * override method for a channeled zwave event
 * @param  deviceEvent	zwave command/event parent class Command
 * @param  endpoint		the endpoint this command was destined for
 * @return              CommandQueue instance of events and commands in response to this event
 */
def channeledZwaveEvent(physicalgraph.zwave.Command deviceEvent, Integer endpoint)
{
	String message = "Unhandled device event: ${deviceEvent} for endpoint ${endpoint}"
	smartlog.warn message
	String command = extractCommandId(deviceEvent)

	def cq = CommandQueue()
	cq.add([name: 'unhandled', value:command, description: deviceEvent as String, descriptionText: message, displayed: false])
	return cq
}

/**
 * zwaveEvent override method
 * Catch-all handler for unknown/unexpected Z-wave events the device might send at us
 * @param  deviceEvent  zwave command/event parent class Command
 * @return              CommandQueue instance of events and commands in response to this event
 */
def zwaveEvent(physicalgraph.zwave.Command deviceEvent)
{
	String message = "Unhandled device event: ${deviceEvent}"
	smartlog.warn message
	String command = extractCommandId(deviceEvent)

	def cq = CommandQueue()
	cq.add([name: 'unhandled', value:command, description: deviceEvent as String, descriptionText: message, displayed: false])
	return cq
}
// REGION END - ORIGIN cc_generic_command_snip.groovy main region

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicReport deviceEvent)
{
	// empty BasicReport handler so that this event isn't considered unhandled.
	// Nothing to do as we are handing this, through the MultiChannel encapsulated commands instead
	smartlog.trace(ZWEH, "handling BasicReport: '$deviceEvent'")
	return null
}

def zwaveEvent(physicalgraph.zwave.commands.switchbinaryv1.SwitchBinaryReport deviceEvent) {
	// empty SwitchBinaryReport handler so that this event isn't considered unhandled
	// Nothing to do as we are handing this through the MultiChannel encapsulated commands instead
	smartlog.trace(ZWEH, "handling SwitchBinaryReport: '$deviceEvent'")
	return null
}


// REGION BEGIN - ORIGIN cc_manufacturer_specific_snip.groovy region v1
/*
 * Command Class Manufacturer Specific v1
 * 0x72
 */

/**
 * Generates the manufacturer Specific Get command
 * 0x7204
 * @return Z-wave command object for Manufacturer Specific Get V1
 */
def ccManufacturerSpecificGet()
{
	smartlog.fine(CCC, 'issuing ccManufacturerSpecificGet')
	def cmd = zwave.manufacturerSpecificV1.manufacturerSpecificGet()
	return cmd
}

/**
 * Handles a Manufacturer Specific Report device event
 * @param  deviceEvent the device event parsed by the SmartThings z-wave utility parse command
 * @event  sends an msr event with the Manufacturer, Product Type and Product IDs
 * @state  sets the msr data under the deviceMeta.msr keys in state
 */
def zwaveEvent(physicalgraph.zwave.commands.manufacturerspecificv1.ManufacturerSpecificReport deviceEvent)
{
	smartlog.fine(ZWEH, 'handling event manufacturerspecificv1.ManufacturerSpecificReport')

	storeMsr(deviceEvent)

	String msr = msrGetMsr()
	String niceMsr = "${device.displayName} MSR: $msr; Manufacturer: ${msrGetManufacturerId()}; Product Type: ${msrGetProductTypeId()}; Product: ${msrGetProductId()}"

	smartlog.info niceMsr
	sendLoggedEvent([name: 'msr', value: msr, description: state.deviceMeta.msr.toString(), description: deviceEvent as String, descriptionText: niceMsr, displayed: true])
}

def storeMsr(physicalgraph.zwave.commands.manufacturerspecificv1.ManufacturerSpecificReport msrEvent)
{
	def msr = String.format("%04X-%04X-%04X", msrEvent.manufacturerId, msrEvent.productTypeId, msrEvent.productId)
	updateDataValue("MSR", msr)
	if (state?.deviceMeta == null) state.deviceMeta = [:]
	state.deviceMeta.msr = [msr: "$msr"]
	state.deviceMeta.msr.manufacturerId = msrEvent.manufacturerId
	state.deviceMeta.msr.productTypeId = msrEvent.productTypeId
	state.deviceMeta.msr.productId = msrEvent.productId
}

String msrGetMsr()
{
	String result = state?.deviceMeta?.msr?.msr
	smartlog.fine "msrGetMsr: returning stored MSR: $result"
	return result
}

String msrGetManufacturerId()
{
	String result = state?.deviceMeta?.msr?.manufacturerId
	smartlog.fine "msrGetMsr: returning stored Manufacturer ID: $result"
	return state?.deviceMeta?.msr?.manufacturerId
}

String msrGetProductTypeId()
{
	String result = state?.deviceMeta?.msr?.productTypeId
	smartlog.fine "msrGetMsr: returning stored Product Type ID: $result"
	return state?.deviceMeta?.msr?.productTypeId
}

String msrGetProductId()
{
	String result = state?.deviceMeta?.msr?.productId
	smartlog.fine "msrGetMsr: returning stored Product ID: $result"
	return state?.deviceMeta?.msr?.productId
}
// REGION END - ORIGIN cc_manufacturer_specific_snip.groovy region v1


// REGION BEGIN - ORIGIN cc_configuration_snip.groovy main region
/**
 * Command Class Configuration v1 0x70
 * Handles setting and getting of device-specific config parameters
 */
def ccConfigurationSet(Integer parameterNumber, Short parameterValue, Short sizeInBytes=1 as Short)
{
	// 0x7004
	smartlog.trace("Setting config parameter $parameterNumber to $parameterValue")
	def cq = CommandQueue()

	cq.add zwave.configurationV1.configurationSet(
		parameterNumber: parameterNumber,
		size: sizeInBytes,
		configurationValue: [parameterValue]
	)
	return cq
}

def ccConfigurationGet(Integer parameterNumber)
{
	// 0x7005
	smartlog.trace("issuing ConfigurationGet for parameter ${parameterNumber}")
	return zwave.configurationV1.configurationGet(parameterNumber: parameterNumber)
}

// Simply call device-specific handler
def zwaveEvent(physicalgraph.zwave.commands.configurationv1.ConfigurationReport deviceEvent)
{
	// 0x7006
	smartlog.trace("handling ConfigurationReport '$deviceEvent'")
	return handleDeviceSpecificConfigurationReport(deviceEvent)
}

def macroConfigurationSetGet(Integer parameterNumber, Short parameterValue, Short sizeInBytes=1 as Short)
{
	def cq = CommandQueue(1000)
	cq.add(ccConfigurationSet(parameterNumber, parameterValue, sizeInBytes))
	cq.add(ccConfigurationGet(parameterNumber))
	return cq
}
// REGION END - ORIGIN cc_configuration_snip.groovy main region


def handleDeviceSpecificConfigurationReport(physicalgraph.zwave.commands.configurationv1.ConfigurationReport deviceEvent)
{
	smartlog.trace("handling configurationReport specifically for $device.displayName")

	def cq = CommandQueue()
	Map evtMap = [name: "config${deviceEvent.parameterNumber}", value: deviceEvent.configurationValue, description: deviceEvent as String]
	if (state?.cfg == null) state.cfg = [:]
	if (deviceEvent.parameterNumber == 3)
	{
		Short configValue = deviceEvent.configurationValue[0]
		state.cfg[deviceEvent.parameterNumber] = configValue
		evtMap.name = 'unsolicitedReport'
		evtMap.descriptionText = "Unsolicited reports"
		switch (configValue)
		{
			case 0:
				evtMap.descriptionText += " will not be sent by device"
				break
			case [1..254]:
				evtMap.descriptionText += " will be sent to node id $configValue"
				break
			case 255:
				evtMap.descriptionText += " will be broadcast by device"
				break
			default:
				evtMap.descriptionText += " are in some weird state that I don't understand"
				break
		}
	}
	cq.add evtMap
	return cq
}


def ccMultiChannelCmdEncap(Integer endpoint, physicalgraph.zwave.Command commandToEncapsulate)
{
	smartlog.trace(CCC, "Encapsulating command $commandToEncapsulate for endpoint $endpoint")
	String formattedCmd = commandToEncapsulate.format()

	// find command class designation
	Short commandClassId = Short.decode("0x${formattedCmd.take(2)}")
	formattedCmd = formattedCmd.drop(2)

	// find command designation
	Short commandId = Short.decode("0x${formattedCmd.take(2)}")
	formattedCmd = formattedCmd.drop(2)

	// find command's parameters
	List commandParameters = []
	while (formattedCmd.size() > 0)
	{
		commandParameters.add(Short.decode("0x${formattedCmd.take(2)}"))
		formattedCmd = formattedCmd.drop(2)
	}
	smartlog.debug(CCC, "Command $commandToEncapsulate disassembles to commandClassId: ${formatAsHex(commandClassId)} ($commandClassId), commandId: ${formatAsHex(commandId)} ($commandId), parameters: ${commandParameters.each{ formatAsHex(it) + ' '}} (${commandParameters.join(' ')})")

	def testGetCmdFmt = zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint:1, destinationEndPoint:1, commandClass:37, command:2).format()
	smartlog.debug(CCC, "example of encapsulated switchBinaryGet: $testGetCmdFmt")

	def cq = CommandQueue()
	def encapCmd = zwave.multiChannelV3.multiChannelCmdEncap(
		sourceEndPoint:endpoint, destinationEndPoint:endpoint,
		commandClass:commandClassId, command:commandId, parameter:commandParameters)
	smartlog.debug(CCC, "encapsulated cmd, formatted: ${encapCmd.format()}")
	cq.add encapCmd
	return cq
}

/**
 * handle receipt of a multi channel encapsulated command
 * @param  deviceEvent [description]
 * @return             [description]
 */
def zwaveEvent(physicalgraph.zwave.commands.multichannelv3.MultiChannelCmdEncap deviceEvent)
{
	smartlog.trace(ZWEH, "MultiChannelCmdEncap: $deviceEvent")

	def cq = CommandQueue()
	smartlog.debug(ZWEH, "commandClass attribute: ${formatAsHex(deviceEvent.commandClass as Byte)}")

	Short commandClassId = deviceEvent.commandClass
	Short commandId = deviceEvent.command
	List<Short> commandParameters = deviceEvent.parameter
	Integer endpoint = deviceEvent.sourceEndPoint as Integer // from whence did the message come?

	def unencapsulatedRawCmdByteArray = []
	smartlog.debug(ZWEH, "adding $commandClassId to array as byte")
	unencapsulatedRawCmdByteArray.add(commandClassId as Byte)
	smartlog.debug(ZWEH, "adding $commandId to array as byte")
	unencapsulatedRawCmdByteArray.add(commandId as Byte)
	commandParameters.each {
		smartlog.debug(ZWEH, "adding param $it to array as byte")
		unencapsulatedRawCmdByteArray.add(it as Byte)
	}
	byte[] byteArr = unencapsulatedRawCmdByteArray as byte[]
	String unencapsulatedRawCmd = byteArr.encodeHex()

	smartlog.debug(ZWEH, "command class and command ids: $commandClassId; $commandId")
	smartlog.debug(ZWEH, "un encapsulated commmand parameters: $commandParameters")

	smartlog.debug(ZWEH, "raw command built from unencapsulated multi channel: $unencapsulatedRawCmd")
	smartlog.debug(ZWEH, "basic set off example: " + zwave.basicV1.basicSet(value: ZWAVE_OFF).format())
	smartlog.debug(ZWEH, "binary switch on example: " + zwave.switchBinaryV1.switchBinarySet(switchValue: ZWAVE_ON).format())

	// Hmm, can we use regular parse here?
	def parsedUnencapsulatedCmd = zwave.parse(unencapsulatedRawCmd, CMD_CLASS_VERSIONS)
	smartlog.debug(ZWEH, "result of parsing the encapsulated command: $parsedUnencapsulatedCmd")
	// now execute the command against the appropriate endpoint
	cq.add(channeledZwaveEvent(parsedUnencapsulatedCmd, endpoint))

	return cq
}

def channeledZwaveEvent(physicalgraph.zwave.commands.basicv1.BasicSet unencapsulatedEvent, Integer endpoint)
{
	return buildChanneledEvent(unencapsulatedEvent, endpoint)
}

def channeledZwaveEvent(physicalgraph.zwave.commands.switchbinaryv1.SwitchBinaryReport unencapsulatedEvent, Integer endpoint)
{
	return buildChanneledEvent(unencapsulatedEvent, endpoint)
}

private def buildChanneledEvent(physicalgraph.zwave.Command unencapsulatedEvent, Integer endpoint)
{
	smartlog.trace "in buildChanneledEvent for $unencapsulatedEvent to $endpoint"
	def cq = CommandQueue()
	def switchEvent = [ name: "switch${endpoint}", description: unencapsulatedEvent as String]
	if (unencapsulatedEvent.value == ZWAVE_OFF) switchEvent.value = "off"
	if (unencapsulatedEvent.value == ZWAVE_ON) switchEvent.value = "on"
	smartlog.debug("adding event for switch tile: $switchEvent")
	cq.add createEvent(switchEvent)

	Map endpointEvent = [:]
	endpointEvent[endpoint as String] = switchEvent.value
	cq.add(buildDualRelayEvent(endpointEvent))
	return cq
}

private def buildDualRelayEvent()
{
	return buildDualRelayEvent([:])
}

private def buildDualRelayEvent(Map newEndpointEvents)
{
	smartlog.trace(CCC, "buildDualRelayEvent to set the main tile state, considering endpoint settings $newEndpointEvents")
	def dualRelayEvent = [ name: 'dualRelay', value: buildDualRelayAttr(newEndpointEvents), description: unencapsulatedEvent as String ]
	smartlog.trace("adding event for dualRelay tile: $dualRelayEvent")
	return createEvent(dualRelayEvent)
}

def mccEncapSwitchBinarySet(Integer endpoint, Boolean setState)
{
	smartlog.trace(CCC, "encapsulating and issuing switchBinarySet for endpoint $endpoint to $setState")
	Short setParameter = ZWAVE_OFF
	if (setState) setParameter = ZWAVE_ON
	def cmd = zwave.switchBinaryV1.switchBinarySet(switchValue: setParameter)
	def cq = CommandQueue()
	cq.add ccMultiChannelCmdEncap(endpoint, cmd)
	return cq
}

/**
 * requests the state of one of the individual switches from the dual relay
 * @param destEndpoint indicate which of the endpoints to get status of
 * @return CommandQueue instance to send to hub.
 */
def mccEncapSwitchBinaryGet(Integer destEndpoint)
{
	smartlog.trace(CCC, "encapsulating and issuing switchBinaryGet for endpoint $endpoint")
	def cmd = zwave.switchBinaryV1.switchBinaryGet()
	def cq = CommandQueue()
	cq.add ccMultiChannelCmdEncap(destEndpoint, cmd)
	return cq
}

/**
 * Set all known device endpoints to the provided binary value
 * @param   setState  boolean specifying the state to set the endpoint(s) to
 * @return  CommandQueue instance with the commands necessary to accomplish the task
 */
 def macroMultiChannelSwitchBinarySetGet(Boolean setState)
{
	return macroMultiChannelSwitchBinarySetGet(ENDPOINTS, setState)
}

/**
 * Set the endpoint specified in the list to the provided binary value
 * @param   endpoint  the endpoint id to modify
 * @param   setState  boolean specifying the state to set the endpoint to
 * @return  CommandQueue instance with the commands necessary to accomplish the task
 */
def macroMultiChannelSwitchBinarySetGet(Integer destEndpoint, Boolean setState)
{
	return macroMultiChannelSwitchBinarySetGet([destEndpoint], setState)
}

/**
 * Set the endpoints specified in the list to the provided binary value
 * @param   endpoints  list of endpoint ids
 * @param   setState   boolean specifying the state to set the endpoint(s) to
 * @return  CommandQueue instance with the commands necessary to accomplish the task
 */
def macroMultiChannelSwitchBinarySetGet(List<Integer> endpoints, Boolean setState)
{
	smartlog.trace("macroMultiChannelSwitchBinarySetGet setting endpoints $endpoints to $setState")
	def cq = CommandQueue(500) // default half second delay
	endpoints.each { channel -> cq.add mccEncapSwitchBinarySet(channel, setState) }
	cq.add('delay 2000')
	endpoints.each { channel -> cq.add mccEncapSwitchBinaryGet(channel) }
	return cq
}


def zwaveEvent(physicalgraph.zwave.commands.hailv1.Hail deviceEvent) {
	smartlog.trace(ZWEH, "handling Hail command $deviceEvent")
	return createEvent([name: "hail", value: "hail", descriptionText: "Switch button was pressed", displayed: false])
}


// utilities

private String buildDualRelayAttr()
{
	return buildDualRelayAttr([:])
}

private String buildDualRelayAttr(Map newStates)
{
	smartlog.debug("buildDualRelayAttr with new endpoint states: $newStates")
	List endpointValues = []
	ENDPOINTS.each {
		channel ->
		String endpointCurrentValue = newStates?."$channel"?:device.currentValue("switch$channel")
		if (!endpointCurrentValue) endpointCurrentValue?:'off'
		endpointValues.add(endpointCurrentValue)
	}
	String dualRelayState = endpointValues.join('-')
	smartlog.debug("dualRelay state to set: $dualRelayState")
	return dualRelayState
}
private String formatAsHex(Byte[] byteArr)
{
	return "0x${byteArr.encodeHex()}"
}
private String formatAsHex(Byte num)
{
	return formatAsHex(num as Integer)
}

private String formatAsHex(Short num)
{
	return formatAsHex(num as Integer)
}

private String formatAsHex(Integer num)
{
	return sprintf('0x%02x', num)
}

private void logCmdQueue(List cmdQueue)
{
	cmdQueue.eachWithIndex { item, idx-> log.trace "$idx) $item" }
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
