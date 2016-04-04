/**
 *  StuffOScope - device handler development helper tool
 *
 *  Props to the SmartThings crew and the fantastic community for the support and knowledge
 *  sharing that allowed me to craft this device
 *
 *  Author: surfous
 *  Date: {{BUILDDATE}}
 *  Build: {{BUILDTAG}}
 */

import groovy.transform.Field

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

// smartlog scopes
@Field final String ZWEH = 'Z-WaveEventHandler' // For handlers of events sent by the device itself
@Field final String DTI = 'DeviceTypeInternal' // for commands that are automatically called in a device handler's lifecycle
@Field final String CCMD = 'STDeviceCommand' // capability or standalone command
@Field final String CCC = 'CommandClassCommand' // wraps a single command class
@Field final String SCOPE = 'StuffOScope' // For StuffOScope results


// Global Variables
@Field def smartlog
@Field Long wakeUpPeriod

metadata
{
	definition (name: "StuffOScope Device Handler Dev Tool", namespace: "surfous", author: "surfous")
	{
		capability "Configuration"
		capability "Sensor"
		capability "Actuator"

		attribute 'lastUpdated', 'number'
		attribute "blank", "string" // just as the name implies...
	}

	simulator
	{

	}

}

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
	smartlog.setLevel(level: smartlog.LEVEL_FINE)
	smartlog.setLevel(scope: smartlog.type, level: smartlog.LEVEL_NONE)
	smartlog.setLevel(scope: 'CommandQueue', level: smartlog.LEVEL_WARN)
	smartlog.setLevel(scope: 'ResponseQueue', level: smartlog.LEVEL_WARN)
	smartlog.setLevel(scope: ZWEH, level: smartlog.LEVEL_FINE)
	smartlog.setLevel(scope: DTI, level: smartlog.LEVEL_FINE)
	smartlog.setLevel(scope: SCOPE, level: smartlog.LEVEL_INFO)
}


