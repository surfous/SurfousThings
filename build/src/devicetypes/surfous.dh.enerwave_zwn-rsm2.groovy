/* ZWN-RSM2 Enerwave Dual Load ZWN-RSM2
 *
 * Based on Matt Frank's handler for this device
 * which was based on the work of chrisb for AEON Power Strip.
 * Made solid by surfous.
 *
 *  Date: {{BUILDDATE}}
 *  Build: {{BUILDTAG}}
 *
 *  URL: https://graph.api.smartthings.com/ide/device/editor/f8df0662-222f-4737-b429-baec0c8109aa
 */
import groovy.transform.Field

import physicalgraph.device.HubAction
import physicalgraph.zwave.Command

// ADDIN TARGET colors_snip

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
	definition (name: "Enerwave ZWN-RSM2 Smart Dual Relay Switch Module", namespace: "surfous", author: "Kevin Shuk") {
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

// ADDIN TARGET event_helpers_snip

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

// ADDIN TARGET parse_command_snip

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

// ADDIN TARGET cc_generic_command_snip

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


// ADDIN TARGET cc_manufacturer_specific_snip v1


// ADDIN TARGET cc_configuration_snip


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


// ADDIN TARGET SmartLog_cc


// ADDIN TARGET CommandQueue_cc
