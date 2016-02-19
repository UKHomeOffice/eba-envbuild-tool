/**
 * Functions used by the Environment Build Jenkins plugin.
 * 
 * @author David Manning
 */

var envBuildRegex = /^(.*)\|(.*)$/;

/**
 * Init.
 */
window.addEventListener("load", function() {
	disableGenerateButton();
	
	// Remove the width from the table otherwise the form elements move when the table resizes
	$$('.parameters')[0].removeAttribute("width");
	
	hideBuildButton();
	
	setupProviderListener();
	
	setupLogButton();
	
	changeStyle('table.parameters > tbody:hover');	
}, false);

function hideBuildButton() {
    document.getElementById('yui-gen1').style.visibility = 'hidden';
}

function showBuildButton() {
	document.getElementById('yui-gen1').style.visibility = 'visible';
	$('yui-gen1').addClassName('vpad');
}

function disableGenerateButton() {
//	document.getElementById('generate_wrapper').style.cursor = 'default'
	document.querySelector('#generate_wrapper #generate').disabled = true
}

function enableGenerateButton() {
//	document.getElementById('generate_wrapper').style.cursor = 'pointer'
	document.querySelector('#generate_wrapper #generate').disabled = false
}


function createRow(paramId, displayText) {
	var row = new Element('tr', {class:'userParamRow'})
	row.insert(new Element('td', {class:'setting-leftspace'}))
	var label = new Element('td', {class:'setting-name'})
	label.insert(displayText)
	row.insert(label)
	var value = new Element('td', {class:'setting-main'})
	value.insert(new Element('input', {type:'text', name: paramId, id: paramId, class:'additionalparams'}))
	row.insert(value)
	row.insert(new Element('td', {class:'setting-no-help'}))
	return row;
}


/** 
 * Finds the insertion point for the user params.
 */
function findRowInsertPoint() {
	var rows = $$('table.parameters tbody tr')
	for(var j = 0; j < rows.length; j++) {
		var row = rows[j]
		if (row.select('[name="insertion"]').length > 0 ) {
			return row;
		}
	}
}

/**
 * Encodes the user params in a URI encoded string separated by &, little bit like query params.
 * @returns
 */
function buildAdditionalParams() {
	var additionalParams = $$('.additionalparams')
	var encodedParams = []
	for (var i = 0 ; i < additionalParams.length ;  i++) {
		encodedParams.push(encodeURIComponent(additionalParams[i].id) + "=" + encodeURIComponent($F(additionalParams[i])));
	}
	
	return encodedParams.join("&");
}
 

/**
 * Check out the plans from git and set up the list of environments once complete.
 */
function checkoutAndRespond(provider) {
	$('spinnergif_fetch').show();
	backendProxy.runCheckoutPhase(provider, function(t) {
		var json = t.responseObject();
		var rc = json['returnCode'];
		
		if (json['err']) {
			updateLog(json['err']);
			showLog();
		}
		
		$('spinnergif_fetch').hide();

		if (rc === 100) {
			$('xmlError').show();
		} else if (rc === 200) {
			$('checkoutError').show();
		} else if (rc === 300) {
			$('planError').show();
		} else {
			envData = json.envs
			contData = json.conts
			$('checkoutSuccess').show();
			
			// Enable the environment drop-down
			var envselect = $('environment')
			envselect.enable();
			
		    for (var i = 0; i < envData.length; i++) {
		    	var entry = json.envs[i] 
		    	envselect.options.add(new Option(entry.environment + ":" + entry.displayName, entry.environment + "|" + entry.buildId));
		    }
		    $('environment').focus();
		    
		    // Enable the container drop-down
			var contselect = $('container')
			contselect.enable();
			
		    for (var i = 0; i < contData.length; i++) {
		    	var entry = json.conts[i] 
		    	contselect.options.add(new Option(entry.container + ":" + entry.displayName, entry.container + "|" + entry.buildId));
		    }
		    
		    setupListeners()
		}
	});
}

function setupListeners() {
	setupEnvironmentListener();
	setupContainerListener();
	setupVersionListener();
}

