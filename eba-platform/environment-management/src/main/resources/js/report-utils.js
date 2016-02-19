// Couple of base64 encoded images so the report can be made portable as a single file
var collapseImg = 'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAMAAAAoLQ9TAAAAAXNSR0IArs4c6QAAAGBQTFRFf6jXNGWkXIO2fabXg6rYhq3ZkLPakrTalLbblbfbn77gtMzltczmtszmts3mt87mu9HnvNHnwNPo////AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAi9M+jgAAAAF0Uk5TAEDm2GYAAAABYktHRACIBR1IAAAACXBIWXMAAAsTAAALEwEAmpwYAAAAB3RJTUUH2gwRFjQZlc7M5wAAAC1JREFUGNNjYKATYGKEAyawAKOQgCA/CPDwM0IE+Dk42NlYWZiZuRixa6EDAACLJAD/O9SDnwAAAABJRU5ErkJggg=='
var expandImg ='data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAMAAAAoLQ9TAAAAAXNSR0IArs4c6QAAAGBQTFRFf6jXNGWkXIO2fabXg6rYhq3ZkLPakrTalLbblbfbn77gtMzltczmtszmts3mt87mu9HnvNHnwNPo////AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAi9M+jgAAAAF0Uk5TAEDm2GYAAAABYktHRACIBR1IAAAACXBIWXMAAAsTAAALEwEAmpwYAAAAB3RJTUUH2gwRFjQluqGwYAAAAEhJREFUGNNjYCASMDEyMqEIMPLzM5IgANTPCBJghJnDKCQgyA8CPFBVjPwcHOxsrCzMzFyMCC2srAgtEFWs6LawEnAHhtPxAAAGFwHbcrcKmwAAAABJRU5ErkJggg=='

window.addListeners = function(paddingOffset) {
	var imgs = document.querySelectorAll('.expando>a>img')
	for (var i = 0 ; i < imgs.length ; i++) {
		imgs[i].src = expandImg
		
		imgs[i].addEventListener('click', function(event) {
			var src = event.target
			var row = src.parentNode.parentNode.parentNode
			var nextRow = row.nextElementSibling
			var contentDiv = nextRow.querySelector('td>.cutoffable')
			expandOrContract(event.currentTarget, contentDiv, paddingOffset)
		})
	}
}

window.setUpAutoRefresh = function() {
	// Click event handling 
	var autoRefreshElem = document.getElementById('auto_refresh_container')
	if (autoRefreshElem) {
		autoRefreshElem.style.display='block'
		// Handle URL param indicating auto-refresh
		var autoRefreshValue= getQueryParam('autorefresh')
		if (autoRefreshValue == "" || autoRefreshValue == 'true') {
			autoRefreshValue = 30 //seconds
		}
		
		if (isInt(autoRefreshValue)) {
			// Or use the supplied period if there is one
			setInterval(function() {
				document.location.reload(true)
			}, parseInt(autoRefreshValue) * 1000) //milliseconds
		} else if (autoRefreshValue == "false") {
			updateautoRefreshLink(true) // switch from the default of 'disable' to 'enable'
		}
	
    	autoRefreshElem.addEventListener('click', function(event) {
        	if (autoRefreshElem.textContent.search('enable') > -1) {
        		updateUrl(true)
        		updateautoRefreshLink(false)
        	} else {
        		updateUrl(false)
        		updateautoRefreshLink(true)
        	}
        	event.preventDefault()
        })
    }
}

function updateautoRefreshLink(enabled) {
	var autoRefreshLink=document.querySelector('#auto_refresh_container a');
	if (enabled) {
		autoRefreshLink.innerHTML = autoRefreshLink.textContent.replace('disable', 'enable')
	} else {
		autoRefreshLink.innerHTML = autoRefreshLink.textContent.replace('enable', 'disable')
	}
}

function updateUrl(enabled) {
	if (window.location.href.search('autorefresh') > -1) {
		window.location.href = window.location.href.replace(/(.*autorefresh\=)[^&]*(.*$)/g, "$1" + enabled + "$2")
	} else {
		var addQueryChar = window.location.href.indexOf("?") == -1
		window.location.href = window.location.href + (addQueryChar? "?": "") + "&autorefresh=" + (enabled ? 'true' : 'false')
	}
							 
}

