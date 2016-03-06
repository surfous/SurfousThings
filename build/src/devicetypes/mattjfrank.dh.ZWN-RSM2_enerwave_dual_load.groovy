/**
 *  ZWN-RSM2 Enerwave Dual Load ZWN-RSM2
 *
 *  Author Matt Frank based on the work of chrisb for AEON Power Strip
 *
 *  Date Created:  6/26/2014
 *  Last Modified: 1/11/2015
 *
 */
import groovy.transform.Field

@Field final Short ZWAVE_OFF = 0x00
@Field final Short ZWAVE_ON  = 0xFF

 // for the UI
metadata {
  definition (name: "ZWN-RSM2 Enerwave Dual Load", namespace: "mattjfrank", author: "Matt Frank") {
	capability "Switch"
	capability "Polling"
	capability "Configuration"
	capability "Refresh"

	attribute "switch1", "string"
	attribute "switch2", "string"


	command "switch1on"
	command "switch1off"
	command "switch2on"
	command "switch2off"

		fingerprint deviceId: "0x1001", inClusters:"0x25, 0x27, 0x60, 0x70, 0x72, 0x86"

  }

  simulator {
	// TODO: define status and reply messages here
  }

  tiles {

	standardTile("switch1", "device.switch1",canChangeIcon: true)
    {
		state "on", label: "switch1", action: "switch1off", icon: "st.switches.switch.on", backgroundColor: "#79b821"
		state "off", label: "switch1", action: "switch1on", icon: "st.switches.switch.off", backgroundColor: "#ffffff"
	}
	standardTile("switch2", "device.switch2",canChangeIcon: true) {
		state "on", label: "switch2", action: "switch2off", icon: "st.switches.switch.on", backgroundColor: "#79b821"
		state "off", label: "switch2", action: "switch2on", icon: "st.switches.switch.off", backgroundColor: "#ffffff"
	}
	standardTile("refresh", "device.switch", inactiveLabel: false, decoration: "flat") {
		state "default", label:"", action:"refresh.refresh", icon:"st.secondary.refresh"
	}

	standardTile("configure", "device.switch", inactiveLabel: false, decoration: "flat") {
		state "default", label:"", action:"configure", icon:"st.secondary.configure"
	}


	main(["switch1", "switch2"])
	details(["switch1","switch2","refresh"])
  }
}

// 0x25 0x32 0x27 0x70 0x85 0x72 0x86 0x60 0xEF 0x82

// 0x25: switch binary
// 0x32: meter
// 0x27: switch all
// 0x70: configuration
// 0x85: association
// 0x86: version
// 0x60: multi-channel
// 0xEF: mark
// 0x82: hail

// parse events into attributes
def parse(String description) {
  // log.debug "Parsing desc => '${description}'"

	def result = null
	def cmd = zwave.parse(description, [0x60:3, 0x25:1, 0x70:1, 0x72:1])
	if (cmd) {
		result = createEvent(zwaveEvent(cmd))
	}

	return result
}


//Reports

//def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicReport cmd) {
//		[name: "switch", value: cmd.value ? "on" : "off", type: "physical"]
//}
//
//def zwaveEvent(physicalgraph.zwave.commands.switchbinaryv1.SwitchBinaryReport cmd) {
//		[name: "switch", value: cmd.value ? "on" : "off", type: "digital"]
//}

// handle an unknown/unexpected Z-wave event sent from the device
def zwaveEvent(physicalgraph.zwave.Command deviceEvent)
{
	log.warn "Unhandled Z-wave event: $deviceEvent"
	return createEvent([name: 'unhandledEvent', value: deviceEvent, displayed: false, descriptionText: "$device.displayName: $deviceEvent"])
}

def zwaveEvent(physicalgraph.zwave.commands.multichannelv3.MultiChannelCmdEncap cmd)
{
    // log.debug "MultiChannelCmdEncap $cmd"
	def eventMap = [ name: "switch$cmd.sourceEndPoint", description: String.valueOf(cmd)]
	if (cmd.commandClass == 0x25)
    {
    	if (cmd.parameter == [ZWAVE_OFF])
        {
    		eventMap.value = "off"
    	}
		elseif (cmd.parameter == [ZWAVE_ON])
        {
			eventMap.value = "on"
		}
	}
    return createEvent(eventMap)
}


// handle commands

def refresh() {

   for ( i in 1..3 )
	  cmds << zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint:1, destinationEndPoint:i, commandClass:37, command:2).format()
	  cmds << zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint:2, destinationEndPoint:i, commandClass:37, command:2).format()



	delayBetween(cmds)
}


def poll() {
  delayBetween([
	zwave.switchBinaryV1.switchBinaryGet().format(),
	zwave.manufacturerSpecificV1.manufacturerSpecificGet().format()
  ])
}

def configure() {
  log.debug "Executing 'configure'"
	delayBetween([
		zwave.configurationV1.configurationSet(parameterNumber:4, configurationValue: [0]).format()				// Report reguarly
	])
}

def switch1on() {
  delayBetween([
	zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint:1, destinationEndPoint:1, commandClass:37, command:1, parameter:[255]).format(),
	zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint:1, destinationEndPoint:1, commandClass:37, command:2).format(),
	zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint:2, destinationEndPoint:2, commandClass:37, command:2).format(),
  ])
}

def switch1off() {
  delayBetween([
	zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint:1, destinationEndPoint:1, commandClass:37, command:1, parameter:[0]).format(),
	zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint:1, destinationEndPoint:1, commandClass:37, command:2).format(),
	zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint:2, destinationEndPoint:2, commandClass:37, command:2).format(),

	])
}

def switch2on() {
  delayBetween([
	zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint:2, destinationEndPoint:2, commandClass:37, command:1, parameter:[255]).format(),
	zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint:2, destinationEndPoint:2, commandClass:37, command:2).format(),
	zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint:1, destinationEndPoint:1, commandClass:37, command:2).format()
	])
}

def switch2off() {
  delayBetween([
	zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint:2, destinationEndPoint:2, commandClass:37, command:1, parameter:[0]).format(),
	zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint:2, destinationEndPoint:2, commandClass:37, command:2).format(),
	zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint:1, destinationEndPoint:1, commandClass:37, command:2).format()
  ])
}
