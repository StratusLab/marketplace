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

<div class="Footer">
                StratusLab is co-funded by the European Community's<br/>Seventh Framework Programme (Capacities)<br/>Grant Agreement INFSO-RI-261552            </div>
</div>

</body>
</html>
