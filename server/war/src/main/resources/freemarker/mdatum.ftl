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

${content}


<input type="button" value="xml" onClick="window.location='${url}?media=xml'">
<input type="button" value="json" onClick="window.location='${url}?media=json'">

</div>

</div>

</body>
</html>
