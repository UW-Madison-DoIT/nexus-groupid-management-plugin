var gidm = gidm || {};

gidm.nexusConfig = {};

gidm.sortOptions = function(selectBox) {
	var options = selectBox.find("option");

	options.sort(function(a, b) {
		if (a.text > b.text)
			return 1;
		else if (a.text < b.text)
			return -1;
		else
			return 0
	})

	selectBox.empty().append(options);
};

gidm.init = function(callback) {
	var updateLinks = function() {
		$("a.gidm_link").each(function() {
			var link = $(this);
			var href = link.attr('href');
			link.attr('href', gidm.nexusConfig.baseUrl + '/' + href);
		});
	};

	// Load nexus config and trigger managed repos loading
	$.getJSON('../../../service/local/status', function(data) {
		gidm.nexusConfig = data.data;
		updateLinks();
		callback();
	});
}
