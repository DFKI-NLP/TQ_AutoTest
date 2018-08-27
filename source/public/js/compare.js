let colorArray = ["rgb(227, 200, 0)",
    "rgb(170, 0, 255)",
    "rgb(0, 138, 0)",
    "rgb(229, 20, 0)",
    "rgb(0, 80, 239)",
    "rgb(250, 104, 0)",
    "rgb(162, 0, 37)",
    "rgb(118, 96, 138)",
    "rgb(164, 196, 0)",
    "rgb(216, 0, 115)",
    "rgb(240, 163, 10)",
    "rgb(0, 171, 169)",
    "rgb(135, 121, 78)",
    "rgb(244, 114, 208)",
    "rgb(109, 135, 100)"
];

function sortTable(n) {
    let table, rows, switching, i, x, y, shouldSwitch, dir, switchcount = 0;
    table = document.getElementById("templateTable");
    switching = true;
    //Set the sorting direction to ascending:
    dir = "asc";
    /*Make a loop that will continue until
     no switching has been done:*/
    while (switching) {
        //start by saying: no switching is done:
        switching = false;
        rows = table.getElementsByTagName("TR");
        /*Loop through all table rows (except the
         first, which contains table headers):*/
        for (i = 1; i < (rows.length - 1); i++) {
            //start by saying there should be no switching:
            shouldSwitch = false;
            /*Get the two elements you want to compare,
             one from current row and one from the next:*/
            x = rows[i].getElementsByTagName("TD")[n];
            y = rows[i + 1].getElementsByTagName("TD")[n];
            /*check if the two rows should switch place,
             based on the direction, asc or desc:*/
            if (dir === "asc") {
                if (n === 0) {
                    if (x.innerHTML.toLowerCase() > y.innerHTML.toLowerCase()) {
                        //if so, mark as a switch and break the loop:
                        shouldSwitch = true;
                        break;
                    }
                } else if (n === 2) {
                    let xa = x.innerHTML.split("<br>").map(function (x) {
                        return x.trim()
                    });
                    let xl = xa.sort(function (a, b) {
                        return b > a;
                    })[0];
                    let ya = y.innerHTML.split("<br>").map(function (x) {
                        return x.trim()
                    });
                    let yl = ya.sort(function (a, b) {
                        return b > a;
                    })[0];
                    if (xl > yl) {
                        shouldSwitch = true;
                        break;
                    }
                }

            } else if (dir === "desc") {
                if (n === 0) {
                    if (x.innerHTML.toLowerCase() < y.innerHTML.toLowerCase()) {
                        //if so, mark as a switch and break the loop:
                        shouldSwitch = true;
                        break;
                    }
                } else if (n === 2) {
                    let xa = x.innerHTML.split("<br>").map(function (x) {
                        return x.trim()
                    });
                    let xl = xa.sort(function (a, b) {
                        return b > a;
                    })[0];
                    let ya = y.innerHTML.split("<br>").map(function (x) {
                        return x.trim()
                    });
                    let yl = ya.sort(function (a, b) {
                        return b > a;
                    })[0];
                    if (xl < yl) {
                        shouldSwitch = true;
                        break;
                    }
                }

            }
        }
        if (shouldSwitch) {
            /*If a switch has been marked, make the switch
             and mark that a switch has been done:*/
            rows[i].parentNode.insertBefore(rows[i + 1], rows[i]);
            switching = true;
            //Each time a switch is done, increase this count by 1:
            switchcount++;
        } else {
            /*If no switching has been done AND the direction is "asc",
             set the direction to "desc" and run the while loop again.*/
            if (switchcount === 0 && dir === "asc") {
                dir = "desc";
                switching = true;
            }
        }
    }
}

document.getElementById("loaderContainer").style.display = "none";
document.getElementById("tableContent").style.display = "none";

let thValue;

