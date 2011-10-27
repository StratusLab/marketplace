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

<br/>

<table id="form_with_details" class="display">
    <thead>
        <tr>
            <th>os</th>
            <th>os-version</th>
            <th>arch</th>
            <th>endorser</th>
            <th>date</th>
            <th>identifier</th>
            <th>description</th>
            <th>location</th>
        </tr>
    </thead>
    <tbody>
     </tbody>
     <tfoot>
         <tr>
             <th></th>
             <th><input type="text" name="search_os" value="Search os" class="search_init" /></th>
	     <th><input type="text" name="search_osversion" value="Search os version" class="search_init" /></th>
	     <th><input type="text" name="search_arch" value="Search architecture" class="search_init" /></th>
             <th><input type="text" name="search_endorser" value="Search endorser" class="search_init" /></th>
             <th><input type="text" name="search_date" value="Search date" class="search_init" /></th>
             <th><input type="text" name="search_identifier" value="Search identifier" class="search_init" /></th>
             <th><input type="text" name="search_location" value="Search location" class="search_init" /></th>
             <th><input type="text" name="search_description" value="Search description" class="search_init" /></th>
         </tr>
      </tfoot>
</table>

<#include "tabledetails-js.ftl">

</div>
</div>

</body>
</html>
