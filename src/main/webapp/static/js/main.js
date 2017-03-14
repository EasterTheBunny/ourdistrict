(function($) {
  var scrollOffset;
  scrollOffset = 100;
  $(window).on('scroll', function() {
    var nav_height, scrollPos;
    if ($(window).scrollTop() < scrollOffset) {
      $('body').removeClass('scrolled');
    } else {
      $('body').addClass('scrolled');
    }
    scrollPos = $(document).scrollTop();
    return nav_height = $('#navbar').outerHeight();
  });
  return $('.parallax').parallax();
})(jQuery);
