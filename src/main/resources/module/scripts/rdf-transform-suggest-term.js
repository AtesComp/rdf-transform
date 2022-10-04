/*
 *  Function Suggest Term for RDF Transform
 *
 *  Modifies OpenRefine's Suggest Term function for RDF Transform terms.
 *  RDF Transform extends the $.suggest object for RDF Term resolution.
 *      Add RDF Term processing to the Suggest flyout.
 *      Based on Freebase Suggest (fbs) Extension.
 *      See: https://docs.openrefine.org/technical-reference/suggest-api
 *
 *  The "suggest" object is a custom extension added to jQuery
 *      See OpenRefine's webapp/modules/core/scripts/util/custom-suggest.js
 *
 *  Copyright 2022 Keven L. Ates
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
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
                //      "this" in final object.
                create_item(data, response) {
                    var li =
                        $("<li></li>")
                        // @ts-ignore
                        .addClass(this.options.css.item);
                    var name =
                        $("<div></div>")
                        // @ts-ignore
                        .addClass(this.options.css.item_name)
                        .append(
                            $("<div></div>")
                            // @ts-ignore
                            .addClass(this.options.css.item_type)
                            .text(data.iri)
                        );
                    var nameText =
                        $("<label></label>")
                        .append(
                            $.suggest
                            .strongify(data.prefix + ":" + data.localPart, response.prefix)
                        );

                    //data.label = nameText.text();
                    name.append(nameText);
                    li.append(name);

                    // TODO: Very smelly hack to disable cache
                    $.suggest.cache = {};

                    return li;
                },

                request(query, cursor) {
                    var data = {};
                    // @ts-ignore
                    data[this.options.query_param_name] = query;

                    // @ts-ignore
                    clearTimeout(this.request.timeout);
                    // Parameters names are defined for Suggest Term Command (SuggestTermCommand.java) Java code.
                    // The "project" holds the Project ID of the project to search...
                    data.project = theProject.id;
                    // The "type" holds the value type to search (class, property)...
                    // @ts-ignore
                    data.type = this.options.type;
                    // The "prefix" holds the query search value...
                    // NOTE: The Query Prefix is needed by the response processor.
                    data.prefix = query;
                    // See Defaults below for other default parameters.

                    var url =
                        // @ts-ignore
                        this.options.service_url +
                        // @ts-ignore
                        this.options.service_path;

                    var ajax_options = {
                        "url"      : url,
                        "data"     : data,
                        "dataType" : "json",
                        "cache"    : true
                    };

                    // @ts-ignore
                    this.request.timeout =
                        setTimeout(
                            () => {
                                $.get(ajax_options)
                                .done(
                                    (data, strStatus, xhr) => {
                                        $.suggest.cache[url] = data;
                                        // NOTE: The Query Prefix is needed by the response processor and
                                        //      is included from the server side.
                                        // @ts-ignore
                                        this.response(data, cursor ? cursor : -1);
                                        // @ts-ignore
                                        this.trackEvent(
                                            // @ts-ignore
                                            this.name, "request", "tid", xhr.getResponseHeader("X-Metaweb-TID")
                                        );
                                    }
                                )
                                .fail(
                                    (xhr, strStatus, errorThrown) => {
                                        // @ts-ignore
                                        this.status_error();
                                        // @ts-ignore
                                        this.trackEvent(
                                            // @ts-ignore
                                            this.name, "request", "error",
                                            {   "url"      : url, //this.url,
                                                "response" : xhr ? xhr.responseText : strStatus
                                            }
                                        );
                                        // @ts-ignore
                                        this.input.trigger("fb-error", Array.prototype.slice.call(arguments));
                                    }
                                )//;
                                .always(
                                    (data, strStatus, xhr) => {
                                        var errorThrown;
                                        if (! data.prefix ) {
                                            errorThrown = xhr;
                                            xhr = data;
                                        }
                                        if (xhr) {
                                            // @ts-ignore
                                            this.trackEvent(
                                                // @ts-ignore
                                                this.name, "request", "tid", xhr.getResponseHeader("X-Metaweb-TID")
                                            );
                                        }
                                    }
                                );
                            },
                            // @ts-ignore
                            this.options.xhr_delay
                        );
                },

                flyout_request(data) {
                    // @ts-ignore
                    var dataSuggest = this.flyoutpane.data("data.suggest");
                    if (dataSuggest && data.iri === dataSuggest.iri) {
                        // @ts-ignore
                        if ( ! this.flyoutpane.is(":visible") ) {
                            // @ts-ignore
                            this.flyout_position( this.get_selected() );
                            // @ts-ignore
                            this.flyoutpane.show();
                            // @ts-ignore
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

                    // @ts-ignore
                    clearTimeout(this.flyout_request.timeout);
                    // @ts-ignore
                    this.flyout_request.timeout =
                        setTimeout(
                            () => {
                                this.flyout_response(data);
                            },
                            // @ts-ignore
                            this.options.xhr_delay
                        );
                },

                flyout_response(data) {
                    // @ts-ignore
                    var selection = this.get_selected() || [];
                    // @ts-ignore
                    if (this.pane.is(":visible") && selection.length) {
                        var dataSuggest = selection.data("data.suggest");
                        if (dataSuggest && data.iri === dataSuggest.iri) {
                            // @ts-ignore
                            this.flyoutpane.html('<div class="fbs-flyout-content">' + data.description + '</div>');
                            // @ts-ignore
                            this.flyout_position(selection);
                            // @ts-ignore
                            this.flyoutpane.show().data("data.suggest", dataSuggest);
                            // @ts-ignore
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
