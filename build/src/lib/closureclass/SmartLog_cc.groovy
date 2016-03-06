import groovy.transform.Field

public class log
{
	public static String lastLogMsg
	public static String lastLogLevel
	public static void trace(msg) { writeLog('TRACE', msg) }
	public static void debug(msg) { writeLog('DEBUG', msg) }
	public static void info(msg)  { writeLog('INFO',  msg) }
	public static void warn(msg)  { writeLog('WARN',  msg) }
	public static void error(msg) { writeLog('ERROR', msg) }

	private static void writeLog(level, msg)
	{
		if (!msg.startsWith('[Smartlog]'))
		{
			// Don't note last logs for metalogging
			lastLogMsg = msg
			lastLogLevel = level
		}
		println String.format('>> %5s: %s', level, msg)
	}
}

@Field Map state = [:]

// VV  ---- 8< ----  START BELOW THIS LINE  ---- 8< ----  VV

// ORIGIN REGION BEGIN
def Smartlog(String defaultScope=null)
{
	Map sl = [:]
	sl.type = 'Smartlog'
	sl.version = '20150530a'

	sl.SCRIPT_SCOPE = 'script'
	sl.INSTANCE_DEFAULT_SCOPE = defaultScope?:sl.SCRIPT_SCOPE
	sl.OVERRIDE_SCOPE = 'OVERRIDE'

	sl.LEVEL_NONE = null
	sl.LEVEL_ERROR = 'error'
	sl.LEVEL_WARN = 'warn'
	sl.LEVEL_INFO = 'info'
	sl.LEVEL_DEBUG = 'debug'
	sl.LEVEL_TRACE = 'trace'
	sl.LEVEL_FINE = 'FINEtrace'

	sl.SMARTLOG_LEVELS = [sl.LEVEL_NONE, sl.LEVEL_ERROR, sl.LEVEL_WARN, sl.LEVEL_INFO, sl.LEVEL_DEBUG, sl.LEVEL_TRACE, sl.LEVEL_FINE]

	sl.SMARTLOG_DEFAULT_LEVEL = sl.LEVEL_DEBUG

	sl.format =
	{
		String scope, String msg ->
		if (scope && scope != sl.SCRIPT_SCOPE) return "[$scope] $msg"
		return msg
	}

	sl.callWrappedLogMethod =
	{
		String scope, String level, String msg ->
		def logLevelMatcher = (level =~ /^([A-Z]+)?([a-z]+)$/)
		String logLevel = logLevelMatcher[0][2]
		log."${logLevel}"(sl.format(scope, msg))
	}

	// for logging within Smartlog
	sl.METALOG_DEFAULT_LEVEL = sl.LEVEL_NONE
	sl.metalog =
	{
		String level, String msg ->
		String scope = sl.type
		String metalogLevel = state.smartlog.get(scope, sl.METALOG_DEFAULT_LEVEL)
		if (sl.SMARTLOG_LEVELS.indexOf(metalogLevel) >= sl.SMARTLOG_LEVELS.indexOf(level))
		{
			sl.callWrappedLogMethod(scope, level, msg)
		}
	}

	sl.log =
	{
		Object[] varArgs ->
		List argList = ([null, null] + varArgs).flatten()
		if (argList.size() < 3)
		{
			// At least one arg, the message, must be passed in
			sl.metalog(sl.LEVEL_ERROR, "log called with no arguments")
			return
		}
		String scope = argList[-3]?:sl.INSTANCE_DEFAULT_SCOPE
		String level = argList[-2]
		String msg = argList[-1]

		sl.metalog(sl.LEVEL_DEBUG, "in log with args scope: $scope; lvl: $level; '$msg'")

		if (!level || !sl.SMARTLOG_LEVELS.contains(level))
		{
			// set a default level
			level = sl.SMARTLOG_DEFAULT_LEVEL
		}

		String scopeActiveLevel = sl.getLevel(scope)
		if (scopeActiveLevel == sl.LEVEL_NONE) return // logging is off
		if (sl.SMARTLOG_LEVELS.indexOf(scopeActiveLevel) >= sl.SMARTLOG_LEVELS.indexOf(level))
		{
			sl.callWrappedLogMethod(scope, level, msg)
		}
	}

	sl.convertLogArgs =
	{
		String level, Object[] varArgs ->
		List argList = ([sl.INSTANCE_DEFAULT_SCOPE] + varArgs).flatten()
		if (argList.size() < 2)
		{
			// At least one arg, the message, must be passed in
			sl.metalog(sl.LEVEL_ERROR, "log level $level helper called with no arguments")
			return
		}
		List logArgs = [argList[-2] as String, level, argList[-1] as String]
		sl.metalog(sl.LEVEL_DEBUG, "convertLogArgs is returning $logArgs")
		return logArgs
	}

	// log level helpers
	//
	sl.error =
	{
		Object[] varArgs ->
		List logArgs = sl.convertLogArgs(sl.LEVEL_ERROR, varArgs)
		sl.log(*logArgs)
	}

	sl.warn =
	{
		Object[] varArgs ->
		List logArgs = sl.convertLogArgs(sl.LEVEL_WARN, varArgs)
		sl.log(*logArgs)
	}

	sl.info =
	{
		Object[] varArgs ->
		List logArgs = sl.convertLogArgs(sl.LEVEL_INFO, varArgs)
		sl.log(*logArgs)
	}

	sl.debug =
	{
		Object[] varArgs ->
		List logArgs = sl.convertLogArgs(sl.LEVEL_DEBUG, varArgs)
		sl.log(*logArgs)
	}

	sl.trace =
	{
		Object[] varArgs ->
		List logArgs = sl.convertLogArgs(sl.LEVEL_TRACE, varArgs)
		sl.log(*logArgs)
	}

	sl.fine =
	{
		Object[] varArgs ->
		List logArgs = sl.convertLogArgs(sl.LEVEL_FINE, varArgs)
		sl.log(*logArgs)
	}

	sl.initialize =
	{
		->
		if (state?.smartlog == null)
		{
			state.smartlog = [:]
			sl.setLevel(scope: sl.INSTANCE_DEFAULT_SCOPE, level: sl.SMARTLOG_DEFAULT_LEVEL)
			sl.setLevel(scope: sl.type, level: sl.LEVEL_NONE) // set our own level off by default
			sl.metalog(sl.LEVEL_DEBUG, 'state initialized' )
		}
	}

	sl.reset =
	{
		->
		sl.metalog(sl.LEVEL_TRACE, 'resetting state' )
		state.smartlog = null
		sl.initialize()
	}

	sl.setLevel =
	{
		Map optArgs = [:] ->
		String scope = optArgs?.scope?:sl.INSTANCE_DEFAULT_SCOPE
		String level = optArgs?.level
		if (!optArgs.containsKey('level')) level = sl.SMARTLOG_DEFAULT_LEVEL

		sl.metalog(sl.LEVEL_TRACE, "in setSmarlLogLevel(scope: ${optArgs?.scope}, level: ${optArgs?.level})")
		if (sl.SMARTLOG_LEVELS.contains(level)) // check validity of level
		{
			sl.metalog(sl.LEVEL_DEBUG, "Actually setting log level for scope '$scope' to '$level'")
			state.smartlog[scope] = level
			sl.metalog(sl.LEVEL_FINE, "Now the smartog state contains ${state.smartlog}")
		}
	}

	sl.setOverrideLevel =
	{
		String level ->
		sl.metalog(sl.LEVEL_TRACE, "in setOverrideLevel($level)")
		if (sl.SMARTLOG_LEVELS.contains(level))
		{
			sl.metalog(sl.LEVEL_DEBUG, "Actually setting override log level for all scopes to '$level'")
			state.smartlog[sl.OVERRIDE_SCOPE] = level
		}
	}

	sl.clearOverride =
	{
		->
		sl.metalog(sl.LEVEL_TRACE, "in clearOverride")
		if (state.smartlog.get(sl.OVERRIDE_SCOPE))
		{
			state.smartlog.remove(sl.OVERRIDE_SCOPE)
			sl.metalog(sl.LEVEL_DEBUG, "Removed override log level")
		}
	}

	// no logging from this member!!
	sl.getLevel =
	{
		String scope->
		scope = scope?:sl.INSTANCE_DEFAULT_SCOPE
		sl.metalog(sl.LEVEL_TRACE, "in getLevel for $scope")

		// if override is set, return it
		String overrideLevel = state.smartlog.get(sl.OVERRIDE_SCOPE)
		if (overrideLevel) return overrideLevel

		String scriptScopeLevel = state.smartlog.get(sl.SCRIPT_SCOPE, sl.SMARTLOG_DEFAULT_LEVEL)
		String level = state.smartlog.get(scope, scriptScopeLevel)
		return level
	}

	sl.initialize()
	sl.metalog(sl.LEVEL_DEBUG, "Constructed Smartlog instance with state of $state.smartlog")

	return sl
}
// ORIGIN REGION END
// ^^  ---- 8< ----  END ABOVE THIS LINE  ---- 8< ----  ^^

