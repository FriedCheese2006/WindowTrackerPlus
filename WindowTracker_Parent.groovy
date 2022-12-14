/**
 *
 * Window Tracker
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
    name: "Window Tracker",
    namespace: "rle.sg+",
    author: "Ryan Elliott",
    description: "Allow for the tracking of contact sensors in groups with the option to aggregate.",
    category: "Convenience",
	iconUrl: "",
    iconX2Url: "")

preferences {
     page(name: "mainPage", title: "", install: true, uninstall: true)
} 

def installed() {
    initialize()
}

def updated() {
    initialize()
}

def initialize() {
	// Do nothing for now
}

def mainPage() {
    dynamicPage(name: "mainPage") {
    	isInstalled()
		if(state.appInstalled == 'COMPLETE'){
			section("${app.label}") {
				paragraph "Provides options for combining multiple sensors into a single device to provide combined updates."
			}
			section("Child Apps") {
				app(name: "windowTrackerApp+", appName: "Window Tracker Child", namespace: "rle.sg+", title: "Add a new Window Tracker Instance", multiple: true)
				app(name: "overwatch", appName: "Window Tracker Aggregate", namespace: "rle.sg+", title: "Add a new Aggregate Instance", multiple: true)
			}
			section("General") {
       			label title: "Enter a name for this parent app (optional)", required: false
 			}
		}
	}
}

def isInstalled() {
	state.appInstalled = app.getInstallationState() 
	if (state.appInstalled != 'COMPLETE') {
		section
		{
			paragraph "Please click <b>Done</b> to install the parent app."
		}
  	}
}