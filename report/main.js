$(document).ready(function() {
  $(window).resize(function() {
    var height = $(window).height();
    $("#overview").height(height);
    $("#detail").height(height);
    $("#packages").height(height - 60);
  }).resize();
  $("a").live("click", function() {
    var $this = $(this);
    if ($this.hasClass('external')) {
      return true;
    }
    if ($this.parent().hasClass("pkgLink")) {
      $(this).parent().next().slideToggle();
    }
    var href = $this.attr("href");
    if (href != "#") {
      var parts = href.split("#");
      $("#detail").empty().load(parts[0], function() {
        if (parts[1]) {
          var location = $("#"+parts[1]);
          var top = location.position().top - 30;
          $("#detail").scrollTop(top > 0 ? top : 0);
          location.parent().effect("highlight", {}, 1000);
        }
      });
    }
    return false;
  });
  $.get('packages.html', '', function(html) {
    $("#packages").html(html);
    resetPackageLinks();
  }, 'html').error(function() {
    showLoadError();
  });
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
  resetPackageLinks();
  $("#filter").val("").focus();
}

function resetPackageLinks() {
  var pkgContents = $(".pkgContent");
  if (pkgContents.size() == 1) {
    pkgContents.show();
  }
}

function showLoadError() {
  $('#load-error').show();
  if (navigator.userAgent.toLowerCase().indexOf('chrome') > -1 && document.location.protocol == 'file:') {
    $('#load-error .browser-chrome').show();
  } else {
    $('#load-error .browser-other').show();
  }
}