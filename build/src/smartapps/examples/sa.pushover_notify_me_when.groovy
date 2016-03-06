/**
*  Pushover Notify Me When
*
*  Added Pushover Notifications to original 'Notify Me When' SmartThings App using
*  the Pushover REST interface.
*
*  Author: jim@concipio-solutions.com (original SmartApp written by SmartThings)
*  Date: 2013-07-20 (original SmartApp written on 2013-03-20)
*
*	This software is released under the Apache V2.0 open source license.
*
*  Copyright (C) 2013-2014 Concipio Technical Solutions, LLC <info@concipio-solutions.com>
*
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
* INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
* PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
* HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
* CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE
* OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*
* Change Log:
* 2015-11-08 - Added token substitution to message text for any event map key
*/
import groovy.transform.Field

@Field final String PUSHOVER_API_ENDPOINT_URL = 'https://api.pushover.net/1/messages.json'
@Field final String DEFAULT_APP_NAME = 'Pushover, Notify Me When (w/ substitutions)'
definition(
	name: DEFAULT_APP_NAME,
	namespace: "surfous",
	author: "Kevin Shuk",
	description: "Receive notifications when anything happens in your home. ",
	category: "Convenience",
	iconUrl: "https://sites.google.com/a/surfous.com/external-assets/st/pushover_icon.png",
	iconX2Url: "https://sites.google.com/a/surfous.com/external-assets/st/pushover_icon@2x.png",
	iconX3Url: "https://sites.google.com/a/surfous.com/external-assets/st/pushover_icon@3x.png"
)

preferences
{
	page(name: "prefsPage")
	page(name: "subHelpPage")
}

def prefsPage()
{
	dynamicPage(name: "prefsPage", title: "", install: true, uninstall: true)
	{
		section()
		{
			label(	title: "Name this Notifier",
					description: DEFAULT_APP_NAME,
					required: false )
		}
		section("Choose one or more; When...")
		{
			input(name: "motion", type: "capability.motionSensor", title: "Motion here", required: false, multiple: true)
			input(name: "contact", type: "capability.contactSensor", title: "Contact opens", required: false, multiple: true)
			input(name: "acceleration", type: "capability.accelerationSensor", title: "Acceleration detected", required: false, multiple: true)
			input(name: "mySwitch", type: "capability.switch", title: "Switch Turned On", required: false, multiple: true)
			input(name: "arrivalPresence", type: "capability.presenceSensor", title: "Arrival Of", required: false, multiple: true)
			input(name: "departurePresence", type: "capability.presenceSensor", title: "Departure Of", required: false, multiple: true)
		}

		section("Message to send")
		{
			href(	name: "subHelp",
					title: "Message to Send:",
					description: "Enter the message to send for detected events. Tap here for information on using substitutions in your message",
					required: false,
					page: "subHelpPage"	)
			input(name: "messageTemplate", type: "text", title: "Message Text",
				capitalization: "sentences", required: true,
				description: "message to send")
		}

		section("Notifications") {}

		section()
		{
			input(name: "doSendPush", type: "bool", title: "SmartThings push")
		}

		section()
		{
			input(name: "doSendSms", type: "bool", title: "Text message", submitOnChange: true)
		}

		if (doSendSms)
		{
			section()
			{
				input(name: "smsPhone", type: "phone", title: "Phone number for SMS", required: false)
			}
		}

		section()
		{
			input(name: "doSendPushover", type: "bool", title: "Pushover", submitOnChange: true)
		}

		if (doSendPushover)
		{
			section()
			{
				input(name: "pushoverApiKey", type: "text", title: "API key", capitalization: "none", required: true)
				input(name: "pushoverUserKey", type: "text", title: "user key", capitalization: "none", required: true)
				input(name: "pushoverDeviceName", type: "text", title: "device name", capitalization: "none",
					description: "Leave blank to send to all your Pushover devices", required: false)
				input(name: "pushoverPriority", type: "enum", title: "priority", required: true, defaultValue: "0",
					options: [ '-1': 'Low', '0': 'Normal', '1': 'High', '2': 'Emergency' ]
				)
				input(name: "pushoverSound", type: "enum", title: "alert sound", required: true, defaultValue: "pushover",
					options: [
						'pushover': 'Pushover (default)',
						'bike': 'Bike',
						'bugle': 'Bugle',
						'cashregister': 'Cash Register',
						'classical': 'Classical',
						'cosmic': 'Cosmic',
						'falling': 'Falling',
						'gamelan': 'Gamelan',
						'incoming': 'Incoming',
						'intermission': 'Intermission',
						'magic': 'Magic',
						'mechanical': 'Mechanical',
						'pianobar': 'Piano Bar',
						'siren': 'Siren',
						'spacealarm': 'Space Alarm',
						'tugboat': 'Tug Boat',
						'alien': 'Alien Alarm (long)',
						'climb': 'Climb (long)',
						'persistent': 'Persistent (long)',
						'echo': 'Pushover Echo (long)',
						'updown': 'Up Down (long)',
						'none': 'None (silent)'
					]
				)
			}
		}
	}
}