def checkLogEmittance(String level, String msg)
{
	checkLogEmittance(null, level, msg)
}

void checkLogEmittance(String scope, String level, String msgPart)
{
	level = level.replaceAll(/[A-Z]/, '')
	def m = (log.lastLogMsg =~ /^(?:\[(.*?)\])?\s?(.*)$/)
	assert m.hasGroup()
	def group = m[0]
	String msgScope = null
	String msgMsg
	if (group.size() > 2)
	{
		msgScope = group[1]
		msgMsg = group[2]
	}
	else
	{
		msgMsg = group[1]
	}
	assert scope == msgScope
	assert msgMsg.contains(msgPart)
	assert level == log.lastLogLevel.toLowerCase()

}

// ORIGIN REGION BEGIN tests
tests:
{
	def smart = Smartlog()
	smart.setLevel(scope: smart.type, level: smart.LEVEL_FINE)
	println "level for 'Smartlog' scope is " + smart.getLevel(smart.type)
	println "smartlog state: ${state?.smartlog}"
	smart.log(smart.LEVEL_INFO, "info is logged")
	checkLogEmittance(smart.LEVEL_INFO, "is logged")

	smart.log(smart.LEVEL_TRACE, 'nope, not logging trace - ERROR IF SEEN')
	checkLogEmittance(smart.LEVEL_INFO, "is logged")

	smart.setLevel(level: smart.LEVEL_TRACE)
	smart.log(smart.LEVEL_TRACE, 'trace is now logged')
	checkLogEmittance(smart.LEVEL_TRACE, "now logged")

	smart.log(smart.LEVEL_FINE, 'but fine trace isn\'t - ERROR IF SEEN')
	checkLogEmittance(smart.LEVEL_TRACE, "now logged")
	println "smartlog state: ${state?.smartlog}"

	def smartNibbleScope = 'SmartNibble'
	smart.setLevel(scope: smartNibbleScope, level: smart.LEVEL_WARN)
	smart.log(smart.LEVEL_INFO, 'base log can log info events')
	checkLogEmittance(smart.LEVEL_INFO, 'can log info')

	smart.log(smartNibbleScope, smart.LEVEL_INFO, 'but smartnibble scope can\'t - ERROR IF SEEN')
	checkLogEmittance(smart.LEVEL_INFO, 'can log info')

	smart.log(smartNibbleScope, smart.LEVEL_WARN, 'smartnibble scope can log warn, though')
	checkLogEmittance(smartNibbleScope, smart.LEVEL_WARN, 'can log warn')

	smart.log(smartNibbleScope, smart.LEVEL_ERROR, 'and error')
	checkLogEmittance(smartNibbleScope, smart.LEVEL_ERROR, 'and error')

	smart.setLevel(scope: smartNibbleScope, level: smart.LEVEL_INFO)
	smart.log(smartNibbleScope, smart.LEVEL_INFO, 'now smartnibble scope can log info events')
	checkLogEmittance(smartNibbleScope, smart.LEVEL_INFO, 'can log info')

	smart.log(smartNibbleScope, smart.LEVEL_FINE, 'neither scope can log fine trace - ERROR IF SEEN')
	checkLogEmittance(smartNibbleScope, smart.LEVEL_INFO, 'can log info')
	smart.log(smart.LEVEL_FINE, 'neither scope can log fine trace - ERROR IF SEEN')
	checkLogEmittance(smartNibbleScope, smart.LEVEL_INFO, 'can log info')

	smart.setOverrideLevel(smart.LEVEL_FINE)
	smart.setLevel(scope: smart.type, level: smart.LEVEL_FINE)

	smart.log(smart.LEVEL_FINE, 'now both scopes can log fine trace')
	checkLogEmittance(smart.LEVEL_FINE, 'both scopes can log fine')

	smart.log(smartNibbleScope, smart.LEVEL_FINE, 'now both scopes can log fine trace')
	checkLogEmittance(smartNibbleScope, smart.LEVEL_FINE, 'both scopes can log fine')

	smart.reset()
	smart.setLevel(scope: smart.type, level: smart.LEVEL_FINE)

	smart.log(smart.LEVEL_FINE, 'now reset back to the default - ERROR IF SEEN')
	checkLogEmittance(smartNibbleScope, smart.LEVEL_FINE, 'both scopes can log fine')
	smart.log(smartNibbleScope, smart.LEVEL_FINE, 'now reset back to the default - ERROR IF SEEN')
	checkLogEmittance(smartNibbleScope, smart.LEVEL_FINE, 'both scopes can log fine')

	smart.log(smart.LEVEL_DEBUG, 'now reset back to the default')
	checkLogEmittance(smart.LEVEL_DEBUG, 'now reset back to the default')
	smart.log(smartNibbleScope, smart.LEVEL_DEBUG, 'now reset back to the default (if its scope is not set, scoped event logs at the base level)')
	checkLogEmittance(smartNibbleScope, smart.LEVEL_DEBUG, 'now reset back to the default')

	smart.setOverrideLevel(smart.LEVEL_FINE)
	smart.setLevel(scope: smart.type, level: smart.LEVEL_FINE)

	smart.fine('log level fine helper')
	checkLogEmittance(smart.LEVEL_FINE, 'helper')
	smart.trace('log level trace helper')
	checkLogEmittance(smart.LEVEL_TRACE, 'helper')
	smart.debug('log level debug helper')
	checkLogEmittance(smart.LEVEL_DEBUG, 'helper')
	smart.info('log level info helper')
	checkLogEmittance(smart.LEVEL_INFO, 'helper')
	smart.info(smartNibbleScope, 'scoped log level info helper')
	checkLogEmittance(smartNibbleScope, smart.LEVEL_INFO, 'helper')
	smart.warn('log level warn helper')
	checkLogEmittance(smart.LEVEL_WARN, 'helper')
	smart.warn(smartNibbleScope, 'scoped log level warn helper')
	checkLogEmittance(smartNibbleScope, smart.LEVEL_WARN, 'helper')
	smart.error('log level error helper')
	checkLogEmittance(smart.LEVEL_ERROR, 'helper')
	smart.error(smartNibbleScope, 'scoped log level error helper')
	checkLogEmittance(smartNibbleScope, smart.LEVEL_ERROR, 'helper')
}
// ORIGIN REGION END tests