$('.compareBtn').click(function () {
    let id = $(this).attr('id');
    thValue = $(this).attr('th');
    $.ajax({
        url: '/getComparisonData',
        data: JSON.stringify({
            'id': id
        }),
        type: 'POST',
        contentType: 'application/json',
        dataType: 'json',
        beforeSend: function () {
            $('#result-panel').css("display", "block");
            document.getElementById("loaderContainer").style.display = "block";
            document.getElementById("tableContent").style.display = "none";
        },
        success: function (data) {
            document.getElementById("loaderContainer").style.display = "none";
            document.getElementById("tableContent").style.display = "block";
            refreshComparison(data);
            getCorrelation(data);
            resultData = data;
        },
        error: function (jqXHR, textStatus, errorThrown) {
            alert(textStatus + ': ' + errorThrown);
        }
    });
});

let resultData;

let borc = 'barrier';
$('.b-or-c').click(function () {
    borc = this.innerHTML;
    if (borc === 'phenomenon') borc = 'barrier';
    //reload(reportid);
    refreshComparison(resultData);
    $('.b-or-c').toggleClass('active');
});

let norp = 'percentage';
$('.np').click(function () {
    norp = this.innerHTML;
    refreshComparison(resultData);
    $('.np').toggleClass('active');
});


let chosenPercentage = ['Pass'];
$('.choosePercentage').click(function () {
    if (chosenPercentage.includes(this.innerHTML)) {
        chosenPercentage.splice(chosenPercentage.indexOf(this.innerHTML), 1);
        $(this).removeClass("active");
    } else {
        chosenPercentage.push(this.innerHTML);
        $(this).addClass('active');
    }
    refreshComparison(resultData);

});

let percentageData = {
    labels: [],
    datasets: []
};
let ctx2 = document.getElementById("percentage-chart").getContext("2d");
window.myBarPercentage = new Chart(ctx2, {
    type: 'bar',
    data: percentageData,
    options: {
        responsive: true,
        title: {
            display: true,
            text: ''
        },
        legend: {
            display: false
        },
        tooltips: {
            enabled: true,
            mode: 'single',
            callbacks: {
                label: function (tooltipItems) {
                    return tooltipItems.yLabel + '%';
                }
            }
        },
        scales: {
            yAxes: [{
                ticks: {
                    beginAtZero: true,
                    maxTicksLimit: 5,
                    callback: function (value) {
                        return value + '%';
                    }
                }
            }],
            xAxes: [{
                ticks: {
                    autoSkip: false,
                    maxTicksLimit: 20
                }
            }]
        }
    }
});

let groupedData = {
    labels: [],
    datasets: []
};
let ctx = document.getElementById("bar-chart").getContext("2d");
window.groupChart = new Chart(ctx, {
    type: 'bar',
    data: groupedData,
    options: {
        responsive: true,
        title: {
            display: true,
            text: ''
        },
        legend: {
            display: true,
            position: 'bottom'
        },
        tooltips: {
            enabled: true,
            mode: 'single',
            callbacks: {
                label: function (tooltipItems) {
                    return tooltipItems.yLabel + '%';
                }
            }
        },
        scales: {
            yAxes: [{
                ticks: {
                    beginAtZero: true,
                    maxTicksLimit: 5,
                    callback: function (value) {
                        return value + '%';
                    }
                }
            }],
            xAxes: [{
                ticks: {
                    autoSkip: false,
                    maxTicksLimit: 20
                }
            }]
        }
    }
});

