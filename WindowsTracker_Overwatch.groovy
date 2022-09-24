/**
 *
 * Window Tracker Child
 *
 * Copyright 2022 Ryan Elliott
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 * v1.0		RLE		Creation
 */
 
definition(
    name: "Window Tracker Child",
    namespace: "rle.sg+",
    author: "Ryan Elliott",
    description: "Creates virtual devices to track groups of contact sensors.",
    category: "Convenience",
	parent: "rle.sg+:Window Tracker+",
	iconUrl: "",
    iconX2Url: "")

preferences {
    page(name: "prefContactGroup")
	page(name: "prefSettings")
}

def prefContactGroup() {
	return dynamicPage(name: "prefContactGroup", title: "Create a Contact Group", nextPage: "prefSettings", uninstall:true, install: false) {
		section {
            label title: "<b>***Enter a name for this child app.***</b><br>This will create a virtual contact sensor which reports the open/closed status based on the sensors you select.", required:true
		}
	}
}

def prefSettings() {
    return dynamicPage(name: "prefSettings", title: "", install: true, uninstall: true) {
		section {
			paragraph "Please choose which sensors to include in this group. The virtual device will provide the sum of total devices, total open, and total closed."+
            "<br>The selected sensors should only be the child devices created by the Window Tracker Child app instances."

			input "contactSensors", "capability.aggregate", title: "Select the sensors you would like to aggregate.", multiple:true, required:true

            input "debugOutput", "bool", title: "Enable debug logging?", defaultValue: true, displayDuringSetup: false, required: false
        }
	}
}

def installed() {
	initialize()
}

def uninstalled() {
	logDebug "uninstalling app"
	for (device in getChildDevices())
	{
		deleteChildDevice(device.deviceNetworkId)
	}
}

def updated() {	
    logDebug "Updated with settings: ${settings}"
	unschedule()
    unsubscribe()
	initialize()
}

def initialize() {
	subscribe(contactSensors, "contact", contactHandler)
	subscribe(doorSensors, "contact", doorHandler)
	createOrUpdateChildDevice()
    if (doorSensors) {
        doorHandler()
    } else {
        contactHandler()}
    def device = getChildDevice(state.contactDevice)
    device.sendEvent(name: "TotalCount", value: contactSensors.size())
	device.sendEvent(name: "OpenThreshold", value: activeThreshold)
    runIn(1800,logsOff)
}

def contactHandler(evt) {
    log.info "Checking status count..."
    getCurrentCount()
    def device = getChildDevice(state.contactDevice)
	if (state.totalOpen >= activeThreshold)
	{
		log.info "Open threshold met; setting virtual device as open"
		logDebug "Current threshold value is ${activeThreshold}"
		device.sendEvent(name: "contact", value: "open", descriptionText: "The open devices are ${state.openList}")
	} else {
		log.info "All closed; setting virtual device as closed"
		logDebug "Current threshold value is ${activeThreshold}"
		device.sendEvent(name: "contact", value: "closed")
	}
}

def createOrUpdateChildDevice() {
	def childDevice = getChildDevice("contactgroup:" + app.getId())
    if (!childDevice || state.contactDevice == null) {
        logDebug "Creating child device"
		state.contactDevice = "contactgroup:" + app.getId()
		addChildDevice("rle.sg+", "Window Tracker Device", "contactgroup:" + app.getId(), 1234, [name: app.label + "_device", isComponent: false])
    }
	else if (childDevice && childDevice.name != (app.label + "_device"))
		childDevice.name = app.label + "_device"
}

def logDebug(msg) {
    if (settings?.debugOutput) {
		log.debug msg
	}
}

def logsOff(){
    log.warn "debug logging disabled..."
    app.updateSetting("debugOutput",[value:"false",type:"bool"])
}

def getCurrentCount() {
	def device = getChildDevice(state.contactDevice)
	def totalOpen = 0
    def totalClosed = 0
	def openList = []
	contactSensors.each { it ->
		if (it.currentValue("contact") == "open")
		{
            totalOpen++
			if (it.label) {
            openList.add(it.label)
            }
            else if (!it.label) {
                openList.add(it.name)
            }
		}
		else if (it.currentValue("contact") == "closed")
		{
			totalClosed++
		}
    }
    state.totalOpen = totalOpen
	if (openList.size() == 0) {
        openList.add("None")
    }
	state.openList = openList.sort()
    logDebug "There are ${totalClosed} sensors closed"
    logDebug "There are ${totalOpen} sensors open"
    device.sendEvent(name: "TotalClosed", value: totalClosed)
    device.sendEvent(name: "TotalOpen", value: totalOpen)
	device.sendEvent(name: "OpenList", value: state.openList)
}