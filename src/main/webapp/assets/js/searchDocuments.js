$(document).ready(function(){
	$( "#search" ).autocomplete({
		source: "/ajax/autocomplete/subjects",
		minLength: 2,
		select: function( event, ui ) {

			$.ajax({
				url: "/api/v1/bills",
				method: "GET",
				dataType: "json",
				data: { "congress": 115, "filter[subject]": ui.item.id }
			}).done(function( json ){
				if(json.data) {
					var container = $("#bills");
					var template = container.find(".panel").first().clone();
					container.empty();

					for(var x = 0, len = json.data.length; x < len; x++) {
						var elem = json.data[x];

						var newPanel = template.clone();
						newPanel.find(".panel-title > a").attr("href", "/document/" + elem.id).text(elem.attributes.official_title);

						try {
							var summary = JSON.parse(elem.attributes.summary);

							newPanel.find(".panel-body").text(summary.text);
						} catch(e) {
							newPanel.find(".panel-body").text(elem.attributes.summary);
						}

						container.append(newPanel);
					}
				}
			});

		}
	});
});
