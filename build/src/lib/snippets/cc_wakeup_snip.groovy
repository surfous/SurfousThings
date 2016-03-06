/*
 * Liases the WakeUp Command Class v2
 * 0x84
 * requires:
 * parse_command_snip
 * SmartLog
 * implemented chainDeviceMetadata method
 */
// ORIGIN REGION BEGIN v2
/**
* Z-wave event handler overload for WakeUpNotification v2
* This is how the device tells us it has awoken and is ready to receive and respond to commands
* @param  deviceEvent parsed z-wave event WakeUpNotification subclass
* @return             CommandQueue
 */
def zwaveEvent(physicalgraph.zwave.commands.wakeupv2.WakeUpNotification deviceEvent) {
	smartlog.trace("handling WakeUpNotification '$deviceEvent'")
	Long period = now()
	def cq = CommandQueue()
	sendEvent([name: "wakeup-$period", value: 'wakeUpNotification', isStateChange: true, description: deviceEvent as String, descriptionText: "${device.displayName} woke up.", displayed: false])

	cq.add(macroWakeUpRitual())
	return cq
}

/**
 * 0x8404
 * Sets the device wakeup interval
 * Min and max will depend on device
 * Sleepy devices will only receive this command when they are awake
 * @param  seconds Number of seconds between wakeups
 * @return         formatted Z-wave command, ready to send to device
 */
def ccWakeUpIntervalSet(Integer seconds)
{
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

/**
 * 0x8405
 * Requests the current wkeup interval from the deviceAttrName
 * Device will only receive and respond to this command when it is awake
 * @return formatted Z-wave command, ready to send to device
 */
def ccWakeUpIntervalGet()
{
	smartlog.trace(CCC, 'issuing WakeUpIntervalGet')
	return zwave.wakeUpV2.wakeUpIntervalGet()
}

/**
 * Z-wave event handler overload for WakeUpIntervalReport v2
 * @param  deviceEvent parsed z-wave event WakeUpIntervalReport subclass
 * @return             CommandQueue
 */
def zwaveEvent(physicalgraph.zwave.commands.wakeupv2.WakeUpIntervalReport deviceEvent)
{
	smartlog.trace("handling WakeUpIntervalReport '$deviceEvent'")
	def cq = CommandQueue()
	Integer intervalSeconds = deviceEvent.seconds
	Short nodeid = deviceEvent.nodeid
	if (!state?.cfg) state.cfg = [:]
	state.cfg.wakeupSeconds = intervalSeconds
	String msg = "Device ${device.displayName} reports a wake up interval of $intervalSeconds for node $nodeid"
	smartlog.info msg
	Map evtMap = [name: 'wakeInterval', value: intervalSeconds, unit: 'seconds', description: deviceEvent as String, descriptionText: msg, displayed: true]
	cq.add evtMap
	return cq
}

/**
 * Requests Wakeup Interval Capabilities
 * @return zwave command
 */
def ccWakeUpIntervalCapabilitiesGet()
{
	// 0x8409
	smartlog.trace(CCC, 'issuing WakeUpIntervalCapabilitiesGet')
	return zwave.wakeUpV2.wakeUpIntervalCapabilitiesGet()
}

/**
 * handles wakeupv2.WakeUpIntervalCapabilitiesReport. Member of the deviceMetadata chain gang
 * @param  deviceEvent zwave device event from zwave.parse method
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

/**
 * 0x8408
 * Tells device that we have no more commands to send it, and that it should sleep when it
 * finishes all pending action
 * Device will only receive and respond to this command when it is awake
 * @return formatted Z-wave command, ready to send to device
 */
def ccWakeUpNoMoreInformation()
{
	// 0x8408
	smartlog.trace(CCC, 'issuing WakeUpNoMoreInformation')
	return zwave.wakeUpV2.wakeUpNoMoreInformation()
}
// ORIGIN REGION END v2
