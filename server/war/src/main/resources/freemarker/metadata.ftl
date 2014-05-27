<html>
  <#include "header.ftl">
  <#include "breadcrumbs.ftl">

<body>

<div class="Page">
  <div class="Header">
     <div class="Banner">
     </div>
  </div>
<h1>${title}</h1>

<br/>

<div id="content">
<table id="search_table" class="search">
    <thead>
        <tr>
            <th>details</th>
            <th>os</th>
            <th>os-version</th>
            <th>arch</th>
            <th>endorser</th>
            <th>kind</th>
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


<br/>
</div>

<div id="sidebar">
<form>
    <label for="status">Status: </label>
    <select name="status" id="status">
        <option selected="selected" value="valid">valid</option>
        <option value="expired">expired</option>
        <option value="deprecated">deprecated</option>
    </select>

    <label for="location">Location: </label>
    <select name="location" id="location">
        <option value="web">web</option>
        <option value="pdisk">pdisk</option>
        <option selected="selected" value="all">all</option>
     </select>
</form>

<form id="filterBy" name="filterBy">
<label for="filterBy">Filter:</label><br/>
<input type="text" name="search_os" value="Search os" class="search_init"><br/>
<input type="text" name="search_version" value="Search version" class="search_init"><br/>
<input type="text" name="search_arch" value="Search arch" class="search_init"><br/>
<input type="text" name="search_endorser" value="Search endorser" class="search_init"><br/>
<input type="text" name="search_kind" value="search kind" class="search_init">
</form>

<form>
<label for="sortBy">Sort by:</label>
<select name="sortBy" id="sortBy">
<option value="_none_"></option>
<option value="kind">Kind</option>
<option value="os">OS</option>
<option value="osversion">OS Version</option>
<option value="arch">Arch</option>
<option value="endorser">Endorser</option>
<option value="date">Date</option>
</select>
</form>
</div>

<script type="text/javascript" language="javascript" src="js/metadata.js"></script>

</div>

</body>
</html>
