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

<p>Deprecated entries:
<a HREF="/metadata">
Off</a> |
<a href="/metadata?deprecated">
On</a> |
<a href="/metadata?deprecated=only">
Only</a>
</p>

<form id="filterBy" name="filterBy">
<label for="filterBy">Filter:</label><br/>
<input type="text" name="search_os" value="Search os" class="search_init"><br/>
<input type="text" name="search_version" value="Search version" class="search_init"><br/>
<input type="text" name="search_arch" value="Search arch" class="search_init"><br/>
<input type="text" name="search_arch" value="Search endorser" class="search_init">
</form>

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
</form>

<table id="search_table" class="search">
    <thead>
        <tr>
            <th>details</th>
            <th>os</th>
            <th>os-version</th>
            <th>arch</th>
            <th>endorser</th>
            <th>date</th>
            <th>identifier</th>
            <th>location</th>
            <th>description</th>
            <th>title</th>
        </tr>
    </thead>
    <tbody>
     </tbody>
</table>

<script type="text/javascript" language="javascript" src="/js/metadata.js"></script>

<br/>

</div>
</div>

</body>
</html>
