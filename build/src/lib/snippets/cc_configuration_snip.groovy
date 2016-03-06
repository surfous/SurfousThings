// ORIGIN REGION BEGIN
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
// ORIGIN REGION END
