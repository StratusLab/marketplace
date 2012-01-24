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

<p>View:
<a HREF="/metadata">
All</a> |
<a href="/metadata?deprecated">
With deprecated</a> |
<a href="/metadata?deprecated=only">
Deprecated only</a>
</p>

<form>
<label for="sortBy">Sort by:</label>
<select name="sortBy" id="sortBy">
<option value="_none_"></option>
<option value="os">OS</option>
<option value="osversion">OS Version</option>
<option value="arch">Arch</option>
<option value="endorser">Endorser</option>
<option value="date">Date</option>
</select>

<table id="search_table" class="search">
    <thead>
        <tr>
            <th>details</th>
            <th>os</th>
            <th>os-version</th>
            <th>arch</th>
            <th>endorser</th>
            <th>date</th>
        </tr>
    </thead>
    <tbody>
     </tbody>
</table>

<script type="text/javascript" language="javascript" src="/js/metadata.js"></script>

</br>

</div>
</div>

</body>
</html>
