var asInitVals = new Array();
var oTable;

function buildHtmlDisplay( aData )
{
        var os = aData[1];
        var osversion = aData[2];
        var arch = aData[3];

        var title = aData[9];

        var header = createHeader(title, os, osversion, arch);
        if( header.length == 0 )
        {
            header = aData[6]; //identifier
        }

        var downloadLink = "";
        if(aData[7].substring(0, "http".length) == "http"){
            downloadLink = "<a href='" + aData[7]
                        + "'><img src='/css/download.png'/></a>";
        } 

        var display = "<table class='vmpanel'>"
                        + "<tr><td colspan='3'><div id='header'>" + header
                        + "</div></td><td></td><td></td></tr>"
                        + "<tr><td></td><td></td><td rowspan='5'>" + downloadLink + "</td></tr>"
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

function createHeader( title, os, osversion, arch ){
        var header = "";

        if(title.length > 0 && title != "null")
        {
            header = title;
        } else if(os.length > 0 && os != "null"){
            header = os;

            if(osversion.length > 0 && osversion != "null")
            {
                header = header + " v" + osversion;
            }       
            if(arch.length > 0 && arch != "null")
            {
                header = header + " " + arch;
            }       
        }

        return header;
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
                                 { "bSearchable": false, "bVisible": false, "aTargets": [9] }, //title
		                 ],
		                 "aaSorting": [[5, 'desc']],
		                 'sPaginationType': 'listbox',
		                 "oLanguage": {
		                	 "sSearch": "Search:"
		                 },
		"sDom": '<"top"fl<"clear">>rt<"bottom"ip<"clear">>'
	});

        var search_timeout = undefined;
        $("#filterBy input").keyup( function (event) {
                if(event.keyCode!='9') {
                        if(search_timeout != undefined) {
                                clearTimeout(search_timeout);
                        }
                        $this = this;
                        search_timeout = setTimeout(function() {
                                search_timeout = undefined;
                                oTable.fnFilter( $this.value, 
                                                 $("#filterBy input").index($this) + 1 );
                        }, 250);
                }
        } );

        $("#filterBy input").focusout( function () {
                if(search_timeout != undefined) {
                        clearTimeout(search_timeout);
                }
                $this = this;
                oTable.fnFilter( $this.value, $("#filterBy input").index($this) + 1 );
        } );

        /*
         * Support functions to provide a little bit of 'user friendliness' to the textboxes in
         * the footer
         */
        $("#filterBy input").each( function (i) {
                asInitVals[i] = this.value;
        } );

        $("#filterBy input").focus( function () {
                if ( this.className == "search_init" )
                {
                        this.className = "";
                        this.value = "";
                }
        } );

        $("#filterBy input").blur( function (i) {
                if ( this.value == "" )
                {
                        this.className = "search_init";
                        this.value = asInitVals[$("#filterBy input").index(this)];
                }
        } );

        /*
         * sorting select
         */ 
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

function getParameterByName(name)
{
    name = name.replace(/[\[]/, "\\\[").replace(/[\]]/, "\\\]");
    var regexS = "[\\?&]" + name + "=([^&#]*)";
    var regex = new RegExp(regexS);
    var results = regex.exec(window.location.search);
    if(results == null)
        return "";
    else
        return decodeURIComponent(results[1].replace(/\+/g, " "));
}

var text1 = getParameterByName("deprecated");
$("select option").filter(function() {
    return $(this).text() == text1;
}).attr('selected', true);

$(function() {
   $('#deprecated').change(function() {
       $(this).closest('form').submit();
   });
 });