function fadeOut(element, duration, callback) {
	element.style.opacity = "1"
	var intId = setInterval(function() {
		var currentO = element.getOpacity()
		if (currentO > 0) {
			element.setOpacity(currentO - 0.01)
		} else {
			clearInterval(intId)
			element.style.display = "none"
			callback()
		}
	}, duration/100)
} 

/** 
 * Sets up a listener for changes of environment.
 */
function setupEnvironmentListener() {
	var dropdown = $("environment");
	observeForChange(dropdown, envData, 
		function(entry) {
			return entry.environment == getEnvironment() && entry.buildId == getEnvironmentBuildId()
		},
		function(entry) {
			document.getElementById('container').value = 0 // de-select the container when an environment's chosen
		}
	);
}

function setupContainerListener() {
	var dropdown = $("container");
	observeForChange(dropdown, contData, 
		function(entry) {
			return entry.container == getContainer() && entry.buildId == getContainerBuildId()
		},
		function(entry) {
			document.getElementById('environment').value = 0// de-select the environment when a container's chosen
		}
	);
}

function observeForChange(element, data, matchPredicate, entrySelectedCallback) {
	element.observe("change", function() {
		resetLog();
		document.getElementById("reportdiv").innerHTML = "";
		hideBuildButton();
		hideDetailsButton()
		$$('.userParamRow').invoke('hide')
		// Clear out all the select options except the 'select...' one
		$('version').select('option[value!=0]').invoke('remove')
		
		disableGenerateButton()

		if (element.options[element.selectedIndex].value == 0) {
			// Disable the version button if they haven't chosen an environment yet
	    	document.getElementById("version").disabled = true;
	    	$('noupscaption').show();
		} else {
			var select = $('version')
			for (var i = 0; i < data.length; i++) {
		    	var entry = data[i] 
		    	if (matchPredicate(entry)) {
					// Fill in the versions drop down
		    		for (var j = 0; j < entry.versions.length; j++) {
		    			var version = entry.versions[j]
		    			select.options.add(new Option(version, version));
		    		}
		    		
		    		var insertionPoint = findRowInsertPoint()
		    		
		    		// Add any user params
		    		for (var j = 0; j < entry.userParams.length; j++) {
		    			var param = entry.userParams[j]
		    			insertionPoint.insert({after: createRow("up_" + param.id, param.displayName)});
		    		}
		    		if (entry.userParams.length > 0) {
		    			$('noupscaption').hide();
		    		} else {
		    			$('noupscaption').show();
		    		}
		    		
		    		entrySelectedCallback.call(entry);
		    		break;
		    	}
		    }
		    
		    
			document.getElementById("version").disabled = false;
		}
	});
	
}

/** 
 * Sets up a listener for changes of version.
 */
function setupVersionListener() {
	var dropdown = $("version");
	dropdown.observe("change", function() {
		resetLog();
		document.getElementById("reportdiv").innerHTML = "";
		hideBuildButton();
		hideDetailsButton()
	    if (dropdown.options[dropdown.selectedIndex].value == 0) {
	    	disableGenerateButton();
		} else {
			enableGenerateButton();
		}
	});
}

/** 
 * Sets up a listener for changes of provider.
 */
function setupProviderListener() {
	var dropdown = $("provider");
	dropdown.observe("change", function() {
		resetLog();
		document.getElementById("reportdiv").innerHTML = "";
		$('checkoutSuccess').hide();
		hideBuildButton();
		hideDetailsButton();
		disableGenerateButton();
		$$('.userParamRow').invoke('hide')
		
		// Clear out all the select options except the 'select...' one
		$('environment').select('option[value!=0]').invoke('remove')
    	document.getElementById("environment").disabled = true;
		$('container').select('option[value!=0]').invoke('remove')
    	document.getElementById("container").disabled = true;
		$('version').select('option[value!=0]').invoke('remove')
    	document.getElementById("version").disabled = true;
    	$('noupscaption').show();
    	
	    if (dropdown.options[dropdown.selectedIndex].value != 0) {
			checkoutAndRespond(dropdown.options[dropdown.selectedIndex].value);
		}
	});
}

function getEnvironment() {
	var dropdown = $$('select[name="environment"]')[0];
	var environmentAndBuild = dropdown.options[dropdown.selectedIndex].value
	if (envBuildRegex.test(environmentAndBuild)) {
		var matcher = environmentAndBuild.match(envBuildRegex);
		return matcher[1];
	} 
	return "";
}

