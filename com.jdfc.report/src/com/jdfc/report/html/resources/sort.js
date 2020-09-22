(function () {
    function initialSort(linkelementids) {
        window.linkelementids = linkelementids;
        var hash = window.location.hash;
        if (hash) {
            var m = hash.match(/up-./);
            if (m) {
                var header = window.document.getElementById(m[0].charAt(3));
                if (header) {
                    sortColumn(header, true);
                }
                return;
            }
            var m = hash.match(/dn-./);
            if (m) {
                var header = window.document.getElementById(m[0].charAt(3));
                if (header) {
                    sortColumn(header, false);
                }
                return
            }
        }
    }

    window['initialSort'] = initialSort;
})