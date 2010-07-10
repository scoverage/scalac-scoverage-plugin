$(document).ready(function() {
  $(window).resize(function() {
    var height = $(window).height();
    $("#overview").height(height);
    $("#detail").height(height);
    $("#packages").height(height - 40);
  }).resize();
  $("a").live("click", function() {
    var $this = $(this);
    if ($this.parent().hasClass("pkgLink")) {
      $(this).parent().next().slideToggle();
    }
    var href = $this.attr("href");
    $("#detail").empty().load(href.replace(/#.*/, ''));
    return false;
  });
  $("#packages").load("packages.html");
  $("#detail").load("summary.html");

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
          $content.show().prev().removeClass("disabled");
        } else {
          $content.hide().prev().addClass("disabled");
        }
      });
    }
  });
});

function filterCleared() {
  $(".pkgLink,.pkgRow").show();
  $(".pkgLink").removeClass("disabled");
  $(".pkgContent").hide();
  $("#filter").val("").focus();
}