function refreshComparison(compData) {

    let labels = [];

    let totals = [];

    let percentages = [];

    let allp = [];

    let totalsall = 0;

    for (let j = 0; j < compData.length; j++) {
        let passMap = new Map();
        let failMap = new Map();
        let warnMap = new Map();
        let data = compData[j];
        for (let i = 0; i < data.length; i++) {
            let temp = data[i];

            let barrier = temp[borc];

            let theMap;
            if (temp['pass'] === "<i class='glyphicon glyphicon-ok isBlocked' style='color:black'></i>") theMap =
                passMap;
            else if (temp['pass'] === "<i class='glyphicon glyphicon-remove isBlocked' style='color:black'></i>")
                theMap = failMap;
            else theMap = warnMap;

            if (theMap.has(barrier)) {
                theMap.set(barrier, theMap.get(barrier) + 1);
            } else {
                theMap.set(barrier, 1);
            }

            if (labels.indexOf(barrier) === -1) labels.push(barrier);
        }

        let passData = [];
        let failData = [];
        let warnData = [];

        let pData = [];
        let allpass = [0, 0, 0];

        for (let i = 0; i < labels.length; i++) {
            let label = labels[i];
            if (passMap.has(label)) passData.push(passMap.get(label));
            else passData.push(0);

            if (failMap.has(label)) failData.push(failMap.get(label));
            else failData.push(0);

            if (warnMap.has(label)) warnData.push(warnMap.get(label));
            else warnData.push(0);

            let passp = passMap.has(label) ? passMap.get(label) : 0;
            let failp = failMap.has(label) ? failMap.get(label) : 0;
            let warnp = warnMap.has(label) ? warnMap.get(label) : 0;
            let total = passp + failp + warnp;
            if (total === 0)
                pData.push([0.0, 0.0, 0.0]);
            else {
                let perArray = [-1, -1, -1];
                chosenPercentage.forEach(function (option) {
                    switch (option) {
                        case 'Pass':
                            perArray[0] = ((passp / total) * 100).toFixed(1);
                            break;
                        case 'Fail':
                            perArray[1] = ((failp / total) * 100).toFixed(1);
                            break;
                        case 'Warning':
                            perArray[2] = ((warnp / total) * 100).toFixed(1);
                            break;

                    }

                });
                pData.push(perArray);

            }


            //push total only once.
            if (totals.length < labels.length) {
                totals.push(total);
                //add to total
                totalsall += total;
            }
            //add pass
            chosenPercentage.forEach(function (option) {
                switch (option) {
                    case "Pass":
                        allpass[0] += ((passMap.has(label)) ? passMap.get(label) : 0);
                        break;
                    case "Fail":
                        allpass[1] += ((failMap.has(label)) ? failMap.get(label) : 0);
                        break;
                    case "Warning":
                        allpass[2] += ((warnMap.has(label)) ? warnMap.get(label) : 0);
                        break;

                }

            });

        }
        percentages.push(pData);
        allp.push(allpass);
    }

    //render table now
    let textTable = $("#textTable");
    textTable.empty();
    textTable.append('<tr><th></th><th class="right">#</th></tr>');
    textTable.find('th:last-child').after(thValue);

    if (chosenPercentage.length === 1) {
        textTable.removeClass("poly-colored");
    } else {
        textTable.addClass("poly-colored");
    }

    for (let i = 0; i < labels.length; i++) {
        textTable.find('tr:last').after(`<tr class="statistic-row"><td onclick="showComparision('${labels[i]}',${borc !== 'barrier'})">` + labels[i] +
            '</td><td class="right total-number">' + totals[i] + '</td></tr>');
        for (let j = 0; j < percentages.length; j++) {
            if (norp === 'percentage') {
                let per = "";
                let filterMap = ["Pass", "Fail", "Warning"];
                percentages[j][i].forEach(function (value, index) {
                    if (value !== -1) {
                        per += ` <span class="compare-table color-${index}" onclick="showSamples('${labels[i]}',${j},'${filterMap[index]}')">${value}%</span>`;
                    }
                });


                textTable.find('td:last').after("<td class='right statistic-per'>" + per.trim() + '</td>');

            } else {
                let num = "";
                percentages[j][i].forEach(function (value, index) {

                    if (value !== -1) {
                        num +=
                            ` <span class="compare-table color-${index}">${(totals[i] * value / 100).toFixed(0)}</span>`;
                    }
                });

                textTable.find('td:last').after("<td class='right statistic-num'>" + num.trim() + '</td>');

            }

        }
    }


    textTable.find('tr:last').after('<tr class="statistic-sum-row"><td>Sum</td><td class="right statistic-sum">' +
        totalsall + '</td></tr>');
    for (let i = 0; i < allp.length; i++) {
        let sum = ["", "", ""];
        chosenPercentage.forEach(function (value) {
            switch (value) {
                case "Pass":
                    sum[0] = `<span class="compare-table color-0">${allp[i][0]}</span>`;
                    break;
                case "Fail":
                    sum[1] = `<span class="compare-table color-1">${allp[i][1]}</span>`;
                    break;
                case "Warning":
                    sum[2] = `<span class="compare-table color-2">${allp[i][2]}</span>`;
                    break;

            }
        });
        textTable.find('td:last').after('<td class="right statistic-sum-col">' + sum.join(" ") + '</td>');
    }

    textTable.find('tr:last').after('<tr class="statistic-ave-row"><td>Average</td><td></td></tr>');
    for (let i = 0; i < allp.length; i++) {
        let avg = ["", "", ""];
        chosenPercentage.forEach(function (value) {
            switch (value) {
                case "Pass":
                    avg[0] =
                        `<span class="compare-table color-0">${(allp[i][0] / totalsall * 100).toFixed(1)}%</span>`;
                    break;
                case "Fail":
                    avg[1] =
                        `<span class="compare-table color-1">${(allp[i][1] / totalsall * 100).toFixed(1)}%</span>`;
                    break;
                case "Warning":
                    avg[2] =
                        `<span class="compare-table color-2">${(allp[i][2] / totalsall * 100).toFixed(1)}%</span>`;
                    break;

            }
        });
        textTable.find('td:last').after('<td class="right statistic-ave-col">' + avg.join(" ") + '</td>');
    }

    //set bold to the highest values only if there is exactly one column selected
    if (chosenPercentage.length === 1) {
        textTable.find("tbody tr.statistic-row").each(function () {
            let data = [];
            let x = $(this);
            let cells = x.find('td');
            let max = 0;

            let zBase = 1.96;
            $(cells).each(function () {
                let d = $(this).text() + '';
                if ($(this).hasClass("statistic-num")) {
                    data.push(d);
                    if (parseFloat(d) > max) {
                        max = parseFloat(d);
                    }
                }
                if (d.indexOf('%') !== -1) {
                    data.push(d);
                    if (parseFloat(d) > max) {

                        max = parseFloat(d);
                    }
                }
            });
            let totalCount = parseInt($(this).find(".total-number").text() + '');
            let boldCellNum = 0;

            //highlight
            $(cells).each(function () {
                if ($(this).hasClass("statistic-per")) {
                    let maxCounts = totalCount * max / 100;
                    let maxPercentage = max / 100;
                    let currentPercentage = $(this).text().split("%")[0] / 100;
                    let currentCounts = parseInt(currentPercentage * totalCount + '');
                    let p = (maxPercentage + currentPercentage) / 2;
                    let zScore;
                    if (maxPercentage === currentPercentage) {
                        zScore = 0;
                    } else if (currentPercentage === 0) {
                        zScore = Infinity;
                    } else {
                        zScore = (maxPercentage - currentPercentage) / Math.sqrt(p * (1.00 - p) * (
                            (
                                1.00 / currentCounts) + (1.00 / maxCounts)));
                    }
                    if (Math.abs(zScore) <= zBase) {
                        $(this).css("font-weight", "bold");
                        boldCellNum++;
                    }
                }

            });
            if (boldCellNum === cells.length - 2) { //Here it's a bit volatile, but I don't want to change too much of the code.
                $(cells).css("font-weight", "inherit");
            }
        });
        let aveRow = textTable.find(".statistic-ave-row");
        let sumRow = textTable.find(".statistic-sum-row");
        let sumCounts = sumRow.find(".statistic-sum").text();
        let aveRowValues = aveRow.find(".statistic-ave-col").sort(function (a, b) {
            return $(b).text().split("%")[0] - $(a).text().split("%")[0];
        });
        let boldAveNum = 0;

        aveRowValues.each(function () {

            let maxPercentage = aveRowValues[0].innerText.split("%")[0] / 100;
            let maxCounts = parseInt(maxPercentage * sumCounts + '');
            let currentPercentage = $(this).text().split("%")[0] / 100;
            let currentCounts = parseInt(currentPercentage * sumCounts + '');
            let p = (maxPercentage + currentPercentage) / 2;
            let zScore;

            let zBase = 1.96;
            if (maxPercentage === currentPercentage) {
                zScore = 0;
            } else if (currentPercentage === 0) {
                zScore = Infinity;
            } else {
                zScore = (maxPercentage - currentPercentage) / Math.sqrt(p * (1.00 - p) * ((1.00 /
                    currentCounts) + (1.00 / maxCounts)));
            }
            if (Math.abs(zScore) <= zBase) {
                $(this).css("font-weight", "bold");
                boldAveNum++;
            }
        });

        if (boldAveNum === aveRowValues.length) {
            aveRowValues.css("font-weight", "inherit");
        }
    }

    if (chosenPercentage.length === 1) {

        $("#percentage-chart-holder").show();
        $("#bar-chart-holder").show();
        let currentChosen = "";
        switch (chosenPercentage[0]) {
            case "Pass":
                currentChosen = 0;
                break;
            case "Fail":
                currentChosen = 1;
                break;
            case "Warning":
                currentChosen = 2;
                break;
        }
        //now the graphs
        let tmplabels = thValue.split(/<th class='right'>/);
        let finallabels = [];
        for (label of tmplabels) {
            let ll = label.trim();
            if (ll.length > 0)
                finallabels.push(ll.replace('</th>', '').trim());
        }
        let colors = [];
        for (let i = 0; i < percentageData.labels.length; i++)
            colors.push(colorArray[i]);

        percentageData.labels = finallabels;
        percentageData.datasets = [{
            backgroundColor: colors,
            data: allp.map(function (x) {
                return (x[currentChosen] / totalsall * 100).toFixed(1)
            })
        }];
        window.myBarPercentage.update();

        //sort alphabetically. is so creepy. <=Why is it creepy?
        let newlabels = [];
        let newtotals = [];
        let newpercentages = new Array(percentages.length);
        newlabels.push(labels[0]);
        newtotals.push(totals[0]);
        for (let i = 0; i < percentages.length; i++) {
            newpercentages[i] = [];
            newpercentages[i].push(percentages[i][0][currentChosen])
        }

        for (let i = 1; i < labels.length; i++) {
            let j = 0;

            while (labels[i] > newlabels[j]) {
                j++;
            }
            newlabels.splice(j, 0, labels[i]);
            newtotals.splice(j, 0, totals[i]);
            for (let k = 0; k < percentages.length; k++)
                newpercentages[k].splice(j, 0, percentages[k][i][currentChosen]);

        }


        groupedData.labels = newlabels.map(function (x, i) {
            return ['', x, '(N=' + newtotals[i] + ')']
        });
        let newgroupdata = [];
        for (let i = 0; i < finallabels.length; i++) {

            newgroupdata.push({
                label: finallabels[i],
                backgroundColor: colors[i],
                data: newpercentages[i]
            })
        }
        groupedData.datasets = newgroupdata;
        window.groupChart.update();
    } else {
        $("#percentage-chart-holder").hide();
        $("#bar-chart-holder").hide();
    }


}