function getContainer() {
	var dropdown = $$('select[name="container"]')[0];
	var environmentAndBuild = dropdown.options[dropdown.selectedIndex].value
	if (envBuildRegex.test(environmentAndBuild)) {
		var matcher = environmentAndBuild.match(envBuildRegex);
		return matcher[1];
	} 
	return "";
}


function getEnvironmentBuildId() {
	var dropdown = $$('select[name="environment"]')[0];
	var environmentAndBuild = dropdown.options[dropdown.selectedIndex].value
	if (envBuildRegex.test(environmentAndBuild)) {
		var matcher = environmentAndBuild.match(envBuildRegex);
		return matcher[2];
	} 
	return "";
}

function getContainerBuildId() {
	var dropdown = $$('select[name="container"]')[0];
	var environmentAndBuild = dropdown.options[dropdown.selectedIndex].value
	if (envBuildRegex.test(environmentAndBuild)) {
		var matcher = environmentAndBuild.match(envBuildRegex);
		return matcher[2];
	} 
	return "";
}


function getVersion() {
	var dropdown = $('version');
	return dropdown.options[dropdown.selectedIndex].value
}

function getProvider() {
	var dropdown = $('provider');
	return dropdown.options[dropdown.selectedIndex].value
}


function generateReport() {
	var environment = getEnvironment();
	var container = getContainer();
	var buildRef = {}
	if (environment) {
		buildRef = getEnvironmentBuildId();
	} else {
		buildRef = getContainerBuildId();
	}
	
	var version = getVersion();
	var provider = getProvider();
	var additionalparams = buildAdditionalParams();
	clearExistingAlerts(true)
	backendProxy.runPreparePhase(environment, container, buildRef, additionalparams, version, provider, function(t) {
		var json = t.responseObject();
		var report = json['preparationSummaryReport'];
		var rc = json['returnCode'];
		var out = json['out'];
		var err = json['err'];
		var log = out;
		
		if (err.length > 0) {
			log += "\n---ERROR---\n" + err;
		}

		updateLog(log);
		
		clearExistingAlerts(false)

		if (rc == 300) {
			$('planError').show();
			showLog();
			hideDetailsButton();
		} else {
			$('reportdiv').update(report)
			$('generateSuccess').show();
			showBuildButton();
			showDetailsButton();
		}
	});
}

function updateLog(log) {
	$('logdiv').update(escapeHTML(log));
	$('logwrapperdiv').show();
}

function showLog() {
	$('logwrapperdiv').show();
	$('logdiv').show();
	$('logshow').hide();
	$('log_title').show();
}

function setupLogButton() {
	$('logshow').observe('click', function(event) {
		showLog();
	});
	$('detailshow').observe('click', function(event) {
		showDetails();
	});
}

function showDetails() {
	showDetailedView(rawData);
	document.getElementById('report').style.display="block";
	hideDetailsButton();
}

function hideDetailsButton() {
	$('detailshow').hide();
}

function showDetailsButton() {
	$('detailshow').show();
}

function resetLog() {
	$('logwrapperdiv').hide();
	$('logdiv').update('').hide();
	$('logshow').show();
	$('log_title').hide();
}

function clearExistingAlerts(displaySpinner) {
	var resultAlerts = $$('.result_alert');
	for (var i = 0 ; i < resultAlerts.length ; i++) {
		resultAlerts[i].hide();
	}
	if (displaySpinner) {
		$('spinnergif_generate').show();
	}
}

/*
 * There is a really nasty hover style that applies to the parameter table.
 * I cannot get rid of the table, nor the class, nor the fact that table applies to the class
 * but I can get rid of the horrid hover style, like this!!
 *
 * Lovingly ripped off from SteveWare in release plugin.
 */
function changeStyle(selectorText)
{
    var theRules = new Array();
    if (document.styleSheets[0].cssRules) {
        theRules = document.styleSheets[0].cssRules;
    } 
    else if (document.styleSheets[0].rules) {
        theRules = document.styleSheets[0].rules;
    }
    for (n in theRules)
    {
        if (theRules[n].selectorText == selectorText)   {
            theRules[n].style.backgroundColor = 'white';
        }
    }
}