$(function(){
    var displayConnectDropboxButton = function(url){
        $('#drop-box-container').append('<a href="' + url + '">Connect Dropbox</a>');
    };
    var displayDropBoxBrowser = function(){
        var container = $('#drop-box-container');
        container.append('<div>Dropbox connected. File browser TBD</div>');
    };
    API.dropbox.connectionStatus(function(isConnected){
        if (isConnected){
            displayDropBoxBrowser();
        }else{
            API.dropbox.connectUrl(function(url){
                displayConnectDropboxButton(url);
            });
        }
    });
});