<html>
  <#include "header.ftl">
  <#include "breadcrumbs.ftl">
<body>
<div class="Page">
  <div class="Header">
     <div class="Banner">
     </div>
  </div>
  
<div class="Content">

<h1>${title}</h1>

    <form>
        <label for="range">Last </label>
        <select name="range" id="range">
        <option value="30"></option>
        <option selected="selected" value="30">30 days</option>
        <option value="60">60 days</option>
        <option value="90">90 days</option>
        </select>
     </form> 

     <table cellpadding="0" cellspacing="0" border="0" class="display" id="endorserhistory">
	  <thead>
	      <tr>
                  <th>Identifier</th>
                  <th>Created</th>
                  <th>Location</th>
                  <th>Deprecated</th>
              </tr>
          </thead>
          <tbody>
            <#list content as history>
              <tr>
              <td><a href=metadata/${history.identifier}/${history.email}/${history.created}>${history.identifier}</a></td>
              <td>${history.created}</td>
	      <td>${history.location}</td>
	      <td><b><font color="red">${history.deprecated}</font></b></td>
              </tr>
             </#list>
           </tbody>
      </table> 

<script type="text/javascript" charset="utf-8">
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
        //may want to use $.trim in here
            return $(this).text() == text2; 
        }).attr('selected', true);

        $(function() {
            $('#range').change(function() {
                $(this).closest('form').submit();
        });
        });

</script>

</div>
</div>

</body>
</html>
