// ORIGIN REGION BEGIN
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
// ORIGIN REGION END
