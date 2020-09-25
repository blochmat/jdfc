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
    // switching = true;
    // /* Make a loop that will continue until
    // no switching has been done: */
    // while (switching) {
    //     // Start by saying: no switching is done:
    //     switching = false;
    //     rows = table.rows;
    //     /* Loop through all table rows (except the
    //     first, which contains table headers): */
    //     for (i = 1; i < (rows.length - 1); i++) {
    //         // Start by saying there should be no switching:
    //         shouldSwitch = false;
    //         /* Get the two elements you want to compare,
    //         one from current row and one from the next: */
    //         x = rows[i].getElementsByTagName("TD")[0];
    //         y = rows[i + 1].getElementsByTagName("TD")[0];
    //         // Check if the two rows should switch place:
    //         if (x.innerHTML.toLowerCase() > y.innerHTML.toLowerCase()) {
    //             // If so, mark as a switch and break the loop:
    //             shouldSwitch = true;
    //             break;
    //         }
    //     }
    //     if (shouldSwitch) {
    //         /* If a switch has been marked, make the switch
    //         and mark that a switch has been done: */
    //         rows[i].parentNode.insertBefore(rows[i + 1], rows[i]);
    //         switching = true;
    //     }
    // }
