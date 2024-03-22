$(function(){
    API.uploads.getMappings(function(mappings){
        for(var i = 0; i < mappings.length; i++){
            $("#search-start-node").append("<option value='" + mappings[i] + "'>" + mappings[i] + "</option>");
            $("#search-join-on").append("<option value='" + mappings[i] + "'>" + mappings[i] + "</option>");
        }
        $("#search-button").click(function(){
            var queryString = $("#search-query-string").val();
            var enrichmentDepth = parseInt($("#search-enrichment-depth-number").val());
            var skip = parseInt($("#search-skip-number").val());
            var limit = parseInt($("#search-limit-number").val());
            var enrichmentMode = $('#search-enrichment-mode').find(":selected").val();
            var searchType = $('#search-query-field').find(":selected").val();
            var startNode = $('#search-start-node').find(":selected").val();
            var joinOn = $("#search-join-on").val();
            $("#search-button-results-container").empty();
            var buildTableHeader = function(dataArray){
                var res = "<tr>";
                for(var i = 0; i < dataArray.length; i++){
                    res += "<th>" + dataArray[i] + "</th>";
                }
                res += "</tr>";
                return res;
            };
            var buildTableRow = function(dataObject){
                var res = "<tr>";
                for(var i = 0; i < mappings.length; i++){
                    var value = dataObject[mappings[i]];
                    if (!value){
                        value = "";
                    }else{
                        value = JSON.stringify(value);
                    }
                    res += "<td>" + value + "</td>";
                }
                res += "</tr>";
                return res;
            };
            $("#search-button-results-container").append("<table id='search-results-table'>" + buildTableHeader(mappings) + "</table>");

            var batchSize = 10;
            var loopLimit = limit < batchSize?1:(limit/batchSize);
            for(var i = 0; i < loopLimit; i++){
                var params = {};
                params.limit = batchSize;
                params.skip = skip + (batchSize * i);
                params.maxDepth = enrichmentDepth;
                params.enrichmentMethod = enrichmentMode;
                params.filterType = searchType;
                params.filter = queryString;
                params.joinOn = joinOn;
                if (startNode !== "ALL"){
                    params.recordType = startNode;
                }
                $.ajax({
                    url: '/search/search?' + $.param(params),
                    method: 'get',
                    dataType: 'json',
                    success: function(data){
                    console.log(data);
                        for(var i = 0; i < data.length; i++){
                            $("#search-results-table").append(buildTableRow(data[i]));
                        }
                    }
                });
            }
        });
    });
});