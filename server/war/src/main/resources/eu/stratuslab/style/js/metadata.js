var asInitVals = new Array();
var oCache = {
		iCacheLower: -1
};
var oTable;

function fnSetKey( aoData, sKey, mValue )
{
	for ( var i=0, iLen=aoData.length ; i<iLen ; i++ )
	{
		if ( aoData[i].name == sKey )
		{
			aoData[i].value = mValue;
		}
	}
}

function fnGetKey( aoData, sKey )
{
	for ( var i=0, iLen=aoData.length ; i<iLen ; i++ )
	{
		if ( aoData[i].name == sKey )
		{
			return aoData[i].value;
		}
	}
	return null;
}

function buildHtmlDisplay( aData )
{
        var os = aData[1];
        var osversion = aData[2];
        var arch = aData[3];

        var header = createHeader(os, osversion, arch);
        if( header.length == 0 )
        {
            header = aData[6];
        }

        var display = "<table class='vmpanel'>"
                        + "<tr><td colspan='3'><div id='header'>" + header
                        + "</div></td><td></td><td></td></tr>"
                        + "<tr><td></td><td></td><td rowspan='5'><a href='" + aData[7]
                        + "'><img src='/css/download.png'/></a></td></tr>"
                        + "<tr><td><div id='detail'>Endorser:</div></td>"
                        + "<td><div id='detail'>" + aData[4] + "</div></td></tr>"
                        + "<tr><td><div id='detail'>Identifier:</div></td>"
                        + "<td><div id='detail'>" + aData[6] + "</div></td></tr>"
                        + "<tr><td><div id='detail'>Created:</div></td>"
                        + "<td><div id='detail'>" + aData[5] + "</div></td></tr>" + "<tr></tr></div>"
                        + "<tr><td colspan='3'><div id='description'>" + aData[8] + "</div></td></tr>"
                        + "<tr><td><a href='/metadata/" + aData[6] + "/" + aData[4] + "/" + aData[5] + "'>More...</a></td></tr>"
                        + "</table>";

        return display;
}

function createHeader( os, osversion, arch ){
        var header = os;
        if(osversion.length > 0)
        {
            header = header + " v" + osversion;
        }       
        if(arch.length > 0)
        {
            header = header + " " + arch;
        }       

        return header;
}

function fnDataTablesPipeline ( sSource, aoData, fnCallback ) {
	var iPipe = 5; /* Ajust the pipe size */

	var bNeedServer = false;
	var sEcho = fnGetKey(aoData, "sEcho");
	var iRequestStart = fnGetKey(aoData, "iDisplayStart");
	var iRequestLength = fnGetKey(aoData, "iDisplayLength");
	var iRequestEnd = iRequestStart + iRequestLength;
	oCache.iDisplayStart = iRequestStart;

	/* outside pipeline? */
	if ( oCache.iCacheLower < 0 || iRequestStart < oCache.iCacheLower || iRequestEnd > oCache.iCacheUpper )
	{
		bNeedServer = true;
	}

	/* sorting etc changed? */
	if ( oCache.lastRequest && !bNeedServer )
	{
		for( var i=0, iLen=aoData.length ; i<iLen ; i++ )
		{
			if ( aoData[i].name != "iDisplayStart" 
				&& aoData[i].name != "iDisplayLength" 
					&& aoData[i].name != "sEcho" )
			{
				if ( aoData[i].value != oCache.lastRequest[i].value )
				{
					bNeedServer = true;
					break;
				}
			}
		}
	}

	/* Store the request for checking next time around */
	oCache.lastRequest = aoData.slice();

	if ( bNeedServer )
	{
		if ( iRequestStart < oCache.iCacheLower )
		{
			iRequestStart = iRequestStart - (iRequestLength*(iPipe-1));
			if ( iRequestStart < 0 )
			{
				iRequestStart = 0;
			}
		}

		oCache.iCacheLower = iRequestStart;
		oCache.iCacheUpper = iRequestStart + (iRequestLength * iPipe);
		oCache.iDisplayLength = fnGetKey( aoData, "iDisplayLength" );
		fnSetKey( aoData, "iDisplayStart", iRequestStart );
		fnSetKey( aoData, "iDisplayLength", iRequestLength*iPipe );

		var re = /[?&]([^=]+)(?:=([^&]*))?/g;
		var matchInfo;

		while(matchInfo = re.exec(window.location.search))
                {
			aoData.push( {"name": matchInfo[1], "value": matchInfo[2]} );
		} 
		
		$.ajax( {
			"dataType": 'json', 
			"type": "POST", 
			"url": sSource, 
			error: function (jqXHR, textStatus, errorThrown) {
				alert(jqXHR, textStatus, errorThrown);
			},
			"data": aoData, 
			"success": function (json) {
				var message = json.rMsg;
				if(message.indexOf("ERROR") != -1){
					alert(message);
				}
				oTable.fnSettings().oLanguage.sZeroRecords = json.rMsg;
				oTable.fnSettings().oLanguage.sEmptyTable = json.rMsg;
                
				/* Callback processing */
				oCache.lastJson = jQuery.extend(true, {}, json);

				if ( oCache.iCacheLower != oCache.iDisplayStart )
				{
					json.aaData.splice( 0, oCache.iDisplayStart-oCache.iCacheLower );
				}
				json.aaData.splice( oCache.iDisplayLength, json.aaData.length );

				fnCallback(json);
			}
		});
	}
	else
	{
		json = jQuery.extend(true, {}, oCache.lastJson);
		json.sEcho = sEcho; /* Update the echo for each response */
		json.aaData.splice( 0, iRequestStart-oCache.iCacheLower );
		json.aaData.splice( iRequestLength, json.aaData.length );
		fnCallback(json);
		return;
	}
}

