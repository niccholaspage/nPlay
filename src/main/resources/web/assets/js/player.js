if (typeof(EventSource) !== "undefined") {
    var source = new EventSource('play-status');
    source.onmessage = function (event) {
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
    };
} else {
    console.log("No event source");
}

function toggle(video) {
    video.paused ? video.play() : video.pause();
}

window.addEventListener("load", function () {
    var video = document.getElementById('myvideo');

    var url = document.getElementById('url');

    url.onkeydown = function (event) {
        if (event.which == 13) {
            $.post("play-url", {url: url.value});
            this.value = ""
        }
    };

    document.onkeypress = function (event) {
        if (event.which == 32 && document.activeElement !== url) {
            toggle(video);
        }
    };

    video.addEventListener('play', function () {
        $.get("play");
    });

    video.addEventListener('pause', function () {
        $.get("pause");
    });

    video.addEventListener('seeked', function () {
        $.get("seek", {currentTime: this.currentTime});
    });
});