function getCorrelation(data) {
    let datanew = data.map(function (x) {
        return x.map(function (y) {
            if (y.pass === "<i class='glyphicon glyphicon-ok isBlocked' style='color:black'></i>")
                return 0;
            else if (y.pass ===
                "<i class='glyphicon glyphicon-remove isBlocked' style='color:black'></i>")
                return 1;
            else return 2;
        })
    });

    let correlationTable = $('#correlationTable');

    correlationTable.empty().append('<tr><th></th><tr>').find('th:last-child').after(thValue);
    let titles = thValue.split(/<\/th>\s*<th class='right'>/).map(function (x) {
        return x.replace("<th class='right'>", "").replace("</th>", "").trim()
    });

    for (let i = 0; i < titles.length; i++) {
        correlationTable.find('tr:last').after('<tr><td>' + titles[i] + '</td></tr>');

        for (let j = 0; j <= i; j++) {
            correlationTable.find('td:last').after('<td class="right">' + getPearsonCorrelation(datanew[i], datanew[j])
                .toFixed(2) + '</td>');
        }
    }
}

function getPearsonCorrelation(x, y) {
    let shortestArrayLength = 0;

    if (x.length === y.length) {
        shortestArrayLength = x.length;
    } else if (x.length > y.length) {
        shortestArrayLength = y.length;
        console.error('x has more items in it, the last ' + (x.length - shortestArrayLength) +
            ' item(s) will be ignored');
    } else {
        shortestArrayLength = x.length;
        console.error('y has more items in it, the last ' + (y.length - shortestArrayLength) +
            ' item(s) will be ignored');
    }

    let xy = [];
    let x2 = [];
    let y2 = [];

    for (let i = 0; i < shortestArrayLength; i++) {
        xy.push(x[i] * y[i]);
        x2.push(x[i] * x[i]);
        y2.push(y[i] * y[i]);
    }

    let sum_x = 0;
    let sum_y = 0;
    let sum_xy = 0;
    let sum_x2 = 0;
    let sum_y2 = 0;

    for (let i = 0; i < shortestArrayLength; i++) {
        sum_x += x[i];
        sum_y += y[i];
        sum_xy += xy[i];
        sum_x2 += x2[i];
        sum_y2 += y2[i];
    }

    let step1 = (shortestArrayLength * sum_xy) - (sum_x * sum_y);
    let step2 = (shortestArrayLength * sum_x2) - (sum_x * sum_x);
    let step3 = (shortestArrayLength * sum_y2) - (sum_y * sum_y);
    let step4 = Math.sqrt(step2 * step3);
    return step1 / step4;
}

