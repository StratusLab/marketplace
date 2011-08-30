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
		$('#endorserhistory').dataTable();
	} );
</script>

</div>
</div>

</body>
</html>
