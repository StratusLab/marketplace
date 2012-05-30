<html>
  <#include "header.ftl">
<body>
<div class="Page">
  <div class="Header">
     <div class="Banner">
     </div>
  </div>
  
    <#include "breadcrumbs.ftl">

<div class="Content">

<h1>${title}</h1>

<form method="GET">
<table>
    <tbody>
        <tr>
            <td>Query</td>
            <td><textarea name="query" rows="10" cols="80">${query}</textarea></td>
        </tr>
        <tr>
            <td/>
            <td><input type="submit" value="Submit"></td>
        </tr>
    </tbody>
</table>
</form>
<h2>Results:</h2>

<table cellpadding="0" cellspacing="0" border="0" id="resultstable" class="display">
    <thead>
        <#if (results?size > 0)>
        <tr>
            <#list results[0]?keys as meta>
                <th>${meta}</th>
            </#list>
        </tr>
        </#if>
    </thead>
    <tbody>
        <#if (results?size > 0)>
         <#list results as row>
             <tr> 
             <#list row?keys as key>
                 <td>${row[key]}</td>
             </#list>
             </tr>
         </#list>
       </#if>
     </tbody>
</table>

<script type="text/javascript" charset="utf-8">
        $(document).ready(function() {
                $('#resultstable').dataTable({
			'bPaginate':true,
                        'sPaginationType':'listbox',		
		});
        } );
</script>

</div>
</div>

</body>
</html>
