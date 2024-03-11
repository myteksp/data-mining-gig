var API = {};

$(function(){
    var dropbox = {
        connectionStatus: function(callBack){
            $.ajax({
                url: '/dropbox/connectionStatus',
                method: 'get',
                dataType: 'json',
                success: function(data){
            	     callBack(data.isConnected);
                }
            });
        },
        connectUrl: function(callBack){
            $.ajax({
                url: '/dropbox/getAuthorizationUrl',
                method: 'get',
                dataType: 'html',
                success: function(data){
                    callBack(data);
                }
            });
        },
        listFolder: function(path, callBack){
            $.ajax({
                url: '/dropbox/listFolder?path=' + path,
                method: 'get',
                dataType: 'json',
                success: function(data){
                    callBack(data);
                }
            });
        }
    };
    API.dropbox = dropbox;

    var uploads = {
        getMappings: function(callBack){
            $.ajax({
                url: '/uploads/getMappings',
                method: 'get',
                dataType: 'json',
                success: function(data){
                    callBack(data);
                }
            });
        },
        listUnfinishedUploads: function(callBack){
            $.ajax({
                url: '/uploads/listUnfinishedUploads',
                method: 'get',
                dataType: 'json',
                success: function(data){
                    callBack(data);
                }
            });
        },
    };
    API.uploads = uploads;
});