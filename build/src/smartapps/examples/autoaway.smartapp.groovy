definition(
    name: "Auto away/home with night modes",
    namespace: "tslagle13",
    author: "Tim Slagle",
    description: "Monitor a set of presence sensors and change mode based on when your home is empty or occupied.  Included Night and Day modes for both an occupied and unoccupied house.",
    category: "Mode Magic",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience%402x.png"
)

preferences {
  page(name: "selectPhrases")

  page( name:"Settings", title:"Settings", uninstall:true, install:true ) {
  	section("False alarm threshold (defaults to 10 min)") {
    	input "falseAlarmThreshold", "decimal", title: "Number of minutes", required: false
  	}

  	section("Zip code (for sunrise/sunset)") {
   		input "zip", "decimal", required: true
  	}

      section("Notifications") {
        input "sendPushMessage", "enum", title: "Send a push notification?", metadata:[values:["Yes","No"]], required:false
  	}
    section(title: "More options", hidden: hideOptionsSection(), hideable: true) {

			def timeLabel = timeIntervalLabel()

			href "timeIntervalInput", title: "Only during a certain time", description: timeLabel ?: "Tap to set", state: timeLabel ? "complete" : null

			input "days", "enum", title: "Only on certain days of the week", multiple: true, required: false,
				options: ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"]

			input "modes", "mode", title: "Only when mode is", multiple: true, required: false
		}
  }
}

def selectPhrases() {
	def configured = (settings.awayDay && settings.awayNight && settings.homeDay && settings.awayDay)
    dynamicPage(name: "selectPhrases", title: "Configure", nextPage:"Settings", uninstall: true) {
		section("Who?") {
			input "people", "capability.presenceSensor", title: "Monitor These Presences", required: true, multiple: true,  refreshAfterSelection:true
		}

		def phrases = location.helloHome?.getPhrases()*.label
		if (phrases) {
        	phrases.sort()
			section("Run This Phrase When...") {
				log.trace phrases
				input "awayDay", "enum", title: "Everyone Is Away And It's Day", required: true, options: phrases,  refreshAfterSelection:true
				input "awayNight", "enum", title: "Everyone Is Away And It's Night", required: true, options: phrases,  refreshAfterSelection:true
                input "homeDay", "enum", title: "At Least One Person Is Home And It's Day", required: true, options: phrases,  refreshAfterSelection:true
                input "homeNight", "enum", title: "At Least One Person Is Home And It's Night", required: true, options: phrases,  refreshAfterSelection:true
			}
		}
    }
}

def installed() {
  init()
  initialize()
  subscribe(app)

}

def updated() {
  unsubscribe()
  initialize()

  init()
}

def init() {
  subscribe(people, "presence", presence)

  checkSun();
}

def uninstalled() {
unsubscribe()
}

def initialize() {
	schedule("0 0/5 * 1/1 * ? *", checkSun)
}

def checkSun() {
  def zip     = settings.zip as String
  def sunInfo = getSunriseAndSunset(zipCode: zip)
  def current = now()

  if(sunInfo.sunrise.time > current ||
     sunInfo.sunset.time  < current) {
    state.sunMode = "sunset"
  }

  else {
    state.sunMode = "sunrise"
  }

  log.info("Sunset: ${sunInfo.sunset.time}")
  log.info("Sunrise: ${sunInfo.sunrise.time}")
  log.info("Current: ${current}")
  log.info("sunMode: ${state.sunMode}")

  if(current < sunInfo.sunrise.time) {
    runIn(((sunInfo.sunrise.time - current) / 1000).toInteger(), setSunrise)
  }

  if(current < sunInfo.sunset.time) {
    runIn(((sunInfo.sunset.time - current) / 1000).toInteger(), setSunset)
  }
}

def setSunrise() {
  state.sunMode = "sunrise";
  changeSunMode(newMode)
}

def setSunset() {
  state.sunMode = "sunset";
  changeSunMode(newMode)
}


