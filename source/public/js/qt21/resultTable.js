/**
 * @author Renlong Ai <renlong.ai@dfki.de>
 * requirements:
 * 1. table with id result-table
 * 2. set on table: data-row-style="rowStyle"
 * 3. /updatePass in route for ajax calls
 * 4. var reportid from other js source
 */
function rowStyle(row, index) {
    var classes = ['success', 'warning', 'danger', 'info'];
    if (row["pass"].indexOf(" glyphicon-alert")!==-1) return {
        classes: 'warning'
    };
    else if (row["pass"].indexOf("glyphicon-remove") !==-1) return {
        classes: 'danger'
    };
    else if (row["pass"].indexOf("glyphicon-exclamation-sign")!==-1) return {
        classes: 'info'
    };
    else return {
        classes: 'success'
    };
}


// $('#warningButton').html("<i id='toggleWarning' class='glyphicon glyphicon-info-sign' style='color:black;top:-1px;'></i>");
// $('#toggleWarning').parent().click(function(evt) {
//     evt.stopPropagation();
//     var x = $('#toggleWarning');
//     x.toggleClass('glyphicon-info-sign').toggleClass('glyphicon-exclamation-sign');
//     if (x.hasClass('glyphicon-info-sign')) {
//         x.css('color', 'black');
//         $('#result-table').bootstrapTable('filterBy', {
//             pass: ["<i class='glyphicon glyphicon-alert' style='color:black'></i>",
//              "<i class='glyphicon glyphicon-remove isBlocked' style='color:black'></i>",
//               "<i class='glyphicon glyphicon-ok isBlocked' style='color:black'></i>"]
//         });
//     } else {
//         x.css('color', 'orange');
//         $('#result-table').bootstrapTable('filterBy', {
//             pass: ["<i class='glyphicon glyphicon-alert' style='color:black'></i>"]
//         });
//     }
//     renderPassRows();
//     return false;
// });
//
// $('#ruleButton').click(function(){
//     var x = $(this).children().first();
//     if(x.hasClass('notShowing')){
//         x.css('color','orange');
//         x.removeClass('notShowing');
//         $('#result-table').bootstrapTable('showColumn','positiveTokens').bootstrapTable('showColumn','positiveRegex').bootstrapTable('showColumn','negativeTokens').bootstrapTable('showColumn','negativeRegex');
//     }
//     else{
//         x.css('color','black');
//         x.addClass('notShowing');
//         $('#result-table').bootstrapTable('hideColumn','positiveTokens').bootstrapTable('hideColumn','positiveRegex').bootstrapTable('hideColumn','negativeTokens').bootstrapTable('hideColumn','negativeRegex');
//     }
//     renderPassRows();
// });

// $('#result-table').on('column-search.bs.table', function(field, checked) {
//     renderPassRows();
// });

$('#result-table').on('post-body.bs.table', function(e, field, checked) {
    renderPassRows();
});

//$('#result-table').on('click','td:last-child',function(){
//        var a = this.firstChild;
//        $(a).editable('show');
//    });



var openedIndex;
var openedRow;

// $('#result-table').on('expand-row.bs.table', function (e, index, row, $detail) {
//
//             if(openedIndex!==index)
//                 $('#result-table').bootstrapTable('collapseRow',openedIndex);
//             openedIndex=index;
//             openedRow=row;
//             $.get('/detail',
//                 {source:row['source'],
//                 translation:row['translation'],
//                 pr:row['positiveRegex'],
//                 nr:row['negativeRegex'],
//                 pt:row['positiveTokens'],
//                 nt:row['negativeTokens'],
//                 sentenceid:row['id']},
//                 function (res) {
//                     $detail.html(res);
//             });
// });



$('#result-table').on('click-row.bs.table',
    function(e, row, element, field){
        if(field==='pass') {
            return;
        }
        else if(field==='comment'){
            return;
        }

        $('#orderModal').modal('show');

        $.get('/detail',
            {source:row['source'],
                translation:row['translation'],
                pr:row['positiveRegex'],
                nr:row['negativeRegex'],
                pt:row['positiveTokens'],
                nt:row['negativeTokens'],
                sentenceid:row['id']},
            function (res) {
                $('.modal-content').html(res);

            });

        openedRow=row;
        openedIndex = element.index();

        element.siblings().removeClass('marked');
        element.addClass('marked');
    }
);

$('#result-table').on('editable-save.bs.table', function (e, row, element, $detail) {

    $.ajax({
        type: 'POST',
        url: '/updateSentenceComment',
        data: JSON.stringify({'id':element['id'],'reportid':reportid,'value':element['comment']}),
        success: function(data) {
        },
        contentType: 'application/json',
        dataType: 'json'
    });

});