Chart.plugins.register({
    afterDatasetsDraw: function (chart) {
        // To only draw at the end of animation, check for easing === 1
        let ctx = chart.ctx;

        chart.data.datasets.forEach(function (dataset, i) {
            let meta = chart.getDatasetMeta(i);
            if (!meta.hidden) {
                meta.data.forEach(function (element, index) {
                    // Draw the text in black, with the specified font
                    ctx.fillStyle = 'rgb(0, 0, 0)';

                    let fontSize = 10;
                    let fontStyle = 'normal';
                    let fontFamily = 'Helvetica Neue';
                    ctx.font = Chart.helpers.fontString(fontSize, fontStyle, fontFamily);

                    // Just naively convert to string for now
                    let dataString = dataset.data[index].toString() + '%';

                    // Make sure alignment settings are correct
                    ctx.textAlign = 'center';
                    ctx.textBaseline = 'middle';

                    let padding = 5;
                    let position = element.tooltipPosition();
                    ctx.fillText(dataString, position.x, position.y - (fontSize / 2) -
                        padding);

                });
            }
        });
    }
});

let samples;

$("#percentage-chart").click(function (e) {
    let activeBars = myBarPercentage.getElementAtEvent(e);
    let index = activeBars[0]._index;
    let pass = $('.choosePercentage.active')[0].innerHTML;

    samples = $.grep(resultData[index], function (v) {
        let filter = 'ok';
        if (pass === 'Fail') filter = 'remove';
        else if (pass === 'Warning') filter = 'alert';
        return v['pass'].indexOf(filter) > 0;
    });


    $.get('/getSampleSentences',
        function (res) {
            $('.modal-content').html(res);

        });

    $('#orderModal').modal('show');
});

