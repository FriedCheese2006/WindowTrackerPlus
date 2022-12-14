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
 * v1.1		RLE		Added door closed listing
 */
 
definition(
    name: "Window Tracker Child",
    namespace: "rle.sg+",
    author: "Ryan Elliott",
    description: "Creates virtual devices to track groups of contact sensors.",
    category: "Convenience",
	parent: "rle.sg+:Window Tracker",
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
			paragraph "Please choose which sensors to include in this group. The virtual device will report status based on the configured threshold."

			input "contactSensors", "capability.contactSensor", title: "Window contact sensors to monitor", multiple:true, required:true

            input "doorSensors", "capability.contactSensor", title: "Door contact sensors (optional)", multiple:true, required:false
        }
		section {
            paragraph "Set how many sensors are required to change the status of the virtual device."
            
            input "activeThreshold", "number", title: "How many sensors must be open before the group is open? Leave set to one if any contact sensor open should make the group open.", required:false, defaultValue: 1
            
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
	createOrUpdateChildDevice()
	def device = getChildDevice(state.contactDevice)
    if (doorSensors) {        
	    subscribe(doorSensors, "contact", doorHandler)
        doorHandler()
    } else {
        closedDoorList = groovy.json.JsonOutput.toJson("None")
        logDebug "No doors sensors selected"
		device.sendEvent(name: "ClosedDoorList", value: closedDoorList)
        contactHandler()}    
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

def doorHandler(evt) {
	def device = getChildDevice(state.contactDevice)
    def closedCount = 0
	def closedDoorList = []
    def openWindowList = []
	doorSensors.each { it ->
		if (it.currentValue("contact") == "closed") {
			closedCount++
			if (it.label) {
            closedDoorList.add(it.label)
            }
            else if (!it.label) {
                closedDoorList.add(it.name)
            }
		}
	}
	if (closedCount >= 1) {
		log.info "Door(s) closed; ignoring windows."
		unsubscribe(contactSensors, "contact", contactHandler)
		device.sendEvent(name: "TotalClosed", value: contactSensors.size())
    	device.sendEvent(name: "TotalOpen", value: "0")
        openWindowList.add("None")
        openWindowList = groovy.json.JsonOutput.toJson(openWindowList)
		device.sendEvent(name: "OpenWindowList", value: openWindowList)
        device.sendEvent(name: "contact", value: "closed")
        closedDoorList = closedDoorList.sort()
        closedDoorList = groovy.json.JsonOutput.toJson(closedDoorList)
		device.sendEvent(name: "ClosedDoorList", value: closedDoorList)
	} else {
		log.info "Door(s) open; checking windows..."
        closedDoorList.add("None")
        closedDoorList = groovy.json.JsonOutput.toJson(closedDoorList)
        device.sendEvent(name: "ClosedDoorList", value: closedDoorList)
		subscribe(contactSensors, "contact", contactHandler)
        contactHandler()
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
	def openWindowList = []
	contactSensors.each { it ->
		if (it.currentValue("contact") == "open")
		{
            totalOpen++
			if (it.label) {
            openWindowList.add(it.label)
            }
            else if (!it.label) {
                openWindowList.add(it.name)
            }
		}
		else if (it.currentValue("contact") == "closed")
		{
			totalClosed++
		}
    }
    state.totalOpen = totalOpen
	if (openWindowList.size() == 0) {
        openWindowList.add("None")
    }
	openWindowList = openWindowList.sort()
    openWindowList = groovy.json.JsonOutput.toJson(openWindowList)
    logDebug "There are ${totalClosed} sensors closed"
    logDebug "There are ${totalOpen} sensors open"
    device.sendEvent(name: "TotalClosed", value: totalClosed)
    device.sendEvent(name: "TotalOpen", value: totalOpen)
	device.sendEvent(name: "OpenWindowList", value: openWindowList)
}