jQuery.fn.dataTableExt.oApi.fnSetFilteringDelay = function ( oSettings, iDelay ) {
	/*
	 * Inputs: object:oSettings - dataTables settings object - automatically
	 * given integer:iDelay - delay in milliseconds Usage:
	 * $('#example').dataTable().fnSetFilteringDelay(250); Author: Zygimantas
	 * Berziunas (www.zygimantas.com) and Allan Jardine License: GPL v2 or BSD 3
	 * point style Contact: zygimantas.berziunas /AT\ hotmail.com
	 */
	var
	_that = this,
	iDelay = (typeof iDelay == 'undefined') ? 250 : iDelay;

	this.each( function ( i ) {
		$.fn.dataTableExt.iApiIndex = i;
		var
		$this = this,
		oTimerId = null,
		sPreviousSearch = null,
		anControl = $( 'input', _that.fnSettings().aanFeatures.f );

		anControl.unbind( 'keyup' ).bind( 'keyup', function() {
			var $$this = $this;

			if (sPreviousSearch === null || sPreviousSearch != anControl.val()) {
				window.clearTimeout(oTimerId);
				sPreviousSearch = anControl.val();
				oTimerId = window.setTimeout(function() {
					$.fn.dataTableExt.iApiIndex = i;
					_that.fnFilter( anControl.val() );
				}, iDelay);
			}
		});

		return this;
	} );
	return this;
}

$(document).ready(function() {
	/*
	 * Initialise DataTables, with no sorting on the 'details' column
	 */
	oTable = $('#search_table').dataTable( {
		"bServerSide": true,
		"sAjaxSource": window.location.href,
		"bProcessing": true,
		"fnServerData": fnDataTablesPipeline,
                //"fnCreatedRow": function( nRow, aData, iDataIndex ) {
                //                      $('td:eq(0)', nRow).html( displayHtml );
                //                 },
		"aoColumnDefs": [
		                 { "bSortable": false, 
                                   "fnRender": function ( o, val ) {
                                                   return buildHtmlDisplay( o.aData );
                                               },
                                   "aTargets": [ 0 ] },
		                 { "bSearchable": false, "bVisible": false, "aTargets": [1] }, //os
		                 { "bSearchable": false, "bVisible": false, "aTargets": [2] }, //osversion
		                 { "bSearchable": false, "bVisible": false, "aTargets": [3] }, //arch
		                 { "bSearchable": false, "bVisible": false, "aTargets": [4] }, //endorser
		                 { "bSearchable": false, "bVisible": false, "aTargets": [5] }, //created
                                 { "bSearchable": false, "bVisible": false, "aTargets": [6] }, //identifier
                                 { "bSearchable": false, "bVisible": false, "aTargets": [7] }, //location
                                 { "bSearchable": false, "bVisible": false, "aTargets": [8] }, //description
		                 ],
		                 "aaSorting": [[5, 'desc']],
		                 'sPaginationType': 'listbox',
		                 "oLanguage": {
		                	 "sSearch": "Search:"
		                 },
		"sDom": '<"top"fl<"clear">>rt<"bottom"ip<"clear">>'
	});

	$('#sortBy').change( function () {
		switch ($(this).val()) {
		case "_none_":  // first option chosen, not associated with any column,
			// do some default
			oTable.fnSort( [ [1,'asc'] ] );
			break;

		case "os":
			oTable.fnSort( [ [1,'asc'] ] );
			break;

		case "osversion":
			oTable.fnSort( [ [2,'asc'] ] );
			break;

		case "arch":
			oTable.fnSort( [ [3,'asc'] ] );
			break;

		case "endorser":
			oTable.fnSort( [ [4,'asc'] ] );
			break;

		case "date":
			oTable.fnSort( [ [5,'desc'] ] );
			break;
		}
	});
	
	oTable.fnSetFilteringDelay();
} );
