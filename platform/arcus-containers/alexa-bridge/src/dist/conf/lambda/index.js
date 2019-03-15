var https = require('https');
var REMOTE_CLOUD_BASE_PATH = '/alexa/shs';
var REMOTE_CLOUD_HOSTNAME = '';
var REMOTE_CLOUD_PORT = 443;

exports.handler = function(event, context, callback) {

    console.log("received input event: ", event);

    var json = JSON.stringify(event);

    var options = {
        hostname: REMOTE_CLOUD_HOSTNAME,
        port: REMOTE_CLOUD_PORT,
        path: REMOTE_CLOUD_BASE_PATH,
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Content-Length': Buffer.byteLength(json)
        }
    };

    var serverErrorV2 = function (e) {
        console.log("failed to execute post: ", e);
        var headers = {
            namespace: 'Alexa.ConnectedHome.Control',
            name: 'DependentServiceUnavailableError',
            payloadVersion: '2',
            messageId: event.header.messageId
        };

        var payload = {
            'dependentServiceName': 'Iris Alexa Bridge'
        };

        var result = {
            header: headers,
            payload: payload
        };

        console.log("sending error ", result);
        callback(null, result);
    };

    var serverErrorV3 = function (e) {
        console.log("failed to execute post: ", e);
        var headers = {
            namespace: 'Alexa',
            name: 'ErrorResponse',
            payloadVersion: '3',
            messageId: event.directive.header.messageId
        };

        if(event.directive.header.correlationToken) {
            headers.correlationToken = event.directive.header.correlationToken;
        }

        var payload = {
            'type': 'INTERNAL_ERROR',
            'message': 'Dependent service Iris Alexa Bridge is not responding.'
        };

        var result = {
            event: {
                header: headers,
                payload: payload
            }
        };

        console.log("sending error ", result);
        callback(null, result);
    };

    var serverError = function(e) {
        var ver = '2';
        if(event.header) {
            ver = event.header.payloadVersion;
        } else if(event.directive.header) {
            ver = event.directive.header.payloadVersion;
        }
        if(ver === '3') {
            serverErrorV3(e);
        } else {
            serverErrorV2(e);
        }
    };

    var callbackFn = function(response) {
        var str = '';
        response.on('data', function(chunk) { str += chunk.toString('utf-8'); });
        response.on('end', function() {
            console.log("received response: ", str);
            try {
                callback(null, JSON.parse(str));
            } catch(e) {
                serverError(e);
            }
        });

        response.on('error', serverError);
    };

    var post_req = https.request(options, callbackFn);
    post_req.on('error', serverError);
    post_req.write(json);
    post_req.end();
};