/* CommandQueue closure class
 *
 * Helper that takes a series of ST commands and events, inserts delays if necessary, and
 * formats it suitable for either sending a command or a response
 *
 *  Date: {{BUILDDATE}}
 *  Build: {{BUILDTAG}}
 *
 */
import groovy.transform.Field

// Harness
public class HubAction
{
	private Object commands

	HubAction(Object cmds)
	{
		commands = cmds;
	}

	public String toString()
	{
		return "HA: $commands"
	}
}

HubAction response(Object cmds)
{
	return new HubAction(cmds)
}

public class Command
{
	public String cmdName
	public Map argMap

	public Command(String command)
	{
		this([:], command)
	}
	public Command(Map named, String command)
	{
		this.cmdName = command
		this.argMap = named
	}

	public String format()
	{
		return "FORMATTED: ${this as String}"
	}

	public String toString()
	{
		def argStr = ""
		argMap.each{ arg,val-> argStr += "$arg: $val, "}
		argStr = argStr.replaceAll(/,\s$/, '')
		return "${cmdName}($argStr)"
	}
}

public class log
{
	public static void trace(msg) { println ">> TRACE: $msg" }
	public static void debug(msg) { println ">> DEBUG: $msg" }
	public static void info(msg)  { println ">>  INFO: $msg" }
	public static void warn(msg)  { println ">>  WARN: $msg" }
	public static void error(msg) { println ">> ERROR: $msg" }
}

Map createEvent(Map eventMap)
{
	eventMap.eventCreateLevel = eventMap?.eventCreateLevel?eventMap.eventCreateLevel++:1
	return eventMap
}

@Field Map state = [:]

// ORIGIN REGION BEGIN
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
// ORIGIN REGION END


tests:
{
	def slog = Smartlog()
	def d = CommandQueue()
	assert d.type == 'CommandQueue'
	slog.setLevel(scope: d.type, level: slog.LEVEL_FINE)

	def cqChkDefaultDelay = CommandQueue(d.defaultDelayMsec + 25)
	assert cqChkDefaultDelay.defaultDelayMsec > d.defaultDelayMsec

	assert d.length() == 0
	assert d.isLastEntryACommand() == false

	d.add(new Command("First command"))
	assert d.length() == 1
	assert d.assemble()[0].startsWith('FORMATTED: First')

	d.add(new Command("Second command"))
	assert d.length() == 3
	assert d.assemble()[1].startsWith('delay')
	assert d.assemble()[2].startsWith('FORMATTED: Second')
	d.add([name: 'event after second command', value: '42'])
	assert d.length() == 4
	assert d.assemble()[3] instanceof Map
	assert d.assemble()[3].eventCreateLevel == 1

	d.add("delay 1000")
	assert d.length() == 5
	assert d.assemble()[4].startsWith('delay 1000')

	d.add(new Command("Third command"), 2000)
	assert d.length() == 7
	assert d.assemble()[5].startsWith('FORMATTED: Third')
	assert d.assemble()[6].startsWith('delay 2000')

	d.prepend(new Command("Zeroth command"))
	assert d.length() == 9
	assert d.assemble()[0].startsWith('FORMATTED: Zeroth')
	assert d.assemble()[1].startsWith('delay')

	def e = CommandQueue()
	assert e.length() == 0
	def noDelayCmdList = [new Command("Fake Cmd 1"), new Command("Fake Cmd 2"), new Command("Fake Cmd 3")]
	e.add(noDelayCmdList)
	assert e.length() == 5
	assert d.assemble()[1].startsWith('delay')
	assert d.assemble()[3].startsWith('delay')

	// replicating a queue's command list should come out identically
	def f = CommandQueue()
	f.add(d.getEntries())
	assert d.length() == f.length()

	// Should be exactly the combined lengths since d ends in a delay
	f.add(e.getEntries())
	assert f.length() == (d.length() + e.length())

	// The other way around, though, we need to add a delay
	def g = CommandQueue()
	g.add(e.getEntries())
	g.add(d.getEntries())
	assert g.length() == (d.length() + e.length() + 1)


	def h = CommandQueue()
	assert h.type == 'CommandQueue' // still a command queue
	// add a CommandQueue directly to a ResponseQueue
	h.add(g)
	assert g.length() == h.length()
	Integer rqExpectedSize = h.length()

	h.add([name: "event", value: "cool event"])
	assert ++rqExpectedSize == h.length()
	h.add(createEvent([name: "event", value: "cooler event"]))
	assert ++rqExpectedSize == h.length()

	h.prepend(new Command("first command"), 777)
	assert (rqExpectedSize += 2) == h.length()

	h.prepend(createEvent([name: "event", value: "first event"]))
	assert ++rqExpectedSize == h.length()

	println "rq final expected size is $rqExpectedSize. Actual size is ${h.length()}"
	h.getEntries().eachWithIndex { entry, idx -> println "${idx}.\t${entry}" }

	println "\n\nnow prepared:"
	h.assembleResponse().eachWithIndex { entry, idx -> println "${idx}.\t${entry}" }

	println "\n\nBut the entry list should remain unchanged"
	h.getEntries().eachWithIndex { entry, idx -> println "${idx}.\t${entry}" }
}

// ADDIN TARGET SmartLog_cc.groovy
