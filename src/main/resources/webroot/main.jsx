var React = require('react');
var ReactDOM = require('react-dom');
var $;
window.jQuery = $ = require('./jquery-2.1.4.min');
var bootstrap = require('./bootstrap.min');
var TaskList = require('./task-list.jsx');
var FileList = require('./file-list.jsx');
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

setupForm();

ReactDOM.render(
  <TaskList pollInterval={2000} />,
  document.getElementById('tasks')
);
ReactDOM.render(
  <FileList pollInterval={2000} />,
  document.getElementById('files')
);
