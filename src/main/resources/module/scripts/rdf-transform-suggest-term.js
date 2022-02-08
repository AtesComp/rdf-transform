/*
 * Customized Suggest Extension Modifier:
 *		Add RDF Term processing to the Suggest flyout.
 *		Based on Freebase Suggest (fbs) Extension.
 *		See: https://docs.openrefine.org/technical-reference/suggest-api
 *
 * The "suggest" object is a custom extension added to jQuery
 *		See OpenRefine's webapp/modules/core/scripts/util/custom-suggest.js
 *
 * RDFTransform extends the $.suggest object for RDF Term resolution
 */
( function($) {
    if (! $.suggest) {
    	alert(
			"ERROR: $.suggest required!\n" +
			"See: https://docs.openrefine.org/technical-reference/suggest-api"
		);
    }

	//
	// Add New Suggested Terms Functions
	//
    $.suggest(
		// object created...
		"suggestTerm",
		// create with...
        $.extend(
			// deep copy...
            true,
			// object extended...
            {},
			// object merged (overwrites same named prior objects)...
            $.suggest.suggest.prototype,
			// object merged (overwrites same named prior objects)...
            {   // List of functions to merge...
				// NOTE: Added as normal functions (not arrow =>) to preserve
				//		"this" in final object.
                create_item(data, response) {
					var li =
						$("<li></li>")
						.addClass(this.options.css.item);
	                var name =
						$("<div></div>")
						.addClass(this.options.css.item_name)
						.append(
							$("<div></div>")
							.addClass(this.options.css.item_type)
							.text(data.iri) // data.id
						);
					var nameText =
						$("<label></label>")
						.append(
							$.suggest
							.strongify(data.cirie, response.prefix) // data.name
						);

					data.cirie = nameText.text(); // data.name
					name.append(nameText);
	                li.append(name);

					// TODO: Very smelly hack to disable cache
	                $.suggest.cache = {};

					return li;
	            },

	            request(query, cursor) {
	                var data = {};
	                data[this.options.query_param_name] = query;

	                clearTimeout(this.request.timeout);
					// Parameters names are defined for Suggest Term Command (SuggestTermCommand.java) Java code.
					// The "project" holds the Project ID of the project to search...
                    data.project = theProject.id;
					// The "type" holds the value type to search (class, property)...
					data.type = this.options.type;
					// "The prefix" holds the query search value...
					data.query = query;
					// See Defaults below for other default parameters.

                    var url =
						this.options.service_url +
						this.options.service_path; // + "?" +
						//$.param(data, true);

					var ajax_options = {
	                    "url"      : url,
	                    "data"     : data,
	                    "dataType" : "json",
                        "cache"    : true
	                };

					this.request.timeout =
						setTimeout(
							() => {
	                        	$.get(ajax_options)
								.done(
									(data, strStatus, xhr) => {
										$.suggest.cache[url] = data;
										data.query = query;  // ...keep track of query to match with response
										this.response(data, cursor ? cursor : -1);
										this.trackEvent(
											this.name, "request", "tid",
											xhr.getResponseHeader("X-Metaweb-TID")
										);
									}
								)
								.fail(
									(xhr, strStatus, errorThrown) => {
										this.status_error();
										this.trackEvent(
											this.name, "request", "error",
											{	"url"      : url, //this.url,
												"response" : xhr ? xhr.responseText : strStatus
											}
										);
										this.input.trigger("fb-error", Array.prototype.slice.call(arguments));
									}
								);
								/*
								.always(
									(data, strStatus, xhr) => {
										var errorThrown;
										if (! data.prefix ) {
											errorThrown = xhr;
											xhr = data;
										}
										if (xhr) {
											this.trackEvent(
												this.name, "request", "tid",
												xhr.getResponseHeader("X-Metaweb-TID")
											);
										}
									}
								);
								*/
	                    	},
							this.options.xhr_delay
						);
	            },

				flyout_request(data) {
	                var dataSuggest = this.flyoutpane.data("data.suggest");
	                if (dataSuggest && data.iri === dataSuggest.iri) {
	                    if ( ! this.flyoutpane.is(":visible") ) {
	                        this.flyout_position( this.get_selected() );
	                        this.flyoutpane.show();
	                        this.input.trigger("fb-flyoutpane-show", this);
	                    }
	                    return;
	                }

	                // Check $.suggest.flyout.cache...
	                var cached = $.suggest.flyout.cache[data.iri];
	                if (cached) {
	                    this.flyout_response(cached);
	                    return;
	                }

	                clearTimeout(this.flyout_request.timeout);
	                this.flyout_request.timeout =
	                    setTimeout(
							() => {
								this.flyout_response(data);
							},
							this.options.xhr_delay
						);
	            },

	            flyout_response(data) {
	                var selection = this.get_selected() || [];
	                if (this.pane.is(":visible") && selection.length) {
	                    var dataSuggest = selection.data("data.suggest");
	                    if (dataSuggest && data.iri === dataSuggest.iri) {
	                        this.flyoutpane.html('<div class="fbs-flyout-content">' + data.description + '</div>');
	                        this.flyout_position(selection);
	                        this.flyoutpane.show().data("data.suggest", dataSuggest);
	                        this.input.trigger("fb-flyoutpane-show", this);
	                    }
	                }
	            }
            }
		)
	);

	//
	// Add New Defaults
	//
	$.extend(
		// object extended...
		$.suggest.suggestTerm,
		// object merged (overwrites same named prior objects)...
		{
         	defaults : $.extend(
				// deep copy...
        		true,
				// object extended...
        		{},
				// object merged (overwrites same named prior objects)...
        		$.suggest.suggest.defaults,
				// object merged (overwrites same named prior objects)...
        		{
        			"service_url"         : "",
        			"service_path"        : "command/rdf-transform/suggest-term",
        			"flyout_service_path" : "command/rdf-transform/suggest-term",
        			"type"                : "class",
        			"suggest_new"         : $.i18n('rdft-dialog/add-it'),
        			"cache"               : false,
        			//"soft                : true,
        			"nomatch" :  {
        				"title"   : $.i18n('rdft-dialog/no-matches'),
        				"heading" : null,
        				"tips"    : null
        			}
         		}
			)
     	}
	);
} ) (jQuery); // ...self-execute on JQuery