// -----
//  Device type interface methods
//
def parse(String rawZwaveEventDescription) {
	initDeviceEvent()
	String parseInstance = now() as String
	smartlog.fine(DTI, "parse $parseInstance: raw incoming event '${rawZwaveEventDescription}'")
	def result = []
	try
	{
		if (rawZwaveEventDescription == 'updated')
		{
			// do nothing - handles rogue invocation of parse with the arg set simply to 'updated'
			smartlog.warn(DTI, 'parse argument rawZwaveEventDescription was the plain string "updated." Bypassing this erroneous command.')
			return null
		}
		def cmd = zwave.parse(rawZwaveEventDescription, CMD_CLASS_VERSIONS)
		smartlog.trace(DTI, "parse $parseInstance: zwave.parse returned $cmd")
		if (cmd)
		{
			def deviceEventResult = zwaveEvent(cmd)
			// If event handler returned a CommandQueue or ResponseQueue & prepare it accordingly
			if (deviceEventResult instanceof Map && deviceEventResult?.type)
			{
				if (deviceEventResult.type == 'CommandQueue')
				{
					smartlog.fine(DTI, 'parse: event handler returned a CommandQueue')
					def rq = ResponseQueue()
					rq.add(deviceEventResult)
					result = rq.prepare()
				}
				else if (deviceEventResult.type == 'ResponseQueue')
				{
					smartlog.fine(DTI, 'parse: device event handler returned a ResponseQueue')
					result = deviceEventResult.prepare()
				}
				else
				{
					smartlog.fine(DTI, 'parse: device event handler returned a confusing map with a type member. I guess we pass it on?')
					result = deviceEventResult
				}
			}
			else
			{
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

// -----
//	Local utility methods
//
private void sendExceptionEvent(Throwable t)
{
	sendExceptionEvent(t, 'caught exception')
}

private void sendExceptionEvent(Throwable t, String prologue)
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
		sendLoggedEvent([name: 'EXCEPTION', value: addlExceptionMsg,
			description: addlExceptionMsg, descriptionText: addlDescText, displayed: true])
	}
	finally
	{
		smartlog.error( msg)
		sendLoggedEvent(evtMap)
	}
}

private String formatOctetAsHex(Short octet)
{
	return sprintf('%#x', octet)
}

private void initReport()
{
	if (state?.report == null)
	{
		clearReport()
	}
}

private void clearReport()
{
	state.report = [:]
}

/**
 * Callback support methods
 */
import static java.util.UUID.randomUUID

private def CallbackSupport()
{
	Map cs = [:]

	cs.instanceUuid = randomUUID() as String

	// cs.Register(physicalgraph.zwave.Command expectedResponse, Map expectedResponseProperties, physicalgraph.zwave.Command callingCommand, List methodAndArgs, Integer timeoutSeconds)
	{

	}

	// state.callbackdb
	// 	.registry
	// 		callbackRegistrationUuid
	// 		callbackSupportInstanceUuid
	// 		callingCommandName
	// 		responseCommandName
	// 		zwaveCommandVersion
	// 		timeoutSeconds
	// 		registrationTimestamp
	// 		sentTimestamp
	// 		elapsedSinceSent
	//

	cs.markCommandsSent =
	{
		// set all members of state.callbackdb.registry with callbackSupportInstanceUuid = cs.instanceUuid
		// with a sentTimestamp of now()
		// note that this does not take into account delays between commands
	}

	cs.checkForTimeouts =
	{

	}

	return cs
}

/**
 * kick off SOS exam
 * * need to determine whether device is mains connected, beaming or sleepy
 */
private void startExam()
{

}

private void endExam()
{

}

private void pendCommandExpectingResponse()
{

}

private void acknowledgeResponseReceived()
{

}


/**
 * wrapper method for sendEvent. Uses smartlog to log the event initiation
 * @param eventMap [description]
 */
private void sendLoggedEvent(Map eventMap)
{
	smartlog.info "sendLoggedEvent - sending immediate event: $eventMap"
	sendEvent(eventMap)
}

/**
 * Gets the name of a zwave command method
 * @param  zwaveCmd a zwave command received from the device and parsed by the zwave utility class
 * @return		  The name of the Z-Wave command method
 */
String getZwaveCommandName(physicalgraph.zwave.Command zwaveCmd)
{
	String zwaveCmdStr = zwaveCmd as String
	return zwaveCmdStr.replaceAll(~/\(.*$/, '')
}


String dustForFingerprint()
{
	String mark = '0xEF'
	//String rawDescriptionString = device.device.rawDescription
	String rawDescriptionString = '0 0 0x0701 0 0 0 f 0x5E 0x71 0x31 0x33 0x72 0x86 0x59 0x85 0x70 0x77 0x5A 0x7A 0x73 0xEF 0x20' // for testing
	Short deviceId = null
	List inClusters = []
	List outClusters = []
	Boolean isMarkFound = false
	rawDescriptionString.tokenize().each {
		token ->
		// Look for device id first
		if (deviceId == null && token ==~ /0x[0-9A-Fa-f]{4}/)
		{
			deviceId = Short.decode(token)
		}
		// Second, collect hex formatted bytes as inClusters, until the end or we find the Mark
		else if (token ==~ /0x[0-9A-Fa-f]{2}/ && !isMarkFound && !(token ==~ /$mark/))
		{
			inClusters.add(Byte.decode(token))
		}
		// Third, if we find a Mark
		else if (token ==~ /$mark/)
		{
			isMarkFound = true;
		}
		// Fourth, collect any additional hex-formatted bytes after the mark
		else if (token ==~ /0x[0-9A-Fa-f]{2}/ && isMarkFound)
		{
			outClusters.add(token)
		}

	}
	def fingerprint = "fingerprint deviceId:\"$deviceId\""
	if (inClusters.size() > 0)
	{
		fingerprint += /, inClusters:"${inClusters.join(', ')}"/
	}
	if (outClusters.size() > 0)
	{
		fingerprint += /, outClusters:"${outClusters.join(', ')}"/
	}
	return fingerprint
}

// -----
// CommandClass command and handler implementations
//


// // Catch-all handler.for any unsupported zwave events the device might send at us
def zwaveEvent(physicalgraph.zwave.Command deviceEvent)
{
	String message = "Unhandled device event: '${deviceEvent}'"
	smartlog.warn(ZWEH, message)
	return createEvent([name: 'unhandled', value:'', description: deviceEvent as String, descriptionText: message, displayed: false])
}

/**
 * Catch-all device-specific code hook handler
 * @param  deviceEvent parsed event object
 * @return			 CommandQueue
 */
def zwaveCallback(physicalgraph.zwave.Command deviceEvent)
{
	smartlog.trace "No device-specific event defined for $deviceEvent"
}

// // CommandClass ZWave

def ccZwaveCmdClassRequestNodeInfo()
{
	smartlog.debug(CCC, "issuing zwaveCmdClassRequestNodeInfo")
	def cmd = zwave.zwaveCmdClassV1.requestNodeInfo()
	return cmd
}

def zwaveEvent(physicalgraph.zwave.commands.zwavecmdclassv1.NodeInfo deviceEvent)
{
	smartlog.fine(ZWEH, "handling event zwavecmdclassv1.NodeInfo: $deviceEvent")
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

/*
// TODO Move to StuffOScope
def checkCommandClassVersions()
{
	smartlog.debug("Checking versions for these CommandClasses: ${CMD_CLASS_VERSIONS.keySet().collect{ formatOctetAsHex(it as Short) }.join(', ')}")
	def cq = CommandQueue()
	CMD_CLASS_VERSIONS.keySet().each
	{
		String ccIdStr = formatOctetAsHex(it as Short)
		if (state.ccVersions.get(ccIdStr) == null)
		{
			cq.add(ccVersionCommandClassGet(it as Short))
		}
		else
		{
			smartlog.debug "We already have a version for $ccIdStr: ${state.ccVersions.get(ccIdStr)}"
		}
	}
	return cq
}
*/

// // Command Class Security

def ccSecurityCommandsSupportedGet(String callbackMethod=null)
{
	log.trace "Executing zwave.securityV1.securityCommandsSupportedGet"
	if (callbackMethod)
	{
		registerCallbackOnReport(physicalgraph.zwave.commands.securityv1.SecurityCommandsSupportedReport,
			callbackMethod)
	}
	return secure(zwave.securityV1.securityCommandsSupportedGet())
}

	* registerCallbackOnReport
	* doesReportHaveRegisteredCallback
	* runRegisteredCallbackMethod
	* maintainRegisteredCallbacks ( resend raw zwave cmd if timed out)

// 0x9803
def zwaveEvent(physicalgraph.zwave.commands.securityv1.SecurityCommandsSupportedReport cmd)
{
	log.trace "handling event securityv1.SecurityCommandsSupportedReport"
	state.sec = cmd.commandClassSupport.collect { formatOctetAsHex(it as Byte) + ' ' }.join()
	if (cmd.commandClassControl)
	{
		state.secCon = cmd.commandClassControl.collect { formatOctetAsHex(it as Byte) + ' ' }.join()
	}
	log.debug "Command Classes requiring security: $state.sec"

	if (doesReportHaveRegisteredCallback(cmd))
	{
		List callbackCmds = runRegisteredCallbackMethod(cmd)
	}
	return createEvent(name:"secureInclusion", value:"success", descriptionText:"Lock is securely included")
}

/*
	ClosureClasses down here.
 */

// ADDIN TARGET Smartlog_cc

// ADDIN TARGET CommandQueue_cc
