/**
 *
 * Window Tracker Device
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
 * v1.1		RLE		Added door listing
 * v1.2		RLE		Added aggregate attributes
 */

metadata {
	definition(
    name: "Window Tracker Device",
    namespace: "rle.sg+",
    author: "Ryan Elliott") 
		{
		capability "ContactSensor"
			
		attribute "contact", "enum", ["closed", "open"]
		attribute "TotalCount", "number"
		attribute "TotalOpen", "number"
		attribute "TotalClosed", "number"
		attribute "OpenWindowList", "json"
		attribute "ClosedDoorList", "json"
		attribute "OpenThreshold", "number"
		attribute "AggregateTotalCount", "number"
		attribute "AggregateTotalOpen", "number"
		attribute "AggregateTotalClosed", "number"
        attribute "AggregateOpenWindowList", "json"
        attribute "AggregateClosedDoorList", "json"
	}
}

def installed() {
	log.warn "installed..."
}

def updated() {
	log.info "updated..."
}