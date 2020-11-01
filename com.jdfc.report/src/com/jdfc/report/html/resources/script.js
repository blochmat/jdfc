window.onload = init

function init() {
    const elements = document.getElementsByClassName("defaultOpen");
    for (let i = 0; i < elements.length; i++) {
        elements[i].click();
    }
}
function sortTables() {
    var tables, body, rows, switching, i, x, y, shouldSwitch;
    tables = document.getElementsByTagName("table");
    for (let table of tables) {
        if (table.id !== "classDetailView") {
            body = table.tBodies.item(0);
            switching = true;
            while (switching) {
                switching = false;
                rows = body.rows;
                for(i = 0; i < (rows.length - 1); i++) {
                    shouldSwitch = false;
                    x = rows[i].cells.item(0);
                    y = rows[i + 1].cells.item(0);
                    if (isNaN(Number(x.innerHTML))
                        && isNaN(Number(y.innerHTML))
                        && !x.innerText.toLowerCase().includes("default")
                        && !x.innerText.toLowerCase().includes("init")
                        && x.innerText.toLowerCase() > y.innerText.toLowerCase()) {
                        shouldSwitch = true;
                        break;
                    } else {
                        if(compare(x.innerHTML, y.innerHTML) > 0) {
                            shouldSwitch = true;
                            break;
                        }
                    }
                }
                if(shouldSwitch){
                    rows[i].parentNode.insertBefore(rows[i+1], rows[i]);
                    switching = true;
                }
            }
        }
    }
}

function compare(a, b) {
    return a - b;
}

function openTab(element, tabName) {
    let i, tabcontent, tablinks;
    tabcontent = document.getElementsByClassName("bla");
    for (i = 0; i < tabcontent.length; i++) {
        tabcontent[i].style.display = "none";
    }
    tablinks = document.getElementsByClassName("tablinks");
    for (i = 0; i < tablinks.length; i++) {
        tablinks[i].className = tablinks[i].className.replace(" active", "");
    }
    const elements = document.getElementsByClassName(tabName);
    for (i = 0; i < elements.length; i++) {
        elements[i].style.display = "block";
    }
    element.className += " active";
}
