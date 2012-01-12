var asInitVals = new Array();

$(document).ready(function() {
	/*
	 * Initialse DataTables, with no sorting on the 'details' column
	 */
	var oTable = $('#search_table').dataTable( {
		"bServerSide": true,
                "sAjaxSource": window.location.href,
                "bProcessing": true,
                "fnServerData": function ( sSource, aoData, fnCallback ) {
                        var re = /[?&]([^=]+)(?:=([^&]*))?/g;
                        var matchInfo;

                        while(matchInfo = re.exec(window.location.search)){
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

                                    fnCallback(json);
                                }	
                         } );
		},
                
                "aoColumnDefs": [
			{ "bSortable": false, "aTargets": [ 0 ] },
                        { "bSearchable": false, "bVisible": false, "aTargets": [1] },
                        { "bSearchable": false, "bVisible": false, "aTargets": [2] },
                        { "bSearchable": false, "bVisible": false, "aTargets": [3] },
                        { "bSearchable": false, "bVisible": false, "aTargets": [4] },
                        { "bSearchable": false, "bVisible": false, "aTargets": [5] },
		],
		"aaSorting": [[5, 'desc']],
                'sPaginationType': 'listbox',
                "oLanguage": {
                        "sSearch": "Search:"
                }
	});

        $('#sortBy').change( function () {
            switch ($(this).val()) {
                case "_none_":  // first option chosen, not associated with any column, do some default
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

} );
