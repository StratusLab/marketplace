/* Formating function for row details */
function fnFormatDetails ( oTable, nTr )
{
	var aData = oTable.fnGetData( nTr );
	var sOut = '<table cellpadding="5" cellspacing="0" border="0" style="padding-left:50px;">';
	sOut += '<tr><td>Subject:</td><td>'+aData[2]+'</td></tr>';
        sOut += '<tr><td>Issuer:</td><td>'+aData[3]+'</td></tr>'
        sOut += '<tr><td>Email:</td><td>'+aData[4]+'</td><td/></tr>';
	sOut += '<tr><td>View:</td><td><a href=metadata/'
             +aData[4]+'>Current</a> <a href=endorsers/'
             +aData[4]+'>History</a> <a href=endorsers/'
             +aData[4]+'/tags>Tags</a></td></tr>';
        sOut += '</table>';
	return sOut;
}

var asInitVals = new Array();

$(document).ready(function() {
	/*
	 * Insert a 'details' column to the table
	 */
	var nCloneTh = document.createElement( 'th' );
	var nCloneTd = document.createElement( 'td' );
	nCloneTd.innerHTML = '<img src="img/details_open.png">';
	nCloneTd.className = "center";
	
	$('#endorsers_table thead tr').each( function () {
		this.insertBefore( nCloneTh, this.childNodes[0] );
	} );
	
	$('#endorsers_table tbody tr').each( function () {
		this.insertBefore(  nCloneTd.cloneNode( true ), this.childNodes[0] );
	} );
	
	/*
	 * Initialse DataTables, with no sorting on the 'details' column
	 */
	var oTable = $('#endorsers_table').dataTable( {
		"aoColumnDefs": [
			{ "bSortable": false, "aTargets": [ 0 ] },
                        { "bVisible": false, "aTargets": [2] },
                        { "bVisible": false, "aTargets": [3] },
                        { "bVisible": false, "aTargets": [4] },                       
                ],
		"aaSorting": [[1, 'asc']],
                'sPaginationType': 'listbox',
                "oLanguage": {
                        "sSearch": ""
                }
	});
	
	/* Add event listener for opening and closing details
	 * Note that the indicator for showing which row is open is not controlled by DataTables,
	 * rather it is done here
	 */
	$('#endorsers_table tbody td img').live('click', function () {
		var nTr = this.parentNode.parentNode;
		if ( this.src.match('details_close') )
		{
			/* This row is already open - close it */
			this.src = "img/details_open.png";
			oTable.fnClose( nTr );
		}
		else
		{
			/* Open this row */
			this.src = "img/details_close.png";
			oTable.fnOpen( nTr, fnFormatDetails(oTable, nTr), 'details' );
		}
	} );

        $('#endorsers_table_filter input').attr("placeholder", "Search...");

        $("tfoot input").keyup( function () {
                /* Filter on the column (the index) of this element */
                oTable.fnFilter( this.value, $("tfoot input").index(this) + 1 );
        } );

        /*
         * Support functions to provide a little bit of 'user friendlyness' to the textboxes in
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

} );
