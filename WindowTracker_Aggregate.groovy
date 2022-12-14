/**
 *
 * Window Tracker Aggregate
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
 * v1.1		RLE		Added aggregation of open windows and closed door lists from child apps.
 */

import groovy.json.*
 
definition(
    name: "Window Tracker Aggregate",
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
			paragraph "Please choose which sensors to include in this group. The virtual device will provide the sum of total devices, total open, and total closed."+
            "<br>The selected sensors should only be the child devices created by the Window Tracker Child app instances."

			input "contactSensors", "capability.contactSensor", title: "Select the sensors you would like to aggregate.", multiple:true, required:true

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
	subscribe(contactSensors, "TotalOpen", aggregateHandler)
	createOrUpdateChildDevice()
	aggregateHandler()
    def device = getChildDevice(state.contactDevice)
    runIn(1800,logsOff)
}

def aggregateHandler(evt) {
    log.info "Update received. Processing..."
    getCurrentAggregateCount()
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

def getCurrentAggregateCount() {
	def device = getChildDevice(state.contactDevice)
	def aggregateOpen = 0
    def aggregateClosed = 0
	def aggregateTotal = 0
	contactSensors.each { it ->
        totalOpen = it.currentValue("TotalOpen")
		aggregateOpen = (aggregateOpen + totalOpen)
        totalClosed = it.currentValue("TotalClosed")
		aggregateClosed = (aggregateClosed + totalClosed)
        totalCount = it.currentValue("TotalCount")
		aggregateTotal = (aggregateTotal + totalCount)
    }
	getLists()
    logDebug "There are ${aggregateClosed} sensors closed."
    logDebug "There are ${aggregateOpen} sensors open."
	logDebug "There are ${aggregateTotal} sensors in total."
    device.sendEvent(name: "AggregateTotalClosed", value: aggregateClosed)
    device.sendEvent(name: "AggregateTotalCount", value: aggregateTotal)
	device.sendEvent(name: "AggregateTotalOpen", value: aggregateOpen)
}

def getLists () {
	def device = getChildDevice(state.contactDevice)
	def aggregateOpenWindowsList = []
	def aggregateClosedDoorList = []
	contactSensors.each { it ->
        def openWindows = new groovy.json.JsonSlurper().parseText(it.currentValue("OpenWindowList"))
        if (!openWindows.contains("None")) {
			aggregateOpenWindowsList.addAll(openWindows)
		}
        def closedDoors = new groovy.json.JsonSlurper().parseText(it.currentValue("ClosedDoorList"))
        if (!closedDoors.contains("None")) {
			aggregateClosedDoorList.addAll(closedDoors)
		}
	}
    if (!aggregateOpenWindowsList) {
        aggregateOpenWindowsList = "None"
    }
    aggregateOpenWindowsList = groovy.json.JsonOutput.toJson(aggregateOpenWindowsList)
    if (!aggregateClosedDoorList) {
        aggregateClosedDoorList = "None"
    }
    aggregateClosedDoorList = groovy.json.JsonOutput.toJson(aggregateClosedDoorList)
	logDebug "These windows are open: ${aggregateOpenWindowsList}"
    logDebug "These doors are closed: ${aggregateClosedDoorList}"
	device.sendEvent(name: "AggregateOpenWindowList", value: aggregateOpenWindowsList)
    device.sendEvent(name: "AggregateClosedDoorList", value: aggregateClosedDoorList)
}