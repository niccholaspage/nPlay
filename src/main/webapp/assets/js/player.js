var cookie = document.cookie.substr(document.cookie.indexOf("=") + 1);

var webSocket = new WebSocket("ws://" + location.hostname + ":" + location.port + "/play-status");

webSocket.onopen = function (event) {
    webSocket.send("channel," + cookie);
};

webSocket.onmessage = function (event) {
    var obj = JSON.parse(event.data);

    if (obj.status) {
        setStatus(obj.status);

        return;
    }

    var video = document.getElementById("myvideo");

    if (video.src !== obj.url) {
        video.src = obj.url;
    }

    var diff = Math.abs(video.currentTime - obj.seconds);

    if (diff > 1) {
        video.currentTime = obj.seconds;
    }

    if (obj.playing && video.paused) {
        video.currentTime = obj.seconds;

        video.play();
    } else if (!obj.playing && !video.paused) {
        video.currentTime = obj.seconds;

        video.pause();
    }
};

webSocket.onclose = function () {
    setStatus("e:WebSocket closed! This is Luke's fault. Try refreshing the page.");
};

function setStatus(message) {
    var snackbarContainer = document.querySelector('#snackbar');

    if (message.length == 0) {
        snackbarContainer.MaterialSnackbar.cleanup_();

        return;
    }

    snackbarContainer.MaterialSnackbar.showSnackbar({
        message: message.substring(2)
    });
}

function toggle(video) {
    video.paused ? video.play() : video.pause();
}

window.addEventListener("load", function () {
    var video = document.getElementById('myvideo');

    var url = document.getElementById('url');

    var channel = document.getElementById('channel');

    var channelURL = window.location.origin + "/channel/" + cookie;

    channel.href = channelURL;

    channel.text = channelURL;

    url.onkeydown = function (event) {
        if (event.which == 13) {
            webSocket.send("play-url," + this.value);

            this.value = "";

            document.getElementById('url-container').MaterialTextfield.checkDirty();
        }
    };

    document.addEventListener('keypress', function (event) {
        if (event.which == 32 && document.activeElement !== url) {
            toggle(video);
        }
    });

    video.addEventListener('play', function () {
        webSocket.send("play");
    });

    video.addEventListener('pause', function () {
        webSocket.send("pause");
    });

    video.addEventListener('seeked', function () {
        webSocket.send("seek," + this.currentTime);
    });
});