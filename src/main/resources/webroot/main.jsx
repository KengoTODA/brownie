var React = require('react');
var ReactDOM = require('react-dom');
var $;
window.jQuery = $ = require('./jquery-2.1.4.min');
var bootstrap = require('./bootstrap.min');
var TaskList = require('./task-list.jsx');
'use strict';

function setupForm() {
  'use strict';
  var $form = $('#post-video').submit(function(e){
    'use strict';
    e.preventDefault();
    var formData = new FormData($form[0]);
    $.ajax(action, {
      processData: false,
      type: 'post',
      contentType: false,
      dataType: 'text',
      data: formData
    })
    .done(function(data) {
      'use strict';
      $result.addClass('info').removeClass('warning').text(data).show();
    })
    .fail(function() {
      'use strict';
      $result.addClass('warning').removeClass('info').text('failed to register').show();
    });
    return false;
  });
  var $result = $form.find('#result');
  var action = $form.attr('action');
}
/**
 * @param {number} x
 * @return {string}
 */
function numberWithCommas(x) {
    return x.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ",");
}
function loadTasks() {
  'use strict';
  /**
   * @type {!Element}
   */
  var $tasks = document.getElementById('tasks');
  ReactDOM.render(
    <TaskList url="/tasks" pollInterval={2000} />,
    $tasks
  );
}

function loadFiles() {
  'use strict';
  var $div = $('#files');
  var $tasks = $div.find('tbody');
  $.ajax('files/', {
    type: 'get'
  })
  .done(function(data) {
    'use strict';
    function displayTable() {
        var flagment = document.createDocumentFragment();
        $.each(data, function(i, file) {
          'use strict';
          var fileId = file['fileId'];
          var $tr = $('<tr>').data('key', fileId).append(
            $('<td>').append(
              $('<a>').text(file['fileName']).attr('href', 'files/' + fileId)
            )
          ).append(
            $('<td>').text(numberWithCommas(file['contentLength']) + ' bytes').css('text-align', 'right')
          ).append(
            $('<td>').text(new Date(file['generated']))
          );
          flagment.appendChild($tr[0]);
        });
        $tasks[0].appendChild(flagment);
        $tasks.closest('table').show();
    }
    if (data.length === 0) {
      $div.find('p').show();
    } else {
      displayTable();
    }
  });
}
setupForm();
loadTasks();
loadFiles();
