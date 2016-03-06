/*
	Requirements:
	* CommandQueue ClosureClass
	* SmartLog ClosureClass
	* initDeviceEvent must be implemented
	* smartLog must be instantiated and initialized
		* usually called from initDevice Event
 */

// ORIGIN REGION BEGIN
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
// ORIGIN REGION END
