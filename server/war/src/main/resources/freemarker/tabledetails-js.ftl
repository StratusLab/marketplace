<script language="javascript">

/* Formating function for row details */
function fnFormatDetails ( oTable, nTr )
{
	var aData = oTable.fnGetData( nTr );
	var sOut = '<table cellpadding="5" cellspacing="0" border="0" style="padding-left:50px;">';
	sOut += '<tr><td>Description:</td><td>'+aData[8]+'</td></tr>';
	sOut += '<tr><td>Identifier:</td><td>'+aData[6]+'</td></tr>';
        sOut += '<tr><td>URL:</td><td><a href="'+aData[7]+'">'+aData[7]+'</a></td></tr>';
        sOut += '<tr><td><a href="/metadata/'+aData[6]+'/'+aData[4]+'/'+aData[5]+'">More...</a></td><td></td></tr>';
	sOut += '</table>';
	
	return sOut;
}

jQuery.fn.dataTableExt.oApi.fnSetFilteringDelay = function ( oSettings, iDelay ) {
	/*
	 * Inputs:      object:oSettings - dataTables settings object - automatically given
	 *              integer:iDelay - delay in milliseconds
	 * Usage:       $('#example').dataTable().fnSetFilteringDelay(250);
	 * Author:      Zygimantas Berziunas (www.zygimantas.com) and Allan Jardine
	 * License:     GPL v2 or BSD 3 point style
	 * Contact:     zygimantas.berziunas /AT\ hotmail.com
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

var asInitVals = new Array();

$(document).ready(function() {
	/*
	 * Insert a 'details' column to the table
	 */
	var nCloneTh = document.createElement( 'th' );
	var nCloneTd = document.createElement( 'td' );
	nCloneTd.innerHTML = '<img src="/css/details_open.png">';
	nCloneTd.className = "center";
	
	$('#form_with_details thead tr').each( function () {
		this.insertBefore( nCloneTh, this.childNodes[0] );
	} );
	
	$('#form_with_details tbody tr').each( function () {
		this.insertBefore(  nCloneTd.cloneNode( true ), this.childNodes[0] );
	} );
	
	/*
	 * Initialse DataTables, with no sorting on the 'details' column
	 */
	var oTable = $('#form_with_details').dataTable( {
		"bServerSide": true,
                "sAjaxSource": window.location.href,
                "bProcessing": true,
                "fnServerData": function ( sSource, aoData, fnCallback ) {
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

                                    fnCallback(json);
                                }	
                         } );
		},
                "aoColumnDefs": [
			{ "bSortable": false, "aTargets": [ 0 ] },
                        { "bVisible": false, "aTargets": [6] },
                        { "bVisible": false, "aTargets": [7] },
                        { "bVisible": false, "aTargets": [8] },
		],
		"aaSorting": [[1, 'asc']],
                'sPaginationType': 'listbox',
                "oLanguage": {
                        "sSearch": "Search all columns:"
                }
	});
	
	/* Add event listener for opening and closing details
	 * Note that the indicator for showing which row is open is not controlled by DataTables,
	 * rather it is done here
	 */
	$('#form_with_details tbody td img').live('click', function () {
		var nTr = this.parentNode.parentNode;
		if ( this.src.match('details_close') )
		{
			/* This row is already open - close it */
			this.src = "/css/details_open.png";
			oTable.fnClose( nTr );
		}
		else
		{
			/* Open this row */
			this.src = "/css/details_close.png";
			oTable.fnOpen( nTr, fnFormatDetails(oTable, nTr), 'details' );
		}
	} );

        var search_timeout = undefined;
        $("tfoot input").keyup( function (event) {
		if(event.keyCode!='9') {
			if(search_timeout != undefined) {
				clearTimeout(search_timeout);
			}
			$this = this;
			search_timeout = setTimeout(function() {
				search_timeout = undefined;
				oTable.fnFilter( $this.value, $("tfoot input").index($this) + 1 );
			}, 250);
		}
	} );
				
	$("tfoot input").focusout( function () {
		if(search_timeout != undefined) {
			clearTimeout(search_timeout);
		}
		$this = this;
		oTable.fnFilter( $this.value, $("tfoot input").index($this) + 1 );
	} );

        /*
         * Support functions to provide a little bit of 'user friendliness' to the textboxes in
         * the footer
         */
        $("tfoot input").each( function (i) {
                asInitVals[i] = this.value;
        } );

        $("tfoot input").focus( function () {
                if ( this.className == "search_init" )
                {
                        this.className = "";
                        this.value = "";
                }
        } );

        $("tfoot input").blur( function (i) {
                if ( this.value == "" )
                {
                        this.className = "search_init";
                        this.value = asInitVals[$("tfoot input").index(this)];
                }
        } );

	oTable.fnSetFilteringDelay();

} );

</script>
