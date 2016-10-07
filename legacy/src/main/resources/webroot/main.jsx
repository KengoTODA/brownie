let React = require('react');
let ReactDOM = require('react-dom');
let $;
window.jQuery = $ = require('./jquery-2.1.4.min');
let bootstrap = require('./bootstrap.min');
let TaskList = require('./task-list.jsx');
let FileList = require('./file-list.jsx');
require('./css/bootstrap.min.css');
require('./main.css');
'use strict';

function setupForm() {
  'use strict';
  let $form = $('#post-video').submit((e) => {
    'use strict';
    e.preventDefault();
    let formData = new FormData($form[0]);
    $.ajax(action, {
      processData: false,
      type: 'post',
      contentType: false,
      dataType: 'text',
      data: formData
    })
    .done((data) => {
      'use strict';
      $result.addClass('info').removeClass('warning').text(data).show();
    })
    .fail(() => {
      'use strict';
      $result.addClass('warning').removeClass('info').text('failed to register').show();
    });
    return false;
  });
  let $result = $form.find('#result');
  let action = $form.attr('action');
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
