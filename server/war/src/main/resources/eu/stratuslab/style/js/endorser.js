$(document).ready(function() {
    $('#endorserhistory').dataTable({
    'sPaginationType':'listbox',
    "aaSorting": [[ 1, "desc" ]]
    });
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

var text1 = getParameterByName("range");
var text2 = text1 + " days";
$("select option").filter(function() {
    return $(this).text() == text2;
}).attr('selected', true);
        
$(function() {
   $('#range').change(function() {
       $(this).closest('form').submit();
   });
 });