$("#bar-chart").click(function (e) {
    let activeBars = groupChart.getElementAtEvent(e);
    let label = activeBars[0]._model['label'][1];
    let pass = $('.choosePercentage.active')[0].innerHTML;
    let index = activeBars[0]._datasetIndex;

    samples = $.grep(resultData[index], function (v) {
        let filter = 'ok';
        if (pass === 'Fail') filter = 'remove';
        else if (pass === 'Warning') filter = 'alert';
        return v['pass'].indexOf(filter) > 0 && (v['category'] === label || v['barrier'] === label);
    });


    $.get('/getSampleSentences',
        function (res) {
            $('.modal-content').html(res);

        });

    $('#orderModal').modal('show');
});


/**
 * This is the function to show the sample table
 * @param {string} label - The label of the samples
 * @param {number} j - the index number of the compared engine in the comparison table
 * @param {string} filter - the tag of the current samples(Pass, Fail or Warning)
 *
 */
function showSamples(label, j, filter) {
    let pass = filter;
    samples = $.grep(resultData[j], function (v) {
        let filter = 'ok';
        if (pass === 'Fail') filter = 'remove';
        else if (pass === 'Warning') filter = 'alert';
        return v['pass'].indexOf(filter) > 0 && (v['category'] === label || v['barrier'] === label);
    });


    $.get('/getSampleSentences',
        function (res) {
            $('.modal-content').html(res);
            console.log(res);

        });

    $('#orderModal').modal('show');
}

