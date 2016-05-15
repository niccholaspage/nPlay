window.addEventListener("load", function () {
    var currentPage = window.location.pathname;

    $("a[href='" + currentPage + "']").last().parent().attr('class', 'active');
});