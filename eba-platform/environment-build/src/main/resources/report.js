window.init = function(rawData, detailed) {
	var rawDataClone = clone(rawData[0])
	if (detailed) {
		showDetailedView(rawData);
	}
	document.getElementById('actions_table').innerHTML = showActions(rawDataClone);
	
	// Handle the expando-hiera-table-thingy
	$$("div.detailtable").each(function(div) {
		var detail = div.down(".detailcontent");
		var button = div.down("button");
		detail.hide();
		button.observe('click', function(event) {
			detail.show();
			button.hide();
		});
	});
}

window.showDetailedView = function(rawData) {
	webix.ui({
		container:"report_container",
		view:"treetable",
		columns:[
		         { id:"id", header:["Sequence\\Step",{content:"textFilter"}], template:"{common.treetable()} #id#", width:350},
		         { id:"contextParameters", header:["Contributed Parameters",{content:"textFilter"}], adjust:true},
		         { id:"fullParameters", header:["Full Context Parameters",{content:"textFilter"}], adjust:true}
		         ],
		         filterMode:{
		        	 level:false,
		        	 showSubItems:false
		         },
		         autoheight:true,
		         autowidth:true,
		         data: rawData,
	});
}

/** Start of 'class' TreeNode **/

/**
 * Structure to allow easier recursion into the raw JSON.
 */
window.TreeNode = function(object) {
	this.children = []
	this.value = object
	if (typeof object.data !== "undefined") {
	    for (var i = 0; i < object.data.length; i++) { 
	    	this.children.push(new TreeNode(object.data[i]));
	    }
    }
    this.isLeaf = function() {
  	  return this.children.length == 0;
	};
}


/** End of TreeNode **/

/**
 * Recurses into the server response sequence/actions graph and spits out actions
 * into the #actions div. 
 */
window.showActions = function(json) {
	var node = new TreeNode(json);
	var content = "<thead><tr><th>Actions</th><th>Context</th><th>Parameters</th></tr></thead>"
	var rows = visitDfs(node, function(node) {
		if (node.isLeaf()) {
			var rawParams = node.value.fullParameters;
			var formattedParams = rawParams.replace(/\s*,\s*/g, "<br />");
			var actionContext = "";
			var actionParams = node.value.actioncontext;
			if (actionParams) {
				for (x in actionParams) {
					actionContext += "<dt>" + escapeHTML(x) + "</dt><dd><pre>" + escapeHTML(actionParams[x]) + "</pre></dd>";
				}
			}
			
			var detailsButton = "<div class='detailtable yui-button yui-submit-button submit-button primary'><button type='button'>Details...</button>";
			
			var table = node.value.table;
			var tableHTML = "";

			if (table) {
				tableHTML = detailsButton + "<table class='detailcontent'><thead><tr>";
				var head = table.head;
				var body = table.body;
				for (var i=0; i < head.length; i++) {
					tableHTML += "<th>" + escapeHTML(head[i]) + "</th>";
				}
				tableHTML += "</tr></thead><tbody>"
				
				for (var r=0; r < body.length; r++) {
					var row = body[r];
					tableHTML += "<tr>";
					for (var c=0; c < row.length; c++) {
						tableHTML += "<td>" + escapeHTML(row[c]) + "</td>"
					}
					tableHTML += "</tr>";
				}
					
				tableHTML += "</tbody></table></div>";
			}
			
			var beforeAfters = node.value.beforeAfters;
			var beforeAftersHTML = "";
			
			if (beforeAfters) {
				beforeAftersHTML = detailsButton + "<div class='detailcontent'>";
				for (var i=0; i<beforeAfters.length; i++) {
					beforeAftersHTML += "<h4>" + escapeHTML(beforeAfters[i].domain + "/" + beforeAfters[i].basename) + "</h4>"
						+ dodiff(beforeAfters[i].before, beforeAfters[i].after).outerHTML;
				}
				beforeAftersHTML += "</div></div>";
			}
			
			return "<tr><th>" + node.value.id + "<br />" +node.value.actiontype + "</th>"
					+ "<td>" + formattedParams + "</td>"
					+ "<td><dl>" + actionContext +"</dl>"
					+ tableHTML + beforeAftersHTML + "</td></tr>";
		}
		return "";
	});
	return content + "<tbody>" + rows + "</tbody>";
}

window.visitDfs = function(node, func) {
    if (func) {
        var row = func(node);
    }
 
    for (var i = 0; i < node.children.length; i++) { 
	    row = row + visitDfs(node.children[i], func);
	}
	return row;
}

window.escapeHTML = function(s) { 
    return s.replace(/&/g, '&amp;')
            .replace(/"/g, '&quot;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;');
}

window.clone = function(obj) {
	var newData = [];
	for (var i = 0; i < obj.data.length ; i++) {
		newData.push(clone(obj.data[i]))
	}
	
	var output = {};
	for (var property in obj) {
		if (property == "data") {
			output.data = newData;
		} else {
			output[property] = obj[property];
		}
	}
	
	return output;
}

window.dodiff = function(before, after) {
	"use strict";
		var beforeLines = difflib.stringAsLines(before),
		afterLines = difflib.stringAsLines(after),
		sm = new difflib.SequenceMatcher(beforeLines, afterLines),
		opcodes = sm.get_opcodes();

	return diffview.buildView({
		baseTextLines: beforeLines,
		newTextLines: afterLines,
		opcodes: opcodes,
		baseTextName: "Before",
		newTextName: "After",
		contextSize: null,
		viewType: 0
	});
}