/**
 * This is the function to show the comparision table for one particular category or phenomenon.
 *  @author He Wang
 *  @param {string} label - either a category name or a phenomenon to display
 *  @param {boolean} isCategory - whether the label is a category or a phenomenon
 */


function showComparision(label, isCategory) {
    let comparisonTable = $(
        `<table class="table" style="border-radius:5px">
            <thead>
                <tr>
                    <th class="comparison-id">ID</th><th class="comparision-source">Source</th>
                    <th class="comparison-category">Category</th><th class="comparision-phenomenon">Phenomenon</th>${thValue}
                </tr>
            </thead>
            <tbody>
            </tbody>
        </table>
        `);
    let ids = new Map();
    for (let i = 0; i < resultData.length; i++) {
        for (let j = 0; j < resultData[i].length; j++) {
            let currentId = resultData[i][j].id;
            if (!ids.has(currentId)) {
                ids.set(currentId, [resultData[i][j]]);
            } else {
                let updated = ids.get(currentId);
                updated.push(resultData[i][j]);
                ids.set(currentId, updated);
            }
        }
    }

    ids.forEach(function (value, key, map) {
        if (value.length < resultData.length) {
            map.delete(key);
        }
    });
    ids.forEach(function (value) {

        if ((isCategory && value[0].category === label) || (!isCategory && value[0].barrier === label)) {

            let rowBuilder =
                $(`
                  <tr>
                    <td>${value[0].id}</td><td>${value[0].source}</td><td>${value[0].category}</td><td>${value[0].barrier}</td>
                  </tr>`);
            for(let i=0;i<resultData.length;i++){
                let style=value[i].pass.includes("glyphicon-ok")?"success":value[i].pass.includes("glyphicon-remove")?"danger":"warning"
                rowBuilder.append(`<td class="${style}">${value[i].translation}</td>`);
            }

            comparisonTable.append(rowBuilder);
        }


    });

    $(".modal-content").empty().append(comparisonTable);
    $("#orderModal").modal('show');
}