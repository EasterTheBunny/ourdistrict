$(document).ready(function(){
    $( "#search" ).autocomplete({
      source: "/ajax/autocomplete/subjects",
      minLength: 2,
      select: function( event, ui ) {
        log( "Selected: " + ui.item.value + " aka " + ui.item.id );
      }
    });
});