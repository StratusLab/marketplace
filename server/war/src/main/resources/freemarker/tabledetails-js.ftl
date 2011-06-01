<script language="javascript">

/* Formating function for row details */
function fnFormatDetails ( oTable, nTr )
{
	var aData = oTable.fnGetData( nTr );
	var sOut = '<table cellpadding="5" cellspacing="0" border="0" style="padding-left:50px;">';
	sOut += '<tr><td>Description:</td><td>'+aData[8]+'</td></tr>';
	sOut += '<tr><td>Identifier:</td><td>'+aData[5]+'</td></tr>';
	sOut += '<tr><td>URL:</td><td><a href="'+aData[7]+'">'+aData[7]+'</a></td></tr>';
	sOut += '</table>';
        sOut += '<br/><p align="right"><a href="/metadata/'+aData[5]+'/'+aData[4]+'/'+aData[6]+'">More...</a></p>';
	
	return sOut;
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
		"aoColumnDefs": [
			{ "bSortable": false, "aTargets": [ 0 ] },
                        { "bVisible": false, "aTargets": [5] },
                        { "bVisible": false, "aTargets": [6] },
                        { "bVisible": false, "aTargets": [7] },
                        { "bVisible": false, "aTargets": [8] },
		],
		"aaSorting": [[1, 'asc']],
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

</script>
