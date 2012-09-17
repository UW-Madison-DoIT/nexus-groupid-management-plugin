/*
 * Copyright 2012, Board of Regents of the University of
 * Wisconsin System. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Board of Regents of the University of Wisconsin
 * System licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a
 * copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
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
