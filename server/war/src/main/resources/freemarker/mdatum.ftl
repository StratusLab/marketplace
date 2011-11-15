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

</div>

<br>

<div class='image_rating'>
    Rate this image:
    <div id="${identifier}" class="rate_image">
        <div class="star_1 ratings_stars"></div>
        <div class="star_2 ratings_stars"></div>
        <div class="star_3 ratings_stars"></div>
        <div class="star_4 ratings_stars"></div>
        <div class="star_5 ratings_stars"></div>
        <div class="total_votes">vote data</div>
    </div>
</div>

</div>

</body>
</html>