/**
 * Lovingly stolen from SO.
 * @param key
 * @returns The query param value associated with the provided key
 */
function getQueryParam(key) {
	var a = window.location.search.substr(1).split('&');
    if (a == "") return "";
    for (var i = 0; i < a.length; ++i) {
        var p=a[i].split('=', 2);
        if (p.length == 1 && p[0] == key)
            return ""
        else if (p[0] == key)
            return decodeURIComponent(p[1].replace(/\+/g, " "));
    }
    return "";
}

function isInt(value) {
	  var x;
	  if (isNaN(value)) {
	    return false;
	  }
	  x = parseFloat(value);
	  return (x | 0) === x;
	}

function expandOrContract(img, contentDiv, paddingOffset) {
	if (contentDiv) {
		var contentTable = contentDiv.querySelector('table')
		var desiredHeight = 0
			
		if (contentDiv.offsetHeight == 0) {
			desiredHeight = contentTable.offsetHeight + paddingOffset
			if (img) img.src= collapseImg
		} else {
			if (img) img.src= expandImg
		}
	
		if (typeof Prototype != 'undefined') {
			// Swanky animation since we've got prototype here to help
			var anim = new YAHOO.util.Anim(contentDiv, {
				height: { to: desiredHeight }
			}, 0.5, YAHOO.util.Easing.easeBoth)
			
			anim.animate()
		} else {
			// Morribund animation since we've just got plain old JS
			contentDiv.style.height = desiredHeight + "px"
		}
	}
}


function scrollToPlan() {
	document.getElementById('plan_anchor').scrollIntoView();
}

window.populateTemplate = function(data) {
    populateDeploymentDetails(data)
	populateDeployments(data)
	populatePhases(data)
	populateDependencies(data)
	populateDeploymentDescriptors(data)
	populateFailFiles(data)
	
	log("DOM updated")
	
	var elems = document.querySelectorAll('.icon-expando')
	
	for (var i = 0; i < elems.length ; i++) {
		elems[i].src = window.imageRoot + "/expand.png";
	}
}

function populateDeploymentDetails(data){
	if (!data.details) return;
	
	var dt = document.querySelector('#deployment_details_template')
	dt.content.querySelector('#environment').textContent = data.details.environment;
	dt.content.querySelector('#releaseVersion').textContent = data.details.releaseVersion;
	//TODO: Fill in more details
	setStatus(data.details.status, dt);
	if (data.details.status == "ERRORED") {
		dt.content.querySelector('#statusMessage').style.display= 'block';
	}
	if (data.details.started) {
		dt.content.querySelector('#started .text').textContent = data.details.started;
	} else {
		dt.content.querySelector('#started .text').textContent = '';
	}
	if (data.details.ended) {
		dt.content.querySelector('#finished .text').textContent = data.details.finished;
	} else {
		dt.content.querySelector('#finished .text').textContent = '';
	}
	
	
	var deplNode = document.importNode(dt.content, true)
	document.querySelector('#deployment_details').appendChild(deplNode)
	
	if (data.details.started) {
		//Actually, we only show this section if we're doing a deployment
		enableSection('deployment_details_container')
	}
}

function populateDeployments (data, paddingOffset) {
	if (data.deployments && data.deployments.length > 0) {
		enableSection('application_table_container')
		
		var st = document.querySelector('#deployments_summary_template')
		var ct = document.querySelector('#deployments_content_template')
		for (var i=0; i<data.deployments.length ; i++) {
			// Create deployment (top-level) rows first
			var depl = data.deployments[i]
			st.content.querySelector('td.col1').textContent = depl.name
			st.content.querySelector('td.col2').textContent = depl.version
			st.content.querySelector('td.col3').innerHTML = generateChangeSummary(depl)
			
			// Clear out the template from the last run
			removeChildren(ct.content.querySelector('tbody'));
			
			// Now create the component breakdowns
			var displayDeployedVersionColumn = false
			if (depl.components) {
				for (var j=0; j<depl.components.length ; j++) {
					displayDeployedVersionColumn += appendComponent(depl.components[j], ct.content.querySelector('tbody'));
				}
			}
	
			if (displayDeployedVersionColumn) {
				var elems = document.getElementsByClassName('deployedVersion')
				for (var i = 0 ; i < elems.length ; i++) {
					var elem = elems[i]
					elem.style.display='block'
				}
			}
			var contentNode = document.importNode(ct.content, true)
			var deplNode = document.importNode(st.content, true)
			document.querySelector('#application_table tbody').appendChild(deplNode)
			document.querySelector('#application_table tbody').appendChild(contentNode)
		}
	}
}

