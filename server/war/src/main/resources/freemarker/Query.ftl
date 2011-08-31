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
${results}

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
