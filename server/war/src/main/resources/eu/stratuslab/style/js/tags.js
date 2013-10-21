$(document).ready(function() {
    $('#endorsertags').dataTable({
    'sPaginationType':'listbox',
    "aaSorting": [[ 1, "desc" ]],
    "oLanguage": { "sSearch": "" }
    });
    $('#endorsertags_filter input').attr("placeholder", "Search...");
} );