function enableSection(id) {
	var section = document.getElementById(id)
	if (section) {
		section.style.display = 'block'
	}
}

function generateChangeSummary(depl) {
	var changes = []
	if (depl.deploy) {
		changes.push(" Deploys: " + depl.deploy)
	}
	if (depl.undeploy) {
		changes.push(" Undeploys: " + depl.undeploy)
	}
	if (depl.upgrade) {
		changes.push(" Upgrades: " + depl.upgrade)
	}
	if (depl.downgrade) {
		changes.push(" Downgrades: " + depl.downgrade)
	}
	if (depl.fix) {
		changes.push(" Fixes: " + depl.fix)
	}
	if (depl.fail) {
		changes.push(" <span class='error'>Failures: " + depl.fail + "</span>")
	}
	
	if (changes.length == 0) {
		return "(no changes)"
	}
	return changes.join()
}

function populatePhases(data) {
	var foundCurrent = false;
	if (data.phases) {
		enableSection('phases_table_container')
		var st = document.querySelector('#phases_summary_template')
		for (var i=0; i<data.phases.length ; i++) {
			// Create phase (top-level) rows first
			var ct = document.querySelector('#phases_content_template')
			var phase = data.phases[i]
			setStatus(phase.status, st)
			st.content.querySelector('td.col1').textContent = i + 1
			st.content.querySelector('td.col2').textContent = phase.applications
			
			// Clear out the template from the last run
			removeChildren(ct.content.querySelector('tbody'));
	
			// Now create the transitions
			var complete = true; // whether all the transitions that make up this phase have completed
			if (phase.transitions) {
				for (var j=0; j<phase.transitions.length ; j++) {
					var transition = phase.transitions[j];
					appendTransition(transition, ct.content.querySelector('.transitions_table > tbody'), j);
					if (transition.status != 'COMPLETED') {
						complete = false;
					}
				}
			}
	
			var contentNode = document.importNode(ct.content, true)
			var phaseNode = document.importNode(st.content, true)
			document.querySelector('#phases_table tbody').appendChild(phaseNode)
			document.querySelector('#phases_table tbody').appendChild(contentNode)

			
			if (data.autoExpand && !complete && !foundCurrent) {
				// we've found the first incomplete phase - open it up
				foundCurrent = true;
				// get the css padding offset from the first transitions table if one exists
				var paddingOffset;
				if (document.getElementsByClassName('transitions_table')[0]) {
					var paddingOffset = document.getElementsByClassName('transitions_table')[0].offsetTop
				} else {
					var paddingOffset = 30; // otherwise a sensible guess
				}
				var contentToExpand = document.querySelector('#phases_table tbody tr:last-child .cutoffable');
				if (contentToExpand) {
					expandOrContract(null, contentToExpand, paddingOffset)
				}
				var previousToExpand = document.querySelector('#phases_table tbody tr:nth-last-child(3) .cutoffable');
				if (previousToExpand) {
					expandOrContract(null, previousToExpand, paddingOffset)
				}
			}
		}
	}
}

function populateDependencies(data) {
	if (data.dependency_chains && data.dependency_chains.length > 0) {
		enableSection('dependencies_container')
		var upperList = document.createElement('ul')
		
		for (var i = 0 ; i < data.dependency_chains.length; i++) {
			var listItem = document.createElement('li')
			listItem.textContent = data.dependency_chains[i].componentId
			addChildren(listItem, data.dependency_chains[i])
			upperList.appendChild(listItem)
		}
		
		
		document.getElementById('dependencies_container').appendChild(upperList)
	}
}

