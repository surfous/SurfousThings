// ORIGIN REGION BEGIN disambiguation
// for fingerprint disambiguation
@Field final Map DEVICE_PRODUCT_ID_DISAMBIGUATION = [
	'0x2001': 'Monoprice NO-LABEL Door & Window Sensor (Contact Sensor)',
	'0x2003': 'Monoprice NO-LABEL Shock Sensor (Acceleration Sensor)',
	'0x200a': 'Monoprice NO-LABEL Garage Door Sensor (Contact Sensor via tilt)'
	].withDefault {UNKNOWN.NAME}
// ORIGIN REGION END disambiguation


// ORIGIN REGION BEGIN wakeup_macros
def macroWakeUpRitual()
{
	smartlog.trace('macroWakeUpRitual')
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
		cq.add(taskGetBattery())
		cq.add(taskGetWakeupInterval())
		cq.add(taskGetAssociation())
		cq.add(macroSendToSleep())
	}

	return cq
}

def macroSendToSleep()
{
	smartlog.trace('macroSendToSleep')
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
// ORIGIN REGION END wakeup_macros