def subHelpPage()
{
	page(name: "Using Substitutions", title: "Use the following tokens to substitute info from the event", nextPage: "prefsPage")
	{
		paragraph '''Tokens are enclosed in curly craces and begin with an exclamation point like this:
		{!token}

		Tokens will be substituted with the corresponding information from the event. Available tokens include those listed below.

		{!displayName}: The user-friendly name of the source of the event. Typically the user-assigned device label.
		{!descriptionText}: The description of this event to be displayed to the user in the mobile application.
		{!description}: The raw description that generated this event.
		{!deviceId}: The unique system identifier of the device assocaited with this Event, or null if there is no device associated with this Event
		{!hubId}: The unique system identifier of the Hub associated with this Event, or null if no Hub is associated with this Event.
		{!installedSmartAppId}: The unique system identifier of the SmartApp instance associated with this Event.
		{!isoDate}: Acquisition time of this Event as an ISO-8601 String
		{!location}: The Location associated with this Event, or null if no Location is associated with this Event.
		{!name}: The name of this event.
		{!value}: The value of this Event as a String
		{!unit}: the unit of measure of this Event, if applicable. null otherwise.
		{!source}: The source of the Event
		'''
	}
}


def installed()
{
	log.debug "Installed with settings: ${settings}"
	initialize()
}

def updated()
{
	log.debug "Updated with settings: ${settings}"
	unsubscribe()
	initialize()
}

def initialize()
{
	subscribeToEvents()
	// Custom states
	state.priorityMap = ["Low":-1,"Normal":0,"High":1,"Emergency":2];
}

def subscribeToEvents() {
	subscribe(contact, "contact.open", sendMessage)
	subscribe(acceleration, "acceleration.active", sendMessage)
	subscribe(motion, "motion.active", sendMessage)
	subscribe(mySwitch, "switch.on", sendMessage)
	subscribe(arrivalPresence, "presence.present", sendMessage)
	subscribe(departurePresence, "presence.not present", sendMessage)
}

String formatMessageWithSubstitutions(evt, String messageTemplate)
{
	String messageText = messageTemplate.replaceAll("(\\{!(\\w+)\\})", {
		match, token, tokenName ->
		log.debug "tokenName: $tokenName"
		String substitution = subFromEvent(evt, tokenName)
		log.debug "substitution $substitution"
		return substitution
	})
	log.debug "Template: $messageTempate"
	log.debug "Text: $messageText"
	return messageText
}

String subFromEvent(evt, String tokenName)
{
	String substitution = 'NULL'
	try
	{
		substitution=String.valueOf(evt."${tokenName}")
	}
	catch (e)
	{
		substitution = 'ERROR'
	}
	return substitution
}

private defaultText(evt)
{
	if (evt.name == "presence")
	{
		if (evt.value == "present")
		{
			if (includeArticle)
			{
				"$evt.linkText has arrived at the $location.name"
			}
			else
			{
				"$evt.linkText has arrived at $location.name"
			}
		}
		else
		{
			if (includeArticle)
			{
				"$evt.linkText has left the $location.name"
			}
			else
			{
				"$evt.linkText has left $location.name"
			}
		}
	}
	else
	{
		evt.descriptionText
	}
}

private getIncludeArticle()
{
	def name = location.name.toLowerCase()
	def segs = name.split(" ")
	!(["work","home"].contains(name) || (segs.size() > 1 && (["the","my","a","an"].contains(segs[0]) || segs[0].endsWith("'s"))))
}

def sendMessage(evt)
{
	String messageText=formatMessageWithSubstitutions(evt, messageTemplate)
	log.debug "$evt.name: $evt.value, $messageTemplate, $messageText"

	if (doSendPush)
	{
		log.debug "Sending SmartThings notification"
		sendPush(messageText)
	}
	else
	{
		log.debug "Skipping SmartThings Notification"
	}

	if (doSendSms)
	{
		log.debug "Sending SMS message to [$phone]"
		sendSms(phone, messageText)
	}
	else
	{
		log.debug "Skipping SMS message"
	}

	if (doSendPushover)
	{
		log.debug "Sending Pushover with API Key [$pushoverApiKey] and User Key [$pushoverUserKey]"

		log.debug "priority = $pushoverPriority"
		log.debug "sound = $pushoverSound"

		def postBody = [token: "$pushoverApiKey", user: "$pushoverUserKey", message: "$messageText", priority: "$pushoverPriority"]
		postBody.sound = pushoverSound

		// All devices, or just one?
		def deviceText = "all devices"
		if (pushoverDeviceName)
		{
			deviceText = "device '$pushoverDeviceName'"
			postBody.device = pushoverDeviceName
		}

		// in an emergency
		if (pushoverPriority == '2')
		{
			postBody.retry = "60"
			postBody.expire = "3600"
		}

		log.debug "Sending Pushover notification $messageText to $deviceText with priority $pushoverPriority using alert sound $pushoverSound"
		log.debug postBody

		def params = [
			uri: PUSHOVER_API_ENDPOINT_URL,
			body: postBody
		]

		try
		{
			httpPost(params) {
				response ->
				log.debug "Response Received: Status [$response.status]"
				if (response.status != 200)
				{
					// I doubt this will happen due to the nature of httpPost
					sendPush("Received HTTP Error Response. Check Install Parameters.")
				}
			}
		}
		catch (e)
		{
			log.error "Exception received posting to Pushover: $e"
		}

	}
	else
	{
		log.debug "Skipping Pushover Notification"
	}
}