function populateDeploymentDescriptors(data) {
	if (data.descriptors && (data.descriptors.releaseDescriptor || data.descriptors.appDescriptors)) {
		enableSection('descriptors_container')
	}
	if (data.descriptors && data.descriptors.releaseDescriptor) {
		var releaseDDLink = createAnchorNode(data.descriptors.releaseDescriptor, 'here')
		var p = document.createElement('p')
		p.innerHTML = 'The release deployment descriptor can be found ' + releaseDDLink.outerHTML
		document.querySelector('#descriptors_container').insertBefore(p, document.getElementById('releaseDDHeader').nextSibling)
	}
	if (data.descriptors && data.descriptors.appDescriptors) {
		var appDDList = document.querySelector('#descriptors_container ul')
		for (var i = 0 ; i < data.descriptors.appDescriptors.length; i++) {
			var entry = data.descriptors.appDescriptors[i]
			var ddRow = document.createElement('li')
			var link = createAnchorNode(entry.fileURL, entry.appName + ' [' + entry.shortName + ']')
			ddRow.appendChild(link);
			appDDList.appendChild(ddRow)
		}
	}
}

function createAnchorNode(url, text) {
	var link = document.createElement('a')
	link.className='ddlink'
	link.setAttribute('href', url)
	link.setAttribute('target', '_blank')
	link.textContent = text
	return link
}

function populateFailFiles(data) {
	if (data.failFiles) {
		var ffs = document.querySelector('#ffs_template')
		var contentNode = document.importNode(ffs.content, true)
		for (var i = 0 ; i < data.failFiles.length; i++) {
			var entry = data.failFiles[i]
			var ffr = document.querySelector('#fail_file_row')
			ffr.content.querySelector('td.col1').textContent=entry.host 
			ffr.content.querySelector('td.col2').textContent=entry.status
			
			contentNode.querySelector('tbody').appendChild(document.importNode(ffr.content, true))
		}
		document.querySelector('#ffs_container').appendChild(contentNode)
	} else {
		document.getElementById('ff_error').style.display="block";
	}
}

function addChildren(parentNode, data) {
	var upperList = document.createElement('ul')
	for (var i = 0 ; i < data.children.length; i++) {
			var listItem = document.createElement('li')
			listItem.textContent = data.children[i].componentId + " " + data.children[i].maxDepth 
			addChildren(listItem, data.children[i])
			upperList.appendChild(listItem)
		}
	parentNode.appendChild(upperList)
}

function appendComponent(comp, container) {
	var displayDeployedVersionColumn = false
	var dcdt = document.querySelector('#component_data_template')
	dcdt.content.querySelector('td.col1').textContent = comp.name
	dcdt.content.querySelector('td.col2').textContent = comp.minPlan
	dcdt.content.querySelector('td.col3').textContent = comp.target
	dcdt.content.querySelector('td.col4').textContent = comp.existing
	dcdt.content.querySelector('td.col5').textContent = comp.maxDepth
	
	if (comp.deployedVersion) {
		dcdt.content.querySelector('td.deployedVersion').textContent = comp.deployedVersion
		displayDeployedVersionColumn = true
	}
	
	var actionsNode = document.createElement('ul');
	appendActions(comp.actions, actionsNode)

	dcdt.content.querySelector('td.col6').innerHTML = actionsNode.outerHTML

	var clone = document.importNode(dcdt.content, true)
	container.appendChild(clone)
	
	return displayDeployedVersionColumn
}