def changeSunMode(newMode) {
  if(allOk) {

  if(everyoneIsAway() && (state.sunMode = "sunrise")) {
    log.info("Home is Empty  Setting New Away Mode")
    def delay = (falseAlarmThreshold != null && falseAlarmThreshold != "") ? falseAlarmThreshold * 60 : 10 * 60
    runIn(delay, "setAway")
  }

  if(everyoneIsAway() && (state.sunMode = "sunset")) {
    log.info("Home is Empty  Setting New Away Mode")
    def delay = (falseAlarmThreshold != null && falseAlarmThreshold != "") ? falseAlarmThreshold * 60 : 10 * 60
    runIn(delay, "setAway")
  }

  else {
  log.info("Home is Occupied Setting New Home Mode")
  setHome()


  }
}
}

def presence(evt) {
  if(allOk) {
  if(evt.value == "not present") {
    log.debug("Checking if everyone is away")

    if(everyoneIsAway()) {
      log.info("Nobody is home, running away sequence")
      def delay = (falseAlarmThreshold != null && falseAlarmThreshold != "") ? falseAlarmThreshold * 60 : 10 * 60
      runIn(delay, "setAway")
    }
  }

else {
	def lastTime = state[evt.deviceId]
    if (lastTime == null || now() - lastTime >= 1 * 60000) {
  		log.info("Someone is home, running home sequence")
  		setHome()
    }
	state[evt.deviceId] = now()

  }
}
}

def setAway() {
  if(everyoneIsAway()) {
    if(state.sunMode == "sunset") {
      def message = "Performing \"${awayNight}\" for you as requested."
      log.info(message)
      send(message)
      location.helloHome.execute(settings.awayNight)
    }

    else if(state.sunMode == "sunrise") {
      def message = "Performing \"${awayDay}\" for you as requested."
      log.info(message)
      send(message)
      location.helloHome.execute(settings.awayDay)
      }
    else {
      log.debug("Mode is the same, not evaluating")
    }
  }

  else {
    log.info("Somebody returned home before we set to '${newAwayMode}'")
  }
}

def setHome() {

log.info("Setting Home Mode!!")
if(anyoneIsHome()) {
      if(state.sunMode == "sunset"){
      def message = "Performing \"${homeNight}\" for you as requested."
        log.info(message)
        location.helloHome.execute(settings.homeNight)
			send(message)
            sendSms(phone1, message)
       }

      if(state.sunMode == "sunrise"){
      def message = "Performing \"${homeDay}\" for you as requested."
        log.info(message)
        location.helloHome.execute(settings.homeDay)
			send(message)
            sendSms(phone1, message)
            }
    }

}

private everyoneIsAway() {
  def result = true

  if(people.findAll { it?.currentPresence == "present" }) {
    result = false
  }

  log.debug("everyoneIsAway: ${result}")

  return result
}

private anyoneIsHome() {
  def result = false

  if(people.findAll { it?.currentPresence == "present" }) {
    result = true
  }

  log.debug("anyoneIsHome: ${result}")

  return result
}

private send(msg) {
  if(sendPushMessage != "No") {
    log.debug("Sending push message")
    sendPush(msg)
  }

  log.debug(msg)
}



private getAllOk() {
	modeOk && daysOk && timeOk
}

private getModeOk() {
	def result = !modes || modes.contains(location.mode)
	log.trace "modeOk = $result"
	result
}

private getDaysOk() {
	def result = true
	if (days) {
		def df = new java.text.SimpleDateFormat("EEEE")
		if (location.timeZone) {
			df.setTimeZone(location.timeZone)
		}
		else {
			df.setTimeZone(TimeZone.getTimeZone("America/New_York"))
		}
		def day = df.format(new Date())
		result = days.contains(day)
	}
	log.trace "daysOk = $result"
	result
}

private getTimeOk() {
	def result = true
	if (starting && ending) {
		def currTime = now()
		def start = timeToday(starting).time
		def stop = timeToday(ending).time
		result = start < stop ? currTime >= start && currTime <= stop : currTime <= stop || currTime >= start
	}
	log.trace "timeOk = $result"
	result
}

private hhmm(time, fmt = "h:mm a")
{
	def t = timeToday(time, location.timeZone)
	def f = new java.text.SimpleDateFormat(fmt)
	f.setTimeZone(location.timeZone ?: timeZone(time))
	f.format(t)
}

private hideOptionsSection() {
	(starting || ending || days || modes) ? false : true
}

private timeIntervalLabel() {
	(starting && ending) ? hhmm(starting) + "-" + hhmm(ending, "h:mm a z") : ""
}
