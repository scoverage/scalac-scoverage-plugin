$(document).ready(function(){
  $(".pkgLink").click(function() {
    $(this).next().slideToggle();
    return true;
  });
});