function appendTransition(transition, container, index) {
	if (transition.stopAfter) {
		// Stop Transition
		var wdt = document.querySelector('#stop_data_template')
		setStatus(transition.status, wdt)
		wdt.content.querySelector('td.col1').textContent = i == 0 ? index : ""
		wdt.content.querySelector('td.col2').textContent = "STOP" + (transition.stopMessage ? ": " : "");
		if (transition.stopMessage) {
			wdt.content.querySelector('td.col3').textContent = transition.stopMessage
		}
		var clone = document.importNode(wdt.content, true)
		container.appendChild(clone)
	} else if (transition.waitInterval) {
		// Wait Transition
		var wdt = document.querySelector('#wait_data_template')
		setStatus(transition.status, wdt)
		wdt.content.querySelector('td.col1').textContent = i == 0 ? index : ""
		wdt.content.querySelector('td.col2').textContent = "WAIT: " + transition.waitInterval + "s"
		var clone = document.importNode(wdt.content, true)
		container.appendChild(clone)
	} else {
		// Normal Hiera-updates transition
		for (var i = 0 ; i < transition.updates.length; i++ ) {
			var tcdt = document.querySelector('#transitions_content_data_template')
			var update = transition.updates[i]
			if (i == 0) {
				setStatus(transition.status, tcdt)
				tcdt.content.querySelector('td.col1').textContent = index
			} else {
				setStatus('', tcdt)
				tcdt.content.querySelector('td.col1').textContent = ''
			}
			tcdt.content.querySelector('td.col2').textContent = update.application
			tcdt.content.querySelector('td.col3').textContent = update.existingPath
			tcdt.content.querySelector('td.col4').textContent = update.existingValue
			tcdt.content.querySelector('td.col5').textContent = update.requestedPath
			tcdt.content.querySelector('td.col6').textContent = update.requestedValue
			tcdt.content.querySelector('td.col7').textContent = update.pathsAdded
			tcdt.content.querySelector('td.col8').textContent = update.pathsRemoved
			tcdt.content.querySelector('td.col9').textContent = update.hieraFile
			
			var clone = document.importNode(tcdt.content, true)
			container.appendChild(clone)
		}
		
		if (transition.statusMessage) {
			var tsdt = document.querySelector('#transitions_status_data_template')
			tsdt.content.querySelector('.message').textContent = transition.statusMessage
			
			if (transition.exception) {
				tsdt.content.querySelector('.contentRow .cutoffable .exception td').textContent = transition.exception
				tsdt.content.querySelector('.contentRow .cutoffable .stackTrace td').textContent = transition.stackTrace
				var logText = '';
				if (transition.log) {
					for (var l = 0; l < transition.log.length; l++) {
						logText += transition.log[l] + '<br />';
					}
				} 	
				tsdt.content.querySelector('.contentRow .cutoffable .log td').innerHTML = logText
			}
			
			var clone = document.importNode(tsdt.content, true)
			container.appendChild(clone)
			
		}
		
		var contentDiv = container.querySelector('.cutoffable')
		expandOrContract(null, contentDiv, 0);
	}
	// Then arbitrary commands
	for (var i = 0 ; i < transition.commands.length; i++ ) {
		var cdt = document.querySelector('#command_data_template')
		var command = transition.commands[i]

		setStatus(transition.status, cdt)
		cdt.content.querySelector('td.col1').textContent = i == 0 ? index : ""
		cdt.content.querySelector('td.col2').textContent = command.application
		cdt.content.querySelector('td.col3').textContent = "Command: " + command.command
		cdt.content.querySelector('td.col4').textContent = "Hosts: " + command.hosts
		var clone = document.importNode(cdt.content, true)
		container.appendChild(clone)
	}
	
	container.lastElementChild.className="last"
}

/*
 * Accepts a status (can be undefined) and sets the status column within the supplied template
 * root as appropriate.
 */
function setStatus(status, root) {
	var statusHolder = root.content.querySelector('td.progress_indicator');
	statusHolder.innerHTML = ""
	if (status == 'STARTED') {
		statusHolder.innerHTML="<div class=\"spinner\" ></div>";
	} else if (status == 'COMPLETED') {
		statusHolder.innerHTML="&#x2713;"
	} else if (status == 'ERRORED') {
		statusHolder.innerHTML="<div class=\"error\">&#x2717;</div>";
	}
}

function appendActions(actions, container) {
	if (typeof actions != 'undefined') {
		for (var i=0 ; i<actions.length; i++) {
			var action = actions[i]
			var actionNode = document.createElement('li')
			var subActionNodes = document.createElement('ul');
			if (action.secondaryChanges) {
				for (var j=0 ; j<action.secondaryChanges.length; j++) {
					var subAction = action.secondaryChanges[j]
					var subActionNode = document.createElement('li')
					subActionNode.textContent = subAction.change
					subActionNodes.appendChild(subActionNode)
				}
			}
			actionNode.innerHTML = action.change + subActionNodes.outerHTML
			container.appendChild(actionNode)
		}
	}
}

window.removeChildren = function(node) {
	while (node.firstChild) {
	    node.removeChild(node.firstChild);
	}
}

function log(msg, logElemId) {
	if (console && console.log) {
		console.log(msg)
	}
	if (logElemId) {
		var elem = document.getElementById(logElemId);
   		elem.innerHTML = elem.innerHTML + '<br/>' + msg;
	}
}