$(function(){
    $('#orderModal').modal({
        keyboard: true,
        show:false,

    });
});

//$('#result-table1').bootstrapTable({
//    onClickRow: function(row, element, field){
//        if(field==='pass') return;
//        else if(field==='comment'){
//            $(element[0]).find('.editable').editable('toggle');
//            console.log($(element[0]).find('.editable'));
//            return;
//        }
//        if(openedRow!==row)
//            $('#result-table').bootstrapTable('collapseAllRows',false);
//        $(element[0]).find('.detail-icon').triggerHandler("click");
//        openedRow=row;
//    }
//});

var tokens = {};

function renderPassRows() {
    //set click function to warning
    $('#result-table td i').parent().unbind('click').click(function(evt) {
        evt.stopPropagation();
        var x = $(this).find('i');
        var index = parseInt(x.parent().parent().attr("data-index"));
        var tableData = $('#result-table').bootstrapTable('getData');
        if (!x.hasClass('isBlocked') && !x.hasClass('icon-setting')) {
            var np = 1;
            if (x.hasClass('glyphicon-alert')) {
                x.removeClass('glyphicon-alert').addClass('glyphicon-remove');
                $(this).parent().attr('class', 'danger');
                np = 2;
                tableData[index]['pass']="<i class='glyphicon glyphicon-remove' style='color:black'></i>";
                tokens[tableData[index]['id']]='n'+tableData[index]['translation'];
            } else if (x.hasClass('glyphicon-ok')) {
                x.removeClass('glyphicon-ok').addClass('glyphicon-alert');
                $(this).parent().attr('class', 'warning');
                np = 3;
                tableData[index]['pass']="<i class='glyphicon glyphicon-alert' style='color:black'></i>";
                delete tokens[tableData[index]['id']];
            } else if (x.hasClass('glyphicon-remove')){
                x.removeClass('glyphicon-remove').addClass('glyphicon-ok');
                $(this).parent().attr('class', 'success');
                np = 1;
                tableData[index]['pass']="<i class='glyphicon glyphicon-ok' style='color:black'></i>";
                //tokens[tableData[index]['id']]={'w': '0'};
                tokens[tableData[index]['id']]='p'+tableData[index]['translation'];
            }

            //console.log("np:" + np);

            // if(np<3) x.parent().parent().addClass('marked');
            // else x.parent().parent().removeClass('marked');
            //
            // if(Object.keys(tokens).length>0) $('#btnApplyToken').addClass('marked');
            // else $('#btnApplyToken').removeClass('marked');

            $('#tokenCount').html(Object.keys(tokens).length);

            $.ajax({
                type: 'POST',
                url: '/updatePass',
                data: JSON.stringify({
                    'report': reportid,
                    'id': $(this).next().html(),
                    'pass': np
                }),
                success: function(data) {},
                contentType: 'application/json',
                dataType: 'json'
            });
        }
        return false;
    });

    $('td i').each(function(){
        if(!$(this).hasClass('isBlocked') && ($(this).hasClass('glyphicon-remove') || $(this).hasClass('glyphicon-ok'))){
            // $(this).parent().parent().addClass('marked');
            // $('#btnApplyToken').addClass('marked');
            $('#tokenCount').html(Object.keys(tokens).length);
        }
    });
}


function refreshTable(data) {

    $('#result-table').bootstrapTable('load', data);
    renderPassRows();

}

function applyTokens() {

    if(Object.keys(tokens).length===0) return;

    for(var pid in tokens){
        var result = $.grep($('#result-table').bootstrapTable('getData'), function(e){ return e.id == pid; })[0];
        var token = tokens[pid];
        var head = token.substring(0,1);
        var tail = token.substring(1);
        var a = head==='p'?'positiveTokens':'negativeTokens';
        if(result[a].length===0)
            result[a]=tail;
        else
            result[a]=result[a]+'|'+tail;
    }

    $.ajax({
        type: 'POST',
        url: '/updateTokens',
        data: JSON.stringify({
            'direction': direction,
            'tokens': tokens
        }),
        success: function(data) {
            // $('#btnApplyToken').removeClass('marked');
            tokens={};
            $('#tokenCount').html('0');
            // $('td i').each(function(){
            //     $(this).parent().parent().removeClass('marked');
            // });
        },
        contentType: 'application/json',
        dataType: 'json'
    });
}

function skipTokens(){
    // $('#btnApplyToken').removeClass('marked');
    tokens={};
    $('#tokenCount').html('0');
    // $('td i').each(function(){
    //     $(this).parent().parent().removeClass('marked');
    // });
}
