$(document).ready(function(){
  $(".pkgLink").click(function() {
    $(this).next().slideToggle();
    return true;
  });
  $(".filterContainer .post").click(filterCleared);
  $("#filter").keyup(function(evt) {
    var filterText = $("#filter").val();
    if (filterText.replace(/^\s+|\s+$/g, '') == "") {
      filterCleared();
    } else {
      var filter = new RegExp(" "+$("#filter").val(), "i")
      $(".pkgContent").each(function(i,e) {
        var $content = $(this);
        var shown = false;
        $(".pkgRow .className", $content).each(function(i, e) {
          var $this = $(this);
          var text = " "+$this.text().replace(".", " ");
          if (filter.test(text)) {
            $this.parents(".pkgRow").show();
            shown = true;
          } else {
            $this.parents(".pkgRow").hide();
          }
        });
        if (shown) {
          $content.show().removeClass("disabled");
        } else {
          $content.hide().prev().addClass("disabled");
        }
      });
    }
  });
});

function filterCleared() {
  $(".pkgContent").hide();
  $(".pkgRow,.pkgLink").show();
  $(".pkgLink").removeClass("disabled");
  $("#filter").val("").focus();
}