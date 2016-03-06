/**
 * event_helpers_snip - snippet which
 * @requires
 * 		- SmartLog
 */
// ORIGIN REGION BEGIN
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
// ORIGIN REGION END
