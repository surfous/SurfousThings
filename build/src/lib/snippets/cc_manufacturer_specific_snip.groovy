/*
	Requires:
	SmartLog closure class configured and instantiated to 'smartlog'
 */

// ORIGIN REGION BEGIN v1
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
// ORIGIN REGION END v1
