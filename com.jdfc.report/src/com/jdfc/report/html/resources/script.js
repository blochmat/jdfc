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

function openTab(element) {
    let i, otherTabButtonId, otherTabButton, tabElements, otherTabElements;

    if(element.id.includes("DefTabButton")) {
        otherTabButtonId = element.id.replace("DefTabButton", "UseTabButton");
        otherTabButton = document.getElementById(otherTabButtonId);
    } else {
        otherTabButtonId = element.id.replace("UseTabButton", "DefTabButton");
        otherTabButton = document.getElementById(otherTabButtonId);
    }

    element.className += " active";
    let tabElementClass = element.id.replace("Button", "")
    tabElements = document.getElementsByClassName(tabElementClass);
    console.log(tabElements);
    for (i = 0; i < tabElements.length; i++) {
        tabElements[i].style.display = "flex";
    }

    if(otherTabButton != null) {
        otherTabButton.className =  otherTabButton.className.replace(" active", "");
        let otherTabElementClass = otherTabButtonId.replace("Button", "")
        otherTabElements = document.getElementsByClassName(otherTabElementClass);
        for (i = 0; i < otherTabElements.length; i++) {
            otherTabElements[i].style.display = "none";
        }
    }
}
