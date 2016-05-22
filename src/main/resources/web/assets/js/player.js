var cookie = document.cookie.substr(document.cookie.indexOf("=") + 1);

var webSocket = new WebSocket("ws://" + location.hostname + ":" + location.port + "/play-status");

webSocket.onopen = function (event) {
    webSocket.send("channel," + cookie);
};

/**
 *
 * @param event WebSocket event
 * @param obj.seconds   Current video time
 * @param obj.playing   If the video is currently playing or not
 */
webSocket.onmessage = function (event) {
    var obj = JSON.parse(event.data);

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

    setStatus(obj.status);
};

webSocket.onclose = function () {
    setStatus("e:WebSocket closed! This is Luke's fault. Try refreshing the page.");
};

function setStatus(message) {
    var status = document.getElementById("status");

    if (message.length == 0) {
        status.style.visibility = "hidden";

        return;
    }

    var type = message.substring(0, 1);

    if (type == "i") {
        status.setAttribute("class", "alert alert-info");
    } else {
        status.setAttribute("class", "alert alert-danger");
    }

    status.textContent = message.substring(2);

    status.style.visibility = "visible";
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
            this.value = ""
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